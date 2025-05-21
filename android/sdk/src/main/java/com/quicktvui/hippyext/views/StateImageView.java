package com.quicktvui.hippyext.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import com.quicktvui.hippyext.RenderUtil;
import com.tencent.mtt.hippy.common.HippyMap;

import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.image.HippyImageView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

import java.util.HashMap;

public class StateImageView extends HippyViewGroup {
  private boolean needUpdate = true;

  private HashMap<Integer,HippyImageView> mChildren;
  private HashMap<Integer,String> mStateUrl;

  int imgLoadDelay = 0;

  public StateImageView(Context context) {
    super(context);
  }

  static String NotNullStr(String str){
    if (str == null) {
      return "";
    }
    return str;
  }

  public void setImgLoadDelay(int imgLoadDelay) {
    this.imgLoadDelay = imgLoadDelay;
  }

  private void clearChildren(){
    removeAllViews();
    if (mChildren != null) {
      mChildren.clear();
      mChildren = null;
    }
  }

  public void setStateSrc(HippyMap  map){
    boolean noChange = false;
    int oldSize = mStateUrl != null ? mStateUrl.size() : 0;
    int newSize = map != null ? map.size() : 0;
    if (oldSize == newSize && newSize > 0) {
      //全部相同就是没有变化
      noChange = !NotNullStr(map.getString("focused")).equals(mStateUrl.get(android.R.attr.state_focused))
        && !NotNullStr(map.getString("normal")).equals(mStateUrl.get(-1))
        && !NotNullStr(map.getString("selected")).equals(mStateUrl.get(android.R.attr.state_selected));
    }
    if (!noChange) {
      clearChildren();
    }else{
      if (LogUtils.isDebug()) {
        Log.w(TAG,"return on noChanged");
      }
    }
    if (map == null || map.size() < 1) {
      this.mStateUrl = null;
    }else{
      if (mStateUrl == null) {
        mStateUrl = new HashMap<>();
      }else{
        mStateUrl.clear();
      }
      for(String key : map.keySet()){
        switch (key){
          case "focused":
            mStateUrl.put(android.R.attr.state_focused,map.getString(key));
            break;
          case "normal":
            mStateUrl.put(-1,map.getString(key));
            break;
          case "selected":
            mStateUrl.put(android.R.attr.state_selected,map.getString(key));
            break;
        }
      }
    }
    if (!noChange) {
      markUpdate();
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    layoutChildren();
    if (getChildCount() > 0 && w > 0 && h > 0) {
      this.markUpdate();
    }
  }


  public void markUpdate(){
    this.needUpdate = true;
    this.invalidate();
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (needUpdate) {
      needUpdate = false;
      crateChildrenIfNeed();
    }
  }

  void crateChildrenIfNeed(){
    if (mStateUrl != null && getWidth() > 0 && getHeight() > 0) {
      if (mChildren == null) {
        mChildren = new HashMap<>();
        for(int state : mStateUrl.keySet()){
          HippyImageView view = new HippyImageView(getContext());
          view.setFocusable(false);
          view.setFocusableInTouchMode(false);
          view.setDuplicateParentStateEnabled(true);
          //view.refreshDrawableState();
          if (imgLoadDelay > 0) {
            view.setDelayLoad(imgLoadDelay);
          }
          addView(view);
          view.setShowOnState(new int[]{state});
          RenderUtil.reLayoutView(view,0,0,getWidth(),getHeight());
          view.setUrl(mStateUrl.get(state));
          mChildren.put(state,view);
        }
        refreshDrawableState();
      }
    }

  }
  void layoutChildren() {
    for(int i = 0; i < getChildCount(); i ++){
      View v = getChildAt(i);
      RenderUtil.reLayoutView(v,0,0,getWidth(),getHeight());
    }

  }


}
