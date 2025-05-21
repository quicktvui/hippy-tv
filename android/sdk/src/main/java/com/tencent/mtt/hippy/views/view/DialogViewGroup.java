package com.tencent.mtt.hippy.views.view;

import android.content.Context;
import android.util.Log;
import android.view.View;

public class DialogViewGroup extends HippyViewGroup implements WindowRoot{

  private HippyViewGroup rootView;
  private final String TAG = "debugDialogDiv";

  public DialogViewGroup(Context context) {
    super(context);
    super.setVisibility(View.INVISIBLE);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    View v = findPageRootView(this);
    if(v instanceof HippyViewGroup){
      rootView = (HippyViewGroup) v;
    }
    Log.i("DebugPage","onAttachedToWindow,rootView:"+rootView  );
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    rootView = null;
//    af.re
//    af.set
  }


//  @Override
//  public void setVisibility(int visibility) {
//    boolean changed = visibility != getVisibility();
//    boolean isShow = visibility == View.VISIBLE;
//    if (rootView != null && changed) {
//      rootView.beforeDialogDivVisibleChange(this,isShow);
//    }
//    super.setVisibility(visibility);
//    Log.i(TAG,"setVisibility visibility:"+visibility+",this:"+ ExtendUtil.debugViewLite(this));
////    requestChangeShow(visibility == View.VISIBLE);
//    if(changed){
//      if (isShow) {
//        onShow();
//      } else {
//        onHide();
//      }
//      if (rootView != null) {
//        rootView.notifyDialogDivVisibleChange(this,isShow);
//      }
//    }
//  }

  public void requestChangeShow(boolean show) {
    int visibility = getVisibility();
    int newVisibility = show ? View.VISIBLE : View.INVISIBLE;
    if(visibility == newVisibility){
      Log.e(TAG,"requestChangeShow return on same visibility:"+visibility );
      return;
    }

    if (rootView != null) {
      rootView.beforeDialogDivVisibleChange(this,show);
    }
//    if(show) {
//      resetChildren(this);
//    }
    setVisibility(newVisibility);
    Log.i(TAG,"requestChangeShow show:"+show+",rootView:"+rootView  );
    if (show) {
      onShow();
    } else {
      onHide();
    }
    if (rootView != null) {
      rootView.notifyDialogDivVisibleChange(DialogViewGroup.this,show);
    }
  }

  protected void onHide() {

  }

  protected void onShow() {

  }

//  private void resetChildren(View view) {
//    if (view == null) {
//      return;
//    }
//    if (view instanceof HippyViewGroup) {
//      HippyViewGroup viewGroup = (HippyViewGroup) view;
//      viewGroup.overScrollImmediately = true;
//      mScroller = null;
//      viewGroup.scrollTo(0,0);
//    }
//    if(view instanceof ViewGroup){
//      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
//        View child = ((ViewGroup) view).getChildAt(i);
//        resetChildren(child);
//      }
//    }
//  }




}
