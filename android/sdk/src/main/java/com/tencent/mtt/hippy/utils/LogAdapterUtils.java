package com.tencent.mtt.hippy.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.adapter.HippyLogAdapter;

public class LogAdapterUtils {


  private static boolean DEBUG_ENABLE = true;

  public static @Nullable
  HippyLogAdapter getLoggerAdapter(Context context){
    if(context instanceof HippyInstanceContext){
      return getLoggerAdapter((HippyInstanceContext) context);
    }
    return null;
  }

  public static void setDebugLogEnable(boolean enable){
    DEBUG_ENABLE = enable;
  }


  public static @Nullable HippyLogAdapter getLoggerAdapter(HippyInstanceContext instanceContext){
    if (instanceContext != null) {
      HippyEngineContext engineContext = instanceContext.getEngineContext();
      if (engineContext != null) {
        return engineContext.getGlobalConfigs().getLogAdapter();
      }
    }
    return null;
  }

  public static void log(Context context,String tag, String msg) {
    HippyLogAdapter adapter = getLoggerAdapter(context);
    if(adapter != null){
      adapter.log(tag,msg);
    }else {
      Log.w(tag,"log error on log adapter is null tag:"+tag+" msg:"+msg);
    }
  }
}
