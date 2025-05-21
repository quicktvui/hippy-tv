package com.quicktvui.hippyext.views.fastlist;

import android.util.Log;
import android.view.View;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;

public class FastListNode extends PendingListNode implements FastAdapter.ListNode {
  private final static String TAG = "FastListNodeLog";
    private FastAdapter.ListNodeTag tag;
    public FastListNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
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
    public void update() {
//        Log.d("FastList","FastListNode update nodeCount:"+getChildCount());
        super.update();
    }

    @Override
    public void batchComplete() {
        super.batchComplete();
//        Log.e("FastList","FastListNode batchComplete:"+getChildCount());
    }

    public void updateViewRecursive(){
        super.updateViewRecursive();
    }


  @Override
  public void manageChildrenComplete() {
    super.manageChildrenComplete();
    if (LogUtils.isDebug()) {
      Log.d(TAG,"manageChildrenComplete FastListNode childCount:"+getChildCount());
    }

  }

  @Override
  public void dispatchUIFunction(String functionName, HippyArray parameter, Promise promise) {
    super.dispatchUIFunction(functionName, parameter, promise);
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

//    @Override
//    public void update() {
//        super.update();
//        this.update();
//
//    }


  @Override
  public void nodeToMap(HippyMap map) {
    super.nodeToMap(map);
  }
}
