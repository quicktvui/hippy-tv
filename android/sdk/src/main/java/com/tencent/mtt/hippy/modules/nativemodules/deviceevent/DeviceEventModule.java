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
package com.tencent.mtt.hippy.modules.nativemodules.deviceevent;

import com.tencent.mtt.hippy.HippyEngine;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceLifecycleEventListener;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.modules.javascriptmodules.EventDispatcher;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.UIThreadUtils;

/**
 * @author: edsheng
 * @date: 2018/6/19 11:04
 * @version: V1.0
 */
@HippyNativeModule(name = "DeviceEventModule",init = true)
public class DeviceEventModule extends HippyNativeModuleBase
{
  HippyEngine.BackPressHandler mBackPressHandler = null;
  private boolean mIsListening = false;
  private boolean mIsListeningKeyEvent = true;
  private boolean isKeyEventIntercept = false;
  private int rootViewID = 0;

  private HippyInstanceLifecycleEventListener mInstanceLifecycleEventListener;

  public DeviceEventModule(final HippyEngineContext context)
  {
    super(context);

    mInstanceLifecycleEventListener = new HippyInstanceLifecycleEventListener() {
      @Override
      public void onInstanceLoad(int instanceId) {
        rootViewID = instanceId;
        callRootViewEventMethod(context,instanceId);
      }

      @Override
      public void onInstanceResume(int instanceId) {

      }

      @Override
      public void onInstancePause(int instanceId) {

      }

      @Override
      public void onInstanceDestroy(int instanceId) {

      }
    };
    context.addInstanceLifecycleEventListener(mInstanceLifecycleEventListener);
  }

  public boolean onBackPressed(HippyEngine.BackPressHandler handler)
  {
    if (mIsListening)
    {
      mBackPressHandler = handler;
      if (mContext != null && mContext.getModuleManager().getJavaScriptModule(EventDispatcher.class) != null)
      {
        mContext.getModuleManager().getJavaScriptModule(EventDispatcher.class).receiveNativeEvent("hardwareBackPress", null);
        return true;
      }
      else
      {
        return false;
      }
    }
    return false;
  }

  /**
   * 前端JS告知SDK：我要监听back事件（如果没有告知，则SDK不用把back事件抛给前端，这样可以加快back的处理速度，毕竟大部分hippy业务是无需监听back事件的）
   * @param listen 是否监听？
   */
  @HippyMethod(name = "setListenBackPress")
  public void setListenBackPress(boolean listen)
  {
    mIsListening = listen;
  }



  @HippyMethod(name = "setDispatchKeyEventIntercept")
  public void setKeyEventIntercept(boolean b)
  {
    this.isKeyEventIntercept = b;
    callRootViewEventMethod(mContext,rootViewID);
  }

  private HippyRootView findRootViewFromEngine(HippyEngineContext context,int rootViewID){
    return context.getInstance(rootViewID);
  }

  private void callRootViewEventMethod(HippyEngineContext context,int rootViewID){
    if(rootViewID > 0) {
      HippyRootView rootView = findRootViewFromEngine(context, rootViewID);
      if (rootView != null) {
        LogUtils.d("hippy","callRootViewEventMethod isKeyEventIntercept :"+isKeyEventIntercept+" ,mIsListeningKeyEvent:"+mIsListeningKeyEvent);
        rootView.setKeyEventIntercept(isKeyEventIntercept);
        rootView.enableDispatchEvent(mIsListeningKeyEvent);
        rootView.enableKeyDownEvent(mIsListeningKeyEvent);
        rootView.enableKeyUpEvent(mIsListeningKeyEvent);
      }else{
        LogUtils.e("hippy","callRootViewEventMethod rootView is null");
      }
    }else{
      LogUtils.e("hippy","callRootViewEventMethod rootViewID == 0");
    }
  }

  @HippyMethod(name = "setListenDispatchKeyEvent")
  public void setListenDispatchKeyEvent(boolean listen)
  {
    this.mIsListeningKeyEvent = listen;
    callRootViewEventMethod(mContext,rootViewID);
  }


  @HippyMethod(name = "invokeDefaultBackPressHandler")
  public void invokeDefaultBackPressHandler()
  {
    UIThreadUtils.runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        HippyEngine.BackPressHandler handler = mBackPressHandler;
        if (handler != null)
        {
          handler.handleBackPress();
        }
      }
    });
  }

  @Override
  public void destroy()
  {
    super.destroy();
    mBackPressHandler = null;
    if(mContext != null && mInstanceLifecycleEventListener != null){
      mContext.removeInstanceLifecycleEventListener(mInstanceLifecycleEventListener);
      mInstanceLifecycleEventListener = null;
    }

  }

  @Deprecated
  public interface InvokeDefaultBackPress
  {
    void callSuperOnBackPress();
  }
}
