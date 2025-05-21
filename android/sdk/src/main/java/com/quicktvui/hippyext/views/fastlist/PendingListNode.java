package com.quicktvui.hippyext.views.fastlist;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;

public class PendingListNode extends RenderNode {
  public PendingListNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
    super(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad);
  }

  public PendingListNode(int id, String className, ControllerManager componentManager) {
    super(id, className, componentManager);
  }


}
