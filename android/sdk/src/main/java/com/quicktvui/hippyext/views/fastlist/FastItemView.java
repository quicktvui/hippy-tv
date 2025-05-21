package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.quicktvui.base.ui.IRecyclerItemView;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

public class FastItemView extends HippyViewGroup implements IRecyclerItemView, FastAdapter.ScrollTaskHandler, FastAdapter.ItemContainer {
    private FastListView parentListView;
    private FastFlexView parentFlexView;
    protected int bindPosition = -1;
    protected Object bindItem = null;
    protected int JSEventViewID = -1;
    boolean enablePlaceholder = false;
    boolean disablePlaceholderFocus = false;
    public FastItemView(Context context) {
        super(context);
        setClipChildren(false);
        setFocusable(false);
    }

    public void setParentListView(FastListView parentListView) {
      this.parentListView = parentListView;
    }

    public void setParentFlexView(FastFlexView parentFlexView) {
      this.parentFlexView = parentFlexView;
    }

  public void setJSEventViewID(int JSEventViewID) {
    this.JSEventViewID = JSEventViewID;
  }

  public void updateInstance(int index, Object object) {

    }

  @Override
  public HippyMap getScrollOverride() {
    HippyMap map =  super.getScrollOverride();
    if (map != null) {
      return map;
    }
    if (getContentView() instanceof HippyViewGroup) {
      return ((HippyViewGroup) getContentView()).getScrollOverride();
    }
    return null;
  }

  public FastListView getParentListView() {
    return parentListView;
  }

  public FastFlexView getParentFlexView() {
    return parentFlexView;
  }

  public void updateItemDataInList(int position, Object object){
      assert getId() != -1;
      FastPendingView parent = parentListView;
      if(LogUtils.isDebug()){
        Log.i("FastItemViewLog","updateIteDataInList id："+getId()+",parent:"+parent);
      }
      if(parent != null){
        parent.updateItem(position,object);
      }else{
        throw new IllegalStateException("updateItemProps error, parent cant be null here,position:"+position);
      }
    }

    private FastPendingView findParent(){
      if(getParent() instanceof FastPendingView){
        return (FastPendingView) getParent();
      }
      if(LogUtils.isDebug()){
        Log.d("FastItemViewLog","findParent step 1 null id："+getId()+",parent:"+getParent());
      }
      final RenderNode node = Utils.getRenderNode(this);
      if(node != null && node.getParent() != null){
        final View view =  Utils.findBoundView(getHippyContext(),node.getParent());
        if(view instanceof FastPendingView){
          return (FastPendingView) view;
        }
      }
      if(LogUtils.isDebug()){
        Log.e("FastItemViewLog","findParent final null id："+getId()+",parent:"+getParent());
      }
      return null;
    }


    public void batchUpdate(int position) {

    }

    public static FastItemView findTemplateItemView(View view){
      if(view instanceof FastItemView){
        return (FastItemView) view;
      }
      if(view .getParent() instanceof View) {
        return findTemplateItemView((View) view.getParent());
      }
      return null;
    }

    public void updateItemProps(String name,int anInt, HippyMap map, boolean aBoolean) {
      FastPendingView parent = parentListView;
      if(LogUtils.isDebug()){
        Log.i("FastItemViewLog","updateItemProps id："+getId()+",parent:"+parent+",name:"+name+",pos:"+anInt);
      }
      if(parent != null){
        parent.updateItemProps(name,anInt,map,aBoolean);
      }else{
        throw new IllegalStateException("updateItemProps error, parent cant be null here,name:"+name+",position:"+anInt);
      }
    }

  public void dispatchItemFunction(HippyArray var, Promise promise) {
    FastPendingView parent = parentListView;
    if(LogUtils.isDebug()){
      Log.i("FastItemViewLog","dispatchItemFunction id："+getId()+",parent:"+parent);
    }
    if(parent != null){
      parent.dispatchItemFunction(var,promise);
    }else{
      throw new IllegalStateException("dispatchItemFunction error, parent cant be null,var："+var);
    }
  }

  HippyViewEvent itemEvent;



  protected HippyViewEvent getItemEvent(){
    if(itemEvent == null){
      itemEvent = new HippyViewEvent("onItemViewEvent");
    }
    return itemEvent;
  }

  protected HippyMap generateItemEventMap(String eventName,int position,Object item){
    HippyMap hm = new HippyMap();
    hm.pushString("eventName",eventName);
    hm.pushInt("position",position);
    hm.pushObject("itemData",item);
    return hm;
  }

  @Override
  public void onBind(View parent, int position, Object item) {
    this.bindPosition = position;
    this.bindItem = item;
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("bind", position, item));
    }
  }

  @Override
  public void onCreate(View parent) {
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(),generateItemEventMap("create", -1, null));
    }
  }

  @Override
  public void onUnBind(View parent, int position, Object item) {
    this.bindPosition = -1;
    this.bindItem = null;
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("unBind", position, item));
    }
  }


  @Override
  public void onAttachToWindow(View parent, int position, Object item) {
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("attach", position, item));
    }
  }

  @Override
  public void onDetachFromWindow(View parent, int position, Object item) {
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("detach", position, item));
    }
  }

  @Override
  public void clearPostTask(int type) {

  }

  @Override
  public void notifyDetachFromParent() {

  }

  @Override
  public void notifyAttachToParent() {

  }

  @Override
  public void notifyBringToFront(boolean front) {
    if(JSEventViewID > -1) {
      if (front) {
        getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("toFront", bindPosition, bindItem));
      } else {
        getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("toBack", bindPosition, bindItem));
      }
    }
  }


  @Override
  public void notifyPauseTask() {
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("pause", bindPosition, bindItem));
    }
  }

  @Override
  public void notifyResumeTask() {
    if(JSEventViewID > -1) {
      getItemEvent().send(JSEventViewID, getHippyContext(), generateItemEventMap("resume", bindPosition, bindItem));
    }
  }


  @Override
  public View getItemView() {
    return this;
  }


  @Override
  public View getContentView() {
    if(getChildCount() > 0){
      return getChildAt(0);
    }
    return null;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    if (enablePlaceholder && isFocusable() && getContentView() != null && !disablePlaceholderFocus) {
      getContentView().refreshDrawableState();
    }
  }

  @Override
  public void toggle(boolean toPlaceholder) {
    if(!enablePlaceholder){
      return;
    }
    final View content = getContentView();
    if(toPlaceholder){
      if (content != null) {
        content.setAlpha(0);
      }
      setBackgroundColor(placeholderBackgroundColor);
    }else {
      if (content != null) {
        if(!disablePlaceholderFocus) {
          content.setDuplicateParentStateEnabled(true);
        }
        content.setAlpha(1);
      }
      setBackgroundColor(Color.TRANSPARENT);
    }
  }


  public void setEnablePlaceholder(boolean enablePlaceholder) {
    this.enablePlaceholder = enablePlaceholder;
  }

  int placeholderBackgroundColor;
  float placeholderBorderRadius = 0;

  public void configPlaceholder(int placeholderBackgroundColor, float placeholderBorderRadius) {
      if(enablePlaceholder) {
        this.placeholderBackgroundColor = placeholderBackgroundColor;
        this.placeholderBorderRadius = placeholderBorderRadius;
        if (getContentView() != null) {
          setBackgroundColor(placeholderBackgroundColor);
        }
        setBorderRadius(placeholderBorderRadius, BorderRadiusDirection.ALL.ordinal());
      }
  }

  public void setDisablePlaceholderFocus(boolean disablePlaceholderFocus) {
      this.disablePlaceholderFocus = disablePlaceholderFocus;
  }
}
