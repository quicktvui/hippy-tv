package com.quicktvui.hippyext.views.fastlist;

import android.util.Log;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;

import java.util.HashMap;
import java.util.Map;

public class ItemStoreNode extends RenderNode {
  private static final String TAG = "ItemStoreNodeLog";


  public ItemStoreNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
    super(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad);
  }

  @Override
  protected void addChildToPendingList(RenderNode renderNode) {
//    super.addChildToPendingList(renderNode);
  }

  public static Map<Integer, RenderNode> buildItemStoreTemplate(ItemStoreNode node){
    Map<Integer, RenderNode> templateNodes = new HashMap<>();
    for (int i = 0; i < node.getChildCount(); i++) {
      final RenderNode child = node.getChildAt(i);
        if (child.getProps().containsKey("type")) {
          final String childType = child.getProps().getString("type");

          final int type = Integer.parseInt(childType);

          if (LogUtils.isDebug()) {
            Log.i(TAG, "buildTemplate put type:" + type + ",node:" + child);
          }
          templateNodes.put(type, child);
        } else {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "buildTemplate put fail,node:" + child+",no type");
          }
          //templateNodes.put(defaultType, child);
        }
      }
    return templateNodes;
  }




  @Override
  public void manageChildrenComplete() {
    super.manageChildrenComplete();
    if (LogUtils.isDebug()) {
      Log.d(TAG,"manageChildrenComplete ItemStoreNode childCount:"+getChildCount());
    }
  }




}
