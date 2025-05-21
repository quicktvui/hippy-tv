package com.quicktvui.hippyext.views.fastlist;

import android.view.View;
/**
 * 焦点事件
 */
public interface OnFastItemFocusChangeListener {
  void onFocusChange(View view, boolean hasFocus, int position);
}
