package com.tencent.mtt.hippy.uimanager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.NonNull;

import com.quicktvui.base.ui.FocusDispatchView;
import com.quicktvui.base.ui.FocusUtils;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewBase;
import com.tencent.mtt.supportui.views.viewpager.ViewPager;

import java.util.Arrays;

/**
 * create by zhaopeng 2020
 */
public class InternalExtendViewUtil {


  public static final String CHILD_FOCUS_EVENT_NAME = "onChildFocus";
  public static final String CHILD_SELECT_EVENT_NAME = "onChildSelect";
  public static final String LIST_FOCUS_SEARCH_FAIL_EVENT_NAME = "onFocusSearchFailed";

  public static void sendEventOnRequestChildFocus(ViewGroup parent,View child, View focused,@NonNull HippyViewEvent event){

    HippyMap map = new HippyMap();



    HippyMap cm = new HippyMap();
    cm.pushInt("index",parent.indexOfChild(child));
    cm.pushInt("id",child.getId());
    cm.pushString("sid", ExtendUtil.getViewSID(child));
    final String cname = ControllerManager.findName(child);
    if(cname != null){
      cm.pushString("name",cname);
    }
    if(parent instanceof RecyclerViewBase){
      final int position = ((RecyclerViewBase) parent).getChildPosition(child);
      cm.pushInt("position",position);
    }

    map.pushMap("child",cm);
    if(focused != null) {
      HippyMap fm = new HippyMap();
      fm.pushInt("id", focused.getId());
      fm.pushString("sid", ExtendUtil.getViewSID(focused));
      final String fName = ControllerManager.findName(focused);
      if (fName != null) {
        fm.pushString("name", fName);
      }
      map.pushMap("focused",fm);
    }

    event.send(parent,map);


  }

  public static void postTaskByRootView(View v,Runnable task ,int delay){
    final View root = getRootViewFromContext(v);
    if(root != null){
      root.postDelayed(task,delay);
    }
  }

  public static void sendEventOnRequestListChildSelect(ViewGroup parent,View child){

    HippyMap map = new HippyMap();

    HippyMap cm = new HippyMap();
    cm.pushInt("index",parent.indexOfChild(child));
    cm.pushInt("id",child.getId());
    cm.pushString("sid", ExtendUtil.getViewSID(child));
    final String cname = ControllerManager.findName(child);
    if(cname != null){
      cm.pushString("name",cname);
    }
    if(parent instanceof RecyclerViewBase){
      final int position = ((RecyclerViewBase) parent).getChildPosition(child);
      cm.pushInt("position",position);
    }

    map.pushMap("child",cm);

    new HippyViewEvent(CHILD_SELECT_EVENT_NAME).send(parent,map);

  }

  public static void sendEventOnListFocusSearchFailed(ViewGroup parent,View child,View focused,int direction,HippyViewEvent event){

    HippyMap map = new HippyMap();
    if(child != null) {
      HippyMap cm = new HippyMap();
      cm.pushInt("index", parent.indexOfChild(child));
      cm.pushInt("id", child.getId());
      cm.pushString("sid", ExtendUtil.getViewSID(child));
      final String cname = ControllerManager.findName(child);
      if (cname != null) {
        cm.pushString("name", cname);
      }
      if (parent instanceof RecyclerViewBase) {
        final int position = ((RecyclerViewBase) parent).getChildPosition(child);
        cm.pushInt("position", position);
      }

      map.pushMap("child", cm);
    }
    if(focused != null) {
      HippyMap fm = new HippyMap();
      fm.pushInt("id", focused.getId());
      fm.pushString("sid", ExtendUtil.getViewSID(focused));
      final String fName = ControllerManager.findName(focused);
      if (fName != null) {
        fm.pushString("name", fName);
      }
      map.pushMap("focused",fm);
    }

    map.pushInt("direction",direction);

    event.send(parent,map);

  }

  public static void sendEventOnFocusViewPagerSearchFail(ViewPager parent, View child,View focused,int direction){

    HippyMap map = new HippyMap();

    if(child != null) {
      HippyMap cm = new HippyMap();
      cm.pushInt("index", parent.indexOfChild(child));
      cm.pushInt("id", child.getId());
      cm.pushString("sid", ExtendUtil.getViewSID(child));
      final String cname = ControllerManager.findName(child);
      if (cname != null) {
        cm.pushString("name", cname);
      }
      map.pushMap("child", cm);
    }

    if(focused != null) {
      HippyMap fm = new HippyMap();
      fm.pushInt("id", focused.getId());
      fm.pushString("sid", ExtendUtil.getViewSID(focused));
      final String fName = ControllerManager.findName(focused);
      if (fName != null) {
        fm.pushString("name", fName);
      }
      map.pushMap("focused",fm);
    }

    if(parent instanceof ViewPager){
      final int page = ((ViewPager) parent).getCurrentPage();
      map.pushInt("page",page);
    }
    map.pushInt("direction",direction);
    new HippyViewEvent(LIST_FOCUS_SEARCH_FAIL_EVENT_NAME).send(parent,map);
  }

  public static final int FOCUS_BLOCK_DIRECTION_ALL = 1;

  public static boolean isContainBlockDirection(final int direction,final int[] directions){
    if(directions != null){
      if(Arrays.binarySearch(directions,FOCUS_BLOCK_DIRECTION_ALL) > -1){
        return true;
      }
      if(Arrays.binarySearch(directions,direction) > -1){
        return true;
      }
    }
    return false;
  }

  public static boolean testFocusable(View view) {
    return FocusUtils.testFocusable(view);
  }

  public static void blockFocus(ViewGroup group) {
    if(group != null) {
      group.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }
  }

  public static void unBlockFocus(ViewGroup group) {
    if(group != null) {
      group.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }
  }
  public static void clearFocus(View v){
    if(v instanceof ViewGroup){
      if(v.findFocus() != null) {
        v.findFocus().clearFocus();
      }
    }else if(v != null){
      v.clearFocus();
    }
  }
  public static void blockRootFocus(View v) {
//    final View rootFromWindow = getRootViewFromContext(v);
//    if(rootFromWindow instanceof ViewGroup){
//      blockFocus((ViewGroup) rootFromWindow);
//      LogUtils.e(FocusDispatchView.TAG,"+++blockRootFocus on Window :"+rootFromWindow);
//      LogUtils.e("RootFocusManager","+++blockRootFocus on Window :"+rootFromWindow);
//      return;
//    }
    final View root = getRootView(v);
    if (root instanceof ViewGroup ) {
        if(LogUtils.isDebug()) {
          LogUtils.i(FocusDispatchView.TAG, "+++blockRootFocus on Root :" + root);
        }
       blockFocus((ViewGroup) root);
    }
  }

  public static void logMap(HippyMap map,String tag){
    if(map == null || map.size() < 1){
      Log.e(tag,"map is null or empty");
      return;
    }
    for(String key : map.keySet()){
      Log.d(tag,"key:"+key+",value:"+map.get(key));
    }
  }

  public static void unBlockRootFocus(View v) {
//    final View rootFromWindow = getRootViewFromContext(v);
//    if(rootFromWindow instanceof ViewGroup){
//      unBlockFocus((ViewGroup) rootFromWindow);
//      if(LogUtils.isDebug()) {
//        LogUtils.e(FocusDispatchView.TAG, "---unBlockRootFocus on Window :" + rootFromWindow);
//        LogUtils.e("RootFocusManager", "---unBlockRootFocus on Window :" + rootFromWindow);
//      }
//      return;
//    }
    final View root = getRootView(v);
    if (root instanceof ViewGroup ) {
      unBlockFocus((ViewGroup) root);
      if(LogUtils.isDebug()) {
        LogUtils.e(FocusDispatchView.TAG, "---unBlockRootFocus on Root :" + root);
        LogUtils.e("RootFocusManager", "---unBlockRootFocus on Root :" + root);
      }
    }
  }

  @Deprecated
  public static View findRootView(View view){
    View rootView;
    rootView = findRootFromContext(view);
    if(rootView == null) {
//      View parentView = templateItemView.getParentListView();
//      if (parentView == null) {
//        parentView = templateItemView.getParentFlexView();
//      }
      rootView = HippyViewGroup.findPageRootView(view);
      if (rootView == null) {
        rootView = FocusDispatchView.findRootView(view);
      }
    }
    return rootView;
  }

  public static View findRootViewConsiderOrder(View view){
    View rootView = null;
    //1.先从以rootTag标记的view优先
    rootView = HippyViewGroup.findPageRootView(view);
    if (rootView == null) {
      //2. 找hippyRootView
      rootView = FocusDispatchView.findRootView(view);
    }
    return rootView;
  }

  public static View getRootViewFromContext(View view) {
    final Context context = view.getContext();
    if(context instanceof Activity){
      return getRootViewFromContext(context);
    }
    if(context instanceof HippyInstanceContext){
      return getRootViewFromContext(((HippyInstanceContext) context).getBaseContext());
    }
    return null;
  }

  public static View getRootViewFromContext(Context context){
    if(context instanceof Activity){
      final Activity activity = (Activity) context;
      return activity.getWindow().getDecorView();
    }
    return null;
  }

  /**
   * <p>Finds the topmost view in the current view hierarchy.</p>
   *
   * @return the topmost view containing this view
   */
  public static View getRootView(View view) {

    View parent = view;

    while (parent.getParent() != null && parent.getParent() instanceof View) {
      parent = (View) parent.getParent();
    }

    return parent;
  }

  public static View interceptViewGroupIfNeed(View parent,int id,View focused,View searchResult){
      if(id > -1){
        LogUtils.e(FocusDispatchView.TAG,"div: intercept group focus by blockFocusDirections next id:"+id+",parent:"+ExtendUtil.debugFocusInfo(parent));
        return focused;
      }
      return searchResult;
  }

  public static int findInterceptViewID(HippyViewGroup group,int direction){
    int id = -1;
    switch (direction){
      case View.FOCUS_UP:
        id = group.getNextFocusUpId();
        break;
      case View.FOCUS_DOWN:
        id = group.getNextFocusDownId();
        break;
      case View.FOCUS_LEFT:
        id = group.getNextFocusLeftId();
        break;
      case View.FOCUS_RIGHT:
        id = group.getNextFocusRightId();
        break;
    }
    return id;
  }


  public static void notifyViewInReFocus(View view,boolean isIn) {

  }

  public static boolean containValue(HippyArray array,Object obj){
    if(array == null || obj == null){
      return false;
    }
    for(int i = 0;i < array.size(); i ++){
      Object a = array.get(i);
      if(obj.equals(a)){
        return true;
      }
    }
    return false;
  }

  public static boolean containValue(HippyArray source,HippyArray target){
    if(source == null || target == null){
      return false;
    }
    for(int i = 0;i < source.size(); i ++){
      final Object a = source.get(i);
      for(int j = 0; j < target.size(); j++){
        final Object b = target.get(j);
        if(a.equals(b)){
          return true;
        }
      }
    }
    return false;
  }

  public static View findRootFromContext(View v) {
    try {
      HippyInstanceContext instanceContext = (HippyInstanceContext) v.getContext();
      HippyEngineContext engineContext = instanceContext.getEngineContext();
      View rootFromEngine = engineContext.getInstance(engineContext.getDomManager().getRootNodeId());
//      Log.e("RootFromContext","findRootFromContext rootID  rootViewFromEngine:"+rootFromEngine);
      return rootFromEngine;
    }catch (Exception e){
      e.printStackTrace();
      return null;
    }
  }



}
