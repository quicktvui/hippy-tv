package com.tencent.mtt.hippy.views.list;

import com.quicktvui.hippyext.views.fastlist.ListViewControlProp;

public interface TVSingleLineListView {

  void setBlockFocusOn(int[] focus);

  void setBlockFocusOnFail(int[] directions);

  ListViewControlProp getControlProps();

  void diffSetScrollToPosition(int position, int offset);

}
