package com.quicktvui.hippyext.views;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.utils.PixelUtil;

@HippyController(name = TVButtonViewController.CLASS_NAME)
public class TVButtonViewController extends HippyViewController<TVButtonView> {

    public static final String	CLASS_NAME						= "TextButtonView";
    @Override
    protected View createViewImpl(Context context) {
        return null;
    }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
      return new TVButtonView(context,iniProps);
  }

  @HippyControllerProps(name = "markWidth", defaultType = HippyControllerProps.NUMBER)
    public void setMarkWidth(TVButtonView tv, int width){
        tv.setMarkWidth(width);
    }

    @HippyControllerProps(name = "markHeight", defaultType = HippyControllerProps.NUMBER)
    public void setMarkHeight(TVButtonView tv, int markHeight) {
        tv.setMarkHeight(markHeight);
    }

    @HippyControllerProps(name = "markMargin", defaultType = HippyControllerProps.NUMBER)
    public void setMarkMargin(TVButtonView tv, int margin) {
        tv.setMarkMargin(margin);
    }

    @HippyControllerProps(name = "enableMark", defaultType = HippyControllerProps.BOOLEAN)
    public void setEnableMark(TVButtonView tv, boolean enableMark) {
        tv.setEnableMark(enableMark);
    }

    @HippyControllerProps(name = "markRounder", defaultType = HippyControllerProps.NUMBER)
    public void setMarkRounder(TVButtonView tv, int rounder) {
       tv.setMarkRounder(rounder);
    }

  @HippyControllerProps(name = "textSize", defaultType = HippyControllerProps.NUMBER)
  public void setTextSize(TVButtonView tv, int size) {
    tv.setTextSize((int) PixelUtil.sp2px(size));
  }

    @HippyControllerProps(name = "markColor", defaultType = HippyControllerProps.STRING)
    public void setMarkColor(TVButtonView tv, String color){
        tv.setMarkColor(Color.parseColor(color));
    }

    @HippyControllerProps(name = "text", defaultType = HippyControllerProps.STRING)
    public void setText(TVButtonView tv, String text){
        tv.setText(text);
    }

    @HippyControllerProps(name = "stateTextColor", defaultType = HippyControllerProps.MAP)
    public void setMarkColor(TVButtonView tv, HippyMap map){
        tv.setColorStateListMap(map);
    }

    @HippyControllerProps(name = "stateBackground", defaultType = HippyControllerProps.MAP)
    public void setStateDrawable(TVButtonView tv, HippyMap map) {
        tv.setStateDrawableMap(map);
    }

    @HippyControllerProps(name = "sateBackgroundPadding", defaultType = HippyControllerProps.ARRAY)
    public void setBackgroundPadding(TVButtonView tv, HippyArray array) {
        tv.setBackgroundPadding(new int[]{array.getInt(0),array.getInt(1)});
    }


  @Override
  protected StyleNode createNode(boolean isVirtual) {
    return new TextButtonNode();
  }
}
