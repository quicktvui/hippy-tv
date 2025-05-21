package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;


@HippyController(
        name = FastItemViewController.CLASS_NAME
)
public class FastItemViewController extends HippyViewController<FastItemView> {
    public static final String CLASS_NAME = "FastItemView";
    public static final String TAG = "FastItemViewLog";

    public FastItemViewController() {
    }

  @Override
  protected void addView(ViewGroup parentView, View view, int index) {
    super.addView(parentView, view, index);
  }


  protected View createViewImpl(Context context) {
        return new FastItemView(context);
    }

    @Override
    protected View createViewImpl(Context context, HippyMap iniProps) {
//      if(iniProps != null) {
//        if (iniProps.containsKey("type")) {
//          Log.d(CLASS_NAME, "createViewImpl hasType:" + iniProps.get("type"));
//        } else {
//          //throw new IllegalArgumentException("tv-item必须指定type属性");
//          Log.e(CLASS_NAME, "createViewImpl eror hasType null");
//        }
//      }
        return super.createViewImpl(context, iniProps);
    }


    @Override
    public RenderNode createRenderNode(int id, HippyMap props, String className, HippyRootView hippyRootView, ControllerManager controllerManager, boolean lazy) {
        return new FastItemNode(id, props, className, hippyRootView, controllerManager, lazy);
    }


  @HippyControllerProps(name = "focusScrollTarget", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = false)
  public void setFocusScrollTarget(FastItemView view, boolean enable) {
    if(view != null){
      view.setFocusScrollTarget(enable);
    }
  }

  @Override
  public void dispatchFunction(FastItemView view, String functionName, HippyArray params, Promise promise) {
    super.dispatchFunction(view, functionName, params, promise);
    switch (functionName) {
      case "dispatchItemFunctionWithPromise":
        view.dispatchItemFunction(params,promise);
        break;
    }
  }

  @Override
  public void dispatchFunction(FastItemView view, String functionName, HippyArray var) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "dispatchFunction functionName:" + functionName + ",var :" + var);
    }
    super.dispatchFunction(view,functionName,var);
    switch (functionName) {
      case "updateInstance":
        view.updateInstance(var.getInt(0),var.getObject(1));
        break;
      case "updateItem":
        view.updateItemDataInList(var.getInt(0),var.getObject(1));
        break;
      case "batch":
        view.batchUpdate(var.getInt(0));
        break;
      case "updateItemProps":
        view.updateItemProps(var.getString(0), var.getInt(1),var.getMap(2),var.getBoolean(3));
        break;
      case "dispatchItemFunction":
        view.dispatchItemFunction(var,null);
        break;


    }
  }


}
