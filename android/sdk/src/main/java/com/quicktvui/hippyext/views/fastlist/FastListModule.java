package com.quicktvui.hippyext.views.fastlist;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;

import java.util.HashMap;

@HippyNativeModule(name = "FastListModule")
public class FastListModule extends HippyNativeModuleBase {
  public static HashMap<Integer, GlobalConfig> globalConfig;
  private GlobalConfig config;
  public FastListModule(HippyEngineContext context) {
    super(context);
    config = new GlobalConfig();
    if(globalConfig == null){
      globalConfig = new HashMap<>();
    }
    globalConfig.put(context.hashCode(),config);
  }

  @HippyMethod(name = "clearAllCache")
  public void clearAllCache() {
    if(FastAdapter.gSharedCachePools != null){
      FastAdapter.gSharedCachePools.clear();
      FastAdapter.gSharedCachePools = null;
    }
  }

  @HippyMethod(name = "setPlaceholderIcon")
  public void setPlaceholderIcon(HippyArray array) {
    Log.i("FastListModule","setPlaceholderIcon array:"+array);
    config.placeholderIcon = array.getString(0);
    config.placeholderIconSize = array.getArray(1);
  }

  @HippyMethod(name = "setPlaceholderBorderRadius")
  public void setPlaceholderIcon(int radius) {
    Log.i("FastListModule","setPlaceholderBorderRadius radius:"+radius);
    config.placeholderBorderRadius = radius;
  }

  @HippyMethod(name = "scrollFactor")
  public void setScrollFactor(float scrollFactor) {
    Log.i("FastListModule","scrollFactor scrollFactor:"+scrollFactor);
    config.scrollFactor = scrollFactor;
  }

  @HippyMethod(name = "setFadeEnabled")
  public void setFadeEnabled(boolean fadeImageEnable) {
    Log.i("FastListModule","setFadeEnabled fadeImageEnable:"+fadeImageEnable);
    config.fadeImageEnable = fadeImageEnable;
  }

  @HippyMethod(name = "setDefaultFadingEdgeLength")
  public void setFadingEdgeLength(double length) {
    Log.i("FastListModule","setDefaultFadingEdgeLength length:"+length);
    config.defaultFadingEdgeLength = (float) length;
  }

  @HippyMethod(name = "setFadeDuration")
  public void setFadeDuration(int duration) {
    Log.i("FastListModule","setFadeDuration duration:"+duration);
    config.fadeDuration = duration;
  }

  public static class GlobalConfig {
    public String placeholderIcon = null;
    public HippyArray placeholderIconSize = null;
    public float scrollFactor = 1.2f;
    public int placeholderBorderRadius = 8;
    public float defaultFadingEdgeLength = 0.0f;
    public boolean fadeImageEnable = false;
    public int fadeDuration = 500;
  }

  public static @NonNull FastListModule.GlobalConfig getGlobalConfig(@Nullable HippyEngineContext context){
    FastListModule.GlobalConfig config = null;
    if (globalConfig != null && context != null) {
      config = globalConfig.get(context.hashCode());
    }
    if (config != null) {
      return config;
    }
    return new FastListModule.GlobalConfig();
  }
}
