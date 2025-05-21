package com.quicktvui.hippyext.views;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.base.ui.ICoverFlow;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.HippyGroupController;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.scroll.HippyScrollView;

/**
 * @Description: TODO
 * @author: edsheng
 * @date: 2018/8/27 9:55
 * @version: V1.0
 */
@HippyController(name = CoverFlowViewController.CLASS_NAME)
public class CoverFlowViewController<T extends ViewGroup & HippyScrollView> extends HippyGroupController
{
  protected static final String	SCROLL_TO	= "scrollTo";
	private static final String	SCROLL_TO_WITHOPTIONS	= "scrollToWithOptions";

	public static final String	CLASS_NAME	= "CoverFlow";

	@Override
	protected View createViewImpl(Context context, HippyMap iniProps)
	{
    if (iniProps.getBoolean("isVertical")) {
      return new CoverFlowVerticalView(context);
    }
    return new CoverFlowHorizontalView(context);
	}

	@Override
	protected View createViewImpl(Context context)
	{
		return null;
		//        return new HippyScrollView(context);
	}

	@HippyControllerProps(name = "scrollEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
	public void setScrollEnabled(HippyScrollView view, boolean flag)
	{
		view.setScrollEnabled(flag);
	}

	@HippyControllerProps(name = "showScrollIndicator", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setShowScrollIndicator(HippyScrollView view, boolean flag)
	{
		view.showScrollIndicator(flag);
	}

	@HippyControllerProps(name = "onScrollEnable", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setScrollEventEnable(HippyScrollView view, boolean flag)
	{
		view.setScrollEventEnable(flag);
	}

	@HippyControllerProps(name = "onScrollBeginDrag", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setScrollBeginDragEventEnable(HippyScrollView view, boolean flag)
	{
		view.setScrollBeginDragEventEnable(flag);
	}

	@HippyControllerProps(name = "onScrollEndDrag", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setScrollEndDragEventEnable(HippyScrollView view, boolean flag)
	{
		view.setScrollEndDragEventEnable(flag);
	}

	@HippyControllerProps(name = "onMomentumScrollBegin", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setMomentumScrollBeginEventEnable(HippyScrollView view, boolean flag)
	{
		view.setMomentumScrollBeginEventEnable(flag);
	}

	@HippyControllerProps(name = "onMomentumScrollEnd", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setMomentumScrollEndEventEnable(HippyScrollView view, boolean flag)
	{
		view.setMomentumScrollEndEventEnable(flag);
	}

	@HippyControllerProps(name = "onScrollAnimationEnd", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setScrollAnimationEndEventEnable(HippyScrollView view, boolean flag)
	{
//		view.setScrollAnimationEndEventEnable(flag);
	}

	@HippyControllerProps(name = "flingEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
	public void setFlingEnabled(HippyScrollView view, boolean flag)
	{
		view.setFlingEnabled(flag);
	}

	@HippyControllerProps(name = "contentOffset4Reuse")
	public void setContentOffset4Reuse(HippyScrollView view, HippyMap offsetMap)
	{
		view.setContentOffset4Reuse(offsetMap);
	}

	@HippyControllerProps(name = "pagingEnabled", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setPagingEnabled(HippyScrollView view, boolean pagingEnabled)
	{
		view.setPagingEnabled(pagingEnabled);
	}

	@HippyControllerProps(name = "scrollEventThrottle", defaultType = HippyControllerProps.NUMBER, defaultNumber = 30.0D)
	public void setScrollEventThrottle(HippyScrollView view, int scrollEventThrottle)
	{
		view.setScrollEventThrottle(scrollEventThrottle);
	}

	@HippyControllerProps(name = "scrollMinOffset", defaultType = HippyControllerProps.NUMBER, defaultNumber = 5)
	public void setScrollMinOffset(HippyScrollView view, int scrollMinOffset)
	{
		view.setScrollMinOffset(scrollMinOffset);
	}

	@Override
	public void dispatchFunction(View view, String functionName, HippyArray args) {
		super.dispatchFunction((ViewGroup) view, functionName, args);
		if(view instanceof  HippyScrollView)
		{

			if (TextUtils.equals(SCROLL_TO, functionName))
			{
				int destX = Math.round(PixelUtil.dp2px(args.getDouble(0)));
				int destY = Math.round(PixelUtil.dp2px(args.getDouble(1)));
				boolean animated = args.getBoolean(2);

				if (animated)
				{
					((HippyScrollView)view).callSmoothScrollTo(destX, destY,0);//用默认的动画事件
				}
				else
				{
					view.scrollTo(destX, destY);
				}
			}
			if (TextUtils.equals(SCROLL_TO_WITHOPTIONS, functionName) && args != null && args.size() > 0)
			{
				HippyMap hippyMap = args.getMap(0); //取第一个元素
				int destX = Math.round(PixelUtil.dp2px(hippyMap.getInt("x")));
				int destY = Math.round(PixelUtil.dp2px(hippyMap.getInt("y")));
				int duration = hippyMap.getInt("duration");
				if(duration > 0 )
				{
					((HippyScrollView)view).callSmoothScrollTo(destX, destY,duration);//用默认的动画事件
				}
				else
				{
					view.scrollTo(destX, destY);
				}
			}
		}
	}

  @Override
  public void dispatchFunction(View view, String functionName, HippyArray params, Promise promise) {
    if (TextUtils.equals("getCurrentIndex", functionName)) {
      if (view instanceof ICoverFlow && promise != null) {
        ICoverFlow cView = (ICoverFlow) view;
        int index = cView.getCurrentIndex();
        HippyMap resultMap = new HippyMap();
        resultMap.pushInt("index", index);
        promise.resolve(resultMap);
      }
      return;
    }
    super.dispatchFunction(view, functionName, params, promise);
  }

  @HippyControllerProps(name = "autoScrollInterval", defaultType = HippyControllerProps.NUMBER,defaultNumber = 0)
  public void setAutoScrollInterval(HippyScrollView view, int interval){
    if (view instanceof ICoverFlow) {
      ((ICoverFlow) view).setAutoScrollInterval(interval);
    }
  }

  @HippyControllerProps(name = "zoomInValue", defaultType = HippyControllerProps.NUMBER,defaultNumber = 0)
  public void setZoomInValue(HippyScrollView view, float value){
    if (view instanceof ICoverFlow) {
      ((ICoverFlow) view).setZoomInValue(value);
    }
  }


}
