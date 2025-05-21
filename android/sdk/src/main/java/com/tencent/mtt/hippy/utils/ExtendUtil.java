package com.tencent.mtt.hippy.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.Nullable;

import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.base.ui.TVViewUtil;
import com.quicktvui.hippyext.RenderUtil;
import com.quicktvui.hippyext.pm.PageRootNode;
import com.quicktvui.hippyext.pm.WindowNode;
import com.quicktvui.base.ui.ExtendTag;
import com.quicktvui.hippyext.views.fastlist.ClonedViewTag;
import com.quicktvui.hippyext.views.fastlist.FastAdapter;
import com.quicktvui.hippyext.views.fastlist.FastFlexView;
import com.quicktvui.hippyext.views.fastlist.FastListView;
import com.quicktvui.hippyext.views.fastlist.FastPendingView;
import com.quicktvui.hippyext.views.fastlist.Utils;
import com.quicktvui.hippyext.views.fastlist.VirtualListView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.uimanager.ViewStateProvider;
import com.tencent.mtt.hippy.views.view.CardRootView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerView;

public class ExtendUtil {

  public static boolean stateContainsAttribute(int[] stateSpecs, int attr) {
    return FocusUtils.stateContainsAttribute(stateSpecs,attr);
  }

  /**
   * 获取view的
   * @param view
   * @return
   */
  public static String getViewSID(@Nullable View view){
    return TVViewUtil.getViewSID(view);
  }

  public static @Nullable RenderNode getRenderNodeFromTag(@Nullable View view){
    if (view == null) {
      return null;
    }
    final ExtendTag et = ExtendTag.getExtendTag(view);
    if(et instanceof ClonedViewTag){
      return ((ClonedViewTag) et).getOriginNode();
    }
    return null;
  }



  public static String getViewName(@Nullable View view){
    return TVViewUtil.getViewName(view);
  }

  public static String debugView(View view){
    return TVViewUtil.debugView(view);
  }

  public static String debugFocusInfo(View view){
    return TVViewUtil.debugFocusInfo(view);
  }

  public static String debugViewLite(View view){
    return TVViewUtil.debugViewLite(view);
  }

  /**
   * 获取view的
   * @param view
   * @return
   */
  public static void putViewSID(View view,String sid){
    TVViewUtil.putViewSID(view,sid);
  }

  static void logParent(String TAG,View view){
    if (view != null) {
      Log.i(TAG,"log view:"+ExtendUtil.debugView(view));
      if (view.getParent() instanceof View) {
        logParent(TAG, (View) view.getParent());
      }
    }
  }

  public ExtendUtil() {
  }


  public static boolean handleShowOnState(View view, int[] states, int showOnState[]){
    // [-1,focused,selected]
    // [-1,focused]
    return FocusUtils.handleShowOnState(view, states, showOnState);
  }


  public static boolean handleCustomShowOnState(View view, ArrayMap<String,Boolean> states, String[] showOnState){
    // [-1,focused,selected]
    // [-1,focused]
    return FocusUtils.handleCustomShowOnState(view, states, showOnState);
  }

  //[-1,focused] -> [focused]
  //[-1] -> [focused,selected]
  public static int[] findOppositeState(int[] showOnState) {
    return FocusUtils.findOppositeState(showOnState);
  }



  public static boolean stateContainsAttribute(int[] stateSpecs, int state[]) {
    return FocusUtils.stateContainsAttribute(stateSpecs,state);
  }


  public static void layoutViewManual(View v) {
    final ViewGroup.LayoutParams lp = v.getLayoutParams();
    if (lp != null && lp.width > 0 && lp.height > 0) {
      if (lp.width != v.getWidth() || lp.height != v.getHeight()) {
        Log.e("ViewController", "layout width not match , layoutManual , v :" + v + ",width:" + v.getWidth() + ",height:" + v.getHeight() + ",lp width:" + lp.width + ",lp height:" + lp.height);
        final int mw = View.MeasureSpec.makeMeasureSpec(v.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
        final int mh = View.MeasureSpec.makeMeasureSpec(v.getMeasuredHeight(), View.MeasureSpec.EXACTLY);
        int x = 0;
        int y = 0;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
          x = ((ViewGroup.MarginLayoutParams) lp).leftMargin;
          y = ((ViewGroup.MarginLayoutParams) lp).topMargin;
        }
        v.measure(mw, mh);
        v.layout(x, y, x + lp.width, y + lp.height);
      }
    }
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) {
        View child = g.getChildAt(i);
        layoutViewManual(child);
      }
    }

  }

  public static void layoutView(View v,int x,int y,int width,int height) {
        final int mw = View.MeasureSpec.makeMeasureSpec(v.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
        final int mh = View.MeasureSpec.makeMeasureSpec(v.getMeasuredHeight(), View.MeasureSpec.EXACTLY);
        v.measure(mw, mh);
        v.layout(x, y, x + mw, y + mh);
  }

  public static FocusUtils.FocusParams findUserPureSpecifiedNextFocus(View focused, int direction){
    return FocusUtils.findUserPureSpecifiedNextFocus(focused, direction);
  }


  public static FocusUtils.FocusParams findUserSpecifiedNextFocusViewIdTraverse(ViewGroup parent, View focused, int direction,@Nullable View root) {
    return FocusUtils.findUserSpecifiedNextFocusViewIdTraverse(parent, focused, direction, root);
  }

  /***
   * 寻找当前view指定的下一焦点id
   * @param
   */
  public static String findSpecifiedNextFocusName(View sourceView, int direction) {
    return FocusUtils.findSpecifiedNextFocusName(sourceView,direction);
  }

  /***
   * 寻找当前view指定的下一焦点id
   * @param
   */
  public static String findSpecifiedNextFocusSID(View sourceView, int direction) {
    return FocusUtils.findSpecifiedNextFocusSID(sourceView,direction);
  }
  public static View findViewFromRootBySID(String sid,View view){
    View root = HippyViewGroup.findPageRootView(view);
    return findViewBySID(sid,root);
  }

  public static RenderNode findRenderNodeByName(String name, RenderNode node) {
    if (TextUtils.isEmpty(name)) {
      return null;
    }
    if (node.getProps() != null && name.equals(node.getProps().getString(NodeProps.NAME))) {
      return node;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      RenderNode child = node.getChildAt(i);
      RenderNode r = findRenderNodeByName(name, child);
      if (r != null) {
        return r;
      }

    }
    return null;
  }



  public static View findViewBySID(String id, View view,boolean checkValid) {
    return TVViewUtil.findViewBySID(id,view,checkValid);
  }

  public static View findViewBySID(String id, View view) {
    return findViewBySID(id,view,false);
  }

  @Deprecated
  public static View findViewByItemID(String id, View view) {
    return findViewBySID(id,view);
  }



  public static View findViewByName(String name, View view) {
    return TVViewUtil.findViewBySID(name,view);
  }

  public static View findViewByNameOrSID(View focused,FocusUtils.FocusParams fp, View view) {
//    Log.i(FocusDispatchView.TAG,"------findViewByNameOrSID view:"+ExtendUtil.debugView(view)+",fp:"+fp);
    return TVViewUtil.findViewByNameOrSID(focused,fp,view);
  }

  public static View executeFindNextFocus(ViewGroup group, View focused, int direction) {
    //注意： 此方法会调用this.addFocusables()
    //由于 focused可能不是group的子孙view,可能导致崩溃，所以这里try catch
    return FocusUtils.executeFindNextFocus(group, focused, direction);
  }

  public static boolean sameDescend(View parent,View focused, String userSpecifiedId) {
    return FocusUtils.sameDescend(parent,focused,userSpecifiedId);
  }


  public static boolean sameDescend(View parent,View focused, String userSpecifiedId,String userSpecialSID) {
    return FocusUtils.sameDescend(parent,focused,userSpecifiedId,userSpecialSID);
  }

  public static HippyMap getViewState(View view){
    HippyMap map = new HippyMap();
    map.pushInt("left", view.getLeft());
    map.pushInt("top", view.getTop());
    map.pushInt("right", view.getRight());
    map.pushInt("bottom", view.getBottom());
    map.pushInt("width", view.getWidth());
    map.pushInt("height", view.getHeight());
    map.pushBoolean("hasFocus", view.hasFocus());
    map.pushBoolean("isFocused", view.isFocused());
    map.pushDouble("alpha", view.getAlpha());
    map.pushString("name", TVViewUtil.findName(view));
    try {
      if (view instanceof ViewStateProvider) {
        ((ViewStateProvider) view).getState(map);
      }
    }catch (Throwable t){
      t.printStackTrace();
    }
    return map;
  }

  public static @Nullable  HippyMap getChildState(View view, int position){
    HippyMap map = null;
    if (view instanceof RecyclerView) {
        final RecyclerView rv = (RecyclerView) view;
        if(position > -1){
          View child =  rv.findViewByPosition(position);
          if (child != null) {
            map = getViewState(child);
          }
        }
    }else if(view instanceof android.support.v7.widget.RecyclerView){
      final android.support.v7.widget.RecyclerView rv = (android.support.v7.widget.RecyclerView) view;
      if(position > -1){
        android.support.v7.widget.RecyclerView.ViewHolder vh =  rv.findViewHolderForAdapterPosition(position);
        if (vh != null) {
          map = getViewState(vh.itemView);
        }
      }

    } else if (view instanceof ViewGroup) {
        if(position > -1 && position < ((ViewGroup) view).getChildCount()){
          map = getViewState(((ViewGroup) view).getChildAt(position));
        }
    }

    return map;
  }

  public static View findViewFromRenderNodeBySid(HippyEngineContext context,String id, RenderNode node) {
      if (TextUtils.isEmpty(id)) {
        return null;
      }
      Object sid = node.getProps() != null ? node.getProps().getString("sid") : null;

      if (id.equals(sid)) {
        return Utils.findBoundView(context,node);
      }

      for (int i = 0; i < node.getChildCount(); i++) {
        RenderNode child = node.getChildAt(i);
        View rv = findViewFromRenderNodeBySid(context,id,child);
        if (rv != null) {
          return rv;
        }
      }
      return null;
  }

  /**
   *通过itemID，寻找到需要更新的item的位置，然后更新item的数据
   * @param view
   * @param id
   * @param data
   * @return
   */
  public static boolean searchReplaceItemByItemID(View view, String id, Object data,boolean traverse){
    //step1. 从传入的view做为root,通过sid向下遍历找到需要更新的item的位置
    SearchResult sr = replaceItemViewByItemIDTraverse(view,id,data,traverse);
    return sr != null;
  }

  public static boolean searchReplaceItemByItemID(View view, String id, Object data){
    return searchReplaceItemByItemID(view,id,data,false);
  }



  public static void replaceParentItemDataTraverse(View root,View view,String itemID,Object data){
    if (view == null) {
      //没有parent了，直接返回
      return ;
    }
    if(view == root){
      //如果view是root，那么不需要再向上遍历
      return;
    }
    if (view.getParent() instanceof FastPendingView) {
      FastPendingView parent = (FastPendingView) view.getParent();
      //FIXME 这里相当于每次遍历都从parent里重新搜索一次，效率不高，后续优化
      parent.searchUpdateItemDataByItemID(itemID,data,false);
    }
    if(view.getParent() instanceof View) {
      replaceParentItemDataTraverse(root, (View) view.getParent(), itemID, data);
    }
  }


  public static SearchResult replaceItemViewByItemIDTraverse(View view, String id, Object data) {
    return replaceItemViewByItemIDTraverse(view,id,data,false);
  }
  public static SearchResult replaceItemViewByItemIDTraverse(View view, String id, Object data,boolean traverseUpdateView) {
    //1. 通过sid找到需要更新的item的位置
    int pos = -1;
    if(view instanceof FastPendingView){
      FastPendingView listView = (FastPendingView) view;
      pos = listView.findItemPositionBySID(id);
      //每一层的FastPendingView都要将里面的数据更新
      if(LogUtils.isDebug()) {
        Log.d("DebugReplaceItem", "---->start searchUpdateItemDataByItemID view:" + ExtendUtil.debugViewLite(view));
      }
    }
    if(view instanceof VirtualListView){
      ((VirtualListView) view).searchUpdateItemDataByItemID(id,data,true);
    }
    //2. 通过updateItem()更新itemView
    if(pos > -1 ){
      if(LogUtils.isDebug()) {
        Log.e("DebugReplaceItem", "found FastPendingView view:" + ExtendUtil.debugViewLite(view) + ",execute updateItem pos:" + pos);
      }
      ((FastPendingView)view).updateItem(pos,data,traverseUpdateView);
      return new SearchResult((FastPendingView)view,pos);
    }else if(view instanceof ViewGroup){
      //没有找到对应的位置，在子view中查找
      ViewGroup viewGroup = (ViewGroup) view;
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        final SearchResult resultOfChild = replaceItemViewByItemIDTraverse(child,id,data);
        if(resultOfChild != null){
          return resultOfChild;
        }
      }
      if(view instanceof FastPendingView){
        FastAdapter  fa = null;
        if (view instanceof FastListView) {
          fa = ((FastListView) view).getFastAdapter();
        } else if (view instanceof FastFlexView) {
          fa = ((FastFlexView) view).getAdapter();
        }
        if (fa != null && fa.getDetachedChildren() != null) {
          for(FastAdapter.Holder h : fa.getDetachedChildren().values()){
            View child = h.itemView;
            final SearchResult resultOfChild = replaceItemViewByItemIDTraverse(child,id,data);
//            Log.e("DebugReplaceItem","found view in detached holders");
            if(resultOfChild != null){
              return resultOfChild;
            }
          }
        }
      }
    }
    return null;
  }

  public static boolean isPureFocusView(@NonNull View view) {
    //获取view是一个直接可以获取焦点的View,还是一个viewGroup
    return FocusUtils.isPureFocusView(view);
  }

  static class SearchResult{
    //找到对应ite,并且执行更新的fastPendingView
    @NonNull public final FastPendingView pendingView;
    @NonNull public final int position;
    public SearchResult(FastPendingView pendingView, int position) {
      this.pendingView = pendingView;
      this.position = position;
    }
  }

  /**
   * 从view中寻找`PageRootView`
   * @param
   * @return
   */
  @SuppressLint("ResourceType")
  public static View findPageRootView(View v) {
    View root = null;
    if(v != null ) {
      //先从renderNode 里找
      if (v instanceof CardRootView) {
//        Log.d(WindowNode.TAG,"findPageRootView v instanceof CardRootView :"+ExtendUtil.debugViewLite(v));
        return v;
      }
      if (v.getId() != -1) {
        RenderNode node = RenderUtil.getRenderNodeByID(v.getContext(),v.getId());
        if (node != null && node.getWindowRootNode() != null) {
          root = node.getWindowRootNode().getWindowRootView();
        }
        if (node == null) {
          Log.e(WindowNode.TAG,"findPageRootView from renderNode v  error node is null v:"+ExtendUtil.debugViewLite(v));
        }
//        Log.i(WindowNode.TAG,"findPageRootView from renderNode v: "+ExtendUtil.debugViewLite(v)+",root:"+ExtendUtil.debugViewLite(root));
      }else{
        //证明此View在FastAdapter中生成的view
        RenderNode node = FastAdapter.findRenderNodeFromView(v);
        if (node != null && node.getWindowRootNode() != null) {
          root = node.getWindowRootNode().getWindowRootView();
        }
        if (node == null && LogUtils.isDebug()) {
          Log.e(WindowNode.TAG,"findPageRootView from renderNode v id -1 error node is null v:"+ExtendUtil.debugViewLite(v));
        }
//        Log.i(WindowNode.TAG,"findPageRootView from renderNode v id -1: "+ExtendUtil.debugViewLite(v)+",root:"+ExtendUtil.debugViewLite(root));
      }
      if (LogUtils.isDebug() && root == null && v.getId() > 5) {
        Log.w(PageRootNode.TAG,"findPageRootView from renderNode fail root is null , view :"+ ExtendUtil.debugViewLite(v));
      }
      //先从context中找
      if (root == null && v.getContext() instanceof HippyInstanceContext) {
        root = ((HippyInstanceContext) v.getContext()).findCurrentPageRootView();
      }
//      Log.i(TAG,"findPageRootView from context : "+root);
    }
    if(root != null){
      return root;
    }else{
      if (v != null && v.getId() > 0 && v.getId() > 5) { //rootView以外的才提示
        Log.w(HippyViewGroup.TAG, "findPageRootView from context fail, find from parent v:" + ExtendUtil.debugViewLite(v));
      }
      //return searchRootViewTraverse(v);
      return null;
    }
  }

  @Deprecated
  public static View findPageRootViewFromContext(Context context){
    View root = null;
    if(context instanceof HippyInstanceContext){
      root = ((HippyInstanceContext) context).findCurrentPageRootView();
    }
    return root;
  }

  /**
   * 向上遍历搜索RootView
   * @param v
   * @return
   */
  public static View searchRootViewTraverse(View v){
    if (v != null && ExtendViewGroup.ROOT_TAG.equals(v.getTag(R.id.page_root_view))) {
      Log.e(WindowNode.TAG,"searchRootViewTraverse return root view :"+ExtendUtil.debugViewLite(v));
      return v;
    } else {
      return v != null && v.getParent() instanceof View ? searchRootViewTraverse((View) v.getParent()) : null;
    }
  }

//  public static View findPageRootViewFromContext(Context context){
//    View root = null;
//    if(context instanceof HippyInstanceContext){
//      root = ((HippyInstanceContext) context).findCurrentPageRootView();
//    }
//    return root;
//  }


  /**
   *从引擎context中寻找window的RootView
   * @param context
   * @return
   */
  public static View findWindowRoot(HippyEngineContext context){
    if (context == null) {
      return null;
    }
    if(context.getDomManager() == null){
      return null;
    }
    View root = null;
    if(context.getRenderManager() != null && context.getRenderManager().getControllerManager() != null){
      final int rootID = context.getDomManager().getRootNodeId();
      final View hippyRoot = context.getRenderManager().getControllerManager().findView(rootID);
      if (hippyRoot != null) {
        root = hippyRoot.getRootView();
      }
    }
    return root;
  }

  public static int getDirectionFromDpadKey(KeyEvent e){
    return FocusUtils.getDirectionFromDpadKey(e);
  }


  public static String getDirectionName(int direction) {
    return FocusUtils.getDirectionName(direction);
  }

}
