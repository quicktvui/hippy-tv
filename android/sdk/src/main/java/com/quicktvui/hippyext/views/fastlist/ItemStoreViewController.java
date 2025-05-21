package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.RenderNode;


@HippyController(
        name = ItemStoreViewController.CLASS_NAME
)
public class ItemStoreViewController extends HippyViewController<ItemStoreView> {
    public static final String CLASS_NAME = "ItemStoreView";
    public static final String TAG = "ItemStoreViewLog";

    public ItemStoreViewController() {
    }

    @Override
    protected void addView(ViewGroup parentView, View view, int index) {
//      super.addView(parentView, view, index);
    }

    protected View createViewImpl(Context context) {
        return null;
    }

    @Override
    protected View createViewImpl(Context context, HippyMap iniProps) {
        return new ItemStoreView(context);
    }

    @Override
    public RenderNode createRenderNode(int id, HippyMap props, String className, HippyRootView hippyRootView, ControllerManager controllerManager, boolean lazy) {
        return new ItemStoreNode(id, props, className, hippyRootView, controllerManager, lazy);
    }

}
