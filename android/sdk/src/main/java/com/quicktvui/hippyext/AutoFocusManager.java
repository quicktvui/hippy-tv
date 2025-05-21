package com.quicktvui.hippyext;


import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.quicktvui.base.ui.ExtendViewGroup;
import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.base.ui.ITVView;
import com.quicktvui.base.ui.ExtendTag;
import com.quicktvui.base.ui.TVFocusAnimHelper;
import com.quicktvui.hippyext.views.fastlist.FastAdapter;
import com.quicktvui.hippyext.views.fastlist.FastListView;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.modal.HippyModalHostView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

public class AutoFocusManager {
  /**!!注意!!
   * 下一个焦点，当页面当中没有焦点时，指定为nextAutofocus的view会自动请求焦点，如果当页面中已经存在焦点时，nextAutofocus不会自动请求焦点
   */
  private String nextAutofocus;
  private String nextPendingAutofocus;
  private int delay = 0;
  public static final String TAG = "DebugAutofocus";
  private Runnable requestFocusTask;
  final View containerView;
//  View currentFocus;
  private View pendingAutofocus;
  final static boolean DEBUG = LogUtils.isDebug();


  public AutoFocusManager(View containerView) {
    this.containerView = containerView;
  }

  public static void handleFocusScaleImmediately(boolean gainFocus, int direction, Rect previouslyFocusedRect) {

  }

  public boolean isNextAutofocus(String id){
    return id != null && id.equals(nextAutofocus);
  }

  public boolean isNextPendingAutofocus(String id){
    return id != null && id.equals(nextPendingAutofocus);
  }

  private void exeRequestFocus(@NonNull View view){

    if(DEBUG) {
      Log.i(TAG, "exeRequestFocus view:"+ExtendUtil.debugView(view)+",isFocusable:"+view.isFocusable());
    }
    if(view.getVisibility() != View.VISIBLE){
      Log.e(TAG, "exeRequestFocus error on view invisible view:"+ExtendUtil.debugView(view)+",view.getVisibility():"+view.getVisibility());
      return;
    }
    if(view.isFocusable()){
//      this.nextAppearFocus = null;
      Log.i(TAG,"exeRequestFocus tag:"+this.nextAutofocus +",view :"+view);
      requestFocusOnFocusableView(view);
    }else {
      if (view.getParent() instanceof FastAdapter.ItemContainer) {
        ((FastAdapter.ItemContainer) view.getParent()).getItemView().requestFocus();
      }else{
        requestFocusOnFocusableView(view);
      }
    }
  }
  private boolean requestFocusOnFocusableView(View view){
    if (view != null ) {
      if(DEBUG) {
        if (view.isFocusable()) {
          Log.e(TAG, "------requestFocusOnFocusableView: isFocusable " + view.isFocusable() + ",view:" + view + ",isFocused:" + view.isFocused());
        } else {
          Log.i(TAG, "------requestFocusOnFocusableView: isFocusable " + view.isFocusable() + ",view:" + view + ",isFocused:" + view.isFocused());
        }
      }
      if(view.isFocusable()) {
        if(containerView instanceof ViewGroup && ((ViewGroup) containerView).getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS){
            Log.e(TAG, "------requestFocusOnFocusableView return on container blocked" + ",view :" + view);
          pendingAutofocus = view;
        }
//        this.nextAutofocus = null;
        final ViewParent blockedView = FocusUtils.findFocusBlockedParent(view.getParent());
        if (blockedView instanceof View) {
          Log.w(TAG,"---exe view requestFocus error on blockedView:"+ExtendUtil.debugView((View) blockedView));
          return false;
        }else{
//          preventFocusAnimationDirty = true;
          if(view.isFocused()) {
            view.clearFocus();
            if(LogUtils.isDebug()) {
              Log.d(TAG, "---exe view requestFocus on focused View:" + ExtendUtil.debugView(view));
            }
          }else{
            if(LogUtils.isDebug()) {
              Log.d(TAG, "---exe view requestFocus :" + ExtendUtil.debugView(view));
            }
          }
          pendingAutofocus = null;
          return view.requestFocus();
        }
      }else{
        if(view instanceof ViewGroup){
          for(int i =0 ; i < ((ViewGroup) view).getChildCount();i ++) {
            View child = ((ViewGroup) view).getChildAt(i);
            if(requestFocusOnFocusableView(child)){
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public void setGlobalAutofocusSID(String sid, int delay){
    setGlobalAutofocusSID(sid,delay,null);
  }


  public void setGlobalAutofocusSID(String sid, int delay,@Nullable View postView){

    View taskRunner = postView;
    if(taskRunner == null){
      taskRunner = containerView;
    }
    if(DEBUG) {
      Log.d(TAG, "setGlobalAutofocusSID called sid:" + sid + ",this:" + this+",delay:"+delay+",postView:"+ExtendUtil.debugViewLite(postView));
    }
    this.nextAutofocus = sid;
    this.nextPendingAutofocus = sid;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      //final RootView root = InternalExtendViewUtil.findRootViewConsiderOrder()
      if (taskRunner != null) {
        View view = findViewByTagID(containerView,sid);
        if(DEBUG) {
          Log.d(TAG, "setAppearFocusTag findViewByTagID sid:" + sid + ",view:" + view + ",isAttachedToWindow:" + ((view != null) ?
            view.isAttachedToWindow() : null));
        }
        if(view != null && view.isAttachedToWindow() && delay < 1){
          exeRequestFocus(view);
        }else if(delay > 0){
          taskRunner.removeCallbacks(requestFocusTask);
          requestFocusTask = () -> {
            if(DEBUG) {
              Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>setAppearFocusTag delay find target sid:" + sid );
            }
            View delayTarget = findViewByTagID(containerView,sid);
            if (delayTarget != null) {
              exeRequestFocus(delayTarget);
            }
          };
          taskRunner.postDelayed(requestFocusTask,delay);
        }
      }
    }
//    if(delay > -1) {
//      this.delay = delay;
//    }
  }

//  public static void logParent(View view){
////    Log.d(TAG,"@@@@@@@@logParent check view:"+view);
//    if (view != null && view.getParent() instanceof View) {
//      logParent((View) view.getParent());
//    }
//  }

  public static View  findViewByTagID(View view,String id){

    return ExtendUtil.findViewBySID(id,view);
  }

  public static boolean isNextGlobalPendingAutofocus(View view){
    AutoFocusManager af = findAutoFocusManagerFromRoot(view);
    if (af != null) {
      boolean  b =  af.isNextPendingAutofocus(ExtendTag.obtainExtendTag(view).sid);
      if(b && LogUtils.isDebug()) {
        Log.d(TAG, "find NextGlobalPendingAutofocus view by sid , sid:" + ExtendTag.obtainExtendTag(view).sid+",view :"+view);
      }
      return b;
    }else {
      Log.e(TAG,"isNextGlobalPendingAutofocus return on af is null global ,this sid:"+ExtendTag.obtainExtendTag(view).sid);
    }
    return false;
  }

  public static boolean isNextGlobalAutofocus(View view,String sid){
    AutoFocusManager af = findAutoFocusManagerFromRoot(view);
    if (af != null) {
      boolean  b =  af.isNextAutofocus(sid);
      if(b && LogUtils.isDebug()) {
        Log.d(TAG, "find isNextGlobalAutofocus view by sid , sid:" + ExtendTag.obtainExtendTag(view).sid+",view :"+view);
      }
      return b;
    }else {
      Log.e(TAG,"isNextGlobalAutofocus return on af is null global ,this sid:"+ExtendTag.obtainExtendTag(view).sid);
    }
    return false;
  }

  public static boolean isAutofocusView(View view){
    if(view instanceof ITVView){
      if(((ITVView) view).isAutoFocus()){
        if(LogUtils.isDebug()) {
          Log.i(TAG, "find autoFocus  view:" + ExtendUtil.debugView(view));
        }
        return true;
      }
    }
    AutoFocusManager af = findAutoFocusManagerFromRoot(view);
    if (af != null) {
      boolean  b =  af.isNextAutofocus(ExtendTag.obtainExtendTag(view).sid);
      if(b && LogUtils.isDebug()) {
        Log.d(TAG, "find autofocus view by sid , sid:" + ExtendTag.obtainExtendTag(view).sid+",view :"+ExtendUtil.debugViewLite(view)+",this:"+af+",getWindowId:"+view.getWindowId());
      }
      return b;
    }else {
      Log.w(TAG,"isAutofocusView return on af is null global ,this sid:"+ExtendTag.obtainExtendTag(view).sid);
    }
    return false;
  }


  private void postRequestFocus(final View view,int delay){
    final View root = containerView;
    if (root != null) {
      if (requestFocusTask != null) {
        root.removeCallbacks(requestFocusTask);
      }
      requestFocusTask = () -> {
        exeRequestFocus(view);
      };
      if(delay < 1){
        requestFocusTask.run();
      }else{
        root.postDelayed(requestFocusTask,delay);
      }
    }else{
//      Log.e(TAG,"postRequestFocus error on rootView is null");
    }

  }

  public static boolean requestAutofocusTraverse(View v){

    final AutoFocusManager af = findAutoFocusManagerFromRoot(v);
    if(LogUtils.isDebug()){
      Log.i(TAG,"requestAutofocusTraverse view:"+v+",af:"+af);
    }
    if (af != null) {
      af.preventFocusAnimationDirty = true;
      return af.requestAutofocusTraverseInner(v);
    }
    return false;
  }

  boolean preventFocusAnimationDirty = false;

  public static void preventFocusAnimationOneShot(View v){
    final AutoFocusManager af = findAutoFocusManagerFromRoot(v);
    if (af != null) {
      af.preventFocusAnimationDirty = true;
    }
  }

//  public static boolean isPreventFocusAnimation(View v){
//    final AutoFocusManager af = findAutoFocusManagerFromRoot(v);
//    if (af != null) {
//      return af.preventFocusAnimationDirty ;
//    }
//    return false;
//  }


  public static boolean checkCanAutofocus(View view){
    if (view != null) {
        if(view.getWidth() > 0 && view.getHeight() > 0  && view.getVisibility() == View.VISIBLE){
            return true;
        }
    }
    return false;
  }

  boolean requestAutofocusTraverseInner(View view){
    if(LogUtils.isDebug()) {
      Log.i(TAG, "-->requestAutofocusTraverseInner called by view:" + ExtendUtil.debugViewLite(view));
    }
    if (view != null && view.getVisibility() == View.VISIBLE) {
      if(isAutofocusView(view) && checkCanAutofocus(view)) {
//        this.nextAutofocus = null;
        //Log.e(TAG, "------requestAutofocusTraverse: view " + view.isFocusable() + ",view:" + view + ",isFocused:" + view.isFocused());
        postRequestFocus(view,delay);
        return true;
      }else{
        if (view instanceof FastListView) {
          ((FastListView) view).requestAutofocusPosition();
        }
        if(view instanceof ViewGroup){
          for(int i =0 ; i < ((ViewGroup) view).getChildCount();i ++) {
            View child = ((ViewGroup) view).getChildAt(i);
            if(requestAutofocusTraverse(child)){
              return true;
            }
          }
        }
      }
    }
    return false;
  }


  public void checkAndRequestAutoFocus(View view, String id){
      if(isNextAutofocus(id)){
        Log.i(TAG,"checkAndRequestFocus targeted! name:"+id+",view:"+view);
        postRequestFocus(view,delay);
      }
  }

  /**
   * 从任意view的中寻找rootView中的AutoFocusManager
   * @param anyView
   * @return
   */
  public static @Nullable AutoFocusManager findAutoFocusManagerFromRoot(View anyView){

    final View rootView = ExtendUtil.findPageRootView(anyView);
    if(rootView instanceof HippyViewGroup){
//      Log.i(AutoFocusManager.TAG,"findAutoFocusManagerFromRoot rootView is :"+ExtendUtil.debugViewLite(rootView)+",view:"+ExtendUtil.debugViewLite(anyView));
      //((HippyViewGroup) rootView).getAutoFocusManager().setAppearFocusTag(autoFocusID,0);
      return ((HippyViewGroup) rootView).getAutoFocusManager();
    }else{
      //final View rootView = HippyViewGroup.findPageRootView(view);
      if(anyView instanceof HippyModalHostView.DialogRootViewGroup){
        return ((HippyModalHostView.DialogRootViewGroup) anyView).getAutoFocusManager();
      }
      Log.w(AutoFocusManager.TAG,"findAutoFocusManagerFromRoot error rootView is :"+ExtendUtil.debugViewLite(rootView)+",view:"+ExtendUtil.debugViewLite(anyView));
    }
    return null;
  }

  public static void checkAutoFocusSID(View view){

  }

  public void requestGlobalRequestFocus(View requestContainer,View view,int type){
    if(view != null && view.getVisibility() == View.VISIBLE && !view.isFocused()) {
        pendingAutofocus = null;
        // Log.i(TAG,"globalRequestFocus view:"+view+",requestFocusTask:"+af.requestFocusTask);
        if (LogUtils.isDebug()) {
          Log.i(TAG, "requestGlobalRequestFocus target:" + ExtendUtil.debugViewLite(view) + ",type:" + FocusUtils.getAutofocusTypeString(type)+",globalNextFocus:"+nextAutofocus+"," +
            "container hasFocus:"+(requestContainer == null ? "null" : requestContainer.hasFocus())+",container:"+ExtendUtil.debugViewLite(requestContainer));
        }
        postRequestFocus(view, delay);
      }
  }

//  public static void globalRequestFocus(View view){
//    if(view != null && view.getVisibility() == View.VISIBLE && !view.isFocused()) {
//      final AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(view);
//      if (af != null) {
//          af.pendingAutofocus = null;
//         // Log.i(TAG,"globalRequestFocus view:"+view+",requestFocusTask:"+af.requestFocusTask);
//          af.postRequestFocus(view, af.delay);
//      }
//    }
//  }

  public static void notifyUnBlockFocus(View view){
    if(view != null) {
      final AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(view);
      if (af != null) {
        af.onUnBlockFocus();
      }
    }
  }

  private void onUnBlockFocus() {
    if (pendingAutofocus != null) {
      postRequestFocus(pendingAutofocus,0);
    }
  }

  public void handleOnFocusChange(View view, boolean gainFocus, float mFocusScaleX, float mFocusScaleY, int duration) {
    //Log.e(TAG,"-----handleOnFocusChange preventFocusAnimationDirty :"+preventFocusAnimationDirty+",duration:"+duration);
    if(preventFocusAnimationDirty){
      TVFocusAnimHelper.handleOnFocusChange(view, gainFocus, mFocusScaleX, mFocusScaleY, 0);
      preventFocusAnimationDirty = false;
    }else {
      TVFocusAnimHelper.handleOnFocusChange(view, gainFocus, mFocusScaleX, mFocusScaleY, duration);
    }
  }

  public void onRequestChildFocus(View child, View focused) {
      nextPendingAutofocus = null;
//      currentFocus = focused;
  }

  public String getNextFocus() {
    return nextAutofocus;
  }
}
