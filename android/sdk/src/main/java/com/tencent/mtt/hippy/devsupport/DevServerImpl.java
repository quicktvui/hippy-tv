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
package com.tencent.mtt.hippy.devsupport;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.Toast;

import com.quicktvui.base.ui.DebugCache;
import com.tencent.mtt.hippy.HippyGlobalConfigs;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.UIThreadUtils;

import java.io.InputStream;

@SuppressWarnings({"unused"})
public class DevServerImpl implements View.OnClickListener, DevServerInterface,
  DevExceptionDialog.OnReloadListener,
  DevRemoteDebugManager.RemoteDebugExceptionHandler, LiveReloadController.LiveReloadCallback {

  private static final String TAG = "DevServerImpl";

  final DevServerHelper mFetchHelper;
  DevServerCallBack mServerCallBack;
  DevExceptionDialog mExceptionDialog;
  private Context mHostContext;
  private final DevServerConfig mServerConfig;
  private final LiveReloadController mLiveReloadController;

  //zhaopeng add
  private BroadcastReceiver mCodeReceiver;
  public static String CODE_CHANGE_ACTION = "eskit.sdk.action.CODE_CHANGED";

  DevServerImpl(HippyGlobalConfigs configs, String serverHost, String bundleName) {
    mFetchHelper = new DevServerHelper(configs, serverHost);
    mServerConfig = new DevServerConfig(serverHost, bundleName);
    mLiveReloadController = new LiveReloadController(mFetchHelper);
  }

  @Override
  public void onClick(final View v) {
    final boolean isLiveReloadEnable = mServerConfig.enableLiveDebug();
    if (v.getContext() instanceof Application) {
      LogUtils.e(TAG, "Hippy context is an Application, so can not show a dialog!");
    } else {
      new AlertDialog.Builder(v.getContext()).setItems(
        new String[]{"Reload", isLiveReloadEnable ? "Disable Live Reload" : "Enable Live Reload"},
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            switch (which) {
              case 0:
                reload();
                break;
              case 1:
                mServerConfig.setEnableLiveDebug(!isLiveReloadEnable);
                startLiveDebug();
                break;
            }
          }
        }).show();
    }
  }

  void startLiveDebug() {
    if (mServerConfig.enableLiveDebug()) {
      mLiveReloadController.startLiveReload(this);
    } else {
      mLiveReloadController.stopLiveReload();
    }

  }

  @Override
  public String createResourceUrl(String resName) {
    return mFetchHelper
      .createBundleURL(mServerConfig.getServerHost(), resName, mServerConfig.enableRemoteDebug(),
        false, false);
  }

  @Override
  public void loadRemoteResource(String url, final DevServerCallBack serverCallBack) {
    mFetchHelper.fetchBundleFromURL(new BundleFetchCallBack() {
      @Override
      public void onSuccess(InputStream inputStream) {
        if (serverCallBack != null) {
          serverCallBack.onDevBundleLoadReady(inputStream);
        }
      }

      @Override
      public void onFail(Exception exception) {
        if (serverCallBack != null) {
          serverCallBack.onInitDevError(exception);
        }
      }
    }, url);
  }

  @Override
  public void reload() {
    if (mServerCallBack != null) {
      mServerCallBack.onDevBundleReLoad();
    }
  }

  @Override
  public void setDevServerCallback(DevServerCallBack devServerCallback) {
    this.mServerCallBack = devServerCallback;
  }

  @Override
  public void attachToHost(HippyRootView view) {
    Context host = view.getHost();
    mHostContext = host;
    if (mCodeReceiver == null) {
      mCodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          tryReload(context);
        }
      };
      host.getApplicationContext().registerReceiver(mCodeReceiver, new IntentFilter(CODE_CHANGE_ACTION));
    }
  }

  private void tryReload(Context context) {
    DebugCache.get().release();
    Toast.makeText(context, "界面刷新！", Toast.LENGTH_SHORT).show();
    reload();
  }

  @Override
  public void detachFromHost(HippyRootView view) {
    LogUtils.d(TAG, "hippy DevServerImpl detachFromHost");
    if (mExceptionDialog != null && mExceptionDialog.isShowing()) {
      UIThreadUtils.runOnUiThread(() -> mExceptionDialog.dismiss());
    }
    mHostContext = null;
    mExceptionDialog = null;
    Context host = view.getHost();
    if (mCodeReceiver != null) {
      host.getApplicationContext().unregisterReceiver(mCodeReceiver);
      mCodeReceiver = null;
    }
  }

  @Override
  public void handleException(final Throwable throwable) {
    if (mExceptionDialog != null && mExceptionDialog.isShowing()) {
      return;
    }

    /*if (mHostContext != null) {
      UIThreadUtils.runOnUiThread(() -> {
        mExceptionDialog = new DevExceptionDialog(mHostContext);
        mExceptionDialog.handleException(throwable);
        mExceptionDialog.setOnReloadListener(DevServerImpl.this);
        mExceptionDialog.show();
      });
    }*/
  }

  @Override
  public void onReload() {
    reload();
  }

  @SuppressWarnings("unused")
  @Override
  public void onHandleRemoteDebugException(Throwable t) {
  }

  @Override
  public void onCompileSuccess() {
    reload();
  }

  @Override
  public void onLiveReloadReady() {
    reload();
  }
}
