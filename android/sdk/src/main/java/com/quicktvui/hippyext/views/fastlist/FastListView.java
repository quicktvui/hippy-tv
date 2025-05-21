package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;

import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.hippyext.TriggerTaskManagerModule;
import com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.quicktvui.base.ui.StateView;
import com.tencent.mtt.hippy.uimanager.ViewStateProvider;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.list.HippyRecycler;
import com.tencent.mtt.hippy.views.list.TVRecyclerView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

import java.util.Arrays;
import java.util.Objects;

public class FastListView extends TVListView implements HippyViewBase, FastPendingView, FastAdapter.ScrollTaskHandler, ViewStateProvider {

  //    private FastRecyclerView mRealList;
  protected InitParams mInitParams;
  public static final String TAG = TVListView.TAG;
  private FastAdapter mAdapter;
  private ItemDecoration mItemDecoration;
  private NativeGestureDispatcher mGestureDispatcher;
  private boolean hasSet = false;
  private boolean isDisplayed = true;
  private boolean isDraw = true;
  private boolean pendingChangeVisible = false;
  private Runnable changeVisibilityRunnable = null;
  private Runnable pendingDiffScroll = null;
  private RenderNode templateNode;//根node节点
  //重新设置数据时，重新判断autofocus
  protected boolean autofocusDirtyOnDataChange = false;
  private View agentView;//处理滑动事件的代理view
  private boolean pauseTaskOnHide = false;//处理滑动事件的代理view
  private OnFastScrollStateChangedListener onScrollStateChangedListener;//外部处理滑动事件变化
  int lastScrollX = 0;
  int lastScrollY = 0;

  private boolean isInfinityLoop = false;
  private boolean suspendUpdateTaskOnPause = false;

  int customFadeLength = -1;
  /**
   * 当列表pause时，是否将执行的updateItemXX等任务添加到队列中
   *
   * @param suspendUpdateTaskOnPause
   */
  public void setSuspendUpdateTaskOnPause(boolean suspendUpdateTaskOnPause) {
    this.suspendUpdateTaskOnPause = suspendUpdateTaskOnPause;
  }

  public boolean isSuspendUpdateTask() {
    return suspendUpdateTaskOnPause && isPostTaskPaused();
  }

  public void setPauseTaskOnHide(boolean pauseOnHide) {
    this.pauseTaskOnHide = pauseOnHide;
  }

  private HippyMap infiniteParams = null;


  private boolean useDiff = false; // 是否使用diff
  private String keyName; // diff的key

  private Object focusViewTag;

  private boolean reFocusAfterDiff = false;

//  public FastListView(Context context) {
//    super(context);
//  }

  public FastListView(Context context, HippyMap params) {
    super(context);
    applyProps(params);
  }

  public static final String TAG_CLONED = "ClonedDispatch";

  public static View findNodeViewByTemplateView(int position, View view) {
      HippyEngineContext context = Utils.getHippyContext(view);
     RenderNode node =  context != null  ? context.getRenderManager().getRenderNode(view.getId()) : null;
     final FastListView fastList = findParentFastList(context,node);
     View targetView = null;
      if (fastList != null) {
        if (fastList.getFastAdapter() != null) {
          targetView = fastList.getFastAdapter().findClonedViewByTemplateView(position,view);
          if(LogUtils.isDebug()) {
            Log.i(TAG_CLONED, "findNodeViewByTemplateView  targetView targetView is :" + targetView);
          }
        }
      }else{
          Log.e(TAG_CLONED,"findNodeViewByTemplateView  error fastList is null");
      }

      return targetView;
  }

  public static FastListView findParentFastList(HippyEngineContext context,RenderNode node){
    if (context != null && node != null) {
      //RenderNode node = context.getRenderManager().getRenderNode(view.getId());
      ///Log.i(TAG_CLONED,"findParentFastList node:"+node+",class Name:"+node.getClassName());
      if(FastListViewController.CLASS_NAME.equals(node.getClassName())){
          View fv =  context.getRenderManager().getControllerManager().findView(node.getId());
          if(fv instanceof FastListView){
            return (FastListView) fv;
          }
      }else{
        return findParentFastList(context, node.getParent());
      }
    }
    return null;
  }

  public void setReFocusAfterDiff(boolean reFocusAfterDiff) {
    this.reFocusAfterDiff = reFocusAfterDiff;
  }


  public void diffSetSelectionPosition(int select){
    if(LogUtils.isDebug()) {
      Log.i(TAG, "diffSetSelectionPosition select:" + select + ",getControlProps().singleSelectPosition:" + getControlProps().singleSelectPosition + ",this :" + ExtendUtil.debugViewLite(this));
    }
    if(select != getControlProps().singleSelectPosition){
      this.setSelectChildPosition(select,true);
    }else{
      defaultSectionPosition = select;
    }
    getControlProps().singleSelectPosition = select;
  }

  public int getFocusChildPosition(){
    if(hasFocus()) {
      final View child = getFocusedChild();
      if (child != null) {
        return getChildAdapterPosition(child);
      }
    }
    return -1;
  }

  public void setAutofocusPositionInfiniteMode(int offset){
    this.setAutofocusPosition(convertInfinitePosition(offset));
  }

  public void setAutofocusPosition(int position){
      Log.i(AutoFocusManager.TAG, "list setAutofocusPosition :" + position+",hasFocus:"+hasFocus()+",current autofocusPosition:"+getControlProps().autofocusPosition);
    if(getControlProps().autofocusPosition != position) {
      getControlProps().autofocusPosition = position;
      if (position == -1) {
        return;
      }
      if(!hasFocus()) {
        HippyMap hippyMap = new HippyMap();
        hippyMap.pushInt("focusPosition", position);
        hippyMap.pushInt("scrollToPosition", position);
        hippyMap.pushBoolean("alignCenter", true);
        hippyMap.pushBoolean("autofocus", true);
        setInitPositionInfo(hippyMap);
      }else{
        final View view = findViewByPosition(position);
        //这里
        if(view != null && getFocusChildPosition() != position){
          onRequestAutofocus(this,view,AUTOFOCUS_TYPE_FORCE);
          if(LogUtils.isDebug()) {
            Log.i(AutoFocusManager.TAG, "setAutofocusPosition requestFocus directly on childVisible position" + position);
          }
        } else {
          if(LogUtils.isDebug()) {
            Log.e(AutoFocusManager.TAG, "setAutofocusPosition return on getFocusChildPosition() == position:" + getFocusChildPosition());
          }
          HippyMap hippyMap = new HippyMap();
          hippyMap.pushInt("focusPosition", position);
          hippyMap.pushInt("scrollToPosition", position);
          hippyMap.pushBoolean("alignCenter", true);
          hippyMap.pushBoolean("autofocus", true);
          setInitPositionInfo(hippyMap);
        }
      }
    }else{
      Log.e(AutoFocusManager.TAG,"setAutofocus return on getControlProps().autofocusPosition = position :"+position);
    }
//    setupInitScrollWork(position,);
  }

  public void setAutoSelectPosition(int position){
    Log.i(AutoFocusManager.TAG, ">>>list setAutoSelectPosition :" + position);
    diffSetSelectionPosition(position);
//    if(position != getControlProps().autoScrollToPosition){
//      setAutoscrollPosition(position);
//    }
  }

  public void setAutoscrollPosition(int position){
    setAutoscrollPosition(position,false,0);
  }


  int convertInfinitePosition(int offset){
    return FastAdapter.INFINITE_START_POSITION + offset;
  }

  public void setAutoscrollInfiniteMode(int positionOffset, boolean force, int offset){
      final int finalPosition = convertInfinitePosition(positionOffset);
      this.setAutoscrollPosition(finalPosition,force,offset);
  }

  public void setAutoscrollPosition(int position,boolean force,int offset){
    Log.i(AutoFocusManager.TAG, ">list setAutoscrollPosition :" + position+",focusChildPos:"+getFocusChildPosition());
    if(getControlProps().autoScrollToPosition != position || force){
      getControlProps().autoScrollToPosition = position;
      getControlProps().scrollOffset = offset;
      if(getControlProps().autofocusPosition == position && hasFocus()){
        Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on autofocusPosition == position :" + position);
        return;
      }
      if (getControlProps().autoScrollToPosition == -1) {
        return;
      }
      final int focusChildPos = getFocusChildPosition();
      if(focusChildPos == position && hasFocus()){
        Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on focusChildPos == position :" + position);
        return;
      }
//      HippyMap hippyMap = new HippyMap();setAutoscrollPosition
//      hippyMap.pushInt("scrollToPosition",position);
//      hippyMap.pushBoolean("alignCenter",true);
//      setInitPositionInfo(hippyMap);
      if(!hasFocus() || getChildCount() < 1) {
        setTargetFocusChildPosition(position);
//        int diffScroll = Math.abs(getControlProps().currentScrollToPosition - position);
        boolean anim = false;
//        final View view = findViewByPosition(position);
//        if (view != null) {
//          anim = true;
//        }
        scrollToPosition4Autoscroll(position, anim);
//        HippyMap hippyMap = new HippyMap();
//        hippyMap.pushInt("scrollToPosition",position);
//        hippyMap.pushBoolean("alignCenter",true);
//        setInitPositionInfo(hippyMap);
      }else{
        Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on hasFocus position :" + position);
      }
        //scrollToPosition(position);
    }else{
      Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on autoScrollToPosition == position :" + position);
    }
  }

//  public void diffSetFocusPosition(int position){
////    if(((getControlProps().focusPosition != position)
////      || (getFocusChildPosition() != position))//当焦点不是当前焦点时，也执行
////      && position > -1){
////        //这里证明焦点发生变化，需要获取焦点
////      removeCallbacks(this.pendingDiffScroll);
////      requestPendingScroll(position,position,
////        getControlProps().scrollOffset,null);
////    }
//    getControlProps().autofocusPosition = position;
//  }

  public int getAutofocusPosition(){
    return getControlProps().autofocusPosition;
  }
  public void diffSetScrollToPosition(int position, int offset){
    this.diffSetScrollToPosition(position,offset,50);
  }
  public void diffSetScrollToPosition(int position, int offset,int delay){
    removeCallbacks(this.pendingDiffScroll);
    if((position != getControlProps().scrollToPosition && getControlProps()  .focusPosition != position)
      || offset != getControlProps().scrollOffset && position > -1){
        //setInitPositionInfo();
        requestPendingScroll(-1, position,
          offset, null,delay,true);
    }else{
      Log.e(TAG,"diffSetScrollToPosition return on same position");
    }
    getControlProps().scrollToPosition = position;
    getControlProps().scrollOffset  = offset;
  }

  @Deprecated
  public void setUseDiff(boolean useDiff) {
    this.useDiff = useDiff;
  }

  public boolean isUseDiff() {
    return useDiff;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  protected void applyProps(HippyMap params) {
    setVerticalScrollBarEnabled(false);
    setHorizontalScrollBarEnabled(false);

    final int spanCount = params.containsKey("spanCount") ? params.getInt("spanCount") : 1;
    final int orientation = params.containsKey("horizontal") ? HORIZONTAL : VERTICAL;
    mManagerType = spanCount > 1 ? 2 : 1;
    //setAdapter(mAdapter);
    //notice:使用viewPool会导致列表切换时不刷新，暂不使用
    if(params.containsKey("infiniteParams")){
      infiniteParams = params.getMap("infiniteParams");
      isInfinityLoop = true;

    }else{
      isInfinityLoop = params.containsKey("infiniteMode");
    }

//    Log.i("DebugInfinite","applyProps isInfinityLoop : "+isInfinityLoop+",this:"+ExtendUtil.debugViewLite(this));
//    isInfinityLoop = true;
    if (mAdapter == null) {
      mAdapter = newAdapter();

//      if(params.containsKey("stableIdKey")){
//        mAdapter.setStableIdKey(params.getString("stableIdKey"));
//      }
//      setRecycledViewPool(mAdapter.getCacheWorker(getContext(),this));
//      Log.d("EsPageViewLog", "getCacheWorker set this:" + this+",findPageRootView():"+findPageRootView());
    }


    if (LogUtils.isDebug()) {
      Log.i(TAG, "fastList applyProps spanCount:" + spanCount + ",orientation:" + orientation + ",isInfinityLoop:" + isInfinityLoop);
    }
    if (spanCount > 1) {
      setGridViewLayout(spanCount, orientation, mAdapter);
    } else {
      init(orientation, mAdapter);
    }
    if (params.containsKey("disableAdvancedFocusSearch")) {
      setUseAdvancedFocusSearch(false);
    }
    if (params.containsKey("advancedFocusSearchSpan")) {
      this.advancedFocusSearchMore = params.getInt("advancedFocusSearchSpan");
      if (LogUtils.isDebug()) {
        Log.d(TAG, "advancedFocusSearchSpan set :" + this.advancedFocusSearchMore);
      }
    }
    //padding="60,0,0,0"
    if (params.containsKey("padding")) {
      String paddingString = params.getString("padding");
      if (paddingString.contains(",")) {
        String[] padding = paddingString.split(",");
        this.setPadding(Utils.toPX(padding[0] != null ? Integer.parseInt(padding[0]) : 0), Utils.toPX(padding[1] != null ? Integer.parseInt(padding[1]) : 0),
          Utils.toPX(padding[2] != null ? Integer.parseInt(padding[2]) : 0), Utils.toPX(padding[3] != null ? Integer.parseInt(padding[3]) : 0));
      }
    }
    mItemDecoration = mAdapter.buildListItemDecoration();
    addItemDecoration(mItemDecoration);

//    setItemAnimatorEnable(false);

  }

  public void setItemAnimatorEnable(boolean b){
    if (b) {
      setItemAnimator(new DefaultItemAnimator());
    }else{
      setItemAnimator(null);
    }
  }

  public void updateItemProps(String name, int position, HippyMap dataToUpdate, boolean updateView) {

    if (mAdapter != null) {
      final ViewHolder vh = findViewHolderForAdapterPosition(position);
      mAdapter.updateItemProps(name, position, dataToUpdate, updateView, vh);
      if (vh == null) {
        Log.e(TAG, "updateItemProps error,没有找到对应的Holder，当前View可能没有在展示");
      }
    }
  }

  FastAdapter newAdapter() {
    final FastAdapter a = new FastAdapter();
    a.eventDeliverer = eventDeliverer;
    a.setBoundListView(this);
    a.isInfiniteLoop = isInfinityLoop;
//    setReFocusAfterDiff();
    //a.setShareViewPoolType(hashCode() + "");
    a.setContext(getEngineContext(),getContext());
    return a;
  }


  HippyEngineContext getEngineContext() {
    return ((HippyInstanceContext) getContext()).getEngineContext();
  }

  @Override
  public View getView() {
    return this;
  }

  @Override
  public void notifyRecycled() {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "FastListView notifyRecycled");
    }
    if(resetOnDetach) {
      if (LogUtils.isDebug()) {
        Log.w(TAG, "FastListView resetOnDetach");
      }
      scrollToTop();
      setSelectChildPosition(0, true);
    }
  }

  public void setGroupChildSelectByItemPosition(int anInt) {
  }

  public void scrollToPosition(int pos) {
    this.scrollToPosition(pos, 0);
  }

  public void scrollToPosition(int pos, int offset) {
    this.scrollToPosition(pos, offset, false);
  }

  public void callScrollToInfinitePosition(int pos, int offset, boolean anim) {
    this.callScrollToPosition(convertInfinitePosition(pos),offset,anim);
  }

  public void callScrollToPosition(int pos, int offset, boolean anim) {
    if(pos > -1) {
//      autofocusDirty = true;
//      autofocusDirtyOnDataChange = true;
      if(LogUtils.isDebug()) {
        Log.i(AutoFocusManager.TAG, "callScrollToPosition pos:" + pos + ",offset:" + offset + ",anim:" + anim + ",autofocusPosition:" + getControlProps().autofocusPosition);
      }
      if(getControlProps().autofocusPosition > -1){
        setupInitScrollWork(getControlProps().autofocusPosition,getControlProps().autofocusPosition,0,null,false,0,true);
      }
    }
    this.scrollToPosition(pos,offset,anim);
  }

  public void scrollToPosition4Autoscroll(int pos, boolean anim){
    if(getChildOnScreenScroller().getScrollType() == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER){
      getControlProps().pendingScrollToPosition = pos;
    }
    int offset = getControlProps().scrollOffset;
    if(LogUtils.isDebug()) {
      Log.i(AutoFocusManager.TAG, "handleAutoscroll scrollToPositionWithAlignCenter pos:" + pos + ",offset:" + offset + ",getControlProps().pendingScrollToPosition:" + getControlProps().pendingScrollToPosition);
    }
    //setupInitScrollWork(-1,pos,offset,null,false,0,false);
    this.scrollToPosition(pos,offset,anim);
//    this.updateList(anim);
//    RenderUtil.req
//    RenderUtil.requestNodeLayout(this);
  }


  public void scrollToPosition(int pos, int offset, boolean anim) {

    if (LogUtils.isDebug()) {
      Log.i(AutoFocusManager.TAG, "handleAutoscroll scrollToPosition pos:" + pos + ",offset:" + offset + " ,anim:" + anim);
    }
    getControlProps().currentScrollToPosition = pos;
    stopScroll();
    if (!anim) {
      handleScrollBackToTop(pos);
      getLayoutExecutor().scrollToPositionWithOffset(pos,offset);
//      getLayoutManager().requestLayout();
//      RenderUtil.reLayoutView(this, (int) getX(), (int) getY(), getWidth(), getHeight());
//      super.scrollToPosition(pos);
      requestLayoutManual();
//      requestLayout();`
//      postInvalidateDelayed(16);
//      RenderUtil.requestNodeLayout(this);
    } else {
      handleScrollBackToTop(pos);
      SmoothScroller ss = new MyLinearScroller(this, offset);
      ss.setTargetPosition(pos);
      getLayoutManagerCompat().startSmoothScroll(ss);
    }
  }

  //设置根节点
  public void setTemplateNode(RenderNode templateNode) {
    this.templateNode = templateNode;
  }

  public void setPostContentDelay(int time) {
    if (mAdapter != null) {
      mAdapter.postDelay = time;
    }
  }

  @Override
  public void requestChildFocus(View child, View focused) {
//    if (stateEnableOnFocusNames != null) {
//      if (mFocusChildPosition > -1) {
//        View last = findViewByPosition(mFocusChildPosition);
//        if (last != null) {
//          for(String s : stateEnableOnFocusNames){
//            if(last instanceof StateView){
//              if("selected".equals(s)){
//                last.setSelected(false);
//              }
//              ((StateView) last).setCustomState(s,false);
//            }
//          }
//        }
//      }
//    }
    super.requestChildFocus(child, focused);
    //Log.i("DebugInfinite","requestChildFocus position:"+mFocusChildPosition);
    if (stateEnableOnFocusNames != null) {
      for(String s : stateEnableOnFocusNames){
        if(child instanceof StateView){
          if("selected".equals(s)){
            //child.setSelected(true);
            //diffSetSelectionPosition(mFocusChildPosition);
            this.setSelectChildPosition(mFocusChildPosition,true);
          }
          ((StateView) child).setCustomState(s,true);
        }
      }
    }
  }

  public void requestChildFocus(int pos, int direction) {
    if (LogUtils.isDebug()) {
      LogUtils.e(FocusDispatchView.TAG, "requestChildFocus view:" + getId());
    }

    final View v = findViewByPosition(pos);
    if (v != null) {
      v.requestFocus(direction);
    }
  }

  public void setup() {

  }


  public RenderNode getTemplateNode() {
    return templateNode;
  }

  private FastListNode getRenderNode() {
    if (templateNode != null) {
      if (templateNode instanceof FastListNode) {
        return (FastListNode) templateNode;
      }
    }
    final RenderNode node = Utils.getRenderNode(this);
    if (node instanceof FastListNode) {
      return (FastListNode) node;
    }
    Log.e(TAG, "FastListView getRenderNode return null id :" + getId());
    return null;
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);
//    if(visibility != )
    if(LogUtils.isDebug()) {
      if (visibility == VISIBLE) {
        Log.d(AutoFocusManager.TAG, "FastListView setVisibility :" + "View.VISIBLE" + ",this:" + ExtendUtil.debugView(this));
      } else {
        Log.d(AutoFocusManager.TAG, "FastListView setVisibility :" + (visibility == INVISIBLE ? "invisible" : "gone") + ",this:" + ExtendUtil.debugView(this));
      }
    }
//    if(visibility == View.VISIBLE){
//      final int autofocusPosition = getControlProps().autofocusPosition;
//      final View targetChild = findViewByPosition(autofocusPosition);
//      if (targetChild != null && targetChild.getVisibility() == View.VISIBLE) {
//        if (LogUtils.isDebug()) {
//          Log.i(AutoFocusManager.TAG, "setVisibility find target requestFocus directly :" + targetChild);
//        }
//        onRequestAutofocus(this, targetChild, ExtendViewGroup.AUTOFOCUS_TYPE_FORCE);
//      }else {
//        if (!checkAutoFocus("onSetVisibility", true)) {
//          checkAutoscroll();
//        }
//      }
//    }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    globalVisibility = visibility;
    if(LogUtils.isDebug()) {
      if (visibility == VISIBLE) {
        Log.d(AutoFocusManager.TAG, "FastListView onWindowVisibilityChanged :" + "View.VISIBLE" + ",this:" + ExtendUtil.debugView(this));
      } else {
        Log.d(AutoFocusManager.TAG, "FastListView onWindowVisibilityChanged :" + (visibility == INVISIBLE ? "invisible" : "gone") + ",this:" + ExtendUtil.debugView(this));
      }
    }
    if(pauseTaskOnHide) {
      //Fixme 这里嵌套的tv-list自动将任务pause，后面应该根据一些逻辑判断来执行
      if (visibility != VISIBLE) {
        pausePostTask();
      } else {
        resumePostTask();
      }
    }
    super.onWindowVisibilityChanged(visibility);
    if(visibility == VISIBLE && getControlProps().pendingCheckAutofocus){
      getControlProps().pendingCheckAutofocus = false;
//      autofocusDirty = true;
      if(!checkAutoFocus("onVisibilityChanged",true)){
        checkAutoscroll();
      }
    }

//    checkAutoFocus("visibility changed");
  }
  private int globalVisibility = INVISIBLE;

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
//    Log.e(TAG, "FastListView onLayout width:"+getWidth()+",height:"+getHeight());
//    notifyUpdateItemsLayout();
//    if(getWidth())
    super.onLayout(changed, l, t, r, b);
  }



  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (w > 0 && h > 0) {
      setupListView();
    }
    if (LogUtils.isDebug()) {
      Log.d(TAG, "FastListView onSizeChanged w:" + w + ",h:" + h);
    }
    if(w < 1 || h < 1){
      if(LogUtils.isDebug()){
        Log.i(AutoFocusManager.TAG,"resetAutoWork on size 0 ");
      }
      resetAutoWork();
    }

    if(oldw < 1 && oldh < 1 && w > 0 && h > 0){
      //v-show变化后
      if(globalVisibility == VISIBLE){
//        autofocusDirty = true;
        if(!checkAutoFocus("sizeChanged",true)){
          checkAutoscroll();
        }
      }else{
        getControlProps().pendingCheckAutofocus = true;
      }
    }

  }


  public void requestAutofocusPosition(){
    checkAutoFocus("CustomRequest",true);
  }

  private boolean checkAutoFocus(String state){
    return checkAutoFocus(state,false);
  }
  private boolean checkAutoFocus(String state,boolean force){
    if(LogUtils.isDebug()) {
      Log.i(AutoFocusManager.TAG, "FastListView checkAutoFocus focusPosition on "+ state
        +",autoscrollPosition:"+getControlProps().autoScrollToPosition
        +",focusChildPosition:"+mFocusChildPosition+",hasFocus:"+hasFocus()
         + ",autofocusPosition " + getControlProps().autofocusPosition + " ,view:" + ExtendUtil.debugView(this) +",childCount:"+getChildCount());
    }
//    if(globalVisibility == View.VISIBLE
//      &&  getVisibility() == View.VISIBLE
//      &&  getControlProps().focusPosition > -1
//      && isAttached()
//      && getWidth() > 0 && getHeight() > 0
//      ) {
//      Log.i(AutoFocusManager.TAG,"FastListView auto requestFocus focusPosition on "+state+",position "+getControlProps().focusPosition+" ,view:"+this);
//      diffSetFocusPosition(getControlProps().focusPosition);
//    }

    final int autofocusPosition = getControlProps().autofocusPosition;
    if(getControlProps().autofocusPosition > -1
    ){
      if(getVisibility() == VISIBLE
        && ((getFocusChildPosition() != getControlProps().autofocusPosition) || isFocused() || force)){
        if(LogUtils.isDebug()) {
          Log.e(AutoFocusManager.TAG, "FastListView do autofocus on " + state + ",autofocusPosition " + autofocusPosition + " ,view:" +  ExtendUtil.debugView(this));
        }
        setupInitScrollWork(autofocusPosition,autofocusPosition,0,null,false,0,true);
        if((getChildCount() > 0 || force) && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0)
        {
          updateList();
          return true;
        }else{
          Log.e(AutoFocusManager.TAG,"FastListView do autofocus on childCount:"+getChildCount()+",width:"+getWidth()+",height:"+getHeight());
        }
      }else{
          Log.e(AutoFocusManager.TAG, "should do autofocus return on getVisibility():" + getVisibility()
            + ",getControlProps().autofocusPosition :" + getControlProps().autofocusPosition
            + ",getFocusChildPosition() :" + getFocusChildPosition()
            + ",mFocusChildPosition :" + mFocusChildPosition
            + ",hasFocus() :" + hasFocus()
            + ",isFocused :" + isFocused()
            + ",this :" +  ExtendUtil.debugView(this)
          );
      }

    }
    return false;
  }

  private void checkAutoscroll(){
    if(getVisibility() == VISIBLE && getControlProps().autoScrollToPosition > -1){
      Log.i(AutoFocusManager.TAG,"onSizeChanged exeAutoscroll :"+getControlProps().autoScrollToPosition+",getControlProps().scrollOffset:"+getControlProps().scrollOffset);
      setAutoscrollPosition(getControlProps().autoScrollToPosition,true,getControlProps().scrollOffset);
        //setupInitScrollWork(-1,getControlProps().autofocusPosition,getControlProps().scrollOffset,null,false,0,true);

    }
  }


  private void setupListView() {
//    Log.d(TAG, "FastListView setupListView width:" + getWidth() + ",height:" + getHeight());
//      if(mRealList == null){
//        mRealList = new FastRecyclerView(getContext());
//        mRealList.init();
//        mRealList.setAdapter(mAdapter);
//        addView(mRealList,new ViewGroup.LayoutParams(getWidth(),getHeight()));
//      }
    if(customFadeLength < 0) {
      float defaultLength = FastListModule.getGlobalConfig(getEngineContext()).defaultFadingEdgeLength;
      if (defaultLength > 0) {
        boolean horizontal = getLayoutManagerCompat().getOrientation() == HORIZONTAL;
        int length;
        if (horizontal) {
          length = (int) (getWidth() * defaultLength);
          setHorizontalFadingEdgeEnabled(true);
        } else {
          length = (int) (getHeight() * defaultLength);
          setVerticalFadingEdgeEnabled(true);
        }
        if (length > 0) {
          super.setFadingEdgeLength(length);
        }
        invalidate();
      }
    }
  }

  @Override
  public void setFadingEdgeLength(int length) {
    super.setFadingEdgeLength(length);
    this.customFadeLength = length;
  }

  public void setList(HippyArray array) {
    this.setListWithParams(array, false, useDiff);
  }

  public void setListWithParams(HippyArray array, boolean autoChangeVisible, boolean useDiff) {
    //clearAllTask();
    final FastListNode node = getRenderNode();
    if (LogUtils.isDebug()) {
      Log.i(TAG, "FastListView TEST_FOCUS setList node:" + node + ",array :" + array + ",hasFocus:" + hasFocus());
    }
    assert (node != null);
    this.setPendingData(array, node, autoChangeVisible, useDiff);
  }


  public void notifyPause() {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "DebugPool notifyPause,this:" + Utils.hashCode(this));
    }
    notifyChildrenSaveState();
  }

  public void notifyRestore() {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "DebugPool notifyRestore,this:" + Utils.hashCode(this));
    }
    notifyChildrenRestoreState();
  }

  public void setDisplay(boolean display, boolean autoDataState) {
    if (LogUtils.isDebug()) {
      if (display) {
        Log.e(TAG, "++++DebugPool setDisplay display:" + true + ",this:" + Utils.hashCode(this) + ",autoDataState:" + autoDataState);
      } else {
        Log.e(TAG, "----DebugPool setDisplay display:" + false + ",this:" + Utils.hashCode(this) + ",autoDataState:" + autoDataState);
      }
    }
    if (!display) {
      pendingChangeVisible = false;
      removeCallbacks(changeVisibilityRunnable);
    }
    if (isDisplayed == display) {
      if (LogUtils.isDebug()) {
        Log.d(TAG, "----DebugPool setDisplay ");
      }
      return;
    }
    this.isDisplayed = display;
    //notifyRestore();
    if (!display) {
      changeVisibility4Display(false);
    } else {
      changeVisibility4Display(true);
    }
    if (autoDataState) {
      //notifyPause();
    }
  }


  @Override
  public void requestLayout() {
    super.requestLayout();
//    ExtendUtil.layoutViewManual(this);
  }

  public void clearInitFocusPosition() {
    if (mInitParams != null) {
      mInitParams.targetFocusPosition = -1;
    }
  }


  public void setupInitScrollWork(int position, int scrollToPosition, int offset, String focusName, boolean block, int delay, boolean alignCenter) {
    if (position < 0 && scrollToPosition < 0) {
      clearInitFocusPosition();
    } else {
      mInitParams = new InitParams(position, scrollToPosition, offset, focusName, block, delay, alignCenter);
      if(LogUtils.isDebug()) {
        Log.i(AutoFocusManager.TAG, "setupInitScrollWork initParams:" + mInitParams);
      }
      if (block) {
        if (LogUtils.isDebug()) {
          Log.e("PendingFocus", "setPendingFocusChild blockRootFocus!!");
        }

        InternalExtendViewUtil.blockRootFocus(this);
        if (hasFocus()) {
          this.clearFocus();
        }
      }
    }

  }

  @Override
  public void clearFocus() {
    super.clearFocus();
    mFocusChildPosition = -1;
  }

  private Runnable hideTask;

  private void hideAWhile(int time) {
    //暂时将自己隐藏一会，主要用来解决列表重设数据，多次变更的问题
    if (LogUtils.isDebug()) {
      Log.i(TAG, "hideAWhile time:" + time + ",this:" + ExtendUtil.debugViewLite(this));
    }
    if (hideTask != null) {
      removeCallbacks(hideTask);
    }
    setAlpha(0);
    hideTask = () -> setAlpha(1);
    postDelayed(hideTask, time);
  }

  void updateList(boolean anim){
    if (mInitParams != null) {
      if (LogUtils.isDebug()) {
        Log.i("PendingFocus", "after setListData  scrollToPosition:" + mInitParams + ",id:" + getId());
      }
      if (mInitParams.alignCenter) {
        //只有滚动居中时才需要设置pendingWork
        getLayoutManagerCompat().setPendingScroll(mInitParams.scrollToPosition, mInitParams.scrollOffset);
      }
      scrollToPosition(mInitParams.scrollToPosition, mInitParams.scrollOffset,anim);
//      RenderUtil.requestNodeLayout(this);
    }
  }

  void updateList() {
      this.updateList(false);
  }

  protected int getScrollToPosition() {
    return mInitParams == null ? -1 : mInitParams.scrollToPosition;
  }

  public void scrollToFocus(int pos) {
    this.scrollToFocus(pos, 0, -1, null);
  }
  public void scrollToFocus(int pos, int scrollOffset, int delay, String target) {
      this.scrollToFocus(pos,scrollOffset,delay,target,true);
  }

  // FIXME sdk<19时，isInLayout方法会报VerifyError，需要删除
  public void scrollToFocus(int pos, int scrollOffset, int delay, String target,boolean force) {
    Log.i(TAG,"scrollToFocus pos:"+pos+",scrollOffset:"+scrollOffset+",delay:"+delay+",target:"+target);

      final HippyMap hm = new HippyMap();
      hm.pushInt("focusPosition", pos);
      hm.pushBoolean("force", force);
      hm.pushBoolean("alignCenter", true);
      hm.pushBoolean("blockOthers", true);
      hm.pushBoolean("hide", false);
      hm.pushInt("scrollToPosition", pos);
      if (target != null) {
        hm.pushString("target", target);
      }
      if (scrollOffset != 0) {
        hm.pushInt("scrollOffset", scrollOffset);
      }
      if (delay > 0) {
        hm.pushInt("delay", delay);
      }
      this.clearInitFocusPosition();
      if (pos == 0 && scrollOffset == 0) {
        //如果回到0，需要通知vue层scrollToTop
        mOffsetY = 0;
        mOffsetX = 0;
        handleScrollValue(true);
        resetScrollOffset();
        if (mScrollToTopListener != null) {
          mScrollToTopListener.onScrolled(this, 0, 0);
        }
        resetScrollOffset();
      }
    this.stopScroll();
    if (!isComputingLayout() && !isInLayout()) {
      this.setInitPositionInfo(hm);
    } else {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "scrollToFocus on error state postTask:" + lastState);
      }
    }
  }

  @Override
  protected void resetScrollOffset() {
    super.resetScrollOffset();
    if (mAdapter != null) {
      mAdapter.clearDetachCaches();
    }
  }

  @Override
  protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);
    if(visibility == VISIBLE){
//      autofocusDirty = true;
    }
  }




  public void requestPendingScroll(int focusPosition, int scrollToPosition, int scrollOffset, final String target,int postDelay,boolean alignCenter){
    removeCallbacks(this.pendingDiffScroll);
    //Log.i(AutoFocusManager.TAG,"requestPendingScroll focusPosition:"+focusPosition+",scrollToPosition:"+scrollToPosition+",scrollOffset:"+scrollOffset+",target:"+target);
    this.setupInitScrollWork(focusPosition,scrollToPosition,scrollOffset,null,true,0,alignCenter);
//    hideAWhile(200);
    this.pendingDiffScroll = new Runnable() {
      @Override
      public void run() {
//        hideAWhile(0);
        updateList();
      }
    };
    postDelayed(this.pendingDiffScroll,postDelay);
  }

  public void setInitPositionInfo(HippyMap map) {
    /*
     :initPosition=
     {
     // 请求焦点的位置可以不传，不传不会请求焦点
     focusPosition:5,
     //首次滚动的位置, 可以不传
     scrollToPosition:3,
     //首次滚动的偏移值, 可以不传
     scrollOffset:200,
     }
     *
     **/
    if (map == null || map.size() < 1) {
      clearInitFocusPosition();
      return;
    }
    int position = map.containsKey("focusPosition") ? map.getInt("focusPosition") : -1;
    if (position < 0) {
      position = map.containsKey("position") ? map.getInt("position") : -1;
    }
    final int scrollToPosition = map.containsKey("scrollToPosition") ? map.getInt("scrollToPosition") : -1;
    final int scrollOffset = map.containsKey("scrollOffset") ? map.getInt("scrollOffset") : 0;

    final String target = map.containsKey("target") ? map.getString("target") : null;
//    final boolean oneShot = map.getBoolean("oneShot");
    final boolean block = map.containsKey("blockOthers") && map.getBoolean("blockOthers");
    final boolean force = map.getBoolean("force");
    final boolean alignCenter = map.getBoolean("alignCenter");
    final boolean autofocus = map.getBoolean("autofocus");
    if(alignCenter){
      if(scrollToPosition > 0){
        setTargetFocusChildPosition(scrollToPosition);
      }
    }
    final int initTargetFocusPosition = map.containsKey("nextTargetFocusPosition") ? map.getInt("nextTargetFocusPosition") : -1;
    if(initTargetFocusPosition > -1){
      setTargetFocusChildPosition(initTargetFocusPosition);
    }
    final int delay = map.containsKey("delay") ? map.getInt("delay") : -1;
    final boolean hideBeforeScroll = !map.containsKey("hide") || map.getBoolean("hide");
    if (position < 0 && scrollToPosition < 0) {
      this.clearInitFocusPosition();
      return;
    }
    if(autofocus && position > -1){
      getControlProps().autofocusPosition = position;
    }

    boolean requestFocus = position > -1;
    if (LogUtils.isDebug()) {
      Log.d("PendingFocus", "begin initPosition requestFocus:" + requestFocus+",autofocus:"+autofocus);
    }
    if (requestFocus) {
      final View targetView = findTargetChildFocus(position, target);
      if (LogUtils.isDebug()) {
        Log.d("PendingFocus", "findTargetChildFocus findTargetChildFocus:" + targetView);
      }
      if (targetView != null && targetView.getVisibility() == VISIBLE) {
//        requestTargetFocus(block,delay,position,target);
        setupInitScrollWork(position, scrollToPosition, scrollOffset, target, block, delay, alignCenter);
        if (force) {
          if (hideBeforeScroll) {
            hideAWhile(300);
          }
          updateList();
        }
      } else {
        setupInitScrollWork(position, scrollToPosition, scrollOffset, target, block, delay, alignCenter);
        if (force) {
          if (hideBeforeScroll) {
            hideAWhile(300);
          }
          updateList();
        }
        if (scrollToPosition > -1) {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "scrollToPositionWithScrollType Directly");
          }
//          scrollToPositionWithScrollType(scrollToPosition,scrollOffset);
        } else {
//          scrollToPositionWithScrollType(position,scrollOffset);
        }
      }
    } else {
      setupInitScrollWork(-1, scrollToPosition, scrollOffset, target, block, delay, alignCenter);
      if (LogUtils.isDebug()) {
        Log.e("PendingFocus", "scrollToPositionWithScrollType Directly!! scrollToPosition:" + scrollToPosition + ",offset:" + scrollOffset + ",id:" + getId());
      }
      if (force) {
        hideAWhile(300);
        updateList();
      }
//      scrollToPositionWithScrollType(scrollToPosition,scrollOffset);
    }


  }


  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    //fixme 暂不置空
//    pageRootView = null;
    if (LogUtils.isDebug()) {
      Log.e(TAG, "FastList onDetachedFromWindow this:" + hashCode());
    }
//    if (mAdapter != null) {
//      mAdapter.clearDetachCaches();
//    }
  }

  void recycleAdapter() {
    if (mAdapter != null) {
      mAdapter.clearCache();
    }
  }

  public RenderNode getTemplateNodeByPosition(int position){
    if (mAdapter != null) {
      int type = mAdapter.getItemViewType(position);
      return mAdapter.getTemplateNodeByType(type);
    }
    return null;
  }


  @Override
  public NativeGestureDispatcher getGestureDispatcher() {
    return mGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {
    mGestureDispatcher = dispatcher;
  }

  public void updateItem(int pos, Object data) {
    this.updateItem(pos,data,true);
  }
  @Override
  public void updateItem(int pos, Object data,boolean traverse) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "updateItem pos:" + pos+"-----------------start");
      Log.d("DebugReplaceItem", "updateItem pos:" + pos+"-----------------start "+data);
    }
    final HippyArray array = new HippyArray();
    array.pushObject(data);
    this.updateItemRange(pos, array,traverse);
    requestLayoutManual();
  }

  public void diffUpdateItem(int pos,Object data){
    Log.d(TAG, "diffUpdateItem pos:" + pos+",this:"+this);
    //

  }

  /**
   * 从某个位置插入数据
   *
   * @param pos
   * @param array
   */
  public void insertItemRange(int pos, HippyArray array) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "insertItem pos:" + pos);
    }
    if (array != null && array.size() > 0) {
      this.updateItemRangeInserted(pos, array, true);
    } else {
      if (LogUtils.isDebug()) {
        Log.d(TAG, "insertItem fail : array = null or array.size() = 0");
      }
    }
  }

  /**
   * 根据数据指定的id，来寻找对应item刷新
   *
   * @param id
   * @param newData
   */
  public void updateItemMatched(String idKey, Object id, Object newData) {
    HippyArray array = getFastAdapter().dataList;
    if (array != null) {
      final int itemPosition = Utils.searchFastAdapterData(array, id, idKey);
      if (itemPosition > -1) {
        updateItem(itemPosition, newData);
      } else {
        Log.e(TAG, "updateItemMatched on itemPosition error:" + itemPosition);
      }
    }
  }


  public void requestItemLayout(int pos) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "updateItemLayout pos:" + pos);
    }

    if (mAdapter != null) {
      final ViewHolder vh = findViewHolderForAdapterPosition(pos);
      if (vh instanceof FastAdapter.Holder) {
        mAdapter.requestItemLayout((FastAdapter.Holder) vh, pos);
      }
    } else {
      Log.e(TAG, "updateItemLayout error,adapter is null pos:" + pos);
    }
  }

  @Override
  public void updateItemProps(String name, int pos, Object data, boolean updateView) {
    updateItemProps(name, pos, (HippyMap) data, updateView);
  }


  private HippyMap recycledViewPoolMap;
  private String poolName = hashCode()+"";

  public HippyMap getRecycledViewPoolMap() {
    if (recycledViewPoolMap == null) {
      recycledViewPoolMap = new HippyMap();
    }
    return recycledViewPoolMap;
  }



  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    //checkAutoFocus("onAttachedToWindow");
//    Log.d("EsPageViewLog", "onAttachedToWindow this:" + this+",root："+findPageRootView());
    updateCachePoolName();
    updateCachePoolMap();
  }

  public void setCachePoolName(String name) {
    this.poolName = name;
    if(isAttached()){
      updateCachePoolName();
    }
  }

  private void updateCachePoolName(){
    mAdapter.setShareViewPoolType(poolName);
    final RecycledViewPool pool = mAdapter.getCacheWorker(getContext(),this);
    setRecycledViewPool(pool);
    if (LogUtils.isDebug()) {
      Log.i(TAG, "setCachePoolMap name=" + poolName + ",this:" + this);
      Log.i("EsPageViewLog", "DebugPool setupPool  instance:" + Utils.hashCode(pool)+",context:"+getContext());
    }
  }

  private void updateCachePoolMap(){
    final HippyMap map = recycledViewPoolMap;
    if (map == null) {
      return;
    }
    if (mAdapter != null) {
      if (map.containsKey("name")) {
        mAdapter.setShareViewPoolType(map.getString("name"));
        final RecycledViewPool pool = mAdapter.getCacheWorker(getContext(),this);
        setRecycledViewPool(pool);
        if (LogUtils.isDebug()) {
          Log.e("DebugPool", "setCachePoolMap name=" + map.getString("name") + ",this:" + this);
          Log.e("DebugPool", "DebugPool setupPool  instance:" + Utils.hashCode(pool)+",context:"+getContext());
        }
        HippyMap size = map.getMap("size");
        for (String typeS : size.keySet()) {
          final int type = Integer.parseInt(typeS);
          final int count = size.getInt(typeS);
          if (LogUtils.isDebug()) {
            Log.e("DebugPool", "setCachePoolMap type:" + type + ",count:" + count + ",this:" + this);
          }
          if (mAdapter.findOldCacheWorker() != null) {
            mAdapter.findOldCacheWorker().setMaxCacheSize(type, count);
          }
        }
      }
    }
  }


  @Override
  public void setCachePoolMap(HippyMap map) {
    this.recycledViewPoolMap = map;
    if(isAttached()){
      updateCachePoolMap();
    }
  }

  @Override
  protected void onLayoutComplete(State state) {
    super.onLayoutComplete(state);
    if (LogUtils.isDebug()) {
      Log.e(TAG, "~~~~~~onLayoutComplete pendingChangeVisible " + pendingChangeVisible + ",this:" + Utils.hashCode(this)+",autofocusDirty:"+ autofocusDirtyOnDataChange);
      Log.i(TAG,"onLayoutComplete selectPosition:"+getSelectChildPosition()+",select:"+findSelectedChild());
    }
    if (pendingChangeVisible) {
      pendingChangeVisible = false;
      changeVisibility4Display(true);
      this.isDisplayed = true;
      changeVisibilityRunnable = null;
    }
    if (mAdapter != null) {
      mAdapter.onLayoutComplete(state,this);
    }
//    handleAutoscroll();
//    handleAutoscroll();
    if(autofocusDirtyOnDataChange && state.getItemCount() > 0){
      autofocusDirtyOnDataChange = false;
      if(!checkAutoFocus("layoutComplete",true)){
        checkAutoscroll();
      }
    }
    handlePendingFocus();
  }

  protected void handleAutoscroll(){
//    if(getControlProps().autoScrollToPosition > -1){
//      int pos = find
//    }
    Log.v(TAG,"handleAutoscroll called  pendingScrollToPosition"+ getControlProps().pendingScrollToPosition );
    if(getControlProps().pendingScrollToPosition > -1){
      //final View targetChild = findViewByPosition(position);
      int position = getControlProps().pendingScrollToPosition;
      final View target = findViewByPosition(position);
      if (target == null) {
        Log.e(TAG,"handleAutoscroll on pendingScrollToPosition find target "+ null +",position:"+position);
          return;
      }
      Log.i(TAG,"handleAutoscroll on pendingScrollToPosition find target "+target+",position:"+position);
      if(getChildOnScreenScroller().getScrollType() == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER) {
        //scrollToPositionWithAlignCenter(mInitParams.targetScrollToPosition);
        int scrollOffset = ChildOnScreenScroller.getScrollToPositionOffset(this,position,target,0);
        Log.i(TAG,"handleAutoscroll on pendingScrollToPosition scrollBy "+scrollOffset+",position:"+position);
        if(scrollOffset != 0) {
          if (getOrientation() == VERTICAL) {
            scrollBy(0, scrollOffset);
            requestLayoutManual();
          } else {
            scrollBy(scrollOffset, 0);
            requestLayoutManual();
          }
        }
      }
      getControlProps().pendingScrollToPosition  = -1;
    }
  }

  protected void handlePendingFocus() {
    if (mInitParams != null) {
      if (mInitParams.blockOthers) {
        InternalExtendViewUtil.unBlockRootFocus(this);
      }
      if (mInitParams.targetFocusPosition > -1) {
        final int position = mInitParams.targetFocusPosition;
        final View targetChild = findViewByPosition(position);
        if (targetChild != null && targetChild.getVisibility() == VISIBLE) {
          if (LogUtils.isDebug()) {
            Log.i("PendingFocus", "handlePendingFocus find target:" + targetChild);
          }
          //targetChild.requestFocus();
          //AutoFocusManager.globalRequestFocus(targetChild);
          onRequestAutofocus(this,targetChild, AUTOFOCUS_TYPE_FORCE);
        } else {
          Log.e("PendingFocus", "handlePendingFocus error on no target shown : " + targetChild);
        }
        if (LogUtils.isDebug()) {
          Log.i("PendingFocus", "~~~~~~onLayoutComplete handlePendingFocus targetFocusPosition " + mInitParams.targetFocusPosition + ",targetChild:" + targetChild);
        }
        mInitParams.targetFocusPosition = -1;
      }
      if (mInitParams.targetName != null) {
        final View targetView = ExtendUtil.findViewByName(mInitParams.targetName,this);
        if (targetView != null && targetView.getVisibility() == VISIBLE) {
          if (LogUtils.isDebug()) {
            Log.i("PendingFocus", "handlePendingFocus find target by name :" + targetView);
          }
          onRequestAutofocus(this,targetView, AUTOFOCUS_TYPE_FORCE);
        }
      }
    }
  }

  void changeVisibility4Display(boolean b) {
    this.isDraw = b;
    invalidate();
  }


  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (!isDraw) {
      return;
    }
    super.dispatchDraw(canvas);
  }

  @Override
  public void replaceItemData(int pos, Object data) {
    if (mAdapter != null) {
      mAdapter.replaceItemData(pos,data);
    }
  }

  void updateItemRange(int pos, HippyArray data, boolean traverse) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "updateItemRange pos:" + pos + ",count:" + 1 + ",data List size:" + data.size());
    }

    if (mAdapter != null) {
      if (mAdapter.getItemCount() > pos && pos > -1) {
        mAdapter.updateItemDataRange(pos, 1, data);
//        final ViewHolder vh = findViewHolderForAdapterPosition(pos);
//        if (vh instanceof FastAdapter.Holder) {
//          mAdapter.updateItemOnReplace((FastAdapter.Holder) vh, pos,traverse);
//          mAdapter.requestItemLayout((FastAdapter.Holder) vh, pos);
//        } else {
          Log.d(TAG, "updateItemRange pos:" + pos + ",count:" + 1 + " notifyItemChanged this:"+ExtendUtil.debugViewLite(this));
          HippyMap params = new HippyMap();
          params.pushBoolean("traverse",traverse);
          mAdapter.notifyItemChanged(pos,new FastAdapter.Payload(FastAdapter.PayloadType.TYPE_UPDATE,params));
//        }
      } else {
        Log.e(TAG, "updateItemRange error,pos invalid, pos:" + pos + ",adapter count :" + (mAdapter == null ? 0 : mAdapter.getItemCount()));
      }
    } else {
      Log.e(TAG, "updateItemRange error,adapter is null pos:" + pos + ",count:" + 1);
    }
  }

  public int getScrolledOffset(){
    if (getOrientation() == HORIZONTAL) {
      return getOffsetX();
    }else{
      return getOffsetY();
    }
  }

  public void updateItemRange(int pos, int count, HippyArray data, boolean updateLayout) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "updateItemRange start pos:" + pos + ",count:" + count + ",data List size:" + data.size() +",currentIndex:"+getScrollX()+",getScrollY"+getScrollY());

    }
    if (mAdapter != null) {
      final int itemCount = mAdapter.getItemCount();
      if (pos < 0 || (pos + count) > itemCount) {
        Log.e(TAG, "updateItemRange error on wrong state, pos:" + pos + ",count:" + count);
        return;
      }
      mAdapter.updateItemDataRange(pos, count, data);
      mAdapter.notifyItemRangeChanged(pos, count, new FastAdapter.Payload(FastAdapter.PayloadType.TYPE_UPDATE,null));
      if (updateLayout) {
        requestLayoutManual();
      }
    } else {
      Log.e(TAG, "updateItemRange error,adapter is null pos:" + pos + ",count:" + count);
    }
  }

  public void notifyItemsLayoutChanged(){
    if(mAdapter != null && getChildCount() > 0) {
      View firstChild = getChildAt(0);
      int start = firstChild == null ? -1 :  findPositionByChild(firstChild);
      if (LogUtils.isDebug()) {
        Log.d(TAG, "notifyUpdateItemsLayout start:" + start + ",childCount:" + getChildCount()+",this:"+ExtendUtil.debugViewLite(this));
      }
      if(start > -1){
        mAdapter.notifyItemRangeChanged(start, getChildCount(), new FastAdapter.Payload(FastAdapter.PayloadType.TYPE_LAYOUT,null));
        requestLayoutManual();
      }
    }
  }

//  public void updateItemsLayout(){
//    Log.i(TAG,"updateItemsLayout");
//    if (mAdapter != null) {
//      mAdapter.notifyitem
//    }
//  }

  public void updateItemRangeInserted(int pos, HippyArray data, boolean updateLayout) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "updateItemRangeInserted pos:" + pos + ",data List size:" + data.size());
    }
    if (mAdapter != null) {
      final int itemCount = mAdapter.getItemCount();
      final int count = data.size();
      if (pos > itemCount || pos < 0) {
        Log.e(TAG, "updateItemRangeInserted error on IndexOutOfBoundsException, pos:" + pos);
        return;
      }
      mAdapter.insertItemDataRange(pos, count, data);
      mAdapter.notifyItemRangeInserted(pos, count);
      if(pos == 0){
        if(LogUtils.isDebug()){
          Log.d(TAG,"updateItemRangeInserted at position 0 getScrolledOffset :"+getScrolledOffset());
        }
        if(getScrolledOffset() < 10){
          scrollToTop();
        }
      }

      if (updateLayout) {
        requestLayoutManual();
      }
    } else {
      Log.e(TAG, "updateItemRangeInserted error,adapter is null pos:" + pos);
    }
  }

  public void deleteItemRange(int pos, int count) {
    if (mAdapter != null) {
      mAdapter.deleteData(pos, count);
      mAdapter.notifyItemRangeRemoved(pos, count);
      requestLayoutManual();
    } else {
      Log.e(TAG, "deleteItemRange error,adapter is null pos:" + pos + ",count:" + count);
    }
  }

  public void addData(HippyArray data, int deleteCount) {
    if (mAdapter == null || mAdapter.dataList == null || mAdapter.dataList.size() < 1) {
      Log.i(TAG, "addData on mAdapter empty call setListWithParams");
      setListWithParams(data, false, useDiff);
      return;
    }
    if (LogUtils.isDebug()) {
      Log.d(FocusDispatchView.TAG,"addData called deleteCount: "+deleteCount+",data size:" + (data == null ? 0 : data.size()));
    }
//    Log.i(TAG,"add data this:"+Utils.hashCode(this)+",data size:"+(data == null ? 0 : data.size()));
    if (data != null && data.size() > 0) {
      boolean hasFocus = hasFocus();
      if (deleteCount > 0) {
        int deleteStart = mAdapter.getItemCount() - deleteCount;
        if (LogUtils.isDebug()) {
          Log.i(TAG, "addData on mAdapter deleteStart :" + deleteStart);
        }
        //Log.i(FocusDispatchView.TAG, "addData on data size :" + data.size());
        if(!hasFocus) {
          FocusDispatchView.blockFocus(this);
        }
        mAdapter.deleteData(deleteCount);
        mAdapter.notifyItemRangeRemoved(deleteStart, deleteCount);
      }
      int start = mAdapter.getItemCount();
      if (LogUtils.isDebug()) {
        Log.i(TAG, "addData array size :" + data.size() + ",start:" + start + ",data:" + data);
      }
      if(!hasFocus) {
        FocusDispatchView.blockFocus(this);
      }
      mAdapter.addData(data);
      mAdapter.notifyItemRangeInserted(start, data.size());
      requestLayoutManual();
      if(!isPostTaskPaused() && !hasFocus) {
        postDelayed(() -> FocusDispatchView.unBlockFocus(FastListView.this), 100);
      }
    } else {
      if (LogUtils.isDebug()) {
        Log.i(TAG, "addData array is null or empty");
      }
    }
  }

//  public void addData(HippyArray data){
//    if(mAdapter == null || mAdapter.dataList == null || mAdapter.dataList.size() < 1){
//      Log.e(TAG,"addData on mAdapter empty call setListWithParams");
//      setListWithParams(data,false);
//      return;
//    }
//    if(data != null && data.size() > 0) {
//      int start = mAdapter.getItemCount();
//      if(LogUtils.isDebug()){
//        Log.i(TAG,"addData array size :"+data.size()+",start:"+start+",data:"+data);
//      }
//      mAdapter.addData(data);
//      FocusDispatchView.blockFocus(this);
//      mAdapter.notifyItemRangeInserted(start, data.size());
//      requestLayoutManual();
//      postDelayed(() -> FocusDispatchView.unBlockFocus(FastListView.this), 100);
//    }else{
//      if(LogUtils.isDebug()){
//        Log.i(TAG,"addData array is null or empty");
//      }
//    }
//  }


  //根据viewpager的节点创建新的fastlist
  public void createFastList(Object data, RenderNode templateNode) {
    setPendingData(data, templateNode, true, useDiff);
  }


  @Override
  public void setHandleEventNodeId(int id) {
    super.setHandleEventNodeId(id);
    if (mAdapter != null) {
      mAdapter.rootListNodeID = id;
    }
  }

  public void setPendingData(Object data, RenderNode templateNode, boolean changeVisibility) {
    this.setPendingData(data, templateNode, changeVisibility, this.isUseDiff());
  }

  @Override
  public void setSharedItemStore(String name) {
    if (mAdapter != null) {
      mAdapter.setShareItemStoreName(name);
    }
  }

  public boolean isInfiniteLoopEnabled(){
    return isInfinityLoop;
  }

  public void setPendingData(Object data, RenderNode templateNode, boolean changeVisibility, boolean useDiff) {
    if(destroyed){
      Log.e(TAG,"setPendingData return on FastListView destroyed，this"+this);
      return;
    }
//    Log.d(, "setPendingData this:" + this+",root:"+findPageRootView());
    if(LogUtils.isDebug()) {
      Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER clearAllTask on setPendingData this:"+this+",useDiff:"+useDiff+",hasSet:"+hasSet);
    }
    clearAllTask();
    if (data instanceof HippyArray) {
      HippyArray newData = new HippyArray();
      final int newSize = ((HippyArray) data).size();
      for (int i = 0; i < newSize; i++) {
        newData.pushObject(((HippyArray) data).get(i));
      }
      final FastListNode node = templateNode instanceof FastListNode ? (FastListNode) templateNode : getRenderNode();
      if (LogUtils.isDebug()) {
        Log.i(TAG, "DebugPool setPendingData setList this:" + Utils.hashCode(this) + ",id:" + getId() + "dataCount:" + newData.size() + ",isComputingLayout:" + isComputingLayout()
        +",stableKey:"+mAdapter.getStableIdKey());
        Log.d(TAG, "DebugPool setPendingData setList array:" + newData);
//        Log.d(TAG, "setPendingData setList node:" + node);
        Log.d(TAG, "setPendingData setPendingData setList this:" + Utils.hashCode(this) + ",id:" + getId() + ",isComputingLayout:" + isComputingLayout());
      }
      if (isComputingLayout()) {
        Log.e(TAG, "setPendingData 出现错误 ： isComputingLayout true ,layoutManager:" + getLayoutManagerCompat());
        return;
      }
//      if(mItemDecoration != null){
//        stopScroll();
//        if(!isInLayout() && !getMyLayoutManager().isSmoothScrolling()) {
//          removeItemDecoration(mItemDecoration);
//        }
//      }
      if (mManagerType == 1) {
        assert getLayoutManagerCompat() != null : "没有设置LayoutManager";
      }
//      if(mAdapter.getStableIdKey() != null) {
//        mAdapter.setStableIdKey(mAdapter.getStableIdKey());
//      }

      if (changeVisibility) {
        removeCallbacks(changeVisibilityRunnable);
        changeVisibility4Display(false);
        this.pendingChangeVisible = true;
      }

      if(newSize > 0 && isInfinityLoop){
        int scrollTo = FastAdapter.INFINITE_START_POSITION;
        int scrollOffset = 0;
        int selectPosition = -1;
        if (infiniteParams != null) {
          if(infiniteParams.containsKey("autoscroll")){
            HippyArray array = infiniteParams.getArray("autoscroll");
            scrollTo = convertInfinitePosition(array.getInt(0));
            scrollOffset = (int) PixelUtil.dp2px(array.getInt(1));
          }else{
            if(infiniteParams.containsKey("scrollOffset")){
              scrollOffset = (int) PixelUtil.dp2px(infiniteParams.getInt("scrollOffset"));
            }else{
              final int itemSize = (int) PixelUtil.dp2px(infiniteParams.getInt("itemSize"));
              if (getWidth() < 1 || getHeight() < 1) {
                Log.e(TAG,"setPendingData compute scrollOffset Error on parent size invalid");
              }else {
                scrollOffset = ChildOnScreenScroller.computeAlignCenterScrollOffset(getOrientation() == VERTICAL ? getHeight() : getWidth(), itemSize);
              }
            }
          }
          if(infiniteParams.containsKey("selectPosition")) {
            selectPosition = infiniteParams.getInt("selectPosition");
          }
          if(infiniteParams.containsKey("minChildScale")){
            minScale = (float) infiniteParams.getDouble("minChildScale");
          }
        }
        //scrollToPosition(scrollTo);
        //diffSetScrollToPosition(scrollTo,scrollOffset);
        setAutoscrollPosition(scrollTo,true,scrollOffset);
        if (selectPosition > -1) {
          diffSetSelectionPosition(convertInfinitePosition(selectPosition));
          setTargetFocusChildPosition(selectPosition);
        }else{
          setTargetFocusChildPosition(scrollTo);
          getFirstFocusHelper().setFindAtStart(true);
        }
        if(LogUtils.isDebug()) {
          Log.i("DebugInfinite", "fix infinite scroll on setPendingData scrollTo" + scrollTo + ",scrollOffset:" + scrollOffset + ",selectPosition:" + selectPosition + ",minScale:" + minScale);
        }
      }

      if (newSize < 1 && mAdapter != null) {
        mAdapter.onDetachedFromView(this);
        autofocusDirtyOnDataChange = true;
        mAdapter.clearData();
        resetAutoWork();
        if (hasSet) {
          mAdapter.notifyDataSetChanged();
          requestLayoutManual();
        }
        return;
      }

      assert (node != null);
      if (mAdapter != null && mAdapter.getItemCount() > 0) {
//        Log.d(TAG, "DebugPool setPendingData find items count:" + mAdapter.getItemCount());
        mAdapter.onDetachedFromView(this);
//        mAdapter.clearData(); TODO 感觉这里不需要clear 而且影响下面逻辑
//        setAdapter(null);
//        recycle();
//        removeAllViews();
      }

      //mAdapter = newAdapter();
      if (mAdapter != null) {
        if (node.getId() > 0) {
          mAdapter.rootListNodeID = node.getId();
          if (LogUtils.isDebug()) {
            Log.d(TAG, "setRootListNodeID id:" + node.getId() + ",this:" + this);
          }
        }
        mAdapter.setListNode(node);

//        mAdapter.setData(newData);

        mAdapter.onAttachedToView(this);
//        mItemDecoration = mAdapter.buildListItemDecoration();
        //addItemDecoration(mItemDecoration);
        mAdapter.onBeforeChangeAdapter();

        if(destroyed){
          Log.e(TAG,"setPendingData return on FastListView destroyed，this"+this);
          return;
        }
        if (hasSet) {
          if (useDiff) {
            // 处理diff
            final HippyArray oldData = mAdapter.dataList;
            handleDataDiff(oldData, newData);
          } else {
            resetAutoWork();
            autofocusDirtyOnDataChange = true;
            mAdapter.setData(newData);
            mAdapter.notifyDataSetChanged();
            if(isLayoutAnimationAutoPlay){
              scheduleLayoutAnimation();
            }
          }
          requestLayoutManual();
        } else {
          resetAutoWork();
          autofocusDirtyOnDataChange = true;
          mAdapter.setData(newData);
          setAdapter(mAdapter);
          if(isLayoutAnimationAutoPlay){
            scheduleLayoutAnimation();
          }
          hasSet = true;
        }
//        checkAutoFocus("setData");
        if (LogUtils.isDebug()) {
          Log.i(TAG, "DebugPool setPendingData setAdapter done");
        }
      }
    } else {
      throw new IllegalArgumentException("setPendingData data必须是HippyArray data:"+data);
    }
  }

  private void handleDataDiff(HippyArray oldArray, HippyArray newArray) {
//    if (mAdapter.mRootListView != null &&
//      mAdapter.mRootListView != mAdapter.mBoundListView) {
//      mAdapter.setData(newArray);
//      mAdapter.notifyDataSetChanged();
//      if (LogUtils.isDebug()) {
//        Log.i(TAG, "handleDataDiff 嵌套子fastList刷新数据");
//      }
//      return;
//    }
    if (LogUtils.isDebug()) {
      Log.i(TAG, "handleDataDiff 刷新数据 oldArray.size--" + oldArray.size() + "  newArray.size--" + newArray.size()
        + "  hashCode--" + FastListView.this.hashCode());
    }
    if (getItemAnimator() != null) {
      ((SimpleItemAnimator) Objects.requireNonNull(getItemAnimator())).setSupportsChangeAnimations(false);
    }
    FastListDataBindingHelper.handleDataDiffCallBack3(oldArray, newArray, keyName,
      new FastListDataBindingHelper.OnDataDiffListener3() {

        @Override
        public void onRangeInsert(int position, int count) {
          mAdapter.notifyItemRangeInserted(position, count);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onRangeInsert--" + "  position--" + position + "  count--" + count
              + "  hashCode--" + FastListView.this.hashCode());
          }
        }

        @Override
        public void onRangeDelete(int position, int count) {
          mAdapter.notifyItemRangeRemoved(position, count);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onRangeDelete--" + "  position--" + position + "  count--" + count
              + "  hashCode--" + FastListView.this.hashCode());
          }
        }

        @Override
        public void onUpdateViewData() {
          mAdapter.setData(newArray);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onUpdateViewData"
              + "  hashCode--" + FastListView.this.hashCode());
          }
        }

        @Override
        public void onMove(int position, int nextPosition) {
          mAdapter.notifyItemMoved(position, nextPosition);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onMove--" + "  position--" + position + "  nextPosition--" + nextPosition
              + "  hashCode--" + FastListView.this.hashCode());
          }
        }

        @Override
        public void onRangeUpdate(int start, int count) {
//          ((SimpleItemAnimator) getItemAnimator()).setSupportsChangeAnimations(false);
          mAdapter.notifyItemRangeChanged(start, count);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onRangeUpdate--" + "  start--" + start + "  count--" + count
              + "  hashCode--" + FastListView.this.hashCode());
          }
        }

        @Override
        public void onAllChange() {
          mAdapter.notifyDataSetChanged();
          if (LogUtils.isDebug()) {
            Log.i(TAG, "handleDataDiff 刷新数据 onAllChange"
              + "  hashCode--" + FastListView.this.hashCode());
          }
        }
      },
      item -> (HippyMap) item.raw, FastAdapter.ItemEntity.class);
  }

  @Override
  protected void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter) {
    super.onAdapterChanged(oldAdapter, newAdapter);
//    Log.i(TAG,"-------onAdapterChanged newAdapter:"+newAdapter);
    if(LogUtils.isDebug()){
      Log.i(AutoFocusManager.TAG,"resetAutoWork on onAdapterChanged");
    }
    if (mAdapter != null) {
      mAdapter.clearDetachCaches();
    }
    resetAutoWork();
  }

  private void resetAutoWork(){
//    autofocusDirty = true;
    if(getControlProps().singleSelectPosition != -1){
//      Log.i(TAG, "resetAutoWork on singleSelectPosition:" + getControlProps().singleSelectPosition);
      defaultSectionPosition = getControlProps().singleSelectPosition;
    }
    getControlProps().currentScrollToPosition = -1;
  }

  @Override
  public void setPendingData(Object data, RenderNode templateNode) {
    this.setPendingData(data, templateNode, false, useDiff);
  }


  public void smoothScrollToTop() {
    smoothScrollToPosition(0);
  }

  public void scrollToIndex(int xIndex, int yIndex, boolean animated, int duration, int offset) {
    if (getLayoutManagerCompat() != null) {
      handleScrollBackToTop(yIndex);
      getLayoutManagerCompat().scrollToPositionWithOffset(yIndex, offset);
    }
  }

  private void handleScrollBackToTop(int position) {
    if (position < 1) {
      if (LogUtils.isDebug()) {
        Log.e("ScrollLog", "handleScrollBackToTop mOffsetY:" + mOffsetY);
      }
      if (mOffsetY >= scrollYLesserReferenceValue) {
        handleScrollValue(true, -1, 0);
      }
      resetScrollOffset();
    }
  }

  public void recycle() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "DebugPool recycle holder type:" + ((FastAdapter.Holder) h).type);
          }
          mAdapter.recycleItem((FastAdapter.Holder) h);
        }
      }
      //mAdapter.recycleAll();
    }
  }

  public void setListenBoundEvent(boolean enable) {
    if (mAdapter != null) {
      mAdapter.setListenBoundEvent(enable);
    }
  }


  public void prepareForRecycle() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "DebugPool FastList prepareForRecycle holder type:" + ((FastAdapter.Holder) h).type + ",view :" + h.itemView);
          }
          if (h.itemView instanceof HippyRecycler) {
            ((HippyRecycler) h.itemView).onResetBeforeCache();
          }
        }
      }
      //mAdapter.recycleAll();
    }
  }

  public void notifyChildrenSaveState() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "+++DebugPool FastList notifyChildrenSaveState holder type:" + ((FastAdapter.Holder) h).type + ",view :" + h.itemView);
          }
          if (h.itemView instanceof HippyRecycler) {
            ((HippyRecycler) h.itemView).notifySaveState();
          }
        }
      }
      //mAdapter.recycleAll();
    }
  }


  @Override
  public void resumePostTask() {
    super.resumePostTask();
    notifyResumeTask();
  }

  @Override
  public void pausePostTask() {
    super.pausePostTask();
    notifyPauseTask();
  }

  @Override
  public void notifyPauseTask() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (h.itemView instanceof FastAdapter.ScrollTaskHandler) {
            ((FastAdapter.ScrollTaskHandler) h.itemView).notifyPauseTask();
          }
        }
      }
    }
  }

  @Override
  public void notifyBringToFront(boolean front) {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (h.itemView instanceof FastAdapter.ScrollTaskHandler) {
            ((FastAdapter.ScrollTaskHandler) h.itemView).notifyBringToFront(front);
          }
        }
      }
    }
  }

  @Override
  public void notifyResumeTask() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (h.itemView instanceof FastAdapter.ScrollTaskHandler) {
            ((FastAdapter.ScrollTaskHandler) h.itemView).notifyResumeTask();
          }
        }
      }
    }
  }

  public void notifyChildrenRestoreState() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          if (LogUtils.isDebug()) {
            Log.i(TAG, "---DebugPool FastList notifyChildrenRestoreState holder type:" + ((FastAdapter.Holder) h).type + ",view :" + h.itemView);
          }
          if (h.itemView instanceof HippyRecycler) {
            ((HippyRecycler) h.itemView).notifyRestoreState();
          }
        }
      }
      //mAdapter.recycleAll();
    }
  }

  @Override
  public void clearPostTask(int type) {

  }


  @Override
  public void notifyDetachFromParent() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          mAdapter.onViewDetachedFromWindow((FastAdapter.Holder) h);
        }
      }
    }
  }

  @Override
  public void notifyAttachToParent() {
    if (mAdapter != null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        final ViewHolder h = getChildViewHolder(child);
        if (h instanceof FastAdapter.Holder) {
          mAdapter.onViewAttachedToWindow((FastAdapter.Holder) h);
        }
      }
    }
  }


  @Override
  protected void onReAttached() {
    super.onReAttached();
    if (LogUtils.isDebug()) {
      Log.d(TAG, "FastListAttachEvent ++ onReAttached defaultSectionPosition: " + defaultSectionPosition + " this :" + this);
    }
    if (defaultSectionPosition != -1) {
      //zhaopeng 20220731 在重新attach后，如果不在这里更新状态，select状态会不显示
      changeSelectState(defaultSectionPosition, true);
    }
  }


  @Override
  protected boolean onInterceptRequestChildRectangleOnScreen(@NonNull View child, @NonNull Rect rect, boolean immediate, int direction, boolean focusedChildVisible) {
    return mAdapter != null && mAdapter.onInterceptRequestChildRectangleOnScreen(this, child, rect, immediate, direction, focusedChildVisible);
  }

  public void clearData() {
    if (LogUtils.isDebug()) {
      Log.i(TAG, "clearData called");
    }
//    autofocusDirtyOnDataChange = true;
    if (mAdapter != null) {
      mAdapter.clearData();
      mAdapter.notifyDataSetChanged();
    } else {
      setAdapter(null);
    }
    requestLayoutManual();
  }

  private boolean resetOnDetach = true;
  public void setResetOnDetach(boolean enable) {
    this.resetOnDetach = enable;
  }

  public void setEventSendItem(boolean flag) {
    if (mAdapter != null) {
      mAdapter.setEventSendItem(flag);
    }
  }

  public void setPlaceholderPostDelay(int time) {
    if (mAdapter != null) {
      mAdapter.placeholderPostDelay = time;
    }
  }


  public interface FastListItemView {
    void onRecycleItemView(FastListView listView, FastAdapter adapter);
  }

  static final class InitParams {
    int customFocusPosition;
    int targetFocusPosition;
    int targetScrollToPosition;
    int scrollToPosition;
    int scrollOffset;
    boolean alignCenter;
    String targetName;
    boolean valid = true;
    final boolean blockOthers;
    int delay;

    private InitParams(int targetFocusPosition, int scrollToPosition, int scrollOffset, String targetName, boolean blockOthers, int delay, boolean alignCenter) {
      this.targetFocusPosition = targetFocusPosition;
      this.customFocusPosition = targetFocusPosition;
      this.targetScrollToPosition = scrollToPosition;
      this.scrollToPosition = scrollToPosition;
      this.scrollOffset = scrollOffset;
      this.targetName = targetName;
      this.blockOthers = blockOthers;
      this.delay = delay;
      this.alignCenter = alignCenter;
    }

    @Override
    public String toString() {
      return "InitParams{" +
        "customFocusPosition=" + customFocusPosition +
        ", targetFocusPosition=" + targetFocusPosition +
        ", scrollToPosition=" + scrollToPosition +
        ", scrollOffset=" + scrollOffset +
        ", targetName='" + targetName + '\'' +
        ", valid=" + valid +
        ", blockOthers=" + blockOthers +
        ", delay=" + delay +
        ", alignCenter=" + alignCenter +
        '}';
    }
  }

  public void dispatchItemFunction(HippyArray var, @Nullable Promise promise) {
    final int itemPos = var.getInt(0);
    final String targetName = var.getString(1);
    final String functionTargetName = var.getString(2);
    final HippyArray array = var.getArray(3);

    final ViewHolder holder = findViewHolderForAdapterPosition(itemPos);
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

  @Override
  public void onScrolled(int x, int y) {
    super.onScrolled(x, y);
    if(y != 0){
      TriggerTaskManagerModule.dispatchTriggerTask(this,"onScrollY");
    }
    if(x != 0){
      TriggerTaskManagerModule.dispatchTriggerTask(this,"onScrollX");
    }
  }

  @Override
  public void onScrollStateChanged(int state) {
    int dx = mOffsetX - lastScrollX;
    int dy = mOffsetY - lastScrollY;
    lastScrollX = mOffsetX;
    lastScrollY = mOffsetY;
    if (onScrollStateChangedListener != null) {
      onScrollStateChangedListener.onScrollStateChanged(lastState, state, dx, dy);
    }
    if (state == SCROLL_STATE_IDLE) {
      if (lastState != state) {
        TriggerTaskManagerModule.dispatchTriggerTask(this,"onScrollStateIdle");
      }
    }else if(lastState == SCROLL_STATE_IDLE){
        TriggerTaskManagerModule.dispatchTriggerTask(this,"onScrollStateScrolling");
    }
    super.onScrollStateChanged(state);
  }

  @Override
  protected void onTriggerScrollYGreater() {
    super.onTriggerScrollYGreater();
    if (onScrollStateChangedListener != null) {
      onScrollStateChangedListener.onTriggerScrollYGreater();
    }
  }

  @Override
  protected void onTriggerScrollYLesser() {
    super.onTriggerScrollYLesser();
    if (onScrollStateChangedListener != null) {
      onScrollStateChangedListener.onTriggerScrollYLesser();
    }
  }

  public FastAdapter getFastAdapter() {
    return mAdapter;
  }

  @Override
  public void setRootList(FastListView rootList, FastAdapter adapter) {
    if (mAdapter != null) {
      mAdapter.setRootList(rootList, adapter);
    }

  }

  public View getAgentView() {
    return agentView;
  }

  public void setAgentView(View agentView) {
    this.agentView = agentView;
  }

  public OnFastScrollStateChangedListener getOnScrollStateChangedListener() {
    return onScrollStateChangedListener;
  }

  public void setOnScrollStateChangedListener(OnFastScrollStateChangedListener onScrollStateChangedListener) {
    this.onScrollStateChangedListener = onScrollStateChangedListener;
  }

  @Override
  public int findPositionByChild(View child) {
    return getLayoutManagerCompat().getPosition(child);
  }

  public boolean isShakeEnd() {
    return mShakeEndAnimator == null;
  }

  private boolean destroyed = false;
  @Override
  public void destroy() {
    super.destroy();
    this.destroyed = true;
    try {
      if (mAdapter != null) {
        mAdapter.clearDetachCaches();
        mAdapter.clearCache();
        mAdapter.clearData();
        setAdapter(null);
      }
      templateNode = null;
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void getState(@NonNull HippyMap map) {
    Utils.getFastListState(this, map);
  }

  @Override
  public void onRequestAutofocus(View child, View target,int type) {
//    if(target != null && autofocusDirty){
    if(target != null){//暂时不判断
//      autofocusDirty = false;
      if(!hasFocus() || isFocused() || type == AUTOFOCUS_TYPE_SIZE_VALID
      || type == AUTOFOCUS_TYPE_FORCE) {
          super.onRequestAutofocus(child, target,type);
      }else{
        if(AutoFocusManager.isNextGlobalPendingAutofocus(target)){
          super.onRequestAutofocus(child, target,type);
        }else {
          Log.e(AutoFocusManager.TAG, "onRequestAutofocus return on List hasFocus :" + this);
        }
      }
    }
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
    FastAdapterUtil.updateItemData4FastAdapterTraverse(id,data,getFastAdapter().dataList,traverse,ExtendUtil.debugViewLite(this));
  }

  @Override
  public void searchUpdateItemViewByItemID(String id, Object data, boolean traverse) {
    FastAdapterUtil.updateItemViewByItemID(this.getView(),id,data,traverse);
  }

  @Override
  public HippyArray getItemListData() {
    return getFastAdapter().dataList;
  }

  @Override
  public int findItemPositionBySID(String id) {
    return getFastAdapter() == null ? -1 :  getFastAdapter().findItemPositionBySID(id);
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
      final ViewHolder vh = findViewHolderForAdapterPosition(position);
      mAdapter.updateItemSpecificPropByTargetName(name, position, key, value, updateView,vh);
      if (vh == null) {
        Log.e(TAG, "updateItemSpecificProp error,没有找到对应的Holder，当前View可能没有在展示");
      }
    }
  }

  public void changeItemState(int position,String state,boolean on){
    if("selected".equals(state)){
      if(on){
        setSelectChildPosition(position,true);
      }else{
        setSelectChildPosition(-1,false);
      }
    }else if (mAdapter != null) {

      final ViewHolder vh = findViewHolderForAdapterPosition(position);
      if(vh != null){
        if(vh.itemView instanceof StateView){
          ((StateView) vh.itemView).setCustomState(state,on);
        }
      }else {
        Log.e(TAG, "changeItemState error,没有找到对应的Holder，当前View可能没有在展示");
      }
    }
  }

  private String[] stateEnableOnFocusNames;
  public void setStateEnableOnFocusNames(String[] names) {
    Log.e(TAG,"setStateEnableOnFocusNames new names = " + Arrays.toString(names)+",old stateEnableOnFocusNames:"+Arrays.toString(stateEnableOnFocusNames));
    if (stateEnableOnFocusNames != null  && stateEnableOnFocusNames.length  > 0 && names == null || names.length == 0) {
      //取消
      View child = getFocusedChild();
      for(String s : stateEnableOnFocusNames){
        Log.i(TAG,"setStateEnableOnFocusNames on clearSelectChild s:"+s);
        if("selected".equals(s)){
          // setSelectChildPosition(-1,false);
          clearSelectChild();
        }
        if(child instanceof StateView){
          ((StateView) child).setCustomState(s,false);
        }
      }
    }
    this.stateEnableOnFocusNames = names;
    if (stateEnableOnFocusNames != null) {
      View child = getFocusedChild();
      for(String s : stateEnableOnFocusNames){
        if(child instanceof StateView){
          if("selected".equals(s)){
            int pos = findPositionByChild(child);
            Log.i(TAG,"setSelectChildPosition on setStateEnableOnFocusNames pos:"+pos);
            setSelectChildPosition(pos,false);
          }
          ((StateView) child).setCustomState(s,true);
        }
      }
    }
  }

  @Override
  public View findFirstFocusByDirection(int direction) {
    return getLayoutManagerCompat().findFirstFocusByDirection(direction);
  }

  @Override
  protected int getFirstFocusChildPosition(int direction) {
    if (mTargetFocusChildPosition > -1) {
      return mTargetFocusChildPosition;
    }else {
      if (isInfinityLoop) {
        return FastAdapter.INFINITE_START_POSITION;
      }else{
        return 0;
      }
    }
  }


}
