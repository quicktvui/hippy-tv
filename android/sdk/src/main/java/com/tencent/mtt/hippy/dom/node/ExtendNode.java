package com.tencent.mtt.hippy.dom.node;

import android.graphics.Color;
import android.util.Log;


import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyMap;

//暂未使用
 class ExtendNode extends DomNode {

  private HippyMap stateMap;
  /**
   * 普通状态
   */
  public static final int STATE_NORMAL = 0;
  /**
   * 选中状态
   */
  public static final int STATE_SELECTED = android.R.attr.state_selected;
  /**
   * 焦点状态
   */
  public static final int STATE_FOCUSED = android.R.attr.state_focused;


  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setFocusBorderColor(int color)
  {
    Log.d("ExtNode","setFocusBorderColor from node , color"+color);
    stateMap(STATE_FOCUSED).pushInt(NodeProps.BORDER_COLOR,color);
  }

  @HippyControllerProps(name = NodeProps.FOCUS_BACKGROUND_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setFocusBackgroundColor(int color)
  {
    Log.d("ExtNode","setFocusBackgroundColor from node , color"+color);
    stateMap(STATE_FOCUSED).pushInt(NodeProps.BACKGROUND_COLOR,color);
  }

  protected HippyMap stateMap(int state){
    if(stateMap == null){
      stateMap = new HippyMap();
    }
    final String key = getStateKey(state);

    if(!stateMap.containsKey(key)){
      stateMap.pushMap(key,new HippyMap());
    }
    return stateMap.getMap(key);
  }

  private static String getStateKey(int state){
    switch (state){
      case STATE_FOCUSED :
        return "focus";
      case STATE_SELECTED :
        return "select";
      default:
        return "normal";
    }
  }

  public int getBackgroundColor(int state){
    return stateMap(state).getInt(NodeProps.BACKGROUND_COLOR);
  }





}
