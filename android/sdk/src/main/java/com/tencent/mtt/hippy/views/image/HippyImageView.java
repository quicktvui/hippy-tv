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
package com.tencent.mtt.hippy.views.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.quicktvui.base.ui.AttachInfo;
import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.hippyext.FocusManagerModule;
import com.quicktvui.base.ui.IFloatFocusManager;
import com.quicktvui.base.ui.ITVView;
import com.quicktvui.base.ui.TVFocusAnimHelper;
import com.quicktvui.base.ui.ExtendTag;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.adapter.image.HippyDrawable;
import com.tencent.mtt.hippy.adapter.image.HippyImageLoader;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.quicktvui.base.ui.StateView;
import com.tencent.mtt.hippy.uimanager.ViewStateProvider;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.UrlUtils;
import com.tencent.mtt.hippy.views.common.CommonBackgroundDrawable;
import com.tencent.mtt.hippy.views.common.CommonBorder;
import com.quicktvui.hippyext.views.fastlist.ListItemHolder;
import com.quicktvui.hippyext.views.fastlist.PostHandlerView;
import com.quicktvui.hippyext.views.fastlist.PostTaskHolder;
import com.tencent.mtt.hippy.views.view.CustomLayoutView;
import com.tencent.mtt.supportui.adapters.image.IDrawableTarget;
import com.tencent.mtt.supportui.views.asyncimage.AsyncImageView;
import com.tencent.mtt.supportui.views.asyncimage.BackgroundDrawable;
import com.tencent.mtt.supportui.views.asyncimage.ContentDrawable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"deprecation", "unused"})
public class HippyImageView extends AsyncImageView implements CommonBorder, HippyViewBase, ITVView, StateView,
  PostTaskHolder, ListItemHolder, ViewStateProvider {

  public static final String IMAGE_TYPE_APNG = "apng";
  public static final String IMAGE_TYPE_GIF = "gif";
  public static final String IMAGE_PROPS = "props";
  public static final String IMAGE_VIEW_OBJ = "viewobj";


  /** zhaopeng**/
  private HippyMap initProps = new HippyMap();
  private boolean mHasSetTempBackgroundColor = false;
  private boolean mUserHasSetBackgroudnColor = false;
  private int mUserSetBackgroundColor = Color.TRANSPARENT;

  private boolean isAutoFocus = false;
  private boolean isFillParent = false;
  private boolean focusScaleOnDuplicateParentState = false;
  //zhaopeng add roleType
  private RoleType mRoleType = RoleType.UNDEFINE;

  public enum RoleType {
    UNDEFINE,
    COVER,
    ICON,
  }



  private IImageStateListener<HippyImageView> mCustomStateListener;

  public void setCustomImageRequestListener(IImageStateListener mCustomImageRequestListener) {
    this.mCustomStateListener = mCustomImageRequestListener;
  }

  public void setRoleType(RoleType mRoleType) {
    this.mRoleType = mRoleType;
  }

  public RoleType getRoleType() {
    return mRoleType;
  }

  public int getFetchState(){
    return mUrlFetchState;
  }

  public void setFocusScaleOnDuplicateParentState(boolean focusScaleOnDuplicateParentState) {
    this.focusScaleOnDuplicateParentState = focusScaleOnDuplicateParentState;
  }


  @Override
  public void onSetSid(String sid) {
    AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(this);
    if(af != null){
      if(af.isNextAutofocus(sid)){
        //setAutoFocus(true,true);
        onRequestAutofocus(this, this, ExtendViewGroup.AUTOFOCUS_TYPE_FORCE);
      }
    }

  }

  @Override
  public boolean isAutoFocus() {
    return isAutoFocus;
  }

  @Override
  public void setAutoFocus(boolean b) {
    setAutoFocus(b,false);
//    if(b && isAttached() && !isFocused()
//      && getWidth() > 0 && getHeight() > 0 && getVisibility() == View.VISIBLE){
//      Log.i(AutoFocusManager.TAG,"auto requestFocus on setAutoFocus this:"+ExtendUtil.debugView(this));
//      //AutoFocusManager.globalRequestFocus(this);
////      onRequestAutofocus(this,this,ExtendViewGroup.AUTOFOCUS_TYPE_FORCE);
//    }
  }

  @Override
  public void setAutoFocus(boolean b,boolean requestFocusOnExist) {
    this.isAutoFocus = b;
    if(b) {
      Log.i(AutoFocusManager.TAG, "setAutoFocus true this:"+ExtendUtil.debugView(this)+",requestFocusOnExist "+requestFocusOnExist);
    }
    if(b && requestFocusOnExist && isAttached() && !isFocused()
      && getWidth() > 0 && getHeight() > 0 && getVisibility() == View.VISIBLE) {
      Log.i(AutoFocusManager.TAG, "auto requestFocus on setAutofocus this:" + ExtendUtil.debugView(this));
      onRequestAutofocus(this, this, ExtendViewGroup.AUTOFOCUS_TYPE_FORCE);
    }
  }

  private boolean skipRequestFocus = false;

  @Override
  public void setSkipRequestFocus(boolean b) {
    this.skipRequestFocus = b;
  }

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    if(skipRequestFocus){
      Log.e(AutoFocusManager.TAG,"requestFocus return false on skipRequestFocus this:"+this);
      return false;
    }
    boolean forceDisableFocus = FocusManagerModule.findGlobalFocusConfig(getContext()).forceDisableFocus;
    if(forceDisableFocus){
      return false;
    }
    return super.requestFocus(direction, previouslyFocusedRect);
  }

  //zhaopeng add
//  private final BorderFrontDrawable focusFrontDrawable;

  protected PostHandlerView mPostView;
  private boolean onBindNew = false;
  private boolean enablePostTask = false;
  private boolean shouldBindReset = true;
  //border相关配置本地变量
  private boolean isBorderVisible;
  private boolean isBlackRectEnable;
  private int borderColor;
  private float borderCorner;
  private int borderWidth;
  private int globalVisibility = View.VISIBLE;

  public void setEnablePostTask(boolean enablePostTask) {
    this.enablePostTask = enablePostTask;
  }


  @Override
  public void setRootPostHandlerView(PostHandlerView pv) {
    this.mPostView = pv;
  }

  private int getPostType() {
    return hashCode();
  }

//  @Override
//  protected void onWindowVisibilityChanged(int visibility) {
//
//  }

  @Override
  public void onResetBeforeCache() {
    if (isPostTaskEnabled()) {

      mPostView.clearTask(POST_TASK_CATEGORY_IMG, getPostType());
    }
    if (LogUtils.isDebug()) {
      Log.i("HippyImage", "onResetBeforeCache this:" + this);
    }

    if (mCustomStateList != null && !mCustomStateList.isEmpty()) {
      //mCustomStateList.clear();
      for(String key:mCustomStateList.keySet()){
        setCustomState(key,false);
      }
    }
//    setAutoFocus(false);
  }

  protected boolean isPostTaskEnabled() {
    return enablePostTask && mPostView != null;
  }

  @Override
  protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);
//    if(LogUtils.isDebug()) {
//      Log.i(AutoFocusManager.TAG,"div on view onVisibilityChanged visibility:"+visibility+",changedView:"+ExtendUtil.debugViewLite(changedView)+",hasFocus:"+changedView.hasFocus());
//      if(changedView == getRootView()){
//        Log.e(AutoFocusManager.TAG,"onVisibilityChanged on rootView visibility:"+visibility+",changedView:"+ExtendUtil.debugView(changedView));
//      }
//    }
//    Log.v(AutoFocusManager.TAG,"onWindowVisibilityChanged visibility:"+visibility+",this:"+ExtendUtil.debugView(this));
    globalVisibility = visibility;
    if(AutoFocusManager.isAutofocusView(this) && visibility == View.VISIBLE){
      if(changedView != getRootView()) {//这里如果是decorView，证明是activity之间的切换，理论上来说，不需要再次请求焦点
        if (getWidth() > 0 && getHeight() > 0) {
          if (LogUtils.isDebug()) {
            Log.i(AutoFocusManager.TAG, "auto requestFocus on onWindowVisibilityChanged ,view:" + ExtendUtil.debugView(this));
          }
          onRequestAutofocus(this, this, ExtendViewGroup.AUTOFOCUS_TYPE_VISIBILITY);
        } else {
          Log.e(AutoFocusManager.TAG, "auto requestFocus return on onWindowVisibilityChanged ,view size invalid:" + ExtendUtil.debugView(this));
        }
      }else if(LogUtils.isDebug()){
        Log.e(AutoFocusManager.TAG,"auto requestFocus return on rootView changed:"+ExtendUtil.debugView(changedView));
      }
    }
//    if(visibility == View.VISIBLE) {
//      if(getWidth() <1 || getHeight() < 1){
//        Log.e(AutoFocusManager.TAG, "------onVisibilityChanged this:" + this + ",isAttached:" + isAttached() + ",width:" + getWidth());
//      }else {
//        Log.v(AutoFocusManager.TAG, "------onVisibilityChanged this:" + this + ",isAttached:" + isAttached() + ",width:" + getWidth());
//      }
//    }else{
//      Log.e(AutoFocusManager.TAG, "------onVisibilityChanged to false this:" + this + ",isAttached:" + isAttached() + ",width:" + getWidth());
//    }
//    if(visibility == View.VISIBLE && (AutoFocusManager.isAutofocusView(this)) && getWidth() > 0 && getHeight() > 0){
//      Log.i(AutoFocusManager.TAG,"auto requestFocus on onVisibilityChanged this:"+this);
//      //AutoFocusManager.globalRequestFocus(this);
//      onRequestAutofocus(this,this,ExtendViewGroup.AUTOFOCUS_TYPE_VISIBILITY);
//    }
  }

  @Override
  protected void postTask(Runnable task, int delayLoad) {
    if (isPostTaskEnabled()) {
      if (LogUtils.isDebug()) {
        Log.i("HippyImage", "postTask by postView:" + hashCode() + ",url:" + getUrl() + ",task:" + task.hashCode() + ",mPostView:" + ((View) mPostView).getId());
      }
      mPostView.clearTask(POST_TASK_CATEGORY_IMG, getPostType());
      mPostView.postTask(POST_TASK_CATEGORY_IMG, getPostType(), task, delayLoad);
    } else {
      if (LogUtils.isDebug()) {
        Log.i("HippyImage", "postTask by super:" + this);
      }
      super.postTask(task, delayLoad);
    }
  }

  /**
   * 新增beforeItemBind方法，onItemBind前逻辑处理
   *
   * @param url bind的图片url
   */
  public void beforeItemBind(Object url) {
    if (LogUtils.isDebug()) {
      Log.e("HippyImage", "beforeItemBind:" + hashCode() + ",url:" + url);
    }
    shouldBindReset = !(url instanceof String) || !TextUtils.equals(getUrl(), (String) url);
    //setAutoFocus(false);
  }



  @Override
  public void onItemBind() {
    if (LogUtils.isDebug()) {
      Log.e("HippyImage", "onItemBind:" + hashCode() + ",url:" + getUrl());
    }
    if (isPostTaskEnabled() && shouldBindReset) {
      //resetContent();
//      mOldDrawable = getBackground();
      if (mBGDrawable != null) {
        setBackgroundDrawable(mBGDrawable);
      } else {
        setBackgroundDrawable(null);
      }
      invalidate();
      onBindNew = true;
      mPostView.clearTask(POST_TASK_CATEGORY_IMG, getPostType());
    }
    shouldBindReset = true;
  }

  //zhaopeng

  /**
   * 播放GIF动画的关键类
   */
  private Movie mGifMovie;
  private int mGifStartX = 0;
  private int mGifStartY = 0;
  private float mGifScaleX = 1;
  private float mGifScaleY = 1;
  private boolean mGifMatrixComputed = false;
  private int mGifProgress = 0;
  private long mGifLastPlayTime = -1;
  private boolean selectState = false;

  @Override
  public void resetProps() {
    HippyViewController.resetTransform(this);
    setAlpha(1.0f);
    mTintColor = 0;
    mBGDrawable = null;
    mContentDrawable = null;
    mScaleType = AsyncImageView.ScaleType.FIT_XY;
    setImagePositionX(0);
    setImagePositionY(0);
    setAutoFocus(false);
    mUrl = null;
    mImageType = null;
    setBackgroundDrawable(null);
    if (mCustomStateList != null && !mCustomStateList.isEmpty()) {
      //mCustomStateList.clear();
      for(String key:mCustomStateList.keySet()){
        setCustomState(key,false);
      }
    }
//    LogUtils.v("HippyDrawable","resetProps ,this:"+this);
    Arrays.fill(mShouldSendImageEvent, false);
  }

  @Override
  public void clear() {
    //先解决图片绘制黑屏的问题。 更完全的改法是调用resetProps.
    mTintColor = 0;
  }


  /**zhaopeng add*/
//  protected PostHandlerView mPostHandlerView;
//  protected boolean enablePostTask = false;
//  @Override
//  public void setRootPostHandlerView(PostHandlerView pv) {
//    this.mPostHandlerView = pv;
//  }
//  protected boolean isPostTaskEnabled(){
//    return mPostHandlerView != null && enablePostTask;
//  }

  /**
   * zhaopeng add end
   */

  public enum ImageEvent {
    ONLOAD,
    ONLOAD_START,
    ONLOAD_END,
    ONERROR
  }

  protected NativeGestureDispatcher mGestureDispatcher;

  private OnLoadEvent mOnLoadEvent;
  private OnLoadEndEvent mOnLoadEndEvent;
  private OnErrorEvent mOnErrorEvent;
  private OnLoadStartEvent mOnLoadStartEvent;
  private final boolean[] mShouldSendImageEvent;
  private Rect mNinePatchRect;
  private final HippyEngineContext hippyEngineContext;

  public HippyImageView(Context context) {
    super(context);
    mShouldSendImageEvent = new boolean[ImageEvent.values().length];
    hippyEngineContext = ((HippyInstanceContext) context).getEngineContext();
    if (hippyEngineContext != null) {
      setImageAdapter(hippyEngineContext.getGlobalConfigs().getImageLoaderAdapter());
      //自定义borderDrawable
      this.borderType = hippyEngineContext.getGlobalConfigs().getEsBaseConfigManager().getFocusBorderType();
      setBorderDrawable(hippyEngineContext.getGlobalConfigs().getBorderDrawableProvider().create());
    }
    //zhaopeng addsetBlackRectEnable
//    focusFrontDrawable = new BorderFrontDrawable();
//    focusFrontDrawable.setBorderVisible(false);
//    focusFrontDrawable.setBlackRectEnable(FocusManagerModule.defaultFocusBorderInnerRectEnable);
//    focusFrontDrawable.setVisible(false, false);
    FocusManagerModule.GlobalFocusConfig focusConfig = FocusManagerModule.getGlobalFocusConfig(hippyEngineContext);
    if (borderDrawable != null) {
      borderDrawable.setBorderVisible(false);
      borderDrawable.setCallback(this);
      borderDrawable.setBlackRectEnable(focusConfig.defaultFocusBorderInnerRectEnable);
      borderDrawable.setVisible(false, false);
    }
    setBlackRectEnable(focusConfig.defaultFocusBorderInnerRectEnable);
    setFocusBorderColor(focusConfig.defaultFocusBorderColor);
    setFocusBorderEnable(focusConfig.defaultFocusBorderEnable);
    setFocusBorderCorner(focusConfig.defaultFocusBorderRadius);
    setFocusBorderWidth(focusConfig.defaultFocusBorderWidth);
    setFocusBorderInset(focusConfig.defaultFocusBorderInset);
  }

  @Override
  public void invalidateDrawable(@NonNull Drawable dr) {
    super.invalidateDrawable(dr);
    if (borderDrawable != null && dr == borderDrawable) {
      invalidate();
    }
  }

  public void initBorderDrawable() {
    if (borderDrawable != null) {
      borderDrawable.setBorderVisible(this.isBorderVisible);
      borderDrawable.setCallback(this);
      borderDrawable.setBlackRectEnable(this.isBlackRectEnable);
      borderDrawable.setVisible(false, false);
      borderDrawable.setBorderColor(this.borderColor);
      borderDrawable.setBorderCorner(this.borderCorner);
    }
  }

  //设置focusDrawable样式
  public void setFocusBorderType(int type) {
    this.borderType = type;
    if (hippyEngineContext != null) {
      setBorderDrawable(hippyEngineContext.getGlobalConfigs().getBorderDrawableProvider().create());
      initBorderDrawable();
      invalidate();
    }
  }

  //zhaopeng add

  /**
   * 设置边框颜色
   *
   * @param color
   */
  public void setFocusBorderColor(@ColorInt int color) {
    if (borderDrawable != null) {
      this.borderColor = color;
      borderDrawable.setBorderColor(color);
      invalidate();
    }
  }

  /**
   * 设置边框弧度
   *
   * @param
   */
  public void setFocusBorderCorner(float radius) {
    if (borderDrawable != null) {
      this.borderCorner = radius;
      borderDrawable.setBorderCorner(radius);
      invalidate();
    }
  }

  /**
   * 设置边框宽度
   *
   * @param
   */
  public void setFocusBorderWidth(int width) {
    if (borderDrawable != null && width > -1) {
      this.borderWidth = width;
      borderDrawable.setBorderWidth(width);
      invalidate();
    }
  }

  /**
   * 设置边框宽度
   *
   * @param
   */
  public void setFocusBorderInset(int inset) {
    if (borderDrawable != null) {
      borderDrawable.setBorderInset(inset);
      invalidate();
    }
  }

  public void setFocusBorderEnable(boolean enable) {
    if (borderDrawable != null) {
      this.isBorderVisible = enable;
      borderDrawable.setBorderVisible(enable);
      invalidate();
    }
  }

  public void setBlackRectEnable(boolean enable) {
    if (borderDrawable != null) {
      this.isBlackRectEnable = enable;
      borderDrawable.setBlackRectEnable(enable);
      invalidate();
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    if (borderDrawable != null) {
      borderDrawable.onDraw(canvas);
    }
  }

  //zhaopeng add

  public void setInitProps(HippyMap props) {
    initProps = props;
  }

  /**
   * 前端传递下来的参数 left 到左边的距离 right 到右边的距离 top 到上边的距离 botttom 到下边的距离 Robinsli
   */
  public void setNinePatchCoordinate(boolean shouldClearNinePatch, int left, int top, int right,
                                     int botttom) {
    if (shouldClearNinePatch) {
      mNinePatchRect = null;
    } else {
      if (mNinePatchRect == null) {
        mNinePatchRect = new Rect();
      }
      mNinePatchRect.set(left, top, right, botttom);
    }
    if (mContentDrawable instanceof HippyContentDrawable) {
      ((HippyContentDrawable) mContentDrawable).setNinePatchCoordinate(mNinePatchRect);
      invalidate();
    }
  }

  public void setImageEventEnable(int index, boolean enable) {
    mShouldSendImageEvent[index] = enable;
  }

  @Override
  protected void resetContent() {
    super.resetContent();
//    LogUtils.v("HippyDrawable","resetContent ,this:"+this);
    mGifMovie = null;
    mGifProgress = 0;
    mGifLastPlayTime = -1;
  }

  @Override
  protected boolean shouldUseFetchImageMode(String url) {
    return UrlUtils.isWebUrl(url) || UrlUtils.isFileUrl(url);
  }

  public void setHippyViewDefaultSource(String defaultSourceUrl) {
    setDefaultSource(defaultSourceUrl);
  }

  @Override
  protected void doFetchImage(Object param, final int sourceType) {
    if (mImageAdapter != null) {
      if (param == null) {
        param = new HashMap<String, Object>();
      }

      if (param instanceof Map) {
        if (hippyEngineContext != null) {
          RenderNode node = hippyEngineContext.getRenderManager().getRenderNode(getId());
          if (node != null) {
            initProps = node.getProps();
          }
        }

        try {
          //noinspection unchecked,rawtypes
          ((Map) param).put(IMAGE_PROPS, initProps);
          //noinspection unchecked,rawtypes
          ((Map) param).put(IMAGE_VIEW_OBJ, this);
        } catch (Exception e) {
          LogUtils.d("HippyImageView", "doFetchImage: " + e);
        }
      }

      // 这里不判断下是取背景图片还是取当前图片怎么行？
      final String url = sourceType == SOURCE_TYPE_SRC ? mUrl : mDefaultSourceUrl;
      //noinspection unchecked
      mImageAdapter.fetchImage(url, new HippyImageLoader.Callback() {
        @Override
        public void onRequestStart(HippyDrawable drawableTarget) {
          mSourceDrawable = drawableTarget;
        }

        @Override
        public void onRequestSuccess(HippyDrawable drawableTarget) {
          if (sourceType == SOURCE_TYPE_SRC) {
            if (!TextUtils.equals(url, mUrl)) {
              return;
            }
            mUrlFetchState = IMAGE_LOADED;
          }

          if (sourceType == SOURCE_TYPE_DEFAULT_SRC && !TextUtils.equals(url, mDefaultSourceUrl)) {
            return;
          }

          handleImageRequest(drawableTarget, sourceType, null);
        }

        @Override
        public void onRequestFail(Throwable throwable, String source) {
          if (sourceType == SOURCE_TYPE_SRC) {
            if (!TextUtils.equals(url, mUrl)) {
              return;
            }
            mUrlFetchState = IMAGE_UNLOAD;
          }

          if (sourceType == SOURCE_TYPE_DEFAULT_SRC && !TextUtils.equals(url, mDefaultSourceUrl)) {
            return;
          }

          handleImageRequest(null, sourceType, throwable);
        }
      }, param);
    }
  }

  public void setBackgroundColor(int backgroundColor) {
    mUserHasSetBackgroudnColor = true;
    mUserSetBackgroundColor = backgroundColor;
//    LogUtils.v("HippyDrawable","setBackgroundColor ,this:"+this);
    super.setBackgroundColor(backgroundColor);
  }

  @Override
  protected void onFetchImage(String url) {
    if (mContentDrawable instanceof ContentDrawable &&
      ((ContentDrawable) mContentDrawable).getSourceType() == SOURCE_TYPE_DEFAULT_SRC) {
      return;
    }

    Drawable oldBGDrawable = getBackground();
    resetContent();


    if (url != null && (UrlUtils.isWebUrl(url) || UrlUtils.isFileUrl(url))) {
      int defaultBackgroundColor = Color.LTGRAY;
      if (mUserHasSetBackgroudnColor) {
        defaultBackgroundColor = mUserSetBackgroundColor;
      }

      if (oldBGDrawable instanceof CommonBackgroundDrawable) {
        ((CommonBackgroundDrawable) oldBGDrawable).setBackgroundColor(defaultBackgroundColor);
        setCustomBackgroundDrawable((CommonBackgroundDrawable) oldBGDrawable);
      } else if (oldBGDrawable instanceof LayerDrawable) {
        LayerDrawable layerDrawable = (LayerDrawable) oldBGDrawable;
        int numberOfLayers = layerDrawable.getNumberOfLayers();

        if (numberOfLayers > 0) {
          Drawable bgDrawable = layerDrawable.getDrawable(0);
          if (bgDrawable instanceof CommonBackgroundDrawable) {
            ((CommonBackgroundDrawable) bgDrawable).setBackgroundColor(defaultBackgroundColor);
            setCustomBackgroundDrawable((CommonBackgroundDrawable) bgDrawable);
          }
        }
      }
      super.setBackgroundColor(defaultBackgroundColor);
      mHasSetTempBackgroundColor = true;
    }
  }

  @Override
  protected void afterSetContent(String url) {
    restoreBackgroundColorAfterSetContent();
  }

  @Override
  protected void restoreBackgroundColorAfterSetContent() {
    if (mBGDrawable != null && mHasSetTempBackgroundColor) {
      int defaultBackgroundColor = Color.TRANSPARENT;
      mBGDrawable.setBackgroundColor(defaultBackgroundColor);
//      LogUtils.v("HippyDrawable","restoreBackgroundColorAfterSetContent ,this:"+this);
      mHasSetTempBackgroundColor = false;
    }
  }

  @Override
  protected void updateContentDrawableProperty(int sourceType) {
    super.updateContentDrawableProperty(sourceType);
    if (mContentDrawable instanceof HippyContentDrawable && sourceType == SOURCE_TYPE_SRC) {
      ((HippyContentDrawable) mContentDrawable).setNinePatchCoordinate(mNinePatchRect);
    }
  }

  @Override
  protected ContentDrawable generateContentDrawable() {
    return new HippyContentDrawable();
  }

  @Override
  protected BackgroundDrawable generateBackgroundDrawable() {
    return new CommonBackgroundDrawable();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = super.onTouchEvent(event);
    if (mGestureDispatcher != null) {
      result |= mGestureDispatcher.handleTouchEvent(event);
    }
    return result;
  }

  @Override
  public NativeGestureDispatcher getGestureDispatcher() {
    return mGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {
    mGestureDispatcher = dispatcher;
  }

  @Override
  protected void handleGetImageStart() {
    // send onLoadStart event
    super.handleGetImageStart();
    if (mShouldSendImageEvent[ImageEvent.ONLOAD_START.ordinal()]) {
      getOnLoadStartEvent().send(this, null);
    }
    onBindNew = false;
    if (mCustomStateListener != null) {
      mCustomStateListener.onRequestStart(this);
    }
  }

  @Override
  protected void handleGetImageSuccess() {
    // send onLoad event
    if (mShouldSendImageEvent[ImageEvent.ONLOAD.ordinal()]) {
      getOnLoadEvent().send(this, null);
    }
    if(mCustomStateListener != null) {
      mCustomStateListener.onRequestSuccess(this);
    }
    // send onLoadEnd event
    if (mShouldSendImageEvent[ImageEvent.ONLOAD_END.ordinal()]) {
      HippyMap map = new HippyMap();
      map.pushInt("success", 1);
      if (mSourceDrawable != null) {
        Bitmap bitmap = mSourceDrawable.getBitmap();
        HippyMap imageSize = new HippyMap();
        if (bitmap != null) {
          imageSize.pushInt("width", bitmap.getWidth());
          imageSize.pushInt("height", bitmap.getHeight());
        } else {
          imageSize.pushInt("width", 0);
          imageSize.pushInt("height", 0);
        }
        map.pushMap("image", imageSize);
      } else {
        HippyMap imageSize = new HippyMap();
        imageSize.pushInt("width", 0);
        imageSize.pushInt("height", 0);
        map.pushMap("image", imageSize);
      }
      getOnLoadEndEvent().send(this, map);
    }
  }

  @Override
  protected void handleGetImageFail(Throwable throwable) {
    // send onError event
    if (mShouldSendImageEvent[ImageEvent.ONERROR.ordinal()]) {
      getOnErrorEvent().send(this, null);
    }
    // send onLoadEnd event
    if (mShouldSendImageEvent[ImageEvent.ONLOAD_END.ordinal()]) {
      HippyMap map = new HippyMap();
      map.pushInt("success", 0);
      getOnLoadEndEvent().send(this, map);
    }
    onBindNew = false;
    if (mCustomStateListener != null) {
      mCustomStateListener.onRequestFail(throwable, getUrl());
    }
  }

  private void computeMatrixParams() {
    if (!mGifMatrixComputed) {
      // reset
      mGifStartX = 0;
      mGifStartY = 0;
      mGifScaleX = 1;
      mGifScaleY = 1;
      if (mGifMovie.width() > 0 && mGifMovie.height() > 0 && getWidth() > 0 && getHeight() > 0) {
        mGifScaleX = getWidth() / (float) mGifMovie.width();
        mGifScaleY = getHeight() / (float) mGifMovie.height();
      }
      ScaleType type = mScaleType != null ? mScaleType : ScaleType.FIT_XY;
      switch (type) {
        case FIT_XY:
          // 拉伸图片且不维持宽高比，直到宽高都刚好填满容器
          break;
        case CENTER:
          // 居中不拉伸
          mGifScaleX = 1;
          mGifScaleY = 1;
          break;
        case CENTER_INSIDE:
          // 在保持图片宽高比的前提下缩放图片，直到宽度和高度都小于等于容器视图的尺寸
          // 这样图片完全被包裹在容器中，容器中可能留有空白
          if (mGifScaleX > mGifScaleY) {
            //noinspection SuspiciousNameCombination
            mGifScaleX = mGifScaleY;
          } else {
            //noinspection SuspiciousNameCombination
            mGifScaleY = mGifScaleX;
          }
          break;
        case CENTER_CROP:
          // 在保持图片宽高比的前提下缩放图片，直到宽度和高度都大于等于容器视图的尺寸
          // 这样图片完全覆盖甚至超出容器，容器中不留任何空白
          if (mGifScaleX < mGifScaleY) {
            //noinspection SuspiciousNameCombination
            mGifScaleX = mGifScaleY;
          } else {
            //noinspection SuspiciousNameCombination
            mGifScaleY = mGifScaleX;
          }
          break;
        case ORIGIN:
          mGifScaleX = mGifScaleY = 1;
          // 不拉伸，居左上
          break;
      }
      if (mScaleType != ScaleType.ORIGIN) {
        mGifStartX = (int) ((getWidth() / mGifScaleX - mGifMovie.width()) / 2f);
        mGifStartY = (int) ((getHeight() / mGifScaleY - mGifMovie.height()) / 2f);
      }
      mGifMatrixComputed = true;
    }
  }

  @Override
  protected void handleImageRequest(IDrawableTarget target, int sourceType, Object requestInfo) {
    if (target != null && !TextUtils.isEmpty(target.getImageType())) {
      mImageType = target.getImageType();
    }

    if (target instanceof HippyDrawable && ((HippyDrawable) target).isAnimated()) {
      mGifMovie = ((HippyDrawable) target).getGIF();
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    if (!TextUtils.isEmpty(mImageType) && mImageType.equals(IMAGE_TYPE_APNG)
      && sourceType == SOURCE_TYPE_SRC) {
      if (target != null) {
        Drawable drawable = target.getDrawable();
        if (drawable != null) {
          mSourceDrawable = null;
          mContentDrawable = drawable;
          mUrlFetchState = IMAGE_LOADED;
          setContent(sourceType);
          handleGetImageSuccess();
          return;
        }
      }

      mUrlFetchState = IMAGE_UNLOAD;
      handleGetImageFail(requestInfo instanceof Throwable ? (Throwable) requestInfo : null);
    } else {
      super.handleImageRequest(target, sourceType, requestInfo);
    }
  }

  protected boolean drawGIF(Canvas canvas) {
    if (mGifMovie == null) {
      return false;
    }

    int duration = mGifMovie.duration();
    if (duration == 0) {
      duration = 1000;
    }

    long now = System.currentTimeMillis();

    if (!isGifPaused) {
      if (mGifLastPlayTime != -1) {
        mGifProgress += now - mGifLastPlayTime;

        if (mGifProgress > duration) {
          mGifProgress = 0;
        }
      }
      mGifLastPlayTime = now;
    }

    computeMatrixParams();
    mGifMovie.setTime(mGifProgress);
    canvas.save(); // 保存变换矩阵
    canvas.scale(mGifScaleX, mGifScaleY);
    mGifMovie.draw(canvas, mGifStartX, mGifStartY);
    canvas.restore(); // 恢复变换矩阵

    if (!isGifPaused) {
      postInvalidateDelayed(40);
    }

    return true;
  }

  protected boolean shouldFetchImage() {
    if (mUrlFetchState == IMAGE_LOADING) {
      return onBindNew;
    } else if (mUrlFetchState == IMAGE_UNLOAD) {
      return true;
    }

    boolean isGif = (initProps != null) && initProps.getBoolean(NodeProps.CUSTOM_PROP_ISGIF);
    if (!isGif) {
      isGif = !TextUtils.isEmpty(mImageType) && mImageType.equals(IMAGE_TYPE_GIF);
    }

    if (!TextUtils.isEmpty(mImageType) && mImageType.equals(IMAGE_TYPE_APNG)
      && mContentDrawable != null && !(mContentDrawable instanceof ContentDrawable)) {
      return onBindNew;
    } else if (isGif) {
      return mGifMovie == null || onBindNew;
    } else {
      Bitmap bitmap = getBitmap();
      //noinspection RedundantIfStatement
      if (bitmap == null || bitmap.isRecycled()) {
        return true;
      }
    }

    return onBindNew;
  }

  // 图片自绘功能（方便自定义支持gif、webp、tpg等）尚未实现，暂时注释
//	private void drawSelf(Canvas canvas, HippyDrawable drawable) {
//		computeMatrixParams();
//		canvas.save(); // 保存变换矩阵
//
//		canvas.clipRect(new Rect(mGifStartX, mGifStartY, canvas.getWidth(), canvas.getHeight()));
//		canvas.scale(mGifScaleX, mGifScaleY);
//
//		drawable.draw(canvas);
//
//		// canvas.restore(); // 恢复变换矩阵
//		postInvalidateDelayed(40);
//	}


  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (isFocusable() && borderDrawable != null) {
      borderDrawable.onDetachedFromWindow(this);
    }
  }

  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    // 图片自绘功能（方便自定义支持gif、webp、tpg等）尚未实现，暂时注释
//		if (mSourceDrawable instanceof HippyDrawable)
//		{
//			HippyDrawable drawable = (HippyDrawable) mSourceDrawable;
//			if (drawable.isSelfDraw())
//				drawSelf(canvas, drawable);
//		}
//		else
    if (mGifMovie != null) {
      // 如果是GIF，就调用drawGIF()方法播放GIF动画
      drawGIF(canvas);
    }
  }

  private boolean isGifPaused = false;

  public void startPlay() {
    isGifPaused = false;
    invalidate();
  }

  public void pause() {
    isGifPaused = true;
    mGifLastPlayTime = -1;
  }

  @Override
  public void notifySaveState() {

  }

  @Override
  public void notifyRestoreState() {

  }

  private OnLoadEvent getOnLoadEvent() {
    if (mOnLoadEvent == null) {
      mOnLoadEvent = new OnLoadEvent();
    }
    return mOnLoadEvent;
  }

  private OnLoadEndEvent getOnLoadEndEvent() {
    if (mOnLoadEndEvent == null) {
      mOnLoadEndEvent = new OnLoadEndEvent();
    }
    return mOnLoadEndEvent;
  }

  private OnLoadStartEvent getOnLoadStartEvent() {
    if (mOnLoadStartEvent == null) {
      mOnLoadStartEvent = new OnLoadStartEvent();
    }
    return mOnLoadStartEvent;
  }

  private OnErrorEvent getOnErrorEvent() {
    if (mOnErrorEvent == null) {
      mOnErrorEvent = new OnErrorEvent();
    }
    return mOnErrorEvent;
  }

  /**
   * event to js
   **/
  static class OnLoadEvent extends HippyViewEvent {

    OnLoadEvent() {
      super("onLoad");
    }
  }

  static class OnLoadEndEvent extends HippyViewEvent {

    OnLoadEndEvent() {
      super("onLoadEnd");
    }
  }

  static class OnLoadStartEvent extends HippyViewEvent {

    OnLoadStartEvent() {
      super("onLoadStart");
    }
  }

  static class OnErrorEvent extends HippyViewEvent {

    OnErrorEvent() {
      super("onError");
    }
  }

  /**
   * add by zhaopeng 20201110
   */


  private int mDuration = TVFocusAnimHelper.DEFAULT_DURATION;
  private float mFocusScaleX = FocusManagerModule.defaultFocusScale;
  private float mFocusScaleY = FocusManagerModule.defaultFocusScale;

  private final AttachInfo mAttachInfo = new AttachInfo();

  /**
   * -------------------------------------------------------------------------------------
   **/

  @Override
  public void onHandleFocusScale(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    if (isFocusable() && mFocusScaleX != 1 || mFocusScaleY != 1) {
      handleFocusScale(gainFocus, direction, previouslyFocusedRect, mDuration);
    }

  }


  public void handleFocusScale(boolean gainFocus, int direction, Rect previouslyFocusedRect, int duration) {
    if ((mFocusScaleX != 1 || mFocusScaleY != 1)) {
      AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(this);
      if (af != null) {
        af.handleOnFocusChange(this, gainFocus, mFocusScaleX, mFocusScaleY, duration);
      }else {
        TVFocusAnimHelper.handleOnFocusChange(this, gainFocus, mFocusScaleX, mFocusScaleY, duration);
      }
    }
  }

  public void handleFocusScaleImmediately(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    if (isFocusable() && (mFocusScaleX != 1 || mFocusScaleY != 1)) {
//      Log.e("zhaopeng","!!!!!!handleFocusScaleImmediately gainFocus:"+gainFocus);
      TVFocusAnimHelper.handleOnFocusChange(this, gainFocus, mFocusScaleX, mFocusScaleY, 0);
    }
  }

  @Override
  public Rect getFloatFocusMarginRect() {
    return mAttachInfo.mFloatFocusMarginRect;
  }


  /**
   * {@link ITVView#setFocusScale(float)}
   *
   * @param scale 缩放倍数
   */
  @Override
  public void setFocusScale(float scale) {
    this.mFocusScaleX = scale;
    this.mFocusScaleY = scale;
  }


  /**
   * 设置View获得焦点的放大倍数
   */
  @Override
  public void setFocusScaleX(float scale) {
    this.mFocusScaleX = scale;
  }

  /**
   * 设置View获得焦点的放大倍数
   */
  @Override
  public void setFocusScaleY(float scale) {
    this.mFocusScaleY = scale;
  }


  /**
   * {@link ITVView#setFocusScaleDuration(int)}
   *
   * @param duration 缩放动画时长 单位：毫秒
   */
  @Override
  public void setFocusScaleDuration(int duration) {
    this.mDuration = duration;
  }

  @Override
  public float getFocusScale() {
    return mFocusScaleX ;
  }

  @Override
  public float getFocusScaleX() {
    return mFocusScaleX;
  }

  @Override
  public float getFocusScaleY() {
    return mFocusScaleY;
  }

  @Override
  public IFloatFocusManager getFloatFocusManager() {
    return null;
  }

  @Override
  public void setFloatFocusFocusedAlpha(float alpha) {
    mAttachInfo.setFloatFocusFocusedAlpha(alpha);
  }

  @Override
  public AttachInfo getAttachInfo() {
    return mAttachInfo;
  }

  @Override
  public View getView() {
    return this;
  }


  protected int getUserSetBackgroundColor() {
    return mUserSetBackgroundColor;
  }

  private boolean mInReFocus = false;

  @Override
  public void notifyInReFocus(boolean isIn) {
    this.mInReFocus = isIn;
//    Log.d("zhaopeng"," notifyInReFocus :"+isIn+" this:"+this);
    if (!isFocusable()) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        if (child instanceof ITVView) {
          ((ITVView) child).notifyInReFocus(isIn);
        }
      }
    }
  }

  @Override
  protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    if (borderDrawable != null && isBorderVisible) {
      borderDrawable.onFocusChanged(this, gainFocus);
    }
  }

  @Override
  public boolean isInReFocus() {
    return mInReFocus;
  }

  private int mUserSetFocusBackgroundColor = 0;
  private int mUserSetSelectBackgroundColor = 0;
  private boolean relayoutAfterAttach = false;


  /***
   *
   * @param color
   */
  public void setFocusBackGroundColor(int color) {
    this.mUserSetFocusBackgroundColor = color;
//    invalidate();
    drawableStateChanged();
  }

  /***
   *
   * @param color
   */
  public void setSelectBackGroundColor(int color) {
    this.mUserSetSelectBackgroundColor = color;
//    invalidate();
    drawableStateChanged();
  }


  public void setRelayoutAfterAttach(boolean relayoutAfterAttach) {
    this.relayoutAfterAttach = relayoutAfterAttach;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (selectState) {
      setSelected(true);
    }
    if (relayoutAfterAttach) {
      if (getWidth() <= 0 || getHeight() <= 0) {
        if (getContext() instanceof HippyInstanceContext) {
          final HippyEngineContext context = ((HippyInstanceContext) getContext()).getEngineContext();
          if (context != null) {
            final RenderNode node = context.getRenderManager().getRenderNode(getId());
            if (node != null && !node.isDelete()) {
              node.updateLayout(node.getX(), node.getY(), node.getWidth(), node.getHeight());
              node.updateViewRecursive();
            }
          }
        }
      }
    }
    //Log.v(AutoFocusManager.TAG, "------onAttachedToWindow this:" + this + ",isAttached:" + isAttached()+",width:"+getWidth());
    if(getWidth() > 0 && getHeight() > 0 && getVisibility() == View.VISIBLE){
      if(AutoFocusManager.isAutofocusView(this)) {
        if(LogUtils.isDebug()) {
          Log.i(AutoFocusManager.TAG, "auto requestFocus on onAttachedToWindow ,view:" + this);
        }
        onRequestAutofocus(this, this,ExtendViewGroup.AUTOFOCUS_TYPE_ATTACH);
      }
    }
//    LogUtils.v("HippyDrawable","onAttachedToWindow ,this:"+this);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (borderDrawable != null) {
      borderDrawable.onSizeChanged(w, h, oldw, oldh);
    }
    for(int i = 0; i < getChildCount(); i ++){
      final View v = getChildAt(i);
      if(v instanceof ITVView){
        final ITVView cv = (ITVView) v;
        if(cv.isFillParent()){
          cv.getView().layout(0,0,w,h);
          //layoutChildrenLegacy(cv.getView(),l,t,r,b);
          if(LogUtils.isDebug()) {
            Log.d(CustomLayoutView.TAG, "layout fill child:" + cv.getView());
          }
        }
      }
    }

    if(oldw < 1 && oldh < 1 && w > 0 && h > 0){
      //Log.v(AutoFocusManager.TAG,"------onSizeChanged this:"+this+",isAttached:"+isAttached());
      //v-show变化后
      if(globalVisibility == View.VISIBLE && getVisibility() == View.VISIBLE && isAttached()){
        //AutoFocusManager.globalRequestFocus(this);
        if(AutoFocusManager.isAutofocusView(this)) {
          if(LogUtils.isDebug()) {
            Log.i(AutoFocusManager.TAG, "auto requestFocus on size changed ,view:" + ExtendUtil.debugView(this));
          }
          onRequestAutofocus(this, this,ExtendViewGroup.AUTOFOCUS_TYPE_SIZE_VALID);
        }
      }
      //这里由于设置backgroundColorStr背景色，在尺寸0的时候不生效，所以这里再设置一下。
      ExtendTag tag = ExtendTag.getExtendTag(this);
      if(tag!= null && tag.pendingBackGroundColor != -1){
        setBackgroundColor(tag.pendingBackGroundColor);
        tag.pendingBackGroundColor = -1;
      }

      //if(AutoFocusManager.)
    }
  }

  public void onRequestAutofocus(View child, View target, int type) {
      if(child.getParent() instanceof ExtendViewGroup){
        ExtendViewGroup parent = (ExtendViewGroup) child.getParent();
        parent.onRequestAutofocus(child,target,type);
      }else{
        Log.i(AutoFocusManager.TAG,"onRequestAutofocus parent is not a instance of ExtendViewGroup parent: "+getParent());
      }
  }

  @Override
  protected void onDrawableAttached() {
    super.onDrawableAttached();
  }

  protected void onChangeShowOnState() {

  }

  private Runnable updateBGTask;

  private void postSetBGColor(final int bg) {
//    if(updateBGTask != null){
//      removeCallbacks(updateBGTask);
//    }
//    updateBGTask = () -> {
//      getBackgroundDrawable().setBackgroundColor(bg);
//      invalidate();
//    };
//    postDelayed(updateBGTask,16);
    getBackgroundDrawable().setBackgroundColor(bg);
    postInvalidateDelayed(16);
//    invalidate();
  }



  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    int[] states = getDrawableState();


    if (!isFocused() && isDuplicateParentStateEnabled()) {
      final boolean focused = ExtendUtil.stateContainsAttribute(states, android.R.attr.state_focused);
      if(isBorderVisible){
        if (borderDrawable != null) {
          borderDrawable.onDrawableStateChanged(this, focused);
        }
      }
      if (focusScaleOnDuplicateParentState && (mFocusScaleX != 1 || mFocusScaleY != 1)) {
        //TVFocusScaleExcuter.handleOnFocusChange(this, focused, mFocusScaleX, mFocusScaleY, mDuration);
//        onHandleFocusScale();
        handleFocusScale(focused,-1,null,mDuration);
      }
    }
    if (ExtendUtil.handleShowOnState(this, states, showOnState)) {
      onChangeShowOnState();
    }
    ExtendUtil.handleCustomShowOnState(this, mCustomStateList,showOnStateCustom);
//    if(showOnState == null && isDuplicateParentStateEnabled()) {
//      if(LogUtils.isDebug()) {
//        LogUtils.e("HippyDrawable", "isDuplicateParentStateEnabled0 ,this:" + hashCode() + ",showOnState is NULL");
//      }
//    }
//    else{
//      LogUtils.v("HippyDrawable", "isDuplicateParentStateEnabled0 ,this:" + hashCode() + ",showOnState length:" + showOnState.length);
//    }
    if (isFocusable() || isDuplicateParentStateEnabled()) {
      if (mUserSetFocusBackgroundColor != 0) {

        final boolean focused = ExtendUtil.stateContainsAttribute(states, android.R.attr.state_focused);
        if (focused || isFocused()) {
          postSetBGColor(mUserSetFocusBackgroundColor);
          return;
        }
      }

      final boolean select = ExtendUtil.stateContainsAttribute(states, android.R.attr.state_selected);
      if (mUserSetSelectBackgroundColor != 0) {
        if (select || isSelected()) {
          postSetBGColor(mUserSetSelectBackgroundColor);
          return;
        }
      }
      if (mUrlFetchState != IMAGE_LOADED || onBindNew || getId() != -1) {
        //zhaopeng 因为图片已经加载成功，直接展示图片，不需要再展示背景
        //getId != -1的判断是由于在fastList里会走onBindNew，而在普通的img标签上却没有机会执行，所以这里做一下修正
        postSetBGColor(getUserSetBackgroundColor());
      } else {
        postInvalidateDelayed(16);
      }
    }
  }

  @Override
  public void getState(@NonNull HippyMap map) {
    if (getBitmap() != null) {
      map.pushInt("imgWidth", getBitmap().getWidth());
      map.pushInt("imgHeight", getBitmap().getHeight());
    }
  }

  private int[] showOnState;


  @Override
  public void setShowOnState(int[] showOnState) {
    if (LogUtils.isDebug()) {
      LogUtils.i("HippyDrawable", "setShowOnState ,showOnState size:" + showOnState.length + ",this:" + hashCode());
    }
    this.showOnState = showOnState;
  }

  /** custom showOnState
   * --------------------
   */
  private String[] showOnStateCustom;
  ArrayMap<String,Boolean> mCustomStateList = null;
  @Override
  public void setCustomState(String state, boolean on) {
    if (mCustomStateList == null) {
      mCustomStateList = new ArrayMap<>();
    }
    mCustomStateList.put(state,on);
    if("selected".equals(state)){
      setSelectState(on);
    }
    Log.i("CustomState","setCustomState state:"+state+" on:"+on+" this:"+ExtendUtil.debugViewLite(this));
    changeChildrenCustomState(this,state,on);
    refreshDrawableState();
  }

  void changeChildrenCustomState(ViewGroup view,String state, boolean on){
    for (int i = 0; i < view.getChildCount(); i++) {
      View child = view.getChildAt(i);
      if (child.isDuplicateParentStateEnabled()) {
        if (child instanceof StateView) {
          ((StateView) child).setCustomState(state, on);
        } else if (child instanceof ViewGroup) {
          changeChildrenCustomState((ViewGroup) child, state, on);
        }
      }
    }
  }


  @Override
  public void setShowOnCustomState(String[] showOnState) {
    this.showOnStateCustom = showOnState;
  }

  /**custom showOnState
   * ----------------
   */
  /**
   * -------------------------------------------------------------------------------------
   **/

  public void setSelectState(boolean selectState) {
    this.selectState = selectState;
    setSelected(selectState);
  }



  @Override
  public void setSelected(boolean selected) {
    super.setSelected(selected);
    if (showOnState != null && showOnState.length > 0) {
      HippyMap hippyMap = new HippyMap();
      hippyMap.pushBoolean("isSelected", selected);
      new HippyViewEvent("onSelect").send(this, hippyMap);
    }
  }


  @Override
  public void setFillParent(boolean b) {
    isFillParent = b;
  }

  @Override
  public boolean isFillParent() {
    return isFillParent;
  }



  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    if(changed){
      if (mCustomStateListener != null) {
        mCustomStateListener.onImageLayout(this, l, t, r, b);
      }
    }
  }

  @Override
  public void notifyBringToFront(boolean b) {

  }

  //这里暂不需要
//  static void layoutChildrenLegacy(View view,int l, int t, int r, int b){
//    if(!(view instanceof ViewGroup)){
//      return;
//    }
//    for(int i = 0; i < ((ViewGroup) view).getChildCount(); i ++){
//      final View child = ((ViewGroup) view).getChildAt(i);
//      LayoutParams lp = child.getLayoutParams();
//      int cl,ct,cr,cb;cl = ct= cr = cb = 0;
//      if (lp != null) {
//        if(lp.width == LayoutParams.MATCH_PARENT){
//          cr = r - l;
//        }
//        if(lp.height == LayoutParams.MATCH_PARENT){
//          cb = b - t ;
//        }
//      }else {
//        cr = r - l;
//        cb = b - t;
//      }
//      Log.e(CustomLayoutView.TAG,"layoutChildrenLegacy left:"+cl+",top :"+ct+",right :"+cr+",b:"+cb);
//      RenderUtil.layoutView(child,cl,ct,cr,cb);
//      layoutChildrenLegacy(child,cl,ct,cr,cb);
//
//    }
//  }

}
