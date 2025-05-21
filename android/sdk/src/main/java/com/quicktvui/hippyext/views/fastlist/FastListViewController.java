package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngine;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.ControllerRegistry;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogAdapterUtils;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.list.TVRecyclerView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;


/**
 * Create by WeiPeng on 2020/11/22 16:01
 */

@HippyController(name = FastListViewController.CLASS_NAME)
public class FastListViewController extends HippyViewController<FastListView> implements PendingViewController {

  public static final int LEFT_IN = 100;
  public static final int RIGHT_IN = 101;
  public static final int TOP_IN = 102;
  public static final int BOTTOM_IN = 103;


  public static final String CLASS_NAME = "FastListView";
  public static final String TAG = "FastListController";

  public FastListViewController() {}

  @Override
  protected void addView(ViewGroup parentView, View view, int index) {
    //FastList 不将数据添加到view树里
    //super.addView(parentView, view, index);updateItem
  }


  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
//    boolean isVertical = false;
//    if(iniProps.containsKey("horizontal")){
//      isVertical = true;
//    }
    FastListView fv = new FastListView(context, iniProps);
    if (LogUtils.isDebug()) {
      LogUtils.d("ScrollLog", "++++createViewImpl FastListView fv:" + fv);
    }
    return fv;
  }

  @Override
  public void onAfterUpdateProps(FastListView v) {
    super.onAfterUpdateProps(v);
  }

  @Override
  public RenderNode createRenderNode(int id, HippyMap props, String className, HippyRootView hippyRootView, ControllerManager controllerManager, boolean lazy) {
    return new FastListNode(id, props, className, hippyRootView, controllerManager, lazy);
  }

  @Override
  protected View createViewImpl(Context context) {
    return null;
  }

  @HippyControllerProps(name = "keyName", defaultType = HippyControllerProps.STRING)
  public void setHasKey(final FastListView lv, String keyName) {
    lv.setKeyName(keyName);
  }

  @HippyControllerProps(name = "stableIdKey", defaultType = HippyControllerProps.STRING)
  public void setStableIdKey(final FastListView lv, String keyName) {
    if(LogUtils.isDebug()) {
      Log.i("DebugUpdate", "setStableIdKey keyName:" + keyName + ",this:" + ExtendUtil.debugViewLite(lv) + ",adapter:" + lv.getFastAdapter());
    }
    if(lv.getFastAdapter() != null) {
      lv.getFastAdapter().setStableIdKey(keyName);
    }
  }



  @Deprecated
  @HippyControllerProps(name = "useDiff", defaultType = HippyControllerProps.BOOLEAN)
  public void setUseDiff(final FastListView lv, boolean useDiff) {
    Log.e(TAG,"setUseDiff useDiff:"+useDiff+",this:"+ExtendUtil.debugViewLite(lv));
    lv.setUseDiff(useDiff);
  }


  @HippyControllerProps(name = "scrollOffset", defaultType = HippyControllerProps.NUMBER)
  public void setScrollOffset(final FastListView lv, int offset) {
      lv.setScrollOffset((int) PixelUtil.dp2px(offset));
  }

  @HippyControllerProps(name = "initPosition", defaultType = HippyControllerProps.MAP)
  public void setScrollTargetPosition(FastListView view, HippyMap map) {
    LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG, ":initPosition map:" + map);
    view.setInitPositionInfo(map);
  }

  @HippyControllerProps(name = PendingViewController.PROP_LIST, defaultType = HippyControllerProps.ARRAY)
  public void setListData(final FastListView lv, HippyArray list) {
//      if(list != null) {
//        Log.d(TAG, "setListData lv:" + lv + ",list size:" + list.size()+"lv:"+Utils.hashCode(lv));
//      }else{
//        Log.e(TAG, "setListData lv:" + lv + ",list empty"+"lv:"+Utils.hashCode(lv));
//      }
    //lv.setList(list);
    //20230127   使用属性默认使用diff
    if(LogUtils.isDebug()){
      Log.i("FastListAdapter","setPendingData in controller FastListView id :"+lv.getId()+",list itemCount:"+(list == null ? 0 : list.size()));
    }
    lv.setListWithParams(list, false, true);
  }

  @HippyControllerProps(name = "listenBoundEvent", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setListenBoundEvent(FastListView view, boolean enable) {
    view.setListenBoundEvent(enable);
  }

  @HippyControllerProps(name = "enableItemAnimator", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setItemAnimatorEnable(FastListView view, boolean enable) {
    view.setItemAnimatorEnable(enable);
  }

  @HippyControllerProps(name = "disableScrollOnFirstScreen", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setNoScrollOnFirstScreen(FastListView view, boolean enable) {
    if(LogUtils.isDebug()) {
      Log.i("ChildOnScreenScroller", "setNoScrollOnFirstScreen view" + view + ",enable:" + enable);
    }
    if (view.getLayoutManagerCompat() != null) {
      view.getLayoutManagerCompat().getExecutor().setNoScrollOnFirstScreen(enable);
    }
  }

  @HippyControllerProps(name = "skipFocusOnPause", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setSkipFocusOnPause(FastListView view, boolean enable) {
    view.setSkipFocusOnPause(enable);
  }

  /**
   * 1.8.x
   * @param view
   * @param enable
   */
  @HippyControllerProps(name = "suspendUpdateTaskOnPause", defaultType = HippyControllerProps.BOOLEAN)
  public void setSuspendUpdateTaskOnPause(FastListView view, boolean enable) {
    view.setSuspendUpdateTaskOnPause(enable);
  }

  @HippyControllerProps(name = "taskPaused", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void changePauseTaskState(FastListView view, boolean b) {
    view.changePauseTaskState(b);
  }

  @HippyControllerProps(name = "resetOnDetach", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setResetOnDetach(FastListView view, boolean enable) {
    view.setResetOnDetach(enable);
  }


  @HippyControllerProps(name = "display", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setDisplay(final FastListView lv, boolean display) {
    lv.setDisplay(display, false);
  }

  @HippyControllerProps(name = "setData", defaultType = HippyControllerProps.ARRAY)
  public void setData(FastListView view, HippyArray array) {
    view.setList(array);
  }

  @HippyControllerProps(name = "scrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setScrollEnable(FastListView view, boolean flag) {
    view.setScrollEnable(flag);
  }

  @HippyControllerProps(name = "touchScrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setTouchScrollEnable(FastListView view, boolean flag) {
    view.setTouchScrollEnable(flag);
  }

  @HippyControllerProps(name = "verticalScrollBarEnabled", defaultType = HippyControllerProps.BOOLEAN)
  public void setVerticalScrollBarEnabled(FastListView view, boolean flag) {
    view.setVerticalScrollBarEnabled(flag);
  }

  @HippyControllerProps(name = "horizontalScrollBarEnabled", defaultType = HippyControllerProps.BOOLEAN)
  public void setScrollThresholdHorizontal(FastListView view, boolean flag) {
    view.setHorizontalScrollBarEnabled(flag);
  }

  @HippyControllerProps(name = "firstFocusChild", defaultType = HippyControllerProps.MAP)
  public void setTouchScrollEnable(FastListView view, HippyMap map) {
   view.getFirstFocusHelper().setFirstFocusChildMap(map);
  }

  @HippyControllerProps(name = "enableFirstFocusAtStart", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = true)
  public void setFindAtStart(FastListView view, boolean enable) {
    view.getFirstFocusHelper().setFindAtStart(enable);
  }

  @Override
  public void updateLayout(int id, int x, int y, int width, int height, ControllerRegistry componentHolder) {
    super.updateLayout(id, x, y, width, height, componentHolder);
  }

  public void setCurrent(HippyViewGroup view, int pos) {
    //设置当前选中位置
  }

  @HippyControllerProps(name = "enablePlaceholder", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setEnablePlaceholder(FastListView view, boolean enable) {

    if (view.getFastAdapter() != null) {
      view.getFastAdapter().setEnablePlaceholder(enable);
    }
  }

  //1.9.128
  @HippyControllerProps(name = "refocusType", defaultType = HippyControllerProps.STRING)
  public void setRefocusType(FastListView view, String typeStr) {
    if (view.getFastAdapter() != null) {
      TVListView.RefocusType type = TVListView.RefocusType.NONE;
      switch (typeStr){
        case "None":
          break;
        case "AtStart":
          type = TVListView.RefocusType.AT_START;
          break;
        case "Legacy":
          type = TVListView.RefocusType.LEGACY;
          break;
        case "FirstVisible":
          type = TVListView.RefocusType.FIRST_VISIBLE;
          break;
        case "KeepPosition":
          type = TVListView.RefocusType.KEEP_POSITION;
          break;
        case "KeepSid":
          type = TVListView.RefocusType.KEEP_SID;
          break;
      }
      view.setEnableReFocus(type != TVListView.RefocusType.NONE);
      view.setRefocusType(type);
    }
  }

  @Override
  public void onBatchComplete(FastListView view) {
    super.onBatchComplete(view);
  }

  //onRequestLoadPageData(int page)

  @Override
  public void dispatchFunction(FastListView view, String functionName, HippyArray params, Promise promise) {
    super.dispatchFunction(view, functionName, params, promise);
    switch (functionName){
      case "dispatchItemFunctionWithPromise":
        view.dispatchItemFunction(params,promise);
        break;
      case "getScrollOffset":
        final HippyMap map = new HippyMap();
        map.pushInt("x", view.getOffsetX());
        map.pushInt("y", view.getOffsetY());
        promise.resolve(map);
        break;
    }
  }


  //[ {'FastList'}[] ]
  public static boolean dispatchFunctionByTemplate(HippyViewController controller,View view, String functionName, HippyArray var,Promise promise){
    boolean b = false;
    if(var.size() == 3){
      //:4cv
      if(":4cv".equals(var.get(0))){
        b = true;
      }
    }
    if(b){
      int position = var.getInt(1);
      try {
        HippyArray realVar = var.getArray(2);
        View targetView = FastListView.findNodeViewByTemplateView(position, view);
        if (LogUtils.isDebug()) {
          Log.i(FastListView.TAG_CLONED, "dispatchFunctionByTemplate execute ! functionName : " + functionName + ",var :" + var + ",view:" + view);
          Log.i(FastListView.TAG_CLONED, "dispatchFunctionByTemplate execute ! functionName : " + functionName + ",realVar :" + realVar + ",targetView:" + targetView);
        }
        if (targetView != null) {
          if (promise == null) {
            controller.dispatchFunction(targetView, functionName, realVar);
          } else {
            //promise的要通过templateView返回结果
            Promise takeoverPromise = new PromiseTakeover(promise, position);
            controller.dispatchFunction(targetView, functionName, realVar, takeoverPromise);
          }
        }else{
          Promise takeoverPromise = new PromiseTakeover(promise, position);
          takeoverPromise.reject("view is null");
        }
      }catch (Throwable t){
        Promise takeoverPromise = new PromiseTakeover(promise, position);
        takeoverPromise.reject(t.getMessage());
        t.printStackTrace();
      }
    }
    return b;
  }


  @Override
  public void dispatchFunction(FastListView view, String functionName, HippyArray var) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "dispatchFunction functionName:" + functionName + ",var:" + var);
    }
    super.dispatchFunction(view, functionName, var);
    switch (functionName) {
      case "scrollToIndex":
        // list滑动到某个item
        int xIndex = var.getInt(0);
        int yIndex = var.getInt(1);
        boolean animated = var.getBoolean(2);
        int duration = var.getInt(3); //1.2.7 增加滚动时间 ms,animated==true时生效
        int offset = var.getInt(4);
        view.callScrollToPosition(yIndex, offset, animated);
        break;
      case "startScroll":
        LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG, ">startScroll map:" + var.getMap(0));
        view.setInitPositionInfo(var.getMap(0));
        break;
      case "requestChildFocus":
        LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG, ">requestChildFocus var:" + var);
        if (var.size() == 1) {
          view.requestChildFocus(var.getInt(0), View.FOCUS_DOWN);
        } else if (var.size() == 2) {
          view.requestChildFocus(var.getInt(0), var.getInt(1));
        }
        break;
      case "setSelectChildPosition":
        // 设置某一页的数据
        if (var.size() == 2) {
          view.setSelectChildPosition(var.getInt(0), var.getBoolean(1));
        } else {
          view.setSelectChildPosition(var.getInt(0), true);
        }
        break;
      case "scrollToPositionWithOffset":
        // 设置某一页的数据
        view.callScrollToPosition(var.getInt(0), (int) PixelUtil.dp2px(var.getInt(1)), var.getBoolean(2));
        break;
      case "scrollToPositionWithOffsetInfiniteMode":
        // 设置某一页的数据
        view.callScrollToInfinitePosition(var.getInt(0), (int) PixelUtil.dp2px(var.getInt(1)), var.getBoolean(2));
        break;
      case "scrollToPosition":
        // 设置某一页的数据
        view.callScrollToPosition(var.getInt(0),0,false);
        break;
      case "setGroupChildSelectByItemPosition":
        view.setGroupChildSelectByItemPosition(var.getInt(0));
        break;
      case "refreshListData":
        LogUtils.d("hippy", "refreshListData called by  :" + view.getId());
        view.updateList();
        break;
      case "updateItem":
        if(view.isSuspendUpdateTask()){
            view.postTask(PostTaskHolder.POST_TASK_CATEGORY_UPDATE_ITEM, var.getInt(0), new Runnable() {
              @Override
              public void run() {
                if(var.size() == 2) {
                  view.updateItem(var.getInt(0), var.getObject(1));
                }else if(var.size() == 3){
                  view.updateItem(var.getInt(0), var.getObject(1),var.getBoolean(2));
                }
              }
            },16);
        }else{
          if(var.size() == 2) {
            view.updateItem(var.getInt(0), var.getObject(1));
          }else if(var.size() == 3){
            view.updateItem(var.getInt(0), var.getObject(1),var.getBoolean(2));
          }
        }
        break;
      case "requestItemLayout":
        view.requestItemLayout(var.getInt(0));
        break;
      case "updateItemRange":
        if(view.isSuspendUpdateTask()){
          view.postTask(PostTaskHolder.POST_TASK_CATEGORY_UPDATE_ITEM, var.getInt(0), new Runnable() {
            @Override
            public void run() {
              view.updateItemRange(var.getInt(0), var.getInt(1), var.getArray(2), true);
            }
          },16);
        }else {
          view.updateItemRange(var.getInt(0), var.getInt(1), var.getArray(2), true);
        }
        break;
      case "insertItemRange":
        //从某个位置插入数据
        view.insertItemRange(var.getInt(0), var.getArray(1));
        break;
      case "updateItemMatched":
        if(view.isSuspendUpdateTask()){
          view.postTask(PostTaskHolder.POST_TASK_CATEGORY_UPDATE_ITEM, var.getInt(0), new Runnable() {
            @Override
            public void run() {
              if (var.size() == 2) {
                view.updateItemMatched("id", var.get(0), var.getObject(1));
              } else if (var.size() == 3) {
                //支持自定义id的key
                view.updateItemMatched(var.getString(0), var.get(1), var.getObject(2));
              }
            }
          },16);
        }else {
          if (var.size() == 2) {
            view.updateItemMatched("id", var.get(0), var.getObject(1));
          } else if (var.size() == 3) {
            //支持自定义id的key
            view.updateItemMatched(var.getString(0), var.get(1), var.getObject(2));
          }
        }
        break;
      case "deleteItemRange":
        view.deleteItemRange(var.getInt(0), var.getInt(1));
        break;
      case "setListData":
        if (LogUtils.isDebug()) {
          LogUtils.e("ScrollLog", "++++setListData FastListView fv:" + view);
        }
        if (view.getId() != -1) {
          view.setTemplateNode(Utils.getRenderNode(view));
          view.setHandleEventNodeId(view.getId());
        }
        //view.setList(var);
        view.setListWithParams(var, false, view.isUseDiff());
        break;
      case "setListDataWithParams":
        if (LogUtils.isDebug()) {
          LogUtils.e("ScrollLog", "++++setListDataWithParams FastListView fv:" + view);
        }
        if (view.getId() != -1) {
          view.setTemplateNode(Utils.getRenderNode(view));
          view.setHandleEventNodeId(view.getId());
        }
        //0:data
        //1: autoChangeVisible
        //2: useDiff
        view.setListWithParams(var.getArray(0), var.getBoolean(1), var.getBoolean(2));
        break;
      case "addListData":
        view.addData(var, 0);
        break;
      case "addListDataWithParams":
        view.addData(var.getArray(0), var.getInt(1));
        break;
      case "destroy":
        view.destroy();
        break;
      case "recycle":
        view.recycle();
        break;
      case "scrollToTop": {
        view.scrollToTop();
        break;
      }
      case "scrollToFocus": {
        LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG, ">scrollToFocus var:" + var);
        if (var.size() == 1) {
          view.scrollToFocus(var.getInt(0));
        } else if (var.size() == 2) {
          view.scrollToFocus(var.getInt(0), 0, var.getInt(1), null);
        } else {
          view.scrollToFocus(var.getInt(0), var.getInt(1), var.getInt(2), var.getString(3));
        }
        break;
      }
      case "prepareForRecycle":
        view.prepareForRecycle();
        break;
      case "setDisplay":
        view.setDisplay(var.getBoolean(0), false);
        break;
      case "changeDisplayState":
        view.setDisplay(var.getBoolean(0), var.getBoolean(1));
        break;
      case "notifySaveInstance":
        view.notifyPause();
        break;
      case "notifyRestoreInstance":
        view.notifyRestore();
        break;
      case "updateItemProps":
        view.updateItemProps(var.getString(0), var.getInt(1), var.getMap(2), var.getBoolean(3));
        break;
      case "dispatchItemFunction":
        view.dispatchItemFunction(var,null);
        break;
      case "clearAllPostTask":
        Log.e(FastAdapter.TAG_POST,"SCROLL_POSTER clearAllTask called from function this:"+view);
        view.clearAllTask();
        break;
        //2.5
      case "clearPostTaskByCate":
        Log.e(FastAdapter.TAG_POST,"SCROLL_POSTER clearPostTaskByCate called from function");
        HippyArray array = var.getArray(0);
        for(int i  = 0; i < array.size(); i ++){
          view.clearTaskByCate(array.getInt(i));
        }
        break;
      case "clearData":
        view.clearData();
        break;
      case "pausePostTask":
        view.pausePostTask();
        break;
      case "resumePostTask":
        view.resumePostTask();
        break;
      case "requestLayoutManual":
        view.requestLayoutManual();
        break;
      case "setSpanCount":
        view.setSpanCount(var.getInt(0));
        break;
      case "setBackgroundColor":
        view.setBackgroundColor(Color.parseColor(var.getString(0)));
        break;
      case "searchReplaceItem":
        ExtendUtil.searchReplaceItemByItemID(view,var.getString(0),var.getObject(1));
        break;
      case "setCustomStateEnableOnFocus":
        if (var.getArray(0) != null) {
          String[] names = new String[var.getArray(0).size()];
          for (int i = 0; i < var.getArray(0).size(); i++) {
            names[i] = var.getArray(0).getString(i);
          }
          view.setStateEnableOnFocusNames(names);
        }else{
          view.setStateEnableOnFocusNames(null);
        }
        break;
      case "setItemCustomState":
        view.changeItemState(var.getInt(0), var.getString(1), var.getBoolean(2));
        break;
      case "scheduleLayoutAnimation":
        if (var == null || var.size() == 0) return;
        int type = var.size() >= 1 ? var.getInt(0) : -1;
        if (type == -1) return;
        int interpolator = var.size() >= 2 ? var.getInt(1) : -1;
        int aniaDuration = var.size() >= 3 ? var.getInt(2) : -1;
        float delay = var.size() >= 4 ? (float) var.getDouble(3) : 0;
        //[type, interpolator, aniaDuration, delay]
//        Log.i("ZHAOPENG","scheduleLayoutAnimation var:"+var );
        view.startCustomLayoutAnimation(type,aniaDuration,interpolator,delay,false);
      break;
    }
  }


  @Override
  public void setPendingData(View view, Object data, RenderNode templateNode,boolean useDiff) {
    if (view instanceof FastListView) {
      if(LogUtils.isDebug()) {
        Log.i("FastListAdapter", "setPendingData controller method  view:" + view + ",id:" + view.getId());
      }
      ((FastListView) view).setHandleEventNodeId(templateNode.getId());
      ((FastListView) view).setPendingData(data, templateNode,false,useDiff);
    }
  }

  @Override
  public void setPendingData(View view, Object data, RenderNode templateNode) {
    if (view instanceof FastListView) {
      if(LogUtils.isDebug()) {
        Log.i("FastListAdapter", "setPendingData controller method  view:" + view + ",id:" + view.getId());
      }
      ((FastListView) view).setHandleEventNodeId(templateNode.getId());
      ((FastListView) view).setPendingData(data, templateNode,false);
    }
  }

  @HippyControllerProps(name = "checkScrollOffsetOnStateChanged", defaultType = HippyControllerProps.BOOLEAN)
  public void setCheckScrollOffsetOnStateChanged(FastListView view, boolean b) {
    view.setCheckScrollOffsetOnStateChanged(b);
  }


  @HippyControllerProps(name = "scrollEventThrottle", defaultType = HippyControllerProps.NUMBER, defaultNumber = 30.0D)
  public void setscrollEventThrottle(FastListView view, int scrollEventThrottle) {
    view.setScrollEventThrottle(scrollEventThrottle);
  }

  @HippyControllerProps(name = "onScrollEnable", defaultType = HippyControllerProps.BOOLEAN)
  public void setOnScrollEventEnable(FastListView view, boolean flag) {
    view.setOnScrollEventEnable(flag);
  }

  @HippyControllerProps(name = "eventSendItem", defaultType = HippyControllerProps.BOOLEAN)
  public void setEventSendItem(FastListView view, boolean flag) {
    if(view != null){
      view.setEventSendItem(flag);
    }
  }



  @Deprecated
  @HippyControllerProps(name = "enableScrollOffsetEvent", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setEnableScrollOffsetEvent(FastListView view, boolean mEnableScrollOffsetEvent) {
    view.setEnableScrollOffsetEvent(mEnableScrollOffsetEvent);
  }


  @HippyControllerProps(name = "scrollYLesserReferenceValue", defaultType = HippyControllerProps.NUMBER)
  public void setScrollYLesserReferenceValue(FastListView view, int value) {
    view.setScrollYLesserReferenceValue(value);
  }

  @HippyControllerProps(name = "scrollYGreaterReferenceValue", defaultType = HippyControllerProps.NUMBER)
  public void setScrollYGreaterReferenceValue(FastListView view, int value) {
    view.setScrollYGreaterReferenceValue(value);
  }

  @HippyControllerProps(name = "layoutTargetPosition", defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setLayoutTriggerTargetPosition(FastListView view, int position) {
    view.setLayoutTriggerTargetPosition(position);
  }

  @HippyControllerProps(name = "makeChildVisibleType", defaultType = HippyControllerProps.STRING)
  public void setMakeChildVisibleType(FastListView view, String type) {
    if ("normal".equals(type)) {
      view.setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID);
    } else if ("none".equals(type)) {
      view.setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_NONE);
    } else {
      view.setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER);
    }
  }

  @HippyControllerProps(name = "makeChildVisibleClampBackward", defaultType = HippyControllerProps.NUMBER)
  public void setRequestChildOnScreenClampBackward(FastListView view, int clampBackward) {
    view.setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID);
    view.setRequestChildOnScreenClampBackward(clampBackward);
  }

  @HippyControllerProps(name = "makeChildVisibleClampForward", defaultType = HippyControllerProps.NUMBER)
  public void setRequestChildOnScreenClampForward(FastListView view, int clampForward) {
    view.setRequestChildOnScreenType(TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID);
    view.setRequestChildOnScreenClampForward(clampForward);
  }

  @HippyControllerProps(name = "scrollThresholdHorizontal", defaultType = HippyControllerProps.NUMBER)
  public void setScrollThresholdHorizontal(FastListView view, int threshold) {
    view.setScrollThresholdHorizontal(threshold);
  }

  @HippyControllerProps(name = "scrollThresholdVertical", defaultType = HippyControllerProps.NUMBER)
  public void setScrollThresholdVertical(FastListView view, int threshold) {
    view.setScrollThresholdVertical(threshold);
  }


  @HippyControllerProps(name = "cachePool", defaultType = HippyControllerProps.MAP)
  public void setCacheSizeMap(FastListView view, HippyMap map) {
    view.setCachePoolMap(map);
  }

  @HippyControllerProps(name = "cachePoolName", defaultType = HippyControllerProps.STRING)
  public void setCacheSizeMap(FastListView view, String name) {
    view.setCachePoolName(name);
  }

  /*** zhaopeng  add  20201117 **/
  @HippyControllerProps(name = "focusMemory", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setFocusMemory(FastListView view, boolean enable) {
    view.enableFocusMemory(enable);
  }


  /*** zhaopeng  add  20201117 **/
  @HippyControllerProps(name = "pauseTaskOnHide", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setPauseTaskOnHide(FastListView view, boolean enable) {
    view.setPauseTaskOnHide(enable);
  }

  @HippyControllerProps(name = "setSelectChildPosition", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setSelectChildPosition(FastListView view, int position) {
    if (view != null) {
      view.setSelectChildPosition(position, true);
    }
  }

  @HippyControllerProps(name = "selectChildPosition", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setSelectChildPositionSimple(FastListView view, int position) {
    if (view != null) {
      view.setSelectChildPosition(position, true);
    }
  }

  @HippyControllerProps(name = "shakePreCheckNumber", defaultType = HippyControllerProps.NUMBER, defaultNumber = 2)
  public void setShakePreCheckNumber(FastListView view, int shakePreCheckNumber) {
    if (view != null) {
      view.setShakePreCheckNumber(shakePreCheckNumber);
    }
  }

  @HippyControllerProps(name = "placeholderFocusScale", defaultType = HippyControllerProps.NUMBER)
  public void setPlaceholderConfig(FastListView view,float placeholderFocusScale) {
    if (view != null) {
      if (view.getFastAdapter() != null) {
        view.getFastAdapter().placeholderFocusScale = placeholderFocusScale;
      }
    }
  }
  @HippyControllerProps(name = "placeholderColorString", defaultType = HippyControllerProps.STRING)
  public void setPlaceholderColor(FastListView view,String color) {
    if (view != null) {
      if (view.getFastAdapter() != null) {
        view.getFastAdapter().placeholderBackgroundColor = Color.parseColor(color);
      }
    }
  }
  @HippyControllerProps(name = "placeholderColor", defaultType = HippyControllerProps.NUMBER)
  public void setPlaceholderColor(FastListView view,int color) {
    if (view != null) {
      if (view.getFastAdapter() != null) {
        view.getFastAdapter().placeholderBackgroundColor = color;
      }
    }
  }
  @HippyControllerProps(name = "placeholderBorderRadius", defaultType = HippyControllerProps.NUMBER)
  public void setPlaceholderConfig(FastListView view,int br) {
    if (view != null) {
      if (view.getFastAdapter() != null) {
        view.getFastAdapter().placeholderBorderRadius = br;
      }
    }
  }


  @HippyControllerProps(name = "preloadItemNumber", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setPreloadItemNumber(FastListView view, int preloadItemNumber) {
    if (view != null) {
      view.setPreloadItemNumber(preloadItemNumber);
    }
  }

  //触底回弹
  @HippyControllerProps(name = "endHintEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setShakeEndEnable(FastListView view, boolean enable) {
    if (view != null) {
      view.setShakeEndEnable(enable);
    }
  }

  //焦点抖动
  @HippyControllerProps(name = "endShakeEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setListShakeSelf(FastListView view, boolean shakeSelf){
    view.setListShakeSelf(shakeSelf);
  }

  @HippyControllerProps(name = "forceBlockFocusOnFail", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setForceBlockFocusOnFail(FastListView view, boolean enable) {
    if (view != null) {
      view.setForceBlockFocusOnFail(enable);
    }
  }

  //此处vue是怎么传值的
  @HippyControllerProps(name = "blockFocusOnFail", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setBlockFocusOnFail(FastListView view, HippyMap hippyMap) {
    if (view != null) {
      view.setBlockFocusOnFail(new int[InternalExtendViewUtil.FOCUS_BLOCK_DIRECTION_ALL]);
    }
  }

  //此处vue是怎么传值的
  @HippyControllerProps(name = "listenFocusSearchOnFail", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setListenFocusSearchOnFail(FastListView view, boolean listen) {
    if (view != null) {
      view.setListenFocusSearchOnFail(listen);
    }
  }

  @HippyControllerProps(name = "enableSelectOnFocus", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setEnableSelectOnFocus(FastListView view, boolean flag) {
    view.setEnableSelectOnFocus(flag);
  }

  @HippyControllerProps(name = "enableStatesOnFocus", defaultType = HippyControllerProps.STRING)
  public void setEnableSelectOnFocus(FastListView view,HippyArray array) {
    if (array != null) {
      String[] names = new String[array.size()];
      for (int i = 0; i < array.size(); i++) {
        names[i] = array.getString(i);
      }
      view.setStateEnableOnFocusNames(names);
    }else{
      view.setStateEnableOnFocusNames(null);
    }
  }



  @HippyControllerProps(name = "useAdvancedFocusSearch", defaultType = HippyControllerProps.BOOLEAN)
  public void setUseAdvancedFocusSearch(FastListView view, boolean flag) {
    view.setUseAdvancedFocusSearch(flag);
  }

  @HippyControllerProps(name = "negativeKeyTime", defaultType = HippyControllerProps.NUMBER)
  public void setNegativeKeyTime(FastListView view, int time) {
    view.setNegativeKeyTime(time);
  }

  @HippyControllerProps(name = "postContentDelay", defaultType = HippyControllerProps.NUMBER)
  public void setPostContentDelay(FastListView view, int time) {
    view.setPostContentDelay(time);
  }

  @HippyControllerProps(name = "placeholderPostDelay", defaultType = HippyControllerProps.NUMBER)
  public void setPlaceholderPostDelay(FastListView view, int time) {
    view.setPlaceholderPostDelay(time);
  }

  @HippyControllerProps(name = "initFocusPositionAfterLayout", defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setInitFocusAfterLayout(FastListView view, int position) {
    view.setInitFocusPositionAfterLayout(position);
  }


  @HippyControllerProps(name = "enableKeepFocus", defaultType = HippyControllerProps.BOOLEAN)
  public void setEnableReFocus(FastListView view, boolean enable) {
//    Log.i("ZHAOPENG","enableKeepFocus :"+enable+",view : "+ExtendUtil.debugViewLite(view));
    view.setEnableReFocus(enable);
  }

  @HippyControllerProps(name = "enableGridLoad", defaultType = HippyControllerProps.BOOLEAN)
  public void setGridLoading(FastListView view, boolean enable) {
    view.setGridLoading(enable);
  }

  @HippyControllerProps(name = "scrollFactor", defaultType = HippyControllerProps.NUMBER)
  public void setScrollFactor(FastListView view, float factor) {
    view.setScrollFactor(factor);
  }

  @HippyControllerProps(name = "setLoadDelayTime", defaultType = HippyControllerProps.NUMBER)
  public void setLoadDelayTime(FastListView view, int time) {
    view.setLoadDelayTime(time);
  }
//  @HippyControllerProps(name = "enableAutoFocus", defaultType = HippyControllerProps.BOOLEAN)
//  public void setEnableAutoFocus(FastListView view, boolean enable)
//  {
//    view.setEnableAutoFocus(enable);
//  }

  //START >>>>>>>>>>list control 属性 2.7 添加
  @HippyControllerProps(name = "nextTargetFocusPosition", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setTargetFocusChildPosition(FastListView view, int position) {
    if (view != null) {
      view.setTargetFocusChildPosition(position);
    }
  }
//  @HippyControllerProps(name = "scrollToPosition", defaultType = HippyControllerProps.NUMBER)
//  public void setScrollTo(FastListView view, int pos) {
//    view.diffSetScrollToPosition(pos);
//  }
//  @HippyControllerProps(name = "scrollToOffset", defaultType = HippyControllerProps.NUMBER)
//  public void setScrollToOffset(FastListView view, int offset) {
//    view.diffSetScrollToOffset(offset);
//  }

  @HippyControllerProps(name = "singleSelectPosition", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setSelectPosition(FastListView view, int position) {
    if (view != null) {
      view.diffSetSelectionPosition(position);
    }
  }

//  @HippyControllerProps(name = "focusPosition", defaultType = HippyControllerProps.NUMBER)
//  public void setFocusPosition(FastListView view, int pos) {
//    view.diffSetFocusPosition(pos);
//  }

  @HippyControllerProps(name = "autofocusPosition", defaultType = HippyControllerProps.NUMBER)
  public void setAutofocusPosition(FastListView view, int pos) {
    Log.i(AutoFocusManager.TAG,"--------setAutofocusPosition by user pos:"+pos);
    view.setAutofocusPosition(pos);
    LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG,":setAutofocusPosition :"+pos+" view is :"+ ExtendUtil.debugViewLite(view));
  }

  @HippyControllerProps(name = "autofocusPositionInfiniteMode", defaultType = HippyControllerProps.NUMBER)
  public void setAutoscrollPositionOffset(FastListView view, int offset) {
    view.setAutofocusPositionInfiniteMode( (int) PixelUtil.dp2px(offset));
  }

  @HippyControllerProps(name = "autoscrollPosition", defaultType = HippyControllerProps.NUMBER)
  public void setAutoscrollPosition(FastListView view, int pos) {
    view.setAutoscrollPosition(pos);
  }

  @HippyControllerProps(name = "autoscroll", defaultType = HippyControllerProps.ARRAY)
  public void setAutoscroll(FastListView view, HippyArray array) {
    Log.i(AutoFocusManager.TAG,"--------setAutoscroll by user array:"+array);
    view.setAutoscrollPosition(array.getInt(0),false, (int) PixelUtil.dp2px(array.getInt(1)));
    LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG,":autoscrollPosition +"+array+" view is :"+ ExtendUtil.debugViewLite(view));
  }

  @HippyControllerProps(name = "autoscrollInfiniteMode", defaultType = HippyControllerProps.ARRAY)
  public void setAutoscrollOffset(FastListView view, HippyArray array) {
    Log.i(AutoFocusManager.TAG,"--------setAutoscroll by user array:"+array);
    view.setAutoscrollInfiniteMode(array.getInt(0),false, (int) PixelUtil.dp2px(array.getInt(1)));
    LogAdapterUtils.log(view.getContext(), FocusDispatchView.TAG,":autoscrollPosition +"+array+" view is :"+ ExtendUtil.debugViewLite(view));
  }

  @HippyControllerProps(name = "autoSelectPosition", defaultType = HippyControllerProps.NUMBER)
  public void setAutoSelectPosition(FastListView view, int pos) {
    view.setAutoSelectPosition(pos);
  }
  @HippyControllerProps(name = "sharedItemStore", defaultType = HippyControllerProps.STRING)
  public void setSharedItemStore(final FastListView lv, String name) {
    lv.setSharedItemStore(name);
  }

  //2.10
  @HippyControllerProps(name = "useLayoutAnimation", defaultType = HippyControllerProps.ARRAY)
  public void setUseLayoutAnimation(FastListView view, HippyArray array) {
//    Log.i("ZHAOPENG","setUseLayoutAnimation  call by attr array:" + array);
    if (array == null || array.size() == 0) return;
    int type = array.size() >= 1 ? array.getInt(0) : -1;
    if (type == -1) return;
    int interpolator = array.size() >= 2 ? array.getInt(1) : -1;
    int duration = array.size() >= 3 ? array.getInt(2) : -1;
    float delay = array.size() >= 4 ? (float) array.getDouble(3) : 0;
    view.startCustomLayoutAnimation(type, duration, interpolator, delay, true);
  }

  //END  >>>>>>>>>>>>>list control 属性
  public final static class PromiseTakeover implements Promise{
    private Promise realPromise;
    private int position;
    private String sid;

    public PromiseTakeover(Promise realPromise,int position) {
      this.realPromise = realPromise;
      this.position = position;
      this.sid = "";
    }

    public PromiseTakeover(Promise realPromise,String sid) {
      this.realPromise = realPromise;
      this.position = -1;
      this.sid = sid;
    }

    @Override
    public void resolve(Object value) {
      HippyMap map = new HippyMap();
      map.pushInt("position",position);
      map.pushString("sid",sid);
      map.pushObject("value",value);
      realPromise.resolve(map);
    }

    @Override
    public void reject(Object error) {
      HippyMap map = new HippyMap();
      map.pushInt("position",position);
      map.pushString("sid",sid);
      map.pushObject("error",error);
      realPromise.reject(map);
    }

    @Override
    public boolean isCallback() {
      return realPromise.isCallback();
    }

    @Override
    public String getCallId() {
      return realPromise.getCallId();
    }

    @Override
    public void setTransferType(HippyEngine.BridgeTransferType type) {
      realPromise.setTransferType(type);
    }



  }
}
