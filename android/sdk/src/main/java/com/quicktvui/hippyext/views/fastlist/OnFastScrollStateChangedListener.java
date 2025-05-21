package com.quicktvui.hippyext.views.fastlist;

public interface OnFastScrollStateChangedListener {
  void onScrollStateChanged(int lastState, int state,int deltaX,int deltaY);
   void onTriggerScrollYGreater();
   void onTriggerScrollYLesser();
}
