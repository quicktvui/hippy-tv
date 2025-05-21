package com.quicktvui.hippyext.views.fastlist;

import com.quicktvui.base.ui.ExtendTag;
import com.tencent.mtt.hippy.uimanager.RenderNode;

public class ClonedViewTag extends ExtendTag {

  final RenderNode originNode;

  public ClonedViewTag(RenderNode origin) {
    this.originNode = origin;
  }

  public RenderNode getOriginNode() {
    return originNode;
  }
}
