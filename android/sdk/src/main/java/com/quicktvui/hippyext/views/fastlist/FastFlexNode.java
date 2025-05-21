package com.quicktvui.hippyext.views.fastlist;

import android.view.View;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;

public class FastFlexNode extends PendingListNode implements FastAdapter.ListNode {
  static String TAG = "FastFlexLog";
  private FastAdapter.ListNodeTag tag;



    public FastFlexNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
        super(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad);
    }

    @Override
    protected void addChildToPendingList(RenderNode renderNode) {
//        super.addChildToPendingList(renderNode);
    }

  @Override
  public View createView() {
    return super.createView();
  }





  @Override
  public RenderNode getNode() {
    return this;
  }

  @Override
  public void setBoundTag(FastAdapter.ListNodeTag tag) {
      this.tag = tag;
  }

  @Override
  public FastAdapter.ListNodeTag getBoundTag() {
    return tag;
  }
}
