package com.quicktvui.hippyext.views.fastlist;

import android.view.View;

/**
 * 点击、长按事件
 */
public interface OnFastItemClickListener {
  void onItemClickListener(View view, int position);

  boolean onItemLongClickListener(View view, int position);
}
