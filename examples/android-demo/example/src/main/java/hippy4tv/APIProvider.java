package hippy4tv;

import com.tencent.mtt.hippy.HippyAPIProvider;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.common.Provider;
import com.tencent.mtt.hippy.modules.javascriptmodules.HippyJavaScriptModule;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;
import com.tencent.mtt.hippy.uimanager.HippyViewController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class APIProvider implements HippyAPIProvider {
    @Override
    public Map<Class<? extends HippyNativeModuleBase>, Provider<? extends HippyNativeModuleBase>> getNativeModules(HippyEngineContext hippyEngineContext) {
        return null;
    }

    @Override
    public List<Class<? extends HippyJavaScriptModule>> getJavaScriptModules() {
        return null;
    }

    @Override
    public List<Class<? extends HippyViewController>> getControllers() {
        final List<Class<? extends HippyViewController>> components = new ArrayList<>();
        //regist the MyViewController
        components.add(WaterfallViewController.class);
        return components;
    }
}
