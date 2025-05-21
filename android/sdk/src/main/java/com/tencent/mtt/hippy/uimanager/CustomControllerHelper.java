package com.tencent.mtt.hippy.uimanager;

import android.content.Context;
import android.util.Log;
import android.view.View;

import android.support.annotation.Nullable;

import com.quicktvui.base.ui.StateView;
import com.quicktvui.hippyext.FocusManagerModule;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.utils.ArgumentUtils;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.quicktvui.hippyext.views.fastlist.TemplateCodeParser;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.quicktvui.hippyext.IEsComponentTag;

/**
 * create by zhaopeng
 * 2022-04-17
 */
public class CustomControllerHelper {
  public static final String TAG = "FastListCCH";


  public static void updateView(ControllerManager m, View view, String name, HippyMap newProps) {
    final HippyViewController viewComponent = m.mControllerRegistry.getViewController(name);
    updateView(m, view, viewComponent, newProps);
  }

  public static void updateView(ControllerManager m, View view, HippyViewController viewComponent, HippyMap newProps) {
    if (view != null && viewComponent != null && newProps != null) {
      m.mControllerUpdateManger.updateProps(viewComponent, view, newProps);
      viewComponent.onAfterUpdateProps(view);
    }
  }

  public static void updateLayout(View view, int x, int y, int width, int height) {
    if(view.getWidth() < 1 || view.getHeight() < 1 ||
      view.getWidth() != width || view.getHeight() != height){
      //尺寸有变化
      view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      view.layout(x, y, x + width, y + height);
    }else if(view.getLeft() != x || view.getTop() != y){
      view.layout(x,y,x + view.getWidth(),y + view.getHeight());
    }
  }

  public View createView(ControllerManager m, String name, HippyMap newProps) {
    final HippyViewController viewComponent = m.mControllerRegistry.getViewController(name);
    if (viewComponent != null) {
      return viewComponent.createViewImpl(m.mContext.getGlobalConfigs().getContext(), newProps);
    }
    return null;
  }

  static ControllerRegistry getControllerRegistry(ControllerManager controllerManager) {
    return controllerManager.mControllerRegistry;
  }

  public static HippyViewController getViewControllerByName(ControllerManager controllerManager, String className) {
    return getControllerRegistry(controllerManager).getViewController(className);
  }

  public static @Nullable HippyViewController getViewController(ControllerManager controllerManager, RenderNode node) {
    if (node == null) {
      return null;
    }
    return getControllerRegistry(controllerManager).getViewController(node.getClassName());
  }


  public static View createViewImpl(Context context, HippyViewController controller) {
    return controller.createViewImpl(context);
  }


  public static View createViewImpl(Context context, HippyMap iniProps, HippyViewController controller) {
    return controller.createViewImpl(context, iniProps);
  }


  public static Context getContext(RenderNode node) {
    return node.mRootView.getContext();
  }

  public static void updateExtra(ControllerManager m, View view, String name, Object object) {
    HippyViewController component = m.mControllerRegistry.getViewController(name);
    component.updateExtra(view, object);
  }

  public static void updateExtra(HippyViewController c, View view, Object textExtra) {
    c.updateExtra(view, textExtra);
  }

  public static void updateExtraIfNeed(HippyViewController c, View view, RenderNode node) {
    if (node.mTextExtra != null && c != null) {
      c.updateExtra(view, node.mTextExtra);
    }
  }


  public static Object getTextExtra(RenderNode node) {
    return node.mTextExtra;
  }

  public static void dispatchUIFunction(ControllerManager m, View view, String className, String functionName, HippyArray var,
                                        Promise promise) {
    HippyViewController hippyViewController = m.mControllerRegistry.getViewController(className);
    if (promise == null || !promise.isCallback()) {
      hippyViewController.dispatchFunction(view, functionName, var);
    } else {
      hippyViewController.dispatchFunction(view, functionName, var, promise);
    }

  }



  public static Map<String, ControllerUpdateManger.PropsMethodHolder> findPropsMethodGlobal(Class cla) {
    return ControllerUpdateManger.getPropsMethodSafety(cla);
  }

  /**
   * 执行node的属性
   * @param t
   * @param g
   * @param prop
   * @param object
   */
  public static void invokePropMethodForPending(HippyViewController t, View g, String prop, Object object) {
    Class cla = t.getClass();
    Map<String, ControllerUpdateManger.PropsMethodHolder> methodHolder = findPropsMethodGlobal(cla);

    ControllerUpdateManger.PropsMethodHolder propsMethodHolder = methodHolder.get(prop);
    try {
      if (propsMethodHolder != null) {
        if(TemplateCodeParser.isPendingProForce(object)){
          return;
        }
//        if(LogUtils.isDebug()){
//          if(g instanceof TVTextView){
//            Log.v("AutoMeasure", "invokePropMethodForPending prop:" + prop + ",value:" + object+",view:"+g.hashCode());
//          }
//        }
        if (object instanceof Number) {
          if (propsMethodHolder.mTypes == null) {
            propsMethodHolder.mTypes = propsMethodHolder.mMethod.getGenericParameterTypes();
          }
          object = ArgumentUtils.parseArgument(propsMethodHolder.mTypes[1], object);
        }
//        if (LogUtils.isDebug()) {
//          Log.i(TAG, "invokePropMethodForPending prop:" + prop + ",value:" + object);
//        }
        propsMethodHolder.mMethod.invoke(t, g, object);
      } else if (t instanceof IEsComponentTag) {
        if(TemplateCodeParser.isPendingProForce(object)){
          return;
        }
//        if (LogUtils.isDebug()) {
//          Log.i(TAG, "invokePropMethodForPending for IEsComponentTag prop:" + prop + ",value:" + object);
//        }
//        if(LogUtils.isDebug()){
//          if(g instanceof TVTextView){
//            Log.v("AutoMeasure", "invokePropMethodForPending for IEsComponentTag:" + prop + ",value:" + object+",view:"+g.hashCode());
//          }
//        }
        try {
          IEsComponentTag tagController = (IEsComponentTag) t;
          tagController.invokePropMethodForPending(g, prop, object);
        } catch (Exception e) {
          if(LogUtils.isDebug()) {
            e.printStackTrace();
          }else{
            Log.e(TAG,"invokePropMethodForPending error msg : "+e.getMessage());
          }
        }
      } else {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "invokePropMethodForPending propsMethodHolder is null, prop:" + prop + ",value:" + object);
        }
      }

    } catch (Exception e) {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "invokePropMethodForPending Exception prop:" + prop + ",value:" + object + ",Exception:" + e.getMessage());
        //e.printStackTrace();
      }
    }
  }


//  static void invokePropMethod4StyleSlot(HippyViewController t, View g, HippyMap hippyMap) {
//    assert (hippyMap != null);
//
//    Class cla = t.getClass();
//    Map<String, ControllerUpdateManger.PropsMethodHolder> methodHolder = findPropsMethodGlobal(cla);
//    Set<String> props = hippyMap.keySet();
//    for (String prop : props) {
//      ControllerUpdateManger.PropsMethodHolder propsMethodHolder = methodHolder.get(prop);
//      Log.d(TAG, "invokePropMethod4StyleSlot prop:" + prop + ",value:" + hippyMap.get(prop));
//      if (propsMethodHolder != null) {
//        CustomControllerHelper.invokePropMethod(t, g, hippyMap, prop, propsMethodHolder);
//      } else {
//        Log.e(TAG, "invokePropMethod4StyleSlot error propsMethodHolder == null , prop:" + prop + ",value:" + hippyMap.get(prop));
//      }
//    }
//  }

  public static int dealWithColor(String colorString) {
    return 0;
  }

  public static void dealCustomProp(View view, HippyMap initialProps) {
    //zhaopeng add 20201225 focus
    if (view != null && initialProps != null) {
      if (initialProps.containsKey(NodeProps.FOCUS_VIEW)) {
        view.setFocusable(true);
        if (view instanceof HippyViewGroup) {
          ((HippyViewGroup) view).setFocusBorderEnable(true);
          ((HippyViewGroup) view).setFocusScale(FocusManagerModule.defaultFocusScale);
        }
      }
      if (initialProps.containsKey("onChildFocus")) {
        if (view instanceof ExtendViewGroup) {
          ((ExtendViewGroup) view).setDispatchChildFocusEvent(true);
        }
      }

      boolean containShowOnState = initialProps.containsKey("showOnState");
      if (view instanceof StateView) {
        if (containShowOnState) {
          HippyArray array = initialProps.getArray("showOnState");
          if (array != null) {
            Set<String> customStates = new HashSet();
            final int[] states = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
              final String s = array.getString(i);
              switch (s) {
                case "selected":
                  states[i] = android.R.attr.state_selected;
                  break;
                case "focused":
                  states[i] = android.R.attr.state_focused;
                  break;
                case "normal":
                  states[i] = -1;
                  break;
                default:
                  customStates.add(s);
                  break;
              }
            }
            ((StateView) view).setShowOnState(states);
            if(customStates.size() > 0) {
              String[] customStatesArray = new String[customStates.size()];
              customStates.toArray(customStatesArray);
              ((StateView) view).setShowOnCustomState(customStatesArray);
            }else{
              ((StateView) view).setShowOnCustomState(null);
            }
          } else {
            String s = initialProps.getString("showOnState");
            String customState = null;
            int[] states = new int[1];
            switch (s) {
              case "selected":
                states[0] = android.R.attr.state_selected;
                break;
              case "focused":
                states[0] = android.R.attr.state_focused;
                break;
              case "normal":
                states[0] = -1;
                break;
              case "selected&focused":
              case "focused&selected":
                states[0] = StateView.STATE_FOCUS_AND_SELECT;
                break;
              case "selected&!focused":
                states[0] = StateView.STATE_SELECT_NO_FOCUS;
                break;
              case "focused&!selected":
                states[0] = StateView.STATE_FOCUS_NO_SELECT;
                break;
              default:
                customState = s;
                break;
            }
            ((StateView) view).setShowOnState(states);
            if (customState == null) {
              ((StateView) view).setShowOnCustomState(null);
            }else{
              ((StateView) view).setShowOnCustomState(new String[]{customState});
            }
          }
        }
      }

      if (initialProps.containsKey("duplicateParentState") || containShowOnState) {
        view.setDuplicateParentStateEnabled(true);
      } else {
        view.setDuplicateParentStateEnabled(false);
      }


    }
  }


}
