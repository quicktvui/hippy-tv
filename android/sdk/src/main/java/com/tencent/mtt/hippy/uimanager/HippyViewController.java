/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.uimanager;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;


import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.hippyext.FocusManagerModule;
import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.base.ui.ITVView;
import com.quicktvui.hippyext.RenderUtil;
import com.quicktvui.base.ui.TVBaseView;
import com.quicktvui.base.ui.TVFocusAnimHelper;
import com.quicktvui.base.ui.TriggerTaskHost;
import com.quicktvui.hippyext.TriggerTaskManagerModule;
import com.quicktvui.base.ui.ExtendTag;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.quicktvui.hippyext.views.TVTextView;
import com.quicktvui.hippyext.views.fastlist.FastAdapter;
import com.quicktvui.hippyext.views.fastlist.FastItemView;
import com.quicktvui.hippyext.views.fastlist.FastListView;
import com.quicktvui.hippyext.views.fastlist.FastListViewController;
import com.quicktvui.hippyext.views.fastlist.ReplaceChildView;
import com.quicktvui.hippyext.views.fastlist.TemplateCodeParser;
import com.quicktvui.hippyext.views.fastlist.Utils;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.common.HippyTag;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogAdapterUtils;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.common.CommonBorder;
import com.tencent.mtt.hippy.views.image.HippyImageView;
import com.tencent.mtt.hippy.views.list.HippyListView;
import com.tencent.mtt.hippy.views.list.HippyRecycler;
import com.tencent.mtt.hippy.views.list.TVSingleLineListView;
import com.tencent.mtt.hippy.views.text.HippyTextView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.hippy.views.view.HippyViewGroupController;
import com.tencent.mtt.supportui.views.IGradient;
import com.tencent.mtt.supportui.views.IShadow;
import com.tencent.mtt.supportui.views.asyncimage.AsyncImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"deprecation", "unused"})
public abstract class HippyViewController<T extends View & HippyViewBase> implements
    View.OnFocusChangeListener {

  private static final String TAG = "HippyViewController";

  private CacheMap cacheViewsPool;

  private static final MatrixUtil.MatrixDecompositionContext sMatrixDecompositionContext = new MatrixUtil.MatrixDecompositionContext();
  private static final double[] sTransformDecompositionArray = new double[16];
  private boolean bUserChageFocus = false;
  HippyViewEvent mFocusEvent;
  HippyViewEvent mSelectEvent;

  public void setCacheViewsPool(CacheMap cacheViews) {
    this.cacheViewsPool = cacheViews;
  }

  @SuppressWarnings("deprecation")
  public View createView(HippyRootView rootView, int id, HippyEngineContext hippyEngineContext,
      String className,
      HippyMap initialProps) {
    View view = null;

    if (rootView != null) {
      Context rootViewContext = rootView.getContext();
      if (rootViewContext instanceof HippyInstanceContext) {
        @SuppressWarnings("rawtypes") Map nativeParam = ((HippyInstanceContext) rootViewContext)
            .getNativeParams();
        if (nativeParam != null) {
          Object object = nativeParam.get(HippyCustomViewCreator.HIPPY_CUSTOM_VIEW_CREATOR);
          if (object instanceof HippyCustomViewCreator) {
            view = ((HippyCustomViewCreator) object)
                .createCustomView(className, rootView.getContext(), initialProps);
          }
        }
      }

      final String cache = initialProps.getString("cacheType");
      if(view == null && cacheViewsPool != null){
        view = cacheViewsPool.get(cache);
        if(view instanceof HippyRecycler){
          ((HippyRecycler) view).resetProps();
        }
        if(view != null){
          onCreateViewByCache(view,cache,initialProps);
        }
        if(LogUtils.isDebug()) {
          Log.d(TAG, "createView from cacheViews :" + view + ",type:" + initialProps.getString("cacheType"));
        }
      }
      if (view == null) {
        view = createViewImpl(rootView.getContext(), initialProps);
        if (view == null) {
          view = createViewImpl(rootView.getContext());
        }
      }

      //zhaopeng add
      onAfterCreateView(view,initialProps,className);

      LogUtils.d(TAG, "createView id " + id);
      view.setId(id);
      //view.setTag(className);
      HippyMap tagObj = HippyTag.createTagMap(className, initialProps,cache);
      view.setTag(tagObj);

    }
    return view;
  }

  protected void onCreateViewByCache(View view, String type, HippyMap props){

  }

  protected void onAfterCreateView(View view,HippyMap initialProps){
    //zhaopeng add 添加tag
    ExtendTag.putTag(view,new ExtendTag());
    CustomControllerHelper.dealCustomProp(view,initialProps);
  }

  protected void onAfterCreateView(View view,HippyMap initialProps,String className){
    //zhaopeng add 添加tag
    onAfterCreateView(view,initialProps);
    ExtendTag.obtainExtendTag(view).nodeClassName = className;

    if(view.getContext() instanceof HippyInstanceContext) {
      HippyEngineContext engineContext = ((HippyInstanceContext) view.getContext()).getEngineContext();
      FocusManagerModule.GlobalFocusConfig config = FocusManagerModule.getGlobalFocusConfig(engineContext);
      if(config.forceDisableFocus){
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
      }
    }
  }


  public void onAfterUpdateProps(T v) {

  }



  //	public void updateProps(View view, HippyMap props)
  //	{
  //		if (props == null)
  //			return;
  //		try
  //		{
  //			HippyMap styleProps = props.get(NodeProps.STYLE) != null ? (HippyMap) props.get(NodeProps.STYLE) : null;
  //			Method[] targetMethods = getClass().getMethods();
  //
  //			for (Method targetMethod : targetMethods)
  //			{
  //				HippyControllerProps hippyProps = targetMethod.getAnnotation(HippyC ontrollerProps.class);
  //				if (hippyProps != null)
  //				{
  //					String propsName = hippyProps.name();
  //					if (props.get(propsName) != null) // try normal props first
  //					{
  //						targetMethod.invoke(this, view, props.get(propsName));
  //					}
  //					else if (styleProps != null && styleProps.get(propsName) != null) // then try styleprops
  //					{
  //						targetMethod.invoke(this, view, styleProps.get(propsName));
  //					}
  //				}
  //			}
  //		}
  //		catch (IllegalAccessException e)
  //		{
  //			e.printStackTrace();
  //		}
  //		catch (InvocationTargetException e)
  //		{
  //			e.printStackTrace();
  //		}
  //		catch (IllegalArgumentException e)
  //		{
  //			e.printStackTrace();
  //		}
  //	}


  protected void updateExtra(View view, Object object) {

  }

  @SuppressWarnings("SameReturnValue")
  protected StyleNode createNode(boolean isVirtual, int rootId) {
    return null;
  }

  protected StyleNode createNode(boolean isVirtual) {
    return new StyleNode();
  }

  public void updateLayout(int id, int x, int y, int width, int height,
      ControllerRegistry componentHolder) {
    View view = componentHolder.getView(id);
    if (view != null) {
      view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      if (!shouldInterceptLayout(view, x, y, width, height)) {
        view.layout(x, y, x + width, y + height);
      }
    }
  }

  protected boolean shouldInterceptLayout(View view, int x, int y, int width, int height) {
    return false;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean handleGestureBySelf() {
    return false;
  }

  @Deprecated
  protected abstract View createViewImpl(Context context);

  protected View createViewImpl(Context context, HippyMap iniProps) {
    return null;
  }


  /**
   * transform
   **/
  @HippyControllerProps(name = NodeProps.TRANSFORM, defaultType = HippyControllerProps.ARRAY)
  public void setTransform(T view, HippyArray transformArray) {
    if (transformArray == null) {
      resetTransform(view);
    } else {
      applyTransform(view, transformArray);
    }
  }

  /**
   * transform
   **/
  @HippyControllerProps(name = "layout", defaultType = HippyControllerProps.ARRAY)
  public void setLayoutInfo(T view, HippyArray layout) {
    if(layout.size() == 4){
      CustomControllerHelper.updateLayout(view,Utils.toPX(layout.getInt(0)),Utils.toPX(layout.getInt(1)),Utils.toPX(layout.getInt(2)),Utils.toPX(layout.getInt(3)));
    }
  }


  @HippyControllerProps(name = NodeProps.PROP_ACCESSIBILITY_LABEL)
  public void setAccessibilityLabel(T view, String accessibilityLabel) {
    if (accessibilityLabel == null) {
      accessibilityLabel = "";
    }
    view.setContentDescription(accessibilityLabel);
  }

  /**
   * zIndex
   **/
  @HippyControllerProps(name = NodeProps.Z_INDEX, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setZIndex(T view, int zIndex) {
    HippyViewGroupController.setViewZIndex(view, zIndex);
    ViewParent parent = view.getParent();
    if (parent instanceof IHippyZIndexViewGroup) {
      ((IHippyZIndexViewGroup) parent).updateDrawingOrder();
    }
  }

  /**
   * color/border/alpha
   **/
  @HippyControllerProps(name = NodeProps.BACKGROUND_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBackground(T view, int backgroundColor) {
    view.setBackgroundColor(backgroundColor);
  }

  /**
   * color/border/alpha
   **/
  @HippyControllerProps(name = "backgroundColorStr", defaultType = HippyControllerProps.STRING)
  public void setBackgroundString(T view, String backgroundColor) {
    try {
      view.setBackgroundColor(Color.parseColor(backgroundColor));
      ExtendTag.obtainExtendTag(view).pendingBackGroundColor = Color.parseColor(backgroundColor);
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  @HippyControllerProps(name = NodeProps.OPACITY, defaultType = HippyControllerProps.NUMBER, defaultNumber = 1.f)
  public void setOpacity(T view, float opacity) {
    view.setAlpha(opacity);
  }

  @HippyControllerProps(name = NodeProps.BORDER_RADIUS, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setBorderRadius(T view, float borderRadius) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderRadius(borderRadius, CommonBorder.BorderRadiusDirection.ALL.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_TOP_LEFT_RADIUS, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setTopLeftBorderRadius(T view, float topLeftBorderRadius) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view).setBorderRadius(topLeftBorderRadius,
          CommonBorder.BorderRadiusDirection.TOP_LEFT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_TOP_RIGHT_RADIUS, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setTopRightBorderRadius(T view, float topRightBorderRadius) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view).setBorderRadius(topRightBorderRadius,
          CommonBorder.BorderRadiusDirection.TOP_RIGHT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_BOTTOM_RIGHT_RADIUS, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setBottomRightBorderRadius(T view, float bottomRightBorderRadius) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view).setBorderRadius(bottomRightBorderRadius,
          CommonBorder.BorderRadiusDirection.BOTTOM_RIGHT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_BOTTOM_LEFT_RADIUS, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setBottomLeftBorderRadius(T view, float bottomLeftBorderRadius) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view).setBorderRadius(bottomLeftBorderRadius,
          CommonBorder.BorderRadiusDirection.BOTTOM_LEFT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_WIDTH, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setBorderWidth(T view, float borderWidth) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderWidth(borderWidth, CommonBorder.BorderWidthDirection.ALL.ordinal());
    }
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_DOWN_ID, defaultType = HippyControllerProps.BOOLEAN)
  public void setNextFocusDownId(T view, int id) {
    view.setNextFocusDownId(id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_UP_ID, defaultType = HippyControllerProps.BOOLEAN)
  public void setNextFocusUpId(T view, int id) {
    view.setNextFocusUpId(id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_LEFT_ID, defaultType = HippyControllerProps.BOOLEAN)
  public void setNextFocusLeftId(T view, int id) {
    view.setNextFocusLeftId(id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_RIGHT_ID, defaultType = HippyControllerProps.BOOLEAN)
  public void setNextFocusRightId(T view, int id) {
    view.setNextFocusRightId(id);
  }

  //2.7 add 指定 nextFocus By sid
  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_DOWN_SID, defaultType = HippyControllerProps.STRING)
  public void setNextFocusDownSId(T view, String id) {
   FocusUtils.setNextFocusDownSID(view,id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_UP_SID, defaultType = HippyControllerProps.STRING)
  public void setNextFocusUpSId(T view, String id) {
    FocusUtils.setNextFocusUpSID(view,id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_LEFT_SID, defaultType = HippyControllerProps.STRING)
  public void setNextFocusLeftSId(T view, String id) {
    FocusUtils.setNextFocusLeftSID(view,id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_RIGHT_SID, defaultType = HippyControllerProps.STRING)
  public void setNextFocusRightSId(T view, String id) {
    FocusUtils.setNextFocusRightSID(view,id);
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_SID, defaultType = HippyControllerProps.MAP)
  public void setNextFocusSId(T view, HippyMap hippyMap) {
    if(hippyMap != null){
      final String nextLeft = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_LEFT_FRONT);
      final String nextRight = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_RIGHT_FRONT);
      final String nextUP = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_UP_FRONT);
      final String nextDown = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_DOWN_FRONT);
      setNextFocusLeftSId(view,nextLeft);
      setNextFocusRightSId(view,nextRight);
      setNextFocusUpSId(view,nextUP);
      setNextFocusDownSId(view,nextDown);
    }
  }

  //nextFocus By sid

  @Deprecated
  @HippyControllerProps(name = "idTag", defaultType = HippyControllerProps.STRING)
  public void setTagID(T view, String tagID) {
    view.setTag(R.id.tag_item_id,tagID);
  }

  @HippyControllerProps(name = "sid", defaultType = HippyControllerProps.STRING)
  public void setSID(T view, String sid) {
    //给view设置string的id，整个view树里唯一
    ExtendUtil.putViewSID(view,sid);
    if (view instanceof TVBaseView) {
      ((TVBaseView) view).onSetSid(sid);
    }
    if(LogUtils.isDebug()){
      Log.d("configID4Item","call by vue setSID :"+sid+",view:"+ExtendUtil.debugViewLite(view));
    }
  }

  //此处vue是怎么传值的
  @HippyControllerProps(name = "scrollOverride", defaultType = HippyControllerProps.MAP)
  public void setScrollOverride(T view, HippyMap hippyMap) {
    if(view instanceof HippyViewGroup) {
      ((HippyViewGroup) view).setScrollOverride(hippyMap);
    }
  }
  @HippyControllerProps(name = "focusableInTouchMode", defaultType = HippyControllerProps.BOOLEAN)
  public void setFocusableInTouchMode(T view, boolean focusable) {
    view.setFocusableInTouchMode(focusable);
  }

  @HippyControllerProps(name = NodeProps.FOCUSABLE, defaultType = HippyControllerProps.BOOLEAN)
  public void setFocusable(T view, boolean focusable) {
//    LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":setFocusable "+focusable+" view is :"+ExtendUtil.debugViewLite(view));
    view.setFocusable(focusable);
    if (view instanceof AsyncImageView) {
      ((AsyncImageView) view).setEnableMouse(focusable);
    }
    if((view.getId() != -1)) {//2022.05.18 zhaopeng itemView不需要在这里设置事件
      if (focusable) {
        if (LogUtils.isDebug()) {
          Log.i("ViewController", "ElementCallback setFocusable this view:" + view+",id:"+view.getId());
        }
        view.setOnFocusChangeListener(this);
      } else {
        if (LogUtils.isDebug()) {
          Log.i("ViewController", "ElementCallback setFocusable null view:" + view+",id:"+view.getId());
        }
        view.setOnFocusChangeListener(null);
      }
    }else{
      if(LogUtils.isDebug()) {
        Log.e(TAG, "setup Focus fail,itemView 不需要设置FOCUSABLE");
      }
    }
  }

  @HippyControllerProps(name = NodeProps.REQUEST_FOCUS, defaultType = HippyControllerProps.BOOLEAN)
  public void requestFocus(final T view, boolean request) {
    if(LogUtils.isDebug() && request){
      Log.w(FocusDispatchView.TAG,"requestFocus called !!!! view:"+ExtendUtil.debugViewLite(view));
    }
    LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":requestFocus "+request+" view is :"+ExtendUtil.debugViewLite(view));
    if (request) {
      //noinspection AccessStaticViaInstance
      Looper.getMainLooper().myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
        @Override
        public boolean queueIdle() {
          bUserChageFocus = true;
          boolean result = view.requestFocusFromTouch();

          if (!result) {
            result = view.requestFocus();
            LogUtils.d("requestFocus", "requestFocus result:" + result);
          }
          //Zhaopeng 20210421 解决9.0上有时候焦点请求慢的问题。
//          view.requestFocus();
          bUserChageFocus = false;
          return false;
        }
      });
    }

  }

  /*2.5 add**/
  @HippyControllerProps(name = "fadingEdgeLength", defaultType = HippyControllerProps.NUMBER)
  public void setFadingEdgeLength(View view,int length) {
    view.setFadingEdgeLength(Utils.toPX(length));
  }

    @HippyControllerProps(name = "skipRequestFocus", defaultType = HippyControllerProps.BOOLEAN)
  public void setSkipRequestFocus(View view, Boolean b) {
      LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":skipRequestFocus "+b+" view is :"+ExtendUtil.debugViewLite(view));
      if(view instanceof ITVView){
        ((ITVView) view).setSkipRequestFocus(b);
      }
      if(view instanceof ExtendViewGroup){
        ((ExtendViewGroup) view).setSkipRequestFocus(b);
      }
  }

  @HippyControllerProps(name = "fillParent", defaultType = HippyControllerProps.BOOLEAN)
  public void setFillParentEnabled(View view, Boolean fillParent) {
    if(view instanceof ITVView){
      ((ITVView) view).setFillParent(fillParent);
    }
  }

  /*2.5 add**/
  @HippyControllerProps(name = "horizontalFadingEdgeEnabled", defaultType = HippyControllerProps.BOOLEAN)
  public void setHorizontalFadingEdgeEnabled(View view,boolean horizontalFadingEdgeEnabled) {
    view.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled);
  }
  /*2.5 add**/
  @HippyControllerProps(name = "verticalFadingEdgeEnabled", defaultType = HippyControllerProps.BOOLEAN)
  public void setVerticalFadingEdgeEnabled(View view,boolean verticalFadingEdgeEnabled) {
    view.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
  }

  @HippyControllerProps(name = "requestFocusFromTouchDirectly", defaultType = HippyControllerProps.BOOLEAN)
  public void requestFocusFromTouch(final T view, final boolean request) {
    LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":requestFocusFromTouchDirectly request "+request+" view is :"+ExtendUtil.debugViewLite(view));
    if (request) {

      bUserChageFocus = true;
      boolean result = view.requestFocusFromTouch();

      if (!result) {
        result = view.requestFocus();
        LogUtils.d("requestFocus", "requestFocusFromTouch result:" + result);
      }
      if(LogUtils.isDebug()) {
        LogUtils.e("requestFocus", "requestFocusFromTouch view:" + view.getId());
      }
    }

  }

  @HippyControllerProps(name = "requestFocusDirectly", defaultType = HippyControllerProps.BOOLEAN)
  public void requestFocusDirectly(final T view, final boolean request) {
    LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":requestFocusDirectly request "+request+" view is :"+ExtendUtil.debugViewLite(view));
    if (request) {
      view.requestFocus();
      if(LogUtils.isDebug()) {
        LogUtils.e(FocusDispatchView.TAG, "requestFocusDirectly view:" + view.getId());
      }
    }
  }
//  @Deprecated
//  //list controll属性
//  @HippyControllerProps(name = "autoFocusID", defaultType = HippyControllerProps.STRING)
//  public void setAutoFocus(final T view, final String  autoFocusID) {
//    final View rootView = HippyViewGroup.findPageRootView(view);
//    if(rootView instanceof HippyViewGroup){
//      ((HippyViewGroup) rootView).getAutoFocusManager().setAppearFocusTag(autoFocusID,0);
//    }else{
//      //final View rootView = HippyViewGroup.findPageRootView(view);
//      Log.e(AutoFocusManager.TAG,"setAutoFocus called in controller error rootView is :"+rootView);
//    }
//  }

  @Deprecated
  @HippyControllerProps(name = "autoFocus", defaultType = HippyControllerProps.STRING)
  public void setFocusSID(final T view, final String  autoFocusID) {
    LogUtils.i(AutoFocusManager.TAG,"set autoFocus by autoFocusID : "+autoFocusID+",view:"+view);
    final View rootView = HippyViewGroup.findPageRootView(view);
    if(rootView instanceof HippyViewGroup){
      ((HippyViewGroup) rootView).getAutoFocusManager().setGlobalAutofocusSID(autoFocusID,0);
    }else{
      //final View rootView = HippyViewGroup.findPageRootView(view);
      Log.w(AutoFocusManager.TAG,"setAutoFocus 1 called in controller error rootView is :"+rootView);
    }
  }

  @HippyControllerProps(name = "autofocusSID", defaultType = HippyControllerProps.STRING)
  public void setAutoFocusSID(final T view, final String  autoFocusID) {
    if(TemplateCodeParser.isPendingProForce(autoFocusID)){
      return;
    }
    final View rootView = HippyViewGroup.findPageRootView(view);
    LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":autofocusSID "+autoFocusID+" view is :"+ExtendUtil.debugViewLite(view));
    if(rootView instanceof HippyViewGroup){
      ((HippyViewGroup) rootView).getAutoFocusManager().setGlobalAutofocusSID(autoFocusID,0);
    }else{
      //final View rootView = HippyViewGroup.findPageRootView(view);
      Log.w(AutoFocusManager.TAG,"setAutofocusSID called in controller error rootView is :"+rootView);
    }
  }


  //list controll属性
  @HippyControllerProps(name = "autofocus", defaultType = HippyControllerProps.BOOLEAN , defaultBoolean = true)
  public void setAutoFocus(final T view, final boolean  autoFocus) {
    LogUtils.i(AutoFocusManager.TAG,"set autoFocus by bool : "+autoFocus+",view:"+view);
    if (view instanceof ITVView) {
      ((ITVView) view).setAutoFocus(autoFocus,false);
      LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":setAutoFocus "+autoFocus+" view is :"+ExtendUtil.debugViewLite(view));
    }
  }

  //list controll属性
  @HippyControllerProps(name = "autofocusForce", defaultType = HippyControllerProps.BOOLEAN ,defaultBoolean = true)
  public void setAutoFocusForce(final T view, final boolean  autoFocus) {
    LogUtils.i(AutoFocusManager.TAG,"set autoFocus by bool : "+autoFocus+",view:"+view);
    if (view instanceof ITVView) {
      ((ITVView) view).setAutoFocus(autoFocus,true);
      LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,":setAutoFocusForce "+autoFocus+" view is :"+ExtendUtil.debugViewLite(view));
    }
  }

  @Override
  public void onFocusChange(final View v, boolean hasFocus) {
      HippyMap hippyMap = new HippyMap();
      hippyMap.pushBoolean("isFocused", hasFocus);
      hippyMap.pushString("sid", ExtendUtil.getViewSID(v));
      if(mFocusEvent == null){
        mFocusEvent = new HippyViewEvent("onFocus");
      }
      if(v instanceof TriggerTaskHost){
        if(hasFocus) {
          TriggerTaskManagerModule.dispatchTriggerTask((TriggerTaskHost) v,"onFocused" );
        }else{
          TriggerTaskManagerModule.dispatchTriggerTask((TriggerTaskHost) v,"unFocused" );
        }
      }
      mFocusEvent.send(v, hippyMap);

    }


  @HippyControllerProps(name = NodeProps.LINEAR_GRADIENT, defaultType = HippyControllerProps.MAP)
  public void setLinearGradient(T view, HippyMap linearGradient) {
    if (linearGradient != null && view instanceof IGradient) {
      String angle = linearGradient.getString("angle");
      HippyArray colorStopList = linearGradient.getArray("colorStopList");

      if (TextUtils.isEmpty(angle) || colorStopList == null || colorStopList.size() == 0) {
        return;
      }

      int size = colorStopList.size();
      ArrayList<Integer> colorsArray = new ArrayList<>();
      ArrayList<Float> positionsArray = new ArrayList<>();
      for(int i = 0; i < size; i++){
        HippyMap colorStop = colorStopList.getMap(i);
        if (colorStop == null) {
          continue;
        }

        int color = colorStop.getInt("color");
        colorsArray.add(color);

        float ratio = 0.0f;
        if (colorStop.containsKey("ratio")) {
          ratio = (float)colorStop.getDouble("ratio");
        } else if(i == (size - 1)) {
          ratio = 1.0f;
        }

        positionsArray.add(ratio);
      }

      ((IGradient)view).setGradientAngle(angle);
      ((IGradient)view).setGradientColors(colorsArray);
      ((IGradient)view).setGradientPositions(positionsArray);
    }
  }

  @HippyControllerProps(name = NodeProps.SHADOW_OFFSET, defaultType = HippyControllerProps.MAP)
  public void setShadowOffset(T view, HippyMap shadowOffset) {
    if (shadowOffset != null && view instanceof IShadow) {
      float shadowOffsetX = shadowOffset.getInt("x");
      float shadowOffsetY = shadowOffset.getInt("y");
      ((IShadow) view).setShadowOffsetX(shadowOffsetX);
      ((IShadow) view).setShadowOffsetY(shadowOffsetY);
    }
  }

  @HippyControllerProps(name = NodeProps.SHADOW_OFFSET_X, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setShadowOffsetX(T view, float shadowOffsetX) {
    if (view instanceof IShadow)
      ((IShadow) view).setShadowOffsetX(shadowOffsetX);
  }

  @HippyControllerProps(name = NodeProps.SHADOW_OFFSET_Y, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setShadowOffsetY(T view, float shadowOffsetY) {
    if (view instanceof IShadow)
      ((IShadow) view).setShadowOffsetY(shadowOffsetY);
  }

  @HippyControllerProps(name = NodeProps.SHADOW_OPACITY, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setShadowOpacity(T view, float shadowOpacity) {
    if (view instanceof IShadow) {
      ((IShadow) view).setShadowOpacity(shadowOpacity);
    }
  }

  @HippyControllerProps(name = NodeProps.SHADOW_RADIUS, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setShadowRadius(T view, float shadowRadius) {
    if (view instanceof IShadow) {
      ((IShadow) view).setShadowRadius(shadowRadius);
    }
  }

  @HippyControllerProps(name = NodeProps.SHADOW_SPREAD, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setShadowSpread(T view, float shadowSpread) {
    if (view instanceof IShadow) {
      ((IShadow) view).setShadowSpread(shadowSpread);
    }
  }

  @HippyControllerProps(name = NodeProps.SHADOW_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setShadowColor(T view, int shadowColor) {
    if (view instanceof IShadow) {
      ((IShadow) view).setShadowColor(shadowColor);
    }
  }

  @HippyControllerProps(name = NodeProps.BORDER_LEFT_WIDTH, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setLeftBorderWidth(T view, float borderLeftWidth) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderWidth(borderLeftWidth, CommonBorder.BorderWidthDirection.LEFT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_TOP_WIDTH, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setTopBorderWidth(T view, float borderTopWidth) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderWidth(borderTopWidth, CommonBorder.BorderWidthDirection.TOP.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_RIGHT_WIDTH, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setRightBorderWidth(T view, float borderRightWidth) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderWidth(borderRightWidth, CommonBorder.BorderWidthDirection.RIGHT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_BOTTOM_WIDTH, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setBottomBorderWidth(T view, float borderBottomWidth) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderWidth(borderBottomWidth, CommonBorder.BorderWidthDirection.BOTTOM.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBorderColor(T view, int borderColor) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderColor(borderColor, CommonBorder.BorderWidthDirection.ALL.ordinal());
    }
  }

  @HippyControllerProps(name = NodeProps.BORDER_LEFT_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBorderLeftColor(T view, int borderLeftColor) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderColor(borderLeftColor, CommonBorder.BorderWidthDirection.LEFT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_TOP_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBorderTopWidth(T view, int borderTopColor) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderColor(borderTopColor, CommonBorder.BorderWidthDirection.TOP.ordinal());
    }
  }

  @HippyControllerProps(name = NodeProps.BORDER_RIGHT_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBorderRightWidth(T view, int borderRightColor) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderColor(borderRightColor, CommonBorder.BorderWidthDirection.RIGHT.ordinal());
    }
  }


  @HippyControllerProps(name = NodeProps.BORDER_BOTTOM_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBorderBottomWidth(T view, int borderBottomColor) {
    if (view instanceof CommonBorder) {
      ((CommonBorder) view)
          .setBorderColor(borderBottomColor, CommonBorder.BorderWidthDirection.BOTTOM.ordinal());
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.ON_CLICK, defaultType = HippyControllerProps.BOOLEAN)
  public void setClickable(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      if (flag) {
        view.setOnClickListener(NativeGestureDispatcher.getOnClickListener());
      } else {
        view.setOnClickListener(null);
        view.setClickable(false);
      }
    }
  }


  @HippyControllerProps(name = NodeProps.ON_LONG_CLICK, defaultType = HippyControllerProps.BOOLEAN)
  public void setLongClickable(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      if (flag) {
        view.setOnLongClickListener(NativeGestureDispatcher.getOnLongClickListener());
      } else {
        view.setOnLongClickListener(null);
        view.setLongClickable(false);
      }
    }
  }

  @HippyControllerProps(name = NodeProps.ON_PRESS_IN, defaultType = HippyControllerProps.BOOLEAN)
  public void setPressInable(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      setGestureType(view, NodeProps.ON_PRESS_IN, flag);
    }
  }

  @HippyControllerProps(name = NodeProps.ON_PRESS_OUT, defaultType = HippyControllerProps.BOOLEAN)
  public void setPressOutable(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      setGestureType(view, NodeProps.ON_PRESS_OUT, flag);
    }
  }

  @HippyControllerProps(name = NodeProps.ON_TOUCH_DOWN, defaultType = HippyControllerProps.BOOLEAN)
  public void setTouchDownHandle(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      setGestureType(view, NodeProps.ON_TOUCH_DOWN, flag);
    }
  }

  @HippyControllerProps(name = NodeProps.ON_TOUCH_MOVE, defaultType = HippyControllerProps.BOOLEAN)
  public void setTouchMoveHandle(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      setGestureType(view, NodeProps.ON_TOUCH_MOVE, flag);
    }
  }

  @HippyControllerProps(name = NodeProps.ON_TOUCH_END, defaultType = HippyControllerProps.BOOLEAN)
  public void setTouchEndHandle(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      setGestureType(view, NodeProps.ON_TOUCH_END, flag);
    }
  }

  @HippyControllerProps(name = NodeProps.ON_TOUCH_CANCEL, defaultType = HippyControllerProps.BOOLEAN)
  public void setTouchCancelHandle(T view, boolean flag) {
    if (!handleGestureBySelf()) {
      setGestureType(view, NodeProps.ON_TOUCH_CANCEL, flag);
    }
  }

  @HippyControllerProps(name = NodeProps.ON_ATTACHED_TO_WINDOW, defaultType = HippyControllerProps.BOOLEAN)
  public void setAttachedToWindowHandle(T view, boolean flag) {
    if (flag) {
      view.addOnAttachStateChangeListener(NativeGestureDispatcher.getOnAttachedToWindowListener());
    } else {
      view.removeOnAttachStateChangeListener(
          NativeGestureDispatcher.getOnAttachedToWindowListener());
    }
  }

  @HippyControllerProps(name = NodeProps.ON_DETACHED_FROM_WINDOW, defaultType = HippyControllerProps.BOOLEAN)
  public void setDetachedFromWindowHandle(T view, boolean flag) {
    if (flag) {
      view.addOnAttachStateChangeListener(
          NativeGestureDispatcher.getOnDetachedFromWindowListener());
    } else {
      view.removeOnAttachStateChangeListener(
          NativeGestureDispatcher.getOnDetachedFromWindowListener());
    }
  }

  @HippyControllerProps(name = "renderToHardwareTextureAndroid", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setRenderToHardwareTexture(T view, boolean useHWTexture) {
    view.setLayerType(useHWTexture ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, null);
  }

  @SuppressWarnings("EmptyMethod")
  @HippyControllerProps(name = NodeProps.CUSTOM_PROP)
  public void setCustomProp(T view, String methodName, Object props) {

  }

  protected void setGestureType(T view, String type, boolean flag) {
    // add by weipeng
    // 自定义的组件为实现HippyViewBase接口，需要判断下
    if(!(view instanceof HippyViewBase)) return;
    if (flag) {
      if (view.getGestureDispatcher() == null) {
        view.setGestureDispatcher(new NativeGestureDispatcher(view));
      }
      view.getGestureDispatcher().addGestureType(type);
    } else {
      if (view.getGestureDispatcher() != null) {
        view.getGestureDispatcher().removeGestureType(type);
      }
    }
  }

  public RenderNode createRenderNode(int id, HippyMap props, String className,
      HippyRootView hippyRootView, ControllerManager controllerManager,
      boolean lazy) {
    return new RenderNode(id, props, className, hippyRootView, controllerManager, lazy);
  }

  private void applyTransform(T view, HippyArray transformArray) {
    TransformUtil.processTransform(transformArray, sTransformDecompositionArray);
    sMatrixDecompositionContext.reset();
    MatrixUtil.decomposeMatrix(sTransformDecompositionArray, sMatrixDecompositionContext);
    view.setTranslationX(PixelUtil.dp2px((float) sMatrixDecompositionContext.translation[0]));
    view.setTranslationY(PixelUtil.dp2px((float) sMatrixDecompositionContext.translation[1]));
    view.setRotation((float) sMatrixDecompositionContext.rotationDegrees[2]);
    view.setRotationX((float) sMatrixDecompositionContext.rotationDegrees[0]);
    view.setRotationY((float) sMatrixDecompositionContext.rotationDegrees[1]);
    view.setScaleX((float) sMatrixDecompositionContext.scale[0]);
    view.setScaleY((float) sMatrixDecompositionContext.scale[1]);
  }

  public static void resetTransform(View view) {
    view.setTranslationX(0);
    view.setTranslationY(0);
    view.setRotation(0);
    view.setRotationX(0);
    view.setRotationY(0);
    view.setScaleX(1);
    view.setScaleY(1);
  }

  /***
   * dispatch the js call UI Function.
   * @see #dispatchFunction(View, String, HippyArray, Promise)
   * @param view
   * @param functionName
   * @param var
   */
  public void dispatchFunction(T view, String functionName, HippyArray var) {
//    if("dispatchFunctionBySid".equals(functionName)){
//      Log.e(ReplaceChildView.TAG,"native call dispatchFunctionBySid var"+var);
//    }
    if(LogUtils.isDebug()){
      Log.d(TAG,"dispatchFunction functionName:"+functionName+",var :"+var+",callView:"+ExtendUtil.debugViewLite(view));
      //Log.d(FastListView.TAG_CLONED,"dispatchFunction functionName:"+functionName+",var :"+var+",view:"+view);
    }
    if(FastListViewController.dispatchFunctionByTemplate(this,view,functionName,var,null)){
//      Log.e(ReplaceChildView.TAG,"native call dispatchFunctionBySid return on dispatchFunctionByTemplate");
      return;
    }
    switch (functionName)
    {
      case "requestFocus":
        if(LogUtils.isDebug()){
          Log.e(FocusDispatchView.TAG,"requestFocus by dispatchFunction view:"+view.getId());
        }
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">requestFocus view is :"+ExtendUtil.debugViewLite(view));
        if(var.size() > 0){
          int direction = var.getInt(0);
          view.requestFocus(direction);
        }else{
          requestFocus(view,true);
        }
        break;

      case "requestFocusDirectly":
        if(LogUtils.isDebug()){
          Log.e(FocusDispatchView.TAG,"requestFocusDirectly by dispatchFunction view:"+view.getId());
        }
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">requestFocusDirectly view is :"+ExtendUtil.debugViewLite(view));
        if(var.size() > 0){
          int direction = var.getInt(0);
          view.requestFocus(direction);
        }else{
          view.requestFocus();
        }
        break;
      case "requestRootLayout":
          LogUtils.d("hippy","requestRootLayout called by  :"+view.getId());
          if(view.getRootView() != null){
            view.getRootView().requestLayout();
          }
        break;
      case "requestLayout":
        LogUtils.d("hippy","requestLayout called by  :"+view.getId());
        view.requestLayout();
        break;
      case "setDescendantFocusability" :
        if(view instanceof ViewGroup) {
          if (var.size() > 0) {
            int focusAbility = var.getInt(0);
            LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">setDescendantFocusability "+focusAbility+"view is :"+ExtendUtil.debugViewLite(view));
            Log.d("hippy","setDescendantFocusability :"+focusAbility);
            ((ViewGroup) view).setDescendantFocusability(focusAbility);
          }
        }
        break;

      case "changeDescendantFocusability" :
        if(view instanceof ViewGroup) {
          if (var.size() > 0) {
            String focusAbility = var.getString(0);
            LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">changeDescendantFocusability "+focusAbility+"view is :"+ExtendUtil.debugViewLite(view));
            int focusAbilityInt = ViewGroup.FOCUS_AFTER_DESCENDANTS;
            switch (focusAbility){
              case "beforeDescendants":
                ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                break;
              case "blockDescendants":
                ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                break;
              default:
                ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                break;
            }
            LogUtils.d(FocusDispatchView.TAG,"changeDescendantFocusability request focusAbility:"+focusAbility+" ,result :"+focusAbilityInt+", view:"+view);
          }
        }
        break;
      case "forceUpdateRenderNode" :
        if(view != null ){
          Log.d("viewController","forceUpdateRenderNode called,id:"+view.getId());
            final Context context = view.getContext();
            if(context instanceof HippyInstanceContext){
              final HippyEngineContext hippyContext = ((HippyInstanceContext) context).getEngineContext();
              if(context != null){
                final RenderNode node = hippyContext.getRenderManager().getRenderNode(view.getId());
                if(node != null && !node.isDelete()){
                  node.updateViewLayoutRecursive();
                }
              }
            }
          }
        break;
      case "setBackGroundColor":
        if (view != null) {
          try {
            setBackgroundString(view, var.getString(0));
            view.invalidate();
          }catch (Exception e){
            if (LogUtils.isDebug()) {
              e.printStackTrace();
            }
          }
        }
        break;
      case "layoutViewManual":
        if(view != null){
          ExtendUtil.layoutViewManual(view);
        }
        break;
      case "blockRootFocus":
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">blockRootFocus view is :"+ExtendUtil.debugViewLite(view));
        InternalExtendViewUtil.blockRootFocus(view);
        break;
      case "clearFocus":
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">clearFocus view is :"+ExtendUtil.debugViewLite(view));
        InternalExtendViewUtil.clearFocus(view);
        break;
      case "unBlockRootFocus":
        if(LogUtils.isDebug()){
          LogUtils.e(FocusDispatchView.TAG,"unBlockRootFocus by ViewController view:"+view.getId());
        }
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">unBlockRootFocus view is :"+ExtendUtil.debugViewLite(view));
        InternalExtendViewUtil.unBlockRootFocus(view);
        break;
      case "changeVisibility":
        final String visi = var.getString(0);
       switch (visi){
         case "visible":
           view.setVisibility(View.VISIBLE);
           break;
         case "invisible":
           view.setVisibility(View.INVISIBLE);
           break;
         case "gone":
           view.setVisibility(View.GONE);
           break;
       }
       break;
      case "changeAlpha":
        final double alpha = var.getDouble(0);
        view.setAlpha((float)alpha);
        break;
      case "setScale":
        final double scaleX = var.getDouble(0);
        final double scaleY = var.getDouble(1);
        final int duration = var.getInt(2);
        if(duration <= 0){
          view.setScaleX((float)scaleX);
          view.setScaleY((float)scaleY);
          view.invalidate();
        }else {
          TVFocusAnimHelper.bounceScaleTo(view, (float)scaleX, (float)scaleY, duration);
        }
        break;
      case "setPosition":
        final int px = var.getInt(0);
        final int py = var.getInt(1);
        final int pz = var.getInt(2);
        view.setX(Utils.toPX(px));
        view.setY(Utils.toPX(py));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          view.setZ(Utils.toPX(pz));
        }
        break;
      case "updateLayout":
        if (view != null) {
          final int width = Utils.toPX(var.getInt(0));
          final int height = Utils.toPX(var.getInt(1));
          final int x = Utils.toPX(var.getInt(2));
          final int y = Utils.toPX(var.getInt(3));
          if (var.size() >= 5 && var.getBoolean(4) && view.getId() != -1) {
            RenderUtil.updateDomLayout(x, y, width, height, view);
          }else{
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            if (!shouldInterceptLayout(view, x, y, width, height)) {
              view.layout(x, y, x + width, y + height);
            }
          }
        }
        break;
      case "invalidate":
        view.invalidate();
        break;
      case "changeProgress":
        if(view instanceof ProgressBar){
          final ProgressBar pb = (ProgressBar) view;
          final int progress = var.getInt(0);
          final int secondProgress = var.getInt(1);
          final int max = var.getInt(2);
          pb.setProgress(progress);
          pb.setSecondaryProgress(secondProgress);
          pb.setMax(max);
        }
        break;
      case "dispatchFunctionForTarget":
        final String targetName = var.getString(0);
        final String functionTargetName = var.getString(1);
        final View root = InternalExtendViewUtil.getRootView(view);
        if(root != null){
          final View target = ControllerManager.findViewByName(root,targetName);
          if(target != null && functionTargetName != null && !"dispatchFunctionForTarget".equals(functionTargetName)){
            final HippyArray array = var.getArray(2);
            int delay = 0;
            if(var.size() > 3){
              delay = var.getInt(3);
            }
            final Context context = view.getContext();
            if(context instanceof HippyInstanceContext){
              final HippyEngineContext hippyContext = ((HippyInstanceContext) context).getEngineContext();
              final RenderNode node = hippyContext.getRenderManager().getRenderNode(view.getId());
              final String className = node.getClassName();
              if(LogUtils.isDebug()) {
                Log.v("hippy", "dispatchFunctionForTarget view:" + view.getId() + ",className:"+className+",functionTargetName:"+functionTargetName+",array :"+array+",delay:"+delay);
              }
              hippyContext.getRenderManager().getControllerManager().dispatchUIFunction(view.getId(),className,functionTargetName,array,delay);
            }
          }
        }
        break;

      case "setBlockFocusDirections":
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">setBlockFocusDirections "+var.getArray(0)+" view is :"+ExtendUtil.debugViewLite(view));
        this.setBlockFocusDirectionsV2(view,var.getArray(0));
        break;
      case "setBlockFocusDirectionsOnFail":
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">setBlockFocusDirectionsOnFail "+var.getArray(0)+" view is :"+ExtendUtil.debugViewLite(view));
        this.setBlockFocusDirectionsOnFail(view,var.getArray(0));
        break;
      case "dispatchTVItemFunction":
        this.dispatchTVItemFunction(view,var,null);
        break;
        //2.6
      case "setInitFocus":
      case "setAutoFocus":
        final View rootView = HippyViewGroup.findPageRootView(view);
        Log.i(AutoFocusManager.TAG,"setAutoFocus called rootView is :"+rootView+",var:"+var);
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">setAutoFocus params"+var+" view is :"+ExtendUtil.debugViewLite(view));
        if(rootView instanceof HippyViewGroup){
          int delay = var.size() == 2 ? var.getInt(1) : -1;
          if(delay > 0 && view.getId() != -1){
            ((HippyViewGroup) rootView).getAutoFocusManager().setGlobalAutofocusSID(var.getString(0), delay,view);
          }else {
            ((HippyViewGroup) rootView).getAutoFocusManager().setGlobalAutofocusSID(var.getString(0), var.size() == 2 ? var.getInt(1) : -1);
          }
        }else{
          //final View rootView = HippyViewGroup.findPageRootView(view);
          Log.e(AutoFocusManager.TAG,"setAutoFocus called error rootView is :"+rootView);
        }
        break;
      case "setAutofocus":
        boolean autofocus = var.getBoolean(0);
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">setAutofocus "+autofocus+" view is :"+ExtendUtil.debugViewLite(view));
        if (view instanceof ITVView) {
          ((ITVView) view).setAutoFocus(autofocus,true);
        }
        break;
      case "dispatchFunctionBySid":
        dispatchFunctionBySid(view,var,null);
        break;
      case "requestAutofocus":
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,">requestAutofocus  view is :"+ExtendUtil.debugViewLite(view));
        AutoFocusManager.requestAutofocusTraverse(view);
        break;
    }
  }

  private void dispatchTVItemFunction(T view,HippyArray var, Promise promise){
      final String id = var.getString(0);
      final String name = var.getString(1);
      final String functionName = var.getString(2);
      final HippyArray params = var.getArray(3);
      FastItemView templateItemView = FastItemView.findTemplateItemView(view);
      if (templateItemView == null) {
        Log.e(TAG,"dispatchTVItemFunction error on templateItemView == null id:"+id+",name:"+name+",functionName:"+functionName);
        return;
      }
      View rootView;
      rootView = InternalExtendViewUtil.findRootFromContext(view);
      if(rootView == null) {
        View parentView = templateItemView.getParentListView();
        if (parentView == null) {
          parentView = templateItemView.getParentFlexView();
        }
        rootView = HippyViewGroup.findPageRootView(parentView);
        if (rootView == null) {
          rootView = FocusDispatchView.findRootView(parentView);
        }
      }
      final FastItemView itemView = FastAdapter.findTVItemViewById(rootView,id);
//      if(LogUtils.isDebug()){
//        FastAdapter.logTVItemViewById(rootView,id);
//      }
      final View targetView = ControllerManager.findViewByName(itemView,name);
      if(LogUtils.isDebug()) {
        Log.i(TAG, "dispatchTVItemFunction id:" + id + ",name:" + name + ",functionName:" + functionName + ",params:" + params);
        Log.i(TAG,"dispatchTVItemFunction itemView:"+itemView+",targetView:"+targetView+",rootView:"+rootView);
      }
      if (targetView != null) {
        if (promise != null) {
          this.dispatchFunction((T) targetView,functionName,params,promise);
        }else{
          this.dispatchFunction((T) targetView,functionName,params);
        }
      }
  }

  private void setBlockFocusDirectionsV2(T view,HippyArray array){
    if(view == null){
      Log.e(TAG,"setBlockFocusDirectionsOnFail error array is null");
      return;
    }
    if(view instanceof TVSingleLineListView) {
      if(array == null || array.size() == 0){
        ((TVSingleLineListView) view).setBlockFocusOn(null);
        return;
      }
      int[] directions = new int[array.size()];
      for(int i = 0; i < array.size(); i ++){
        switch (array.getString(i)){
          case "up":
            directions[i] = View.FOCUS_UP;
            break;
          case "down":
            directions[i] = View.FOCUS_DOWN;
            break;
          case "left":
            directions[i] = View.FOCUS_LEFT;
            break;
          case "right":
            directions[i] = View.FOCUS_RIGHT;
            break;
          case "all":
            ((TVSingleLineListView) view).setBlockFocusOn(new int[]{View.FOCUS_UP,View.FOCUS_DOWN,View.FOCUS_LEFT,View.FOCUS_RIGHT});
            return;
        }
      }
      ((TVSingleLineListView) view).setBlockFocusOn(directions);
    }else {
      int nextID = view.getId();
      if(array == null || array.size() == 0){
        nextID = View.NO_ID;
        view.setNextFocusUpId(nextID);
        view.setNextFocusDownId(nextID);
        view.setNextFocusLeftId(nextID);
        view.setNextFocusRightId(nextID);
        return;
      }
      for(int i = 0; i < array.size(); i ++){
        switch (array.getString(i)){
          case "up":
            view.setNextFocusUpId(nextID);
            break;
          case "down":
            view.setNextFocusDownId(nextID);
            break;
          case "left":
            view.setNextFocusLeftId(nextID);
            break;
          case "right":
            view.setNextFocusRightId(nextID);
            break;
          case "all":
            view.setNextFocusUpId(nextID);
            view.setNextFocusDownId(nextID);
            view.setNextFocusLeftId(nextID);
            view.setNextFocusRightId(nextID);
            break;
        }
      }
    }
  }

  public void dispatchFunctionBySid(@Nullable View view, HippyArray var, Promise promise) {
    dispatchFunctionBySid(null, view, null, var.getString(0), var.getString(1), var.getArray(2), promise,TAG,false);
  }

  /**
   * 通过一个view的sid来执行对应的方法
   * @param rootView 执行方法的根view
   * @param view vue上层传递过来调用此方法的view
   * @param instanceContext instanceContext
   * @param targetSID 执行的目标sid
   * @param functionTargetName 方法名
   * @param params 传递过来的参数
   * @param promise 回调
   */
  public static void dispatchFunctionBySid(@Nullable View rootView, @Nullable View view,@Nullable HippyInstanceContext instanceContext,String targetSID,String functionTargetName,HippyArray params,@Nullable Promise promise,String TAG,boolean checkValid){
    try {
//      final String targetSID = var.getString(0);
//      final String functionTargetName = var.getString(1);
      if (rootView == null && view == null) {
        Log.e(TAG,"dispatchFunctionBySid error on rootView and view both are null");
        if(promise != null){
          promise.reject("dispatchFunctionBySid error on rootView and view both are null");
        }
        return ;
      }
      final View root = rootView == null  ? HippyViewGroup.findPageRootView(view) : rootView;
//        Log.e(ReplaceChildView.TAG, "dispatchFunctionBySid targetSID :" + targetSID + ",functionTargetName:" + functionTargetName +",root:"+root);
      Promise takeOverPromise = promise == null ? null : new FastListViewController.PromiseTakeover(promise, targetSID);
      if (root != null) {

       // ExtendUtil.logView(ReplaceChildView.TAG,root);

        final View target = ExtendUtil.findViewBySID(targetSID, root,checkValid);
        if (target != null && functionTargetName != null && !"dispatchFunctionBySid".equals(functionTargetName)) {
//          final HippyArray array = var.getArray(2);
          Context context = null;
          if(instanceContext != null){
            context = instanceContext;
          }else{
            if (rootView != null) {
              context = rootView.getContext();
            } else {
              context = view.getContext();
            }
          }
//            instanceContext  == null && view != null ? view.getContext() : instanceContext;
          if (context instanceof HippyInstanceContext) {
            final HippyEngineContext hippyContext = ((HippyInstanceContext) context).getEngineContext();
            final String targetNodeClassName = ExtendTag.obtainExtendTag(target).nodeClassName;
            String className = targetNodeClassName;
            boolean executeOnThisView = false;
            if(view != null && targetSID != null && targetSID.equals(ExtendUtil.getViewSID(view))){
              executeOnThisView = true;
            }
            //Log.i(TAG,"dispatchFunctionBySid executeOnThisView : "+executeOnThisView+",sid:"+targetSID+",dispatch view:"+ExtendUtil.debugViewLite(view));
            if(TextUtils.isEmpty(targetNodeClassName) && view != null && executeOnThisView){
              final RenderNode node = hippyContext.getRenderManager().getRenderNode(view.getId());
              className = node.getClassName();
            }
            if (view != null && view.getId() != -1 && executeOnThisView) {
              final RenderNode node = hippyContext.getRenderManager().getRenderNode(view.getId());
//              Log.i(TAG,"dispatchFunctionBySid  node class:"+className+",dispatchUIFunction");
              node.dispatchUIFunction(functionTargetName,params,takeOverPromise);
            } else if (target.getId() != -1) {
              final RenderNode node = hippyContext.getRenderManager().getRenderNode(target.getId());
//              Log.i(TAG,"dispatchFunctionBySid  node class:"+className+",dispatchUIFunction");
              node.dispatchUIFunction(functionTargetName,params,takeOverPromise);
            }
            //final String className = TextUtils.isEmpty(targetNodeClassName) ? node.getClassName() : targetNodeClassName;
            Log.e(TAG, "dispatchFunctionBySid execute !!! view:" + ExtendUtil.debugViewLite(target) + ",className:" + className + ",functionTargetName:" + functionTargetName + ",array :" + params);
            if (className != null) {
              CustomControllerHelper.dispatchUIFunction(hippyContext.getRenderManager().mControllerManager, target, className, functionTargetName, params, takeOverPromise);
            }else {
              Log.e(TAG, "dispatchFunctionBySid error className null func:" + functionTargetName+",sid:"+targetSID);
              if (takeOverPromise != null) {
                takeOverPromise.reject("dispatchFunctionBySid error on className is null ");
              }
            }
          }else{
            Log.e(TAG, "dispatchFunctionBySid error on  func:" + functionTargetName+",sid:"+targetSID+",context is not HippyInstanceContext context:"+context);
          }
        } else {
          if (takeOverPromise != null) {
            takeOverPromise.reject("dispatchFunctionBySid error on find target null");
          }
          Log.e(TAG, "dispatchFunctionBySid error on find target null func:" + functionTargetName+",sid:"+targetSID);
        }
      } else {
        if (takeOverPromise != null) {
          takeOverPromise.reject("dispatchFunctionBySid error on find root null");
        }
        Log.e(TAG, "dispatchFunctionBySid error find root null func:" + functionTargetName+",sid:"+targetSID);
      }
    }catch (Exception e){
      Log.e(TAG, "dispatchFunctionBySid fatal error  func:"  + functionTargetName+",sid:"+targetSID);
      e.printStackTrace();
      if (promise != null) {
        promise.reject("dispatchFunctionBySid fatal error");
      }
    }
  }

  /***
   * dispatch the js call UI Function with Promise to call back.
   *
   * @param view view实例
   * @param functionName 函数名
   * @param params 函数参数
   * @param promise 回调
   */
  public void dispatchFunction(T view, String functionName, HippyArray params, Promise promise) {
//    if(LogUtils.isDebug()) {
      Log.d(TAG, "dispatchFunction withPromise:" + functionName + ",data:" + params);
      Log.d(FastListView.TAG_CLONED, "dispatchFunction withPromise:" + functionName + ",data:" + params+",view:"+view);
//    }
    if("dispatchFunctionBySid".equals(functionName)){
      Log.e(ReplaceChildView.TAG,"native call dispatchFunctionBySid with promise var"+params);
    }
    if(FastListViewController.dispatchFunctionByTemplate(this,view,functionName,params,promise)){
      Log.e(ReplaceChildView.TAG,"native call dispatchFunctionBySid return on dispatchFunctionByTemplate");
      return;
    }
    switch (functionName)
    {
      case "hasFocus" :
        if(view != null) {
          promise.resolve(view.hasFocus());
        }
        break;
      case "isFocused" :
        if(view != null) {
          promise.resolve(view.isFocused());
        }
        break;
      case "setBackGroundColor":
        if (view != null) {
          try {
            setBackgroundString(view, params.getString(0));
            view.invalidate();
          }catch (Exception e){
            if (LogUtils.isDebug()) {
              e.printStackTrace();
            }
          }
        }
        break;
      case "getLocationOnScreen":
        int[] outputBuffer = new int[2];
        view.getLocationOnScreen(outputBuffer);
        int width = view.getWidth();
        int height = view.getHeight();
        HippyMap map = new HippyMap();
        map.pushInt("left", outputBuffer[0]);
        map.pushInt("top", outputBuffer[1]);
        map.pushInt("right", outputBuffer[0] + width);
        map.pushInt("bottom", outputBuffer[1] + height);
        map.pushInt("width", width);
        map.pushInt("height", height);
        map.pushDouble("density", PixelUtil.getDensity());

        HippyMap result = new HippyMap();
        result.pushInt("code",0);
        result.pushObject("data", map);

        promise.resolve(result);
        break;
      case "getViewState":
        promise.resolve(ExtendUtil.getViewState(view));
        break;
      case "getChildViewState":
        HippyMap stateMap = null;
        try {
         stateMap = ExtendUtil.getChildState(view, params.getInt(0));
        }catch (Exception e){
          e.printStackTrace();
        }
        if (stateMap != null) {
          stateMap.pushBoolean("valid",true);
        }else{
          stateMap = new HippyMap();
          stateMap.pushBoolean("valid",false);
        }
        promise.resolve(stateMap);
        break;
      case "dispatchTVItemFunction":
        this.dispatchTVItemFunction(view,params,promise);
        break;
      case "dispatchFunctionBySid":
        dispatchFunctionBySid(view,params,promise);
        break;
    }
  }




  /***
   * batch complete
   *
   * @param view
   */
  public void onBatchComplete(T view) {

  }

  protected void deleteChild(ViewGroup parentView, View childView) {
    parentView.removeView(childView);
  }

  protected void deleteChild(ViewGroup parentView, View childView, int childIndex) {
    deleteChild(parentView, childView);
  }

  //zhaopeng add
  public void onBeforeViewDestroy(T t) {
    final String type = findViewCacheType(t);
    if(LogUtils.isDebug()) {
      Log.d(TAG, "onBeforeViewDestroy t :" + t.getId() + ",cacheType:" + type);
    }
    if(!TextUtils.isEmpty(type) && cacheViewsPool != null){
      cacheViewsPool.put(type,t);
    }
  }
  public void onViewDestroy(T t) {
    // add by weipeng
    // 自定义的组件为实现HippyViewBase接口，需要判断下
    if(t instanceof HippyViewBase) t.setGestureDispatcher(null);
  }

  protected void addView(ViewGroup parentView, View view, int index) {
    // TODO: 这里需要复现场景来解，先上一个临时方案
    int realIndex = index;
    if (realIndex > parentView.getChildCount()) {
      realIndex = parentView.getChildCount();
    }
    try {
      parentView.addView(view, realIndex);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void onManageChildComplete(T view) {

  }

  public int getChildCount(T viewGroup) {
    if (viewGroup instanceof ViewGroup) {
      return ((ViewGroup) viewGroup).getChildCount();
    }
    return 0;
  }

  public View getChildAt(T viewGroup, int i) {
    if (viewGroup instanceof ViewGroup) {
      return ((ViewGroup) viewGroup).getChildAt(i);
    }
    return null;
  }

  /** add by zhaopeng 20201110*/
  @HippyControllerProps(name = NodeProps.FOCUS_SCALE, defaultType = HippyControllerProps.NUMBER, defaultNumber = 1.1)
  public void setFocusScale(T view,float focusScale){
    if(view instanceof HippyImageView){
      ((HippyImageView) view).setFocusScale(focusScale);
    }
  }

  /** add by zhaopeng 20201110*/
  @HippyControllerProps(name = NodeProps.CLIP_TO_PADDING, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setClipToPadding(T view,boolean clip){
    if(view instanceof ViewGroup){
      ((ViewGroup) view).setClipToPadding(clip);
    }
  }




  /** add by zhaopeng 20201110*/
  @HippyControllerProps(name = NodeProps.CLIP_CHILDREN, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setClip(T view,boolean clip){
    if(view instanceof ViewGroup){
      ((ViewGroup) view).setClipChildren(clip);
    }
  }

  @HippyControllerProps(name = NodeProps.CLIP_BOUNDS, defaultType = HippyControllerProps.MAP)
  public void setClipBounds(T view, HippyMap hippyMap){
    //int left, int top, int right, int bottom
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
    if(view != null){
        Rect rect = new Rect(hippyMap.getInt("left"),hippyMap.getInt("top"),hippyMap.getInt("right") ,hippyMap.getInt("bottom") );
        view.setClipBounds(rect);
      }
    }else{
      Log.e(TAG,"setClipBounds is'nt support under JELLY_BEAN_MR2");
    }
  }



  @HippyControllerProps(name = NodeProps.LAYOUT_PADDING, defaultType = HippyControllerProps.MAP)
  public void setPadding(T view, HippyMap hippyMap){
    if(view != null && hippyMap!= null){
      if(hippyMap.containsKey("all") && hippyMap.getInt("all") > 0){
        final int all = Utils.toPX( hippyMap.getInt("all"));
        view.setPadding(all,all,all,all);
      }else {
        view.setPadding(hippyMap.containsKey("left") ? Utils.toPX(hippyMap.getInt("left")) : 0,
          hippyMap.containsKey("top") ? Utils.toPX(hippyMap.getInt("top")) : 0,
          hippyMap.containsKey("right") ? Utils.toPX(hippyMap.getInt("right")) : 0,
          hippyMap.containsKey("bottom") ? Utils.toPX(hippyMap.getInt("bottom")) : 0);
      }
    }
  }


  /** add by zhaopeng 20201110*/
  @HippyControllerProps(name = NodeProps.DESCENDANT_FOCUSABILITY, defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setDescendantFocusability(T view,int focusability){
    LogUtils.d(FocusDispatchView.TAG,"setDescendantFocusability :"+focusability+" view:"+view);
    LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,"setDescendantFocusability :"+focusability+" view:"+ExtendUtil.debugViewLite(view));
    if(view instanceof ViewGroup){
      switch (focusability){
        case 0:
          ((ViewGroup) view).setDescendantFocusability( ViewGroup.FOCUS_BEFORE_DESCENDANTS);
          break;
        case 1:
          ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
          break;
        case 2:
          ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
          break;
      }

    }
  }

  @HippyControllerProps(name = NodeProps.NEXT_FOCUS_NAME, defaultType = HippyControllerProps.MAP)
  public void steNextFocusName(T view, HippyMap hippyMap){
    setNextFocusName(view,hippyMap);
  }

 private void setNextFocusName(View view, HippyMap hippyMap){
    if(hippyMap != null){
      final String nextLeft = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_LEFT_FRONT);
      final String nextRight = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_RIGHT_FRONT);
      final String nextUP = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_UP_FRONT);
      final String nextDown = hippyMap.getString(FocusUtils.KEY_NEXT_FOCUS_DOWN_FRONT);
      FocusUtils.setNextFocusName(view,nextLeft,nextRight,nextUP,nextDown);
    }
  }



  @HippyControllerProps(name = "bringFocusChildToFront", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = true)
  public void setBringFocusChildToFront(T view, boolean enable) {
    if(view instanceof HippyViewGroup){
      ((HippyViewGroup) view).setBringFocusChildToFront(enable);
    }else if(view instanceof HippyListView){
      ((HippyListView) view).setBringToFrontOnFocus(enable);
    }
  }

  @HippyControllerProps(name = NodeProps.DUPLICATE_PARENT_STATE, defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setDuplicateParentStateEnabled(T view, boolean enable) {
    view.setDuplicateParentStateEnabled(enable);
  }

  @HippyControllerProps(name = "focusScaleDuplicateParentState", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setFocusScaleOnDuplicateParentState(T view, boolean enable) {
    if (view instanceof HippyImageView) {
      ((HippyImageView) view).setFocusScaleOnDuplicateParentState(enable);
    }
  }

  @HippyControllerProps(name = NodeProps.DUPLICATE_PARENT_STATE)
  public void setDuplicateParentStateEnabled(T view) {
    view.setDuplicateParentStateEnabled(true);
  }


  @HippyControllerProps(name = "dispatchChildFocusEvent", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = true)
  public void setDispatchChildFocusEvent(T view, boolean enable) {
    if(view instanceof ExtendViewGroup){
      ((ExtendViewGroup) view).setDispatchChildFocusEvent(enable);
    }
  }

  @Deprecated
  @HippyControllerProps(name = "blockRootFocus", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setBlockRoot(T view, boolean enable) {
      if(view != null && view.getRootView() instanceof ViewGroup) {
        LogUtils.d(FocusDispatchView.TAG,"blockRootFocus called by view:"+view+",enable:"+enable);
        LogAdapterUtils.log(view.getContext(),FocusDispatchView.TAG,"blockRootFocus called enable:"+enable+" view:"+ExtendUtil.debugViewLite(view));
        ((ViewGroup) view.getRootView()).setDescendantFocusability(enable ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : ViewGroup.FOCUS_BEFORE_DESCENDANTS);
      }
  }

  @HippyControllerProps(name = NodeProps.FOCUS_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setFocusColor(T view, int color) {
    if (view instanceof HippyTextView) {
       ((HippyTextView) view).setFocusColor(color);
    }
    if(view instanceof TVTextView){
      ((TVTextView) view).setFocusColor(color);
    }

  }

  @HippyControllerProps(name = NodeProps.SELECT_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void selectColor(T view, int color) {
    if (view instanceof HippyTextView) {
      ((HippyTextView) view).setSelectColor(color);
    }
    if(view instanceof TVTextView){
      ((TVTextView) view).setSelectColor(color);
    }

  }

  @HippyControllerProps(name = NodeProps.COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setColor(T view, int color) {
    if (view instanceof HippyTextView) {
      ((HippyTextView) view).setCustomColor(color);
    }
    if(view instanceof TVTextView){
      ((TVTextView) view).setTextColor(color);
    }
  }

  //此处vue是怎么传值的
  @Deprecated
  @HippyControllerProps(name = NodeProps.BLOCK_FOCUS_DIRECTIONS, defaultType = HippyControllerProps.ARRAY)
  public void setBlockFocusDirectionsOnFail(T view, HippyArray array){
    if(view == null || array == null){
      Log.e(TAG,"setBlockFocusDirectionsOnFail error array is null");
      return;
    }
    if(view instanceof TVSingleLineListView) {
      int[] directions = new int[array.size()];
      for(int i = 0; i < array.size(); i ++){
        switch (array.getString(i)){
          case "up":
            directions[i] = View.FOCUS_UP;
            break;
          case "down":
            directions[i] = View.FOCUS_DOWN;
            break;
          case "left":
            directions[i] = View.FOCUS_LEFT;
            break;
          case "right":
            directions[i] = View.FOCUS_RIGHT;
            break;
        }
      }
      ((TVSingleLineListView) view).setBlockFocusOnFail(directions);
    }else {
      if(array.size() == 0){
        view.setNextFocusUpId(View.NO_ID);
        view.setNextFocusDownId(View.NO_ID);
        view.setNextFocusLeftId(View.NO_ID);
        view.setNextFocusRightId(View.NO_ID);
        return;
      }
      int nextID = view.getId();
      for(int i = 0; i < array.size(); i ++){
        switch (array.getString(i)){
          case "up":
            view.setNextFocusUpId(nextID);
            break;
          case "down":
            view.setNextFocusDownId(nextID);
            break;
          case "left":
            view.setNextFocusLeftId(nextID);
            break;
          case "right":
            view.setNextFocusRightId(nextID);
            break;
        }
      }
    }
  }

  @HippyControllerProps(name = "selected", defaultType = HippyControllerProps.BOOLEAN)
  public void setSelected(T view, boolean selected) {
    view.setSelected(selected);
  }

  @HippyControllerProps(name = "selectState", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setSelectState(T view, boolean selected) {
    if(view instanceof HippyTextView) {
      ((HippyTextView)view).setSelectState(selected);
    }else if(view instanceof HippyImageView){
      ((HippyImageView)view).setSelectState(selected);
    }
  }


  @HippyControllerProps(name = "layoutAfterAttach", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setRelayoutAfterAttach(T view, boolean relayoutAfterAttach){
    if(view instanceof HippyImageView){
      ((HippyImageView) view).setRelayoutAfterAttach(relayoutAfterAttach);
    }
  }

  @HippyControllerProps(name = "cacheType", defaultType = HippyControllerProps.STRING)
  public void setCacheType(T view, String cacheType){
//    if(view instanceof HippyImageView){
//      ((HippyImageView) view).setRelayoutAfterAttach(relayoutAfterAttach);
//    }
  }

  @HippyControllerProps(name = "cacheMax", defaultType = HippyControllerProps.ARRAY)
  public void setCacheMax(T view,HippyArray cache){
//    if(view instanceof HippyImageView){
//      ((HippyImageView) view).setRelayoutAfterAttach(relayoutAfterAttach);
//    }
    if(cache != null && cacheViewsPool != null){
      cacheViewsPool.setMaxCacheSize(cache.getString(0),cache.getInt(1));
    }
  }

  @HippyControllerProps(name = "recycleOnDetach", defaultType = HippyControllerProps.BOOLEAN)
  public void setRecycleOnDetach(T view, boolean recycleOnDetach) {
    if(view instanceof AsyncImageView){
      ((AsyncImageView) view).setRecycleOnDetach(recycleOnDetach);
    }
  }

  @HippyControllerProps(name = "visible", defaultType = HippyControllerProps.BOOLEAN , defaultBoolean = false)
  public void setVisible(T view, boolean visible) {

    view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
  }

  @HippyControllerProps(name = "visibility", defaultType = HippyControllerProps.STRING )
  public void setVisibility(T view, String visibility) {
    switch (visibility){
      case "visible":
        view.setVisibility(View.VISIBLE);
        break;
      case "invisible":
        view.setVisibility(View.INVISIBLE);
        break;
      case "gone":
        view.setVisibility(View.GONE);
        break;
    }

  }

  @HippyControllerProps(name = "viewLayerType", defaultType = HippyControllerProps.STRING )
  public void setLayerType(T view, String type) {
    switch (type){
      case "hardware":
        view.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        break;
      case "soft":
        view.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        break;
      case "none":
        view.setLayerType(View.LAYER_TYPE_NONE,null);
        break;
    }

  }




  protected String getInnerPath(HippyInstanceContext context, String path) {
    //hpfile://./assets/file_banner02.jpg
    if (path != null && path.startsWith("hpfile://")) {
      String relativePath = path.replace("hpfile://./", "");
      //hippysdk的图片加载协议
      String bundlePath = null;
      if (context.getBundleLoader() != null) {
        bundlePath = context.getBundleLoader().getPath();
      }

      path = bundlePath == null ? null
          : bundlePath.subSequence(0, bundlePath.lastIndexOf(File.separator) + 1) + relativePath;
      //assets://index.android.jsbundle
      //file:sdcard/hippy/feeds/index.android.jsbundle
    }
    return path;
  }

  public static class CacheMap{
    Map<String,CacheViewList> map = new HashMap<>();

    View get(String type){
      if(map.containsKey(type)){
        return map.get(type).get();
      }
      return null;
    }
    public void setMaxCacheSize(String type,int max){
      if(!TextUtils.isEmpty(type)){
        if(map.containsKey(type)){
          map.get(type).setMaxCacheSize(max);
        }else{
          final CacheViewList c = new CacheViewList(type);
          c.setMaxCacheSize(max);
          map.put(type,c);
        }
      }
    }

    void put(String type,View view){
      if(view != null && !TextUtils.isEmpty(type)){
        if(map.containsKey(type)){
          map.get(type).put(view);
        }else{
          final CacheViewList c = new CacheViewList(type);
          c.put(view);
          map.put(type,c);
        }
      }

    }
  }

  @HippyControllerProps(name = "visible", defaultType = HippyControllerProps.BOOLEAN )
  public void setVisibleCompat(T view, boolean b) {
    //zhaopeng 2022 0908 兼容方法，勿删除
    this.setVisible(view,b);
  }

  private static class CacheViewList {
    final String type;
    CacheList list;


    private CacheViewList(String type) {
      this.type = type;
    }


    public void setMaxCacheSize(int max){
      if(list == null){
        list = new CacheList();
      }
      list.max = max;
    }

    void put(View view){
      if(list == null){
        list = new CacheList();
      }
      final boolean add = list.add(view);
      if(add) {
        if(LogUtils.isDebug()) {
          Log.d(TAG, "+++CacheViewList put size:" + list.size() + ",type:" + type);
        }
      }else{
        if(LogUtils.isDebug()) {
          Log.e(TAG, "+++CacheViewList put max items " + ",type:" + type);
        }
      }
    }
    View get(){
      if(list != null && list.size() > 0){
        if(LogUtils.isDebug()) {
          Log.d(TAG, "---CacheViewList popup size:" + list.size() + ",type:" + type);
        }
        return list.get();
      }
      return null;
    }
  }

  public static class CacheList{
    int max = 20;
    List<View> list ;

    boolean add(View view){
      if(list == null){
        list = new ArrayList<>();
      }
      if(size() < max){


        final boolean b =  list.add(view);
        if(b){
          if(view instanceof HippyRecycler){
            ((HippyRecycler) view).onResetBeforeCache();
          }
        }
        return b;
      }else{
        return false;
      }
    }

    View get(){
      if(list != null && list.size() > 0){
        return list.remove(0);
      }
      return null;
    }

    int size(){
      return list.size();
    }

  }

  public static String findViewCacheType(View v){
    if(v.getTag() != null && v.getTag() instanceof HippyMap){
        return ((HippyMap) v.getTag()).getString(HippyTag.TAG_CLASS_CACHE);
    }
    return null;
  }

  /**
   * transform
   **/
  @HippyControllerProps(name = "translation", defaultType = HippyControllerProps.ARRAY)
  public void setTranslate(View view, HippyArray pos) {
    Log.d(TAG,"translation pos:"+pos+",view :"+view);
    if(pos != null){
      final int x = Utils.toPX(pos.getInt(0));
      final int y = Utils.toPX(pos.getInt(1));
      view.layout(x,y,x+view.getWidth(),y+view.getHeight());
    }
  }

  /**
   * transform
   **/
  @HippyControllerProps(name = "size", defaultType = HippyControllerProps.ARRAY)
  public void setSize(View view, HippyArray size) {
    Log.d(TAG,"setSize size:"+size+",view :"+view);
    if(size != null){
      final int x = Utils.toPX((int) view.getX());
      final int y = Utils.toPX((int) view.getY());
      final int width = Utils.toPX(size.getInt(0));
      final int height = Utils.toPX(size.getInt(1));
      view.layout(x,y,x+view.getWidth(),y+view.getHeight());
    }
  }

}
