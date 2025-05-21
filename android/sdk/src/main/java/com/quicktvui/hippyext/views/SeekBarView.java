package com.quicktvui.hippyext.views;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.SeekBar;

import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.quicktvui.base.ui.StateView;
import com.tencent.mtt.hippy.utils.ExtendUtil;

public class SeekBarView extends SeekBar implements HippyViewBase, StateView {

  public SeekBarView(Context context) {
    super(context);
  }
  boolean listenProgressEvent = true;
  boolean interceptKeyEvent = false;

  private NativeGestureDispatcher mGestureDispatcher;

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

  public void setInterceptKeyEvent(boolean interceptKeyEvent) {
    this.interceptKeyEvent = interceptKeyEvent;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    return (interceptKeyEvent && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ) || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)
      || super.dispatchKeyEvent(event);
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

  public void setListenProgressEvent(boolean listen){
    this.listenProgressEvent =listen;
  }

  public boolean isListenProgressEvent() {
    return listenProgressEvent;
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
