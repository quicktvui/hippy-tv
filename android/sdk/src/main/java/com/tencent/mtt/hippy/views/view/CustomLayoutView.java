package com.tencent.mtt.hippy.views.view;



public interface CustomLayoutView {
  String TAG = "LayoutLog";
  void setLayoutRequestFromCustom(boolean b);
  boolean isLayoutRequestFromCustom();

}
