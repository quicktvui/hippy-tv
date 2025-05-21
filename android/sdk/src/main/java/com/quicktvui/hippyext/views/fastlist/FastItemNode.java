package com.quicktvui.hippyext.views.fastlist;

import android.util.Log;
import android.view.View;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;

public class FastItemNode extends RenderNode {
    final ControllerManager controllerManager;

    public FastItemNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
        super(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad);
        this.controllerManager = componentManager;

    }

    public static boolean isFastItemNode(RenderNode node){
      return node instanceof FastItemNode;
    }


    @Override
    public View createView() {
        return super.createView();
    }

    @Override
    public void updateLayout(int x, int y, int w, int h) {
        //zhaopeng 这里由于模版view在ListViwe中的位置由adapter在bind时确定，所以这时没有理由可以updateLayout
//        super.updateLayout(x, y, w, h);
    }

    @Override
    public void dispatchUIFunction(String functionName, HippyArray parameter, Promise promise) {
        if(LogUtils.isDebug()) {
          Log.d("FastList", "-- FastItemNode dispatchUIFunction functionName:" + functionName + ",parameter:" + parameter);
        }
        super.dispatchUIFunction(functionName, parameter, promise);
        //FIXME 可能由于此列表不再正常受控制，所有导致dispatchUIFunction时parameter传递的参数会有错误
       // controllerManager.dispatchUIFunction(getId(),getClassName(),functionName,parameter,promise);
    }



    @Override
    public void batchComplete() {
        super.batchComplete();
    }



    public void updateViewRecursive(){
        //zhaopeng 这里不进行update，因为模版不需要
    }



  public View getBoundView() {
      return controllerManager.findView(getId());
  }
}
