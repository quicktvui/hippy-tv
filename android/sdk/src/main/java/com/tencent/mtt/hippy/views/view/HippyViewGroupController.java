/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.views.view;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.quicktvui.hippyext.TriggerTaskManagerModule;
import com.quicktvui.base.ui.anim.FastOutLinearInInterpolator;
import com.quicktvui.base.ui.anim.LinearOutSlowInInterpolator;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.uimanager.HippyGroupController;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.image.HippyImageView;
import com.tencent.mtt.hippy.views.modal.HippyModalHostView;

import java.util.WeakHashMap;

/**
 * Created by leonardgong on 2017/11/29 0029.
 */

@HippyController(name = HippyViewGroupController.CLASS_NAME)
public class HippyViewGroupController extends HippyGroupController<HippyViewGroup> {

  public static final int FADE_IN = 0;
  public static final int FADE_OUT = 1;
  public static final int LEFT_IN = 2;
  public static final int LEFT_OUT = 3;
  public static final int RIGHT_IN = 4;
  public static final int RIGHT_OUT = 5;
  public static final int TOP_IN = 6;
  public static final int TOP_OUT = 7;
  public static final int BOTTOM_IN = 8;
  public static final int BOTTOM_OUT = 9;

  public static final int INTERPOLATOR_FAST_OUT_LINEAR = 1;
  public static final int INTERPOLATOR_OUT_SLOW_LINEAR = 2;

  public static final String CLASS_NAME = "View";

  public static WeakHashMap<View, Integer> mZIndexHash = new WeakHashMap<>();


  public static void setViewZIndex(View view, int zIndex) {
    mZIndexHash.put(view, zIndex);
  }

  public static void removeViewZIndex(View view) {
    mZIndexHash.remove(view);
  }

  public static Integer getViewZIndex(View view) {
    return mZIndexHash.get(view);
  }

  @Override
  protected View createViewImpl(Context context) {
    return new HippyViewGroup(context);
  }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
    HippyViewGroup group = null;
    if (iniProps != null) {
      if(iniProps.containsKey("showDialog")){
        group = new DialogViewGroup(context);
      }else{
        group = (HippyViewGroup) createViewImpl(context);
      }
      group.setInitProps(iniProps);
    }
    //zhaopeng 20201225 add
    if (iniProps.containsKey("focusGroup")) {
      group.setClipChildren(false);
      group.setBringFocusChildToFront(true);
    }
    if(iniProps.containsKey("markAsRoot")){
      Log.i("HippyViewGroup","MARK_AS_ROOT div:"+group);
      group.setAsRootView();
    }
    if (iniProps.containsKey(TriggerTaskManagerModule.KEY_PROP_NAME)) {
      setListenHasFocusChange(group, true);
    }
    if (iniProps.containsKey("shakeSelf")){
      group.setShakeSelf(iniProps.getBoolean("shakeSelf"));
    }
    return group;
  }

  @Override
  protected void onCreateViewByCache(View view, String type, HippyMap iniProps) {
    super.onCreateViewByCache(view, type, iniProps);
    if (view instanceof HippyViewGroup) {
      final HippyViewGroup g = (HippyViewGroup) view;
      if (iniProps != null) {
        g.setInitProps(iniProps);
      }
      //zhaopeng 20201225 add
      if (iniProps.containsKey("focusGroup")) {
        g.setClipChildren(false);
        g.setBringFocusChildToFront(true);
      }
      if (iniProps.containsKey(TriggerTaskManagerModule.KEY_PROP_NAME)) {
        setListenHasFocusChange(g, true);
      }
      if (iniProps.containsKey("shakeSelf")){
        g.setShakeSelf(iniProps.getBoolean("shakeSelf"));
      }
    }

  }

  @Override
  protected void addView(ViewGroup parentView, View view, int index) {
    super.addView(parentView, view, index);
  }

  @HippyControllerProps(name = NodeProps.SHAKE_SELF, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setShakeSelf(HippyViewGroup hippyViewGroup, boolean shakeSelf){
    hippyViewGroup.setShakeSelf(shakeSelf);
  }

  @HippyControllerProps(name = "interceptAllKeys", defaultType = HippyControllerProps.BOOLEAN)
  public void setInterceptKeyEvent(HippyViewGroup hippyViewGroup, boolean interceptKeyEvent){
    hippyViewGroup.setInterceptKeyEvent(interceptKeyEvent);
  }

  @HippyControllerProps(name = "showDialog", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = true)
  public void requestChangeShow(HippyViewGroup hippyViewGroup, boolean show){
    if(hippyViewGroup instanceof DialogViewGroup){
      ((DialogViewGroup) hippyViewGroup).requestChangeShow(show);
    }
  }

  @HippyControllerProps(name = "enableOverScrollY", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = true)
  public void setEnableOverScrollY(HippyViewGroup hippyViewGroup, boolean enable){
    hippyViewGroup.setEnableOverScrollY(enable);
  }

  @HippyControllerProps(name = "firstFocusChild", defaultType = HippyControllerProps.MAP)
  public void setFistFocusMap(HippyViewGroup view, HippyMap map) {
    view.getFirstFocusHelper().setFirstFocusChildMap(map);
  }

  @HippyControllerProps(name = "enableOverScrollX", defaultType = HippyControllerProps.BOOLEAN,defaultBoolean = true)
  public void setEnableOverScrollX(HippyViewGroup hippyViewGroup, boolean enable){
      hippyViewGroup.setEnableOverScrollX(enable);
  }

  @HippyControllerProps(name = "interceptKeys", defaultType = HippyControllerProps.ARRAY)
  public void setInterceptKeyEvent(HippyViewGroup hippyViewGroup, HippyArray interceptKeyEvent){
    hippyViewGroup.setInterceptKeyEvents(interceptKeyEvent);
  }

  @HippyControllerProps(name = "selectChildPosition", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setSelectChildPositionSimple(HippyViewGroup view, int position) {
    if (view != null) {
      view.setSelectChildPosition(position, true);
    }
  }

  @HippyControllerProps(name = "enableSelectOnFocus", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setEnableSelectOnFocus(HippyViewGroup view, boolean flag) {
    //Log.e("DebugDivSelect","call setEnableSelectOnFocus flag:"+flag);
    view.setEnableSelectOnFocus(flag);
  }

  @HippyControllerProps(name = NodeProps.OVERFLOW, defaultType = HippyControllerProps.STRING, defaultString = "visible")
  public void setOverflow(HippyViewGroup hippyViewGroup, String overflow) {
    hippyViewGroup.setOverflow(overflow);
  }

  @HippyControllerProps(name = NodeProps.BACKGROUND_IMAGE, defaultType = HippyControllerProps.STRING, defaultString = "")
  public void setBackgroundImage(HippyViewGroup hippyViewGroup, String url) {
    hippyViewGroup.setUrl(getInnerPath((HippyInstanceContext) hippyViewGroup.getContext(), url));
  }

  @Override
  protected void deleteChild(ViewGroup parentView, View childView) {
    super.deleteChild(parentView, childView);
    try {
      if (childView instanceof HippyModalHostView) {
        ((HippyModalHostView) childView).onInstanceDestroy(0);
      }
    }catch (Throwable t){}
  }


  @HippyControllerProps(name = NodeProps.BACKGROUND_SIZE, defaultType = HippyControllerProps.STRING, defaultString = "origin")
  public void setBackgroundImageSize(HippyImageView hippyImageView, String resizeModeValue) {
    if ("contain".equals(resizeModeValue)) {
      // 在保持图片宽高比的前提下缩放图片，直到宽度和高度都小于等于容器视图的尺寸
      // 这样图片完全被包裹在容器中，容器中可能留有空白
      hippyImageView.setScaleType(HippyImageView.ScaleType.CENTER_INSIDE);
    } else if ("cover".equals(resizeModeValue)) {
      // 在保持图片宽高比的前提下缩放图片，直到宽度和高度都大于等于容器视图的尺寸
      // 这样图片完全覆盖甚至超出容器，容器中不留任何空白
      hippyImageView.setScaleType(HippyImageView.ScaleType.CENTER_CROP);
    } else if ("center".equals(resizeModeValue)) {
      // 居中不拉伸
      hippyImageView.setScaleType(HippyImageView.ScaleType.CENTER);
    } else if ("origin".equals(resizeModeValue)) {
      // 不拉伸，居左上
      hippyImageView.setScaleType(HippyImageView.ScaleType.ORIGIN);
    } else {
      // stretch and other mode
      // 拉伸图片且不维持宽高比，直到宽高都刚好填满容器
      hippyImageView.setScaleType(HippyImageView.ScaleType.FIT_XY);
    }
  }

  @HippyControllerProps(name = NodeProps.BACKGROUND_POSITION_X, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0.0D)
  public void setBackgroundImagePositionX(HippyViewGroup hippyViewGroup, int positionX) {
    hippyViewGroup.setImagePositionX((int) PixelUtil.dp2px(positionX));
  }

  @HippyControllerProps(name = NodeProps.BACKGROUND_POSITION_Y, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0.0D)
  public void setBackgroundImagePositionY(HippyViewGroup hippyViewGroup, int positionY) {
    hippyViewGroup.setImagePositionY((int) PixelUtil.dp2px(positionY));
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_COLOR, defaultType = HippyControllerProps.NUMBER)
  public void setFocusBorderColor(HippyViewGroup view, int color) {
    view.setFocusBorderColor(color);
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_COLOR_STRING, defaultType = HippyControllerProps.STRING)
  public void setFocusBorderColorString(HippyViewGroup view, String color) {
    if (!TextUtils.isEmpty(color)) {
      view.setFocusBorderColor(Color.parseColor(color));
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_CORNER_RADIUS, defaultType = HippyControllerProps.NUMBER)
  public void setFocusBorderRadius(HippyViewGroup view, int corner) {
    view.setFocusBorderCorner(corner);
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_CORNER_WIDTH, defaultType = HippyControllerProps.NUMBER)
  public void setFocusBorderWidth(HippyViewGroup view, int width) {
    ((HippyViewGroup) view).setFocusBorderWidth(width);
  }

  @HippyControllerProps(name = NodeProps.FOCUS_BLACK_BORDER_ENABLE, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setFocusBlackBorderEnable(HippyViewGroup view, boolean enable) {
    ((HippyViewGroup) view).setBlackRectEnable(enable);
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BACKGROUND_COLOR, defaultType = HippyControllerProps.NUMBER)
  public void setFocusBackGroundColor(HippyViewGroup view, int color) {
    view.setFocusBackGroundColor(color);
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.SELECT_BACKGROUND_COLOR, defaultType = HippyControllerProps.NUMBER)
  public void setSelectBackGroundColor(HippyViewGroup view, int color) {
    view.setSelectBackGroundColor(color);
  }


  //此处vue是怎么传值的
  @HippyControllerProps(name = "focusSearchTarget", defaultType = HippyControllerProps.MAP)
  public void setFocusSearchTarget(HippyViewGroup view, HippyMap hippyMap) {
    Log.d("ViewGroupController", "ViewGroupController focusSearchTarget view:" + view + ",map:" + hippyMap);
    view.setFocusSearchTarget(hippyMap);
  }

  /*** zhaopeng  add  20201117 **/
  @HippyControllerProps(name = "focusMemory", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void enableFocusMemory(HippyViewGroup view, boolean enable) {
    view.enableFocusMemory(enable);
  }

  //此处vue是怎么传值的
  @HippyControllerProps(name = "gradientBackground", defaultType = HippyControllerProps.MAP)
  public void setGradientDrawableBackground(HippyViewGroup view, HippyMap hippyMap) {
    view.setGradientDrawable(hippyMap);
  }



  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_STYLE, defaultType = HippyControllerProps.STRING, defaultString = "solid")
  public void setFocusBorderStyle(HippyViewGroup view, String style) {
    if (view != null) {
      ((HippyViewGroup) view).setFocusBorderEnable(!"none".equals(style));
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_ENABLE, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setFocusBorderEnable(HippyViewGroup view, boolean enable) {
    if (view != null) {
      ((HippyViewGroup) view).setFocusBorderEnable(enable);
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = "focusScrollTarget", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setFocusScrollTarget(HippyViewGroup view, boolean enable) {
    if (view != null) {
      view.setFocusScrollTarget(enable);
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = "firstFocusTarget", defaultType = HippyControllerProps.STRING)
  public void setFirstFocusTargetName(HippyViewGroup view, String name) {
    if (view != null) {
      ((HippyViewGroup) view).setFirstFocusTargetName(name);
    }
  }


  /**
   * touch/click
   **/
  @HippyControllerProps(name = "useAdvancedFocusSearch", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setUseAdvancedFocusSearch(HippyViewGroup view, boolean enable) {
    if (view != null) {
      ((HippyViewGroup) view).setUseAdvancedFocusSearch(enable);
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = "listenHasFocusChange", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setListenHasFocusChange(HippyViewGroup view, boolean enable) {
    if (view != null) {
      ((HippyViewGroup) view).setListenGlobalFocusChange(enable);
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = "triggerKeyCodeNum", defaultType = HippyControllerProps.NUMBER)
  public void setTriggerKeyEvent(HippyViewGroup view, int code) {
    view.setTriggerKeyEvent(code);
  }


  /**
   * touch/click
   **/
  @HippyControllerProps(name = "triggerKeyCode", defaultType = HippyControllerProps.STRING)
  public void setTriggerKeyEvent(HippyViewGroup view, String code) {
    int key = -1;
    switch (code) {
      case "left":
        key = KeyEvent.KEYCODE_DPAD_LEFT;
        break;
      case "right":
        key = KeyEvent.KEYCODE_DPAD_RIGHT;
        break;
      case "up":
        key = KeyEvent.KEYCODE_DPAD_UP;
        break;
      case "down":
        key = KeyEvent.KEYCODE_DPAD_DOWN;
        break;
      case "enter":
        key = KeyEvent.KEYCODE_DPAD_CENTER;
        break;
    }
    view.setTriggerKeyEvent(key);
  }

  @HippyControllerProps(name = "useLayoutAnimation", defaultType = HippyControllerProps.ARRAY)
  public void useLayoutAnimation(View view, HippyArray array) {
    if (!(view instanceof ViewGroup)) return;
    if (array == null || array.size() == 0) return;
    int type = array.size() >= 1 ? array.getInt(0) : -1;
    if (type == -1) return;
    int interpolator = array.size() >= 2 ? array.getInt(1) : -1;
    int duration = array.size() >= 3 ? array.getInt(2) : -1;

    int anim = -1;
    switch (type) {
      case FADE_IN:
        anim = R.anim.fade_in;
        break;
      case FADE_OUT:
        anim = R.anim.fade_out;
        break;
      case LEFT_IN:
        anim = R.anim.left_in;
        break;
      case LEFT_OUT:
        anim = R.anim.left_out;
        break;
      case RIGHT_IN:
        anim = R.anim.right_in;
        break;
      case RIGHT_OUT:
        anim = R.anim.right_out;
        break;
      case TOP_IN:
        anim = R.anim.top_in;
        break;
      case TOP_OUT:
        anim = R.anim.top_out;
        break;
      case BOTTOM_IN:
        anim = R.anim.bottom_in;
        break;
      case BOTTOM_OUT:
        anim = R.anim.bottom_out;
        break;
    }
    if (anim == -1) return;

    Animation animation = AnimationUtils.loadAnimation(view.getContext(), anim);

    switch (interpolator) {
      case INTERPOLATOR_FAST_OUT_LINEAR:
        animation.setInterpolator(new FastOutLinearInInterpolator());
        break;
      case INTERPOLATOR_OUT_SLOW_LINEAR:
        animation.setInterpolator(new LinearOutSlowInInterpolator());
        break;
    }

    if (duration > 0) {
      animation.setDuration(duration);
    }

    LayoutAnimationController lac = new LayoutAnimationController(animation);
    ((ViewGroup) view).setLayoutAnimation(lac);
  }

  @Override
  public void dispatchFunction(HippyViewGroup view, String functionName, HippyArray var) {
    super.dispatchFunction(view, functionName, var);

    LogUtils.d(FocusDispatchView.TAG, "viewGorup dispatchFunction:" + functionName + ",data:" + var);
    switch (functionName) {
      case "setDescendantFocusability":
        if (view != null) {
          if (var.size() > 0) {
            int focusAbility = var.getInt(0);
            Log.d("hippy", "setDescendantFocusability :" + focusAbility);
            ((ViewGroup) view).setDescendantFocusability(focusAbility);
          }
        }
        break;
      case "changeDescendantFocusability":
        if (view != null) {
          if (var.size() > 0) {
            String focusAbility = var.getString(0);

            int focusAbilityInt = ViewGroup.FOCUS_AFTER_DESCENDANTS;
            switch (focusAbility) {
              case "beforeDescendants":
                ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                break;
              case "blockDescendants":
                ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                break;
              default:
                ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                break;
            }
            LogUtils.d(FocusDispatchView.TAG, "changeDescendantFocusability request focusAbility:" + focusAbility + " ,result :" + focusAbilityInt + ", view:" + view);
          }
        }
        break;
      case "requestChildFocusAtIndex":
        int p = var.getInt(0);
        if (p > -1 && p < (view != null ? view.getChildCount() : 0)) {
          final View v = view.getChildAt(p);
          Log.e("hippy", "requestChildFocusAtIndex position:" + p + ",view:" + v);
          if (v != null) {
            v.requestFocus();
          }
        } else {
          Log.e("hippy", "requestChildFocusAtIndex error :target view null, position:" + p);
        }
        break;
      case "clearMemoryFocused":
        view.clearMemoryFocused();
        break;
      case "showDialog":
        if (view instanceof DialogViewGroup) {
          ((DialogViewGroup) view).requestChangeShow(var.getBoolean(0));
        }
        break;
      case "smoothShowFade":
        if (view != null) {
          view.smoothShowFade(var.getInt(0), var.getBoolean(1), var.getBoolean(2),
            var.getInt(3),var.getBoolean(4));
        }
        break;
    }
  }

  @HippyControllerProps(name = NodeProps.CLIP_BOUNDS_OUTSET_RECT, defaultType = HippyControllerProps.MAP)
  public void setClipRectOutset(HippyViewGroup view, HippyMap hippyMap) {
    if (view != null) {
      view.setClipOutset(hippyMap.getInt("left"), hippyMap.getInt("top"), hippyMap.getInt("right"), hippyMap.getInt("bottom"));
    }
  }

  @HippyControllerProps(name = NodeProps.CLIP_BOUNDS_OUTSET, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setClipRectOutsetAll(HippyViewGroup view, int outSet) {
    if (view != null) {
      view.setClipOutset(outSet, outSet, outSet, outSet);
    }
  }

  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_TYPE, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setFocusBorderType(HippyViewGroup view, int type) {
    if(view != null){
      view.setFocusBorderType(type);
    }
  }

  @HippyControllerProps(name = NodeProps.ENABLE_MOUSE, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
  public void setEnableMouse(HippyViewGroup view, boolean enableMouse) {
    if(view != null){
      //Log.d("roll", "setEnableMouse: -------------->激活空鼠功能"+view + " 当前viewid--->" + view.getId() + " 是否开启--->" + enableMouse);
      view.setEnableMouse(enableMouse);
    }
  }
}
