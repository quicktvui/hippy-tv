package com.quicktvui.hippyext.pm;



import android.util.Log;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;


public class PageRootNode extends WindowNode {

  boolean isFront = false;

  public PageRootNode(int mId, HippyMap mPropsToUpdate, String className, HippyRootView rootView, ControllerManager componentManager, boolean isLazyLoad) {
    super(mId, mPropsToUpdate, className, rootView, componentManager, isLazyLoad, WindowType.PAGE);
    Log.d(TAG,"create PageRootNode id = " + mId);
  }

  public PageRootNode(int id, String className, ControllerManager componentManager) {
    super(id, className, componentManager);
    Log.d(TAG,"create PageRootNode id = " + id);
  }

  public void setFront(boolean front) {
    boolean changed = front != isFront;
    isFront = front;
    if (changed) {
      if(isFront){
        onPageResume();
      }else{
        onPagePause();
      }
    }
  }






  @Override
  public PageRootNode getWindowRootNode() {
    return this;
  }


  public boolean isPageFront(){
    return isFront;
  }

  void onPageResume() {

  }

  void onPagePause() {

  }
}







