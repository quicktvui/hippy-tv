package com.quicktvui.hippyext;

import android.view.View;

/**
 * Create by weipeng on 2022/06/01 16:13
 * Describe 由于ES-SDK使用了自定义的CommonViewController
 * method并没有使用HP的注解，所以需要自己赋值
 */
public interface IEsComponentTag {

  /**
   * 回调到CommonViewController中
   * @param view
   * @param prop
   * @param data
   * @return 是否拦截到此方法
   */
  boolean invokePropMethodForPending(View view, String prop, Object data);

}
