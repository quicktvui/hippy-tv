package com.quicktvui.hippyext.views;

import android.util.Log;

import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.utils.LogUtils;

public class TextButtonNode extends StyleNode {

  public static final String TAG = "TVButtonTest";
  public TextButtonNode() {
    super();
  }

  @Override
  public void updateProps(HippyMap props) {
    if(LogUtils.isDebug() && props != null) {
      Log.d(TAG, "updateProps props");
      for(String s : props.keySet()){
        Log.d(TAG, "updateProps prop："+s+",value:"+props.get(s));
      }
    }
    super.updateProps(props);
  }


  @Override
  public boolean shouldUpdateLayout(float x, float y) {
    return super.shouldUpdateLayout(x, y);
  }

  public int getTextWidth() {
    return (int) (getStyleWidth() - getPadding(0) - getPadding(2));
  }

  public void setTextWidth(int width) {
    if(LogUtils.isDebug()){
      Log.e(TAG,"setTextWidth width:+"+width+", total width :"+(width + getPadding(0) + getPadding(2)));
    }
    setStyleWidth(width + getPadding(0) + getPadding(2));
  }

  public int getPaddingWidth() {
    return (int) (getPadding(0) + getPadding(2));
  }
}
