package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.hippy.views.view.HippyViewGroupController;


@HippyController(
        name = ReplaceChildController.CLASS_NAME
)
public class ReplaceChildController extends HippyViewGroupController {

  public static final String CLASS_NAME = "ReplaceChildView";
  public static final String TAG = ReplaceChildView.TAG;


  public ReplaceChildController() {

  }

  @HippyControllerProps(name = "childSID", defaultType = HippyControllerProps.STRING)
  public void setBoundTag(final View lv, String idTag){
    //Log.i(TAG," setBoundTag setBoundID :"+idTag);
    if(lv instanceof ReplaceChildView){
      ((ReplaceChildView) lv).setBoundID(idTag);
    }
  }

  @HippyControllerProps(name = "markChildSID", defaultType = HippyControllerProps.STRING)
  public void setMarkChildSID(final View lv, String idTag){
    if(lv instanceof ReplaceChildView){
      ((ReplaceChildView) lv).markChildSID(idTag);
    }
  }

  @HippyControllerProps(name = "eventReceiverSID", defaultType = HippyControllerProps.STRING)
  public void setEventReceiverSID(final View lv, String sid){
    //Log.i(TAG," setBoundTag setBoundID :"+idTag);
    if(lv instanceof ReplaceChildView){
      ((ReplaceChildView) lv).setEventReceiverSID(sid);
    }
  }

  @HippyControllerProps(name = "replaceOnVisibilityChanged", defaultType = HippyControllerProps.BOOLEAN)
  public void setReplaceOnVisibilityChanged(final View lv, Boolean b){
    if(lv instanceof ReplaceChildView){
      ((ReplaceChildView) lv).setReplaceOnVisibilityChanged(b);
    }
  }


  protected View createViewImpl(Context context) {
        //return new SurfaceViewContainer(context);
        return null;
    }

    @Override
    protected View createViewImpl(Context context, HippyMap iniProps) {
      ReplaceChildView ReplaceChildView = new ReplaceChildView(context);
      //Log.i(TAG,"createViewImpl IndieViewContainer:"+ ReplaceChildView);
      return ReplaceChildView;
    }


  @Override
  public void dispatchFunction(HippyViewGroup view, String functionName, HippyArray var) {
    super.dispatchFunction(view, functionName, var);
    if (view instanceof ReplaceChildView) {
      switch (functionName){
        case "setChildSID":
          if(LogUtils.isDebug()) {
            Log.i(TAG, "dispatchFunctionBySid 1  setBoundID:" + var.getString(0));
          }
          ((ReplaceChildView) view).exeReplaceChild(var.getString(0));
          break;
      }
    }
  }


  @Override
  public void dispatchFunction(HippyViewGroup view, String functionName, HippyArray var, Promise promise) {
    super.dispatchFunction(view, functionName, var, promise);
    if (view instanceof ReplaceChildView) {
      switch (functionName){
        case "setChildSID":
          if(LogUtils.isDebug()) {
            Log.i(TAG, "dispatchFunctionBySid 2 setBoundID:" + var.getString(0));
          }
          ((ReplaceChildView) view).setBoundID(var.getString(0));
          break;
      }
    }

  }
}
