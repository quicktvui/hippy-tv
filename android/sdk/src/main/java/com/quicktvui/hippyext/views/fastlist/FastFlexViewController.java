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
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.hippy.views.view.HippyViewGroupController;


@HippyController(
        name = FastFlexViewController.CLASS_NAME
)
public class FastFlexViewController extends HippyViewGroupController implements PendingViewController {
    public static final String CLASS_NAME = "FastFlexView";

    public FastFlexViewController() {
    }


  @Override
  protected void addView(ViewGroup parentView, View view, int index) {
//    super.addView(parentView, view, index);
  }


  @HippyControllerProps(name = PendingViewController.PROP_LIST, defaultType = HippyControllerProps.ARRAY)
  public void setListData(final View lv, HippyArray list){
    if(lv instanceof FastFlexView){
      ((FastFlexView) lv).setPendingData(list, Utils.getRenderNode(lv));
    }
  }

  protected View createViewImpl(Context context) {
        //return new FastFlexView(context);
        return null;
    }

    @Override
    protected View createViewImpl(Context context, HippyMap iniProps) {
      FastFlexView fastFlexView = new FastFlexView(context);
      if(iniProps != null && iniProps.containsKey("enablePlaceholder")){
        fastFlexView.setEnablePlaceholder(iniProps.getBoolean("enablePlaceholder"));
//        Log.e("ZHAOPENG","createViewImpl flex enablePlaceholder " +iniProps.getBoolean("enablePlaceholder"));
      }
      return fastFlexView;
    }


    @Override
    public RenderNode createRenderNode(int id, HippyMap props, String className, HippyRootView hippyRootView, ControllerManager controllerManager, boolean lazy) {
        return new FastFlexNode(id, props, className, hippyRootView, controllerManager, lazy);
    }

//  @HippyControllerProps(name = "enablePlaceholder", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
//  public void setEnablePlaceholder(View view, boolean enable) {
//    if (view instanceof FastFlexView) {
//      ((FastFlexView) view).setEnablePlaceholder(enable);
//    }
//  }

  @Override
  public void setPendingData(View view, Object data, RenderNode templateNode,boolean useDiff) {
      if(LogUtils.isDebug()) {
        Log.i("FastFlexLog", "setPendingData InFlexController data:" + data + ",templateNode:" + templateNode + ",view:" + view);
      }
    if(view instanceof FastFlexView){
      ((FastFlexView) view).setPendingData(data,templateNode,useDiff);
    }
  }

  @Override
  public void setPendingData(View view, Object data, RenderNode templateNode) {
    this.setPendingData(view,data,templateNode,false);
  }

  @HippyControllerProps(name = "cachePool", defaultType = HippyControllerProps.MAP)
  public void setCacheSizeMap(View view,HippyMap map) {
      if(view instanceof FastFlexView) {
        ((FastFlexView) view).setCachePoolMap(map);
      }
  }

  @HippyControllerProps(name = "useDiff", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setUseDiff(View view, boolean useDiff) {
    if(view instanceof FastFlexView) {
      ((FastFlexView) view).setUseDiff(useDiff);
    }
  }

  @HippyControllerProps(name = "keyName", defaultType = HippyControllerProps.STRING)
  public void setUseDiff(View view, String keyName) {
    if(view instanceof FastFlexView) {
      ((FastFlexView) view).setKeyName(keyName);
    }
  }

  @HippyControllerProps(name = "firstFocusChild", defaultType = HippyControllerProps.MAP)
  public void setTouchScrollEnable(FastFlexView view, HippyMap map) {
    view.getFirstFocusHelper().setFirstFocusChildMap(map);
  }

  @Override
  public void dispatchFunction(HippyViewGroup view, String functionName, HippyArray var, Promise promise) {
    super.dispatchFunction(view, functionName, var, promise);
    if (view instanceof FastFlexView) {
      switch (functionName){
        case "updateItem":
          ((FastFlexView) view).updateItem(var.getInt(0), var.getObject(1));
          break;
      }
    }

  }

  @HippyControllerProps(name = "sharedItemStore", defaultType = HippyControllerProps.STRING)
  public void setSharedItemStore(final FastFlexView lv, String name) {
    lv.setSharedItemStore(name);
  }
}
