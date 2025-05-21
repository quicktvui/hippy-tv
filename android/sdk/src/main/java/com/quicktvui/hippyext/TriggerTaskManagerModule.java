package com.quicktvui.hippyext;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.Nullable;

import com.quicktvui.base.ui.ExtendViewGroup;
import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.base.ui.TVViewUtil;
import com.quicktvui.base.ui.TriggerTaskHost;
import com.quicktvui.base.ui.ExtendTag;
import com.quicktvui.hippyext.views.fastlist.Utils;
import com.tencent.mtt.hippy.BuildConfig;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;

@HippyNativeModule(name = TriggerTaskManagerModule.CLASSNAME)
public class TriggerTaskManagerModule extends HippyNativeModuleBase {
  static final String CLASSNAME = "TriggerTaskModule";
  public static final String TAG = "DebugTriggerTask";
  public static final String KEY_PROP_NAME = "triggerTask";
  static final String KEY_EVENT = "event";
  static final String KEY_FUNCTION = "function";
  static final String KEY_VALUE = "refValue";
  static final String KEY_TARGET = "target";
  static final String KEY_TARGET_SID = "sid";
  static final String KEY_PARAMS = "params";
  static final String KEY_DELAY = "delay";
  public static boolean DEBUG = BuildConfig.DEBUG;


  public TriggerTaskManagerModule(HippyEngineContext context) {
    super(context);
  }

//  public static void dispatchTriggerTask(TriggerTaskHost hostView, String event) {
//    dispatchTriggerTask(hostView,event,null);
//  }

  public static boolean dispatchTriggerTask(TriggerTaskHost hostView, String event) {
    return dispatchTriggerTask(hostView,event,-1);
  }

  public static boolean dispatchTriggerTask(TriggerTaskHost hostView, String event,int id) {
    final View view = hostView.getHostView();

    if(view != null && view.getContext() instanceof HippyInstanceContext){
      final HippyEngineContext context =  ((HippyInstanceContext) view.getContext()).getEngineContext();
      return dispatchTriggerTask(context,hostView,event,id);
    }

    return false;
  }


  static boolean dispatchTriggerTask(HippyEngineContext context,TriggerTaskHost host, String event,int id) {
    boolean b = false;
    if(host == null || context == null){
      return false;
    }
    try {
      final View hostView = host.getHostView();
      if(hostView == null){
        return false;
      }
      final RenderManager renderManager = context.getRenderManager();
      //renderManager.getControllerManager().dispatchUIFunction();

      final int originID = hostView.getId();

      if(renderManager == null){
        return false;
      }

      RenderNode rd = renderManager.getRenderNode(originID);

      if(rd == null || rd.getProps() == null){
        return false;
      }
      HippyArray array = rd.getProps().getArray(KEY_PROP_NAME);

      if(array == null || array.size() <= 0){
        return false;
      }
      for (int i = 0; i < array.size(); i++) {
        HippyMap map = array.getMap(i);
        if (event.equals(map.getString(KEY_EVENT))) {
          View targetView = null;
          final String targetSID = map.getString(KEY_TARGET_SID);
            final String targetClassName = map.getString(KEY_TARGET);
            FocusUtils.FocusParams fp = new FocusUtils.FocusParams();
            fp.specifiedTargetViewName = targetClassName;
            fp.specifiedTargetSID = targetSID;
            targetView = findViewByNameOrSID(fp,hostView.getRootView());
          if (targetView != null) {
            final RenderNode targetNode = renderManager.getRenderNode(targetView.getId());
            // 任务命中,执行
            if(DEBUG) {
              Log.d(TAG, "dispatchTriggerTask exe task :" + map + ",id:" + id + ",event：" + event);
            }
            final int delay = map.getInt(KEY_DELAY);
            renderManager.getControllerManager().dispatchUIFunction(targetView.getId(), targetNode.getClassName(), map.getString(KEY_FUNCTION), map.getArray(KEY_PARAMS),delay);
            if (id > -1) {
              new HippyViewEvent(event).send(id, Utils.getHippyContext(hostView),null);
            }else{
              new HippyViewEvent(event).send(hostView,null);
            }

            b = true;
          } else {
            // 任务命中,执行
            if (DEBUG) {
              Log.e(TAG, "dispatchTriggerTask target is null, task :" + map);
            }
          }
        }
      }
    }catch (Throwable t){
      t.printStackTrace();
    }

    return b;

  }

  static View findViewByNameOrSID(FocusUtils.FocusParams fp, View view) {
//    Log.i(TriggerTaskManagerModule.TAG,"------findViewByNameOrSID view:"+ExtendUtil.debugViewLite(view)+",fp:"+fp);
    if (TextUtils.isEmpty(fp.specifiedTargetSID) && TextUtils.isEmpty(fp.specifiedTargetViewName)) {
      return null;
    }
    String viewName = TVViewUtil.findName(view);
    if (fp.specifiedTargetViewName != null &&  fp.specifiedTargetViewName.equals(viewName)) {
      if(LogUtils.isDebug()) {
        Log.i(TriggerTaskManagerModule.TAG, "------findViewByNameOrSID view:" + ExtendUtil.debugViewLite(view) + ",fp:" + fp);
      }
      return view;
    }
    final String viewSID = ExtendTag.obtainExtendTag(view).sid;
    if(fp.specifiedTargetSID != null && fp.specifiedTargetSID.equals(viewSID)){
      if(LogUtils.isDebug()) {
        Log.i(TriggerTaskManagerModule.TAG, "------findViewByNameOrSID view:" + ExtendUtil.debugViewLite(view) + ",fp:" + fp);
      }
      return view;
    }

    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      if(g instanceof ExtendViewGroup){
        if( ((ExtendViewGroup) g).isPageHidden()){
          LogUtils.d(TAG,"findTargetViewByName skip on page is hidden target :"+g);
        }else{
          for(int i = 0; i < g.getChildCount(); i++){
            View r =  findViewByNameOrSID(fp,g.getChildAt(i));
            if( r != null){
              return r;
            }
          }
        }
      }else{
        for(int i = 0; i < g.getChildCount(); i++){
          View r =  findViewByNameOrSID(fp,g.getChildAt(i));
          if( r != null){
            return r;
          }
        }
      }
    }
    return null;
  }

  static View findTargetViewByName(View target, String name){
    if(target != null && name != null){
      final String targetName = findName(target);
      if(name.equals(targetName)){
        return target;
      }
      if(target instanceof ViewGroup){
        ViewGroup g = (ViewGroup) target;
        if(g instanceof ExtendViewGroup){
          if( ((ExtendViewGroup) g).isPageHidden()){
            LogUtils.d(TAG,"findTargetViewByName skip on page is hidden target :"+g);
          }else{
            for(int i = 0; i < g.getChildCount(); i++){
              View r =  findTargetViewByName(g.getChildAt(i),name);
              if( r != null){
                return r;
              }
            }
          }
        }else{
          for(int i = 0; i < g.getChildCount(); i++){
            View r =  findTargetViewByName(g.getChildAt(i),name);
            if( r != null){
              return r;
            }
          }
        }

      }
    }
    return null;
  }

  public static @Nullable String findName(View v){
//    final HippyMap map = getTagFromView(v);
//    if(map != null){
//      return map.getString(NodeProps.NAME);
//    }
//    return null;
    return TVViewUtil.findName(v);
  }

  private static @Nullable HippyMap getTagFromView(View v){
    if(v != null && v.getTag() instanceof HippyMap){
      return (HippyMap) v.getTag();
    }
    return null;
  }





}
