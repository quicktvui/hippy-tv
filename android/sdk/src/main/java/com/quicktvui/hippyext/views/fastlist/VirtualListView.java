package com.quicktvui.hippyext.views.fastlist;

import android.view.View;

/**
 * 虚拟DOM类型的列表
 */
public interface VirtualListView {
  View getView();
  void searchUpdateItemDataByItemID(String id, Object data, boolean traverse);
}
