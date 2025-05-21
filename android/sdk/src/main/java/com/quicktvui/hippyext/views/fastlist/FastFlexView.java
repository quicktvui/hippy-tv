package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.list.HippyRecycler;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 实现瀑布流的绝对布局
 */
public class FastFlexView extends HippyViewGroup implements FastPendingView,HippyRecycler, FastAdapter.ScrollTaskHandler {
  public static final String TAG = "FastFlexLog";
  private  FastAdapter mAdapter;
  private int[] padding = new int[4];
  private int preferWidth = -1;
  private int preferHeight = -1;
  private boolean hasReset = false;
  protected EventDeliverer eventDeliverer;

  private ArrayList<FastAdapter.Holder> cacheHolders;

  private boolean useDiff = false;
  private String keyName;
  private boolean isAttached = false;

  public boolean isUseDiff() {
    return useDiff;
  }

  private boolean enablePlaceholder = FastAdapter.CHILD_LIST_PLACEHOLDER_ENABLE;

  public FastFlexView(Context context) {
    super(context);
    setFocusable(false);
    setClipChildren(false);
    eventDeliverer = new EventDeliverer(Utils.getHippyContext(context));
    init();
  }

  public void setUseDiff(boolean useDiff) {
    this.useDiff = useDiff;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  @Override
  public EventDeliverer getEventDeliverer() {
    return eventDeliverer;
  }

  @Override
  public void onResetBeforeCache() {
    super.onResetBeforeCache();
    if(LogUtils.isDebug()) {
      Log.d(TAG, "DebugPool flexView onResetBeforeCache:"+Utils.hashCode(this)+",rootList:"+mAdapter.getRootListView());
    }
    if(!hasReset) {
      resetChildren();
      hasReset = true;
    }
  }

  private int mHandleEventNodeId = -1;
  @Override
  public void setHandleEventNodeId(int id) {
    this.mHandleEventNodeId = id;
    if(mAdapter != null){
      mAdapter.rootListNodeID = id;
    }
  }

  public void setEnablePlaceholder(boolean b){
    this.enablePlaceholder = b;
    if (mAdapter != null) {
      mAdapter.setEnablePlaceholder(b);
    }
  }

  @Override
  public void pausePostTask() {

  }

  @Override
  public void resumePostTask() {

  }

  @Override
  public HippyArray getItemListData() {
    return mAdapter != null ? mAdapter.dataList : null;
  }

  @Override
  public void notifySaveState() {
    super.notifySaveState();
      if(cacheHolders != null && mAdapter != null) {
        for (int i = 0; i < cacheHolders.size(); i ++){
          final View view = cacheHolders.get(i).itemView;
          if(view instanceof HippyRecycler){
            if(LogUtils.isDebug()) {
              Log.d(TAG, "++DebugPool flexView notifySaveState child:"+view);
            }
            ((HippyRecycler) view).notifySaveState();
          }
        }
      }
  }

  @Override
  public void notifyRestoreState() {
    super.notifyRestoreState();
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        final View view = cacheHolders.get(i).itemView;
        if(view instanceof HippyRecycler){
          if(LogUtils.isDebug()) {
            Log.d(TAG, "--DebugPool flexView notifyRestoreState child:"+view);
          }
          ((HippyRecycler) view).notifyRestoreState();
        }
      }
    }
  }

  void resetChildren(){
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        final FastAdapter.Holder holder = cacheHolders.get(i);
        //onViewRecycled生命周期方法在这里调用
        mAdapter.onViewDetachedFromWindow(holder);
        mAdapter.onViewRecycled(holder);
        final View view = holder.itemView;
        if(view instanceof HippyRecycler){
          if(LogUtils.isDebug()) {
            Log.d(TAG, "DebugPool flexView reset child:"+view);
          }
          ((HippyRecycler) view).onResetBeforeCache();
        }
      }
      final Iterator<FastAdapter.Holder> it = cacheHolders.iterator();
      int i = 0;
      while (it.hasNext()){
       final FastAdapter.Holder holder =  it.next();
       if(holder.singleton){
         //zhaopeng 20220714 这里不将singleton的view缓存在flexView里，以防止后来重用时被误删
         it.remove();
         mAdapter.dataList.removeAt(i);
         if(holder.itemView.getParent() instanceof ViewGroup){
           ((ViewGroup) holder.itemView.getParent()).removeView(holder.itemView);
         }
       }
       i++;
      }
    }
  }

  @Override
  public void resetProps() {
    super.resetProps();
  }

  @Override
  public void clear() {
    super.clear();
  }

  @Override
  public View getView() {
    return this;
  }

  @Override
  public void notifyRecycled() {
    if(LogUtils.isDebug()) {
      Log.d(TAG, "notifyRecycled this:" + hashCode() + ",childrenCount:" + getChildCount());
    }
//    mAdapter.recycleAll();
  }

  @Override
  public void updateItem(int pos, Object data) {
    if (mAdapter != null) {
      mAdapter.replaceItemData(pos,data);
      RecyclerView.LayoutParams lpBefore = findCacheHolderFromIndex(pos) != null ? mAdapter.getItemLayoutParams(findCacheHolderFromIndex(pos)) : null;
      updateHolder(pos);
      RecyclerView.LayoutParams lpAfter = findCacheHolderFromIndex(pos) != null ? mAdapter.getItemLayoutParams(findCacheHolderFromIndex(pos)) : null;
      boolean changed = lpBefore == null && lpAfter != null;
      if (!changed) {
        if (lpBefore != null && lpAfter != null) {
          changed = lpBefore.width != lpAfter.width || lpBefore.height != lpAfter.height;
        }
      }
      if (changed) {
        computeSize(mAdapter.dataList);
      }
    }
  }

  @Override
  public void replaceItemData(int pos, Object data) {
    if (mAdapter != null) {
      mAdapter.replaceItemData(pos,data);
    }
  }



  @Override
  public void updateItem(int pos, Object data, boolean traverse) {
    this.updateItem(pos,data);
  }

  @Override
  public void updateItemProps(String name,int position, Object data, boolean updateView) {
    if (mAdapter != null) {
      final RecyclerView.ViewHolder vh = findViewHolderForAdapterPosition(position);
      mAdapter.updateItemProps(name, position, (HippyMap) data, updateView, vh);
      if (vh == null) {
        Log.e(TAG, "updateItemProps error,没有找到对应的Holder，当前View可能没有在展示");
      }
    }
  }

  @Override
  public void notifyDetachFromParent() {
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        mAdapter.onViewDetachedFromWindow(cacheHolders.get(i));
      }
    }
  }

  @Override
  public void notifyAttachToParent() {
    if(cacheHolders != null && mAdapter != null) {
//      Log.i(AutoFocusManager.TAG,"FastList notifyAttachToParent cacheHolders size:"+cacheHolders.size());
      for (int i = 0; i < cacheHolders.size(); i ++){
        mAdapter.onViewAttachedToWindow(cacheHolders.get(i));
      }
    }else{
//      Log.i(AutoFocusManager.TAG,"FastList notifyAttachToParent cacheHolders size:"+0);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    isAttached = true;
    updateCachePoolName();
    updateCachePoolMap();
  }

  private HippyMap recycledViewPoolMap;
  private String poolName = hashCode()+"";

  public HippyMap getRecycledViewPoolMap() {
    if (recycledViewPoolMap == null) {
      recycledViewPoolMap = new HippyMap();
    }
    return recycledViewPoolMap;
  }

//  public void setCachePoolName(String name) {
//    this.poolName = name;
//    if(isAttached()){
//      updateCachePoolName();
//    }
//  }

  private void updateCachePoolName(){
    mAdapter.setShareViewPoolType(poolName);
  }

  private void updateCachePoolMap(){
    final HippyMap map = recycledViewPoolMap;
    if (map == null) {
      return;
    }
    if (mAdapter != null) {
      if (map.containsKey("name")) {
        mAdapter.setShareViewPoolType(map.getString("name"));
        final RecyclerView.RecycledViewPool pool = mAdapter.getCacheWorker(getContext(),this);
        if (LogUtils.isDebug()) {
          Log.i("DebugPool", "flex setCachePoolMap name=" + map.getString("name") + ",this:" + this);
          Log.i("DebugPool", "DebugPool setupPool in flex  instance:" + Utils.hashCode(pool)+",context:"+getContext());
        }
        HippyMap size = map.getMap("size");
        for (String typeS : size.keySet()) {
          final int type = Integer.parseInt(typeS);
          final int count = size.getInt(typeS);
          if (LogUtils.isDebug()) {
            Log.i("DebugPool", "setCachePoolMap  in flex type:" + type + ",count:" + count + ",this:" + this);
          }
          if (mAdapter.findOldCacheWorker() != null) {
            mAdapter.findOldCacheWorker().setMaxCacheSize(type, count);
          }
        }
      }else{
        //Log.e("DebugPool", "flex updateCachePoolMap on no name:"+map);
      }
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    isAttached = false;
    if(LogUtils.isDebug()) {
      Log.e(TAG, "onDetachedFromWindow this:" + hashCode() + ",childrenCount:" + (cacheHolders == null ? 0 : cacheHolders.size()));
    }
    if (mAdapter != null) {
      mAdapter.clearDetachCaches();
    }
//    recycleChildren();
  }



  HippyEngineContext getEngineContext(){
    return ((HippyInstanceContext) getContext()).getEngineContext();
  }

  public void setPendingData(Object data, RenderNode templateNode){
    setPendingData(data,templateNode,this.useDiff);
  }

  public void setPendingData(Object data, RenderNode templateNode,boolean useDiff) {
    boolean isPauseOnInVisible = mAdapter.getRootListView() != null && mAdapter.getRootListView().isPostTaskPaused();
    if(LogUtils.isDebug()) {
      Log.v(TAG, "setPendingData data:" + data +",isPauseOnInVisible:"+isPauseOnInVisible + ",templateNode:" + templateNode + "，data instanceof HippyArray ：" + (data instanceof HippyArray) );
    }
    assert (templateNode instanceof FastAdapter.ListNode);
    assert (data instanceof HippyArray);
    mAdapter.setContext(getEngineContext(),getContext());
    mAdapter.setListNode((FastAdapter.ListNode) templateNode);
    //共享数据
    mAdapter.onBeforeChangeAdapter();

    final HippyArray newArray = (HippyArray) data;


    if (!useDiff || (isPauseOnInVisible || mAdapter.isEnablePlaceholder()) ) {
      mAdapter.setData(newArray);
      mAdapter.onAttachedToView(this);
      computeSize(newArray);
      onAdapterChange();
      return;
    }

    final HippyArray oldArray = mAdapter.dataList;
    if (LogUtils.isDebug()) {
      Log.i(TAG, "handleDataDiff 刷新数据 oldArray.size--" + (oldArray == null ? "0" : oldArray.size())
        + "  newArray.size--" + (newArray == null ? "0" : newArray.size())
        + "  hashCode--" + FastFlexView.this.hashCode());
    }
    FastListDataBindingHelper.handleDataDiffCallBack3(oldArray, newArray, keyName,
      new FastListDataBindingHelper.OnDataDiffListener3() {

        @Override
        public void onRangeInsert(int position, int count) {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onRangeInsert--" + "  position--" + position + "  count--" + count);
          }
          for(int i = position;i < position+ count; i ++){
            FastAdapter.Holder vh = obtainHolder(i);
            addHolderView(i,vh);
            mAdapter.onBindViewHolder(vh,i);
            cacheHolders.add(i,vh);
            if(isAttached){
              mAdapter.onViewAttachedToWindow(vh);
            }
          }
        }
        //0,1,2     1-2
        @Override
        public void onRangeDelete(int position, int count) {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onRangeDelete--" + "  position--" + position + "  count--" + count);
          }
          for(int i = position + count -1; i > position - 1; i --){
              removeHolder(i);
          }
        }

        @Override
        public void onUpdateViewData() {
          mAdapter.setData(newArray);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onUpdateViewData");
          }
          mAdapter.onAttachedToView(FastFlexView.this);
          computeSize(newArray);
        }


        @Override
        public void onMove(int position, int nextPosition) {
          //mAdapter.notifyItemMoved(position, nextPosition);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onMove--" + "  position--" + position + "  nextPosition--" + nextPosition);
          }
          onAdapterChange();
        }

        @Override
        public void onRangeUpdate(int start, int count) {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onRangeUpdate--" + "  start--" + start + "  count--" + count);
          }
          for(int i = start; i < start + count; i ++){
            updateHolder(i);
          }
        }

        @Override
        public void onAllChange() {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onAllChange");
          }
          onAdapterChange();
        }
      },
      item -> (HippyMap) item.raw, FastAdapter.ItemEntity.class);
  }

  private void computeSize(HippyArray list){
    try {

      int maxWidth = 0;
      int maxHeight = 0;
      for (int i = 0; i < list.size(); i++) {


        final HippyMap item = (HippyMap) list.get(i);
//        Log.d(TAG, "computeSize item:" + item);
        final HippyArray layout = item.getArray("layout");
        if(layout == null || layout.size() < 1){
          //throw new IllegalArgumentException("FastFlexView计算尺寸错误,请确保item数据里有layout对象");
          if(LogUtils.isDebug()) {
            Log.e(TAG, "FastFlexView计算尺寸时， layout数据 未指定,可能会造成显示错误");
          }
          continue;
        }
//        Log.d(TAG, "computeSize layout:" + layout);
        final int x = Utils.toPX(layout.getInt(0));
        final int y = Utils.toPX(layout.getInt(1));
        final int w = Utils.toPX(layout.getInt(2));
        final int h = Utils.toPX(layout.getInt(3));
        if (x + w > maxWidth) {
          maxWidth = x + w;
        }
        if (y + h > maxHeight) {
          maxHeight = y + h;
        }
      }

      if (padding != null) {
        //0 1 2 3 : l t r b
        maxWidth += (padding[0] + padding[2]);
        maxHeight += (padding[1] + padding[3]);
      }
      if (maxWidth > 0) {
        this.preferWidth = maxWidth;
      }
      if (maxHeight > 0) {
        this.preferHeight = maxHeight;
      }
      if(LogUtils.isDebug()) {
        Log.e(TAG, "computeSize preferWidth:" + preferWidth + ",preferHeight:" + preferHeight);
      }
    }catch (Throwable t){
      t.printStackTrace();
      if(LogUtils.isDebug()) {
        Log.e(TAG, "computeSize preferWidth:" + preferWidth + ",preferHeight:" + preferHeight);
      }
    }

  }

  public int getPreferWidth(){
    return  this.preferWidth;
  }

  public int getPreferHeight(){
    return this.preferHeight;
  }

  void recycleChildren(){
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        recycleItem(cacheHolders.get(i),i);
      }
      cacheHolders.clear();
    }
  }

  FastAdapter.Holder findCacheHolderFromIndex(int i){
    if(cacheHolders == null || cacheHolders.size() <= i || i < 0){
      return null;
    }
    final FastAdapter.Holder result =  cacheHolders.get(i);

//    if(result.itemView.getParent() == this){
//      return result;
//    }
    return result;
  }

  void recycleItem(FastAdapter.Holder holder,int index){
    if(LogUtils.isDebug()) {
      Log.i(TAG, "DebugPool FlexView recycleItem holder type:" + holder.type + ",index:" + index);
    }
//    if(getChildCount() > index && index > -1){
//      removeViewAt(index);
//    }else {
      final View v = holder.itemView;
      if (v.getParent() instanceof ViewGroup) {
        ((ViewGroup) v.getParent()).removeView(v);
      }
//    }
    if(mAdapter != null ){
      mAdapter.onViewDetachedFromWindow(holder);
//      if(holder.itemView instanceof HippyRecycler){
//        ((HippyRecycler) holder.itemView).onResetBeforeCache();
//      }
      mAdapter.onViewRecycled(holder);
      mAdapter.recycleItem(holder);
    }
  }

  ArrayList<FastAdapter.Holder> tempHolders;
  ArrayList<Integer> markToDelete;

  void updateHolder(int pos){
//    FastAdapter.Holder vh = findCacheHolderFromIndex(pos);
//    if (vh != null) {
////      mAdapter.bindViewHolder(vh,pos);
//
//      mAdapter.onBindViewHolder(vh,pos);
//    }

    final int type = mAdapter.getItemViewType(pos);
//    if(cacheHolders == null){
//      this.cacheHolders = new ArrayList<>();
//    }
    FastAdapter.Holder vh = null;
    //先从当前view里找
    FastAdapter.Holder cvh = findCacheHolderFromIndex(pos);
    if(cvh != null){
      //类型相同，只需要重新onBind
      if(cvh.type == type) {
        vh = cvh;
//        if(LogUtils.isDebug()) {
//          Log.e(TAG, "DebugPool findCacheHolderFromIndex type Matched ,vh" + vh.hashCode() + ",type:" + vh.type);
//        }
        mAdapter.onBindViewHolder(vh, pos);
//        cacheHolders.add(pos, vh);
      }else{
//        //类型不同，放入缓存池
        recycleItem(cvh, pos);
      }
    }
    if(vh == null) {//对应位置没有缓存
      //从adapter缓存里找
      vh = obtainHolder(pos);
      addHolderView(pos, vh);
      mAdapter.onBindViewHolder(vh, pos);
      cacheHolders.set(pos, vh);
    }
  }

  @Override
  public void setSharedItemStore(String name) {
    if(mAdapter != null){
      mAdapter.setShareItemStoreName(name);
    }
  }

  public FastAdapter getAdapter() {
    return mAdapter;
  }

  void addHolderView(int position, FastAdapter.Holder vh){
    final RenderNode rn = vh.tag.template;
    if(LogUtils.isDebug()) {
      Log.v(TAG, "onAdapterChange add vh templateNode:" + rn);
    }
    // RenderUtil.reLayoutView(vh.itemView,rn.getX(),rn.getY(),rn.getWidth(), rn.getHeight());
    final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(rn.getWidth(),rn.getHeight());
    if(vh.itemView.getParent() == null){
      addView(vh.itemView,position,lp);
    }else{
      throw new IllegalStateException("此view Parent 不为空，请先删除 view:"+vh.itemView);
    }
  }

  void removeHolder(int pos){
      recycleItem(cacheHolders.get(pos),pos);
      cacheHolders.remove(pos);
  }

  FastAdapter.Holder obtainHolder(int position){
    final int type = mAdapter.getItemViewType(position);
    FastAdapter.Holder vh = mAdapter.findOldCacheView(type);
    if(vh == null) {
      vh = mAdapter.createViewHolder(this, type);
      if(LogUtils.isDebug()) {
        Log.e(TAG, "DebugPool flex createHolder new vh" + vh.hashCode() + ",type:" + vh.type+",this:"+ExtendUtil.debugView(this)+",map:"+ recycledViewPoolMap);
      }
    }
    return vh;
  }
  FastAdapter.Holder findHolderByPosition(int pos){
    if (cacheHolders != null && cacheHolders.size() > pos && pos > -1) {
      return cacheHolders.get(pos);
    }
    return null;
  }
  void onAdapterChange(){
    this.hasReset = false;
    //Log.d(TAG,"onAdapterChange itemCount:"+mAdapter.getItemCount());
      if(mAdapter != null){
        if(tempHolders != null){
          tempHolders.clear();
        }else{
          tempHolders = new ArrayList<>();
        }
        //removeAllViews();
        for(int i = 0; i < mAdapter.getItemCount(); i++){
          try {
            final int type = mAdapter.getItemViewType(i);

            FastAdapter.Holder vh = null;
            //先从当前view里找
            FastAdapter.Holder cvh = findCacheHolderFromIndex(i);

            if (cvh != null) {
              //类型相同，只需要重新onBind
              if (cvh.type == type) {
                vh = cvh;
                if (LogUtils.isDebug()) {
                  Log.i(TAG, "DebugPool findCacheHolderFromIndex type Matched ,vh" + vh.hashCode() + ",type:" + vh.type);
                }
                mAdapter.onBindViewHolder(vh, i);
                if(isAttached){
                  mAdapter.onViewAttachedToWindow(vh);
                }
                tempHolders.add(i, vh);
              } else {
                //类型不同，放入缓存池
                recycleItem(cvh, i);
              }
            }
            if (vh == null) {//对应位置没有缓存
              //从adapter缓存里找
              vh = obtainHolder(i);
              addHolderView(i, vh);
              mAdapter.onBindViewHolder(vh, i);
              if(isAttached){
                mAdapter.onViewAttachedToWindow(vh);
              }
//            mAdapter.onViewAttachedToWindow(vh);
              tempHolders.add(i, vh);
            }
          }catch (Throwable t){
            t.printStackTrace();
          }
        }

        if(tempHolders != null){
          //删除不需要的view
          if(cacheHolders == null){
            this.cacheHolders = new ArrayList<>();
          }
          if(tempHolders.size() < cacheHolders.size()){
            for(int i = tempHolders.size(); i < cacheHolders.size(); i ++){
                recycleItem(cacheHolders.get(i),i);
            }
          }
          this.cacheHolders.clear();
          this.cacheHolders.addAll(tempHolders);
          tempHolders.clear();
        }
      }
  }

  @Override
  public void setCachePoolMap(HippyMap map) {
    this.recycledViewPoolMap = map;
//    Log.e("DebugPool","setCachePoolMap map :"+map+",this:"+ ExtendUtil.debugView(this));
    updateCachePoolMap();
  }

  public void dispatchItemFunction(HippyArray var, @Nullable Promise promise) {
    final int itemPos = var.getInt(0);
    final String targetName = var.getString(1);
    final String functionTargetName = var.getString(2);
    final HippyArray array = var.getArray(3);

    final RecyclerView.ViewHolder holder = findViewHolderForAdapterPosition(itemPos);
    if (holder instanceof FastAdapter.Holder) {
      FastAdapter.Holder vh = (FastAdapter.Holder) holder;
      final FastAdapter.ElementNode targetNode = Utils.findElementNodeByName(targetName, vh.tag.getRootNode());
      final View target = targetNode == null ? null : targetNode.boundView;
      if (target != null && functionTargetName != null && !"dispatchFunctionByViewName".equals(functionTargetName)) {
        if (LogUtils.isDebug()) {
          Log.i(TAG, "dispatchItemFunction dispatchItemFunction work, target ==:" + target + ",targetName:" + targetName + ",functionTargetName:" + functionTargetName);
        }
        Utils.dispatchFunction(mAdapter.getControllerManager(), targetNode, functionTargetName, array, promise);
      } else {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchItemFunction dispatchFunctionByViewName error target ==:" + target + ",targetName:" + targetName + ",functionTargetName:" + functionTargetName);
        }
      }
    } else {
      Log.e(TAG, "dispatchItemFunction dispatchItemFunction error , found holder null , pos:" + itemPos);
    }

  }

  private RecyclerView.ViewHolder findViewHolderForAdapterPosition(int itemPos) {
    return findCacheHolderFromIndex(itemPos);
  }

  @Override
  public View findViewByPosition(int position) {
    if(position > -1 && position < getChildCount()){
      return getChildAt(position);
    }
    return null;
  }

  @Override
  public void notifyPauseTask() {
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        final View view = cacheHolders.get(i).itemView;
        if(view instanceof FastAdapter.ScrollTaskHandler){
          ((FastAdapter.ScrollTaskHandler) view).notifyPauseTask();
        }
      }
    }
  }

  @Override
  public void notifyBringToFront(boolean front) {
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        final View view = cacheHolders.get(i).itemView;
        if(view instanceof FastAdapter.ScrollTaskHandler){
          ((FastAdapter.ScrollTaskHandler) view).notifyBringToFront(front);
        }
      }
    }
  }

  @Override
  public void notifyResumeTask() {
    if(cacheHolders != null && mAdapter != null) {
      for (int i = 0; i < cacheHolders.size(); i ++){
        final View view = cacheHolders.get(i).itemView;
        if(view instanceof FastAdapter.ScrollTaskHandler){
          ((FastAdapter.ScrollTaskHandler) view).notifyResumeTask();
        }
      }
    }
  }

  void init(){
    mAdapter = new FastAdapter();
    mAdapter.setEnablePlaceholder(enablePlaceholder);
    mAdapter.eventDeliverer = eventDeliverer;
//    mAdapter.setShareViewPoolType(hashCode()+"");
    mAdapter.setBoundFLexView(this);
  }


  @Override
  public void clearPostTask(int type) {

  }

  @Override
  public void setRootList(FastListView rootList,FastAdapter parentAdapter) {
    if(mAdapter != null){
      mAdapter.setRootList(rootList,parentAdapter);
    }
  }


  @Override
  public int findPositionByChild(View child) {
    return -1;
  }

  private HippyViewGroup pageRootView;
  @Override
  public HippyViewGroup findPageRootView() {
    if (pageRootView != null) {
      return pageRootView;
    }
    View v =  HippyViewGroup.findRootViewFromParent(this);
    if(v instanceof HippyViewGroup){
      pageRootView = (HippyViewGroup) v;
    }
    return pageRootView;
  }

  @Override
  public void searchUpdateItemDataByItemID(String id, Object data, boolean traverse) {
    if (mAdapter != null) {
      FastAdapterUtil.updateItemData4FastAdapterTraverse(id,data,mAdapter.dataList,traverse,"FlexView:"+Integer.toHexString(hashCode()));
    }
  }

  @Override
  public void searchUpdateItemViewByItemID(String id, Object data, boolean traverse) {
    FastAdapterUtil.updateItemViewByItemID(this.getView(),id,data,traverse);
  }

  @Override
  public int findItemPositionBySID(String id) {
    return mAdapter == null ? -1 :  mAdapter.findItemPositionBySID(id);
  }

  /**
   * 根据指定的itemID,动态的更新一个元素的属性，
   */
  @Override
  public void searchUpdateItemPropsBySID(String name, String itemID, String prop, Object newValue, boolean updateView){
    FastAdapterUtil.updateItemPropsByItemID(this.getView(),name,itemID,prop,newValue,updateView);
  }

  @Override
  public void updateItemSpecificProp(String name, int position, String key, Object value,boolean updateView) {
    if (mAdapter != null) {
      final RecyclerView.ViewHolder vh = findViewHolderForAdapterPosition(position);
      mAdapter.updateItemSpecificPropByTargetName(name, position, key, value, updateView,vh);
      if (vh == null) {
        Log.e(TAG, "updateItemSpecificProp error,没有找到对应的Holder，当前View可能没有在展示");
      }
    }
  }

  @Override
  public View findFirstFocusByDirection(int direction) {
    if (direction == View.FOCUS_DOWN) {
      if (getChildCount() > 0) {
        return getChildAt(0);
      }
    }
    return null;
  }

}
