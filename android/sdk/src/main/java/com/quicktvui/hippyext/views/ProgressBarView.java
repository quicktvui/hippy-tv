package com.quicktvui.hippyext.views;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.view.MotionEvent;

import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.quicktvui.base.ui.StateView;
import com.tencent.mtt.hippy.utils.ExtendUtil;

class ProgressBarView extends android.widget.ProgressBar implements HippyViewBase, StateView {

  private NativeGestureDispatcher mGestureDispatcher;

  public ProgressBarView(Context context,int style) {
    super(context,null,style);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    boolean result = super.onTouchEvent(event);
    if (mGestureDispatcher != null)
    {
      result |= mGestureDispatcher.handleTouchEvent(event);
    }
    return result;
  }

  @Override
  public NativeGestureDispatcher getGestureDispatcher()
  {
    return mGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher)
  {
    mGestureDispatcher = dispatcher;
  }


  private int[] showOnState;

  @Override
  public void setShowOnState(int[] showOnState) {
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
    refreshDrawableState();
  }

  @Override
  public void setShowOnCustomState(String[] showOnState) {
    this.showOnStateCustom = showOnState;
  }

  /**custom showOnState
   * ----------------
   */

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    int[] states = getDrawableState();

    ExtendUtil.handleShowOnState(this,states,showOnState);
    ExtendUtil.handleCustomShowOnState(this, mCustomStateList,showOnStateCustom);
  }
}
