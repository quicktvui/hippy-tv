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
package com.tencent.mtt.hippy.views.scroll;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import android.support.annotation.NonNull;

import com.quicktvui.hippyext.AutoFocusManager;
import com.tencent.mtt.hippy.common.HippyMap;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.uimanager.ViewStateProvider;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.PixelUtil;

@SuppressWarnings("deprecation")
public class HippyVerticalScrollView extends ScrollView implements HippyViewBase, HippyScrollView , ExtendViewGroup, ViewStateProvider {

  private NativeGestureDispatcher mGestureDispatcher;

  private boolean mScrollEnabled = true;

  private boolean mDoneFlinging;

  private boolean mDragging;

  private final HippyOnScrollHelper mHippyOnScrollHelper;

  private boolean mScrollEventEnable = true;

  private boolean mScrollBeginDragEventEnable = false;

  private boolean mScrollEndDragEventEnable = false;

  private boolean mMomentumScrollBeginEventEnable = false;

  private boolean mMomentumScrollEndEventEnable = false;

  private boolean mFlingEnabled = true;

  private boolean mPagingEnabled = false;

  protected int mScrollEventThrottle = 400; // 400ms最多回调一次
  private long mLastScrollEventTimeStamp = -1;

  protected int mScrollMinOffset = 0;
  private int mLastY = 0;
  private int initialContentOffset = 0;
  private boolean hasCompleteFirstBatch = false;

  public HippyVerticalScrollView(Context context) {
    super(context);
    mHippyOnScrollHelper = new HippyOnScrollHelper();
    setVerticalScrollBarEnabled(false);
  }

  public void setScrollEnabled(boolean enabled) {
    this.mScrollEnabled = enabled;
  }

  @Override
  public void showScrollIndicator(boolean showScrollIndicator) {
    this.setVerticalScrollBarEnabled(showScrollIndicator);
  }

  public void setScrollEventThrottle(int scrollEventThrottle) {
    mScrollEventThrottle = scrollEventThrottle;
  }

  @Override
  public void callSmoothScrollTo(final int x, final int y, int duration) {
    if (duration > 0) {
      ValueAnimator realSmoothScrollAnimation =
          ValueAnimator.ofInt(getScrollY(), y);
      realSmoothScrollAnimation.setDuration(duration);
      realSmoothScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          int scrollTo = (Integer) animation.getAnimatedValue();
          HippyVerticalScrollView.this.scrollTo(x, scrollTo);
        }
      });
      realSmoothScrollAnimation.start();
    } else {
      smoothScrollTo(x, y);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(
        MeasureSpec.getSize(widthMeasureSpec),
        MeasureSpec.getSize(heightMeasureSpec));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    // Call with the present values in order to re-layout if necessary
    scrollTo(getScrollX(), getScrollY());
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction() & MotionEvent.ACTION_MASK;
    if (action == MotionEvent.ACTION_DOWN && !mDragging) {
      mDragging = true;
      if (mScrollBeginDragEventEnable) {
        HippyScrollViewEventHelper.emitScrollBeginDragEvent(this);
      }
      // 当手指触摸listview时，让父控件交出ontouch权限,不能滚动
      setParentScrollableIfNeed(false);
    } else if (action == MotionEvent.ACTION_UP && mDragging) {
      if (mScrollEndDragEventEnable) {
        HippyScrollViewEventHelper.emitScrollEndDragEvent(this);
      }
      // 当手指松开时，让父控件重新获取onTouch权限
      setParentScrollableIfNeed(true);
      mDragging = false;
    }

    boolean result = mScrollEnabled && super.onTouchEvent(event);
    if (mGestureDispatcher != null) {
      result |= mGestureDispatcher.handleTouchEvent(event);
    }
    return result;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    if (!mScrollEnabled) {
      return false;
    }
    if (super.onInterceptTouchEvent(event)) {
      if (mScrollBeginDragEventEnable) {
        HippyScrollViewEventHelper.emitScrollBeginDragEvent(this);
      }
      mDragging = true;
      return true;
    }
    return false;
  }

  // 设置父控件是否可以获取到触摸处理权限
  private void setParentScrollableIfNeed(boolean flag) {
    // 若自己能上下滚动
    if (canScrollVertically(-1) || canScrollVertically(1)) {
      getParent().requestDisallowInterceptTouchEvent(!flag);
    }
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
  protected void onScrollChanged(int x, int y, int oldX, int oldY) {
    super.onScrollChanged(x, y, oldX, oldY);
    if (mHippyOnScrollHelper.onScrollChanged(x, y)) {
      if (mScrollEventEnable) {
        long currTime = System.currentTimeMillis();
        int offsetY = Math.abs(y - mLastY);
        if (mScrollMinOffset > 0 && offsetY >= mScrollMinOffset) {
          mLastY = y;
        } else if ((mScrollMinOffset == 0) && (currTime - mLastScrollEventTimeStamp
            >= mScrollEventThrottle)) {
          mLastScrollEventTimeStamp = currTime;
        } else {
          return;
        }

        HippyScrollViewEventHelper.emitScrollEvent(this);
      }

      mDoneFlinging = false;
    }

  }

  @Override
  public void fling(int velocityY) {
    if (!mFlingEnabled) {
      return;
    }

    if (mPagingEnabled) {
      smoothScrollToPage(velocityY);
    } else {
      super.fling(velocityY);
    }

    if (mMomentumScrollBeginEventEnable) {
      HippyScrollViewEventHelper.emitScrollMomentumBeginEvent(this);
    }
    Runnable runnable = new Runnable() {
      private boolean mSnappingToPage = false;

      @Override
      public void run() {
        if (mDoneFlinging) {
          boolean doneWithAllScrolling = true;
          if (mPagingEnabled && !mSnappingToPage) {
            mSnappingToPage = true;
            smoothScrollToPage(0);
            doneWithAllScrolling = false;
          }

          if (doneWithAllScrolling) {
            if (mMomentumScrollEndEventEnable) {
              HippyScrollViewEventHelper.emitScrollMomentumEndEvent(HippyVerticalScrollView.this);
            }
          } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
              postOnAnimationDelayed(this, HippyScrollViewEventHelper.MOMENTUM_DELAY);
            } else {
              HippyVerticalScrollView.this.getHandler()
                  .postDelayed(this, 16 + HippyScrollViewEventHelper.MOMENTUM_DELAY);
            }
          }
        } else {
          mDoneFlinging = true;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postOnAnimationDelayed(this, HippyScrollViewEventHelper.MOMENTUM_DELAY);
          } else {
            HippyVerticalScrollView.this.getHandler()
                .postDelayed(this, HippyScrollViewEventHelper.MOMENTUM_DELAY);
          }
        }
      }
    };
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      postOnAnimationDelayed(runnable, HippyScrollViewEventHelper.MOMENTUM_DELAY);
    } else {
      this.getHandler().postDelayed(runnable, HippyScrollViewEventHelper.MOMENTUM_DELAY);
    }
  }

  private void smoothScrollToPage(int velocity) {
    int height = getHeight();
    int currentY = getScrollY();
    int predictedY = currentY + velocity;
    int page = 0;
    if (height != 0) {
      page = currentY / height;
    }

    if (predictedY > page * height + height / 2) {
      page = page + 1;
    }
    smoothScrollTo(getScrollX(), page * height);
  }

  public void setScrollEventEnable(boolean enable) {
    this.mScrollEventEnable = enable;
  }

  public void setScrollBeginDragEventEnable(boolean enable) {
    this.mScrollBeginDragEventEnable = enable;
  }

  public void setScrollEndDragEventEnable(boolean enable) {
    this.mScrollEndDragEventEnable = enable;
  }

  public void setMomentumScrollBeginEventEnable(boolean enable) {
    this.mMomentumScrollBeginEventEnable = enable;
  }

  public void setMomentumScrollEndEventEnable(boolean enable) {
    this.mMomentumScrollEndEventEnable = enable;
  }

  public void setFlingEnabled(boolean flag) {
    this.mFlingEnabled = flag;
  }

  @Override
  public void setContentOffset4Reuse(HippyMap offsetMap) {
    double offset = offsetMap.getDouble("y");
    scrollTo(0, (int) PixelUtil.dp2px(offset));
  }

  @Override
  public void setPagingEnabled(boolean pagingEnabled) {
    mPagingEnabled = pagingEnabled;
  }

  @Override
  public void setScrollMinOffset(int scrollMinOffset) {
    scrollMinOffset = Math.max(5, scrollMinOffset);
    mScrollMinOffset = (int) PixelUtil.dp2px(scrollMinOffset);
  }

  @Override
  public void setInitialContentOffset(int offset) {
    initialContentOffset = offset;
  }

  @Override
  public void scrollToInitContentOffset() {
    if (hasCompleteFirstBatch) {
      return;
    }

    if (initialContentOffset > 0) {
      scrollTo(0, initialContentOffset);
    }

    hasCompleteFirstBatch = true;
  }

  //zhaopeng add
  @Override
  protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
    if(scrollType == SCROLL_TYPE_NONE){
      return 0;
    }
    if(mScrollType == SCROLL_TYPE_CENTER) {
      if (getChildCount() == 0) return 0;
      int height = getHeight();
      int screenTop = getScrollY();
      int childCenter = rect.bottom;
      int parentCenter = (int) (screenTop + height * 0.5f);
      return childCenter - parentCenter;
    }else{
      return super.computeScrollDeltaToGetChildRectOnScreen(rect);
    }
  }

  protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect, View focused) {
    if(scrollType == SCROLL_TYPE_NONE){
      return 0;
    }

    if(mScrollType == SCROLL_TYPE_CENTER) {
      if (getChildCount() == 0) return 0;
      int height = getHeight();
      int screenTop = getScrollY();
      int childCenter = (int) (rect.top + focused.getHeight() * 0.5f);
      int parentCenter = (int) (screenTop + height * 0.5f);
      return childCenter - parentCenter;
    }else{
      return super.computeScrollDeltaToGetChildRectOnScreen(rect);
    }
  }

  @Override
  public boolean arrowScroll(int direction) {
    if(mScrollType == SCROLL_TYPE_CENTER){
      View currentFocused = findFocus();
      if (currentFocused == this) currentFocused = null;

      View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

      if(nextFocused != null){
        nextFocused.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(nextFocused, mTempRect);
        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect,nextFocused);
        doScrollY(scrollDelta);
        nextFocused.requestFocus(direction);
        return true;
      }else{
        return false;
      }
    }else {
      return super.arrowScroll(direction);
    }
  }



  private int mScrollType = SCROLL_TYPE_CENTER;

  private final Rect mTempRect = new Rect();

  private void doScrollY(int delta) {
    if (delta != 0) {
      smoothScrollBy(0, delta);
    }
  }

  public void setScrollType(int type){
    this.mScrollType = type;
  }


  private boolean mEnableChildFocusEvent = false;
  private HippyViewEvent mChildFocusEvent;
  @Override
  public void setDispatchChildFocusEvent(boolean enable) {
    //
    this.mEnableChildFocusEvent = enable;
  }


  @Override
  public void requestChildFocus(View child, View focused) {
    super.requestChildFocus(child, focused);
    if(mEnableChildFocusEvent) {
      if (mChildFocusEvent == null) {
        mChildFocusEvent = new HippyViewEvent(InternalExtendViewUtil.CHILD_FOCUS_EVENT_NAME);
      }
      InternalExtendViewUtil.sendEventOnRequestChildFocus(this, child, focused, mChildFocusEvent);
    }
  }

  private int scrollType = HippyScrollView.SCROLL_TYPE_DEFAULT;
  @Override
  public void setRequestChildOnScreenType(int type) {
    this.scrollType = type;
  }

  private boolean advancedFocusSearch = true;

  @Override
  public void setAdvancedFocusSearch(boolean b) {
    advancedFocusSearch = b;
  }

  @Override
  public boolean executeKeyEvent(KeyEvent event) {
    return !advancedFocusSearch && super.executeKeyEvent(event);
  }


  /** zhaopeng add*/
  private boolean isPageHidden = false;
  protected void onPageHiddenChanged(boolean hidden){
    this.isPageHidden = hidden;
  }

  @Override
  public boolean isPageHidden() {
    return isPageHidden;
  }

  @Override
  public void changePageHidden(boolean hidden) {
    onPageHiddenChanged(hidden);
    for(int i = 0;i < getChildCount(); i ++){
      final View v = getChildAt(i);
      if(v instanceof ExtendViewGroup){
        ((ExtendViewGroup) v).changePageHidden(hidden);
      }
    }
  }

  @Override
  public void getState(@NonNull HippyMap map) {
    map.pushInt("sy",getScrollY());
    map.pushInt("sx",getScrollX());
  }

  @Override
  public void onRequestAutofocus(View child, View target, int type) {
    if(this.getVisibility() != View.VISIBLE){
      Log.e(AutoFocusManager.TAG,"onRequestAutofocus return on parent visibility != View.VISIBLE,"+ExtendUtil.debugView(this)+",target:"+ExtendUtil.debugView(target));
      return;
    }

    if (getParent() instanceof ExtendViewGroup) {
      ExtendViewGroup parent = (ExtendViewGroup) getParent();
      parent.onRequestAutofocus(this, target,type);
    } else {
      Log.i(AutoFocusManager.TAG, "onRequestAutofocus parent is not a instance of ExtendViewGroup parent: " + getParent());
      //AutoFocusManager.globalRequestFocus(target);
      final AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(this);
      if (af != null) {
        af.requestGlobalRequestFocus(this,target,type);
      }else{
        Log.e(AutoFocusManager.TAG, "onRequestAutofocus requestFocus on PageRoot af is null");
      }
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
    return super.requestFocus(direction, previouslyFocusedRect);
  }
}
