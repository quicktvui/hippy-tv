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
package com.tencent.mtt.hippy.dom.node;

import java.util.Arrays;
import java.util.HashSet;

import com.tencent.mtt.hippy.common.HippyMap;

import android.graphics.Color;

@SuppressWarnings({"deprecation", "unused"})
public class NodeProps {

  public static final String ALIGN_ITEMS = "alignItems";
  public static final String ALIGN_SELF = "alignSelf";
  public static final String OVERFLOW = "overflow";
  public static final String BOTTOM = "bottom";
  public static final String COLLAPSABLE = "collapsable";
  public static final String FLEX = "flex";
  public static final String FLEX_GROW = "flexGrow";
  public static final String FLEX_SHRINK = "flexShrink";
  public static final String FLEX_BASIS = "flexBasis";
  public static final String DIRECTION = "direction";
  public static final String FLEX_DIRECTION = "flexDirection";
  public static final String FLEX_WRAP = "flexWrap";
  public static final String HEIGHT = "height";
  public static final String JUSTIFY_CONTENT = "justifyContent";
  public static final String LEFT = "left";
  public static final String DISPLAY = "display";


  public static final String MARGIN = "margin";
  public static final String MARGIN_VERTICAL = "marginVertical";
  public static final String MARGIN_HORIZONTAL = "marginHorizontal";
  public static final String MARGIN_LEFT = "marginLeft";
  public static final String MARGIN_RIGHT = "marginRight";
  public static final String MARGIN_TOP = "marginTop";
  public static final String MARGIN_BOTTOM = "marginBottom";

  public static final String PADDING = "padding";
  public static final String PADDING_VERTICAL = "paddingVertical";
  public static final String PADDING_HORIZONTAL = "paddingHorizontal";
  public static final String PADDING_LEFT = "paddingLeft";
  public static final String PADDING_RIGHT = "paddingRight";
  public static final String PADDING_TOP = "paddingTop";
  public static final String PADDING_BOTTOM = "paddingBottom";

  public static final String POSITION = "position";
  public static final String RIGHT = "right";
  public static final String TOP = "top";
  public static final String WIDTH = "width";

  public static final String MIN_WIDTH = "minWidth";
  public static final String MAX_WIDTH = "maxWidth";
  public static final String MIN_HEIGHT = "minHeight";
  public static final String MAX_HEIGHT = "maxHeight";

  public static final String BORDER_WIDTH = "borderWidth";
  public static final String BORDER_LEFT_WIDTH = "borderLeftWidth";
  public static final String BORDER_TOP_WIDTH = "borderTopWidth";
  public static final String BORDER_RIGHT_WIDTH = "borderRightWidth";
  public static final String BORDER_BOTTOM_WIDTH = "borderBottomWidth";

  public static final String BORDER_COLOR = "borderColor";
  public static final String BORDER_LEFT_COLOR = "borderLeftColor";
  public static final String BORDER_TOP_COLOR = "borderTopColor";
  public static final String BORDER_RIGHT_COLOR = "borderRightColor";
  public static final String BORDER_BOTTOM_COLOR = "borderBottomColor";
  public static final String BORDER_STYLES = "borderStyle";

  public static final String SHADOW_COLOR = "shadowColor";
  public static final String SHADOW_OFFSET = "shadowOffset";
  public static final String SHADOW_OFFSET_X = "shadowOffsetX";
  public static final String SHADOW_OFFSET_Y = "shadowOffsetY";
  public static final String SHADOW_OPACITY = "shadowOpacity";
  public static final String SHADOW_RADIUS = "shadowRadius";
  public static final String SHADOW_SPREAD = "shadowSpread";

  public static final String LINEAR_GRADIENT = "linearGradient";

  //View props

  public static final String ENABLED = "enabled";
  public static final String OPACITY = "opacity";
  public static final String BACKGROUND_COLOR = "backgroundColor";
  public static final String BACKGROUND_COLORS = "backgroundColors";
  public static final String COLORS = "colors";
  public static final String COLOR = "color";
  public static final String BACKGROUND_IMAGE = "backgroundImage";
  public static final String BACKGROUND_POSITION_X = "backgroundPositionX";
  public static final String BACKGROUND_POSITION_Y = "backgroundPositionY";
  public static final String BACKGROUND_SIZE = "backgroundSize";
  public static final String FONT_SIZE = "fontSize";
  public static final String LETTER_SPACING = "letterSpacing";
  public static final String FONT_WEIGHT = "fontWeight";
  public static final String FONT_STYLE = "fontStyle";
  public static final String FONT_FAMILY = "fontFamily";
  public static final String LINE_HEIGHT = "lineHeight";
  public static final String NUMBER_OF_LINES = "numberOfLines";
  public static final String ELLIPSIZE_MODE = "ellipsizeMode";
  public static final String ON = "on";
  public static final String RESIZE_MODE = "resizeMode";
  public static final String RESIZE_METHOD = "resizeMethod";
  public static final String GAUSSIAN_BLUR = "gaussianBlur";
  public static final String TEXT_ALIGN = "textAlign";
  public static final String TEXT_ALIGN_VERTICAL = "textAlignVertical";
  public static final String TEXT_DECORATION_LINE = "textDecorationLine";
  public static final String ON_CLICK = "onClick";
  public static final String ON_LONG_CLICK = "onLongClick";
  public static final String ON_PRESS_IN = "onPressIn";
  public static final String ON_PRESS_OUT = "onPressOut";
  public static final String ON_TOUCH_DOWN = "onTouchDown";
  public static final String ON_TOUCH_MOVE = "onTouchMove";
  public static final String ON_TOUCH_END = "onTouchEnd";
  public static final String ON_TOUCH_CANCEL = "onTouchCancel";
  public static final String ON_INTERCEPT_TOUCH_EVENT = "onInterceptTouchEvent";
  public static final String ON_INTERCEPT_PULL_UP_EVENT = "onInterceptPullUpEvent";
  public static final String ON_ATTACHED_TO_WINDOW = "onAttachedToWindow";
  public static final String ON_DETACHED_FROM_WINDOW = "onDetachedFromWindow";

  public static final String BORDER_RADIUS = "borderRadius";
  public static final String BORDER_TOP_LEFT_RADIUS = "borderTopLeftRadius";
  public static final String BORDER_TOP_RIGHT_RADIUS = "borderTopRightRadius";
  public static final String BORDER_BOTTOM_LEFT_RADIUS = "borderBottomLeftRadius";
  public static final String BORDER_BOTTOM_RIGHT_RADIUS = "borderBottomRightRadius";

  public static final String TRANSFORM = "transform";
  public static final String Z_INDEX = "zIndex";

  public static final float FONT_SIZE_SP = 14.0f;

  public static final String VIEW_CLASS_NAME = "View";
  public static final String TEXT_CLASS_NAME = "Text";
  public static final String IMAGE_CLASS_NAME = "Image";

  public static final String STYLE = "style";
  public static final String PROPS = "props";
  public static final String ROOT_NODE = "RootNode";
  public static final String CUSTOM_PROP = "customProp";
  public static final String CUSTOM_PROP_ISGIF = "isGif";
  public static final String CUSTOM_PROP_IMAGE_TYPE = "imageType";

  public static final String PROP_ACCESSIBILITY_LABEL = "accessibilityLabel";
  public static final String FOCUSABLE = "focusable";
  public static final String NEXT_FOCUS_DOWN_ID = "nextFocusDownId";
  public static final String NEXT_FOCUS_UP_ID = "nextFocusUpId";
  public static final String NEXT_FOCUS_LEFT_ID = "nextFocusLeftId";
  public static final String NEXT_FOCUS_RIGHT_ID = "nextFocusRightId";
  public static final String REQUEST_FOCUS = "requestFocus";

  public static final String VISIBLE = "visible";
  public static final String REPEAT_COUNT = "repeatCount";
  public static final String TEXT_GRAVITY_NUMBER = "gravityNumber";
  public static final String ATTRIBUTES	= "attributes";
  public static final String				FOCUS_VIEW					= "focusView";
  public static final String				FOCUS_IN_TOUCH_MODE					= "focusableInTouchMode";

  /**-----------hippy4tv zhaopeng start --------------------*/
  public static final String ON_FOCUS_CHANGE = "onFocus";
  public static final String FOCUS_SCALE = "focusScale";
  public static final String CLIP_CHILDREN = "clipChildren";
  public static final String CLIP_TO_PADDING = "clipToPadding";
  public static final String CLIP_BOUNDS = "clipBounds";
  public static final String CLIP_BOUNDS_OUTSET_RECT = "clipOutsetRect";
  public static final String CLIP_BOUNDS_OUTSET = "clipOutset";
  public static final String LAYOUT_PADDING = "padding";
  public static final String DESCENDANT_FOCUSABILITY= "descendantFocusability";
  public static final String NEXT_FOCUS_NAME ="nextFocusName";
  public static final String LIST_VIEW_OVER_SCROLL_LENGTH_START = "overScrollStart";
  public static final String LIST_VIEW_OVER_SCROLL_LENGTH_END = "overScrollEnd";
  public static final String DUPLICATE_PARENT_STATE = "duplicateParentState";

  public static final String NEXT_FOCUS_DOWN_SID = "nextFocusDownSID";
  public static final String NEXT_FOCUS_UP_SID = "nextFocusUpSID";
  public static final String NEXT_FOCUS_LEFT_SID = "nextFocusLeftSID";
  public static final String NEXT_FOCUS_RIGHT_SID = "nextFocusRightSID";
  public static final String NEXT_FOCUS_SID = "nextFocusSID";

  public static final String NAME = "name";

  public static final String TEXT = "text";
  public static final String TEXT_GRAVITY = "gravity";
  public static final String TEXT_ELLIPSIZE = "ellipsize";
  public static final String TEXT_SAVE_FROM_PARENT_ENABLED = "saveFromParentEnabled";
  public static final String TEXT_LINES = "lines";
  public static final String TEXT_MAX_LINES = "maxLines";
  public static final String TEXT_COLOR = "textColor";
  public static final String TEXT_SIZE = "textSize";
  public static final String TEXT_SELECT = "select";
  public static final String TEXT_LINE_HEIGHT = "lineHeight";
  public static final String TEXT_ALIGNMENT = "textAlignment";

  public static final String PROGRESS = "progress";
  public static final String SECOND_PROGRESS = "secondProgress";
  public static final String PROGRESS_MAX = "maxProgress";
  public static final String PROGRESS_COLOR = "progressColor";
  public static final String PROGRESS_ORIENTATION = "orientation";
  public static final String FOCUS_BORDER_TYPE = "focusBorderType";//自定义边框type 0:默认边框
  public static final String FOCUS_BORDER_STYLE = "focusBorderStyle";
  public static final String FOCUS_BORDER_ENABLE = "enableFocusBorder";
  public static final String FOCUS_BORDER_COLOR = "focusBorderColor";
  public static final String FOCUS_BORDER_COLOR_STRING = "focusBorderColorString";
  public static final String FOCUS_BORDER_CORNER_RADIUS = "focusBorderRadius";
  public static final String FOCUS_BORDER_CORNER_WIDTH = "focusBorderWidth";
  public static final String FOCUS_BLACK_BORDER_ENABLE = "enableBlackBorder";//黑色内边框开关
  public static final String FOCUS_BACKGROUND_COLOR = "focusBackgroundColor";
  public static final String SELECT_BACKGROUND_COLOR = "selectBackgroundColor";
  public static final String FOCUS_COLOR = "focusColor";
  public static final String SELECT_COLOR = "selectColor";
  public static final String BLOCK_FOCUS_DIRECTIONS = "blockFocusDirections";
  public static final String COLOR_FILTER = "colorFilter";
  public static final String ENABLE_MOUSE = "enableMouse";//是否激活鼠标联动

  /**-----------hippy4tv zhaopeng end--------------------*/
  public static final String SHAKE_SELF = "shakeSelf";



  private static final HashSet<String> JUST_LAYOUT_PROPS = new HashSet<>(
      Arrays.asList(ALIGN_SELF, ALIGN_ITEMS, COLLAPSABLE, FLEX, FLEX_DIRECTION, FLEX_WRAP,
          JUSTIFY_CONTENT,

          /*
           * position
           */
          POSITION, RIGHT, TOP, BOTTOM, LEFT,

          /*
           * dimensions
           */
          WIDTH, HEIGHT, MIN_WIDTH, MAX_WIDTH, MIN_HEIGHT, MAX_HEIGHT,

          /*
           * margins
           */
          MARGIN, MARGIN_VERTICAL, MARGIN_HORIZONTAL, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP,
          MARGIN_BOTTOM,

          /*
           * paddings
           */
          PADDING, PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_LEFT, PADDING_RIGHT, PADDING_TOP,
          PADDING_BOTTOM));
  private static final HashSet<String> TOUCH_EVENT_PROPS = new HashSet<>(
      Arrays.asList(ON_CLICK, ON_LONG_CLICK, ON_PRESS_IN, ON_PRESS_OUT, ON_TOUCH_CANCEL,
          ON_TOUCH_DOWN, ON_TOUCH_END, ON_TOUCH_MOVE));

  public static boolean isTouchEventProp(String prop) {
    return TOUCH_EVENT_PROPS.contains(prop);
  }

  public static boolean isJustLayout(HippyMap map, String prop) {
    if (JUST_LAYOUT_PROPS.contains(prop)) {
      return true;
    }

    switch (prop) {
      case OPACITY:
        return map.isNull(OPACITY) || map.getDouble(OPACITY) == 1d;
      case BORDER_RADIUS:
        if (map.containsKey(BACKGROUND_COLOR)
            && map.getInt(BACKGROUND_COLOR) != Color.TRANSPARENT) {
          return false;
        }
        //noinspection RedundantIfStatement
        if (map.containsKey(BORDER_WIDTH) && !map.isNull(BORDER_WIDTH)
            && map.getDouble(BORDER_WIDTH) != 0d) {
          return false;
        }
        return true;
      case BORDER_LEFT_COLOR:
        return map.getInt(BORDER_LEFT_COLOR) == Color.TRANSPARENT;
      case BORDER_RIGHT_COLOR:
        return map.getInt(BORDER_RIGHT_COLOR) == Color.TRANSPARENT;
      case BORDER_TOP_COLOR:
        return map.getInt(BORDER_TOP_COLOR) == Color.TRANSPARENT;
      case BORDER_BOTTOM_COLOR:
        return map.getInt(BORDER_BOTTOM_COLOR) == Color.TRANSPARENT;
      case BORDER_WIDTH:
        return map.isNull(BORDER_WIDTH) || map.getDouble(BORDER_WIDTH) == 0d;
      case BORDER_LEFT_WIDTH:
        return map.isNull(BORDER_LEFT_WIDTH) || map.getDouble(BORDER_LEFT_WIDTH) == 0d;
      case BORDER_TOP_WIDTH:
        return map.isNull(BORDER_TOP_WIDTH) || map.getDouble(BORDER_TOP_WIDTH) == 0d;
      case BORDER_RIGHT_WIDTH:
        return map.isNull(BORDER_RIGHT_WIDTH) || map.getDouble(BORDER_RIGHT_WIDTH) == 0d;
      case BORDER_BOTTOM_WIDTH:
        return map.isNull(BORDER_BOTTOM_WIDTH) || map.getDouble(BORDER_BOTTOM_WIDTH) == 0d;
      case OVERFLOW:
        return map.isNull(OVERFLOW) || VISIBLE.equals(map.getString(OVERFLOW));
      default:
        return false;
    }
  }
}
