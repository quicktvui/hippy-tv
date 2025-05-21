package com.tencent.mtt.hippy.views.view;

import android.content.Context;
import android.util.Log;

import com.quicktvui.hippyext.pm.WindowNode;
import com.tencent.mtt.hippy.R;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;

public class CardRootView extends HippyViewGroup{

  public CardRootView(Context context) {
    super(context);

    if(LogUtils.isDebug()) {
      Log.i(WindowNode.TAG, "CardRootView crate:"+ ExtendUtil.debugViewLite(this));
    }
    //setAsRootView();
//    setAsRootView();

    setTag(R.id.card_root_view, ExtendViewGroup.ROOT_TAG);
    setUseAdvancedFocusSearch(true);
  }


}
