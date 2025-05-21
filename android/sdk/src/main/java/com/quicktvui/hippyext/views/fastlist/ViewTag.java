package com.quicktvui.hippyext.views.fastlist;

import android.util.Log;
import android.view.View;

import com.quicktvui.hippyext.views.TVTextView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.flex.FlexAlign;
import com.tencent.mtt.hippy.dom.flex.FlexCSSDirection;
import com.tencent.mtt.hippy.dom.flex.FlexJustify;
import com.tencent.mtt.hippy.dom.node.DomNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.list.HippyRecycler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ViewTag {
  private HashMap<Integer,List<Patch>> pendingPatches;
  FastAdapter.ElementNode domNode;
  final RenderNode template;
  Worker worker;

  boolean enableLoadDelay = false;
//  boolean viewCreated = false;
  @Deprecated
  FastAdapter.ElementNode placeHolderNode;
  ArrayList<FastAdapter.ElementNode> mDelayLoadNodes;
//  FastAdapter.ElementNode contentNode;

  ViewTag(RenderNode template) {
    this.template = template;
  }

  void addDelayLoad(FastAdapter.ElementNode node){
    if(mDelayLoadNodes  == null){
      mDelayLoadNodes = new ArrayList<>();
    }
    if(!mDelayLoadNodes.contains(node)) {
      mDelayLoadNodes.add(node);
    }
  }

  List<FastAdapter.ElementNode> getDelayLoadNodes(){
    return mDelayLoadNodes;
  }


  public FastAdapter.ElementNode getRootNode(){
    if(domNode == null){
      domNode = new FastAdapter.ElementNode();
      domNode.setFlexDirection(FlexCSSDirection.COLUMN);
      domNode.setAlignItems(FlexAlign.FLEX_START);
      domNode.setJustifyContent(FlexJustify.FLEX_START);
      domNode.rootNode = domNode;
      domNode.isRootItem = true;
//      domNode.setWrap(FlexWrap.NOWRAP);
    }
    return domNode;
  }

  void cancelWork(){
    //Log.d("WorkLOG", "WorkLOG cancel this:" + Utils.hashCode(worker));
    if(worker != null){
      worker.cancel();
    }
    worker = null;
  }


  void work(Worker worker){
    this.worker = worker;
    //Log.d("WorkLOG", "WorkLOG work run :" + Utils.hashCode(worker));
    worker.work();
    //worker.batch();
  }

  public void preparePatch(Patch p){
    if(pendingPatches == null){
      pendingPatches = new HashMap<>();
    }
    final int type = p.type;
    if(!pendingPatches.containsKey(type)){
      pendingPatches.put(type,new ArrayList<>());
    }
    pendingPatches.get(type).add(p);
  }

  public List<Patch> getPatches(int type) {
    return pendingPatches.get(type);
  }

  public int getPatchesSize(int type){
    return pendingPatches.containsKey(type) ? pendingPatches.get(type).size() : 0;
  }

    public void clear() {
        if(pendingPatches != null){
          pendingPatches.clear();
        }
    }

  public void reset() {
    if(domNode != null){
      for(int i = 0; i < domNode.getChildCount(); i ++){
        final FastAdapter.ElementNode el = (FastAdapter.ElementNode) domNode.getChildAt(i);
        if(el.boundView != null) {
          if(LogUtils.isDebug()) {
            Log.i("ViewTAG","ElementCallback clear callback"+"view:"+el.boundView);
          }
          el.boundView.setOnClickListener(null);
          el.boundView.setOnFocusChangeListener(null);
//          HippyEngineContext context = ((HippyInstanceContext)el.boundView.getContext()).getEngineContext();
//          DomNode tDom = context.getDomManager().getNode(template.getId());
//          Utils.resetNode(el,tDom);
        }
        resetElementNode(domNode);
        //重置嵌套的子列表
        resetAllView(domNode);
      }
    }
  }

  void resetAllView(DomNode node){
    if(node instanceof FastAdapter.ElementNode) {
      final FastAdapter.ElementNode en = (FastAdapter.ElementNode) node;
      if(en.boundView instanceof FastPendingView){
        ((FastPendingView) en.boundView).notifyRecycled();
      }
      //这里如果打开注释的话，会导致频繁切换tab时，点击事件无效，这里暂注掉
//      if (en.boundView != null && en.boundView.isFocusable()) {
//        en.boundView.setOnClickListener(null);
//        en.boundView.setOnFocusChangeListener(null);
//      }
      for(int i = 0; i < node.getChildCount(); i ++){
        resetAllView(node.getChildAt(i));
      }
    }
  }


  public void resetTextView(){
    if(domNode != null){
      for(int i = 0; i < domNode.getChildCount(); i ++){
        resetTextElementNode(domNode);
      }
    }
  }

  void resetTextElementNode(DomNode node){
    if(node instanceof FastAdapter.ElementNode) {
      final FastAdapter.ElementNode en = (FastAdapter.ElementNode) node;
      if(en.boundView instanceof TVTextView){
        ((HippyRecycler) en.boundView).onResetBeforeCache();
      }
      for(int i = 0; i < node.getChildCount(); i ++){
        resetTextElementNode(node.getChildAt(i));
      }
    }
  }


  void resetElementNode(DomNode node){
    if(node instanceof FastAdapter.ElementNode) {
      final FastAdapter.ElementNode en = (FastAdapter.ElementNode) node;
      en.resetState();
      if(en.boundView instanceof HippyRecycler){
        ((HippyRecycler) en.boundView).onResetBeforeCache();
      }
//      for(int i = 0; i < node.getChildCount(); i ++){
//        resetElementNode(node.getChildAt(i));
//      }
    }
  }

  static class Patch{
    final int type;
    View view;
    RenderNode boundNode;
    final static int TYPE_PENDING_CREATE  =0;
    final static int TYPE_PROP =1;
    final static int TYPE_PENDING_PRO =2;
    final static int TYPE_LAYOUT =3;
    final static int TYPE_EXTRA =4;
    final static int TYPE_PENDING_LIST =5;

    Patch(int type) {
      this.type = type;
    }
  }

  static class PendingPropPatch extends Patch{
    String valueKey;
    String prop;
    FastAdapter.ElementNode eNode;
    public PendingPropPatch(RenderNode bindNode, String nodeProp, View view, String valueKey,FastAdapter.ElementNode en) {
      super(TYPE_PENDING_PRO);
      this.view = view;
      this.prop = nodeProp;
      this.valueKey = valueKey;
      this.boundNode = bindNode;
      this.eNode = en;
    }
  }

  static class UpdatePropPatch extends Patch{
    HippyMap data;
    public UpdatePropPatch(RenderNode bindNode, View view, HippyMap data) {
      super(TYPE_PROP);
      this.view = view;
      this.data = data;
      this.boundNode = bindNode;
    }
  }

  static class UpdateExtraPatch extends Patch{
    public UpdateExtraPatch(RenderNode bindNode, View view) {
      super(TYPE_EXTRA);
      this.view = view;
      this.boundNode = bindNode;
    }
  }

  static class ListItemPatch extends Patch{
    String prop;
    public ListItemPatch(RenderNode bindNode, View view,String prop) {
      super(TYPE_PENDING_LIST);
      this.view = view;
      this.boundNode = bindNode;
      this.prop = prop;
    }
  }

  static class UpdateLayoutPatch extends Patch{
    final int[] layout;
    FastAdapter.ElementNode eNode;
    public UpdateLayoutPatch(RenderNode bindNode, View view, FastAdapter.ElementNode node) {
      super(TYPE_LAYOUT);
      this.view = view;
      this.boundNode = bindNode;
      this.layout = new int[]{bindNode.getX(),bindNode.getY(),bindNode.getWidth(),bindNode.getHeight()};
      this.eNode = node;
    }

    public UpdateLayoutPatch(int x,int y,int width,int height, View view,RenderNode bindNode,FastAdapter.ElementNode node) {
      super(TYPE_LAYOUT);
      this.view = view;
      this.boundNode = bindNode;
      this.layout = new int[]{x,y,width,height};
      this.eNode = node;
    }

  }

  static abstract class Worker {
    private boolean canceled = false;
    ArrayList<Task> tasks;

    public Worker() {
    }

    void cancel(){
      this.canceled = true;
      if(tasks != null) {
          for (Task task : tasks) {
            task.canceled = true;
          }
        if(isCanceled()){
          Log.e("WorkLOG", "cancel workSize:" + tasks.size());
        }
          tasks.clear();
      }
    }

    void addTask(Task task){
      if(tasks == null){
        tasks = new ArrayList<>();
      }
      tasks.add(task);
      //rootView.postDelayed(task,16);
    }

    abstract void work();
    void batch(){
      if(tasks != null) {
        for (Task task : tasks) {
          if(isCanceled()){
              Log.e("WorkLOG", "WorkLOG cancel work !!");
            break;
          }
          if (!task.canceled) {
            task.run();
          }else{
              Log.e("WorkLOG", "WorkLOG cancel work ");
          }
        }
        tasks.clear();
      }
    }

    boolean isCanceled(){
      return canceled;
    }

  }

  static abstract class Task implements Runnable{
    boolean canceled = false;
    int batch = 0;
  }

}


