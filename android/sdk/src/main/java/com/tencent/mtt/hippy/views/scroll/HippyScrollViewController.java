package com.tencent.mtt.hippy.views.scroll;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyGroupController;
import com.tencent.mtt.hippy.utils.PixelUtil;

@SuppressWarnings({"deprecation", "unused", "rawtypes"})
@HippyController(name = HippyScrollViewController.CLASS_NAME)
public class HippyScrollViewController<T extends ViewGroup & HippyScrollView> extends
    HippyGroupController {

  protected static final String SCROLL_TO = "scrollTo";
  private static final String SCROLL_TO_WITHOPTIONS = "scrollToWithOptions";

  public static final String CLASS_NAME = "ScrollView";


  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {

    if (iniProps != null && iniProps.containsKey("horizontal") && iniProps
        .getBoolean("horizontal")) {
      return new HippyHorizontalScrollView(context);
    } else {
      return new HippyVerticalScrollView(context);
    }

    //        return super.createViewImpl(context, iniProps);
  }

  @Override
  protected View createViewImpl(Context context) {
    return null;
    //        return new HippyScrollView(context);
  }

  @HippyControllerProps(name = "scrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setScrollEnabled(HippyScrollView view, boolean flag) {
    view.setScrollEnabled(flag);
  }

  @HippyControllerProps(name = "showScrollIndicator", defaultType = HippyControllerProps.BOOLEAN)
  public void setShowScrollIndicator(HippyScrollView view, boolean flag) {
    view.showScrollIndicator(flag);
  }

  @HippyControllerProps(name = "onScrollEnable", defaultType = HippyControllerProps.BOOLEAN)
  public void setScrollEventEnable(HippyScrollView view, boolean flag) {
    view.setScrollEventEnable(flag);
  }

  @HippyControllerProps(name = "onScrollBeginDrag", defaultType = HippyControllerProps.BOOLEAN)
  public void setScrollBeginDragEventEnable(HippyScrollView view, boolean flag) {
    view.setScrollBeginDragEventEnable(flag);
  }

  @HippyControllerProps(name = "onScrollEndDrag", defaultType = HippyControllerProps.BOOLEAN)
  public void setScrollEndDragEventEnable(HippyScrollView view, boolean flag) {
    view.setScrollEndDragEventEnable(flag);
  }

  @HippyControllerProps(name = "onMomentumScrollBegin", defaultType = HippyControllerProps.BOOLEAN)
  public void setMomentumScrollBeginEventEnable(HippyScrollView view, boolean flag) {
    view.setMomentumScrollBeginEventEnable(flag);
  }

  @HippyControllerProps(name = "onMomentumScrollEnd", defaultType = HippyControllerProps.BOOLEAN)
  public void setMomentumScrollEndEventEnable(HippyScrollView view, boolean flag) {
    view.setMomentumScrollEndEventEnable(flag);
  }

  @HippyControllerProps(name = "flingEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setFlingEnabled(HippyScrollView view, boolean flag) {
    view.setFlingEnabled(flag);
  }

  @HippyControllerProps(name = "contentOffset4Reuse")
  public void setContentOffset4Reuse(HippyScrollView view, HippyMap offsetMap) {
    view.setContentOffset4Reuse(offsetMap);
  }

  @HippyControllerProps(name = "pagingEnabled", defaultType = HippyControllerProps.BOOLEAN)
  public void setPagingEnabled(HippyScrollView view, boolean pagingEnabled) {
    view.setPagingEnabled(pagingEnabled);
  }

  @HippyControllerProps(name = "scrollEventThrottle", defaultType = HippyControllerProps.NUMBER, defaultNumber = 30.0D)
  public void setScrollEventThrottle(HippyScrollView view, int scrollEventThrottle) {
    view.setScrollEventThrottle(scrollEventThrottle);
  }

  @HippyControllerProps(name = "scrollMinOffset", defaultType = HippyControllerProps.NUMBER, defaultNumber = 5)
  public void setScrollMinOffset(HippyScrollView view, int scrollMinOffset) {
    view.setScrollMinOffset(scrollMinOffset);
  }

  @HippyControllerProps(name = "initialContentOffset", defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setInitialContentOffset(HippyScrollView view, int offset) {
    view.setInitialContentOffset((int)PixelUtil.dp2px(offset));
  }

  @Override
  public void onBatchComplete(View view) {
    super.onBatchComplete(view);

    if (view instanceof HippyScrollView) {
      ((HippyScrollView)view).scrollToInitContentOffset();
    }
  }

  @Override
  public void dispatchFunction(View view, String functionName, HippyArray args) {
    //noinspection unchecked
    super.dispatchFunction((ViewGroup) view, functionName, args);
    if (view instanceof HippyScrollView) {
      if (TextUtils.equals(SCROLL_TO, functionName)) {
        int destX = Math.round(PixelUtil.dp2px(args.getDouble(0)));
        int destY = Math.round(PixelUtil.dp2px(args.getDouble(1)));
        boolean animated = args.getBoolean(2);

        if (animated) {
          ((HippyScrollView) view).callSmoothScrollTo(destX, destY, 0);//用默认的动画事件
        } else {
          view.scrollTo(destX, destY);
        }
      }
      if (TextUtils.equals(SCROLL_TO_WITHOPTIONS, functionName) && args != null
          && args.size() > 0) {
        HippyMap hippyMap = args.getMap(0); //取第一个元素
        int destX = Math.round(PixelUtil.dp2px(hippyMap.getInt("x")));
        int destY = Math.round(PixelUtil.dp2px(hippyMap.getInt("y")));
        int duration = hippyMap.getInt("duration");
        if (duration > 0) {
          ((HippyScrollView) view).callSmoothScrollTo(destX, destY, duration);//用默认的动画事件
        } else {
          view.scrollTo(destX, destY);
        }
      }
    }
  }

  //zhaopeng add
  @HippyControllerProps(name = "makeChildVisibleType", defaultType = HippyControllerProps.STRING)
  public void setMakeChildVisibleType(HippyScrollView view, String type){
    if(view != null) {
      if("none".equals(type)){
        ((HippyScrollView) view).setRequestChildOnScreenType(HippyScrollView.SCROLL_TYPE_NONE);
      }else if("center".equals(type)){
        ((HippyScrollView) view).setRequestChildOnScreenType(HippyScrollView.SCROLL_TYPE_CENTER);
      }else{
        ((HippyScrollView) view).setRequestChildOnScreenType(HippyScrollView.SCROLL_TYPE_DEFAULT);
      }
    }
  }

  @HippyControllerProps(name = "useAdvancedFocusSearch", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setAdvancedFocusSearch(HippyScrollView view, boolean b)
  {
    view.setAdvancedFocusSearch(b);
  }

  @HippyControllerProps(name = "childOnScreenScrollType", defaultType = HippyControllerProps.NUMBER, defaultNumber = 5)
  public void setScrollType(HippyScrollView view, int type)
  {
    if(view instanceof HippyVerticalScrollView) {
      ((HippyVerticalScrollView) view).setScrollType(type);
    }
  }


}
