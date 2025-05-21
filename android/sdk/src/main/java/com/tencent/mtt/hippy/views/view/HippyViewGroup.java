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
package com.tencent.mtt.hippy.views.view;


import com.quicktvui.hippyext.views.EngineRootView;
import com.quicktvui.hippyext.views.FocusSearchHelper;
import com.quicktvui.hippyext.views.fastlist.Utils;
import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.hippyext.RenderUtil;
import com.quicktvui.base.ui.TVFocusAnimHelper;
import com.quicktvui.base.ui.TVViewUtil;
import com.quicktvui.base.ui.TriggerTaskHost;
import com.quicktvui.hippyext.TriggerTaskManagerModule;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.IHippyZIndexViewGroup;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.uimanager.ViewGroupDrawingOrderHelper;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogAdapterUtils;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.image.HippyImageView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.CycleInterpolator;

import android.support.annotation.NonNull;

import java.util.ArrayList;

@SuppressWarnings({"unused"})
public class HippyViewGroup extends HippyImageView implements IHippyZIndexViewGroup, TriggerTaskHost, ExtendViewGroup, CustomLayoutView {

  private static final int LAYER_TYPE_NOT_SET = -1;
  private final ViewGroupDrawingOrderHelper mDrawingOrderHelper;
  float mDownX = 0;
  float mDownY = 0;
  boolean isHandlePullUp = false;
  //	private CommonBackgroundDrawable			mBGDrawable;
  //	private NativeGestureDispatcher				mGestureDispatcher;
  private String mOverflow;
  private Path mOverflowPath;
  private RectF mOverflowRect;
  private int mOldLayerType;
  private ViewConfiguration mViewConfiguration;

//  Scroller mScroller;

  //zhaopeng add
  private int overFlowViewIndex = -1;
  private boolean mBringToFrontOnFocus = false;

  private boolean useAdvancedFocusSearch = false;


  private GradientDrawable mBackDrawable;

  private View mSpecialFocusSearchRequest;

  public static final String TAG = "DIV_LOG";
  //在tvList焦点滚动时是否以此为目标
  private boolean isFocusScrollTarget = false;
  private final HippyEngineContext hippyEngineContext;
  //焦点抖动开关
  protected boolean shakeSelf;
  private boolean interceptKeyEvent = false;
  private HippyArray interceptKeyEvents = null;
  private HippyViewEvent interceptKeyJSEvent = null;
  //Focus memory
  protected View memoryFocused = null;
  private boolean focusMemoryEnable = false;

  private boolean enableOverScrollX = false;
  private boolean enableOverScrollY = false;
  private FocusSearchHelper mFocusSearchHelper;
//  protected boolean overScrollImmediately = true;


  public void setEnableOverScrollX(boolean enableOverScrollX) {
    this.enableOverScrollX = enableOverScrollX;
  }

  public void setEnableOverScrollY(boolean enableOverScrollY) {
    this.enableOverScrollY = enableOverScrollY;
  }

  public boolean isEnableOverScrollX() {
    return enableOverScrollX;
  }

  public boolean isEnableOverScrollY() {
    return enableOverScrollY;
  }

  public void setInterceptKeyEvent(boolean b){
    Log.i(TAG,"setInterceptKeyEvent :"+b +",this:"+this);
    this.interceptKeyEvent = b;
  }

  public void setInterceptKeyEvents(HippyArray array) {
    Log.i(TAG,"setInterceptKeyEvents :"+array+",this:"+this);
    this.interceptKeyEvents = array;
  }

  public HippyViewGroup(Context context) {
    super(context);


//    setBackgroundResource(R.drawable.selector_item_bg);
    mDrawingOrderHelper = new ViewGroupDrawingOrderHelper(this);
    mOldLayerType = LAYER_TYPE_NOT_SET;
    setScaleType(ScaleType.ORIGIN);
    /*zhaopeng add 默认false*/
//    setClipChildren(false);
    //初始化焦点抖动配置
    hippyEngineContext = ((HippyInstanceContext) context).getEngineContext();
    if (hippyEngineContext != null) {
      shakeSelf = hippyEngineContext.getGlobalConfigs().getEsBaseConfigManager().IsShakeSelf();
    }
    mFocusSearchHelper = new FocusSearchHelper(this);
  }

  @Override
  public void onViewRemoved(View child) {
    super.onViewRemoved(child);
    if (defaultSectionPosition > -1 && defaultSectionPosition == searchChildIndex(child)) {
      defaultSectionPosition = -1;
    }
  }

  @Override
  public void onViewAdded(View child) {
    super.onViewAdded(child);
    if (defaultSectionPosition > -1 && defaultSectionPosition == searchChildIndex(child)) {
      changeSelectState(defaultSectionPosition,true);
    }

  }

  //	@Override
  //	protected void onLayout(boolean changed, int l, int t, int r, int b)
  //	{
  //
  //	}

  //	@Override
  //	public void requestLayout()
  //	{
  //		//super.requestLayout();
  //	}

  @SuppressWarnings("SuspiciousNameCombination")
  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (mOverflow != null) {
      switch (mOverflow) {
        case "visible":
          if (mOverflowPath != null) {
            mOverflowPath.rewind();
          }
          restoreLayerType();
          break;
        case "hidden":
          if (mBGDrawable != null) {
            float left = 0f;
            float top = 0f;
            float right = getWidth();
            float bottom = getHeight();
            float borderWidth;
            if (mBGDrawable.getBorderWidthArray() != null
              && mBGDrawable.getBorderWidthArray()[0] != 0f) {
              borderWidth = mBGDrawable.getBorderWidthArray()[0];
              left += borderWidth;
              top += borderWidth;
              right -= borderWidth;
              bottom -= borderWidth;
            }
            float radius =
              mBGDrawable.getBorderRadiusArray() != null ? mBGDrawable.getBorderRadiusArray()[0]
                : 0f;

            if (radius > 0f) {
              if (mOverflowPath == null) {
                mOverflowPath = new Path();
              }
              mOverflowPath.rewind();
              if (mOverflowRect == null) {
                mOverflowRect = new RectF();
              }
              mOverflowRect.set(left, top, right, bottom);
              mOverflowPath.addRoundRect(mOverflowRect, radius, radius, Path.Direction.CW);
              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (mOldLayerType == LAYER_TYPE_NOT_SET) {
                  mOldLayerType = this.getLayerType();
                }
                this.setLayerType(LAYER_TYPE_SOFTWARE, null);
              }
              try {
                canvas.clipPath(mOverflowPath);
              } catch (Throwable throwable) {
                restoreLayerType();
              }
            }
          }
          break;
        default:
          restoreLayerType();
          break;
      }
    }

    super.dispatchDraw(canvas);

    //zhaopeng add
    if (mBringToFrontOnFocus && getFocusedChild() != null) {
      super.drawChild(canvas, getFocusedChild(), getDrawingTime());
    }
//    if (mScroller != null) {
//      mScroller.computeScrollOffset();
//      int cx = getScrollX();
//      int cy = getScrollY();
//      if(cx != mScroller.getCurrX() || cy != mScroller.getCurrY()){
//        postInvalidateDelayed(16);
//        scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
//      }
//    }

    if (overFlowViewIndex > -1 && overFlowViewIndex < getChildCount()) {
      final View v = getChildAt(overFlowViewIndex);
      if (v != null) {
        super.drawChild(canvas, v, getDrawingTime());
      }
    }
    //        String testString = "View ID:" + this.getId();
    //        Paint mPaint = new Paint();
    //        mPaint.setStrokeWidth(3);
    //        mPaint.setTextSize(40);
    //        mPaint.setColor(Color.RED);
    //        mPaint.setTextAlign(Paint.Align.LEFT);
    //        Rect bounds = new Rect();
    //        mPaint.getTextBounds(testString, 0, testString.length(), bounds);
    //        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
    //        int baseline = (getMeasuredHeight() - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
    //        canvas.drawText(testString, getMeasuredWidth() / 2 - bounds.width() / 2, baseline, mPaint);

    if (needLayoutFromCustom) {
      onMeasureCustom();
      needLayoutFromCustom = false;
    }

  }

  int[] preferSize = new int[4];

  protected void onMeasureCustom() {
    if (LogUtils.isDebug()) {
      Log.e(CustomLayoutView.TAG, "layoutCustom on DispatchDraw view:" + this);
    }
    for (int i = 0; i < getChildCount(); i++) {
      final View child = getChildAt(i);
      if (child instanceof CustomNodeView) {
        ((CustomNodeView) child).getNodeSize(preferSize);
        if (preferSize[2] > -1 || preferSize[3] > -1) {
          RenderUtil.reLayoutView(child, preferSize[0], preferSize[1], preferSize[2], preferSize[3]);
        }
      } else if (child != null) {
        RenderUtil.requestNodeLayout2(getContext(), child.getId());
      }
    }
  }


  private void restoreLayerType() {
    if (mOldLayerType > LAYER_TYPE_NOT_SET) {
      this.setLayerType(mOldLayerType, null);
    }
  }



  //	public void setBorderRadius(float radius, int position)
  //	{
  //		getBackGround().setBorderRadius(radius, position);
  //	}
  //
  //	public void setBorderWidth(float width, int position)
  //	{
  //		getBackGround().setBorderWidth(width, position);
  //	}
  //
  //	public void setBorderColor(int color, int position)
  //	{
  //		getBackGround().setBorderColor(color, position);
  //	}
  //
  //	private CommonBackgroundDrawable getBackGround()
  //	{
  //		if (mBGDrawable == null)
  //		{
  //			mBGDrawable = new CommonBackgroundDrawable();
  //			Drawable currBGDrawable = getBackground();
  //			super.setBackgroundDrawable(null);
  //			if (currBGDrawable == null)
  //			{
  //				super.setBackgroundDrawable(mBGDrawable);
  //			}
  //			else
  //			{
  //				LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] { mBGDrawable, currBGDrawable });
  //				super.setBackgroundDrawable(layerDrawable);
  //			}
  //		}
  //		return mBGDrawable;
  //	}

  public void smoothScrollBy(int x,int y){
    //Fixme 这里初步实现了无动画的滚动，后续支持动画
//    if (mScroller == null) {
//      mScroller = new Scroller(getContext());
//    }
//    mScroller.startScroll(getScrollX(),getScrollY(),x,y,200);
////    scrollBy(x,y);
//    postInvalidateDelayed(16);
  }



  public void setOverflow(String overflow) {
    mOverflow = overflow;
    //robinsli Android 支持 overflow: visible，超出容器之外的属性节点也可以正常显示
    if (!TextUtils.isEmpty(mOverflow)) {
      switch (mOverflow) {
        case "visible":
          setClipChildren(false); //可以超出父亲区域
          break;
        case "hidden": {
          setClipChildren(true); //默认值是false
          break;
        }
      }
    }
    invalidate();
  }

  //	@Override
  //	public void setBackgroundColor(int color)
  //	{
  //		getBackGround().setBackgroundColor(color);
  //	}

  //	@Override
  //	public boolean onTouchEvent(MotionEvent event)
  //	{
  //		boolean result = super.onTouchEvent(event);
  //		if (mGestureDispatcher != null)
  //		{
  //			result |= mGestureDispatcher.handleTouchEvent(event);
  //		}
  //		return result;
  //	}

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    int action = ev.getAction() & MotionEvent.ACTION_MASK;
    if (action == MotionEvent.ACTION_DOWN) {
      mDownX = ev.getX();
      mDownY = ev.getY();
      isHandlePullUp = false;
    }

    boolean result = super.onInterceptTouchEvent(ev);

    if (mGestureDispatcher != null) {
      result |= mGestureDispatcher.needHandle(NodeProps.ON_INTERCEPT_TOUCH_EVENT);
    }

    if (!result && mGestureDispatcher != null && mGestureDispatcher
      .needHandle(NodeProps.ON_INTERCEPT_PULL_UP_EVENT)) {
      //noinspection SwitchStatementWithTooFewBranches
      switch (action) {
        case MotionEvent.ACTION_MOVE: {
          if (isHandlePullUp) {
            break;
          }
          if (mViewConfiguration == null) {
            //noinspection deprecation
            mViewConfiguration = new ViewConfiguration();
          }
          float dx = ev.getX() - mDownX;
          float dy = ev.getY() - mDownY;
          if (dy < 0 && Math.abs(dx) < Math.abs(dy) && Math.abs(dy) > mViewConfiguration
            .getScaledTouchSlop()) {
            mGestureDispatcher.handle(NodeProps.ON_TOUCH_DOWN, mDownX, mDownY);
            isHandlePullUp = true;
          }
          break;
        }
      }
      result = isHandlePullUp;
    }
    return result;
  }

  //	@Override
  //	public NativeGestureDispatcher getGestureDispatcher()
  //	{
  //		return mGestureDispatcher;
  //	}

  //	@Override
  //	public void setGestureDispatcher(NativeGestureDispatcher dispatcher)
  //	{
  //		mGestureDispatcher = dispatcher;
  //	}

  @Override
  protected int getChildDrawingOrder(int childCount, int index) {
    return mDrawingOrderHelper.getChildDrawingOrder(childCount, index);
  }

  @Override
  public int getZIndexMappedChildIndex(int index) {
    if (mDrawingOrderHelper.shouldEnableCustomDrawingOrder()) {
      return mDrawingOrderHelper.getChildDrawingOrder(getChildCount(), index);
    } else {
      return index;
    }
  }

  @Override
  public void updateDrawingOrder() {
    mDrawingOrderHelper.update();
    setChildrenDrawingOrderEnabled(mDrawingOrderHelper.shouldEnableCustomDrawingOrder());
    invalidate();
  }

  @Override
  public void addView(View child, int index) {
    super.addView(child, index);
    mDrawingOrderHelper.handleAddView(child);
    setChildrenDrawingOrderEnabled(mDrawingOrderHelper.shouldEnableCustomDrawingOrder());
  }

  @Override
  public void removeView(View view) {
    super.removeView(view);
    mDrawingOrderHelper.handleRemoveView(view);
    setChildrenDrawingOrderEnabled(mDrawingOrderHelper.shouldEnableCustomDrawingOrder());
  }

  @Override
  public void onResetBeforeCache() {
    super.onResetBeforeCache();
    memoryFocused = null;
  }

  @Override
  public void resetProps() {
    //		HippyViewController.resetTransform(this);

    HippyViewGroupController.removeViewZIndex(this);

    //		mBGDrawable = null;
    //		super.setBackgroundDrawable(null);
    mOverflow = null;
    memoryFocused = null;
//    if(enableOverScrollY || enableOverScrollX) {
//      overScrollImmediately = true;
//      scrollTo(0, 0);
//      setEnableOverScrollX(false);
//      setEnableOverScrollY(false);
//    }
    setClipChildren(true); //默认值是false
    //		setAlpha(1.0f);
  }

  //	@Override
  //	public void clear()
  //	{
  //
  //	}

  public void setUseAdvancedFocusSearch(boolean useAdvancedFocusSearch) {
    this.useAdvancedFocusSearch = useAdvancedFocusSearch;
    LogUtils.d(TAG, "setUseAdvancedFocusSearch :" + useAdvancedFocusSearch + ",this:" + this);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

//    if (oldw == 0 || oldh == 0) {
//      overScrollImmediately = true;
//    }
    if (mBackDrawable != null) {
      mBackDrawable.setBounds(0, 0, w, h);
    }

  }

  @Override
  protected void onChangeShowOnState() {
    super.onChangeShowOnState();
  }

  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
  }


  @Override
  public void draw(Canvas canvas) {
    if (mBackDrawable != null) {
      if (mBackDrawable.getBounds().isEmpty()) {
        if (getBackgroundDrawable().getBounds().isEmpty()) {
          mBackDrawable.setBounds(new Rect(0, 0, getWidth(), getHeight()));
        } else {
          mBackDrawable.setBounds(getBackgroundDrawable().getBounds());
        }
      }
      mBackDrawable.draw(canvas);
    }
    super.draw(canvas);
//    Log.v("zhaopeng","draw called isFocused:"+isFocused());
  }

  public void setBringFocusChildToFront(boolean mBringToFrontOnFocus) {
    this.mBringToFrontOnFocus = mBringToFrontOnFocus;
    if(LogUtils.isDebug()){
      Log.i(TAG,"setBringFocusChildToFront this:"+this+",b : "+mBringToFrontOnFocus);
    }
    postInvalidateDelayed(16);
  }

  @Override
  public View focusSearch(View focused, int direction) {
    final View result = useAdvancedFocusSearch ? advanceFocusSearch(focused, direction) : super.focusSearch(focused, direction);
    final int nextId = InternalExtendViewUtil.findInterceptViewID(this, direction);
    if(LogUtils.isDebug()) {
      Log.i(FocusDispatchView.TAG, "focusSearch nextId :" + nextId + ",result:" + ExtendUtil.debugViewLite(result) + ",this:" + ExtendUtil.debugFocusInfo(this));
    }
    if (nextId > -1) {
      final boolean nextInGroup = TVViewUtil.isViewDescendantOf(result, this);
      if (!nextInGroup || result == null) {
        //
        if (shakeSelf) {
          shakeSelf(focused, direction);
        }
        return InternalExtendViewUtil.interceptViewGroupIfNeed(this,nextId, focused, result);
//        return null;
      }
    }
    return result;
  }



  private void markSpecifiedFocusSearch(View specialTarget) {
//        Log.d(TAG, "+mark SpecifiedFocusSearch  target : "+specialTarget)
    mSpecialFocusSearchRequest = specialTarget;
  }

  private void consumeSpecifiedFocusSearchRequest() {
    if (mSpecialFocusSearchRequest != null) {
//            Log.d(TAG, "-consume SpecifiedFocusSearchRequest")
      mSpecialFocusSearchRequest = null;
    }
  }

  private boolean isSpecifiedFocusSearch() {
    return mSpecialFocusSearchRequest != null;
  }

  private ArrayList<View> mTempFocusList = new ArrayList<>();

  protected View advanceFocusSearch(View focused, int direction) {
    return this.advanceFocusSearch(focused, direction, false);
  }

  protected View advanceFocusSearch(View focused, int direction, boolean justSpecial) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "-----------------begin : focusSearch with focused : $focused direction : $direction -------------");
      Log.d(FocusDispatchView.TAG, "-----------------begin : advanceFocusSearch in div  with focused : $focused direction : $direction -------------");
    }
    //return super.focusSearch(focused, direction)

    //寻找结果
    View found = null;
    //用户自定义的目标view
    View userSpecifiedTarget = null;
    //用户自定义的目标viewID
    String userSpecifiedTargetName = null;
    String userSpecifiedTargetSID = null;
    //当前焦点的parent
    ViewGroup focusedParent = null;

    try {

      FocusUtils.FocusParams fp = null;
      //1, find user specified
      if (focused != null) {
        //发现用户指定targetView，则直接调用此view的addFocusables方法
        fp = ExtendUtil.findUserSpecifiedNextFocusViewIdTraverse(this, focused, direction,this);
        if (LogUtils.isDebug()) {
          Log.i(FocusDispatchView.TAG,"div advance focus search fp :"+fp+",focused:"+ExtendUtil.debugViewLite(focused)+",this:"+ExtendUtil.debugViewLite(this));
        }
        userSpecifiedTargetName = fp.specifiedTargetViewName;
        userSpecifiedTargetSID = fp.specifiedTargetSID;
        userSpecifiedTarget = ExtendUtil.findViewByNameOrSID(focused,fp, this);
        if(LogUtils.isDebug()){
          Log.d(FocusDispatchView.TAG,"1 : pre find userSpecifiedTarget "+ExtendUtil.debugViewLite(userSpecifiedTarget));
        }
        if(userSpecifiedTarget != null){
          if(!userSpecifiedTarget.isFocusable()){
            if(Utils.isParentItemRootView(userSpecifiedTarget)){
              userSpecifiedTarget = Utils.getPlaceholderContainer(userSpecifiedTarget);
            }
          }
          if (LogUtils.isDebug()) {
            Log.e(FocusDispatchView.TAG, "advanceFocusSearch in div return userSpecifiedTarget FocusParams:" + fp + ",result:" + ExtendUtil.debugViewLite(userSpecifiedTarget));
          }
          if(userSpecifiedTarget != null && userSpecifiedTarget.getVisibility() != View.VISIBLE){
            userSpecifiedTarget = null;
          }
        }
        if (LogUtils.isDebug()) {
          Log.d(FocusDispatchView.TAG, "1 : find specifiedTargetViewName is " +
            "$userSpecifiedTargetName specifiedView is $userSpecifiedTarget name:" + userSpecifiedTargetName + ",userSpecifiedTargetSID:" + userSpecifiedTargetSID+",userSpecifiedTarget:"+ExtendUtil.debugViewLite(userSpecifiedTarget));
          if(userSpecifiedTarget instanceof ViewGroup) {
            Log.d(FocusDispatchView.TAG, "1 : userSpecifiedTarget visible :" + userSpecifiedTarget.getVisibility() + ",alpha :" + userSpecifiedTarget.getAlpha()
              + ",descendantFocusability isBlock:" + (((ViewGroup) userSpecifiedTarget).getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) + ",focusable :" + userSpecifiedTarget.isFocusable());
          }
        }
        if (focused.getParent() instanceof ViewGroup)
          focusedParent = (ViewGroup) focused.getParent();
      }

      //2 , find focus
      if (userSpecifiedTarget != null && userSpecifiedTarget.getVisibility() == View.VISIBLE) {

        if (fp.isPureSpecifiedTarget && ExtendUtil.isPureFocusView(userSpecifiedTarget)) {
          found = userSpecifiedTarget;
          if (LogUtils.isDebug()) {
            Log.d(FocusDispatchView.TAG, "2-0 : find userSpecifiedTarget is pure specified target  $found :" + ExtendUtil.debugViewLite(found));
          }
        }

        markSpecifiedFocusSearch(userSpecifiedTarget);
        // it.addFocusables(views,direction,focusableMode)
        //这里要区分出来userSpecifiedTarget是一个focusable的View，还是一个ViewGroup

        //1,优先在当前parent中寻找，以保证优先搜索比较近的view
        if (found == null) {
          if (focusedParent != userSpecifiedTarget && focusedParent != userSpecifiedTarget.getParent()) { //确保俩个parent不是同一个
            if (focusedParent != null)
              focusedParent.addFocusables(mTempFocusList, direction);
            //zhaopeng 20190327 由于这里focused不是focusedParent的子view时，会发生崩溃，所以这里try catch
            if(mTempFocusList.size() > 0) {
              found = ExtendUtil.executeFindNextFocus(focusedParent, focused, direction);
            }
            if (LogUtils.isDebug()) {
              Log.d(FocusDispatchView.TAG, "2-1: find from focusedParent found :  $found :" + ExtendUtil.debugViewLite(found));
            }
          }
        }
        //2， 从IFocusGroup中寻找

        //3， userSpecifiedTarget中寻找
        if (found == null) {
          mTempFocusList.clear();
          userSpecifiedTarget.addFocusables(mTempFocusList, direction);
//          for(int i = 0; i < mTempFocusList.size(); i++){
//            View v = mTempFocusList.get(i);
//            Log.i(FocusDispatchView.TAG,"2-3 : addFocusables view :"+ExtendUtil.debugView(v));
//          }
          if(mTempFocusList.size() > 0){
            found = ExtendUtil.executeFindNextFocus(this, focused, direction);
          }
          if (LogUtils.isDebug()) {
            Log.d(FocusDispatchView.TAG, "2-3 :  find from  userSpecifiedTarget  $found : " + ExtendUtil.debugViewLite(found));
          }
        }


        //4 这里处理用户是否拦截了焦点
        if (found == null) {
          if (focused != null) {
            // if(getParent() instanceof ViewGroup && getParent().id == userSpecifiedTargetId){
            if (ExtendUtil.sameDescend(this, focused, userSpecifiedTargetName,userSpecifiedTargetSID)) {
              // 这种情况下，用户将nextXXID 设置成本身，所以将focused返回
              if (LogUtils.isDebug()) {
                Log.d(FocusDispatchView.TAG, "2-4 : find from : sameDescend return focused "+ExtendUtil.debugViewLite(focused)+", userSpecifiedTargetName is "+userSpecifiedTargetName);
              }
//              if(userSpecifiedTargetName != null && userSpecifiedTargetName.equals(ExtendUtil.getViewName(focused))){
//                //found = focused;
//                //name设置成自身如果是name
//              }
              found = focused;
            }
          }
        }

        //5 这里只有一种情况，用户设置的焦点不太符合物理逻辑，所以不再考虑focused位置的情况下再次搜索
        if (found == null) {
//          Log.i(FocusDispatchView.TAG,"2-5 : mTempFocusList size  :"+mTempFocusList.size());
//          for(int i = 0; i < mTempFocusList.size(); i++){
//            View v = mTempFocusList.get(i);
//            Log.i(FocusDispatchView.TAG,"2-5 : addFocusables view :"+ExtendUtil.debugView(v));
//          }
          if(mTempFocusList == null || mTempFocusList.size() < 1 && (userSpecifiedTarget != null && !userSpecifiedTarget.isFocusable())){
            Log.e(FocusDispatchView.TAG,"2-5 : mTempFocusList is null or size is 0 skip search next focus");
          }else {
            mTempFocusList.clear();
            if (userSpecifiedTarget != null) {
              userSpecifiedTarget.addFocusables(mTempFocusList, direction);
              found = ExtendUtil.executeFindNextFocus(this, null, direction);
              if (LogUtils.isDebug()) {
                Log.d(FocusDispatchView.TAG, "2-5 : find without focused found is $found:" + ExtendUtil.debugViewLite(found));
              }
            }
          }
        }

      } else {
        if (LogUtils.isDebug()) {
          Log.d(FocusDispatchView.TAG, "2 : ** userSpecifiedTarget is NULL find from Root");
        }
        //2 为空时用户没有设定，从root中寻找
        if (!justSpecial) {
          super.addFocusables(mTempFocusList, direction);
          //3 find nextFocus from root
          // 注意： 此方法会调用this.addFocusables()
          found = ExtendUtil.executeFindNextFocus(this, focused, direction);
        }
        if (LogUtils.isDebug()) {
          Log.d(FocusDispatchView.TAG, "3 :  FocusFinder search from Root result is  $found " + found + ",justSpecial" + justSpecial);
        }
      }
    } finally {
      mTempFocusList.clear();
    }
    consumeSpecifiedFocusSearchRequest();
    if (LogUtils.isDebug()) {
      Log.d(FocusDispatchView.TAG, "-----------------end : focusSearch searched : $found-----------------");
    }
    if (justSpecial) {
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "-----------------end : justSpecial focusSearch return -----------------" + found);
      }
      return found;
    }
    if (found != null) {
//      Log.d(TAG, "-----------------end : focusSearch found!=null shakeSelf:" + shakeSelf);
      if ((found == focused || (userSpecifiedTargetName != null && userSpecifiedTarget == null)
        || (userSpecifiedTargetSID != null && userSpecifiedTarget == null)) && shakeSelf) {//todo 有bug
        if (LogUtils.isDebug()) {
          Log.e(FocusDispatchView.TAG, "-----------------end found = focused: shakeSelf-----------------");
        }
        shakeSelf(focused, direction);
      }
      return found;
    } else {
      View view = super.focusSearch(focused, direction);
//      Log.d(TAG, "-----------------end : focusSearch found==null shakeSelf:" + shakeSelf);
      if (view == null && shakeSelf) {
        if (LogUtils.isDebug()) {
          Log.e(FocusDispatchView.TAG, "-----------------end found = null: shakeSelf-----------------");
        }
        shakeSelf(focused, direction);
      }
      return view;
    }
//    return found != null ? found : super.focusSearch(focused,direction);
  }

  private boolean animRunning = false;

  private void shakeSelf(View view, int direction) {
    if (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP) {
      if (!animRunning) {
        animRunning = true;
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y.getName(), 0, 5f);//抖动幅度0到5
        objectAnimator.setDuration(250);//持续时间
        objectAnimator.setInterpolator(new CycleInterpolator(2));//抖动次数
        objectAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationCancel(Animator animation) {
            animRunning = false;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            animRunning = false;
          }
        });
        objectAnimator.start();//开始动画
      }
    } else {
      if (!animRunning) {
        animRunning = true;
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X.getName(), 0, 5f);//抖动幅度0到5
        objectAnimator.setDuration(250);//持续时间
        objectAnimator.setInterpolator(new CycleInterpolator(2));//抖动次数
        objectAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationCancel(Animator animation) {
            animRunning = false;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            animRunning = false;
          }
        });
        objectAnimator.start();//开始动画
      }
    }
  }


  private String firstFocusTargetName = null;

  public void setFirstFocusTargetName(String name) {
    this.firstFocusTargetName = name;
  }

  public HippyMap focusSearchTarget;

  public void setFocusSearchTarget(HippyMap map) {
    this.focusSearchTarget = map;
  }

  public static String getDirectionName(int direction) {
    return ExtendUtil.getDirectionName(direction);
  }

  public View findFocusTargetName(int direction) {
    if (focusSearchTarget != null) {
      final String key = getDirectionName(direction);
      if (key != null) {
        final String target = focusSearchTarget.getString(key);
        return ControllerManager.findViewByName(this, target);
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.e(TAG, "div: +addFocusables findFocusTargetName key is null :" + focusSearchTarget);
        }
      }
    }
    return null;
  }

  public View findNextSpecialFocusView(View focused, int direction) {
    return advanceFocusSearch(focused, direction, true);
  }

  @Override
  public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
    if(mFocusSearchHelper.addFocusables(views,direction)){
      return;
    }
    if (!hasFocus() && focusSearchTarget != null) {
      final View view = findFocusTargetName(direction);
      if (view != null) {
        if (LogUtils.isDebug()) {
          LogUtils.d(TAG, "div: +addFocusables by focusSearchTarget : " + direction + ",view :" + view.getId() + ",focusSearchTarget:" + focusSearchTarget);
        }
        view.addFocusables(views, direction, focusableMode);
        return;
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.d(TAG, "div: +addFocusables by focusSearchTarget : " + direction + ",view :" + null);
        }
      }
    }

//    Log.i(FocusDispatchView.TAG,"addFocusables hasFocus: "+hasFocus()+",this:"+ExtendUtil.debugView(this));
    if(!hasFocus() && focusMemoryEnable && memoryFocused != null){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        if(memoryFocused.getVisibility() == View.VISIBLE && memoryFocused.isAttachedToWindow()){
          if (LogUtils.isDebug()) {
            LogUtils.d(TAG, "div: +addFocusables on memoryFocused : " + direction + ",view :" + ExtendUtil.debugView(memoryFocused));
          }
          memoryFocused.addFocusables(views,direction,focusableMode);
          return;
        }
      }
    }

    if (!hasFocus() && firstFocusTargetName != null) {
      final View view = ControllerManager.findViewByName(this, firstFocusTargetName);
      if (view != null) {
        LogUtils.d(TAG, "div: +addFocusables by firstTargetName : " + firstFocusTargetName + ",view :" + view.getId());
        view.addFocusables(views, direction, focusableMode);
        return;
      }
    }
    if (useAdvancedFocusSearch && mTempFocusList != null && mTempFocusList.size() > 0) {
      if (LogUtils.isDebug()) Log.d(TAG, "+addFocusables views : $mTempFocusList");
      if (views != null) views.addAll(mTempFocusList);
    } else {
      super.addFocusables(views, direction, focusableMode);
    }

  }

  @Override
  public void addFocusables(ArrayList<View> views, int direction) {
    if (!hasFocus() && focusSearchTarget != null) {
      final View view = findFocusTargetName(direction);
      if (view != null) {
        LogUtils.d(TAG, "div: +addFocusables by focusSearchTarget : " + direction + ",view :" + view.getId());
        view.addFocusables(views, direction);
        return;
      } else {
        LogUtils.d(TAG, "div: +addFocusables by focusSearchTarget : " + direction + ",view :" + null);
      }
    }

    if (!hasFocus() && firstFocusTargetName != null) {
      final View view = ControllerManager.findViewByName(this, firstFocusTargetName);
      if (view != null) {
        LogUtils.d(TAG, "div: +addFocusables by firstTargetName : " + firstFocusTargetName + ",view :" + view.getId());
        view.addFocusables(views, direction);
        return;
      }
    }
    if (useAdvancedFocusSearch && mTempFocusList != null && mTempFocusList.size() > 0) {
      if (LogUtils.isDebug()) Log.d(TAG, "+addFocusables views : $mTempFocusList");
      if (views != null) views.addAll(mTempFocusList);
    } else {
      super.addFocusables(views, direction);
    }
  }

  private void postFocusChange(final boolean gainFocus, final int direction, final Rect previouslyFocusedRect) {
//    Log.d("zhaopeng","onFocusChange gainFocus:"+gainFocus+",view:"+this+"");

    if (mPostFocusTask != null) {
      removeCallbacks(mPostFocusTask);
    }

    if (isInReFocus()) {
      handleFocusScaleImmediately(gainFocus, direction, previouslyFocusedRect);
    } else {
      onHandleFocusScale(gainFocus, direction, previouslyFocusedRect);
    }

  }


  @Override
  protected void onDetachedFromWindow() {
//    Log.v("zhaopeng","------onDetachedFromWindow focused:"+isFocused());
    super.onDetachedFromWindow();
    if (isFocusable()) {
      TVFocusAnimHelper.changeFocusScaleDirectly(this, 1, 1);
    }
    if (isSelected()) {
      setSelected(false);
    }
    memoryFocused = null;

    stopListenGlobalFocusChange();

  }



  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
//    Log.v("zhaopeng","+++++++onAttachedToWindow focused:"+isFocused());

    listenGlobalFocusChangeIfNeed();
  }


  private Runnable mPostFocusTask;

  @Override
  protected void onFocusChanged(final boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    //border
//    if(isPostTaskEnabled()){
//      if(gainFocus) {
//        mPostHandlerView.postTask(TVListView.PostContentHolder.POST_TASK_CATEGORY_DIV_FOCUS_CHANGE, 1,
//          () -> postFocusChange(true, direction, previouslyFocusedRect), 300);
//      }else{
//        mPostHandlerView.clearTask(TVListView.PostContentHolder.POST_TASK_CATEGORY_DIV_FOCUS_CHANGE, 1);
//        postFocusChange(false, direction, previouslyFocusedRect);
//      }
//    }else{
    postFocusChange(gainFocus, direction, previouslyFocusedRect);
//    }
  }

  /**
   * zhaopeng add
   */
  public void setOverFlowViewIndex(int overFlowViewIndex) {
    this.overFlowViewIndex = overFlowViewIndex;
    postInvalidateDelayed(16);
  }

  private HippyMap scrollOverride;
  public void setScrollOverride(HippyMap map){
    //Log.i("ChildOnScreenScroller","setScrollOverride map:"+map+",this:"+this);
    this.scrollOverride = map;
  }

  public HippyMap getScrollOverride() {
    return scrollOverride;
  }

  public void setGradientDrawable(HippyMap map) {

    if (map == null) {
      mBackDrawable = null;
    } else {
      this.mBackDrawable = createGradientDrawable(this, map);

    }

    invalidate();
  }


  public static GradientDrawable createGradientDrawable(View view, HippyMap map) {
    int type = GradientDrawable.LINEAR_GRADIENT;
    if (map.containsKey("type")) {
      type = map.getInt("type");
    }
    int shape = GradientDrawable.RECTANGLE;
    if (map.containsKey("shape")) {
      shape = map.getInt("shape");
    }
    GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TOP_BOTTOM;

    if (map.containsKey("orientation")) {
      int orientationValue = map.getInt("orientation");
      switch (orientationValue) {
        case 1:
          orientation = GradientDrawable.Orientation.TR_BL;
          break;
        case 2:
          orientation = GradientDrawable.Orientation.RIGHT_LEFT;
          break;
        case 3:
          orientation = GradientDrawable.Orientation.BR_TL;
          break;
        case 4:
          orientation = GradientDrawable.Orientation.BOTTOM_TOP;
          break;
        case 5:
          orientation = GradientDrawable.Orientation.BL_TR;
          break;
        case 6:
          orientation = GradientDrawable.Orientation.LEFT_RIGHT;
          break;
        case 7:
          orientation = GradientDrawable.Orientation.TL_BR;
          break;
      }
    }

    HippyArray colorArray = map.getArray("colors");
    if (colorArray == null) {
      return null;
    }
    int[] colors = new int[colorArray.size()];

    try {
      for (int i = 0; i < colors.length; i++) {
        final String colorStr = colorArray.getString(i);
        colors[i] = Color.parseColor(colorStr);
        if (LogUtils.isDebug()) {
//          Log.d(TAG, "createGradientDrawable colorStr :" + colorStr + ",this:" + view.getId());
        }
      }
    } catch (Exception e) {
      if(LogUtils.isDebug()) {
        Log.e("hippy", "color parse fail createGradientDrawable return !");
      }
      return null;
    }

    final GradientDrawable g = new GradientDrawable(orientation, colors);
    g.setShape(shape);
    g.setGradientType(type);
    if (map.containsKey("gradientRadius")) {
      g.setGradientRadius(PixelUtil.dp2px(map.getInt("gradientRadius")));
    }
    if (map.containsKey("cornerRadius")) {
      g.setCornerRadius(PixelUtil.dp2px((float) map.getDouble("cornerRadius")));
    }
    if (map.containsKey("cornerRadii4")) {
      HippyArray array = map.getArray("cornerRadii4");
      if (array.size() != 4) {
        throw new IllegalArgumentException("cornerRadii4 size need 8");
      }
      g.setCornerRadii(new float[]{
        PixelUtil.dp2px((float) array.getDouble(0)), PixelUtil.dp2px((float) array.getDouble(0)),
          PixelUtil.dp2px((float) array.getDouble(1)), PixelUtil.dp2px((float) array.getDouble(1)),
            PixelUtil.dp2px((float) array.getDouble(2)), PixelUtil.dp2px((float) array.getDouble(2)),
              PixelUtil.dp2px((float) array.getDouble(3)), PixelUtil.dp2px((float) array.getDouble(3)),
      });
    }
    if (map.containsKey("cornerRadii8")) {
      HippyArray array = map.getArray("cornerRadii8");
      if (array.size() != 8) {
        throw new IllegalArgumentException("cornerRadii8 size need 8");
      }
      float[] radii = new float[array.size()];
      for (int i = 0; i < radii.length; i++) {
        radii[i] = PixelUtil.dp2px((float) (array.getDouble(i)));
      }
      g.setCornerRadii(radii);
    }
    return g;
  }

  @Override
  public boolean drawChild(Canvas canvas, View child, long drawingTime) {
    if (mBringToFrontOnFocus && child == getFocusedChild()) {
      return true;
    }
    if (overFlowViewIndex > -1 && overFlowViewIndex < getChildCount()) {
      final View v = getChildAt(overFlowViewIndex);
      if (child == v) {
        return true;
      }
    }
    return super.drawChild(canvas, child, drawingTime);
  }

  //zhaopeng add
  private boolean mEnableChildFocusEvent = false;
  private HippyViewEvent mChildFocusEvent;

  @Override
  public void setDispatchChildFocusEvent(boolean enable) {
    //
    this.mEnableChildFocusEvent = enable;
  }

//  ChildOnScreenScroller childOnScreenScroller;
  int[] tempOut = new int[2];



  @Override
  public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
    //return super.requestChildRectangleOnScreen(child, rectangle, immediate);
    //int scroll = ChildOnScreenScroller.getScrollToPositionOffset(-1,this,child, RecyclerView.VERTICAL,getChildCount(), TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID,0);
    if(enableOverScrollY || enableOverScrollX){
      getChildRectangleOnScreenScrollAmount(this,child,rectangle,immediate,tempOut);
      int sy = enableOverScrollY ?  tempOut[1] : 0;
      int sx = enableOverScrollX ?  tempOut[0] : 0;
      Log.i(TAG,"requestChildRectangleOnScreen child:"+ExtendUtil.debugViewLite(child)+",sy:"+sy+",sx:"+sx+",immediate:"+immediate);
      if(sy != 0 || sx != 0){
        if(!immediate) {
          smoothScrollBy(sx, sy);
        }else{
          scrollBy(sx,sy);
        }
        return true;
      }else{
        return false;
      }
    }
    return super.requestChildRectangleOnScreen(child, rectangle, immediate);
  }

   static int[] getChildRectangleOnScreenScrollAmount(View parent, View child, Rect rect, boolean immediate,int[] out) {
//    int[] out = new int[2];
    int parentLeft = parent.getPaddingLeft();
    int parentTop = parent.getPaddingTop();
    int parentRight = parent.getWidth() - parent.getPaddingRight();
    int parentBottom = parent.getHeight() - parent.getPaddingBottom();
    int childLeft = child.getLeft() + rect.left - child.getScrollX();
    int childTop = child.getTop() + rect.top - child.getScrollY();
    int childRight = childLeft + rect.width();
    int childBottom = childTop + rect.height();
    int offScreenLeft = Math.min(0, childLeft - parentLeft - parent.getScrollX());
    int offScreenTop = Math.min(0, childTop - parentTop - parent.getScrollY());
    int offScreenRight = Math.max(0, childRight - parentRight - parent.getScrollX());
    int offScreenBottom = Math.max(0, childBottom - parentBottom - parent.getScrollY());
    int dx;
//    if (this.getLayoutDirection() == 1) {
//      dx = offScreenRight != 0 ? offScreenRight : Math.max(offScreenLeft, childRight - parentRight);
//    } else {
    dx = offScreenLeft != 0 ? offScreenLeft : Math.min(childLeft - parentLeft, offScreenRight);
//    }

    int dy = offScreenTop != 0 ? offScreenTop : Math.min(childTop - parentTop, offScreenBottom);

    Log.d(HippyViewGroup.TAG,"childTop:"+child.getTop()+",rect:"+rect.top+",childScrollY:"+child.getScrollY()+",parentScrollY:"+parent.getScrollY());
    Log.d(HippyViewGroup.TAG,"getChildRectangleOnScreenScrollAmount dx : "+dx+",dy : "+dy+",offScreenLeft : "+offScreenLeft+",offScreenTop : "+offScreenTop+",offScreenRight : "+offScreenRight+",offScreenBottom : "+offScreenBottom+",childLeft : "+childLeft+",childTop : "+childTop+",childRight : "+childRight+",childBottom : "+childBottom+",parentLeft : "+parentLeft+",parentTop : "+parentTop+",parentRight : "+parentRight+",parentBottom : "+parentBottom);
    out[0] = dx;
    out[1] = dy;
    return out;
  }

  @Override
  public void requestChildFocus(View child, View focused) {
    super.requestChildFocus(child, focused);
    if (mEnableChildFocusEvent) {
      if (mChildFocusEvent == null) {
        mChildFocusEvent = new HippyViewEvent(InternalExtendViewUtil.CHILD_FOCUS_EVENT_NAME);
      }
      InternalExtendViewUtil.sendEventOnRequestChildFocus(this, child, focused, mChildFocusEvent);
    }
    if(getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
      LogAdapterUtils.log(getContext(), FocusDispatchView.TAG, "focus-> fail on BLOCK => " + ExtendUtil.debugViewLite(focused));
    }else{
      LogAdapterUtils.log(getContext(), FocusDispatchView.TAG, "focus-> " + ExtendUtil.debugViewLite(focused));
    }
    if (LogUtils.isDebug() && ExtendViewGroup.ROOT_TAG.equals(getTag(R.id.page_root_view))) {
      if(getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS){
        Log.e(FocusDispatchView.TAG, "GLOBAL_FOCUS TO VIEW fail on root view FOCUS_BLOCK_DESCENDANTS:" + ExtendUtil.debugView(focused));
      }else{
        Log.e(FocusDispatchView.TAG, "GLOBAL_FOCUS TO VIEW:" + ExtendUtil.debugView(focused));
      }
    }
//    ExtendUtil.logView(FocusDispatchView.TAG,focused);
    if (focusMemoryEnable) {
      memoryFocused = focused;
    }
    if(isPageRoot() && mAutoFocusManager != null){
      mAutoFocusManager.onRequestChildFocus(child, focused);
    }
    //Log.d("DebugDivSelect", "requestChildFocus enableSelectOnFocus:"+enableSelectOnFocus+",requestChildFocus this :"+ExtendUtil.debugViewLite(this));
    if (enableSelectOnFocus) {
      changeSelectState(defaultSectionPosition, false);
      defaultSectionPosition = searchChildIndex(child);
      //Log.d("DebugDivSelect","searchChildIndex:"+child+",defaultSectionPosition:"+defaultSectionPosition);
      changeSelectState(defaultSectionPosition, true);
    }
//    getchild

    postInvalidateDelayed(16);
    if(enableOverScrollY) {
     // Log.i(TAG, "enableOverScrollY : " + true + ",this:" + ExtendUtil.debugViewLite(this));
    }
    if(enableOverScrollX || enableOverScrollY) {
      View rectView = focused == null ? child : focused;
      this.mTempRect.set(0, 0, rectView.getWidth(), rectView.getHeight());
      requestChildRectangleOnScreen(rectView, this.mTempRect, true);
    }
  }

  public int searchChildIndex(View child){
    if (child.getParent() != this) {
      return -1;
    }
    for(int i = 0; i < getChildCount();i ++){
      if(getChildAt(i)== child){
        return i;
      }
    }
    return -1;
  }

  private Rect mTempRect = new Rect();


  @Override
  public View getHostView() {
    return this;
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);
  }


  private void stopListenGlobalFocusChange() {
    if (mOnGlobalFocusChangeListener != null) {
      getViewTreeObserver().removeOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
      mOnGlobalFocusChangeListener = null;
    }
  }

  public void setListenGlobalFocusChange(boolean listenGlobalFocusChange) {
    isListenGlobalFocusChange = listenGlobalFocusChange;
  }

  private void listenGlobalFocusChangeIfNeed() {
    stopListenGlobalFocusChange();
    if (isListenGlobalFocusChange) {
      mOnGlobalFocusChangeListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "onGlobalFocusChanged hasFocus : " + hasFocus() + " this :" + this);
          }
          if (hasFocus()) {
            if (oldFocus == null) {
              //首次获得焦点
              notifyViewFocusChanged(true, false, null, newFocus, false);
            } else {
              //焦点在内部，但上一个view不属于内部
              final boolean isOldFocusDescendantOf = TVViewUtil.isViewDescendantOf(oldFocus, HippyViewGroup.this);
              if (!isOldFocusDescendantOf) {
                notifyViewFocusChanged(true, false, oldFocus, newFocus, false);
              }
            }
          } else {
            final boolean isNewFocusDescendantOf = TVViewUtil.isViewDescendantOf(newFocus, HippyViewGroup.this);
            if (LogUtils.isDebug()) {
              Log.d(TAG, "onGlobalFocusChanged  hasFocus : " + hasFocus() + " isNewFocusDescendantOf : " + isNewFocusDescendantOf);
            }
            if (!isNewFocusDescendantOf) {
              //焦点丢失
              final boolean isOldFocusDescendantOf = TVViewUtil.isViewDescendantOf(oldFocus, HippyViewGroup.this);

              if (isOldFocusDescendantOf) {
                notifyViewFocusChanged(false, true, oldFocus, newFocus, true);
              }
            }
          }
        }
      };
      getViewTreeObserver().addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
    }
  }

  private boolean isListenGlobalFocusChange = false;
  private ViewTreeObserver.OnGlobalFocusChangeListener mOnGlobalFocusChangeListener;

  boolean lastFocusState = false;

  private void notifyViewFocusChanged(boolean hasFocus, boolean isOldFocusDescendantOf, View oldFocus, View focused, boolean loseFocus) {
    //
    Log.d(TAG, "notifyViewGroupFocusChanged lastFocusState != hasFocus:" + (lastFocusState != hasFocus) + ",loseFocus:" + loseFocus + ",isOldFocusDescendantOf:" + isOldFocusDescendantOf);

    if (lastFocusState != hasFocus) {
      onViewFocusChanged(hasFocus, focused);
      lastFocusState = hasFocus;
    }


  }


  protected HippyEngineContext getHippyContext() {
    return ((HippyInstanceContext) getContext()).getEngineContext();
  }

  protected void onViewFocusChanged(boolean hasFocus, View focused) {
    //
    if (LogUtils.isDebug()) {
      Log.d(TAG, "onViewGroupFocusChanged hasFocus : " + hasFocus + " this :" + this);
    }
    LogUtils.d(TAG, "onViewGroupFocusChanged context:" + getHippyContext());
    if (getHippyContext() != null) {
      if (hasFocus) {
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onFocusAcquired");
      } else {
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onFocusLost");
      }
    }
  }


  private int triggerKeyEventCode = -1;

  public void setTriggerKeyEvent(int keyCode) {
    this.triggerKeyEventCode = keyCode;
  }


  private void sendInterceptKeyJSEvent(KeyEvent event){
    if(interceptKeyJSEvent == null){
      interceptKeyJSEvent = new HippyViewEvent("onInterceptKeyEvent");
    }
    HippyMap map = new HippyMap();
    map.pushInt("keyCode",event.getKeyCode());
    map.pushInt("action",event.getAction());
    interceptKeyJSEvent.send(this,map);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if(interceptKeyEvent){
      Log.e(TAG,"dispatchKeyEventPreIme return on interceptKeyEvent this:"+this);
      sendInterceptKeyJSEvent(event);
      return true;
    }
    if(interceptKeyEvents != null){
      for(int i = 0; i <  interceptKeyEvents.size(); i ++){
        int key = interceptKeyEvents.getInt(i);
        if(event.getKeyCode() == key){
          sendInterceptKeyJSEvent(event);
          Log.e(TAG,"dispatchKeyEventPreIme return on interceptKeyEvent keyCode:"+key+",this:"+this);
          return true;
        }
      }
    }
    if (triggerKeyEventCode == event.getKeyCode()) {
      if (TriggerTaskManagerModule.dispatchTriggerTask(this, "onDispatchKeyEvent")) {
        return true;
      }
    }
    return super.dispatchKeyEvent(event);
  }

  private Rect clipOutset;

  public void setClipOutset(int left, int top, int right, int bottom) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (clipOutset == null) {
        clipOutset = new Rect();
      }
      clipOutset.set(left, top, right, bottom);
      requestLayout();
      setClipChildren(false);
    }
  }




  Rect temp;

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (LogUtils.isDebug()) {
      Log.d(CustomLayoutView.TAG, "onLayout id: " + getId() + ",width:" + getWidth() + ",height:" + getHeight() + ",this:" + this + ",l:" + l + ",t:" + t);
    }
    super.onLayout(changed, l, t, r, b);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (clipOutset != null) {
        if (temp == null) {
          temp = new Rect();
        }
        temp.set(-clipOutset.left, -clipOutset.top, getWidth() + clipOutset.right, getHeight() + clipOutset.bottom);

        setClipBounds(temp);
      }
    }
    if(isPageRoot()){
      //fixme 临时适配非16：9适配
      final int tx = EngineRootView.getScreenTranslateX();
      final int ty = EngineRootView.getScreenTranslateY();
      if(tx != 0 || ty != 0) {
        for (int i = 0; i < getChildCount(); i++) {
          final View v = getChildAt(i);
          v.layout(l + tx, t + ty, r + tx, b + ty);
        }
      }
    }
    else if(changed){
      //Log.i(CustomLayoutView.TAG,"onLayout changed this:"+this);
      for(int i = 0; i < getChildCount(); i ++){
        final View v = getChildAt(i);
        if(v instanceof HippyViewGroup){
          final HippyViewGroup cv = (HippyViewGroup) v;
          if(cv.isFillParentEnabled){
            Log.e(CustomLayoutView.TAG,"onLayout changed child isFillParentEnabled true:"+cv);
            cv.layout(l,t,r,b);
          }
        }
      }
    }
  }




  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//    if (LogUtils.isDebug()) {
//      Log.d(CustomLayoutView.TAG, "requestLayout id: " + getId() + ",MeasureWidth:" + getMeasuredWidth() + ",MeasureHeight:" + getMeasuredHeight() + ",this:" + this);
//    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  public void requestLayout() {
//    if(needLayoutFromCustom &&  getParent() instanceof CustomLayoutView){
//      ((CustomLayoutView) getParent()).setLayoutRequestFromCustom(true);
//    }
//    if (LogUtils.isDebug() && getId() != -1) {
//      Log.d(CustomLayoutView.TAG, "requestLayout id: " + getId() + ",this:" + this);
//    }
//    requestLayoutCustom();
    super.requestLayout();
//    if(needLayoutFromCustom) {
//      RenderUtil.addUpdateLayout(this);
//    }
  }


  protected void onLayoutCustom() {

  }

  public void requestLayoutCustom() {
    needLayoutFromCustom = true;
  }


  private boolean needLayoutFromCustom = false;


  @Override
  public boolean isLayoutRequested() {
    boolean b = super.isLayoutRequested();
//    if (LogUtils.isDebug() && getId() != -1) {
//      Log.v(CustomLayoutView.TAG, "isLayoutRequested id: " + getId() + ",super return:" + b + ",isLayoutRequestFromCustom:" + needLayoutFromCustom);
//    }
    if (isLayoutRequestFromCustom()) {
      return false;
    }
    return b;
  }

  @Override
  public void setLayoutRequestFromCustom(boolean b) {
    needLayoutFromCustom = b;
  }

  @Override
  public boolean isLayoutRequestFromCustom() {
    return needLayoutFromCustom;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
  }


  /**
   * zhaopeng add
   */

  public void setFocusScrollTarget(boolean enable) {
    this.isFocusScrollTarget = enable;
  }

  public boolean isFocusScrollTarget() {
    return isFocusScrollTarget;
  }

  private boolean isPageHidden = false;


  private boolean isFillParentEnabled = false;

  public void setFillParentEnabled(boolean fillParentEnabled) {
    isFillParentEnabled = fillParentEnabled;
  }

  protected void onPageHiddenChanged(boolean hidden) {
    this.isPageHidden = hidden;
    if (hidden) {
      stopListenGlobalFocusChange();
    } else {
      listenGlobalFocusChangeIfNeed();
    }
  }

  @Override
  public boolean isPageHidden() {
    return isPageHidden;
  }

  @Override
  public void changePageHidden(boolean hidden) {
    Log.i(TAG,"changePageHidden hidden:"+hidden+",this:"+ExtendUtil.debugViewLite(this));
    onPageHiddenChanged(hidden);
    for (int i = 0; i < getChildCount(); i++) {
      final View v = getChildAt(i);
      if (v instanceof ExtendViewGroup) {
        ((ExtendViewGroup) v).changePageHidden(hidden);
      }
    }
  }

  @Override
  public void setId(int id) {
    super.setId(id);
    if( ExtendViewGroup.ROOT_TAG.equals(getTag(R.id.page_root_view))) {
      //在Context注册
      if(getContext() instanceof HippyInstanceContext){
        ((HippyInstanceContext) getContext()).registerPageRoot(id);
      }
      if(LogUtils.isDebug()) {
        Log.i(TAG, "setId ：" + this + ",context:" + getContext());
        Log.e(AutoFocusManager.TAG, "rootView setId ：" + this + ",context:" + getContext());
      }
    }
  }

  protected void setAsRootView() {
    if(LogUtils.isDebug()) {
      Log.i(TAG, "setAsRootView ：" + this + ",context:" + getContext());
    }
    setTag(R.id.page_root_view, ExtendViewGroup.ROOT_TAG);
//    setEnableMouse(true);
  }


  /**
   * 从View的Context(必须是HippyInstanceContext)中寻找RootView
   * @param context
   * @return
   */
  public static View findPageRootViewFromContext(Context context){
    return ExtendUtil.findPageRootViewFromContext(context);
  }

  @SuppressLint("ResourceType")
  @Deprecated
  public static View findPageRootView(View v) {
    return ExtendUtil.findPageRootView(v);
  }

  @Deprecated()
  public static View findRootViewFromParent(View v){
    return ExtendUtil.searchRootViewTraverse(v);
  }

  @Override
  public void getState(@NonNull HippyMap map) {
    super.getState(map);

    int focusIndex = -1;
    if (getFocusedChild() != null && getChildCount() > 0) {
      focusIndex = indexOfChild(getFocusedChild());
    }
    map.pushInt("focusChildIndex", focusIndex);
    map.pushInt("childCount", getChildCount());
  }

  /**
   * zhaopeng add
   */

  public void setShakeSelf(boolean shakeSelf) {
    this.shakeSelf = shakeSelf;
  }

  //自动获取焦点的逻辑
  private AutoFocusManager mAutoFocusManager;
  public AutoFocusManager getAutoFocusManager(){
    if (mAutoFocusManager == null) {
      mAutoFocusManager = new AutoFocusManager(this);
    }
    return mAutoFocusManager;
  }

  boolean isPageRoot(){
    return ExtendViewGroup.ROOT_TAG.equals(getTag(R.id.page_root_view));
  }

  @Override
  public void onRequestAutofocus(View child, View target,int type) {
    if(LogUtils.isDebug()) {
      Log.d(AutoFocusManager.TAG, "--onRequestAutofocus child:" + child + ",target:" + ExtendUtil.debugView(target)+",blockFocus is :"+(getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS));
    }
    if(this.getVisibility() != View.VISIBLE){
      Log.e(AutoFocusManager.TAG,"onRequestAutofocus return on parent visibility != View.VISIBLE,"+ExtendUtil.debugView(this)+",target:"+ExtendUtil.debugView(target));
      return;
    }
//    if(getWidth() < 1 || getHeight() < 1){
//      Log.e(AutoFocusManager.TAG,"onRequestAutofocus return on parent size invalid "+ExtendUtil.debugView(this)+",target:"+ExtendUtil.debugView(target));
//      return;
//    }



    if(isPageRoot()){
      if(LogUtils.isDebug()) {
        Log.e(AutoFocusManager.TAG, "onRequestAutofocus requestFocus on PageRoot");
      }
      //AutoFocusManager.globalRequestFocus(target);
      final AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(this);
      if (af != null) {
        af.requestGlobalRequestFocus(this,target,type);
      }else{
        Log.e(AutoFocusManager.TAG, "onRequestAutofocus requestFocus on PageRoot af is null");
      }
    }else {
      if (getParent() instanceof ExtendViewGroup) {
        ExtendViewGroup parent = (ExtendViewGroup) getParent();
        parent.onRequestAutofocus(this, target,type);
      } else {
          if(LogUtils.isDebug()) {
            Log.i(AutoFocusManager.TAG, "onRequestAutofocus parent is not a instance of ExtendViewGroup parent: " + getParent());
          }
          final AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(this);
          if (af != null) {
            af.requestGlobalRequestFocus(this,target,type);
          }else{
            Log.e(AutoFocusManager.TAG, "onRequestAutofocus requestFocus on PageRoot af is null");
          }
      }
    }
  }

  public void enableFocusMemory(boolean enable) {
//    Log.i(TAG,"enableFocusMemory enable:"+enable+",this:"+ExtendUtil.debugView(this));
    this.focusMemoryEnable = enable;
    if (!enable) {
      memoryFocused = null;
    }
  }

  public void notifyDialogDivVisibleChange(HippyViewGroup dialogDiv,boolean isShow){

  }

  public void beforeDialogDivVisibleChange(HippyViewGroup dialogDiv,boolean isShow){

  }


  public void clearMemoryFocused(){
    memoryFocused = null;
  }

  public FocusSearchHelper getFirstFocusHelper() {
    return mFocusSearchHelper;
  }


  @Override
  public void setDescendantFocusability(int focusability) {
    final int last = getDescendantFocusability();
    super.setDescendantFocusability(focusability);
    if(LogUtils.isDebug()) {
      if(focusability == FOCUS_BLOCK_DESCENDANTS) {
        Log.e(FocusDispatchView.TAG, "block Focus , this:" + ExtendUtil.debugViewLite(this));
      }else if(last == FOCUS_BLOCK_DESCENDANTS){
        Log.i(FocusDispatchView.TAG, "release block , this:" + ExtendUtil.debugViewLite(this));
      }
    }
  }

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    boolean b = false;
    View view = getFirstFocusHelper().findFirstFocusChildByDirection(direction);
    if (view != null) {
      b =  view.requestFocus();
      if(LogUtils.isDebug()) {
        Log.i(FocusDispatchView.TAG, "!!!!!! div requestFocus by firstFocusHelper view " + ExtendUtil.debugViewLite(view) + ",this:" + ExtendUtil.debugViewLite(this));
      }
    }
    if(!b) {
      b = super.requestFocus(direction, previouslyFocusedRect);
      if(!b && LogUtils.isDebug()){
        final ViewParent blockedView = FocusUtils.findFocusBlockedParent(getParent());
        if(LogUtils.isDebug()) {
          Log.w(FocusDispatchView.TAG, "!!!!!! div requestFocus return " + false + ",this:" + ExtendUtil.debugViewLite(this));
          if (blockedView instanceof View) {
            Log.e(FocusDispatchView.TAG, "!!!!!! div requestFocus return by blockedView " + ExtendUtil.debugViewLite((View) blockedView));
          }
        }
      }
    }
    return b;
  }

  @Override
  public boolean onHoverEvent(MotionEvent event) {
    return super.onHoverEvent(event);
  }

  private int defaultSectionPosition = -1;
  /**
   * 设置选中的子view,注意此方法只在view已经显示出来以后调用才有效。
   *
   * @param position
   */
  public void setSelectChildPosition(int position, boolean changeTargetFocusChild) {
    //Log.i(TAG, "setSelectChildPosition position:" + position + " ,changeTargetFocusChild:" + changeTargetFocusChild + ",hasCode:" + hashCode() + ",enableSelectOnFocus:" + enableSelectOnFocus);
    final View selectedChild = findSelectedChild();
    if (selectedChild != null && selectedChild.isSelected()) {
      selectedChild.setSelected(false);
    }
//    if (changeTargetFocusChild) {
//      setTargetFocusChildPosition(position);
//    }
    this.defaultSectionPosition = position;
    if (!hasFocus() || !enableSelectOnFocus) {
      final View next = findViewByPosition(position);
      changeSelectState(next, true);
    }
  }

  private View findViewByPosition(int position) {
    if (getChildCount() > position && position > -1) {
      return getChildAt(position);
    }
    return null;
  }

  private boolean enableSelectOnFocus = false;
//  protected boolean enableFocusMemory = true;

  public void setEnableSelectOnFocus(boolean enableSelectOnFocus) {
    this.enableSelectOnFocus = enableSelectOnFocus;
    Log.d("DebugDivSelect","setEnableSelectOnFocus flag:"+enableSelectOnFocus+",this:"+ExtendUtil.debugViewLite(this));
  }

  public void clearSelectChild(){
    final View selectedChild = findSelectedChild() == null ? findViewByPosition(defaultSectionPosition) : findSelectedChild();
//    Log.i("CustomState","clearSelectChild selectedChild:"+ExtendUtil.debugViewLite(selectedChild)+" defaultSectionPosition:"+defaultSectionPosition+",singleSelectPosition:"+getControlProps().singleSelectPosition);
    if (selectedChild != null) {
      selectedChild.setSelected(false);
    }
    defaultSectionPosition = -1;
  }

  public View findSelectedChild() {
    return findViewByPosition(defaultSectionPosition);
  }

  //获取当前selection状态
  public int getSelectChildPosition() {
    return defaultSectionPosition;
  }


  void changeSelectState(final int position, boolean select) {
    final View child = findViewByPosition(position);
    //Log.i("DebugDivSelect","changeSelectState position:"+position+",select:"+select);
    if (child != null) {
      changeSelectState(child, select);
    }
  }

  void changeSelectState(View child, boolean select) {
//      if(child != null && child != getFocusedChild()){
    if (child != null) {
      if (select) {
        if (child.isSelected()) {
          child.setSelected(false);
        }
        child.setSelected(true);
      } else {
        child.setSelected(false);
      }
    }
  }

  @Override
  protected float getLeftFadingEdgeStrength() {
    return enableCustomFade ? mFadeRect.left : super.getLeftFadingEdgeStrength();
  }

  @Override
  protected float getRightFadingEdgeStrength() {
    return enableCustomFade ? mFadeRect.right :  super.getRightFadingEdgeStrength();
  }

  @Override
  protected float getTopFadingEdgeStrength() {
    return enableCustomFade ? mFadeRect.top :  super.getTopFadingEdgeStrength();
  }

  @Override
  protected float getBottomFadingEdgeStrength() {
    return enableCustomFade ? mFadeRect.bottom :  super.getBottomFadingEdgeStrength();
  }

  private final RectF mFadeRect = new RectF(); //[left,right,top,bottom ]
  private boolean enableCustomFade = false;
  private ValueAnimator mFadeStartAnimator,mFadeEndAnimator;


  /**
   *
   * @param length 渐变长度
   * @param vertical 纵向或者横向
   * @param startOrEnd 开始位置或者是结束位置
   * @param duration 时间
   * @param showOrDismiss true表示显示，false表示隐藏
   */
  public void smoothShowFade(int length,boolean vertical,boolean startOrEnd,int duration,boolean showOrDismiss){
    enableCustomFade = true;
//    if(showOrDismiss){
//      Log.i("DebugFade", "+++smoothShow length:" + length + ",vertical:" + vertical + ",startOrEnd:" + startOrEnd + ",duration:" + duration );
//    }else {
//      Log.i("DebugFade", "---smoothDismiss length:" + length + ",vertical:" + vertical + ",startOrEnd:" + startOrEnd + ",duration:" + duration );
//    }
    setFadingEdgeLength(length);
    if(vertical){
      setVerticalFadingEdgeEnabled(length > 0);
    }else{
      setHorizontalFadingEdgeEnabled(length > 0);
    }
    float current = 0;
    if(startOrEnd) {
      current = vertical ? mFadeRect.top : mFadeRect.left;
    }else{
      current = vertical ? mFadeRect.bottom : mFadeRect.right;
    }
    float target = showOrDismiss ? 1f : 0f;
    if (current == target) {
      return;
    }
    ValueAnimator animator = showOrDismiss ? ValueAnimator.ofFloat(current, 1f) : ValueAnimator.ofFloat(current, 0f);
    animator.setDuration(duration);
    if(startOrEnd){
      if(mFadeStartAnimator != null){
        mFadeStartAnimator.cancel();
      }
      mFadeStartAnimator = animator;
    }else{
      if(mFadeEndAnimator != null){
        mFadeEndAnimator.cancel();
      }
      mFadeEndAnimator = animator;
    }
    animator.addUpdateListener(animation -> {
      float value = (float) animation.getAnimatedValue();
//      Log.v("DebugFade","smoothShow value:" + value);
      if (vertical) {
        //setTranslationY(startOrEnd ? -length * value : length * value);
         if(startOrEnd){
            mFadeRect.top = value;
         }else{
            mFadeRect.bottom = value;
         }
      } else {
        if(startOrEnd){
          mFadeRect.left = value;
        }else{
          mFadeRect.right = value;
        }
      }
      postInvalidate();
    });
    animator.start();
    invalidate();
  }


}

