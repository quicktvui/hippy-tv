package com.quicktvui.hippyext.views.fastlist;

public class ListViewControlProp {

  //滚动位置
  public int scrollToPosition = -1;
  public int currentScrollToPosition = -1;
  //滚动offset
  public int scrollOffset = 0;
  //焦点位置
  public int focusPosition = -1;
  public int autofocusPosition = -1;
  public int autoScrollToPosition = -1;
  //自动焦点id
  public Object autoFocusID = null;
  //单一选中的位置
  public int singleSelectPosition = -1;
  public int pendingScrollToPosition = -1;
  public boolean pendingCheckAutofocus = false;

//  //滚动位置
//  public int initScrollToPosition = -1;
//  //滚动offset
//  public int initScrollOffset = -1;
//  //焦点位置
//  public int initFocusPosition = -1;
//  //自动焦点id
//  public String initAutofocus = null;
//  //单一选中的位置
//  public int initSingleSelect = -1;


  @Override
  public String toString() {
    return "ListViewControlProp{" +
      "autofocusPosition=" + autofocusPosition +
      ", autoScrollToPosition=" + autoScrollToPosition +
      ", singleSelectPosition=" + singleSelectPosition +
      '}';
  }
}
