package com.quicktvui.hippyext.views.fastlist;

import android.support.annotation.Nullable;
import android.view.View;

import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

/**
 * 数据不定的列表
 */
public interface FastPendingView extends VirtualListView{
//  void onCreateItemViewRecursive(ControllerManager m, RenderNode realNode, View parent,View view, FastItemNode templateNode, ViewTag tag);
//  void onBindItemView(ControllerManager m, RenderNode realNode,View view,  ViewTag tag);
//  void buildTemplate();
  void setPendingData(Object data, RenderNode templateNode);
  void notifyRecycled();
  void updateItem(int pos,Object data);
  void updateItem(int pos,Object data,boolean traverse);
  void replaceItemData(int pos,Object data);
  //----new
  void searchUpdateItemViewByItemID(String id, Object data, boolean traverse);
  void searchUpdateItemPropsBySID(String name, String itemID, String prop, Object newValue, boolean updateView);
  int findItemPositionBySID(String id);
  void updateItemSpecificProp(String name, int position,String prop,  Object dataToUpdate, boolean updateView);
  //------
  void updateItemProps(String name,int pos,Object data,boolean updateView);
  void setCachePoolMap(HippyMap map);
  void pausePostTask();
  void resumePostTask();
  void dispatchItemFunction(HippyArray var,@Nullable Promise promise);
  void setRootList(FastListView rootList,FastAdapter parentAdapter);
  void setHandleEventNodeId(int id);
  int findPositionByChild(View child);
  View findViewByPosition(int position);
  HippyArray getItemListData();
  EventDeliverer getEventDeliverer();
  <T extends View> T findViewWithTag(Object tag);
  HippyViewGroup findPageRootView();
  View findFirstFocusByDirection(int direction);

  /** 1.8.1 add **/
  void setSharedItemStore(String name);
}


