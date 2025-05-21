package com.quicktvui.hippyext.views.fastlist;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.quicktvui.hippyext.RenderUtil;
import com.quicktvui.base.ui.JSEventHandleView;
import com.quicktvui.base.ui.waterfall.Chunk;
import com.quicktvui.base.ui.waterfall.Tabs;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

public class ReplaceChildView extends HippyViewGroup implements JSEventHandleView, Chunk {

  public static final String TAG = "DebugReplaceChild";
  private View boundView;
  boolean isContentSurfaceView;
  private int sendEventViewID = -1;
  private String boundSID = null;
  private static final boolean DEBUG = LogUtils.isDebug();

  private String eventReceiverSID = null;

  private boolean notifyAttachDirty = true;

  public void setEventReceiverSID(String eventReceiverSID) {
    this.eventReceiverSID = eventReceiverSID;
  }

  private boolean replaceOnVisibilityChanged = true;

  public ReplaceChildView(Context context) {
    super(context);
//    setBackgroundColor(Color.GREEN);
  }

  public void setContentSurfaceView(boolean contentSurfaceView) {
    isContentSurfaceView = contentSurfaceView;
  }

  public void replaceChildIfNeed(){
    if(boundSID != null && replaceOnVisibilityChanged){
      exeReplaceChild(boundSID);
    }
  }

  @Override
  public void notifyBringToFront(boolean b){
    super.notifyBringToFront(b);

      if(b) {
        if(boundSID != null && ExtendUtil.getViewSID(this) != null){
          //Log.e(TAG,"exeReplaceChild on notifyBringToFront");
          exeReplaceChild(boundSID);
        }
      }else{
        if (boundView != null) {
          removeAllViews();
          boundView = null;
        }
      }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if(visibility == View.VISIBLE){
      if(DEBUG) {
        Log.i(TAG, "-----onWindowVisibilityChanged true boundView :" + boundView + ",boundSID :" + boundView + ",width:" + getWidth() + ",height:" + getHeight() + ",childCount:" + getChildCount());
      }
      if(boundSID != null && replaceOnVisibilityChanged){
        //Log.e(TAG,"exeReplaceChild onWindowVisibilityChanged boundSID:"+boundSID);
        exeReplaceChild(boundSID);
      }
    }else{
      if(DEBUG) {
//        Log.e(TAG, "-----onWindowVisibilityChanged invisible");
      }
    }
    final HippyMap mp = newEventMap();
    mp.pushString("childSID",boundSID);
    mp.pushBoolean("visible",visibility == VISIBLE);
    sendJSEvent("onWindowVisibilityChanged",mp);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if(DEBUG) {
      Log.i(TAG, "onSizeChanged w :" + w + ",h :" + h);
    }
    if(boundView != null){
      //boundView.layout(0,0,w,h);
      layoutIndieView(boundView,0,0,getWidth(),getHeight());
    }
    final HippyMap mp = newEventMap();
    mp.pushString("childSID",boundSID);
    mp.pushInt("width",w);
    mp.pushInt("height",h);
    sendJSEvent("onSizeChanged",mp);
  }


  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    if(DEBUG) {
      Log.i(TAG, "onLayout r :" + r + ",b :" + b+",this:"+ExtendUtil.debugView(this));
//      ExtendUtil.logView(TAG,this);
    }
    if(boundView != null){
      boundView.layout(0,0,r - l,b - t);
    }
  }

  protected void exeReplaceChild(String childSID){
    if(DEBUG) {
      Log.i(TAG, "exeReplaceChild ----> this :" + this);
      Log.i(TAG, "exeReplaceChild ----> sid :" + childSID + ",currentChildrenCount:" + getChildCount() + ",boundView:" + boundView);
    }
    boolean changed = childSID != null && !childSID.equals(this.boundSID);
    this.boundSID = childSID;
    if(TextUtils.isEmpty(childSID)){
      if (getChildCount() > 0 && getChildAt(0) != null) {
//        Log.e(TAG,"------remove indieView on childSID empty view:"+getChildAt(0));
        removeViewAt(0);
        boundView = null;
      }
    }else {
      final View indieView = findChildViewBySID(childSID);
      if(DEBUG) {
        Log.i(TAG, "exeReplaceChild sid:" + childSID + ",find indieView : " + indieView);
      }
      boundView = indieView;
      if (indieView != null) {
        if (getChildCount() > 0 && getChildAt(0) == indieView) {
//          Log.e(TAG,"exeReplaceChild return on child == indieView");
          layoutIndieView(indieView,0,0,getWidth(),getHeight());
          return;
        }
        boolean sameView = getChildCount() > 0 && getChildAt(0) == indieView;
        boolean requestFocus = indieView.hasFocus() && sameView;
        try {
          View focused = null;
          if (requestFocus) {
            focused = indieView.findFocus();
            this.requestFocus();
          }
          FastAdapterUtil.removeFromParentIfNeed(indieView);
          removeAllViews();
          this.addView(indieView, getWidth(), getHeight());
//          layoutIndieView(indieView,0,0,getWidth(),getHeight());
          if (requestFocus) {
            if (focused != null) {
              focused.requestFocus();
            }else {
              indieView.requestFocus();
            }
          }
        }catch (Exception e){
          Log.e(TAG, "exeReplaceChild error "+e.toString());
          e.printStackTrace();
        }
        if(changed){
          final HippyMap mp = newEventMap();
          mp.pushString("childSID",childSID);
          sendJSEvent("onChildChanged",mp);
        }
        if(DEBUG) {
          Log.i(TAG, "exeReplaceChild addView indieView this" + ExtendUtil.debugView(this));
        }
        //indieView.layout(0,0,getWidth(),getHeight());
        layoutIndieView(indieView,0,0,getWidth(),getHeight());
      }else{
        Log.e(TAG, "!!!!exeReplaceChild on  indieView null this" + this);
      }
    }
  }

  void layoutIndieView(View view,int x,int y,int w,int h){
//    view.layout(0,0,getWidth(),getHeight());
    if(LogUtils.isDebug()) {
      Log.e(TAG, "!!!!layoutIndieView on  indieView null this" + ExtendUtil.debugViewLite(view) + ",x:" + x + ",y:" + y + ",w:" + w + ",h:" + h);
    }
    RenderUtil.updateDomLayout(x,y,w,h,view);
  }

  public void setBoundID(String childSID){
      exeReplaceChild(childSID);
  }

  public void markChildSID(String childSID){
    this.boundSID = childSID;
  }



  public View findChildViewBySID(String id){
    final View root = HippyViewGroup.findPageRootView(this);
    View result = null;
    if (root != null) {
      final RenderNode rootNode = Utils.getRenderNode(root);
      if (rootNode != null) {
        result =  ExtendUtil.findViewFromRenderNodeBySid(getHippyContext(),id,rootNode);
      }
      if (result == null) {
        result =  ExtendUtil.findViewBySID(id,root);
      }
    }
    return result;
  }

  @Override
  public void setJSEventViewID(int eventViewID) {
    sendEventViewID = eventViewID;
  }

  void sendJSEvent(String name,HippyMap param){
    HippyViewEvent event = new HippyViewEvent(name);
    if(sendEventViewID > -1) {
      event.send(sendEventViewID,getHippyContext(),param);
    }else if(getId() != -1){
      event.send(this,param);
    }else{
      Log.e(TAG,"sendJSEvent error name :"+name+",view id invalid,view:"+this);
    }
    if(boundView != null && boundView.getId() != -1){
      event.send(boundView.getId(),getHippyContext(),param);
    }
  }

  protected HippyMap newEventMap(){
    HippyMap hm = new HippyMap();
    hm.pushString("sid",ExtendUtil.getViewSID(this));
    return hm;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

//    Log.i(TAG,"onAttachedToWindow this:"+ExtendUtil.debugView(this)+",childSID:"+boundSID);
    notifyItemAttached();
  }

  protected void notifyItemAttached(){
//    if (getParent() instanceof ChunkGroup) {
//      ((ChunkGroup) getParent()).onChildAttachedToWindow(this);
//    }
      String sid = ExtendUtil.getViewSID(this);

    if (sid != null) {
      final HippyMap mp = newEventMap();
      sendJSEvent("onReplaceChildAttach",mp);
      notifyAttachDirty = false;
      if (eventReceiverSID != null) {
        View target = ExtendUtil.findViewFromRootBySID(eventReceiverSID,this);
        if (target instanceof Tabs) {
          ((Tabs) target).onChunkAttachedToWindow(this);
        }
        if (target != null) {
          HippyMap params = new HippyMap();
          params.pushString("eventName", "onReplaceChildAttach");
          params.pushMap("params", mp);
          new HippyViewEvent("onTabsEvent").send(target.getId(),getHippyContext(),params);
        }else{
          Log.e(TAG,"notifyItemAttached return on target is null");
        }
      }else{
        Log.e(TAG,"notifyItemAttached eventReceiverSID is null");
      }
    }else{
      notifyAttachDirty = true;
    }

  }

  @Override
  public void onSetSid(String sid) {
    super.onSetSid(sid);
      notifyItemAttached();
//      Log.i(TAG,"onSetSid sid:"+sid+",this:"+ExtendUtil.debugView(this));
  }

  @Override
  public void onResetBeforeCache() {
    super.onResetBeforeCache();

  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    notifyAttachDirty = true;
  }


  public void setReplaceOnVisibilityChanged(Boolean b) {
    this.replaceOnVisibilityChanged = b;
  }

  @Override
  public void onItemBind() {
    super.onItemBind();
  }
}
