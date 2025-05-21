/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.views.list;

import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.uimanager.ListViewRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.supportui.views.recyclerview.BaseLayoutManager;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewBase;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewItem;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings({"deprecation", "unused"})
@HippyController(name = HippyListViewController.CLASS_NAME)
public class HippyListViewController extends HippyViewController<HippyListView> {




  public static final String CLASS_NAME = "ListView";

  @Override
  public void onViewDestroy(HippyListView hippyListView) {
    super.onViewDestroy(hippyListView);
    if (hippyListView != null && hippyListView.mListScrollListeners != null) {
      hippyListView.mListScrollListeners.clear();
    }
  }

  @Override
  protected void addView(ViewGroup parentView, View view, int index) {
    //		super.addView(parentView, view, index);
  }

  @Override
  protected void deleteChild(ViewGroup parentView, View childView, int childIndex) {
    // List的childView是RecyclerViewItem类型，不是由Hippy构建的，所以这里需要提前删除RecyclerViewItem的child
    if (childView instanceof RecyclerViewItem) {
      ((RecyclerViewItem) childView).removeAllViews();
    }
    // list里，删掉某个条目后，它后面的条目的位置都要减1
    if (childIndex >= 0 && parentView instanceof HippyListView) {
      HippyListView listView = (HippyListView) parentView;
      listView.getRecycler().updateHolderPositionWhenDelete(childIndex);
    }
  }

  @Override
  public int getChildCount(HippyListView viewGroup) {
    return ((HippyListAdapter) viewGroup.getAdapter()).getRecyclerItemCount();
  }

  @Override
  public View getChildAt(HippyListView viewGroup, int i) {
    return ((HippyListAdapter) viewGroup.getAdapter()).getRecyclerItemView(i);
  }

  @Override
  public void onBatchComplete(HippyListView view) {
    super.onBatchComplete(view);
    if(LogUtils.isDebug()) {
      Log.i(TVRecyclerView.TAG, "onBatchComplete called setListData list:" + ExtendUtil.debugViewLite(view));
    }
    view.setListData();
  }

  @Override
  protected View createViewImpl(Context context) {
    return new HippyListView(context, BaseLayoutManager.VERTICAL);
  }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps)
  {

    if (iniProps != null && iniProps.containsKey("horizontal"))
    {

      final TVRecyclerView r =  new TVRecyclerView(context, BaseLayoutManager.HORIZONTAL);
      if(iniProps.containsKey("disableAdvancedFocusSearch")){
        r.setUseAdvancedFocusSearch(false);
      }
      return r;
    }
    else
    {
      final TVRecyclerView r =  new TVRecyclerView(context, BaseLayoutManager.VERTICAL);
      if(iniProps.containsKey("disableAdvancedFocusSearch")){
        r.setUseAdvancedFocusSearch(false);
      }
      return r;
    }
  }

  @Override
  protected void onCreateViewByCache(View view, String type, HippyMap props) {
    super.onCreateViewByCache(view, type, props);
    if (view instanceof TVRecyclerView)
    {
      if(props.containsKey("disableAdvancedFocusSearch")){
        ((TVRecyclerView) view).setUseAdvancedFocusSearch(false);
      }
    }

  }

  @Override
  public RenderNode createRenderNode(int id, HippyMap props, String className,
      HippyRootView hippyRootView, ControllerManager controllerManager,
      boolean lazy) {
    return new ListViewRenderNode(id, props, className, hippyRootView, controllerManager, lazy);
  }

  @HippyControllerProps(name = "rowShouldSticky")
  public void setRowShouldSticky(HippyListView view, boolean enable) {
    view.setHasSuspentedItem(enable);
  }

  @HippyControllerProps(name = "onScrollBeginDrag", defaultType = HippyControllerProps.BOOLEAN)
  public void setScrollBeginDragEventEnable(HippyListView view, boolean flag) {
    view.setScrollBeginDragEventEnable(flag);
  }

  @HippyControllerProps(name = "onScrollEndDrag", defaultType = HippyControllerProps.BOOLEAN)
  public void setScrollEndDragEventEnable(HippyListView view, boolean flag) {
    view.setScrollEndDragEventEnable(flag);
  }

  @HippyControllerProps(name = "forceBlockFocusOnFail", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setForceBlockFocusOnFail(HippyListView view, boolean enable) {
    if (view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setForceBlockFocusOnFail(enable);
    }
  }

  @HippyControllerProps(name = "onMomentumScrollBegin", defaultType = HippyControllerProps.BOOLEAN)
  public void setMomentumScrollBeginEventEnable(HippyListView view, boolean flag) {
    view.setMomentumScrollBeginEventEnable(flag);
  }

  @HippyControllerProps(name = "onMomentumScrollEnd", defaultType = HippyControllerProps.BOOLEAN)
  public void setMomentumScrollEndEventEnable(HippyListView view, boolean flag) {
    view.setMomentumScrollEndEventEnable(flag);
  }

  @HippyControllerProps(name = "onScrollEnable", defaultType = HippyControllerProps.BOOLEAN)
  public void setOnScrollEventEnable(HippyListView view, boolean flag) {
    view.setOnScrollEventEnable(flag);
  }

  @HippyControllerProps(name = "exposureEventEnabled", defaultType = HippyControllerProps.BOOLEAN)
  public void setExposureEventEnable(HippyListView view, boolean flag) {
    view.setExposureEventEnable(flag);
  }

  @HippyControllerProps(name = "scrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setScrollEnable(HippyListView view, boolean flag) {
    view.setScrollEnable(flag);
  }

  @HippyControllerProps(name = "scrollEventThrottle", defaultType = HippyControllerProps.NUMBER, defaultNumber = 30.0D)
  public void setscrollEventThrottle(HippyListView view, int scrollEventThrottle) {
    view.setScrollEventThrottle(scrollEventThrottle);
  }

  @HippyControllerProps(name = "scrollOffset", defaultType = HippyControllerProps.NUMBER)
  public void setScrollOffset(HippyListView view, int scrollOffset) {
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setScrollOffset(scrollOffset);
    }
  }

  @HippyControllerProps(name = "enableScrollOffsetEvent", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setEnableScrollOffsetEvent(HippyListView view,boolean mEnableScrollOffsetEvent) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setEnableScrollOffsetEvent(mEnableScrollOffsetEvent);
    }
  }

  @HippyControllerProps(name = "preloadItemNumber")
  public void setPreloadItemNumber(HippyListView view, int preloadItemNumber) {
    RecyclerViewBase.Adapter<?> adapter = view.getAdapter();
    if (adapter instanceof HippyListAdapter) {
      ((HippyListAdapter) adapter).setPreloadItemNumber(preloadItemNumber);
    }
  }

  @HippyControllerProps(name = "overScrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setOverScrollEnabled(HippyListView view, boolean flag) {
    view.setOverScrollEnabled(flag);
  }

  @HippyControllerProps(name = "initialContentOffset", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setInitialContentOffset(HippyListView view, int offset) {
    view.setInitialContentOffset((int)PixelUtil.dp2px(offset));
  }

  @HippyControllerProps(name = "listenBoundEvent",defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setListenBoundEvent(HippyListView view, boolean enable)
  {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setListenBoundEvent(enable);
    }
  }

  @HippyControllerProps(name = "setUseNegativeLayout",defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setUseNegativeLayout(HippyListView view, boolean enable)
  {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setUseNegativeLayout(enable);
    }
  }

  //触底回弹
  @HippyControllerProps(name = "endHintEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setShakeEndEnable(HippyListView view, boolean enable)
  {
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setShakeEndEnable(enable);
    }
  }

  //焦点抖动
  @HippyControllerProps(name = "endShakeEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setListShakeEnable(HippyListView view, boolean enable)
  {
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setListShakeSelf(enable);
    }
  }

  @HippyControllerProps(name = "preload")
  public void setPreload(HippyListView view, int preloadItemNumber)
  {
    RecyclerViewBase.Adapter adapter = view.getAdapter();
    if (adapter instanceof HippyListAdapter)
    {
      ((HippyListAdapter)adapter).setPreloadItemNumber(preloadItemNumber);
    }
  }

  @HippyControllerProps(name = "layoutTargetPosition",defaultType = HippyControllerProps.NUMBER,defaultNumber = -1)
  public void setLayoutTriggerTargetPosition(HippyListView view ,int position){
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setLayoutTriggerTargetPosition(position);
    }
  }

  @Override
  public void dispatchFunction(HippyListView view, String functionName, HippyArray params, Promise promise) {
    super.dispatchFunction(view, functionName, params, promise);
    if ("getScrollOffset".equals(functionName)) {
      final HippyMap map = new HippyMap();
      map.pushInt("x",view.getOffsetX());
      map.pushInt("y",view.getOffsetY());
      promise.resolve(map);
    }
  }

  @Override
  public void dispatchFunction(HippyListView view, String functionName, HippyArray dataArray)
  {
    super.dispatchFunction(view, functionName, dataArray);
    switch (functionName)
    {
      case "scrollToIndex":
      {
        // list滑动到某个item
        int xIndex = dataArray.getInt(0);
        int yIndex = dataArray.getInt(1);
        boolean animated = dataArray.getBoolean(2);
        int duration = dataArray.getInt(3); //1.2.7 增加滚动时间 ms,animated==true时生效
        int offset = dataArray.getInt(4);
        view.scrollToIndex(xIndex, yIndex, animated,duration,offset);
        break;
      }
      case "setInitPosition" :

        if(view instanceof TVRecyclerView){
          final HippyMap map = dataArray.getMap(0);
          Log.d("PendingFocus","call setInitPosition view:"+view+",map:"+map);
          ((TVRecyclerView) view).setInitPositionInfo(map);
        }
        break;
      case "clearInitPosition" :
        if(view instanceof TVRecyclerView){
          final HippyMap map = dataArray.getMap(0);
          ((TVRecyclerView) view).clearInitFocusPosition();
        }
        break;
      case "scrollToContentOffset":
      {
        // list滑动到某个距离
        double xOffset = dataArray.getDouble(0);
        double yOffset = dataArray.getDouble(1);
        boolean animated = dataArray.getBoolean(2);
        int duration = dataArray.getInt(3);  //1.2.7 增加滚动时间 ms,animated==true时生效
        view.scrollToContentOffset(xOffset, yOffset, animated,duration);
        break;
      }
      case "scrollToTop":
      {
        view.scrollToTop(null);
        break;
      }
      case "setSelectChildPosition":
        int position = dataArray.getInt(0);
        if(view instanceof TVRecyclerView){
          boolean changeFocusTarget = true;
          if(dataArray.size() > 1){
            changeFocusTarget = dataArray.getBoolean(1);
          }
          ((TVRecyclerView) view).setSelectChildPosition(position,changeFocusTarget);
        }
        break;
      case "setTargetFocusChildPosition":
        int pos = dataArray.getInt(0);
        if(view instanceof TVRecyclerView){
          ((TVRecyclerView) view).setTargetFocusChildPosition(pos);
        }
        break;
      case "requestChildFocus":
        if(LogUtils.isDebug()) {
          LogUtils.e(FocusDispatchView.TAG, "requestChildFocus view:" + view.getId());
        }
        int p = dataArray.getInt(0);
        if(view instanceof TVRecyclerView){
          final View v = view.findViewByPosition(p);
          if(v != null){
            v.requestFocus();
          }
        }
        break;

      case "refreshListData":
        LogUtils.d("hippy","refreshListData called by  :"+view.getId());
        view.setListData();
        break;
    }
  }

  /*** zhaopeng  add  20201117 **/
  @HippyControllerProps(name = "focusMemory", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setFocusMemory(HippyListView view, boolean enable)
  {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).enableFocusMemory(enable);
    }
  }

  @HippyControllerProps(name = NodeProps.CLIP_BOUNDS_OUTSET_RECT, defaultType = HippyControllerProps.MAP)
  public void setClipRectOutset(HippyListView view, HippyMap hippyMap){
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setClipOutset(hippyMap.getInt("left"),hippyMap.getInt("top"),hippyMap.getInt("right") ,hippyMap.getInt("bottom"));
    }
  }

  @HippyControllerProps(name = NodeProps.CLIP_BOUNDS_OUTSET, defaultType = HippyControllerProps.NUMBER,defaultNumber = 0)
  public void setClipRectOutsetAll(HippyListView view, int outSet){
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setClipOutset(outSet,outSet,outSet,outSet);
    }
  }

  @HippyControllerProps(name = "loadMoreState", defaultType = HippyControllerProps.STRING,defaultString = "idle")
  public void setLoadMoreState(HippyListView view, String state){
//    view.setLoadMoreState(state);
  }





  @HippyControllerProps(name = NodeProps.LIST_VIEW_OVER_SCROLL_LENGTH_START, defaultType = HippyControllerProps.NUMBER,defaultNumber = 0)
  public void setOverScrollStart(HippyListView view, int length){
    if(mOverScrollDecoration == null){
      mOverScrollDecoration = new OverScrollDecoration();
      view.addItemDecoration(mOverScrollDecoration);
    }
    mOverScrollDecoration.start = length;
    view.requestLayout();

  }

  @HippyControllerProps(name = NodeProps.LIST_VIEW_OVER_SCROLL_LENGTH_END, defaultType = HippyControllerProps.NUMBER,defaultNumber = 0)
  public void setOverScrollEnd(HippyListView view, int length){
    if(mOverScrollDecoration == null){
      mOverScrollDecoration = new OverScrollDecoration();
      view.addItemDecoration(mOverScrollDecoration);
    }
    mOverScrollDecoration.end = length;

    view.requestLayout();

  }

  @HippyControllerProps(name = "setSelectChildPosition", defaultType = HippyControllerProps.NUMBER,defaultNumber = 0)
  public void setSelectChildPosition(HippyListView view, int position){
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setSelectChildPosition(position);
    }
  }

  @HippyControllerProps(name = "initPosition", defaultType = HippyControllerProps.MAP)
  public void setScrollTargetPosition(HippyListView view, HippyMap map){

    Log.d("PendingFocus","initPosition view:"+view+",map:"+map);
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setInitPositionInfo(map);
    }
  }

  @HippyControllerProps(name = "initScroll", defaultType = HippyControllerProps.ARRAY)
  public void setScrollPosition(HippyListView view, HippyArray array){

    Log.d("","initScroll view:"+view+",array:"+array);
    if(view instanceof TVRecyclerView){
      if(array.size() > 0){

        int pos = array.getInt(0);
        int offset = 0;
        if(array.size() > 1){
          offset = array.getInt(1);
        }
        if(pos > -1) {
          ((TVRecyclerView) view).scrollToPosition(pos, offset);
        }
      }

    }
  }


  @HippyControllerProps(name = "initFocusPositionAfterLayout", defaultType = HippyControllerProps.NUMBER,defaultNumber = -1)
  public void setInitFocusAfterLayout(HippyListView view, int position){
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setInitFocusPositionAfterLayout(position);
    }
  }



  //此处vue是怎么传值的
  @HippyControllerProps(name = "blockFocusOnFail", defaultType = HippyControllerProps.BOOLEAN ,defaultBoolean = false)
  public void setBlockFocusOnFail(HippyListView view, HippyMap hippyMap){
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setBlockFocusOnFail(new int[InternalExtendViewUtil.FOCUS_BLOCK_DIRECTION_ALL]);
    }
  }

  //此处vue是怎么传值的
  @HippyControllerProps(name = "listenFocusSearchOnFail", defaultType = HippyControllerProps.BOOLEAN ,defaultBoolean = false)
  public void setListenFocusSearchOnFail(HippyListView view, boolean listen){
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setListenFocusSearchOnFail(listen);
    }
  }

  @HippyControllerProps(name = "makeChildVisibleType", defaultType = HippyControllerProps.STRING)
  public void setMakeChildVisibleType(HippyListView view, String type){
    if(view instanceof TVRecyclerView) {
      if ("normal".equals(type)) {
        ((TVRecyclerView) view).setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID);
      } else if("none".equals(type)){
        ((TVRecyclerView) view).setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_NONE);
      } else {
        ((TVRecyclerView) view).setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER);
      }
    }
  }

  @HippyControllerProps(name = "makeChildVisibleClampBackward", defaultType = HippyControllerProps.NUMBER)
  public void setRequestChildOnScreenClampBackward(HippyListView view,int clampBackward) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID);
      ((TVRecyclerView) view).setRequestChildOnScreenClampBackward(clampBackward);
    }
  }

  @HippyControllerProps(name = "makeChildVisibleClampForward", defaultType = HippyControllerProps.NUMBER)
  public void setRequestChildOnScreenClampForward(HippyListView view,int clampForward) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID);
      ((TVRecyclerView) view).setRequestChildOnScreenClampForward(clampForward);
    }

  }

  @HippyControllerProps(name = "scrollThresholdHorizontal", defaultType = HippyControllerProps.NUMBER)
  public void setScrollThresholdHorizontal(HippyListView view,int threshold) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setScrollThresholdHorizontal(threshold);
    }
  }

  @HippyControllerProps(name = "scrollThresholdVertical", defaultType = HippyControllerProps.NUMBER)
  public void setScrollThresholdVertical(HippyListView view,int threshold) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setScrollThresholdVertical(threshold);
    }
  }

  @HippyControllerProps(name = "scrollYLesserReferenceValue", defaultType = HippyControllerProps.NUMBER)
  public void setScrollYLesserReferenceValue(HippyListView view,int value) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setScrollYLesserReferenceValue(value);
    }
  }

  @HippyControllerProps(name = "scrollYGreaterReferenceValue", defaultType = HippyControllerProps.NUMBER)
  public void setScrollYGreaterReferenceValue(HippyListView view,int value) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setScrollYGreaterReferenceValue(value);
    }
  }

  @HippyControllerProps(name = "shakePreCheckNumber", defaultType = HippyControllerProps.NUMBER, defaultNumber = 2)
  public void setShakePreCheckNumber(HippyListView view,int shakePreCheckNumber) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setShakePreCheckNumber(shakePreCheckNumber);
    }
  }

  @HippyControllerProps(name = "blankItemDecoration", defaultType = HippyControllerProps.ARRAY)
  public void setBlankItemDecoration(HippyListView view,HippyArray array) {
    if(view instanceof TVRecyclerView){
      ((TVRecyclerView) view).setBlankItemDecoration(array);
    }
  }



  private OverScrollDecoration mOverScrollDecoration;

  private static class OverScrollDecoration extends RecyclerViewBase.ItemDecoration{

    public int start;
    public int end;

    @Override
    public void getItemOffsets(Rect outRect, int itemPosition, RecyclerViewBase parent) {
      super.getItemOffsets(outRect, itemPosition, parent);

      if(parent instanceof TVRecyclerView){

        final boolean isVertical = ((TVRecyclerView) parent).getOrientation() == BaseLayoutManager.VERTICAL;

        if(itemPosition == 0){
          if(isVertical){
            outRect.top = start;
          }else{
            outRect.left = start;
          }
        }else if(itemPosition == parent.getAdapter().getItemCount() -1){
          if(isVertical){
            outRect.bottom = end;
          }else{
            outRect.right = end;
          }
        }

      }

    }
  }

  @HippyControllerProps(name = "enableSelectOnFocus", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setEnableSelectOnFocus(HippyListView view, boolean flag)
  {
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setEnableSelectOnFocus(flag);
    }
  }
  @HippyControllerProps(name = "useAdvancedFocusSearch", defaultType = HippyControllerProps.BOOLEAN)
  public void setUseAdvancedFocusSearch(HippyListView view, boolean flag)
  {
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setUseAdvancedFocusSearch(flag);
    }
  }

  @HippyControllerProps(name = "checkScrollOffsetOnStateChanged", defaultType = HippyControllerProps.BOOLEAN)
  public void setCheckScrollOffsetOnStateChanged(HippyListView view,boolean b){
    if(view instanceof TVRecyclerView) {
      ((TVRecyclerView) view).setCheckScrollOffsetOnStateChanged(b);
    }
  }



}
