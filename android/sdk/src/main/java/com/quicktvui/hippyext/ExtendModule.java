package com.quicktvui.hippyext;


import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.tencent.mtt.hippy.BuildConfig;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.UIThreadUtils;


@HippyNativeModule(name = "ExtendModule",thread = HippyNativeModule.Thread.MAIN)
public class ExtendModule extends HippyNativeModuleBase {
  //这里为了兼容老版本的core
  public static final String LEGACY_CORE_VERSION = "1.10.0";

  public ExtendModule(HippyEngineContext context) {
    super(context);
  }

  @HippyMethod(name = "callUIFunctionWithPromise")
  public void callFunction(String sid, String funcName, HippyArray args,@Nullable Promise promise) {
    handleCallFunction(sid,funcName,args,promise);
  }

  @HippyMethod(name = "callUIFunction")
  public void callFunction(String sid, String funcName, HippyArray args) {
    handleCallFunction(sid,funcName,args,null);
  }

  @HippyMethod(name = "searchReplaceItem")
  public void searchReplaceItem(String sid, HippyMap data) {
    View rootView = ExtendUtil.findWindowRoot(mContext);
    Log.i("ExtendModule","searchReplaceItem sid:"+sid+" rootView:"+rootView);
    if (rootView != null) {
      UIThreadUtils.runOnUiThread(() -> {
        ExtendUtil.searchReplaceItemByItemID(rootView, sid, data);
      });
    }else{
      Log.e("ExtendModule","searchReplaceItem rootView is null");
    }
  }

  @HippyMethod(name = "searchReplaceItemTraverse")
  public void searchReplaceItemTraverse(String sid, HippyMap data) {
    View rootView = ExtendUtil.findWindowRoot(mContext);
    Log.i("ExtendModule","searchReplaceItem sid:"+sid+" rootView:"+rootView);
    if (rootView != null) {
      UIThreadUtils.runOnUiThread(() -> {
        ExtendUtil.searchReplaceItemByItemID(rootView, sid, data,true);
      });
    }else{
      Log.e("ExtendModule","searchReplaceItem rootView is null");
    }
  }

  @HippyMethod(name = "getCoreSDKInfo")
  public void getCoreSDKInfo(Promise promise) {
    HippyMap i = new HippyMap();
    i.pushString("core_version",LEGACY_CORE_VERSION);
    i.pushString("core_version_name",LEGACY_CORE_VERSION);
    i.pushString("commit",BuildConfig.COMMIT);
    i.pushString("version",BuildConfig.CORE_VERSION_CODE);
    i.pushString("versionName",BuildConfig.CORE_VERSION_NAME);
    i.pushBoolean("enable_so_download",BuildConfig.ENABLE_SO_DOWNLOAD);
    i.pushString("build_type",BuildConfig.BUILD_TYPE);
    i.pushString("lib_package",BuildConfig.LIBRARY_PACKAGE_NAME);
    promise.resolve(i);
  }

//  ExtendUtil.searchReplaceItemByItemID(pv, sid, itemData)

  void handleCallFunction(String sid, String funcName, HippyArray args,@Nullable Promise promise) {
    if(LogUtils.isDebug()){
      Log.i("ExtendModule","handleCallFunction sid:"+sid+" funcName:"+funcName+",args:"+args);
    }
    try {
      final int rootID = mContext.getDomManager().getRootNodeId();
      final View hippyRoot = mContext.getRenderManager().getControllerManager().findView(rootID);
      if (hippyRoot != null) {
        final View rootView = hippyRoot.getRootView();
        if (rootView != null) {
          UIThreadUtils.runOnUiThread(() -> {
//          Log.d("DebugExtend", "callFunction rootID:"+rootID+",RootView:"+rootView+",hippyRoot context:"+ hippyRoot.getContext());
//    HippyInstanceContext instanceContext
//          Log.i("DebugExtend","callUIFunction sid:"+sid+" funcName:"+funcName+" args:"+args+" promise:"+promise);
            HippyViewController.dispatchFunctionBySid(rootView, null, (HippyInstanceContext) hippyRoot.getContext(), sid, funcName, args, promise, "DebugExtend", true);
          });
        }else{
          if (promise != null) {
            promise.reject("rootView is null");
          }
        }
      } else {
        if (promise != null) {
          promise.reject("hippyRoot is null");
        }
      }
    }catch (Throwable t){
      t.printStackTrace();
      if(promise!=null){
        promise.reject(t.getMessage());
      }
    }


  }



//  @HippyMethod(name = "scrollFactor")
//  public void setScrollFactor(float scrollFactor) {
//    Log.i("FastListModule","scrollFactor scrollFactor:"+scrollFactor);
//  }


}
