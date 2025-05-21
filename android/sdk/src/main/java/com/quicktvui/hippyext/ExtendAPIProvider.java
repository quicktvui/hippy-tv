package com.quicktvui.hippyext;


import com.quicktvui.hippyext.views.CoverFlowViewController;
import com.quicktvui.hippyext.views.ProgressBarViewController;
import com.quicktvui.hippyext.views.SeekBarViewController;
import com.quicktvui.hippyext.views.TVButtonViewController;
import com.quicktvui.hippyext.views.fastlist.FastFlexViewController;
import com.quicktvui.hippyext.views.fastlist.FastItemViewController;
import com.quicktvui.hippyext.views.fastlist.FastListModule;
import com.quicktvui.hippyext.views.fastlist.FastListViewController;
import com.quicktvui.hippyext.views.fastlist.ItemStoreViewController;
import com.quicktvui.hippyext.views.fastlist.ReplaceChildController;
import com.quicktvui.hippyext.views.StateImageViewController;
import com.quicktvui.hippyext.views.TextViewController;
import com.tencent.mtt.hippy.HippyAPIProvider;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.common.Provider;
import com.tencent.mtt.hippy.modules.javascriptmodules.HippyJavaScriptModule;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.uimanager.HippyViewController;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtendAPIProvider implements HippyAPIProvider {
  @Override
  public Map<Class<? extends HippyNativeModuleBase>, Provider<? extends HippyNativeModuleBase>> getNativeModules(final HippyEngineContext context) {
    Map<Class<? extends HippyNativeModuleBase>, Provider<? extends HippyNativeModuleBase>> modules = new HashMap<>();
    modules.put(TVUIModule.class, (Provider<TVUIModule>) () -> new TVUIModule(context));
    modules.put(FocusManagerModule.class, (Provider<FocusManagerModule>) () -> new FocusManagerModule(context));
    modules.put(TriggerTaskManagerModule.class, (Provider<TriggerTaskManagerModule>) () -> new TriggerTaskManagerModule(context));
    modules.put(FastListModule.class, (Provider<FastListModule>) () -> new FastListModule(context));
    modules.put(ExtendModule.class, (Provider<ExtendModule>) () -> new ExtendModule(context));
    modules.put(UsbManagerModule.class, (Provider<UsbManagerModule>) () -> new UsbManagerModule(context));
    return modules;
  }

  @Override
  public List<Class<? extends HippyJavaScriptModule>> getJavaScriptModules() {
    return null;
  }

  @Override
  public List<Class<? extends HippyViewController>> getControllers() {
    List<Class<? extends HippyViewController>> components = new ArrayList<>();
    components.add(CoverFlowViewController.class);
    components.add(TextViewController.class);
    components.add(ProgressBarViewController.class);
    components.add(SeekBarViewController.class);
    components.add(FastListViewController.class);
    components.add(FastItemViewController.class);
    components.add(FastFlexViewController.class);
    components.add(TVButtonViewController.class);
    components.add(ItemStoreViewController.class);
    components.add(StateImageViewController.class);
    components.add(ReplaceChildController.class);
    return components;
  }
}
