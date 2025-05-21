package hippy4tv;

import android.content.Context;
import android.widget.FrameLayout;

import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;


class DynamicWaterfallPageView extends FrameLayout implements HippyViewBase {
  private NativeGestureDispatcher mGestureDispatcher;
  public DynamicWaterfallPageView(Context context) {
    super(context);
  }

  @Override
  public NativeGestureDispatcher getGestureDispatcher() {
    return mGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {
      mGestureDispatcher = dispatcher;
  }
}
