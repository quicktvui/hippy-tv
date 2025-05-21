package com.quicktvui.hippyext;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.quicktvui.base.ui.TVFocusAnimHelper;
import com.quicktvui.base.ui.TVViewUtil;
import com.quicktvui.base.ui.graphic.BorderFrontDrawable;
import com.tencent.mtt.hippy.BuildConfig;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.utils.LogUtils;

import java.util.HashMap;

@HippyNativeModule(name = FocusManagerModule.CLASSNAME)
public class FocusManagerModule extends HippyNativeModuleBase {

  static final String CLASSNAME = "FocusModule";
  public static HashMap<Integer, GlobalFocusConfig> globalFocusConfig;
  private final GlobalFocusConfig mFocusConfig;
  public static float defaultFocusScale = TVFocusAnimHelper.DEFAULT_SCALE;
  public static float defaultPlaceholderFocusScale = 1.1f;
  public static int clampMaxWidth = -1;




  @HippyMethod(name = "setDefaultFocusBorderColor")
  public void setDefaultFocusBorderColor(String color) {
    Log.i(CLASSNAME, "setDefaultFocusBorderColor :" + color +",engineContext:"+mContext);
    mFocusConfig.defaultFocusBorderColor = Color.parseColor(color);
  }

  @HippyMethod(name = "setClampMaxWidthRate")
  public void setClampMaxWidthRate(float rate) {
    mFocusConfig.clampMaxWidthRate = rate;
    FocusManagerModule.clampMaxWidth = (int) (mFocusConfig.clampMaxWidthRate * mFocusConfig.screenWidth);
    Log.i(CLASSNAME, "setClampMaxWidthRate :" + rate + ",clampMaxWidth:" + FocusManagerModule.clampMaxWidth);
  }

  @HippyMethod(name = "setDefaultFocusInnerBorderEnable")
  public void setDefaultFocusInnerBorderEnable(boolean enable) {
    Log.i(CLASSNAME, "setDefaultFocusInnerBorderEnable :" + enable +",engineContext:"+mContext);
    mFocusConfig.defaultFocusBorderInnerRectEnable = enable;
  }

  @HippyMethod(name = "enableFocusableTouchMode")
  public void setEnableFocusableTouchMode(boolean enable) {
    Log.i(CLASSNAME, "setEnableFocusableTouchMode :" + enable +",engineContext:"+mContext);
    mFocusConfig.enableFocusableTouchMode = enable;
  }

  @HippyMethod(name = "forceDisableFocus")
  public void setForceDisableFocus(boolean enable) {
    Log.i(CLASSNAME, "setForceDisableFocus :" + enable +",engineContext:"+mContext);
    mFocusConfig.forceDisableFocus = enable;
  }

  @HippyMethod(name = "disableScale")
  public void disableScale(boolean enable) {
    TVFocusAnimHelper.DISABLE_SCALE = enable;
  }

  @HippyMethod(name = "setFocusScaleAnimationDuration")
  public void setFocusScaleAnimationDuration(int duration) {
    TVFocusAnimHelper.DEFAULT_DURATION = duration;
  }

  @HippyMethod(name = "setFocusBorderInset")
  public void setFocusBorderInset(int inset) {
    BorderFrontDrawable.FOCUS_INSET = inset;
  }

  @HippyMethod(name = "setFocusBorderInsetValue")
  public void setDefaultFocusBorderInset(int inset) {
    mFocusConfig.defaultFocusBorderInset = inset;
  }

  @HippyMethod(name = "setDefaultFocusBorderCorner")
  public void setFocusBorderCorner(float radius) {
    mFocusConfig.defaultFocusBorderRadius = radius;
  }

  @HippyMethod(name = "setDefaultFocusBorderWidth")
  public void setDefaultFocusBorderWidth(int width) {
    mFocusConfig.defaultFocusBorderWidth = width;
  }

  @HippyMethod(name = "setDefaultFocusBorderEnable")
  public void setFocusBorderEnable(boolean enable) {
    mFocusConfig.defaultFocusBorderEnable = enable;
  }

  @HippyMethod(name = "setDefaultFocusScale")
  public void setDefaultFocusScale(float defaultFocusScale) {
    FocusManagerModule.defaultFocusScale = defaultFocusScale;
  }

  @HippyMethod(name = "setDefaultPlaceholderFocusScale")
  public void setDefaultPlaceholderFocusScale(float defaultFocusScale) {
    FocusManagerModule.defaultPlaceholderFocusScale = defaultFocusScale;
  }
  public FocusManagerModule(HippyEngineContext context) {
    super(context);
    final Context aContext = context.getGlobalConfigs().getContext();
    mFocusConfig = new GlobalFocusConfig();
    if(globalFocusConfig == null){
      globalFocusConfig = new HashMap<>();
    }
    globalFocusConfig.put(context.hashCode(),mFocusConfig);
    if (aContext != null) {
      mFocusConfig.screenWidth = TVViewUtil.getScreenWidth(aContext);
      FocusManagerModule.clampMaxWidth = (int) (mFocusConfig.clampMaxWidthRate * mFocusConfig.screenWidth);
      Log.i(CLASSNAME, "FocusManagerModule init screenWidth:" + mFocusConfig.screenWidth + ",clampMaxWidth:" + FocusManagerModule.clampMaxWidth+",context:"+context
      );
    }
//    mFocusConfig.forceDisableFocus = BuildConfig.
      mFocusConfig.forceDisableFocus = BuildConfig.INPUT_PREFERENCE.equals("touch");
      if(LogUtils.isDebug()){
        Log.i(CLASSNAME,"FocusManagerModule init INPUT_PREFERENCE:"+BuildConfig.INPUT_PREFERENCE);
      }

  }

  public static @NonNull GlobalFocusConfig getGlobalFocusConfig(@Nullable HippyEngineContext context){
    GlobalFocusConfig config = null;
    if (globalFocusConfig != null && context != null) {
      config = globalFocusConfig.get(context.hashCode());
    }
    if (config != null) {
      return config;
    }
    return new GlobalFocusConfig();
  }

  public static @NonNull GlobalFocusConfig findGlobalFocusConfig(@Nullable Context context){
    GlobalFocusConfig config = null;
    HippyEngineContext engineContext = null;
    if(context instanceof HippyInstanceContext){
      engineContext = ((HippyInstanceContext) context).getEngineContext();
    }else if(context instanceof HippyEngineContext){
      engineContext = (HippyEngineContext) context;
    }
    if (globalFocusConfig != null && engineContext != null) {
      config = globalFocusConfig.get(engineContext.hashCode());
    }
    if (config != null) {
      return config;
    }
    return new GlobalFocusConfig();
  }

  public static class GlobalFocusConfig {
    public  int defaultFocusBorderColor = Color.WHITE;
    public  float defaultFocusBorderRadius = 8;
    public  boolean defaultFocusBorderEnable = false;
    public  boolean defaultFocusBorderInnerRectEnable = true;
    public  float clampMaxWidthRate = 1f;
    public  int screenWidth = -1;
    public int defaultFocusBorderWidth = -1;
    public int defaultFocusBorderInset = 0;
    public boolean enableFocusableTouchMode = false;
    public boolean forceDisableFocus = false;

    @Override
    public String toString() {
      return "GlobalFocusConfig{" +
        "defaultFocusBorderColor=" + defaultFocusBorderColor +
        ", defaultFocusBorderRadius=" + defaultFocusBorderRadius +
        ", defaultFocusBorderEnable=" + defaultFocusBorderEnable +
        ", defaultFocusBorderInnerRectEnable=" + defaultFocusBorderInnerRectEnable +
        ", clampMaxWidthRate=" + clampMaxWidthRate +
        ", screenWidth=" + screenWidth +
        ", defaultFocusBorderWidth=" + defaultFocusBorderWidth +
        ", defaultFocusBorderInset=" + defaultFocusBorderInset +
        ", enableFocusableTouchMode=" + enableFocusableTouchMode +
        ", forceDisableFocus=" + forceDisableFocus +
        '}';
    }
  }


}
