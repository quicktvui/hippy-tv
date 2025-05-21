package com.quicktvui.hippyext.views;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.hippyext.views.fastlist.FastPendingView;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.utils.ExtendUtil;

import java.util.ArrayList;

public class FocusSearchHelper {

  private final ViewGroup viewGroup;

  private HippyMap firstFocusChildMap;
  private boolean isFindAtStart = false;
  public final static String TAG = "DebugFocusParent";
  private static boolean DEBUG =  false;

  public FocusSearchHelper(ViewGroup viewGroup) {
    this.viewGroup = viewGroup;
  }


  public void setFindAtStart(boolean findAtStart) {
    isFindAtStart = findAtStart;
    if(DEBUG) {
      Log.i(TAG,"setFindAtStart :"+findAtStart+",view:"+ExtendUtil.debugViewLite(viewGroup));
    }
  }

  public void setFirstFocusChildMap(HippyMap firstFocusChildMap) {
    this.firstFocusChildMap = firstFocusChildMap;
    if(DEBUG) {
      Log.i(TAG,"setFirstFocusChildMap :"+firstFocusChildMap+",view:"+ExtendUtil.debugViewLite(viewGroup));
    }
  }

  public static  View findSpecifiedNext(ViewGroup parent,HippyMap map,int direction){
    if (map == null || parent == null) {
      return null;
    }
    View next = null;
    final Object value = map.get(ExtendUtil.getDirectionName(direction));
    if(DEBUG) {
      Log.i(TAG,"findSpecifiedNext map:"+map+",value:"+value+",parent:"+ExtendUtil.debugViewLite(parent));
    }
    if (value instanceof Integer) {
      //位置,childIndex
      int index = (int) value;
      if(parent instanceof FastPendingView){
        next = ((FastPendingView) parent).findViewByPosition(index);
      }else{
        if (parent.getChildCount() > index && index > -1) {
           next =  parent.getChildAt(index);
        }
      }
      if(DEBUG) {
        if (next != null) {
          Log.e(TAG,"findSpecifiedNext by index:"+index+",next:"+ExtendUtil.debugViewLite(next)+",parent:"+ExtendUtil.debugViewLite(parent));
        } else {
          Log.i(TAG,"findSpecifiedNext by index:"+index+",next:"+null);
        }
      }

    } else if (value instanceof String) {
        next = ExtendUtil.findViewBySID((String) value,parent);
        if(DEBUG) {
          Log.i(TAG,"findSpecifiedNext by sid:"+value+",next:"+ExtendUtil.debugViewLite(next));
        }
    }
    return next;
  }


  public View findFirstFocusChildByDirection(int direction){
    View nextChild = null;
    nextChild = findSpecifiedNext(viewGroup,firstFocusChildMap,direction);
    if(DEBUG) {
      if (firstFocusChildMap != null) {
        Log.i(TAG,"findFirstFocusChildByDirection firstFocusChildMap :"+firstFocusChildMap+",viewGroup:"+ExtendUtil.debugViewLite(viewGroup));
      }
    }
    if (nextChild == null){
      if(isFindAtStart){
        if (viewGroup instanceof FastPendingView) {
          nextChild = ((FastPendingView) viewGroup).findFirstFocusByDirection(direction);
          if(DEBUG) {
            Log.i(TAG, "findFirstFocusChildByDirection from  FastPendingView :" + ExtendUtil.debugViewLite(nextChild) + ",isFindAtStart:" + isFindAtStart);
          }
        }
      }
    }else{
      if(DEBUG) {
        Log.e(TAG,"findFirstFocusChildByDirection specified :"+ExtendUtil.debugViewLite(nextChild));
      }
    }
    return nextChild;
  }

  public boolean addFocusables(ArrayList<View> views, int direction){
    if (viewGroup == null || viewGroup.hasFocus()) {
      return false;
    }
    View next = findFirstFocusChildByDirection(direction);
    if (FocusUtils.testFocusable(next)) {
      if(DEBUG) {
        Log.e(TAG,"addFocusables view :"+ExtendUtil.debugViewLite(next));
      }
      views.add(next);
      return true;
    }else{
      if(DEBUG) {
        Log.i(TAG,"addFocusables view :"+ExtendUtil.debugViewLite(null));
      }
    }
    return false;
  }

}
