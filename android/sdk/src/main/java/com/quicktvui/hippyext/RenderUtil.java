package com.quicktvui.hippyext;

import android.content.Context;
import android.util.Log;
import android.view.View;

import android.support.annotation.Nullable;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.dom.DomManager;
import com.tencent.mtt.hippy.dom.node.DomNode;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.uimanager.RenderManager;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.CustomLayoutView;

public class RenderUtil {

  public static void requestNodeLayout(Context context,RenderNode node){
    if (context instanceof HippyInstanceContext) {
      final HippyEngineContext c = ((HippyInstanceContext) context).getEngineContext();
      if (c != null) {
        node.forceLayout();
        node.update();
        c.getRenderManager().addUpdateNodeIfNeeded(node);
      }
    }
  }

  public static @Nullable RenderNode getRenderNodeByID(Context context, int id){
    if (context instanceof HippyInstanceContext) {
      final HippyEngineContext c = ((HippyInstanceContext) context).getEngineContext();
      if (c != null) {
        return c.getRenderManager().getRenderNode(id);
      }
    }
    return null;
  }

  public static @Nullable DomNode getDomNodeByID(Context context, int id){
    final DomManager dm = getDomManager(context);
    if (dm != null) {
      return dm.getNode(id);
    }
    return null;
  }

  public static @Nullable DomManager getDomManager(Context context){
    if (context instanceof HippyInstanceContext) {
      final HippyEngineContext c = ((HippyInstanceContext) context).getEngineContext();
      if (c != null) {
        return c.getDomManager();
      }
    }
    return null;
  }

  public static void updateViewLayoutByDomManager(RenderNode node,DomManager dm,RenderManager rm){
    if (node != null) {
      DomNode dn = dm.getNode(node.getId());
      if (dn != null) {
        dn.markUpdated();
      }
      dm.batch();
    }
  }


  public static void updateViewLayoutTraverse(RenderNode node,DomManager dm,RenderManager rm){
    if (node != null && rm != null && dm != null && rm.getControllerManager() != null) {
      DomNode dn = dm.getNode(node.getId());
      rm.getControllerManager().updateLayout(node.getClassName(),node.getId(),(int)dn.getLayoutX(),(int)dn.getLayoutY(),(int)dn.getLayoutWidth(),(int)dn.getLayoutHeight());
      for(int i = 0; i < node.getChildCount(); i++){
        updateViewLayoutTraverse(node.getChildAt(i),dm,rm);
      }
    }

  }

  static void updateFillParent(DomNode dn,int width,int height){
      if(dn != null && dn.getTotalProps() != null && dn.getTotalProps().getBoolean("fillParent")){
        dn.setStyleWidth(width);
        dn.setStyleHeight(height);
      }
      if(dn != null) {
        for (int i = 0; i < dn.getChildCount(); i++) {
          updateFillParent(dn.getChildAt(i), width, height);
        }
      }
  }

  public static void refreshDomLayoutDirectly(View view){
    refreshDomLayoutDirectly(view.getContext(),view.getId());
  }

  public static void refreshDomLayoutDirectly(Context context,int id){
      final DomManager dm = getDomManager(context);
    if (dm == null) {
      Log.w("TAG","refreshDomLayoutDirectly error DomManager is null context:"+context+",id:"+id);
      return;
    }
    final DomNode dn = dm.getNode(id);
    if (dn == null) {
      Log.w("TAG","refreshDomLayoutDirectly error dn");
      return;
    }
    if (dn instanceof StyleNode) {
      StyleNode sn = (StyleNode) dn;
      sn.calculateLayout();
      RenderNode rn = RenderUtil.getRenderNodeByID(context,id);
      RenderManager renderManager = ((HippyInstanceContext) context).getEngineContext().getRenderManager();
      updateViewLayoutTraverse(rn,dm,renderManager);
    }

  }

  public static boolean updateDomLayout(int x, int y, int width, int height, View view){
    DomManager domManager = RenderUtil.getDomManager(view.getContext());
    if (domManager != null) {
      DomNode domNode = domManager.getNode(view.getId());
      if (domNode instanceof StyleNode) {
        StyleNode sn = (StyleNode) domNode;
        sn.setLeftPositionValues(x);
        sn.setTopPositionValues(y);
        sn.setStyleWidth(width);
        sn.setStyleHeight(height);
//        sn.markLayoutSeen();
//        sn.markUpdated();
        updateFillParent(sn,width,height);
        sn.calculateLayout();
        RenderNode node = RenderUtil.getRenderNodeByID(view.getContext(),view.getId());
        RenderManager renderManager = ((HippyInstanceContext) view.getContext()).getEngineContext().getRenderManager();
        updateViewLayoutTraverse(node,domManager,renderManager);
//        Log.e(TriggerTaskManagerModule.TAG,"updateDomLayout:"+view.getId()+",x:"+x+",y:"+y+",width:"+width+",height:"+height);
//        domManager.batch();
//        if (node != null) {
//          node.updateLayout(x, y, width, height);
//          node.updateViewRecursive();
//        }
        return true;
      }
    }
    return false;

  }

  public static void  requestNodeLayout(Context context, int id){
    if (context instanceof HippyInstanceContext) {
      final HippyEngineContext c = ((HippyInstanceContext) context).getEngineContext();
      if (c != null) {
        final  RenderNode node = c.getRenderManager().getRenderNode(id);
        if(node != null) {
          node.forceLayout();
          node.update();
          if(LogUtils.isDebug()) {
            Log.d("RenderUtil", "update Node view :" + id);
          }
         c.getRenderManager().addUpdateNodeIfNeeded(node);
        }
      }
    }
  }

  public static void  addUpdateLayout(View v){
    if (v != null && v.getContext() instanceof HippyInstanceContext) {
      final HippyEngineContext c = ((HippyInstanceContext) v.getContext()).getEngineContext();
      if (c != null) {
        final  RenderNode node = c.getRenderManager().getRenderNode(v.getId());
        if(node != null) {
          node.forceLayout();
          if(LogUtils.isDebug()) {
            Log.d("RenderUtil", "update Node view :" + v.getId());
          }
          c.getRenderManager().addUpdateNodeIfNeeded(node);
        }
      }
    }
  }

  public static void  requestNodeLayout2(Context context, int id){
    if (context instanceof HippyInstanceContext) {
      final HippyEngineContext c = ((HippyInstanceContext) context).getEngineContext();
      if (c != null) {
        final  RenderNode node = c.getRenderManager().getRenderNode(id);
        if(node != null) {
          node.updateLayoutManual();
          if(LogUtils.isDebug()) {
            Log.d("RenderUtil", "update Node view :" + id);
            Log.d(CustomLayoutView.TAG, "update Node view :" + id);
          }
          //c.getRenderManager().addUpdateNodeIfNeeded(node);
        }
      }
    }
  }



  public static void  requestNodeLayout(View view){
    requestNodeLayout2(view.getContext(),view.getId());
  }

  public static void reLayoutView(View view,int x,int y,int width,int height){
    view.measure(
      View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY)
    );
    view.layout(x,y,x+width,y+height);
//    if(LogUtils.isDebug()) {
//      Log.d(CustomLayoutView.TAG, "reLayoutView view :" + view.getId()+",x："+x+",y:"+y+",width:"+width+",height:"+height);
//    }
  }

  public static void layoutView(View view,int l,int t,int r,int b){
    view.measure(
      View.MeasureSpec.makeMeasureSpec(r-l,View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(b-t,View.MeasureSpec.EXACTLY)
    );
    view.layout(l,t,r,b);
  }

  public static void reLayoutView(View view){
    if(view != null) {
      reLayoutView(view,view.getLeft(),view.getTop(),view.getWidth(),view.getHeight());
    }
  }


//  public static void  requestNodeLayout2(View view){
//    requestNodeLayout2(view.getContext(),view.getId());
//  }


}
