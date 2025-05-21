package com.tencent.mtt.hippy.views.scroll;

import com.tencent.mtt.hippy.common.HippyMap;

@SuppressWarnings("deprecation")
public interface HippyScrollView {

  void setScrollEnabled(boolean enabled);

  void showScrollIndicator(boolean showScrollIndicator);

  void setScrollEventEnable(boolean enable);

  void setScrollBeginDragEventEnable(boolean enable);

  void setScrollEndDragEventEnable(boolean enable);

  void setMomentumScrollBeginEventEnable(boolean enable);

  void setMomentumScrollEndEventEnable(boolean enable);

  void setFlingEnabled(boolean flag);

  void setContentOffset4Reuse(HippyMap offsetMap);

  void setPagingEnabled(boolean pagingEnabled);

  void setScrollEventThrottle(int scrollEventThrottle);

  void callSmoothScrollTo(int x, int y, int duration);

  void setScrollMinOffset(int scrollMinOffset);

  void setInitialContentOffset(int offset);

  void scrollToInitContentOffset();

  //zhaopeng add
  void setRequestChildOnScreenType(int type);

  void setAdvancedFocusSearch(boolean b);

  int SCROLL_TYPE_DEFAULT = 0;
  int SCROLL_TYPE_CENTER = 1;
  int SCROLL_TYPE_NONE = 2;
}
