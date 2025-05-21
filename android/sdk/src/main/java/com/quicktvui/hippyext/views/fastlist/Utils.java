package com.quicktvui.hippyext.views.fastlist;


import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.quicktvui.hippyext.views.TextViewController;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.DomUpdateManager;
import com.tencent.mtt.hippy.dom.flex.FlexSpacing;
import com.tencent.mtt.hippy.dom.node.DomNode;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.CustomControllerHelper;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;

import java.util.ArrayList;

public class Utils {

  static final String KEY_SINGLETON = "singleton";
  static final String KEY_CONTENT_DELAY = "delayLoad";
  static final String KEY_PLACEHOLDER_VIEW = "placeholderView";

  public static final String HASFOCUS = "hasFocus";//是否含有焦点
  public static final String ORIENTATION = "orientation";//方向
  public static final String ITEMCOUNT = "itemCount";//item数量
  public static final String FOCUS_POSITION = "focusPosition";//当前焦点position
  public static final String SELECT_POSITION = "selectPosition";//当前选择position
  public static final String OFFSETX = "offsetX";//x轴移动距离
  public static final String OFFSETY = "offsetY";//y轴移动距离
  public static final String WIDTH = "width";//宽度
  public static final String HEIGHT = "height";//高度
  public static final String SCROLLSTATE = "scrollState";//滑动状态 0:SCROLL_STATE_IDLE  1:SCROLL_STATE_DRAGGING  2:SCROLL_STATE_SETTLING
  public static final String IS_SCROLL_UP = "isScrollUp";//是否向上滚动
  public static final String IS_SCROLL_LEFT = "isScrollLeft";//是否向左滚动

  static View createViewByNode(RenderNode node) {
    return null;
  }

  static void copyNodeTree(FastAdapter.ElementNode target, DomNode source) {
    cloneNode(target, source);
    for (int i = 0; i < source.getChildCount(); i++) {
      final DomNode sc = source.getChildAt(i);
      FastAdapter.ElementNode tc = new FastAdapter.ElementNode();
      target.addChildAt(tc, i);
      copyNodeTree(tc, sc);
    }
  }

  static void updateStyleSlot(FastAdapter.ElementNode node, HippyMap style, DomUpdateManager domUpdateManager) {


  }

  static HippyEngineContext getHippyContext(Context context) {
    HippyEngineContext engineContext = ((HippyInstanceContext) context).getEngineContext();
    return engineContext;
  }


  static void buildEventBasic(HippyMap m, View view, FastAdapter.ElementNode node, int itemPosition) {
    if (view.getParent() instanceof ViewGroup) {
      ViewGroup p = (ViewGroup) view.getParent();
      m.pushInt("index", p.indexOfChild(view));
    }
//    if(LogUtils.isDebug()) {
//      Log.i("DebugEvent", "buildEventBasic itemPosition: " + itemPosition
//        + ",elementNode adapterPosition:" + node.rootNode.adapterPosition);
//    }
//    int position = itemPosition;
//    if(node.rootNode != null && node.rootNode.adapterPosition > -1){
//      if(position != node.rootNode.adapterPosition){
//        Log.w("DebugEvent","fix from position:"+position+" to "+node.rootNode.adapterPosition);
//      }
//      position = node.rootNode.adapterPosition;
//    }
    m.pushString("sid",ExtendUtil.getViewSID(view));
    m.pushInt("position", itemPosition);
  }

  static void setNode(View view, FastAdapter.ElementNode node) {
    view.setTag(R.id.tag_view_node, node);
  }

  static FastAdapter.ElementNode getNode(View view) {
    final Object tag = view.getTag(R.id.tag_view_node);
    if (tag != null) {
      return (FastAdapter.ElementNode) tag;
    }
    return null;
  }


  static String getNameFromProps(HippyMap map) {
    if (map != null) {
      return map.getString("name");
    }
    return null;
  }

  static boolean containKey(HippyMap map, String key) {
    return map != null && map.containsKey(key);
  }

  static boolean containKey(RenderNode node, String key) {
    return node != null && containKey(node.getProps(), key);
  }


  static Object getPropFromProps(RenderNode node, String prop) {
    if (node.getProps() != null && node.getProps().containsKey(prop)) {
      return node.getProps().get(prop);
    }
    return null;
  }

  public static View findBoundView(HippyEngineContext context, RenderNode node) {
    if (node == null) {
      return null;
    }
    if (context != null && context.getRenderManager() != null && context.getRenderManager().getControllerManager() != null) {
      return context.getRenderManager().getControllerManager().findView(node.getId());
    }
    return null;
  }

  public static View findBoundViewByNodeID(HippyEngineContext context, int nodeID) {
    if (context != null && context.getRenderManager() != null && context.getRenderManager().getControllerManager() != null) {
      return context.getRenderManager().getControllerManager().findView(nodeID);
    }
    return null;
  }

  public final static int ROUGH_PERFORMANCE_LOW = 0;
  public final static int ROUGH_PERFORMANCE_MEDIUM = 1;
  public final static int ROUGH_PERFORMANCE_HIGH = 2;

  /**
   * 使用内存来粗略估算机器配置
   * @param context
   * @return
   */
  public static int getRoughPerformance(Context context){
    int level = ROUGH_PERFORMANCE_MEDIUM;
    try {
//      2083119104
      ActivityManager.MemoryInfo memInfo = getMemoryInfo(context);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        Log.e("FAST_UTILS","memInfo total :"+memInfo.totalMem);
        if(memInfo.totalMem > 2300000000L){//2G
          level = ROUGH_PERFORMANCE_HIGH;
        }else if(memInfo.totalMem < 800000000L){ // < 800M
          level = ROUGH_PERFORMANCE_LOW;
        }
      }else{
        //小于等于4.2直接设置为低端版本
        level = ROUGH_PERFORMANCE_LOW;
      }
    }catch (Throwable t){}
    return level;
  }

  public static ActivityManager.MemoryInfo getMemoryInfo(Context context) {
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
    am.getMemoryInfo(mi);
    return mi;
  }

  static void cloneNodeLayout(DomNode c, DomNode o) {
    if (o == null || c == null) {
      return;
    }
    c.setViewClassName(o.getViewClass());

    if (o.getStyleWidth() > 0) {
      c.setStyleWidth(o.getStyleWidth());
    }
    if (o.getStyleHeight() > 0) {
      c.setStyleHeight(o.getStyleHeight());
    }
    for (int i = 0; i < 4; i++) {
      c.setMargin(i, o.getMargin(i));
      c.setPadding(i, o.getPadding(i));
      c.setBorder(i, o.getBorder(i));
    }


  }

  public static void resetNode(DomNode c, DomNode o) {
    if (o == null || c == null) {
      return;
    }
    if (o.getStyleWidth() < 1) {
      c.setStyleWidth(Float.NaN);
    }
    if (o.getStyleHeight() < 1) {
      c.setStyleHeight(Float.NaN);
    }
    for (int i = 0; i < 4; i++) {
      c.setMargin(i, o.getMargin(i));
      c.setPadding(i, o.getPadding(i));
      c.setBorder(i, o.getBorder(i));
    }
  }

  static void cloneNode(DomNode c, DomNode o) {
    if (o == null || c == null) {
      return;
    }
    c.setViewClassName(o.getViewClass());
    c.setDirection(o.getDirection());
    c.setFlexDirection(o.getFlexDirection());
    c.setJustifyContent(o.getJustifyContent());
    c.setAlignContent(o.getAlignContent());
    c.setAlignItems(o.getAlignItems());
    c.setAlignSelf(o.getAlignSelf());
    c.setPositionType(o.getPositionType());
    if (o.getPosition(FlexSpacing.TOP) != 0) {
      c.setPosition(FlexSpacing.TOP, o.getPosition(FlexSpacing.TOP));
    }
    if (o.getPosition(FlexSpacing.LEFT) != 0) {
      c.setPosition(FlexSpacing.LEFT, o.getPosition(FlexSpacing.LEFT));
    }
    if (o.getPosition(FlexSpacing.RIGHT) != 0) {
      c.setPosition(FlexSpacing.RIGHT, o.getPosition(FlexSpacing.RIGHT));
    }
    if (o.getPosition(FlexSpacing.BOTTOM) != 0) {
      c.setPosition(FlexSpacing.BOTTOM, o.getPosition(FlexSpacing.BOTTOM));
    }
    c.setWrap(o.Style().getWrap());
    c.setOverflow(o.getOverflow());
    c.setFlexGrow(o.getFlexGrow());
    c.setFlexShrink(o.getFlexShrink());
    c.setFlexBasis(o.getFlexBasis());
    if (o.getStyleWidth() > 0) {
      c.setStyleWidth(o.getStyleWidth());
    }
    if (o.getStyleHeight() > 0) {
      c.setStyleHeight(o.getStyleHeight());
    }
    for (int i = 0; i < 4; i++) {
      c.setMargin(i, o.getMargin(i));
      c.setPadding(i, o.getPadding(i));
      c.setBorder(i, o.getBorder(i));
    }
  }

  public static int toPX(int value) {
    return (int) PixelUtil.dp2px(value);
  }


  public static RenderNode getRenderNode(View view) {
    return getHippyContext(view).getRenderManager().getRenderNode(view.getId());
  }

  public static HippyEngineContext getHippyContext(View view) {
    return ((HippyInstanceContext) view.getContext()).getEngineContext();
  }


  public static void updateProps(View view, RenderNode templateNode, HippyMap props) {
    //根据模版和view更新属性
  }


  static boolean isPendingListNode(RenderNode node) {
    return (node instanceof FastAdapter.ListNode);
  }

  static boolean isPendingItemNode(RenderNode node) {
    return (node instanceof FastItemNode);
  }

  static String hashCode(Object object) {
    if (object == null) {
      return null;
    }
    return Integer.toHexString(object.hashCode());
  }

  public static void clearLayout(FastAdapter.ElementNode c) {
    if (c == null) {
      return;
    }
    c.setStyleWidth(0);
    c.setStyleHeight(0);
    for (int i = 0; i < 4; i++) {
      c.setMargin(i, 0);
      c.setPadding(i, 0);
      c.setBorder(i, 0);
    }
  }


  public static void updatePendingPropData(HippyMap itemToUpdate, HippyMap rawPendingPropsReverse, HippyMap propsToUpdate) {
    if (rawPendingPropsReverse != null && itemToUpdate != null && propsToUpdate != null) {
      for (String pendingProp : propsToUpdate.keySet()) {
        TemplateCodeParser.setValueFromCode(itemToUpdate, pendingProp, propsToUpdate.get(pendingProp));
      }
    }
  }

  public static FastAdapter.ElementNode findElementNodeByName(String name, FastAdapter.ElementNode node) {
    if (node.name != null && node.name.equals(name)) {
      return node;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      final FastAdapter.ElementNode child = (FastAdapter.ElementNode) node.getChildAt(i);
      final FastAdapter.ElementNode target = findElementNodeByName(name, child);
      if (target != null) {
        return target;
      }
    }
    return null;
  }

  public static FastAdapter.ElementNode findElementNodeBySID(String sid, FastAdapter.ElementNode node) {
//    if (node.name != null && node.name.equals(name)) {
//      return node;
//    }
//    for (int i = 0; i < node.getChildCount(); i++) {
//      final FastAdapter.ElementNode child = (FastAdapter.ElementNode) node.getChildAt(i);
//      final FastAdapter.ElementNode target = findElementNodeByName(name, child);
//      if (target != null) {
//        return target;
//      }
//    }
//    return null;
    //TODO
    return null;
  }

  /**
   * 判断一个view是不是placeholderView
   * @param view
   * @return
   */
  public static boolean isPlaceholderContainerView(View view){
    return view instanceof FastAdapter.ItemRootView;
  }

  /**
   * 判断一个view的parent是不是tvList中的一个ItemRootView
   * @param view
   * @return
   */
  public static boolean isParentItemRootView(View view){
    if(view != null && view.getParent() instanceof FastAdapter.ItemRootView){
      return true;
    }
    return false;
  }

  /**
   * 通过一个view获取他的placeholderView
   * @param contentView
   * @return
   */
  public static View getPlaceholderContainer(View contentView){
    if (contentView != null && contentView.getParent() instanceof FastAdapter.ItemRootView) {
      return (View) contentView.getParent();
    }
    return null;
  }

  /**
   *
   * @param view
   * @param id
   * @return
   */
  public static View  findItemViewByID(View view,String id){
    if (TextUtils.isEmpty(id)) {
      return null;
    }
    if(id.equals(ExtendUtil.getViewSID(view))){
      return view;
    }
    if(view instanceof ViewGroup){
      for(int i =0 ; i < ((ViewGroup) view).getChildCount(); i ++){
        View v = findItemViewByID(((ViewGroup) view).getChildAt(i),id);
        if (v != null) {
          return v;
        }
      }
    }
    return null;
  }

  public static String findPropByPendingProp(String pendingProp, FastAdapter.ElementNode node) {
    if (node != null && pendingProp != null) {
      final RenderNode templateNode = node.templateNode;
      if (templateNode != null && templateNode.getProps() != null) {
        for (String key : templateNode.getProps().keySet()) {
          final String valueKey = TemplateCodeParser.parsePendingProp(templateNode.getProps().get(key));
          if (pendingProp.equals(valueKey)) {
            return key;
          }
        }
      }
    }
    return null;
  }

  public static int searchFastAdapterData(HippyArray array,Object id,String key){
    int index = -1;
    if (array == null || id == null || TextUtils.isEmpty(key)) {
      return -1;
    }
    for(int i = 0 ; i < array.size(); i ++){
      final FastAdapter.ItemEntity ie = (FastAdapter.ItemEntity) array.get(i);
      if (ie.raw instanceof HippyMap) {
        final HippyMap item = (HippyMap) ie.raw;
        if(item.get(key) != null && item.get(key).equals(id)){
          index = i;
          break;
        }else{
//          Log.i("RenderUtil","searchFastAdapterData 1 skip!!! i:"+i+",item:"+item.get(key)+",item:"+item+",key:"+key);
        }
      }else{
//        Log.e("RenderUtil","searchFastAdapterData error raw is not hippyMap:"+ie.raw);
      }
    }
    return index;
  }




  public static boolean isWrapContent(FastAdapter.ElementNode node) {
    return node.templateNode.getProps() != null && node.templateNode.getProps().containsKey("wrapContent");
  }

  public static boolean isWrapWidth(FastAdapter.ElementNode node) {
    return node.templateNode.getProps() != null && node.templateNode.getProps().containsKey("autoWidth");
  }

  public static boolean isWrapHeight(FastAdapter.ElementNode node) {
    return node.templateNode.getProps() != null && node.templateNode.getProps().containsKey("autoHeight");
  }


  public static View createViewImpl4FastList(Context context, HippyMap iniProps, HippyViewController controller) {
    if (controller instanceof TextViewController) {
      return ((TextViewController) controller).createViewImpl(context, iniProps, true);
    }
    return CustomControllerHelper.createViewImpl(context, iniProps, controller);
  }


  public static void dispatchFunction(ControllerManager cm, FastAdapter.ElementNode targetNode, String functionTargetName, HippyArray array, Promise promise) {
    HippyViewController hippyViewController = CustomControllerHelper.getViewController(cm, targetNode.templateNode);
    final View view = targetNode.boundView;
    if (hippyViewController != null && view != null) {
      if (promise == null || !promise.isCallback()) {
        hippyViewController.dispatchFunction(view, functionTargetName, array);
      } else {
        hippyViewController.dispatchFunction(view, functionTargetName, array, promise);
      }
    } else {
      Log.e("Utils", "dispatchFunction error, view:" + view + ",controller:" + hippyViewController + ",functionTargetName:" + functionTargetName);
    }
  }

  public static int findParentPositionByView(View v, int level) {
    if(LogUtils.isDebug()) {
      Log.i("ElementCallback", "findParentPositionByView " + level + ",view :" + ExtendUtil.debugView(v));
    }
    final ViewParent parent = v.getParent();
    if (parent instanceof FastPendingView) {
      if (level == 1) {
        //找到对应view
        return ((FastPendingView) parent).findPositionByChild(v);
      }
      if (level == 0) {
        return findParentPositionByView((View) parent, level + 1);
      }
    }
    if (parent instanceof ViewGroup) {
      return findParentPositionByView((View) parent, level);
    }
    return -1;
  }

  //获取fastlist状态
  public static HippyMap getFastListState(FastListView fastListView) {
    HippyMap hippyMap = new HippyMap();
    if (fastListView != null) {
      hippyMap.pushBoolean(HASFOCUS, fastListView.hasFocus());
      hippyMap.pushInt(ORIENTATION, fastListView.getLayoutManagerCompat().getOrientation());
      hippyMap.pushInt(ITEMCOUNT, fastListView.getFastAdapter() != null ? fastListView.getFastAdapter().getItemCount() : 0);
      hippyMap.pushInt(FOCUS_POSITION,fastListView.getFocusedChild() != null ?
        fastListView.getChildAdapterPosition(fastListView.getFocusedChild()) : -1);
      hippyMap.pushInt(SELECT_POSITION, fastListView.getSelectChildPosition());
      hippyMap.pushInt(OFFSETX, fastListView.getOffsetX());
      hippyMap.pushInt(OFFSETY, fastListView.getOffsetY());
      hippyMap.pushInt(WIDTH, fastListView.getMeasuredWidth());
      hippyMap.pushInt(HEIGHT, fastListView.getMeasuredHeight());
      hippyMap.pushInt(SCROLLSTATE, fastListView.getScrollState());
      hippyMap.pushBoolean(IS_SCROLL_UP, fastListView.getLayoutManagerCompat().getExecutor().isScrollUp());
      hippyMap.pushBoolean(IS_SCROLL_LEFT, fastListView.getLayoutManagerCompat().getExecutor().isScrollLeft());
    }
    return hippyMap;
  }

  //获取fastlist状态
  public static void getFastListState(FastListView fastListView,HippyMap hippyMap) {
    if (fastListView != null) {
      hippyMap.pushInt(ORIENTATION, fastListView.getLayoutManagerCompat().getOrientation());
      hippyMap.pushInt(ITEMCOUNT, fastListView.getFastAdapter() != null ? fastListView.getFastAdapter().getItemCount() : 0);
      hippyMap.pushInt(FOCUS_POSITION,fastListView.getFocusedChild() != null ?
        fastListView.getChildAdapterPosition(fastListView.getFocusedChild()) : -1);
      hippyMap.pushInt(SELECT_POSITION, fastListView.getSelectChildPosition());
      hippyMap.pushInt(OFFSETX, fastListView.getOffsetX());
      hippyMap.pushInt(OFFSETY, fastListView.getOffsetY());
      hippyMap.pushInt(SCROLLSTATE, fastListView.getScrollState());
      hippyMap.pushBoolean(IS_SCROLL_UP, fastListView.getLayoutManagerCompat().getExecutor().isScrollUp());
      hippyMap.pushBoolean(IS_SCROLL_LEFT, fastListView.getLayoutManagerCompat().getExecutor().isScrollLeft());
    }
  }

  public static void findSameRowChildrenInGridLayout(View[] views, View targetView, @NonNull ArrayList<View> resultList, int tolerance, boolean horizontal){

//    Log.i("ZHAOPENG","findSameRowViewsInGridLayout tolerance:" + tolerance + ",horizontal:" + horizontal+",targetView:"+targetView);
    if ( views == null || targetView == null) {
      return;
    }
    for (View view : views) {
      if(view == targetView){
        continue;
      }
      if (view != null && view.getVisibility() == View.VISIBLE) {
        Rect rect = new Rect();
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        //简单的判断，只要俩个view的边界和目标view的边界相差在tolerance范围内，就认为是同一行
        if (horizontal) {
          if (Math.abs(targetView.getLeft() - rect.left) <= tolerance && Math.abs(targetView.getRight() - rect.right) <= tolerance) {
            resultList.add(view);
          }
        } else {
          if (Math.abs(targetView.getTop() - rect.top) <= tolerance && Math.abs(targetView.getBottom() - rect.bottom) <= tolerance) {
            resultList.add(view);
          }
        }
      }
    }

  }


}
