package com.quicktvui.hippyext.views;

import android.content.Context;
import android.view.View;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.views.view.HippyViewGroupController;

@HippyController(name = "StateImageView")
public class StateImageViewController extends HippyViewGroupController {
  @Override
  protected View createViewImpl(Context context) {
    return new StateImageView(context);
  }

  @HippyControllerProps(name = "src", defaultType = HippyControllerProps.MAP)
  public void setStateDrawable(View tv, HippyMap map) {
    if (tv instanceof StateImageView) {
      ((StateImageView) tv).setStateSrc(map);
    }
  }

  /*** zhaopeng add8***/
  @HippyControllerProps(name = "loadImgDelay", defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setDelayLoadImage(View tv, int delay)
  {
    if (tv instanceof StateImageView) {
      ((StateImageView) tv).setImgLoadDelay(delay);
    }
  }

}
