package com.quicktvui.hippyext.pm;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;

/**
 * 一个最小可显示窗口的根节点，包括jsSlotView、卡片、PageRootNode等
 */
public class WindowNode extends RenderNode {
  public static final String TAG = "DebugWindowNode";
  ViewGroup windowRootView;
  public enum WindowType {
    JS_SLOT_VIEW,
    CARD,
    PAGE
  }

  final WindowType windowType;
  public WindowType getWindowType() {
    return windowType;
  }

  public WindowNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad, WindowType windowType) {
    super(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad);
    this.windowType = windowType;
    setWindowRoot(this);
  }

  public WindowNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
    this(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad, WindowType.PAGE);
  }

  public WindowNode(int id, String className, ControllerManager componentManager) {
    super(id, className, componentManager);
    this.windowType = WindowType.PAGE;
    setWindowRoot(this);
  }

  public ViewGroup getWindowRootView() {
    return windowRootView;
  }

  public void setWindowRootView(ViewGroup windowRootView) {
    this.windowRootView = windowRootView;
  }

  @Override
  public void setDelete(boolean b) {
    super.setDelete(b);
    if(b){
      setWindowRoot(null);
    }
  }

  @Override
  public WindowNode getWindowRootNode() {
    return this;
  }

  @Override
  public void remove(int index) {
    super.remove(index);
    //pageRootView = null;
  }

  @Override
  protected void addChildToPendingList(RenderNode renderNode) {
    renderNode.setWindowRoot(this);
    super.addChildToPendingList(renderNode);
  }

  @Override
  public View createView() {
    View view =  super.createView();
    if(view instanceof ViewGroup){
      setWindowRootView((ViewGroup) view);
      if (LogUtils.isDebug()) {
        Log.i(TAG,"setPageRootView pageRootView :"+ ExtendUtil.debugViewLite(view)+",node:"+this);
      }
    }
    return view;
  }
}
