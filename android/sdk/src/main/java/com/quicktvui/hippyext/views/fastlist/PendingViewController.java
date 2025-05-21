package com.quicktvui.hippyext.views.fastlist;

import android.view.View;

import com.tencent.mtt.hippy.uimanager.RenderNode;


public interface PendingViewController {
  String PROP_LIST = "list";
  String PROP_UPDATE_NESTED = "updateNested";
  void setPendingData(View view, Object data, RenderNode templateNode);
  void setPendingData(View view, Object data, RenderNode templateNode,boolean useDiff);
}
