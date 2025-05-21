package com.quicktvui.hippyext.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.base.ui.ICoverFlow;
import com.quicktvui.base.ui.TVFocusAnimHelper;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.views.scroll.HippyHorizontalScrollView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

public class CoverFlowHorizontalView extends HippyHorizontalScrollView implements ICoverFlow {

  public CoverFlowHorizontalView(Context context) {
    super(context);
  }

  private int mDefaultScrollDuration = 300;
  private int mDefaultAutoScrollInterval = 2000;
  public float mDefaultZoomInValue = 1.5f;

  private int mCurrentPosition = 0;
  private AutoScroll mAutoScroll;

  @Override
  public void setAutoScrollInterval(int autoScrollInterval) {
    this.mDefaultAutoScrollInterval = autoScrollInterval;
    removeCallbacks(mAutoScroll);
    scrollToPosition(mCurrentPosition, 100, mDefaultScrollDuration);
  }

  @Override
  public void setZoomInValue(float zoomInValue) {
    this.mDefaultZoomInValue = zoomInValue;
  }

  @Override
  public int getCurrentIndex() {
    return mCurrentPosition;
  }

  @Override
  public void scrollToIndex(int index, int duration) {
    scrollToPosition(index, 0, duration);
  }

  private class AutoScroll implements Runnable{

    final int position;
    final int duration;

    private AutoScroll(int position, int duration) {
      this.position = position;
      this.duration = duration;
    }

    @Override
    public void run() {
      if(!hasFocus()){
        smoothScrollToPosition(position, duration);
        postInvalidateDelayed(16);
        mCurrentPosition = position;
        if(mDefaultAutoScrollInterval > 0){
          scrollToPosition(mCurrentPosition + 1, mDefaultAutoScrollInterval, mDefaultScrollDuration);
        }
      }
    }
  }

  Rect mTempRect = new Rect();

  View mCurrentChild = null;

  private void smoothScrollToPosition(int position,int duration){
    final View firstChild = getItemViewAt(0);
    if(firstChild == null){
      Log.e("ScrollView","smoothScrollToPosition error firstChild is null");
      return ;
    }
    if(mCurrentChild != null){
      TVFocusAnimHelper.handleOnFocusChange(mCurrentChild,false,1f,1f,300, R.id.tag_cover_flow_animation);
    }
    final View target = getItemViewAt(position);
    mCurrentChild = target;
    TVFocusAnimHelper.handleOnFocusChange(target,true, mDefaultZoomInValue, mDefaultZoomInValue,300, R.id.tag_cover_flow_animation);
    if(target != null){

      if(getContainer() instanceof HippyViewGroup){
        ((HippyViewGroup)getContainer()).setOverFlowViewIndex(position);
      }
      mTempRect.setEmpty();
      offsetDescendantRectToMyCoords(target,mTempRect);
      int parentCenter = (int) (getWidth() * 0.5f);
      final int targetX = (int) (mTempRect.left + target.getWidth() * 0.5f) - parentCenter;
      if(duration > 0) {
        callSmoothScrollTo(targetX, 0,duration);
      }else{
        scrollTo(targetX,0);
      }
    }
  }

  @Override
  public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
    return super.requestChildRectangleOnScreen(child, rectangle, immediate);
  }

  @Override
  public void requestChildFocus(View child, View focused) {
    super.requestChildFocus(child, focused);
  }

  private int getItemCount(){
    if(getChildCount() < 1){
      return 0;
    }
    return ((ViewGroup)getChildAt(0)).getChildCount();
  }

  private ViewGroup getContainer(){
    if(getChildCount() < 1){
      return null;
    }
    return (ViewGroup) getChildAt(0);
  }

  private View getItemViewAt(int index){
    if(getItemCount() > index && index > -1){
      return getContainer().getChildAt(index);
    }
    return null;
  }

  private void stopAutoScroll(){
    if(mAutoScroll != null){
      removeCallbacks(mAutoScroll);
    }
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
//    if( autoScrollInterval > 0 ) {
//      if( getFocusedChild()  != null) {
//        super.drawChild(canvas, getFocusedChild(), getDrawingTime());
//      }else if(mCurrentChild != null){
//        super.drawChild(canvas, mCurrentChild, getDrawingTime());
//      }
//    }
  }

  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    if(getFocusedChild() == child || child == mCurrentChild){
      return true;
    }
    return super.drawChild(canvas, child, drawingTime);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    scrollToPosition(mCurrentPosition, 100, 0);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopAutoScroll();
    mCurrentChild = null;
  }

  private void scrollToPosition(int position, int delay, int duration){
    stopAutoScroll();
    if(position < 0) position = getItemCount() - 1;
    else if(position > getItemCount() - 1) position = 0;
    mAutoScroll = new AutoScroll(position, duration);
    postDelayed(mAutoScroll, delay);
  }

  private float mLastTouchPos;

  @Override
  public boolean dispatchTouchEvent(MotionEvent e) {
    int action = e.getAction() & MotionEvent.ACTION_MASK;
    if(action == MotionEvent.ACTION_DOWN){
      mLastTouchPos = e.getX();
    }else if(action == MotionEvent.ACTION_UP){
      float delta = e.getX() - mLastTouchPos;
      if(delta > 0){
        scrollToPosition(mCurrentPosition - 1, 0, mDefaultScrollDuration);
      }else if(delta < 0){
        scrollToPosition(mCurrentPosition + 1, 0, mDefaultScrollDuration);
      }
    }
    return true;
  }
}
