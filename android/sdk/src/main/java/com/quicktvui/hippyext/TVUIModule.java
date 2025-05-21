package com.quicktvui.hippyext;


import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.utils.PixelUtil;

@HippyNativeModule(name = TVUIModule.CLASSNAME)
public class TVUIModule extends HippyNativeModuleBase {


  static final String CLASSNAME = "TVUIModule";



  public TVUIModule(HippyEngineContext context) {
    super(context);
  }

  @HippyMethod(name = "setScreenAdaptType")
  public void screenAdaptType(int type) {
    PixelUtil.setsScreenAdaptType(type);
  }

  @HippyMethod(name = "screenDevWidth")
  public void screenDevWidth(int devWidth) {
    PixelUtil.setDevWidth(devWidth);
  }





}
