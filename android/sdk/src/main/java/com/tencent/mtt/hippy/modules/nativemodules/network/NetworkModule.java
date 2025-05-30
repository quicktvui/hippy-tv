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
package com.tencent.mtt.hippy.modules.nativemodules.network;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyGlobalConfigs;
import com.tencent.mtt.hippy.adapter.http.HippyHttpAdapter;
import com.tencent.mtt.hippy.adapter.http.HippyHttpRequest;
import com.tencent.mtt.hippy.adapter.http.HippyHttpResponse;
import com.tencent.mtt.hippy.adapter.http.HttpHeader;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.utils.ContextHolder;
import com.tencent.mtt.hippy.utils.LogUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@SuppressWarnings({"deprecation", "unused"})
@HippyNativeModule(name = "network")
public class NetworkModule extends HippyNativeModuleBase {

  // 使用CookieManager之前，需要初始化CookieSyncManager，单例的
  static CookieSyncManager mCookieSyncManager;
  static int CONNECT_TIME_OUT = 10 * 1000;
  static int READ_TIME_OUT = 10 * 1000;

  public NetworkModule(HippyEngineContext context) {
    super(context);
  }

  private void hippyMapToRequestHeaders(HippyHttpRequest request, HippyMap map) {
    if (request == null || map == null) {
      return;
    }

    Set<String> keys = map.keySet();
    for (String oneKey : keys) {
      Object valueObj = map.get(oneKey);
      if (valueObj instanceof HippyArray) {
        HippyArray oneHeaderArray = (HippyArray) valueObj;
        List<String> headerValueArray = new ArrayList<>();
        for (int i = 0; i < oneHeaderArray.size(); i++) {
          Object oneHeaderValue = oneHeaderArray.get(i);
          if (oneHeaderValue instanceof Number) {
            headerValueArray.add(oneHeaderValue + "");
          } else if (oneHeaderValue instanceof Boolean) {
            headerValueArray.add(oneHeaderValue + "");
          } else if (oneHeaderValue instanceof String) {
            headerValueArray.add((String) oneHeaderValue);
          } else {
            LogUtils.e("hippy_console", "Unsupported Request Header List Type");
          }
        }

        if (!headerValueArray.isEmpty()) {
          request.addHeader(oneKey, headerValueArray);
        }
      } else {
        LogUtils.e("hippy_console",
          "Unsupported Request Header Type, Header Field Should All be an Array!!!");
      }
    }
  }

  @HippyMethod(name = "setConnectTimeout")
  public void setConnectTimeout(int timeCount) {
    CONNECT_TIME_OUT = timeCount;
  }

  @HippyMethod(name = "setReadTimeout")
  public void setReadTimeout(int timeCount) {
    READ_TIME_OUT = timeCount;
  }

  @HippyMethod(name = "fetch")
  public void fetch(final HippyMap request, final Promise promise) {
    if (request == null) {
      promise.reject("invalid request param");
      return;
    }

    String url = request.getString("url");
    final String method = request.getString("method");
    if (TextUtils.isEmpty(url) || TextUtils.isEmpty(method)) {
      promise.reject("no valid url for request");
      return;
    }

    HippyHttpRequest httpRequest = new HippyHttpRequest();
    httpRequest.setConnectTimeout(CONNECT_TIME_OUT);
    httpRequest.setReadTimeout(READ_TIME_OUT);
    String redirect = request.getString("redirect");
    httpRequest.setInstanceFollowRedirects(
      !TextUtils.isEmpty(redirect) && TextUtils.equals("follow", redirect));
    httpRequest.setUseCaches(false);
    httpRequest.setMethod(method);
    httpRequest.setUrl(url);
    HippyMap headers = request.getMap("headers");
    if (headers != null) {
      HippyArray requestCookies = headers.getArray("Cookie");
      saveCookie2Manager(url, requestCookies);
      hippyMapToRequestHeaders(httpRequest, headers);
    }
    String body = request.getString("body");
    httpRequest.setBody(body);

    //商店长虹渠道 android:sharedUserId="android.uid.system" 报错 For security reasons, WebView is not allowed in privileged processes
    try {
      String cookie = getCookieManager().getCookie(url);
      if (!TextUtils.isEmpty(cookie)) {
        httpRequest.addHeader(HttpHeader.REQ.COOKIE, cookie);
      }
    } catch (Exception e) {
      if (LogUtils.isDebug()) {
        Log.e("NetworkModule", "异常------>" + e.getMessage());
      }
    }

    // FIXME add by weipeng 支持设置代理
    httpRequest.setProxy(request.getString("proxyHostName"),
      request.getInt("proxyPort"));
    // FIXME add by weipeng 支持设置代理 END

    HippyGlobalConfigs configs = mContext.getGlobalConfigs();
    HippyHttpAdapter adapter = null;
    if (configs != null) {
      adapter = configs.getHttpAdapter();
    }
    if (adapter != null) {
      adapter.sendRequest(httpRequest, new HttpTaskCallbackImpl(promise));
    }
  }

  @HippyMethod(name = "getCookie")
  public void getCookie(String url, Promise promise) {
    String cookie = getCookieManager().getCookie(url);
    promise.resolve(cookie);
  }

  @HippyMethod(name = "setCookie")
  public void setCookie(String url, String keyValue, String expires) {
    if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(keyValue)) {
      // 单个cookie
      if (!TextUtils.isEmpty(expires)) {
        keyValue = keyValue + ";expires=" + expires;
        getCookieManager().setCookie(url, keyValue);
      } else // 多个cookie
      {
        saveCookie2Manager(url, keyValue);
      }

      syncCookie();
    }
  }

  private void saveCookie2Manager(String url, HippyArray cookies) {
    if (cookies != null) {
      CookieManager cookieManager = getCookieManager();
      for (int i = 0; i < cookies.size(); i++) {
        String cookieStr = (String) cookies.get(i);
        saveCookie2Manager(url, cookieStr);
      }
    }
  }

  private void saveCookie2Manager(String url, String cookies) {
    if (cookies != null) {
      cookies = cookies.replaceAll("\\s+", "");
      String[] cookieItems = cookies.split(";");
      CookieManager cookieManager = getCookieManager();
      for (String cookie : cookieItems) {
        cookieManager.setCookie(url, cookie);
      }
    }
  }

  private static CookieManager getCookieManager() {
    if (mCookieSyncManager == null) {
      mCookieSyncManager = CookieSyncManager.createInstance(ContextHolder.getAppContext());

      CookieManager cookieManager = CookieManager.getInstance();
      cookieManager.setAcceptCookie(true);
    }
    return CookieManager.getInstance();
  }

  private static void syncCookie() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().flush();
    } else if (mCookieSyncManager != null) {
      mCookieSyncManager.sync();
    }
  }

  private static class HttpTaskCallbackImpl implements HippyHttpAdapter.HttpTaskCallback {

    private final Promise mPromise;

    public HttpTaskCallbackImpl(Promise promise) {
      mPromise = promise;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Override
    public void onTaskSuccess(HippyHttpRequest request, HippyHttpResponse response)
      throws Exception {
      String respBody = null;
      if (response.getInputStream() != null) {
        InputStream inputStream = response.getInputStream();
        if (isGzipRequest(request)) {
          inputStream = new GZIPInputStream(inputStream); // gzip解压
        }
        StringBuilder sb = new StringBuilder();
        String readLine;
        BufferedReader bfReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        while ((readLine = bfReader.readLine()) != null) {
          sb.append(readLine).append("\r\n");
        }
        respBody = sb.toString();
      }

      HippyMap respMap = new HippyMap();
      respMap.pushInt("statusCode", response.getStatusCode());
      respMap.pushString("statusLine", response.getResponseMessage());

      HippyMap headerMap = new HippyMap();
      if (response.getRspHeaderMaps() != null && !response.getRspHeaderMaps().isEmpty()) {
        Set<String> keys = response.getRspHeaderMaps().keySet();
        for (String oneKey : keys) {
          List<String> value = response.getRspHeaderMaps().get(oneKey);
          HippyArray oneHeaderFiled = new HippyArray();
          if (value != null && !value.isEmpty()) {
            boolean hasSetCookie = false;
            for (int i = 0; i < value.size(); i++) {
              String valueStr = value.get(i);
              oneHeaderFiled.pushString(valueStr);
              if (HttpHeader.RSP.SET_COOKIE.equalsIgnoreCase(oneKey)) {
                hasSetCookie = true;
                getCookieManager().setCookie(request.getUrl(), valueStr);
              }
            }
            if (hasSetCookie) {
              syncCookie();
            }
          }

          headerMap.pushArray(oneKey, oneHeaderFiled);
        }
      }

      respMap.pushMap("respHeaders", headerMap);
      if (respBody == null) {
        respBody = "";
      }

      respMap.pushString("respBody", respBody);

      if(LogUtils.isDebug()) {
        Log.i("DebugHttp", "onTaskSuccess request :"+request.getUrl());
        Log.i("DebugHttp", "onTaskSuccess respBody :"+respBody);
      }
      mPromise.resolve(respMap);
    }

    @Override
    public void onTaskFailed(HippyHttpRequest request, Throwable error) {
      if (error != null) {
        mPromise.resolve(error.getMessage());
      }
      Log.e("DebugHttp","TaskFailed error:"+ (error != null ? error.getMessage() : null));
      if (error != null) {
        error.printStackTrace();
      }
    }
  }

  // 检查是否是gzip/deflate压缩的请求
  private static boolean isGzipRequest(HippyHttpRequest request) {
    if (request == null) {
      return false;
    }
    Map<String, Object> headers = request.getHeaders();
    if (headers != null) {
      for (Map.Entry<String, Object> header : headers.entrySet()) {
        String key = header.getKey();
        if (key != null && key.equalsIgnoreCase(HttpHeader.REQ.ACCEPT_ENCODING)) {
          Object value = header.getValue();
          if (value instanceof ArrayList) {
            //noinspection unchecked
            for (String valueItem : (ArrayList<String>) value) {
              if (valueItem.equalsIgnoreCase("gzip") || valueItem.equalsIgnoreCase("deflate")) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }
}
