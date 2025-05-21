package com.quicktvui.hippyext.views.fastlist;

import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.quicktvui.hippyext.views.TVTextView;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.uimanager.CustomControllerHelper;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;

import java.util.Set;

public final class FastAdapterUtil {

  public static final String TAG = "FastAdapterUtil";

  static void preGeneratePropsToUpdate(FastAdapter.ElementNode node, HippyMap itemData, HippyMap rawMap, HippyMap toMap){
    Set<String> props = node.pendingProps.keySet();
    for (String prop : props) {
      if (TemplateCodeParser.isEquationProp(rawMap.get(prop))) {//showIf="${detailStyle=2}"
        //将等式的值计算出来放进去
        final boolean b = TemplateCodeParser.parseBooleanFromPendingProp(prop, itemData, rawMap.get(prop));
        toMap.pushObject(prop, b);
      } else {
        final String pendingProp = TemplateCodeParser.parsePlaceholderProp(prop, rawMap);
        if (!TextUtils.isEmpty(pendingProp)) {
          //用item中的数据，替换map中的数据
          final Object dataFromValue = TemplateCodeParser.getValueFromCode(itemData, pendingProp);
          toMap.pushObject(prop, dataFromValue);
        }
      }
      //不是pending什么也不做
    }
    node.pendingProps = toMap;
  }

  static void preGeneratePropsToUpdateWithoutPendingProp(FastAdapter.ElementNode node, HippyMap itemData, HippyMap rawMap, HippyMap toMap){
    Set<String> props = node.pendingProps.keySet();
    for (String prop : props) {
//      Log.v("DebugReplaceItem","set prop "+prop+" value "+itemData.get(prop));
      if (TemplateCodeParser.isEquationProp(rawMap.get(prop))) {//showIf="${detailStyle=2}"
        //将等式的值计算出来放进去
        final boolean b = TemplateCodeParser.parseBooleanFromPendingProp(prop, itemData, rawMap.get(prop));
        toMap.pushObject(prop, b);
      } else {
        final String pendingProp = TemplateCodeParser.parsePlaceholderProp(prop, rawMap);
        if (!TextUtils.isEmpty(pendingProp)) {
          //用item中的数据，替换map中的数据
          final Object dataFromValue = TemplateCodeParser.getValueFromCode(itemData, pendingProp);
          toMap.pushObject(prop, dataFromValue);
        }
      }
      //不是pending什么也不做
    }
    node.pendingProps = toMap;
  }

  static void preInitElementNodeProps(FastAdapter.ElementNode node, HippyMap itemData, HippyMap rawMap, HippyMap toMap){
    preInitElementNodeProps(node,itemData,rawMap,toMap,false);
  }
   static void preInitElementNodeProps(FastAdapter.ElementNode node, HippyMap itemData, HippyMap rawMap, HippyMap toMap,boolean force){
    node.pendingProps = new HippyMap();
    Set<String> props = rawMap.keySet();
    for (String prop : props) { // 遍历的是模板的属性
      if(TemplateCodeParser.PENDING_PROP_CREATE_IF.equals(prop)){
        continue;
      }
      if (TemplateCodeParser.isEquationProp(rawMap.get(prop))) {//showIf="${detailStyle=2}"
        //将等式的值计算出来放进去
        final boolean b = TemplateCodeParser.parseBooleanFromPendingProp(prop, itemData, rawMap.get(prop));
        toMap.pushObject(prop, b);
        node.pendingProps.pushObject(prop, b);
      } else {//pending的属性 比如type="${itemType}"中的
        final String pendingProp = TemplateCodeParser.parsePlaceholderProp(prop, rawMap);
        // Log.d(TAG,"prepareItemViewAndCreatePatches pendingProp:"+pendingProp+",isTextProp:"+isTextProp+",isPendingProp:"+isPendingProp+",prop:"+prop);
        if (!TextUtils.isEmpty(pendingProp)) {// 获取的是pending属性或者eventClick、eventFouces
          //用item中的数据，替换map中的数据
          final Object dataFromValue = TemplateCodeParser.getValueFromCode(itemData, pendingProp);
          toMap.pushObject(prop, dataFromValue);
          node.pendingProps.pushObject(prop, dataFromValue);
//            if(itemTag != null) {
//              //反过来，把prop当value
//              itemTag.rawPendingPropsReverse.pushString(pendingProp,prop);
//            }
        } else if (!node.hasInit || force) {
          //直接复制一份
          toMap.pushObject(prop, rawMap.get(prop));
        } else if (LogUtils.isDebug()) {
          //Log.v("FastAdapterUtil", "diffPatch path return on hasCreated true prop:" + prop);
        }
      }
    }
     node.hasInit = true;
  }

  static View findFocusableView(View view){
    if(view != null && view.isFocusable()){
      return view;
    }
    if(view instanceof ViewGroup){
      for(int i = 0; i < ((ViewGroup) view).getChildCount(); i++){
        View child = ((ViewGroup) view).getChildAt(i);
        View result = findFocusableView(child);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  static boolean parseBoolean(Object object){
    if(object instanceof String){
      return Boolean.parseBoolean((String) object);
    }else if((object instanceof Boolean)){
      return (boolean) object;
    }
    return false;
  }

  static void  invokePropsSync(@Nullable HippyViewController vc, HippyMap props, int position, FastAdapter.ElementNode en, FastAdapter adapter,int step) {
    if (vc == null) {
      Log.e(TAG,"FastAdapterUtil invokePropsSync error ,viewController is null");
      return;
    }
//    if(LogUtils.isDebug()){
//      final String tag = getStepStr(step);
//      Log.e(tag,"----------------:invokePropsSync on templateNode "+en.templateNode+",view :"+en.boundView+"---------------");
//    }
    for (String prop : props.keySet()) {
      if (prop == null) {
        Log.e(TAG, "doDiffProps prop is null  en:" + en + ",position:" + position + ",vc:" + vc);
        continue;
      }
//      if(LogUtils.isDebug()) {
//        Log.v(TAG, "-----------------WorkLOG prop:" + prop + ",this:" + Utils.hashCode(adapter));
//      }
      if (prop.equals(NodeProps.STYLE) && props.get(prop) instanceof HippyMap) {
        invokePropsSync(vc, props.getMap(prop), position, en, adapter,step);
      } else {
        invokeProp(vc, props, prop, position, en, adapter,step);
      }
    }
  }

  static String getStepStr(int step){
    String stepPrefix;
    switch (step){
      case FastAdapter.STEP_INIT:
        stepPrefix = "STEP_INIT";
        break;
      case FastAdapter.STEP_ON_BIND:
        stepPrefix = "STEP_ON_BIND";
        break;
      case FastAdapter.STEP_DELAY_POST:
        stepPrefix = "STEP_DELAY_POST";
        break;
      case FastAdapter.STEP_VIF:
        stepPrefix = "STEP_VIF";
        break;
      case FastAdapter.STEP_PLACEHOLDER_TO_CONTENT:
        stepPrefix = "STEP_PLACEHOLDER";
        break;
      default:
        stepPrefix = "UNKNOWN";
        break;
    }
    return stepPrefix;
  }


  // 处理已经拿到的数据
  static void invokeProp(HippyViewController vc, HippyMap props, String prop, int position, FastAdapter.ElementNode en, FastAdapter adapter,int step) {

//    if (LogUtils.isDebug()) {
//      Log.i(TAG, "invokePropU position: " + position + " Prop:" + prop + ",templateNode:" + en.templateNode + ",vc:" + vc + ",view:" + en.boundView);
//    }
    if (en.boundView == null) {
      return;
    }
    if (vc instanceof PendingViewController && PendingViewController.PROP_LIST.equals(prop)) {
      if (en.templateNode instanceof FastAdapter.ListNode) {
        FastAdapter.ListNodeTag tag = ((FastAdapter.ListNode) en.templateNode).getBoundTag();
        if (tag == null) {
          tag = new FastAdapter.ListNodeTag();
          ((FastAdapter.ListNode) en.templateNode).setBoundTag(tag);
        }
//        if (LogUtils.isDebug()) {
//          Log.d(TAG, "setTagPosition:" + position + ",en:" + en + ",prop:" + prop);
//        }
        tag.position = position;
        tag.parent = adapter.listNode;
      }
      if (en.boundView instanceof FastPendingView) {
        //这里设置RootList，用住嵌套使用
        ((FastPendingView) en.boundView).setRootList(adapter.getRootListView(), adapter);
        ((FastPendingView) en.boundView).setHandleEventNodeId(adapter.rootListNodeID);
        if (adapter.getRootListView() != null) {
          ((FastPendingView) en.boundView).getEventDeliverer().setOnEventListener(adapter.getRootListView().getEventDeliverer().onEventListener);
        }
//        if (en.boundView instanceof FastListView) {
//          ((FastListView) en.boundView).setUseDiff(true);
//        }
      }
      final Object dataFromValue = props.get(prop);
      //fixme 这里嵌套list的时候，强制使用diff,有可能造成未知问题，后续需要优化
      ((PendingViewController) vc).setPendingData(en.boundView, dataFromValue, en.templateNode);
    } else { // 非嵌套list的情况处理
      if(en.boundView.isFocusable()){
       setOnElementListenerIfNeed(adapter,position,en);
      }
      if (!dispatchCustomPendingProp(prop, en, props, position, adapter)) { // 一些自定义属性的处理
        final Object dataFromValue = props.get(prop);
//        if("focusScale".equals(prop)){
//          Log.i("DebugPlaceholder","set focusScale value "+dataFromValue+",view : "+ExtendUtil.debugViewLite(en.boundView));
//        }

//        if (LogUtils.isDebug()) {
//          String stepPrefix =  getStepStr(step);
//          Log.i(stepPrefix, "invokePropMethodForPending position:" + position +",prop: "+prop+",targetView："+en.boundView+",dataFromValue:"+dataFromValue+",visibility:"+en.boundView.getVisibility());
//        }
        CustomControllerHelper.invokePropMethodForPending(vc, en.boundView, prop, dataFromValue); // 通常属性的处理
      }
    }
  }

  //设置view的点击和焦点事件
  static void setOnElementListenerIfNeed(FastAdapter adapter, int position, FastAdapter.ElementNode en){
    if(en.boundView.isFocusable()) {
      final View.OnClickListener clickListener = new FastAdapter.ElementClickListener(adapter, adapter.rootListNodeID, en, adapter.eventDeliverer);
      final View.OnLongClickListener longClickListener = new FastAdapter.ElementLongClickListener(adapter, adapter.rootListNodeID, en, adapter.eventDeliverer);
      if (LogUtils.isDebug()) {
        Log.e(TAG, "setOnElementListenerIfNeed ElementCallback setupClick ,boundView:" + en.boundView.hashCode() + ",viewName:" + en.name + ",clickListener:" + clickListener.hashCode()+",position:"+position);
      }
      en.boundView.setOnClickListener(clickListener);
      en.boundView.setOnLongClickListener(longClickListener);
      final FastAdapter.ItemFocusListener listener = new FastAdapter.ItemFocusListener(adapter, adapter.rootListNodeID, en, adapter.eventDeliverer);
      en.boundView.setOnFocusChangeListener(listener);
    }
  }

  static void setClickListener4BoundView(FastAdapter.ElementNode en, View.OnClickListener listener, View.OnLongClickListener longClickListener) {
    if (en.boundView != null) {
      if (en.rootNode != null && en.rootNode.boundView != null && en.rootNode.boundView.getParent() instanceof FastAdapter.ItemRootView) {
        final FastAdapter.ItemRootView irv = (FastAdapter.ItemRootView) en.rootNode.boundView.getParent();
//        irv.setChicl
        irv.setContentOnClickListener(listener);
      }else {
        en.boundView.setOnClickListener(listener);
        en.boundView.setOnLongClickListener(longClickListener);
      }
    }
  }





  static void removeFromParentIfNeed(View view){
    if (view != null && view.getParent() instanceof ViewGroup) {
      //删除view
        if(LogUtils.isDebug()) {
          Log.i(ReplaceChildView.TAG, "removeFromParentIfNeed view:" + ExtendUtil.debugView(view) + ",parent:" + ExtendUtil.debugView((View) view.getParent()));
        }
      ((ViewGroup) view.getParent()).removeView(view);
    }
  }

  static View getRealContent(FastAdapter.Holder holder){
    if(holder.itemView instanceof FastAdapter.ItemRootView){
        return ((FastAdapter.ItemRootView) holder.itemView).getContentView();
    }else{
      return holder.itemView;
    }
  }

  /**
   * 因为adapter中现在包了一层itemRootView，所以如果想获取一个itemView的实例，
   * 需要从ItemRootView中获取
   * @param view
   * @return
   */
  public static View finalRealContent(View view){
    if(view instanceof FastAdapter.ItemRootView){
      return ((FastAdapter.ItemRootView) view).getContentView();
    }else{
      return view;
    }
  }

  public static View findClonedViewByTemplateView(View view,FastAdapter.ElementNode node){
//    if(LogUtils.isDebug()) {
//      Log.i(FastListView.TAG_CLONED, "findClonedViewByTemplateView view :" + ",node:" + node.templateNode);
//    }
    if(node.templateNode != null){
        if(node.templateNode.getId() == view.getId()){
          if(LogUtils.isDebug()) {
            Log.e(FastListView.TAG_CLONED, "findClonedViewByTemplateView find target view:" + node.boundView);
          }
          return node.boundView;
        }
    }
    for(int i = 0; i < node.getChildCount(); i ++){
      FastAdapter.ElementNode child = (FastAdapter.ElementNode) node.getChildAt(i);
      final View result = findClonedViewByTemplateView(view,child);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  static void setDuplicateParentStateEnabled4AllParent(@Nullable View view, boolean b){
    if (view != null) {
      if(!view.isFocusable()) {
        Log.i("ItemContainerLog","setDuplicateParentStateEnabled4AllParent view : "+view);
        view.setDuplicateParentStateEnabled(b);
      }
      if (view.getParent() instanceof View) {
        setDuplicateParentStateEnabled4AllParent((View) view.getParent(),b);
      }
    }
  }

  static void setFocusListener4BoundView(FastAdapter.ElementNode en, FastAdapter.ItemFocusListener listener){
    if (en.boundView != null) {
      if (en.rootNode != null && en.rootNode.boundView != null && en.rootNode.boundView.getParent() instanceof FastAdapter.ItemRootView) {
        final FastAdapter.ItemRootView irv = (FastAdapter.ItemRootView) en.rootNode.boundView.getParent();
//        irv.setChicl
        irv.setContentItemFocusListener(listener);
      }else {
        en.boundView.setOnFocusChangeListener(listener);
      }
    }

  }

  static boolean dispatchCustomPendingProp(String prop, FastAdapter.ElementNode en, HippyMap props, int position, FastAdapter adapter) {
//    if (LogUtils.isDebug()) {
//      Log.i(TAG, "dispatchCustomPendingProp prop:" + prop + ",position:" + position);
//    }
    boolean handled = true;
    switch (prop) {
      case TemplateCodeParser.PENDING_PROP_TRANSLATION: {
        final Object dataFromValue = props.get(prop);
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchCustomPendingProp position dataFromValue:" + dataFromValue+",view:"+en.boundView);
        }
        final HippyArray posArray = (HippyArray) dataFromValue;
        if (posArray != null && posArray.size() == 2) {
          //tempPatchLayout.add(new UpdateLayoutPatch(posArray.getInt(0), posArray.getInt(1), boundNode.getWidth(), boundNode.getHeight(), p.view, boundNode));
          en.setMarginLeft(posArray.getInt(0));
          en.setMarginTop(posArray.getInt(1));
        }
      }
      break;
      case TemplateCodeParser.PENDING_PROP_SIZE: {
        final Object dataFromValue = props.get(prop);
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchCustomPendingProp size dataFromValue:" + dataFromValue+",view:"+en.boundView);
        }
        final HippyArray posArray = (HippyArray) dataFromValue;
        if (posArray != null && posArray.size() == 2) {
          //tempPatchLayout.add(new UpdateLayoutPatch(boundNode.getX(),boundNode.getY(),posArray.getInt(0), posArray.getInt(1), p.view, boundNode));
          en.setStyleWidth(Utils.toPX(posArray.getInt(0)));
          en.setStyleHeight(Utils.toPX(posArray.getInt(1)));
        }
      }
      break;
      case TemplateCodeParser.PENDING_PROP_LAYOUT: {
        final Object dataFromValue = props.get(prop);
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchCustomPendingProp layout dataFromValue:" + dataFromValue+",view:"+en.boundView);
        }
        final HippyArray posArray = (HippyArray) dataFromValue;
        if (posArray != null && posArray.size() == 4) {
          //tempPatchLayout.add(new UpdateLayoutPatch(posArray.getInt(0), posArray.getInt(1), posArray.getInt(2),posArray.getInt(3), p.view, boundNode));
          en.setMarginLeft(posArray.getInt(0));
          en.setMarginTop(posArray.getInt(1));
          en.setStyleWidth(Utils.toPX(posArray.getInt(2)));
          en.setStyleHeight(Utils.toPX(posArray.getInt(3)));
        }
      }
      break;
      case TemplateCodeParser.PENDING_PROP_FLEX_STYLE: {
        final Object dataFromValue = props.get(prop);
//        if (LogUtils.isDebug()) {
//          Log.i(TAG, "dispatchCustomPendingProp PROP_FLEX_STYLE layout dataFromValue:" + dataFromValue + ",node:" + en+",view:"+en.boundView);
//        }
        final HippyMap style = (HippyMap) dataFromValue;
        //Utils.updateStyleSlot(en,style);
        adapter.domUpdateManager.updateStyle(en, style);
        if(style != null) {
          //Log.i("fixElementNodeSize","flexStyle style :"+style);
          if (style.containsKey("left")) {
            en.setMarginLeft(getViewPXValueFromStyle(style, "left"));
          }
          if (style.containsKey("top")) {
            en.setMarginTop(getViewPXValueFromStyle(style, "top"));
          }
          if (style.containsKey("width")) {
           // Log.i("fixElementNodeSize","flexStyle getViewPXValueFromStyle width :"+getViewPXValueFromStyle(style, "width"));
            en.setStyleWidth(getViewPXValueFromStyle(style, "width"));
          }
          if (style.containsKey("height")) {
           // Log.i("fixElementNodeSize","flexStyle getViewPXValueFromStyle height :"+getViewPXValueFromStyle(style, "height"));
            en.setStyleHeight(getViewPXValueFromStyle(style, "height"));
          }
        }
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchCustomPendingProp PROP_FLEX_STYLE layout after dataFromValue:" + dataFromValue + ",node:" + en+",view:"+en.boundView);
        }
      }
      break;
      case TemplateCodeParser.PLACEHOLDER_STYLE:
        if (en.rootNode != null) {
          final Object dataFromValue = props.get(prop);
          if(dataFromValue instanceof HippyMap){
            en.rootNode.placeholderStyle = (HippyMap) dataFromValue;
          }
        }
        break;
      case TemplateCodeParser.ITEM_SID:
        if (en.rootNode != null) {
          final Object dataFromValue = props.get(prop);
//          Log.e("configID4Item","dispatchCustomPendingProp ITEM_SID dataFromValue:" + dataFromValue+",view:"+en.boundView);
          if(dataFromValue instanceof String){
            en.rootNode.itemSID = (String) dataFromValue;
          }else{
            en.rootNode.itemSID = "";
          }
        }
        break;
      case TemplateCodeParser.PENDING_PROP_EVENT_CLICK: {
        final Object dataFromValue = props.get(prop);
//        int parentPos = -1;
//        if(adapter.listNode != null && adapter.listNode.getBoundTag() != null){
//          parentPos = adapter.listNode.getBoundTag().position;
//        }
        final View.OnClickListener clickListener = new FastAdapter.ElementClickListener(adapter, adapter.rootListNodeID, en, adapter.eventDeliverer);
        final View.OnLongClickListener longClickListener = new FastAdapter.ElementLongClickListener(adapter, adapter.rootListNodeID, en, adapter.eventDeliverer);
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchCustomPendingProp ElementCallback setupClick dataFromValue:" + dataFromValue + ",boundView:" + en.boundView + ",viewName:" + en.name + ",clickListener:" + clickListener);
        }
        //en.boundView.setOnClickListener(clickListener);
        setClickListener4BoundView(en, clickListener, longClickListener);
      }
      break;
      case TemplateCodeParser.PENDING_PROP_EVENT_FOCUS: {
        final Object dataFromValue = props.get(prop);
        if (LogUtils.isDebug()) {
          Log.e(TAG, "dispatchCustomPendingProp ElementCallback setupFocus dataFromValue:" + dataFromValue + ",boundView:" + en.boundView + ",viewName:" + en.name);
        }
        final FastAdapter.ItemFocusListener listener = new FastAdapter.ItemFocusListener(adapter, adapter.rootListNodeID, en, adapter.eventDeliverer);
        //en.boundView.setOnFocusChangeListener(listener);
        setFocusListener4BoundView(en,listener);
      }
      break;
      case TemplateCodeParser.PENDING_PROP_SHOW_IF: {
        final Object dataFromValue = props.get(prop);
//        if (LogUtils.isDebug()) {
//          Log.i(TemplateCodeParser.TAG, "dispatchCustomPendingProp dataFromValue " + dataFromValue+",view:"+en.boundView);
//        }
        final boolean isEquationTrue = dataFromValue != null && (boolean) dataFromValue;
//        if (LogUtils.isDebug()) {
//          Log.i(TemplateCodeParser.TAG, "dispatchCustomPendingProp isEquationTrue " + isEquationTrue);
//        }
        changeViewShowIf(en.boundView, isEquationTrue);
      }
      default:
        handled = false;
        break;
    }
    if ("visibility".equals(prop)) {
      final Object dataFromValue = props.get(prop);
      if ("gone".equals(dataFromValue)) {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "doPatch4Prepare change visibility gone en:" + en);
        }
        Utils.clearLayout(en);
      }
    }
    return handled;
  }


  static void changeViewShowIf(View view, boolean b) {
    if (view != null) {
      view.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }
  }

  static Object getPropValue(RenderNode renderNode, int position, String prop, HippyMap itemData){
    final HippyMap rawMap = renderNode.getProps();
    Object value = null;
    if (TemplateCodeParser.isEquationProp(rawMap.get(prop))) {//showIf="${detailStyle=2}"
      //将等式的值计算出来放进去
      value = TemplateCodeParser.parseBooleanFromPendingProp(prop, itemData, rawMap.get(prop));
    } else {
      final String pendingProp = TemplateCodeParser.parsePlaceholderProp(prop, rawMap);
      if (!TextUtils.isEmpty(pendingProp)) {
        //用item中的数据，替换map中的数据
        value = TemplateCodeParser.getValueFromCode(itemData, pendingProp);
      }
    }
    return value;
  }

  static void findItemViewSize4FlexView(RenderNode templateNode, int position, HippyMap itemData, int[] layout){

    if (itemData.containsKey("width")) {
      layout[2] = Utils.toPX(itemData.getInt("width"));
    }
    if (itemData.containsKey("height")) {
      layout[3] = Utils.toPX(itemData.getInt("height"));
    }
    Log.i(TAG,"findItemViewSize findItemViewSize4FlexView layout position:"+position+",itemData:"+itemData);
  }

  static void notifyDetachFromParent(View view){
    if (view instanceof FastAdapter.ScrollTaskHandler) {
      ((FastAdapter.ScrollTaskHandler) view).notifyDetachFromParent();
      return;
    }
    //  //2023 11、14，FastList里嵌套时应该直接是一个list,但考虑到性能和功能平衡这里再深入一层
    if(view instanceof ViewGroup){
      for(int i = 0; i < ((ViewGroup) view).getChildCount(); i ++){
        if (((ViewGroup) view).getChildAt(i) instanceof FastAdapter.ScrollTaskHandler) {
          ((FastAdapter.ScrollTaskHandler) ((ViewGroup) view).getChildAt(i)).notifyDetachFromParent();
        }
      }
    }
  }

  static void notifyAttachToParent(View view){
//    Log.v(AutoFocusManager.TAG,"****FastList notifyAttachToParent view:"+view);
    if (view instanceof FastAdapter.ScrollTaskHandler) {
      ((FastAdapter.ScrollTaskHandler) view).notifyAttachToParent();
      return;
    }
    //2023 11、14，FastList里嵌套时应该直接是一个list,但考虑到性能和功能平衡这里再深入一层
    if(view instanceof ViewGroup){
      for(int i = 0; i < ((ViewGroup) view).getChildCount(); i ++){
        if (((ViewGroup) view).getChildAt(i) instanceof FastAdapter.ScrollTaskHandler) {
          ((FastAdapter.ScrollTaskHandler) ((ViewGroup) view).getChildAt(i)).notifyAttachToParent();
        }
      }
    }
  }

  static void findItemViewSize(RenderNode templateNode, int position, HippyMap itemData, int[] layout){
    if(itemData.containsKey("layout")){
      final HippyArray array = itemData.getArray("layout");
      for(int i =0; i < 4; i ++){
        layout[i] = Utils.toPX(array.getInt(i));
      }
//      if(LogUtils.isDebug()) {
//        Log.i(TAG, "findItemViewSize 1 layout position:" + position + ",itemData:" + itemData);
//      }
      return;
    }
    if (itemData.containsKey("size")) {
      final HippyArray array = itemData.getArray("size");
      layout[2] = Utils.toPX(array.getInt(0));
      layout[3] = Utils.toPX(array.getInt(1));
//      if(LogUtils.isDebug()) {
//        Log.i(TAG, "findItemViewSize 2 size position:" + position);
//      }
      return;
    }
    if (itemData.containsKey("flexStyle")) {
      final HippyMap styleMap = itemData.getMap("flexStyle");
      layout[0] = getViewPXValueFromStyle(styleMap,"left");
      layout[1] = getViewPXValueFromStyle(styleMap,"top");
      layout[2] = getViewPXValueFromStyle(styleMap,"width");
      layout[3] = getViewPXValueFromStyle(styleMap,"height");
//      if(LogUtils.isDebug()) {
//        Log.i(TAG, "findItemViewSize 3 size position:" + position);
//      }
    }

    if (layout[2] < 1 && itemData.containsKey("width")) {
//      if(LogUtils.isDebug()) {
//        Log.i(TAG, "findItemViewSize 4 size position:" + position);
//      }
      layout[2] = Utils.toPX(itemData.getInt("width"));
    }
    if (layout[3] < 1 && itemData.containsKey("height")) {
//      if(LogUtils.isDebug()) {
//        Log.i(TAG, "findItemViewSize 5 size position:" + position);
//      }
      layout[3] = Utils.toPX(itemData.getInt("height"));
    }

    if(layout[2] < 1 || layout[3] < 1) {
      if (templateNode != null && templateNode.getProps().containsKey("style")) {
        final HippyMap map = templateNode.getProps().getMap("style");
//        if(LogUtils.isDebug()) {
//          Log.i(TAG, "findItemViewSize 6 size position:" + position + ",itemData:" + itemData);
//        }
        if (layout[2] < 1) {
          layout[2] = getViewPXValueFromStyle(map,"width");
        }
        if (layout[3] < 1) {
          layout[3] = getViewPXValueFromStyle(map,"height");
        }
      }
    }else{
      if(LogUtils.isDebug()) {
        Log.i(TAG, "findItemViewSize 7 size position:" + position + ",itemData:" + itemData + ",height:" + layout[3]);
      }
    }
  }


  static int getViewPXValueFromStyle(HippyMap styleMap,String prop){
    return Utils.toPX(styleMap.getInt(prop)) ;
  }

  public static FastAdapter.ItemRootView findItemRootViewFromParent(View contentView){
    if (contentView == null) {
      return null;
    }
    if(contentView.getParent() instanceof FastAdapter.ItemRootView){
      return (FastAdapter.ItemRootView) contentView.getParent();
    }
    return null;
  }

  public static void updateLayout(View view, FastAdapter.ElementNode node) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
      //某些4.4设备存在崩溃
      try{
        final int lw = (int) node.getLayoutWidth();
        final int lh = (int) node.getLayoutHeight();
        final int x = (int) node.getLayoutX();
        final int y = (int) node.getLayoutY();
        if (view.getWidth() < 1 || view.getHeight() < 1 ||
          view.getWidth() != lw || view.getHeight() != lh) {
          //尺寸有变化
          if(view instanceof TVTextView && lh < 1){
            //bugfix:这里如果尺寸==0，证明尺寸不确定，将尺寸改为View.MeasureSpec.UNSPECIFIED
            view.measure(View.MeasureSpec.makeMeasureSpec(lw, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(lh, View.MeasureSpec.UNSPECIFIED));
          }else {
            view.measure(View.MeasureSpec.makeMeasureSpec(lw, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(lh, View.MeasureSpec.EXACTLY));
          }
          view.layout(x, y, x + lw, y + lh);
        } else if (view.getLeft() != x || view.getTop() != y) {
          view.layout(x, y, x + view.getWidth(), y + view.getHeight());
        }
      }catch (Throwable ignored){}
    }else {
      try {
        final int lw = (int) node.getLayoutWidth();
        final int lh = (int) node.getLayoutHeight();
        final int x = (int) node.getLayoutX();
        final int y = (int) node.getLayoutY();
//        if (LogUtils.isDebug()) {
//          if (view instanceof TVTextView) {
//            Log.i(TAG, "layoutTextView 1 text:" + ((TVTextView) view).getText() + ",lw : " + lw + ",lh :" + lh + ",x:" + x + ",y:" + y);
//          }
//        }
        if (view.getWidth() < 1 || view.getHeight() < 1 ||
          view.getWidth() != lw || view.getHeight() != lh) {
          //尺寸有变化
          if (view instanceof TVTextView && lh < 1) {
            //bugfix:这里如果尺寸==0，证明尺寸不确定，将尺寸改为View.MeasureSpec.UNSPECIFIED
            view.measure(View.MeasureSpec.makeMeasureSpec(lw, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(lh, View.MeasureSpec.UNSPECIFIED));
          } else {
            view.measure(View.MeasureSpec.makeMeasureSpec(lw, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(lh, View.MeasureSpec.EXACTLY));
          }
          view.layout(x, y, x + lw, y + lh);
//          if (LogUtils.isDebug()) {
//            if (view instanceof TVTextView) {
//              Log.i(TAG, "layoutTextView 2 text:" + ((TVTextView) view).getText() + ",lw : " + lw + ",lh :" + lh + ",x:" + x + ",y:" + y);
//            }
//          }
        } else if (view.getLeft() != x || view.getTop() != y) {
          view.layout(x, y, x + view.getWidth(), y + view.getHeight());
        }
      }catch (Throwable ignored){}
    }

  }

  public static void measureWidth(View view, int height) {
    view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
      View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
  }

  public static void measureHeight(View view, int width) {
    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
  }

  public static void updateLayoutF(View view, float x, float y, float width, float height) {
    updateLayout(view, (int) x, (int) y, (int) width, (int) height);
  }

  public static void updateLayout(View view, int x, int y, int width, int height) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
      //某些4.4设备存在崩溃
      try{
        if (view.getWidth() < 1 || view.getHeight() < 1 ||
          view.getWidth() != width || view.getHeight() != height) {
          //尺寸有变化
          view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
          view.layout(x, y, x + width, y + height);
        } else if (view.getLeft() != x || view.getTop() != y) {
          view.layout(x, y, x + view.getWidth(), y + view.getHeight());
        }
      }catch (Throwable ignored){}
    }else{
      try {
        if (view.getWidth() < 1 || view.getHeight() < 1 ||
          view.getWidth() != width || view.getHeight() != height) {
          //尺寸有变化

//          if (LogUtils.isDebug()) {
//            if (view instanceof TVTextView) {
//              Log.i(TAG, "layoutTextView 3 text:" + ((TVTextView) view).getText() + ",lw : " + width + ",lh :" + height + ",x:" + x + ",y:" + y);
//            }
//          }
          if (view instanceof TextView && height < 1) {
            //bugfix:这里如果尺寸==0，证明尺寸不确定，将尺寸改为View.MeasureSpec.UNSPECIFIED
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.UNSPECIFIED));
          } else {
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
          }
          view.layout(x, y, x + width, y + height);
        } else if (view.getLeft() != x || view.getTop() != y) {
          view.layout(x, y, x + view.getWidth(), y + view.getHeight());
        }
      }catch (Throwable ignored){}
    }

  }

  static void keepWithChildSize(StyleNode node){
      if(node.getChildCount() == 1){
        final StyleNode child = (StyleNode) node.getChildAt(0);
        if (LogUtils.isDebug()) {
          Log.i(TAG,"keepWithChildSize child :"+child.getLayoutWidth()+" node:"+node);
        }
//        node.(child.getStyleWidth());
//        node.setStyleHeight(child.getStyleHeight());
      }
  }

  public static HippyMap getRawItemFromFastList(Object item){
    if (item instanceof HippyMap) {
      return (HippyMap) item;
    }
    if (item instanceof FastAdapter.ItemEntity) {
      return ((FastAdapter.ItemEntity) item).getMap();
    }
    return null;
  }

  public static void replaceItem4FastAdapter(HippyArray array,int pos,Object item){
    if(array == null || array.size() == 0){
      return;
    }
    if(pos < 0 || pos >= array.size()){
      return;
    }
    if (array.get(pos) instanceof FastAdapter.ItemEntity) {
      if(item instanceof FastAdapter.ItemEntity) {
        if(LogUtils.isDebug()) {
          Log.e("DebugReplaceItem", "replaceItem4FastAdapter on pos " + pos + ",item:" + item);
        }
        array.setObject(pos, item);
//        ((FastAdapter.ItemEntity) item).updateItemDirty = true;
      }else {
        FastAdapter.ItemEntity ie = new FastAdapter.ItemEntity(item, pos);
        if(LogUtils.isDebug()) {
          Log.e("DebugReplaceItem", "replaceItem4FastAdapter on pos " + pos + ",ie:" + ie);
        }
//        ie.updateItemDirty = true;
        array.setObject(pos, ie);
      }
    }else {
      if(LogUtils.isDebug()) {
        Log.e("DebugReplaceItem", "replaceItem4FastAdapter on pos " + pos + ",item:" + item);
      }
      array.setObject(pos, item);
    }
  }


  public static boolean updateItemData4FastAdapterTraverse(String sid, Object newData, HippyArray dataList, boolean traverse, String post){
    if(dataList == null || dataList.size() == 0){
      return false;
    }
    for(int i = 0;i < dataList.size();i++){
      final Object io = dataList.get(i);
      HippyMap item = getRawItemFromFastList(io);
      if(item != null){
        final Object itemSid = getIDFromData(item);
        if(itemSid != null && itemSid.equals(sid)){
          //item.pushMap("data",data);
          //dataList.setObject(i,newData);
          replaceItem4FastAdapter(dataList,i,newData);
          if(LogUtils.isDebug()) {
            Log.e("DebugReplaceItem", "exe replace data itemSID:" + itemSid + ",post:" + post);
          }
          return true;
        }
      }
      if(traverse){
        HippyArray children = findDefaultChildrenFromItem(item);
        if(LogUtils.isDebug()) {
          Log.d("DebugReplaceItem", "updateItemDataBySidTraverse findDefaultChildrenFromItem children:" + children + "," + post + ",item:" + item);
        }
        if (children != null) {
          boolean b = updateItemData4FastAdapterTraverse(sid,newData,children,true,post);
          if (b) {
            return true;
          }
        }else if(item != null){
          //从所有列表里遍历
          for(String key : item.keySet()){
            Object value = item.get(key);
            if(value instanceof HippyArray){
              if(LogUtils.isDebug()) {
                Log.i("DebugReplaceItem", "search from item key:" + key);
              }
              boolean b = updateItemData4FastAdapterTraverse(sid,newData,(HippyArray) value,true,post);
              if (b) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  public static  HippyArray findDefaultChildrenFromItem(Object item){

    HippyArray children = null;
    HippyMap itemMap = getRawItemFromFastList(item);
    if (itemMap != null) {
      children = itemMap.getArray("children");
      if (children == null) {
        children = itemMap.getArray("itemList");
      }
    }
    return children;
  }

  static Object getIDFromData(Object item){

    Object id = null;
    HippyMap map = getRawItemFromFastList(item);
    if(map != null){
      HippyMap data = map;
      if (data.containsKey("id")) {
        //holder.itemView.setTag(R.id.tag_item_id, map.get("id"));
        id = data.get("id");
      }
      if (id == null) {
        if (data.containsKey("key")) {
          id =  data.get("key");
        }
      }
      if (id == null) {
        if (data.containsKey("_id")) {
          id =  data.get("_id");
        }
      }
      return id;
    }else{
      return null;
    }
  }



  public static int updateItemViewByItemID(View view,String id, Object data, boolean traverse) {
    //1. 通过sid找到需要更新的item的位置
    int pos = -1;
    if(view instanceof FastPendingView){
      FastPendingView listView = (FastPendingView) view;
      pos = listView.findItemPositionBySID(id);
    }
    //2. 通过updateItem()更新itemView
    if(pos > -1 ){
      ((FastPendingView)view).updateItem(pos,data,false);
      return pos;
    }else if(traverse && view instanceof ViewGroup){
      //没有找到对应的位置，在子view中查找
      ViewGroup viewGroup = (ViewGroup) view;
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        if(updateItemViewByItemID(child,id,data,true) > -1){
          return i;
        }
      }
    }
    return -1;
  }

  /**
   *
   * @param view
   * @param name
   * @param itemID
   * @param prop
   * @param updateView
   * @return
   */
  public static int updateItemPropsByItemID(View view,String name,String itemID,String prop,Object newValue,boolean updateView) {
    //1. 通过sid找到需要更新的item的位置
    int pos = -1;
    if(view instanceof FastPendingView){
      FastPendingView listView = (FastPendingView) view;
      pos = listView.findItemPositionBySID(itemID);
    }
    //2. 通过updateItem()更新itemView
    if(pos > -1 ){
      ((FastPendingView)view).updateItemSpecificProp(name,pos,prop,newValue,updateView);
      return pos;
    }else if(view instanceof ViewGroup){
      //没有找到对应的位置，在子view中查找
      ViewGroup viewGroup = (ViewGroup) view;
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        if(updateItemPropsByItemID(child,name,itemID,prop,newValue,updateView) > -1){
          return i;
        }
      }
    }
    return -1;
  }


}
