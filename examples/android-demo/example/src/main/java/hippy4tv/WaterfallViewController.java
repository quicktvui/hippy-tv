package hippy4tv;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Toast;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.example.view.MyView;
import com.tencent.mtt.hippy.uimanager.HippyViewController;


@HippyController(name = "Waterfall")
public class WaterfallViewController extends HippyViewController<DynamicWaterfallPageView> {
  final String SET_WATERFALL_DATA = "setWaterfallData";
    @Override
    protected View createViewImpl(Context context) {
        final DynamicWaterfallPageView wpv = new DynamicWaterfallPageView(context);
        wpv.setBackgroundColor(Color.RED);
        wpv.setClipChildren(false);
        wpv.setClipToPadding(false);
        return wpv;
    }


  @HippyControllerProps(name = "waterfallData", defaultType = HippyControllerProps.STRING, defaultString = "")
  public void setWaterfallData(DynamicWaterfallPageView waterfallPageView, String waterfallData)
  {
//    if (waterfallData.isEmpty()){
//      return;
//    }
    Toast.makeText(waterfallPageView.getContext(),"data==="+waterfallData,Toast.LENGTH_LONG).show();
//        view.getPageInterface().setData();
  }

  @Override
  public void dispatchFunction(DynamicWaterfallPageView view, String functionName, HippyArray var) {
    super.dispatchFunction(view, functionName, var);
    if (view == null){
      return;
    }
    switch (functionName){
      case SET_WATERFALL_DATA:{
      }
      default:{
        break;
      }
    }
  }
}
