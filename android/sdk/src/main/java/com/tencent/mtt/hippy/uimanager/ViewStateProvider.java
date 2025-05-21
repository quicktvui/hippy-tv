package com.tencent.mtt.hippy.uimanager;

import android.support.annotation.NonNull;

import com.tencent.mtt.hippy.common.HippyMap;

public interface ViewStateProvider {

  void getState(@NonNull HippyMap map);
}
