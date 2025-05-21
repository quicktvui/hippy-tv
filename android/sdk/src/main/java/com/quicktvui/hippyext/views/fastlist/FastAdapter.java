package com.quicktvui.hippyext.views.fastlist;

import static com.quicktvui.hippyext.views.fastlist.Utils.ROUGH_PERFORMANCE_MEDIUM;
import static com.quicktvui.hippyext.views.fastlist.Utils.getRoughPerformance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.base.ui.IPageRootView;
import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.hippyext.FocusManagerModule;
import com.quicktvui.base.ui.ExtendTag;
import com.quicktvui.base.ui.IRecyclerItemView;
import com.quicktvui.base.ui.JSEventHandleView;
import com.quicktvui.hippyext.views.TVTextView;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.DomUpdateManager;
import com.tencent.mtt.hippy.dom.flex.FlexConstants;
import com.tencent.mtt.hippy.dom.node.DomNode;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.CustomControllerHelper;
import com.tencent.mtt.hippy.uimanager.DiffUtils;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.image.HippyImageView;
import com.tencent.mtt.hippy.views.image.HippyImageViewController;
import com.tencent.mtt.hippy.views.image.IImageStateListener;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.smtt.flexbox.FlexNodeStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FastAdapter extends RecyclerView.Adapter<FastAdapter.Holder> {

  HippyArray dataList;
  ListNode listNode;
  Map<Integer, RenderNode> templateNodes;
  Map<Integer, RenderNode> itemStoreNodes;

  HippyEngineContext context;
  Context viewContext;

  static Map<String, CachePool> gSharedCachePools;

  DomUpdateManager<ElementNode> domUpdateManager;

  FastFlexView mBoundFlexView;
  FastListView mBoundListView;
  FastListView mRootListView;
  int rootListNodeID = -1;

  String mShareItemStoreName = null;

  FocusDispatchView rootView;
  View pageRootView;
  int postDelay = 200;
  int placeholderPostDelay = 100;
  int placeholderBackgroundColor;
  float placeholderFocusScale = FocusManagerModule.defaultPlaceholderFocusScale;
  float placeholderBorderRadius = 0;

  private String mUserKeyName = null;




  public void setShareItemStoreName(String mShareItemStoreName) {
    this.mShareItemStoreName = mShareItemStoreName;
  }

  EventDeliverer eventDeliverer;
  //默认嵌套子List的placeholder是否开启
  public static boolean CHILD_LIST_PLACEHOLDER_ENABLE = false;

  //发送事件时，是否传递item数据
  private boolean isEventSendItem = true;

  private boolean enablePlaceholder = false;

  static int rough_performance_level = -1;

  ArrayMap<Integer,Holder> mDetachedChildren;

  boolean isInfiniteLoop = false;

  //10,0000,0000
  final static int INFINITE_START_POSITION = 1000000000;


  private boolean isInfiniteLoopEnabled(){
    return isInfiniteLoop;
  }

  public static RenderNode findRenderNodeFromView(View v) {
    if (v == null) {
      return null;
    }
    if (v instanceof ItemContainer) {
      v = ((ItemContainer) v).getContentView();
    }
    Object tag = ExtendTag.getExtendTag(v);
    if (tag instanceof ClonedViewTag) {
      final ClonedViewTag c = (ClonedViewTag) tag;
      return c.originNode;
    }
    return null;
  }

  public int getRoughPerformanceLevel(Context context) {
    if(rough_performance_level < 0){
      rough_performance_level = getRoughPerformance(context);
    }
    return rough_performance_level;
  }

  public FastAdapter() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      placeholderBackgroundColor = Color.argb(0.1f,1,1,1);
    }else{
      placeholderBackgroundColor = Color.argb((int) ((255 + 0.5) * 0.1f),255,255,255);
    }
    placeholderBorderRadius = 8;
  }

  public static void updateLayoutF(View view, float x, float y, float width, float height) {
    FastAdapterUtil.updateLayout(view, (int) x, (int) y, (int) width, (int) height);
  }


  public void setEnablePlaceholder(boolean enablePlaceholder) {
    this.enablePlaceholder = enablePlaceholder;
  }

  public boolean isEnablePlaceholder() {
    return enablePlaceholder;
  }

  public void setEventSendItem(boolean eventSendItem) {
    isEventSendItem = eventSendItem;
  }

  //item点击事件接口、item焦点事件
  public OnFastItemClickListener onFastItemClickListener;
  public OnFastItemFocusChangeListener onFastItemFocusChangeListener;

  public static int POST_TASK_CATEGORY_DELAY_LOAD = PostTaskHolder.POST_TASK_CATEGORY_DELAY_LOAD;
  public static int POST_TASK_CATEGORY_UPDATE_LAYOUT = PostTaskHolder.POST_TASK_CATEGORY_UPDATE_LAYOUT;
  public static int POST_TASK_CATEGORY_PLACEHOLDER_TO_CONTENT = PostTaskHolder.POST_TASK_CATEGORY_PLACEHOLDER_TO_CONTENT;
  private static final String CACHE_KEY = "FastCache";


  public ControllerManager getControllerManager() {
    assert context != null;

    return context.getRenderManager().getControllerManager();
  }

  public static void clearGlobalCache() {
    if (gSharedCachePools != null) {
      gSharedCachePools.clear();
    }
  }

  public FastPendingView getFastPendingView() {
    return mBoundListView != null ? mBoundListView : mBoundFlexView;
  }

  public int getRootListNodeID() {
    return rootListNodeID;
  }

  private String shareViewPoolType = null;
  //  public static final String TAG = "FastAdapter";
  public static final String TAG = "FastListAdapter";
  public static final String TAG_PERFORMANCE = "WorkLOG";
  public static final String TAG_POST = "FastPostTask";

  void onAttachedToView(FastPendingView view) {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "FastAdapterEvent onAttachToView ,adapter:" + Utils.hashCode(this) + ",view:" + Utils.hashCode(view));
    }


  }

  void onDetachedFromView(FastPendingView view) {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "FastAdapterEvent onDettachToView ,adapter:" + Utils.hashCode(this) + ",view:" + Utils.hashCode(view));
    }
    clearDetachCaches();
  }

  public void setShareViewPoolType(String shareViewPoolType) {
    this.shareViewPoolType = shareViewPoolType;
  }

  FastListModule.GlobalConfig globalConfig;
  public void setContext(HippyEngineContext context,Context viewContext) {
    this.context = context;
    this.viewContext = viewContext;
    this.rootView = context.getInstance(HippyRootView.ROOT_VIEW_TAG_INCREMENT);
    if (LogUtils.isDebug()) {
      Log.i(TAG, "DebugPool getCacheWorker init rootView:" + rootView);
    }
    globalConfig = FastListModule.getGlobalConfig(context);
    placeholderBorderRadius = globalConfig.placeholderBorderRadius;
  }

  private void onFixDevicePerformance(int level){
    if(level  > ROUGH_PERFORMANCE_MEDIUM){
      placeholderPostDelay = 50;
    }else if(level < ROUGH_PERFORMANCE_MEDIUM){
      placeholderPostDelay = 200;
    }
    if(LogUtils.isDebug()) {
      Log.i(TAG, "fixDevicePerformance level:" + level + ",set placeholderPostDelay:" + placeholderPostDelay);
    }
  }


  public View findClonedViewByTemplateView(int position, View template){
    FastListView list = mBoundListView;
    if(LogUtils.isDebug()) {
      int itemCount = list.getAdapter() == null ? -1 : list.getAdapter().getItemCount();
      Log.i(FastListView.TAG_CLONED, "findClonedViewByTemplateView position:" + position + ",list :" + list+",childCount:"+itemCount+",listID:"+list.getId());
      Log.i(FastListView.TAG_CLONED, "findClonedViewByTemplateView position:" + position + ",findViewByPosition :" + list.findViewByPosition(position));
    }
    RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(position);
    if(holder instanceof FastAdapter.Holder){
      return FastAdapterUtil.findClonedViewByTemplateView(template,((Holder) holder).tag.getRootNode());
    }else{
      Log.e(FastListView.TAG_CLONED,"findClonedViewByTemplateView error holder :"+holder);
    }
    return null;
  }

  public RenderNode getTemplateNodeByType(int viewType){
    if (templateNodes != null) {
      return templateNodes.get(viewType);
    }
    return null;
  }

  View createItemContainerView(ViewGroup parent, int viewType, ViewTag tag, boolean isSingleton, View templateView){
    ElementNode itemNode = tag.getRootNode();
    if (isSingleton) {
      Log.i(TAG,"crateItemContainerView itemNode template :"+itemNode.templateNode+",templateView : "+templateView);
    }
    if(!isSingleton && itemNode.boundView instanceof FastItemView){
      final FastItemView fiv = (FastItemView) itemNode.boundView;
      //Log.i(TAG,"crateItemContainerView tag.getRootNode() :"+tag.getRootNode()+",tag.getRootNode().initConfig.isViewFocusable :"+tag.getRootNode().initConfig.isViewFocusable);
      if(enablePlaceholder) {
        fiv.setEnablePlaceholder(true);
        if(!itemNode.initConfig.disablePlaceholderFocus){
          fiv.setFocusable(tag.getRootNode().initConfig.isViewFocusable);
          fiv.setFocusScale(tag.getRootNode().initConfig.placeholderScale > -1 ? tag.getRootNode().initConfig.placeholderScale : placeholderFocusScale);
          fiv.configPlaceholder(placeholderBackgroundColor, placeholderBorderRadius);
        }
        fiv.setDisablePlaceholderFocus(itemNode.initConfig.disablePlaceholderFocus);
      }
      return fiv;
    }else{
          //这里如果是singleton的view，disablePlaceholder只会将背景颜色去掉，也就是singleton的View一直返回ItemRootView
          boolean disablePlaceholder = tag.getRootNode().initConfig.disablePlaceholder || isSingleton;
          View placeholderView = null;
          String placeholderIcon = tag.getRootNode().initConfig.placeholderIcon == null && globalConfig != null ?
            globalConfig.placeholderIcon : tag.getRootNode().initConfig.placeholderIcon;
          HippyArray placeholderIconSize = tag.getRootNode().initConfig.placeholderIconSize == null && globalConfig != null ?
            globalConfig.placeholderIconSize : tag.getRootNode().initConfig.placeholderIconSize;
//          if (placeholderIcon != null) {
//              placeholderView = new IconPlaceholderView(parent.getContext(),placeholderIcon,viewType, disablePlaceholder ?
//                Color.TRANSPARENT : placeholderBackgroundColor, placeholderBorderRadius, tag.getRootNode().initConfig.placeholderLayout,placeholderIconSize);
//          }else {
            placeholderView = new PendingPlaceHolderView(parent.getContext(),
              viewType, disablePlaceholder ? Color.TRANSPARENT : placeholderBackgroundColor, placeholderBorderRadius, tag.getRootNode().initConfig.placeholderLayout);
//          }

          final ItemRootView rootView = new ItemRootView(parent.getContext(), viewType, placeholderView, isSingleton,itemNode);
          rootView.setContentView(tag.getRootNode().boundView,isSingleton);
          rootView.setFocusable(tag.getRootNode().initConfig.isViewFocusable && !itemNode.initConfig.disablePlaceholderFocus);
//          final float rootNodeFocusScale = itemNode.getTemplateNode()

          if(!itemNode.initConfig.disablePlaceholderFocus) {
            if(!isSingleton || rootView.isFocusable()) {
              //这里singleton时，当TVItem里潜逃了列表如果setFocusScale移动焦点时会有bug
              rootView.setFocusScale(tag.getRootNode().initConfig.placeholderScale > -1 ? tag.getRootNode().initConfig.placeholderScale : placeholderFocusScale);
            }
          }
        return rootView;
    }
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    assert (templateNodes != null);
    final RenderNode node = templateNodes.get(viewType);

    if (LogUtils.isDebug()) {
        Log.e(TAG, "FastAdapterAttach FastAdapterEvent DebugPool onCreateViewHolder NewOne viewType:" + viewType + ",this:" + Utils.hashCode(this));
    }
    Holder holder;
    View view = null;
    final ViewTag tag = new ViewTag(node);
    //这里节点在vue层有单一固定的实例对应
    boolean singleton = false;
    int width = 0;
    int height = 0;
    if (node == null) {
      Log.e(TAG,"没有注册对应的item，itemType：" + viewType);
    }else{
      width = node.getWidth();
      height = node.getHeight();
      View templateView = Utils.findBoundView(context, node);
      if (templateView != null) {
        if(LogUtils.isDebug()){
          Log.v(TAG,"node valid,templateView valid,viewType:"+viewType+",nodeClass "+node.getClassName()+",id:"+node.getId());
        }
        singleton = Utils.containKey(node, Utils.KEY_SINGLETON);
        if (templateView instanceof FastItemView) {
          if (mBoundListView != null) {
            ((FastItemView) templateView).setParentListView(mBoundListView);
          } else {
            ((FastItemView) templateView).setParentFlexView(mBoundFlexView);
          }
        }
        if (singleton) {
          if (LogUtils.isDebug()) {
            Log.e(TAG, "SingleItem view node:" + node);
          }
          //view = templateView;
          tag.getRootNode().onNodeInitOnSingleton(node);
          view = createItemContainerView(parent,viewType,tag,true,templateView);
          config4SingleItem(tag, view, node);
        } else {
          //创建所有节点
          createAllElementNodeRecursive(node, tag, null);
          //使用模版将所有view创建，如果placeholder激活则只创建部分view
          createViewRecursiveOnCreateViewHolder(tag.getRootNode(),null);
          //view = tag.getRootNode().boundView;
          boolean enablePlaceholder = this.enablePlaceholder;
          if(enablePlaceholder){
            if(LogUtils.isDebug()) {
              Log.d(TAG, "onCreateViewHolder create ItemRootView type:" + viewType+",hasNestList:"+tag.getRootNode().initConfig.hasNestList+",node class:"+node.getClassName());
            }
            if(tag.getRootNode().initConfig.hasNestList){
              //如果itemView中包含tvList、fastFlexView等，则不创建placeholder
              view = tag.getRootNode().boundView;
            }else if(tag.getRootNode().initConfig.disablePlaceholder){
              if(LogUtils.isDebug()) {
                Log.d(TAG, "onCreateViewHolder use boundView when disablePlaceholder viewType:"+viewType);
              }
              view = tag.getRootNode().boundView;
            }else{
              if(node.getChildCount() < 1 && tag.getRootNode().boundView instanceof HippyViewGroup){
                //这种情况下代表一个空的div，没必要再添加placeholder
                if(LogUtils.isDebug()) {
                  Log.d(TAG, "onCreateViewHolder use boundView when node.getChildCount() < 1 viewType:"+viewType);
                }
                view = tag.getRootNode().boundView;
              }else{
                view = createItemContainerView(parent,viewType,tag,false,templateView);
              }
            }
          }else{
            if(LogUtils.isDebug()) {
              Log.d(TAG, "onCreateViewHolder use boundView type:" + viewType + ",boundView:" + tag.getRootNode().boundView);
            }
            view = tag.getRootNode().boundView;
          }
        }
      }else{
          Log.e(TAG,"onCreateViewHolder error on  确定type为:"+viewType+"的模版已经设置,node:"+node);
      }
    }
    if (view == null) {
      Log.e(TAG,"创建view holder失败，node is "+node);
      view = new ErrorPlaceHolderView(parent.getContext(),viewType);
      holder = new Holder(view, tag, viewType, false);
      holder.isErrorView = true;
    }else{
      holder = new Holder(view, tag, viewType, singleton);
    }

    view.setLayoutParams(new RecyclerView.LayoutParams(width, height));
    if (view instanceof IRecyclerItemView){
      ((IRecyclerItemView)view).setJSEventViewID(node.getId());
    }

    if (holder.itemView instanceof IRecyclerItemView) {
      //通知
      IRecyclerItemView itemView = (IRecyclerItemView) holder.itemView;
      itemView.onCreate(getRootListView());
    }
    return holder;
  }

  void config4SingleItem(ViewTag tag, View view, RenderNode node) {

  }

  @Override
  public long getItemId(int position) {
    if (!TextUtils.isEmpty(mUserKeyName)) {
      HippyMap map = FastAdapterUtil.getRawItemFromFastList(getRawObject(position));
      if (map != null) {
        Object id = map.get(mUserKeyName);
        if (id != null) {
//          Log.d("DebugUpdate","getItemID:"+id+",position:"+position+",list:"+ExtendUtil.debugViewLite(mBoundListView));
          return id.hashCode();
        }else {
          long superID =  super.getItemId(position);
//          Log.e("DebugUpdate","getItemID by super:"+superID+",position:"+position+",list:"+ExtendUtil.debugViewLite(mBoundListView));
          return superID;
        }
      }
    }
    final Object  id = getIDFromData(position);
    if(id instanceof String){
      return id.hashCode();
    }
    return super.getItemId(position);
  }

  @Override
  public void onViewRecycled(@NonNull Holder holder) {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "postAllDelayContent BindEvent onViewRecycled  holder:" + Utils.hashCode(holder) + ",holderParent:" + holder.itemView.getParent());
      Log.e(TAG_POST, "PlaceholderToContent BindEvent onViewRecycled  holder:" + Utils.hashCode(holder) + ",tag:" + holder.itemView.getTag());
    }

    if(holder.tag != null ){
      if (holder.tag.getRootNode() != null) {
        holder.tag.getRootNode().recycled = true;
        holder.tag.getRootNode().clearPostTask();
      }
    }

    if(holder.isErrorView){
      return;
    }
    //已经Recycled的任务，无需再执行
    if (holder.postTask != null) {
      holder.postTask.cancel = true;
    }
    //getRootListView().clearTask(POST_TASK_CATEGORY_PLACEHOLDER_TO_CONTENT,holder.itemView.hashCode());

    final int pos = holder.getAdapterPosition() > -1 ? holder.getAdapterPosition() : holder.adapterPosition;
    removeDetached(pos);
    if (LogUtils.isDebug()) {
      Log.v(TAG, "FastAdapterAttach BindEvent DebugPool onViewRecycled in FastAdapter holder type:" + holder.type + ",view:" + Utils.hashCode(holder.itemView) + ",pos:" + pos);
    }
    super.onViewRecycled(holder);
    holder.reset();
//     recycleItem(holder);
    if (isListenBoundEvent) {
      sendUnBindEvent(rootListNodeID, pos, holder);
    }
    if (holder.singleton) {
      sendUnBindEvent4singleton(holder, pos);
    }

    if (holder.itemView instanceof IRecyclerItemView) {
      //通知
      IRecyclerItemView itemView = (IRecyclerItemView) holder.itemView;
      final ItemEntity ie = getItemEntity(pos);
      itemView.onUnBind(getRootListView(), pos, ie == null ? null : ie.raw);
    }
  }


  private static void changeVisibility(ElementNode node, boolean b) {
    if (node != null && node.boundView != null) {
      node.boundView.setAlpha(b ? 1 : 0);
//      node.boundView.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }
  }

  void dealDelayContent(Holder holder, int position) {
    final ViewTag vt = holder.tag;

    if (LogUtils.isDebug()) {
      Log.i(TAG_POST, "FastAdapterEvent before dealDelayContent  pos:" + position + ",rootElement:" + vt.getRootNode() + ",itemView:" + holder.itemView);
    }
    if (position > -1) {
      final ElementNode itemENode = vt.getRootNode();
      if (vt.enableLoadDelay) {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "FastAdapterEvent post dealDelayContent  pos:" + position + ",enablePlaceHolder:" + true + ",viewCreated:" + false);
        }
        postAllDelayContent(vt, itemENode, holder, position);
      }
    } else {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "FastAdapterEvent post dealDelayContent  pos invalid");
      }
    }
  }


  static int getHolderTaskType(Holder h) {
    return h.hashCode();
  }

  public void clearPostTask(Holder holder) {
    if (getRootListView() != null) {
      ViewTag vt = holder.tag;
      if (vt.getDelayLoadNodes() != null) {
        if(LogUtils.isDebug()) {
          Log.e(TAG_POST, "PlaceholderToContent clearPostTask position:" + holder.adapterPosition+",tag:"+holder.itemView.getTag());
        }
        getRootListView().clearTask(POST_TASK_CATEGORY_PLACEHOLDER_TO_CONTENT, holder.itemView.hashCode());
        for (int i = 0; i < vt.getDelayLoadNodes().size(); i++) {
          final ElementNode en = vt.getDelayLoadNodes().get(i);
          int pType = getUpdateLayoutType(en);
          getRootListView().clearTask(POST_TASK_CATEGORY_DELAY_LOAD, pType);
          getRootListView().clearTask(POST_TASK_CATEGORY_UPDATE_LAYOUT, pType);
        }
      }
    }
  }

  @Override
  public void onViewAttachedToWindow(@NonNull Holder holder) {
    if(holder.isErrorView){
      return;
    }


    ViewTag vt = holder.tag;
    final int position = vt.domNode != null ? vt.domNode.adapterPosition : -1;

    final ItemEntity ie = getItemEntity(position);

    if (LogUtils.isDebug()) {
      Log.w(TAG, "postAllDelayContent FastAdapterAttach onViewAttachedToWindow holder: "
        + Utils.hashCode(holder) + ",pos:" + holder.getAdapterPosition()
        + ",type:"+holder.getItemViewType()
        +",attached:"+holder.attached +",hasOnBind:"+holder.hasOnBind+",patched:"+holder.patched+ ",view:" + holder.itemView);
      Log.i(TAG_POST, "PlaceholderToContent ++++++++onViewAttachedToWindow position:" + position+",holder.itemView.hashCode():"+holder.itemView.hashCode()+",tag:"+holder.itemView.getTag()+",enablePlaceholder:"+enablePlaceholder);
    }
    //attach之前如果textView是marquee状态，先手动layout，避免重新setText位置不正确问题
    holder.resetTextView();
    holder.attached = true;
//    if(holder.boundPos > -1){
//      doPatch(holder,holder.boundPos);
//    }

    FastAdapterUtil.notifyAttachToParent(getContentView(holder));

    if(!this.enablePlaceholder){
      dealDelayContent(holder, position);
    }else{
      //这里在onBind中已经执行，无需在这里requestPlaceholderToContent
//      Object data = getRawObject(position);
//      if(data instanceof HippyMap) {
//        requestPlaceholderToContent(holder, vt.domNode, position, (HippyMap) data);
//      }
    }
    sendEvent4Singleton(holder, holder.adapterPosition, "onAttachedToWindow");
    if (isListenBoundEvent) {
      sendAdapterEvent(rootListNodeID, position, listNode.getNode(), holder, "onAttachedToWindow");
    }
    super.onViewAttachedToWindow(holder);

    if (holder.itemView instanceof IRecyclerItemView) {
      //通知
      IRecyclerItemView itemView = (IRecyclerItemView) holder.itemView;
      //final ItemEntity ie = getItemEntity(position);
      itemView.onAttachToWindow(getRootListView(), position, ie == null ? null : ie.raw);
    }

    Context context = holder.itemView.getContext();
    if(context instanceof HippyInstanceContext){
      final View contentView = getContentView(holder);
      String id = null;
      if(contentView != null) {
        id = ExtendUtil.getViewSID(contentView);
      }
//      Log.i(AutoFocusManager.TAG,"FastList onAttachToWindow name:"+id+",pos:"+position+",view:"+holder.itemView);
      if(getRootListView() != null) {
        //RenderNode renderNode = getRenderNode(getRootListView());
        if(pageRootView == null){
          pageRootView = HippyViewGroup.findPageRootView(getRootListView());
        }
//        if(pageRootView instanceof HippyViewGroup) {
//          ((HippyViewGroup) pageRootView).getAutoFocusManager().checkAndRequestAutoFocus(holder.itemView, id);
//        }else{
//          Log.e(AutoFocusManager.TAG,"checkAndRequestAutoFocus error on renderNode is null,pageRootView :"+pageRootView);
//        }
      }else{
        if(LogUtils.isDebug()) {
          Log.e(AutoFocusManager.TAG, "checkAndRequestAutoFocus error on renderNode is null,getRootListView() :" + getRootListView());
        }
      }
    }else{
      if(LogUtils.isDebug()) {
        Log.i(AutoFocusManager.TAG, "checkAndRequestAutoFocus context error  22 onAttachToWindow ,context:" + context);
      }
    }

    //final ItemEntity ie = getItemEntity(position);
//    if (ie != null && ie.updateItemDirty) {
//      Log.e("DebugReplaceItem","onAttachToWindow when updateItemDirty "+ie.raw);
//      updateItemOnReplace(holder,position,false);
//    }
  }

  public static View getContentView(Holder holder){
    if (holder.itemView instanceof ItemContainer) {
      return ((ItemContainer) holder.itemView).getContentView();
    }
    return holder.itemView;
  }

  public static HippyMap getUnWrappedItem(Object item){
    if(item instanceof HippyMap){
      return (HippyMap) item;
    }else if(item instanceof ItemEntity){
      return ((ItemEntity)item).getMap();
    }
    return null;
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull Holder holder) {
    if(holder.isErrorView){
      return;
    }
    if (LogUtils.isDebug()) {
      Log.e(TAG, "postAllDelayContent onViewDetachedFromWindow holder: " + Utils.hashCode(holder));
      Log.i(TAG_POST, "PlaceholderToContent ________onViewDetachedFromWindow position:" + holder.adapterPosition+",holder.itemView.hashCode():"+holder.itemView.hashCode());
    }


    holder.attached = false;
    holder.patched = false;
    int position = holder.adapterPosition;
    //clearPostTask(holder);
    FastAdapterUtil.notifyDetachFromParent(getContentView(holder));
    sendEvent4Singleton(holder, holder.adapterPosition, "onDetachedFromWindow");
    if (holder.tag != null && isListenBoundEvent) {
      position = holder.tag.domNode != null ? holder.tag.domNode.adapterPosition : -1;
      sendAdapterEvent(rootListNodeID, position, listNode.getNode(), holder, "onDetachedFromWindow");
    }
//    recycleItem(holder);
    super.onViewDetachedFromWindow(holder);

    if (holder.itemView instanceof IRecyclerItemView) {
      //通知
      IRecyclerItemView itemView = (IRecyclerItemView) holder.itemView;
      final ItemEntity ie = getItemEntity(holder.adapterPosition);
      itemView.onDetachFromWindow(getRootListView(), holder.adapterPosition, ie == null ? null : ie.raw);
    }
    cacheDetach(position,holder);
  }

  private void cacheDetach(int pos,Holder holder){
    if (mDetachedChildren == null) {
      mDetachedChildren = new ArrayMap<>();
    }
    mDetachedChildren.put(pos,holder);
    if(LogUtils.isDebug()) {
      Log.d("DebugReplaceItem", "++cacheDetach pos:" + pos + ",total:" + mDetachedChildren.size());
    }
  }

  public ArrayMap<Integer, Holder> getDetachedChildren() {
    return mDetachedChildren;
  }
  public void removeDetached(int pos){
    if (mDetachedChildren != null) {
      mDetachedChildren.remove(pos);
      if(LogUtils.isDebug()) {
        Log.d("DebugReplaceItem","--removeDetached pos:"+pos+",total:"+mDetachedChildren.size());
      }
    }
  }

  void clearDetachCaches(){
    if (mDetachedChildren != null) {
      mDetachedChildren.clear();
      mDetachedChildren = null;
    }
  }

  public void setBoundListView(FastListView fastListView) {
    mBoundListView = fastListView;
    if (LogUtils.isDebug()) {
      Log.i(TAG, "setBoundListView:" + mBoundListView);
    }
  }

  public void setBoundFLexView(FastFlexView fastFlexView) {
    mBoundFlexView = fastFlexView;
  }

  public void setRootList(FastListView fastListView, FastAdapter parentAdapter) {
    mRootListView = fastListView;
    if (LogUtils.isDebug()) {
      Log.i(TAG, "setRootListView:" + mRootListView + ",parentAdapter:" + parentAdapter + ",this:" + this);
    }
    if (parentAdapter != null && parentAdapter.itemStoreNodes != null) {
      if (LogUtils.isDebug()) {
        Log.i(TAG, "setRootListView putTemplate :" + parentAdapter.itemStoreNodes.size());
      }
      //parentAdapter.putTemplate(parentAdapter.itemStoreNodes);
      this.itemStoreNodes = parentAdapter.itemStoreNodes;
    }
    onFixDevicePerformance(getRoughPerformanceLevel(fastListView.getContext()));
  }

  FastListView getRootListView() {
    return mRootListView != null ? mRootListView : mBoundListView;
  }

  FastPendingView getCacheRootView() {
    return getRootListView() != null ? getRootListView() : getFastPendingView();
  }

  public void setListNode(ListNode listNode) {
    this.listNode = listNode;
    if (listNode.getBoundTag() == null) {
      listNode.setBoundTag(new ListNodeTag());
    }
    domUpdateManager = new DomUpdateManager<>();
    buildTemplate();
  }

  int getDefaultItemType() {
    return hashCode();
  }

  public static int DEFAULT_ITEM_TYPE = 0;
  public static int TYPE_HEADER = 1001;//header类型
  public static int TYPE_FOOTER = 1002;//footer类型
  public static int TYPE_ONE_LINE = 1003;//一行展示类型
  public static int TYPE_EMPTY = 1004;//一行展示类型 空view
  public static int TYPE_SPAN_COUNT_TWO = 10002;//spanCount 2布局类型
  public static int TYPE_SPAN_COUNT_THREE = 10003;//spanCount 3布局类型
  public static int TYPE_SPAN_COUNT_FOUR = 10004;//spanCount 4布局类型
  public static int TYPE_SPAN_COUNT_FIVE = 10005;//spanCount 5布局类型
  public static int TYPE_SPAN_COUNT_SIX = 10006;//spanCount 6布局类型

  public void putTemplate(Map<Integer, RenderNode> tm) {
    if (tm != null && tm.size() > 0) {
      if (templateNodes == null) {
        templateNodes = new HashMap<>();
      }
      templateNodes.putAll(tm);
    }
  }

  void buildTemplate() {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "buildTemplate called this: " + this);
    }
    assert (listNode != null);
    if (templateNodes == null) {
      templateNodes = new HashMap<>();
    } else {
      templateNodes.clear();
    }
    if (mShareItemStoreName != null) {
      //2024.09.08 这里添加共享的itemNode,共享的itemStore如果遇到type冲突，会被覆盖
      final RenderNode windowRootNode = listNode.getNode().getWindowRootNode();
      if (windowRootNode != null) {
        final RenderNode sharedNode = ExtendUtil.findRenderNodeByName(mShareItemStoreName,windowRootNode);
        if (sharedNode != null) {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "build Shared Template sharedNode:" + sharedNode);
          }
          final Map<Integer, RenderNode> sharedTemplate = ItemStoreNode.buildItemStoreTemplate((ItemStoreNode) sharedNode);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "buildTemplate put all Shared Template this: " + this + ",mBoundListView:" + mBoundListView);
            for (Map.Entry<Integer, RenderNode> e : sharedTemplate.entrySet()) {
              Log.i(TAG, "buildTemplate put Shared itemNode itemType: " + e.getKey());
            }
          }
          templateNodes.putAll(sharedTemplate);
        }else{
          Log.e(TAG, "build Shared Template error sharedNode is null mShareItemStoreName:" + mShareItemStoreName);
        }
      }else{
        Log.e(TAG, "build Shared Template error windowRootNode is null mShareItemStoreName:" + mShareItemStoreName);
      }
    }
    for (int i = 0; i < listNode.getChildCount(); i++) {
      final RenderNode child = listNode.getChildAt(i);
      if (child instanceof ItemStoreNode) {
        itemStoreNodes = ItemStoreNode.buildItemStoreTemplate((ItemStoreNode) child);
        //templateNodes.putAll(itemStoreNodes);
      } else {
        if (child.getProps().containsKey("type")) {
          final String childType = child.getProps().getString("type");
          final int type = Integer.parseInt(childType);
          if (LogUtils.isDebug()) {
            Log.d(TAG, "buildTemplate put type:" + type + ",node:" + child);
          }
          templateNodes.put(type, child);
        } else {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "buildTemplate put DefaultType,node:" + child);
          }
          templateNodes.put(getDefaultItemType(), child);
        }
      }
    }

    if (itemStoreNodes != null) {
      if (LogUtils.isDebug()) {
        Log.i(TAG, "buildTemplate put all itemStoreNodes this: " + this + ",mBoundListView:" + mBoundListView);
        for (Map.Entry<Integer, RenderNode> e : itemStoreNodes.entrySet()) {
          Log.i(TAG, "buildTemplate put itemNode itemType: " + e.getKey());
        }
      }
      templateNodes.putAll(itemStoreNodes);
    }
  }

  public Map<Integer, RenderNode> getIteStoreNodes() {
    return itemStoreNodes;
  }

  Holder findOldCacheView(int type) {
    if (findOldCacheWorker() != null) {
      if(LogUtils.isDebug()) {
        CachePool cachePool = findOldCacheWorker();
        if (cachePool != null) {
          Log.i("DebugPool","findOldCacheView cache pool cache size of type:"+type+",count:"+cachePool.getRecycledViewCount(type)+",cachePool:"+cachePool);
        }
      }
      return findOldCacheWorker().get(type);
    }else if(LogUtils.isDebug()){
      Log.e("DebugPool","findOldCacheView error, old is null type:"+type);
    }
    return null;
  }

  CachePool findOldCacheWorker() {
    final String k = getPoolCacheKey(viewContext, shareViewPoolType,getCacheRootView());
    if (gSharedCachePools != null) {
      CachePool v = null;
      if (gSharedCachePools.containsKey(k)) {
        v = gSharedCachePools.get(k);
        if (LogUtils.isDebug()) {
          Log.d(TAG, "DebugPool getCacheWorker find old:" + v + ",name:" + k);
        }
      }else if(LogUtils.isDebug()){
        Log.w("DebugPool","findOldCacheWorker null k:"+k+",shareViewPoolType:"+shareViewPoolType);
      }
      return v;
    }
    return null;
  }

  private final static String getPoolCacheKey(@Nullable Context context,String k,FastPendingView boundView){

    final View root = boundView == null ? null : boundView.findPageRootView();
    if (root != null) {
//      Log.d("EsPageViewLog","~~~~~getPoolCacheKey from PageRoot:"+root+",k:"+k);
      return "k-" + k + "|cxt-" + root.hashCode();
    }
    if(LogUtils.isDebug()) {
      Log.i("EsPageViewLog", "~~~~~getPoolCacheKey from Context:" + context + ",k:" + k + ",boundView:" + boundView);
    }
    return context == null ? "k-" + k : "k-" + k + "|cxt-" + context.hashCode();
  }

  private final String getPoolCacheKey(Context context,FastPendingView boundView){
    return  getPoolCacheKey(context,shareViewPoolType,boundView);
  }

  CachePool getCacheWorker(Context context,FastPendingView fastListView) {
    //2023.11.27确定一个缓存池，保证一块context环境里一个缓存池，这里加一个context的hasCode
    final String k = getPoolCacheKey(context,fastListView);
    if (LogUtils.isDebug()) {
      Log.d(TAG, "DebugPool >>>>>>>>>>>>.getCacheWorker k :" + k+",context:"+context+",CACHE_KEY:"+CACHE_KEY);
    }
    if (gSharedCachePools == null) {
      if (rootView != null) {
        final Object tag = rootView.getViewTag(CACHE_KEY);
        if (tag instanceof HashMap) {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "DebugPool getCacheWorker from rootView:" + Utils.hashCode(gSharedCachePools));
          }
          gSharedCachePools = (Map<String, CachePool>) tag;
        }
      }
    }
    if (gSharedCachePools == null) {
      gSharedCachePools = new HashMap<>();
      if (LogUtils.isDebug()) {
        Log.e(TAG, "DebugPool getCacheWorker new  gSharedCachePools :" + Utils.hashCode(gSharedCachePools) + ",rootView:" + rootView);
      }
      if (rootView != null) {
        rootView.putViewTag(CACHE_KEY, gSharedCachePools);
      }
    }
    final CachePool v;
    if (gSharedCachePools.containsKey(k)) {
      v = gSharedCachePools.get(k);
      if (LogUtils.isDebug()) {
        Log.d(TAG, "DebugPool getCacheWorker find old:" + v.hashCode() + ",name:" + CACHE_KEY + ",type:" + k);
      }
    } else {
      v = new CachePool();
      gSharedCachePools.put(k, v);
      if (LogUtils.isDebug()) {
        Log.e(TAG, "DebugPool new CachePool !!!!:" + v.hashCode() + ",adapter :" + Utils.hashCode(this) + ",keyType:" + k);
      }
    }
    return v;
  }


  public Object getRawObject(int position) {
    final ItemEntity item = getItemEntity(position);
    return item != null ? item.raw : null;
  }

  int getLoopPosition(int pos){
    int realPos = pos;
    if (isInfiniteLoopEnabled()) {
      //循环获取数据，需要
      int itemDataCount = getItemDataCount();
      if (itemDataCount <= 0) {
        realPos = -1;
      }else {
        realPos = pos % itemDataCount;
      }
    }
    return realPos;
  }

  ItemEntity getItemEntity(int position) {

    if(isInfiniteLoop){
//      int dataPosition = getLoopPosition(position);
//      if (dataPosition < 0 || getItemDataCount() < 1) {
//        return null;
//      }
      int remain = INFINITE_START_POSITION % getItemDataCount();
      int offsetPosition = Math.max(0,position - remain);
      int dataPosition = getLoopPosition(offsetPosition);
      int finalPosition = dataPosition;
      //Log.d("DebugInfinite"," getItemEntity finalPosition:"+finalPosition+",remain:"+remain+",dataPosition:"+dataPosition+",position:"+position);
      return (ItemEntity) this.dataList.get(finalPosition);
    }else{
      if (position < 0 || this.dataList == null || this.dataList.size() <= position) {
        return null;
      }
      return (ItemEntity) this.dataList.get(position);
    }
  }

  void exeCustomPendingProp4Singleton(View view, String prop, Object dataFromValue) {
    if (view == null) {
      Log.e(TAG, "exeCustomPendingProp error on view is null ");
      return;
    }
    switch (prop) {
//      case TemplateCodeParser.PENDING_PROP_LAYOUT: {
//        if (LogUtils.isDebug()) {
//          Log.e(TAG, "doPatch4Prepare layout dataFromValue:" + dataFromValue);
//        }
//        final HippyArray posArray = (HippyArray) dataFromValue;
//        if (posArray != null && posArray.size() == 4) {
//          //tempPatchLayout.add(new UpdateLayoutPatch(posArray.getInt(0), posArray.getInt(1), posArray.getInt(2),posArray.getInt(3), p.view, boundNode));
//          FastAdapterUtil.updateLayout(view, Utils.toPX(posArray.getInt(0))
//            , Utils.toPX(posArray.getInt(1)),
//            Utils.toPX(posArray.getInt(2)),
//            Utils.toPX(posArray.getInt(3)));
//        }
//        break;
//      }
      case TemplateCodeParser.PENDING_PROP_TRANSLATION: {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "doPatch4Prepare position dataFromValue:" + dataFromValue);
        }
        final HippyArray posArray = (HippyArray) dataFromValue;
        if (posArray != null && posArray.size() == 2) {
          //tempPatchLayout.add(new UpdateLayoutPatch(posArray.getInt(0), posArray.getInt(1), boundNode.getWidth(), boundNode.getHeight(), p.view, boundNode));
          FastAdapterUtil.updateLayout(view, Utils.toPX(posArray.getInt(0))
            , Utils.toPX(posArray.getInt(1)),
            view.getWidth(),
            view.getHeight());
        }
        break;
      }
      case TemplateCodeParser.PENDING_PROP_SIZE: {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "doPatch4Prepare size dataFromValue:" + dataFromValue);
        }
        final HippyArray posArray = (HippyArray) dataFromValue;
        if (posArray != null && posArray.size() == 2) {
          //tempPatchLayout.add(new UpdateLayoutPatch(boundNode.getX(),boundNode.getY(),posArray.getInt(0), posArray.getInt(1), p.view, boundNode));
          FastAdapterUtil.updateLayout(view, view.getLeft()
            , view.getTop(),
            Utils.toPX(posArray.getInt(0)),
            Utils.toPX(posArray.getInt(1)));
        }
        break;
      }
    }
  }


  private boolean isListenBoundEvent = false;

  public void setListenBoundEvent(boolean enable) {
    this.isListenBoundEvent = enable;
  }

  public RecyclerView.ItemDecoration buildListItemDecoration() {
    RecyclerView.ItemDecoration itemDecoration = new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        try {
          final int pos = parent.getChildAdapterPosition(view);

          if (dataList != null && dataList.size() > getLoopPosition(pos)) {
            final HippyMap item = (HippyMap) getRawObject(pos);
            if (item.containsKey("decoration")) {
              final HippyMap dec = item.getMap("decoration");
              if (dec.containsKey("left")) {
                outRect.left = Utils.toPX(dec.getInt("left"));
              }
              if (dec.containsKey("right")) {
                outRect.right = Utils.toPX(dec.getInt("right"));
              }
              if (dec.containsKey("top")) {
                outRect.top = Utils.toPX(dec.getInt("top"));
              }
              if (dec.containsKey("bottom")) {
                outRect.bottom = Utils.toPX(dec.getInt("bottom"));
              }
            }
          }
        } catch (Exception e) {
          //Do Nothing
          //{position:1,prevent:{down,left,right,up}}
        }
      }
    };
    return itemDecoration;
  }



  public void onBeforeChangeAdapter() {
  }

  /***
   *
   * @param position 要更新的位置
   * @param dataToUpdate 从前端传来需要更新的数据，eg:{
   *                      contentList:[{},{}] ,注意这里contentList是在前端对应${contentList}
   *
   * }
   * @param updateView 是否更新view
   * @param holder 当前展示的viewHolder
   */
  public void updateItemProps(String name, int position, HippyMap dataToUpdate, boolean updateView, RecyclerView.ViewHolder holder) {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "BindEvent updateItemProps called position: " + position + ",name:" + name + ",dataToUpdate:" + dataToUpdate + ",updateView:" + updateView + ",holder:" + Utils.hashCode(holder) + ",this:" + Utils.hashCode(this));
    }
    if (this.dataList != null && this.dataList.size() > position) {
      final HippyMap itemToUpdate = (HippyMap) getRawObject(position);
      final ItemEntity it = getItemEntity(position);
      if (it != null) {
        final HippyMap toUpdateProps = new HippyMap();
        if (LogUtils.isDebug()) {
          Log.d(TAG, "updateItemProps ItemEntity raw :" + it.raw);
        }
        final ElementNode target = Utils.findElementNodeByName(name, ((Holder) holder).tag.getRootNode());
        for (String pendingProp : dataToUpdate.keySet()) {
          //先通过${key}里的值，获取到map对象，并更新数据
          final Object newData = dataToUpdate.get(pendingProp);
          TemplateCodeParser.setValueFromCode(itemToUpdate, pendingProp, newData);
//          final String viewProp = it.rawPendingPropsReverse.getString(pendingProp);
          final String viewProp = Utils.findPropByPendingProp(pendingProp, target);
          if (LogUtils.isDebug()) {
            Log.d(TAG, "updateItemProps viewProp " + viewProp + ",pendingProp:" + pendingProp);
          }
          if (TextUtils.isEmpty(viewProp)) {
            throw new IllegalStateException("updateItemProps Error viewProp cant be null!");
          }
          toUpdateProps.pushObject(viewProp, newData);
        }
        if (updateView) {

          if (LogUtils.isDebug()) {
            Log.d(TAG, "updateItemProps doUpdateView toUpdateMap:" + toUpdateProps + ",findTargetByName:" + target + ",name:" + name);
          }
          if (target != null) {
//            new ElementDiffPatch(this, ((Holder) holder).tag.getRootNode(), itemToUpdate, position)
//              .invokeProps(toUpdateProps, target);
            final HippyViewController vc = CustomControllerHelper.getViewController(getControllerManager(), target.templateNode);
            FastAdapterUtil.invokePropsSync(vc,toUpdateProps,position,target,this,STEP_UPDATE_ITEM);
          } else {
            Log.e(TAG, "updateItemProps doUpdateView toUpdateMap:" + toUpdateProps + ",findTargetByName:" + null + ",name:" + name);
          }
        } else {
          Log.e(TAG, "updateItemProp 不执行更新ui，updateView：" + false + ",holder:" + holder);
        }
      } else {
        Log.e(TAG, "updateItemProp error ,此位置的数据，重来没有展示过,无法进行更新 it:" + it);
      }
    } else {
      Log.e(TAG, "updateItemProps error position: " + position + ",name:" + name + ",updateView:" + updateView + ",holder:" + Utils.hashCode(holder) + ",this:" + Utils.hashCode(this) + ",dataList:" + (this.dataList == null ? 0 : this.dataList.size()));
    }
  }

  public void updateItemSpecificPropByTargetName(String name, int position,String prop,  Object dataToUpdate, boolean updateView, RecyclerView.ViewHolder holder) {
    final ElementNode target = Utils.findElementNodeByName(name, ((Holder) holder).tag.getRootNode());
    updateItemSpecificProp(target, position, prop, dataToUpdate, updateView, holder);
  }

  public void updateItemSpecificPropByTargetSID(String targetSID, int position,String prop,  Object dataToUpdate, boolean updateView, RecyclerView.ViewHolder holder) {
    final ElementNode target = Utils.findElementNodeBySID(targetSID, ((Holder) holder).tag.getRootNode());
    updateItemSpecificProp(target, position, prop, dataToUpdate, updateView, holder);
  }

  public void updateItemSpecificProp(ElementNode target, int position,String prop,  Object dataToUpdate, boolean updateView, RecyclerView.ViewHolder holder) {
    if (LogUtils.isDebug()) {
      Log.e(TAG, "BindEvent updateItemSpecificProp called position: " + position + ",target:" + target + ",dataToUpdate:" + dataToUpdate + ",updateView:" + updateView + ",holder:" + Utils.hashCode(holder) + ",this:" + Utils.hashCode(this));
    }
    if (this.dataList != null && this.dataList.size() > position) {
      final HippyMap itemToUpdate = (HippyMap) getRawObject(position);
      final ItemEntity it = getItemEntity(position);
      if (it != null) {
        if (LogUtils.isDebug()) {
          Log.d(TAG, "updateItemSpecificProp ItemEntity raw :" + it.raw);
        }
          //先通过${key}里的值，获取到map对象，并更新数据
        //final Object newData = dataToUpdate.get(prop);
        TemplateCodeParser.setValueFromCode(itemToUpdate, prop, dataToUpdate);
//          final String viewProp = it.rawPendingPropsReverse.getString(pendingProp);
        //final ElementNode target = Utils.findElementNodeByName(name, ((Holder) holder).tag.getRootNode());
        if (updateView) {
          final HippyMap toUpdateProps = new HippyMap();
          toUpdateProps.pushObject(prop, dataToUpdate);
          if (LogUtils.isDebug()) {
            Log.d(TAG, "updateItemSpecificProp doUpdateView toUpdateMap:" + toUpdateProps + ",findTargetByName:" + target );
          }
          if (target != null) {
//            new ElementDiffPatch(this, ((Holder) holder).tag.getRootNode(), itemToUpdate, position)
//              .invokeProps(toUpdateProps, target);
            final HippyViewController vc = CustomControllerHelper.getViewController(getControllerManager(), target.templateNode);
            FastAdapterUtil.invokePropsSync(vc,toUpdateProps,position,target,this,STEP_UPDATE_ITEM);
          } else {
            Log.e(TAG, "updateItemSpecificProp doUpdateView toUpdateMap:" + toUpdateProps + ",findTargetByName:" + null );
          }
        } else {
          Log.e(TAG, "updateItemSpecificProp 不执行更新ui，updateView：" + false + ",holder:" + holder);
        }
      } else {
        Log.e(TAG, "updateItemSpecificProp error ,此位置的数据，重来没有展示过,无法进行更新 it:" + null);
      }
    } else {
      Log.e(TAG, "updateItemProps error position: " + position + ",updateView:" + updateView + ",holder:" + Utils.hashCode(holder) + ",this:" + Utils.hashCode(this) + ",dataList:" + (this.dataList == null ? 0 : this.dataList.size()));
    }
  }



  public void updateItemDataRange(final int pos, final int count, HippyArray data) {
    if (LogUtils.isDebug()) {
      Log.i(TAG, "updateItemRange pos " + pos + ",count:" + count + "this:" + Utils.hashCode(this));
    }
    //更新列表里的数据
    int index = 0;
    if (this.dataList != null) {
      final int totalCount = this.dataList.size();
      //update
      for (int i = pos; i < pos + count; i++) {
        if (pos + count > totalCount) {
          break;
        }
        this.dataList.setObject(i, new ItemEntity(data.get(index), i));
        if (LogUtils.isDebug()) {
          Log.i("DebugReplaceItem", "updateItemRange set data i:" + i + ",index:" + index+",data:"+data.get(index));
        }
        index++;
      }
      if (LogUtils.isDebug()) {
        Log.e(TAG, "updateItemRange pos done");
      }
    } else {
      Log.e(TAG, "updateItemRange error dataList null");
    }
  }

  public void insertItemDataRange(final int pos, int count, HippyArray data) {
    if (LogUtils.isDebug()) {
      Log.d(TAG, "insertItemDataRange pos " + pos + ",count:" + count + "this:" + Utils.hashCode(this));
    }
    //更新列表里的数据
    if (this.dataList != null && data != null) {
      final int totalCount = this.dataList.size();
      //add
      if (pos >= 0 && pos <= totalCount) {
        for (int i = count; i > 0; i--) {
          this.dataList.addObject(pos, new ItemEntity(data.get(i - 1), i - 1));
        }
      }
      if (LogUtils.isDebug()) {
        Log.e(TAG, "insertItemDataRange pos done");
      }
    } else {
      Log.e(TAG, "insertItemDataRange error dataList null");
    }
  }

  //用来阻止列表焦点移动时滚动列表
  public boolean onInterceptRequestChildRectangleOnScreen(RecyclerView parent, View view, Rect rect, boolean immediate, int direction, boolean focusedChildVisible) {
    boolean prevent = false;
    try {
      final int pos = parent.getChildAdapterPosition(view);
      if (dataList != null && dataList.size() > pos && pos > -1) {
        final HippyMap item = (HippyMap) getRawObject(pos);
        if (item.containsKey("preventChildOnScreen")) {
          final HippyArray dec = item.getArray("preventChildOnScreen");
          if (LogUtils.isDebug()) {
            Log.d(TAG, "preventChildOnScreen position:" + pos + ",array:" + dec +",parent:"+parent);
          }
          for (int i = 0; i < dec.size(); i++) {
            final String directionStr = dec.getString(i);
            switch (directionStr) {
              case "up":
                prevent = direction == View.FOCUS_UP;
                break;
              case "down":
                prevent = direction == View.FOCUS_DOWN;
                break;
              case "right":
                prevent = direction == View.FOCUS_RIGHT;
                break;
              case "left":
                prevent = direction == View.FOCUS_LEFT;
                break;
              case "first":
                prevent = direction == -1;
                break;
            }
            if (prevent) {
              break;
            }
          }
          if (LogUtils.isDebug()) {
            Log.d(TAG, "preventChildOnScreen position:" + pos + ",prevent:" + prevent + ",direction:" + direction);
          }
        } else {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "preventChildOnScreen position:" + pos + ",array null" +",parent:"+parent+",item :"+item);
          }
        }
      }
    } catch (Exception e) {
      //Do Nothing
      //{position:1,prevent:{down,left,right,up}}
    }
    return prevent;
  }

  public void onLayoutComplete(RecyclerView.State state,FastListView listView) {
    if(LogUtils.isDebug()) {
      Log.d("DebugEvent", "onLayoutComplete state: " + state + ",view:" + ExtendUtil.debugViewLite(listView) + ",childCount:" + listView.getChildCount());
    }
    if (listView.getChildCount() > 0) {
      for(int i = 0; i < listView.getChildCount(); i ++){
        View child = listView.getChildAt(i);
        int pos = listView.getChildAdapterPosition(child);
        RecyclerView.ViewHolder vh = listView.findViewHolderForAdapterPosition(pos);
        if(vh instanceof FastAdapter.Holder){
            ElementNode elementNode = ((Holder) vh).getItemRootNode();
            if(elementNode != null) {
              elementNode.adapterPosition = pos;
              if(LogUtils.isDebug()){
                Log.i("DebugEvent", "onLayoutComplete elementNode pos: " + elementNode.adapterPosition);
              }
            }else if (LogUtils.isDebug()){
              Log.e("DebugEvent", "onLayoutComplete elementNode null pos: " + pos);
            }
        }

      }
    }
  }


  final static class ElementClickListener implements View.OnClickListener {
    final FastAdapter adapter;
    final ElementNode elementNode;
    final int rootListID;
    final EventDeliverer deliverer;

    ElementClickListener(FastAdapter adapter, int rootListID, ElementNode elementNode, EventDeliverer deliverer) {
      this.adapter = adapter;
      this.elementNode = elementNode;
      this.rootListID = rootListID;
      this.deliverer = deliverer;
    }

    @Override
    public void onClick(View v) {
      final int parentPosition = Utils.findParentPositionByView(v, 0);
      int position = elementNode.rootNode.adapterPosition;
      if (LogUtils.isDebug()) {
        Log.i("DebugClick", "ElementCallback click v:" + v + ",name:" + elementNode.name + ",parentPosition:" + parentPosition+",position:"+position);
      }
      HippyViewEvent event = new HippyViewEvent("onItemClick");
      HippyMap m = new HippyMap();
      Utils.buildEventBasic(m, v, elementNode, position);
      if (adapter != null) {
        m.pushObject("item", adapter.getRawObject(position));
      }
      m.pushString("name", elementNode.name);

      m.pushInt("parentPosition", parentPosition);
      //event.send(rootListID,Utils.getHippyContext(v),m);
      deliverer.sendEvent(event, rootListID, m);
      final ElementNode itemRoot = findItemRoot(elementNode);
      if (itemRoot != null && itemRoot.templateNode.getProps() != null) {

//        for (String s : itemRoot.templateNode.getProps().keySet()) {
//          if (LogUtils.isDebug()) {
//            Log.d(TAG, "ElementCallback 1 prop:" + s + ",value:" + itemRoot.templateNode.getProps().get(s));
//          }
//        }
        if (itemRoot.templateNode.getProps().containsKey("onItemClick")) {
          //View view = Utils.findBoundView(elementNode.boundView, itemRoot.templateNode);
          if (LogUtils.isDebug()) {
            Log.d(TAG, "ElementCallback ClickEvent position of Parent " + parentPosition);
          }
          deliverer.sendEvent(event, itemRoot.templateNode.getId(), m);
        }

      }
    }
  }

  final static class ElementLongClickListener implements View.OnLongClickListener {
    final FastAdapter adapter;
    final ElementNode elementNode;
    final int rootListID;
    final EventDeliverer deliverer;

    public ElementLongClickListener(FastAdapter adapter, int rootListID, ElementNode elementNode, EventDeliverer deliverer) {
      this.adapter = adapter;
      this.elementNode = elementNode;
      this.rootListID = rootListID;
      this.deliverer = deliverer;
    }

    @Override
    public boolean onLongClick(View v) {
      final int parentPosition = Utils.findParentPositionByView(v, 0);
      int position = elementNode.rootNode.adapterPosition;
      if (LogUtils.isDebug()) {
        Log.d("DebugClick", "ElementCallback longClick v:" + v + ",name:" + elementNode.name + ",parentPosition:" + parentPosition+",position:"+position);
      }
      HippyViewEvent event = new HippyViewEvent("onItemLongClick");
      HippyMap m = new HippyMap();

      Utils.buildEventBasic(m, v, elementNode, position);
      if (adapter != null) {
        m.pushObject("item", adapter.getRawObject(position));
      }
      m.pushString("name", elementNode.name);
      m.pushInt("parentPosition", parentPosition);
      //event.send(rootListID,Utils.getHippyContext(v),m);
      deliverer.sendEvent(event, rootListID, m);
      final ElementNode itemRoot = findItemRoot(elementNode);
      if (itemRoot != null && itemRoot.templateNode.getProps() != null) {

//        for (String s : itemRoot.templateNode.getProps().keySet()) {
//          if (LogUtils.isDebug()) {
//            Log.d(TAG, "ElementCallback 1 prop:" + s + ",value:" + itemRoot.templateNode.getProps().get(s));
//          }
//        }
        if (itemRoot.templateNode.getProps().containsKey("onItemLongClick")) {
          //View view = Utils.findBoundView(elementNode.boundView, itemRoot.templateNode);
          if (LogUtils.isDebug()) {
            Log.d(TAG, "ElementCallback LongClickEvent position of Parent " + parentPosition);
          }
          deliverer.sendEvent(event, itemRoot.templateNode.getId(), m);
        }

      }
      return true;
    }
  }


  final static class ItemFocusListener implements View.OnFocusChangeListener {
    final FastAdapter adapter;
    final int rootListID;
    final ElementNode elementNode;
    final EventDeliverer deliverer;

    ItemFocusListener(FastAdapter adapter, int rootListID, ElementNode elementNode, EventDeliverer deliverer) {
      this.adapter = adapter;
      this.rootListID = rootListID;
      this.elementNode = elementNode;
      this.deliverer = deliverer;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      final ElementNode itemRoot = findItemRoot(elementNode);

      final int parentPosition = Utils.findParentPositionByView(v, 0);
      int position = elementNode.rootNode.adapterPosition;
      if (LogUtils.isDebug()) {
        Log.e(TAG, "ElementCallback focus v:" + v.hashCode() + ",hasFocus:" + hasFocus + ",parentPosition:" + parentPosition+",position:"+position);
      }
      HippyViewEvent event = new HippyViewEvent("onItemFocused");
      HippyMap m = new HippyMap();
      Utils.buildEventBasic(m, v, elementNode, position);
      if (adapter != null) {
        m.pushObject("item", adapter.getRawObject(position));
      }
      m.pushString("name", elementNode.name);
      m.pushBoolean("hasFocus", hasFocus);
      m.pushInt("parentPosition", parentPosition);
      //event.send(rootListID,Utils.getHippyContext(v), m);
      deliverer.sendEvent(event, rootListID, m);

      if (itemRoot != null && itemRoot.templateNode.getProps() != null) {

//        for (String s : itemRoot.templateNode.getProps().keySet()) {
//          if (LogUtils.isDebug()) {
//            Log.d(TAG, "ElementCallback 1 prop:" + s + ",value:" + itemRoot.templateNode.getProps().get(s));
//          }
//        }
        if (itemRoot.templateNode.getProps().containsKey("onItemFocused")) {
          //View view = Utils.findBoundView(elementNode.boundView, itemRoot.templateNode);
          if (LogUtils.isDebug()) {
            Log.d(TAG, "ElementCallback position of Parent " + parentPosition);
          }

          deliverer.sendEvent(event, itemRoot.templateNode.getId(), m);
        }
      }
    }
  }

  static ElementNode findItemRoot(DomNode node) {
    if (node != null) {
      if (node instanceof ElementNode && ((ElementNode) node).isRootItem) {
        return (ElementNode) node;
      } else {
        return findItemRoot(node.getParent());
      }
    }
    return null;
  }

  void notifyElementBeforeOnBind(DomNode node, Object itemData) {
    if (node instanceof ElementNode) {

      final View view = ((ElementNode) node).boundView;
      if (view instanceof ListItemHolder) {
        imgSpecial(view, node, itemData);
        //把ListItemHolder相关的生命周期执行一下
        ((ListItemHolder) view).onItemBind();
      }
      if (view instanceof PostTaskHolder) {
        ((PostTaskHolder) view).setRootPostHandlerView(getRootListView());
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      notifyElementBeforeOnBind(node.getChildAt(i), itemData);
    }
  }

  private void imgSpecial(View view, DomNode node, Object itemData) {
    if (view instanceof HippyImageView && !(view instanceof HippyViewGroup) && ((ElementNode) node).templateNode != null) {

      HippyMap uiProps = ((ElementNode) node).templateNode.getProps();

      if (uiProps != null && uiProps.containsKey("src") && itemData instanceof HippyMap) {
        final String pendingProp = TemplateCodeParser.parsePlaceholderProp("src", uiProps);
        if (!TextUtils.isEmpty(pendingProp)) {
          final Object dataFromValue = TemplateCodeParser.getValueFromCode((HippyMap) itemData, pendingProp);
          if (dataFromValue != null) {
            ((HippyImageView) view).beforeItemBind(dataFromValue);
          }
        }
      }
    }
  }

  void updateItemLayout(DomNode node) {
    if (node instanceof ElementNode) {
      final ElementNode en = (ElementNode) node;
      final View view = en.boundView;
      if (view != null) {
//        if(LogUtils.isDebug()){
//          Log.v(TAG,"updateItemLayout node:"+en+",view:"+view);
//        }
        if(en.isMarkToDelay){
          changeVisibility(en,true);
        }
        FastAdapterUtil.updateLayout(view, (ElementNode) node);
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      updateItemLayout(node.getChildAt(i));
    }
  }


  void updateItemLayout4ItemRootView(View itemView,ElementNode en) {
      final View view = en.boundView;
      if(en.getLayoutWidth() < 1 || en.getLayoutHeight() < 1 && en.getParent() == en.rootNode){
//          Log.e(TAG, "fixElementNodeSize  error width:"+en.getLayoutWidth()+",height:"+en.getLayoutHeight()+",template :"+en.templateNode);
      }
      if (view != null) {
        boolean changed = false;
        int currentWidth = view.getWidth();
        int currentHeight = view.getHeight();
        int toWidth = currentWidth;
        int toHeight = currentHeight;
        if(en.getLayoutWidth() != currentWidth && en.getLayoutWidth() > 0){
          toWidth  = (int) en.getLayoutWidth();
          changed = true;
        }
        if(en.getLayoutHeight() != currentHeight && en.getLayoutHeight() > 0){
          toHeight  = (int) en.getLayoutHeight();
          changed = true;
        }
        if(changed){
          if (en.isRootItem) {
//            if(LogUtils.isDebug()){
//              Log.v(TAG,"updateItemLayout node:"+en+",view:"+view);
//            }
            updateLayoutF(en.boundView, 0, 0, toWidth, toHeight);
          }else{
            updateLayoutF(en.boundView,en.getLayoutX(),en.getLayoutY(),toWidth,toHeight);
          }
        }
      }
      for (int i = 0; i < en.getChildCount(); i++) {
        updateItemLayout4ItemRootView(itemView,(ElementNode) en.getChildAt(i));
      }
      if (en.isRootItem) {
        if(itemView instanceof ItemContainer) {
          updateLayoutF(itemView, en.getLayoutX(), en.getLayoutY(), en.getLayoutWidth(), en.getLayoutHeight());
        }

      }
  }

  /**
   * 只更新item内部所有的view,跳过root的item，以防止列表在已经layout后再次layout出现的位置错误
   *
   * @param node
   */
  void updateItemLayoutInside(DomNode node) {
    if (node instanceof ElementNode && !((ElementNode) node).isRootItem) {
      final ElementNode en = (ElementNode) node;
      final View view = en.boundView;
      if (view != null) {
        FastAdapterUtil.updateLayout(view, (ElementNode) node);
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      updateItemLayoutInside(node.getChildAt(i));
    }
  }


  void measureFastFlexView(ElementNode node) {
    final FastFlexView content = (FastFlexView) node.boundView;
    //updateLayout(content,0,0,content.getPreferWidth(),content.getPreferHeight());
    if (LogUtils.isDebug()) {
      Log.i(TAG, "FIX_ITEM_SIZE prepareFlexView width:" + content.getPreferWidth() + ",height:" + content.getPreferHeight() + ",node:" + node);
      Log.i(TAG, "FIX_ITEM_SIZE prepareFlexView contentView width:" + content.getWidth() + ",height:" + content.getHeight() + ",node:" + node);
    }
      if(node.getStyleWidth() < 1) {
        node.setStyleWidth(content.getPreferWidth());
      }
      if(node.getStyleHeight() < 1) {
        node.setStyleHeight(content.getPreferHeight());
      }
  }

  void measureTextAutoSize(ElementNode node, TVTextView tx) {
    if (node.isWidthWrapContent) {
      tx.measureTextWidth(node);
      if (LogUtils.isDebug()) {
        Log.e("AutoMeasure", "FIX_ITEM_SIZE fixTextView textSize tx:" + tx.getWidth() + ",height:" + tx.getHeight() + ",layoutWidth:" + node.getStyleWidth() + ",layoutHeight:" + node.getStyleHeight() + ",text:" + tx.getText());
      }
      node.measureParentWidthIfNeed(node, node.getStyleWidth());
//      measureWidth(tx, (int) node.getStyleHeight());
    }
    if (node.isHeightWrapContent) {
      if (LogUtils.isDebug()) {
        Log.i("AutoMeasure", "FIX_ITEM_SIZE ----updateItemLayout  :" + tx.hashCode() + ",isLayoutDirty:" + node.isLayoutDirty + ",getLayoutHeight:" + node.getLayoutHeight() + ",text:" + tx.getText());
      }
      FastAdapterUtil.measureHeight(tx, (int) node.getStyleWidth());
    }
  }


  void measureAllCreatedView(ElementNode node) {
    //主要测试autoWidth、autoHeight、以及FastFlex大小
    //执行过pending的任务后，所有的节点大小都已经在node中确认，需要在
    if (node.boundView instanceof FastFlexView) {
      measureFastFlexView(node);
    } else if (node.boundView instanceof TVTextView) {
      measureTextAutoSize(node, (TVTextView) node.boundView);
    } else {
      for (int i = 0; i < node.getChildCount(); i++) {
        measureAllCreatedView((ElementNode) node.getChildAt(i));
      }
    }

  }


  private void doDelayUpdatePropsRecursive(ElementNode node, HippyMap itemData, int itemPos) {
    if (node.boundView != null && node.isMarkToDelay) {
      //node.hasInit = false;
      doDiffProps4ElementNode(node, itemData, itemPos, STEP_DELAY_POST,true);
    }
    //更新节点里的map
    //node.props = toMap;
    if (node.templateNode instanceof ListNode) {
      // 如果嵌套list的话，不应该继续处理子节点，而是由子list自己处理
      return;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      // 这里的(ElementNode) node.getChildAt(i) 一定是ElementNode类型？
      doDelayUpdatePropsRecursive((ElementNode) node.getChildAt(i), itemData, itemPos);
    }
  }

//  private void doDiffProps4ElementNode(ElementNode node, HippyMap itemData, int itemPos, int step) {
//    doDiffProps4ElementNode(node,itemData,itemPos,step,true);
//  }
    private void doDiffProps4ElementNode(ElementNode node, HippyMap itemData, int itemPos, int step,boolean traverse) {
    final HippyMap rawMap = node.templateNode.getProps();
    HippyMap toMap, oldMap;
//    if (LogUtils.isDebug()) {
//      if (node.hasInit) {
//        Log.e(TAG, "testHasInit patch hasInit:" + true + ",node:" + node);
//      } else {
//        Log.i(TAG, "testHasInit patch hasInit:" + false + ",node:" + node);
//      }
//    }
//    if(mBoundListView != null && mBoundListView.getAutofocusPosition() > -1 && node.boundView instanceof ITVView && node.boundView.isFocusable()){
//      ((ITVView) node.boundView).setAutoFocus(mBoundListView.getAutofocusPosition() == itemPos);
//      Log.i(TAG,"发现autofocusPosition set Auto focus");
//    }
    toMap = new HippyMap();
    if (!node.hasInit) {
      //首次初始化时，将所有prop都初始化一遍
      oldMap = null;
      FastAdapterUtil.preInitElementNodeProps(node,itemData,rawMap,toMap);
    } else {
      //doPendingProps
      //只去比如pending的数据
      if(step == STEP_UPDATE_ITEM){
//        oldMap = node.pendingProps;
//        FastAdapterUtil.preGeneratePropsToUpdateWithoutPendingProp(node, itemData, rawMap, toMap);
        oldMap = node.pendingProps;
        FastAdapterUtil.preGeneratePropsToUpdateWithoutPendingProp(node,itemData,rawMap,toMap);
      }else {
        oldMap = node.pendingProps;
        FastAdapterUtil.preGeneratePropsToUpdate(node, itemData, rawMap, toMap);
      }
    }
    //和原有的map对比
    //old
    /**
     * {
     *   progress:100,
     *   list:[item1,item2]
     * }
     */
    //new
    /**
     * {
     *   progress:60,
     *   list:[item1,item2]
     * }
     */
//      if (step == FastAdapter.STEP_UPDATE_ITEM) {
//        String stepPrefix = ""+step;
//        Log.i("DebugReplaceItem","-------old Map:"+oldMap);
//        if(oldMap != null) {
//          for (String key : oldMap.keySet()) {
//            Log.i("DebugReplaceItem", stepPrefix + " ----diffPatch path prop:" + key + ",value:" + oldMap.get(key));
//          }
//        }
//        Log.i("DebugReplaceItem","-------toMap Map:"+toMap);
//        for (String key : toMap.keySet()) {
//          Log.i("DebugReplaceItem", stepPrefix+" ----diffPatch path prop:" + key+",value:"+toMap.get(key));
//        }
//      }
//      if(LogUtils.isDebug()){
//        Log.d("DebugReplaceItem","---------------oldMap---------------------- size:"+ (oldMap == null ? 0 : oldMap.size()));
//        InternalExtendViewUtil.logMap(oldMap,"DebugReplaceItem");
//        Log.i("DebugReplaceItem","--------------------toMap---------------------- size:"+ (toMap == null ? 0 : toMap.size()));
//        InternalExtendViewUtil.logMap(toMap,"DebugReplaceItem");
//      }
    HippyMap toUpdate = DiffUtils.diffProps(oldMap, toMap, 0);
//      Log.i("DebugReplaceItem","--------------------toUpdate---------------------- size:"+ (toUpdate == null ? 0 : toUpdate.size()));
//      InternalExtendViewUtil.logMap(toUpdate,"DebugReplaceItem");
    if(toUpdate != null && toUpdate.containsKey(PendingViewController.PROP_LIST)){
      //FIXME 这里最好变成在调用updateItem时一次性更新时的判断，而不是所有的更新都生效
//      if(LogUtils.isDebug()) {
//        Log.e(TAG, "updateItem 包含嵌套List toMap  traverse : " + traverse);
//        if (toUpdate.get(PendingViewController.PROP_LIST) instanceof HippyArray) {
//          final HippyArray array = toUpdate.getArray(PendingViewController.PROP_LIST);
//          for (int i = 0; i < array.size(); i++) {
//            final Object o = array.get(i);
//            Log.i("updateItem","----updateItem for list , item:"+o);
//          }
//        }
//      }
      //列表这里优化一下，加一个判断
//      if(itemData.containsKey(PendingViewController.PROP_UPDATE_NESTED)){
//        //这里用户期望不刷新下级列表
//        final boolean updateNested = itemData.getBoolean(PendingViewController.PROP_UPDATE_NESTED);
//        if(!updateNested){
//          Log.e(TAG, "updateItem diffPatch 用户设置了updateChildren false,忽略下级列表刷新");
//          toUpdate.remove(PendingViewController.PROP_LIST);
//        }
//      }

      if(!traverse){
        toUpdate.remove(PendingViewController.PROP_LIST);
      }
    }
    if (LogUtils.isDebug() && step == FastAdapter.STEP_UPDATE_ITEM) {
      String stepPrefix = ""+step;
        Log.i("DebugReplaceItem", stepPrefix+" diffPatch path size:" + toUpdate.size() + ",oldSize:" + (oldMap == null ? 0 : oldMap.size()));
        for (String key : toUpdate.keySet()) {
          Log.i("DebugReplaceItem", stepPrefix+" ----diffPatch path prop:" + key+",value:"+toUpdate.get(key));
        }
    }
    //执行map中的属性变化
    final HippyViewController vc = CustomControllerHelper.getViewController(getControllerManager(), node.templateNode);
    CustomControllerHelper.updateExtraIfNeed(vc, node.boundView, node.templateNode);
    FastAdapterUtil.invokePropsSync(vc, toUpdate, itemPos, node, FastAdapter.this,step); // 用处理好的属性key和value 进行处理


  }

  private static boolean isAddVif4ElementNode(ElementNode node,HippyMap itemData,int itemPos){
    if(node.initConfig.needCheckVIF && node.templateNode != null){
      boolean value = TemplateCodeParser.parseBooleanFromPendingProp(
        TemplateCodeParser.PENDING_PROP_CREATE_IF, itemData, node.templateNode.getProps().get(TemplateCodeParser.PENDING_PROP_CREATE_IF));
      if (value) {
        if(LogUtils.isDebug()){
          Log.e("CheckVIF","+++dealVif4ElementNode crateView condition true :"+node.templateNode.getProps().get(TemplateCodeParser.PENDING_PROP_CREATE_IF)+",itemPos:"+itemPos);
        }
        return true;
      }
    }
    return false;
  }

  void onElementViewCreated(ElementNode en){
    if (en.boundView instanceof HippyImageView) {
//      Log.i("DebugImage","onElementViewCreated imageView :"+en);
      if(globalConfig != null) {
        ((HippyImageView) en.boundView).setFadeEnabled(globalConfig.fadeImageEnable);
        if (globalConfig.fadeImageEnable) {
          ((HippyImageView) en.boundView).setFadeDuration(globalConfig.fadeDuration);
        }
      }
      if(en.initConfig.enableCoverRole){
        ItemRootView iv = findItemRootView(en);
//        Log.v("ZHAOPENG","update all preLoadView node.boundView:"+ExtendUtil.debugViewLite(en.boundView)+",iv:"+ExtendUtil.debugViewLite(iv));
        if(iv != null) {
          iv.registerCoverView(en);
        }
      }

    }
  }

  ItemRootView findItemRootView(ElementNode e){
    ElementNode root = e != null ? e.rootNode : null;
    if(root != null){
      if(root.boundView instanceof ItemRootView) {
        return (ItemRootView) root.boundView;
      }
      if(root.boundView != null && root.boundView.getParent() instanceof ItemRootView){
        return (ItemRootView) root.boundView.getParent();
      }
    }
    return null;
  }

  void createViewRecursiveOnVIF(ElementNode en,HippyMap itemData,View parent){
    RenderNode templateNode = en.templateNode;
    if(LogUtils.isDebug() && en.initConfig.needCheckVIF) {
      Log.i(TAG, "createViewRecursiveOnVIF------------------------ ------------------------------------------------- item:"+itemData
      +"en.boundView:"+en.boundView);
    }
    if(en.boundView == null ){
      if(en.initConfig.needCheckVIF){
        if(isAddVif4ElementNode(en,itemData,en.adapterPosition)) {
          en.boundView = createView4ElementNode(getControllerManager(), en, getRootListView(), parent,STEP_VIF);
          onElementViewCreated(en);
        }else {
          if(LogUtils.isDebug()) {
            Log.e("CheckVIF", "createViewRecursiveOnVIF  return on isAddVif4ElementNode false:" + templateNode + ",itemData:" + itemData);
          }
          return;
        }
      }else{
        en.boundView = createView4ElementNode(getControllerManager(), en, getRootListView(),parent, STEP_VIF);
        onElementViewCreated(en);
//        Log.i("CheckVIF", "createViewRecursiveOnVIF  add on needCheckVIF false:" + templateNode);
      }
    }else{
      if(en.initConfig.needCheckVIF){
        if(isAddVif4ElementNode(en,itemData,en.adapterPosition)) {
          if (en.boundView.getParent() == null) {
//            Log.i("CheckVIF", "createViewRecursiveOnVIF  attachViewToParent  on en.boundView != null:" + en.boundView+",item:"+itemData);
            attachViewToParent(en);
          }
        }else{
//          Log.e("CheckVIF", "createViewRecursiveOnVIF  return attachViewToParent isAddVif4ElementNode false:" + templateNode+",itemData:"+itemData);
          return;
        }
      }else{
        if (en.boundView.getParent() == null) {
//          Log.i("CheckVIF", "createViewRecursiveOnVIF  attachViewToParent  on en.boundView != null:" + en.boundView+",item:"+itemData);
          attachViewToParent(en);
        }//          Log.e("CheckVIF", "createViewRecursiveOnVIF  attachViewToParent  parent != null :" + en.boundView+",item:"+itemData);
      }
    }
    if (en.getChildCount() > 0) {
      for (int i = 0; i < en.getChildCount(); i++) {
        ElementNode child = (ElementNode) en.getChildAt(i);
        if (Utils.isPendingListNode(templateNode) && Utils.isPendingItemNode(child.templateNode)) {
          break;
        }
        createViewRecursiveOnVIF(child,itemData,en.boundView);
      }
    }
  }

  static void clearNodeView(ElementNode node){
    if (node.boundView != null) {
      FastAdapterUtil.removeFromParentIfNeed(node.boundView);
//      if (LogUtils.isDebug()) {
//        Log.e("CheckVIF", "createViewRecursiveOnVIF ---clearNodeView:" + node.templateNode);
//      }
    }
    for(int i = 0; i < node.getChildCount(); i ++){
      clearNodeView((ElementNode) node.getChildAt(i));
    }
    node.boundView = null;
    node.hasInit = false;
    node.hasCreateView = false;
  }

  static void removeVif4ElementNode(ElementNode node,HippyMap itemData,int itemPos){
    if(node.initConfig.needCheckVIF && node.templateNode != null && node.boundView != null){
      boolean value = TemplateCodeParser.parseBooleanFromPendingProp(
        TemplateCodeParser.PENDING_PROP_CREATE_IF, itemData, node.templateNode.getProps().get(TemplateCodeParser.PENDING_PROP_CREATE_IF));
        if(!value) {
          if (LogUtils.isDebug()) {
            Log.i("CheckVIF", "createViewRecursiveOnVIF ---clearNodeView removeVif4ElementNode !value itemPos" + itemPos+",node.boundView:"+node.boundView);
          }
          if (node.boundView != null) {
            clearNodeView(node);
          }
        }else{
          if (LogUtils.isDebug()) {
            Log.e("CheckVIF", "createViewRecursiveOnVIF ---clearNodeView removeVif4ElementNode itemPos" + itemPos);
          }
        }
    }
  }


//  protected void onAfterDiffProps(ElementNode node) {
//    //
//    if (node.boundView instanceof HippyImageView && HippyImageViewController.CLASS_NAME.equals(node.templateNode.getClassName())) {
//      HippyImageView.RoleType roleType = ((HippyImageView) node.boundView).getRoleType();
//      if(roleType == HippyImageView.RoleType.COVER){
//        Log.v("ZHAOPENG","update all preLoadView node.boundView:"+ExtendUtil.debugViewLite(node.boundView)+",rootView:"+node);
//        node.rootNode.registerCoverView((HippyImageView) node.boundView);
//      }
//    }
//  }

  /**
   * 更新所有placeholder阶段预先加载view的属性
   * @param node
   * @param itemData
   * @param itemPos
   */
  private void updateAllPreLoadViewRecursive(Holder holder,ElementNode node, HippyMap itemData, int itemPos,boolean traverse,int step) {
//    RenderNode templateNode = node.templateNode;
    //1. 先删除
    //2. 创建
    //3. 执行props
    removeVif4ElementNode(node,itemData,itemPos);
    boolean isInvokeProps = node.boundView != null;
    if (isInvokeProps) {
      //只有嵌套的子列表，还有placeholder才执行属性方法
      isInvokeProps = node.initConfig.createViewOnInit ;
      //如果有postDelay，直接执行
//      isInvokeProps |= (node.initConfig.enablePostDelay);
      isInvokeProps |= (node.initConfig.enablePostDelay && !node.initConfig.enableDelayLoad);
    }
    //如果是更新item，也执行属性方法
    isInvokeProps |= (step == STEP_UPDATE_ITEM);
    if(isInvokeProps){
      if (node.boundView instanceof FastListView) {
        //这里嵌套的子list时，将任务暂时取消
        ((FastListView) node.boundView).pausePostTask();
      }
      doDiffProps4ElementNode(node,itemData,itemPos,step,traverse);
//      if (node.boundView instanceof HippyImageView && HippyImageViewController.CLASS_NAME.equals(node.templateNode.getClassName())) {
//        HippyImageView.RoleType roleType = ((HippyImageView) node.boundView).getRoleType();
//        if(roleType == HippyImageView.RoleType.COVER){
//          Log.v("ZHAOPENG","update all preLoadView node.boundView:"+ExtendUtil.debugViewLite(node.boundView)+",rootView:"+node);
//          //node.rootNode.coverView = ((HippyImageView) node.boundView);
//          if (holder.findItemRootView() != null) {
//            Objects.requireNonNull(holder.findItemRootView()).registerCoverView((HippyImageView) node.boundView);
//          }
//        }
//      }
      if (enablePlaceholder && node.boundView != null) {
        if(!(node.boundView instanceof ItemContainer)) {
          //如果node.boundView是一个ItemContainer,则他的可聚焦性在创建时已经确定，不能在这里置成false
          if(!node.rootNode.initConfig.disablePlaceholderFocus){
            node.boundView.setFocusable(false);
          }
        }

      }
    }
//    if (node.isRootItem) {
//      if (node.boundView instanceof TVBaseView) {
//        Log.i("DebugPlaceholder","root node boundView focusScale:"+((TVBaseView) node.boundView).getFocusScale()+",view:"+ExtendUtil.debugViewLite(node.boundView));
//      }
//    }

    //更新节点里的map
    //node.props = toMap;
    if (node.templateNode instanceof ListNode) {
      // 如果嵌套list的话，不应该继续处理子节点，而是由子list自己处理
      Log.e("DebugReplaceItem","updateAllPreLoadViewRecursive  node is ListNode,return ");
      return;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      // 这里的(ElementNode) node.getChildAt(i) 一定是ElementNode类型？
      final ElementNode child = (ElementNode) node.getChildAt(i);
      updateAllPreLoadViewRecursive(holder,child, itemData, itemPos,traverse,step);
    }
  }

    /**
     * 将所有ElementNode的属性在主线程里全部更新
     * @param node
     * @param itemData
     * @param itemPos
     */
  private void updateAllPropsRecursive(ElementNode node, HippyMap itemData, int itemPos, View parent,boolean traverse,int step) {
    if(node.initConfig.needCheckVIF){
      //先删除
      removeVif4ElementNode(node,itemData,itemPos);
      //再添加
      createViewRecursiveOnVIF(node,itemData,parent);
    }
    if (node.boundView == null) {
      return;
    }
    doDiffProps4ElementNode(node,itemData,itemPos,step,traverse);
    //更新节点里的map
    //node.props = toMap;
    if (node.templateNode instanceof ListNode) {
      // 如果嵌套list的话，不应该继续处理子节点，而是由子list自己处理
      return;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      // 这里的(ElementNode) node.getChildAt(i) 一定是ElementNode类型？
      updateAllPropsRecursive((ElementNode) node.getChildAt(i), itemData, itemPos, node.boundView,traverse,step);
    }
  }

  /**
   * 在主线程里将ElementNode的属性更新
   * @param node
   * @param itemData
   * @param itemPos
   */
  private void postUpdatePropsRecursive(ElementNode node, HippyMap itemData, int itemPos, int step) {
//    if(node.boundView != null){
//    if(node.boundView != null && !node.initConfig.createViewOnInit && !node.isMarkToDelay){
    if(node.boundView != null && !node.initConfig.createViewOnInit && !node.isMarkToDelay){
//      if(node.initConfig.needCheckVIF){
//        Log.i("CheckVIF","doPostUpdatePropsRecursive handle needCheckVIF node.boundView:"+node.boundView+",itemData:"+itemData);
//      }
      doDiffProps4ElementNode(node,itemData,itemPos,step,true);
    }else if(LogUtils.isDebug()){
      if(node.initConfig.needCheckVIF){
        Log.e("CheckVIF","!!doPostUpdatePropsRecursive no handle needCheckVIF node.boundView:"+node.boundView+",itemData:"+itemData);
      }
    }
    //更新节点里的map
    //node.props = toMap;
    if (node.templateNode instanceof ListNode) {
      // 如果嵌套list的话，不应该继续处理子节点，而是由子list自己处理
      return;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      // 这里的(ElementNode) node.getChildAt(i) 一定是ElementNode类型？
      postUpdatePropsRecursive((ElementNode) node.getChildAt(i), itemData, itemPos,step);
    }
  }

  public static FastItemView findTVItemViewById(View view, String id) {
//    if(LogUtils.isDebug()) {
//      LogUtils.d("ZHAOPENG","findTVItemViewById id:"+id+",View:"+view);
//    }
    if (id == null || view == null) {
      return null;
    }
    if (view instanceof FastItemView && id.equals(view.getTag(R.id.tag_item_id))) {
      return (FastItemView) view;
    }
    if (view instanceof IPageRootView) {
//      Log.e("ZHAOPENG","findTVItemViewById  fid IPageRootView id:"+id+",getPageContentView:"+((IPageRootView) view).getPageContentView());
      FastItemView result = findTVItemViewById(((IPageRootView) view).getPageContentView(), id);
      if (result != null) {
        return result;
      }
    }
    if (view instanceof ViewGroup) {
      if (view instanceof HippyViewGroup) {
        if (((HippyViewGroup) view).isPageHidden()) {
          //只找没有隐藏的页面
          LogUtils.e(TAG, "findTVItemViewById return on pageHidden");
          return null;
        }
      }
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View result = findTVItemViewById(((ViewGroup) view).getChildAt(i), id);
        if (result != null) {
          if (LogUtils.isDebug()) {
            LogUtils.i(TAG, "findTVItemViewById found target id:" + id + ",View:" + view);
          }
          return (FastItemView) result;
        }
      }
    }
    return null;
  }

  public void setStableIdKey(String key){
    this.mUserKeyName = key;
    if(LogUtils.isDebug()) {
      Log.i("DebugUpdate", "setKeyName mUserKeyName:" + mUserKeyName+",this:"+hashCode());
    }
    try {
      setHasStableIds(!TextUtils.isEmpty(key));
    }catch (Throwable t){
      Log.e("DebugUpdate","setStableIdKey error",t);
    }
  }

  public String getStableIdKey(){
    return mUserKeyName;
  }

  final Object getIDFromData(int position){
    return FastAdapterUtil.getIDFromData(getRawObject(position));
  }

  public int findItemPositionBySID(final String id) {
    if (id == null) {
      return -1;
    }
    int count = getItemCount();
    for (int i = 0; i < count; i++) {
      Object item = getRawObject(i);
      if (item instanceof HippyMap) {
        HippyMap map = (HippyMap) item;
        if (id.equals(FastAdapterUtil.getIDFromData(map))) {
          return i;
        }
      }
    }
    return -1;
  }




  // 属性赋值主要方法
  void preConfigID4Item(Holder holder, ElementNode itemNode, int position, HippyMap map){
    //这里方法主要为了兼容之前的版本，这里不再需要单独设置，可以直接使用sid属性设置
    final View contentView = FastAdapterUtil.getRealContent(holder);
//    if(itemNode.templateNode.getProps().containsKey("sid")){
//
//    }
    boolean hasSetID = false;
    if (contentView != null) {
      if (map.containsKey("id")) {
        ExtendUtil.putViewSID(contentView,map.getString("id"));
        hasSetID = true;
      }else{
//        ExtendUtil.putViewSID(contentView,null);
//        if (map.containsKey("key")) {
//          ExtendUtil.putViewSID(contentView,map.getString("key"));
//          hasSetID = true;
//        }
      }
      if(map.containsKey("_id")){
        ExtendUtil.putViewSID(contentView,map.getString("_id"));
      }else{
          if(!hasSetID) {
            ExtendUtil.putViewSID(contentView, null);
          }
      }
    }
    if(LogUtils.isDebug()){
      Log.i("configID4Item","configID4Item contentView :"+ExtendUtil.debugViewLite(contentView));
    }
  }

  public void preUpdatePropsRecursive(Holder holder, int position, boolean traverse) {
    this.preUpdatePropsRecursive(holder, position, traverse,STEP_ON_BIND);
  }


  @Deprecated
  public void updateItemOnReplace(Holder holder, int position,boolean traverse){
//    final ItemEntity ie = getItemEntity(position);
//    if (ie != null) {
//      if (ie.updateItemDirty) {
//        Log.e("DebugReplaceItem","updateItemOnReplace  found updateItemDirty ie:"+ie.raw);
//      }
//      ie.updateItemDirty = false;
//    }
    this.preUpdatePropsRecursive(holder,position,traverse,STEP_UPDATE_ITEM);
  }


  /**
   * 使用position对应数据对象，将holder中所有view使用数据更新
   * @param holder
   * @param position
   */
  public void preUpdatePropsRecursive(Holder holder, int position, boolean traverse, int step) {
    if (LogUtils.isDebug()) {
      int itemTpe = getItemViewType(position);
      Log.d(TAG, "FastAdapterEvent exeBindViewHolder pos :" + position
        +",itemType:" + itemTpe + ",holder:" + Utils.hashCode(holder)
        + ",itemView:" + Utils.hashCode(holder.itemView) + ",attached:" + holder.attached
        + ",traverse:" +traverse
        + ",enablePlaceholder:" +enablePlaceholder
      )
      ;
    }
    final ViewTag vt = holder.tag;
    final ElementNode rootNode = vt.getRootNode();
    //TODO 遍历出需要删除的view,并且删除
    //遍历Element节点
    //将pendingMap的值根据position拿到，将toMap中对应的值进行赋值
    //将fromMap 和 toMap 去比对
    //执行出比对完需要更新的属性
    vt.cancelWork();
    final HippyMap map = (HippyMap) getRawObject(position);
    preConfigID4Item(holder,rootNode,position,map);
    //vt.work(new ElementDiffPatch(this, rootNode, map, position,getRootListView()));
    if (enablePlaceholder) {
      updateAllPreLoadViewRecursive(holder,rootNode,map,position,traverse,step);
//      if(rootNode.boundView instanceof ITVView && holder.itemView instanceof ItemRootView && holder.itemView.isFocusable()) {
//        ItemRootView ir = (ItemRootView) holder.itemView;
//        ITVView tv = (ITVView) rootNode.boundView;
//        ir.setFocusScaleX(rootNode.tem);
//        rootNode.initConfig.placeholderScale
//        ir.setFocusScaleY(tv.getFocusScaleY());
//        tv.setFocusScale(1);
//      }
    }else {
      updateAllPropsRecursive(rootNode, map, position,null,traverse,STEP_INIT);
    }
    //dispatchPropsRecursiveAsync(rootNode,map,position);
  }

  public void requestItemLayout(Holder holder, int position) {

    final ViewTag vt = holder.tag;
    final ElementNode node = vt.getRootNode();
    if (node != null) {
      measureAllCreatedView(node);
      node.calculateLayout();
      //layout所有delay的View
      updateItemLayoutInside(node);
    }
  }

  private void sendOnBindEvent4singleton(Holder holder, int position) {
    this.sendEvent4Singleton(holder, position, "onBindItem");
  }

  private void sendEvent4Singleton(Holder holder, int position, String eventName) {
    if (holder.singleton) {
      if (position < 0 || dataList == null || dataList.size() < position) {
        return;
      }
      final HippyViewEvent event = new HippyViewEvent(eventName);
      final RenderNode node = holder.tag.template;
      HippyMap map = new HippyMap();
      map.pushInt("position", position);
      map.pushObject("item", getRawObject(position));
      if (node != null) {
        map.pushObject("name", Utils.getNameFromProps(node.getProps()));
      }
      if (LogUtils.isDebug()) {
        Log.i("BindEvent", "sendEvent4Singleton " + eventName + " position:" + position);
      }
      //event.send(holder.itemView, map);
      if(holder.itemView instanceof ItemContainer){
        if(((ItemContainer) holder.itemView).getContentView() != null) {
          final View contentView = ((ItemContainer) holder.itemView).getContentView();
          if (LogUtils.isDebug()) {
            Log.i("BindEvent", "sendEvent4Singleton contentView is " +contentView +",id : "+(contentView  == null ?  "null" : contentView.getId()));
          }
          eventDeliverer.sendEvent(event, ((ItemContainer) holder.itemView).getContentView().getId(), map);
        }
      }else{
        eventDeliverer.sendEvent(event, holder.itemView.getId(), map);
      }
    }
  }


  private void sendUnBindEvent4singleton(Holder holder, int position) {
    this.sendEvent4Singleton(holder, position, "onItemRecycled");
  }


  /**
   * 请求将placeholder替代成contentView
   * @param holder
   * @param node
   * @param position
   * @param itemData
   */
  private void requestPlaceholderToContent(final Holder holder,ElementNode node,int position,HippyMap itemData){
    if (holder.postTask != null) {
      holder.postTask.cancel = true;
    }
    if(LogUtils.isDebug()) {
      Log.i(TAG_POST, "PlaceholderToContent requestPlaceholderToContent position:" + position+",holder.itemView.hashCode():"+holder.itemView.hashCode()+",tag:"+holder.itemView.getTag());
    }
    if (node.initConfig.disablePlaceholder) {
      //如果节点disablePlaceholder，则不需要
      exePlaceholderToContent(holder,node,position,itemData);
    }else{
      PostTask task = new PostTask(node, new Runnable() {
        @Override
        public void run() {
          exePlaceholderToContent(holder,node,position,itemData);
          holder.postTask = null;
        }
      });
      holder.postTask = task;
      getRootListView().postTask(POST_TASK_CATEGORY_PLACEHOLDER_TO_CONTENT,holder.itemView.hashCode(),task,postDelay);
    }

  }

  /**
   * 执行将placeholder替代成contentView
   * @param holder
   * @param itemENode
   * @param position
   * @param itemData
   */
  private void exePlaceholderToContent(final Holder holder,ElementNode itemENode,int position,HippyMap itemData){
      if(LogUtils.isDebug()) {
        Log.i(TAG_POST, "PlaceholderToContent -----exePlaceholderToContent position:" + position);
      }
      if(!holder.placeholderState){
        if(LogUtils.isDebug()) {
          Log.e(TAG_POST, "PlaceholderToContent -----exePlaceholderToContent return !holder.placeholderState position:" + position);
        }
        return;
      }

      if(!holder.singleton){
        createViewRecursiveOnPostContent(itemENode,itemData,null);
        postUpdatePropsRecursive(itemENode,itemData,position,STEP_PLACEHOLDER_TO_CONTENT);
        //将所有数据与模版建立的view进行一一绑定
        //exeBindViewHolder(holder, position); // 属性赋值的主要方法
        //如果tv-item没有指定大小，而子view指定了大小，则使用子view中的大小
        measureAllCreatedView(itemENode);
        //属性绑定后，位置信息可能发生变化，重新计算
        itemENode.calculateLayout();
        //将itemView中的所有的View遍历，根据绑定的node节点来更新Layout
        updateItemLayoutInside(itemENode);
        dealDelayContent(holder,position);
        if (holder.itemView instanceof ItemRootView) {
          ((ItemRootView) holder.itemView).afterPostBind();
        }
      }
      if (holder.itemView instanceof ItemContainer) {
        ((ItemContainer) holder.itemView).toggle(false);
        holder.placeholderState = false;
      }
  }

  /**
   * singleton BindViewHolder
   * @param holder
   * @param position
   * @param itemENode
   */
  void onSingletBindViewHolder(@NonNull Holder holder, int position, ElementNode itemENode){
    if (LogUtils.isDebug()) {
      Log.d(TAG, "SingleItem onSingletBindViewHolder  position:" + position + ",holder.hasInit:" + holder.hasOnBind);
    }
    int itemWidth = 0;
    int itemHeight = 0;
    final RenderNode rootNode = holder.tag.template;
    View nodeView = Utils.findBoundView(context, rootNode);
//    if (nodeView instanceof FastItemView) {
//      ((FastItemView) nodeView).toggle(true);
//    }
    HippyMap itemData = (HippyMap) getRawObject(position);
    if(nodeView != null) {
      if(nodeView.getParent()  != holder.itemView){
        if (nodeView.getParent() instanceof ViewGroup) {
//      throw new IllegalStateException("FastAdapterEvent DebugPool onCreateViewHolder itemView的Parent不可有值 itemView:"+holder.itemView+",en:"+node+",viewType:"+viewType+",singleton:"+ holder.singleton);
          if (LogUtils.isDebug()) {
            Log.e(TAG, "SingleItem onSingletBindViewHolder nodeView remove from adapter vh" + holder.hashCode() + ",type:" + holder.type);
          }
          ((ViewGroup) nodeView.getParent()).removeView(nodeView);
        }
        if(holder.itemView instanceof ItemRootView){
          final ItemRootView itemRootView = (ItemRootView) holder.itemView;
          itemRootView.setContentView(nodeView,true);
        }
      }
      sendOnBindEvent4singleton(holder, position);
      itemWidth = nodeView.getWidth();
      itemHeight = nodeView.getHeight();
      if ( nodeView instanceof FastItemView) {

        if (rootNode.getChildCount() == 1) {
          itemWidth = rootNode.getChildAt(0).getWidth();
          itemHeight = rootNode.getChildAt(0).getHeight();
          Log.e(TAG, "SingleItem onSingletBindViewHolder fixTVItemSize:" + itemWidth + ",height:" + itemHeight);
        }

        //FastAdapterUtil.updateLayout(holder.itemView, holder.itemView.getLeft(), holder.itemView.getTop(), itemWidth, itemHeight);
        int[] layout = new int[4];

        FastAdapterUtil.findItemViewSize(rootNode,position,itemData,layout);
        if(itemWidth < 1){
          itemWidth = layout[2];
        }
        if(itemHeight < 1){
          itemHeight = layout[3];
        }
        FastAdapterUtil.updateLayout(holder.itemView, layout[0], layout[1], itemWidth, itemHeight);
        //FastAdapterUtil.updateLayout(nodeView, 0, 0, itemWidth, itemHeight);
        if (LogUtils.isDebug()) {
          Log.e(TAG, "SingleItem onSingletBindViewHolder updateLayout holder.itemView " + holder.itemView + ",nodeView:" + nodeView);
          Log.e(TAG, "SingleItem onSingletBindViewHolder updateLayout holder.itemWidth "
            + itemWidth + ",holder.itemHeight:" + itemHeight+",layout x :"+layout[0]+",layout y:"+layout[1]+",layout width:"+layout[2]+",layout height:"+layout[3]);
        }
      }
      itemENode.setStyleWidth(itemWidth);
      itemENode.setStyleHeight(itemHeight);
      //requestPlaceholderToContent(holder,itemENode,position,itemData);
    }else{
      Log.e(TAG,"SingleItem onSingletBindViewHolder  nodeView is null node :"+rootNode);
    }

    assert rootNode != null;
    rootNode.updateViewRecursive();
    if (FastItemNode.isFastItemNode(rootNode)) {
      final HippyMap rawMap = rootNode.getProps();
      Set<String> props = rawMap.keySet();
      for (String prop : props) {
        final String pendingProp = TemplateCodeParser.parsePlaceholderProp(prop, rawMap);
        // Log.d(TAG,"prepareItemViewAndCreatePatches pendingProp:"+pendingProp+",isTextProp:"+isTextProp+",isPendingProp:"+isPendingProp+",prop:"+prop);
        if (!TextUtils.isEmpty(pendingProp)) {
          //只理会FastItem中的layout、size、等自定义属性
          //用item中的数据，替换map中的数据
          final Object dataFromValue = TemplateCodeParser.getValueFromCode((HippyMap) getRawObject(position), pendingProp);
//          if(TemplateCodeParser.PENDING_PROP_LAYOUT.equals(prop) && dataFromValue instanceof HippyArray){
//            Log.e("ZHAOPENGLOG","兼容老版本错误 enable "+enablePlaceholder);
//            ((HippyArray) dataFromValue).setObject(0,0);
//            ((HippyArray) dataFromValue).setObject(1,0);
//          }
          //Log.e("ZHAOPENGLOG","兼容老版本错误 holder itemView "+holder.itemView);
          exeCustomPendingProp4Singleton(nodeView, prop, dataFromValue);
        }
      }
    }
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
//    super.onBindViewHolder(holder, position, payloads);

    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position,payloads);
      if(LogUtils.isDebug()) {
        Log.d("DebugUpdate", "onBindViewHolder  position:" + position + ",holder:" + holder.itemView.hashCode() + ",payloads:" + "[] "+",stableKey:"+mUserKeyName+",this:"+hashCode());
      }
    }else{
       for(Object payload : payloads){
         Payload p = (Payload) payload;
         switch (p.type){
           case TYPE_UPDATE:
               doBindViewHolder(holder, position,p);
             if(LogUtils.isDebug()) {
               Log.d("DebugUpdate", "onBindViewHolder  position:" + position + ",holder:" + holder.itemView.hashCode() + ",payloads:" + "update "+",stableKey:"+mUserKeyName+",this:"+hashCode());
             }
             break;
             case TYPE_LAYOUT:
               if(LogUtils.isDebug()) {
                 Log.d("DebugUpdate", "onBindViewHolder  position:" + position + ",holder:" + holder.itemView.hashCode() + ",payloads:" + "layout "+",stableKey:"+mUserKeyName+",this:"+hashCode());
               }
//               Log.d(TAG,"update item layout : "+ExtendUtil.debugViewLite(holder.tag.getRootNode().boundView));
               updateHolderLayout(holder, position);
              break;
           case TYPE_LAYOUT_ROOT:
             if(LogUtils.isDebug()) {
               Log.d("DebugUpdate", "onBindViewHolder  position:" + position + ",holder:" + holder.itemView.hashCode() + ",payloads:" + "layoutRoot "+",stableKey:"+mUserKeyName+",this:"+hashCode());
             }
//             Log.i(TAG,"update root Layout : "+ExtendUtil.debugViewLite(getRootListView()));
             getRootListView().notifyItemsLayoutChanged();
             break;
         }

       }
    }
  }

  public void updateHolderLayout(Holder holder, int position){
    if (holder.tag == null) {
      return;
    }
    ElementNode itemENode = holder.tag.getRootNode();
    int itemWidth = 0;
    int itemHeight = 0;
    if (holder.singleton) {
      //单例的holder绑定
      itemWidth = (int) itemENode.getStyleWidth();
      itemHeight =  (int) itemENode.getStyleHeight();
    } else {
      //bind之前将节点内一些属性reset
      //将所有数据与模版建立的view进行一一绑定
//      updateAllPropsOnViewHolder(holder, position,true); // 属性赋值的主要方法
      measureAllCreatedView(itemENode);
      //重新计算itemNode中的布局
      itemENode.calculateLayout();
//      if (holder.itemView instanceof FastItemView) {
//        FastAdapterUtil.keepWithChildSize(itemENode);
//      }
      //itemView大小由itemENode的大小决定
      itemWidth = (int) itemENode.getLayoutWidth();
      itemHeight = (int) itemENode.getLayoutHeight();
      if(enablePlaceholder){
        if(holder.itemView instanceof ItemContainer) {
          updateItemLayout4ItemRootView(holder.itemView, itemENode);
        }else{
          updateItemLayout(itemENode);
        }
      }else{
        //将itemView中的所有的View遍历，根据绑定的node节点来更新Layout
        updateItemLayout(itemENode);
        //measureItemTextView(itemENode);
        if (itemWidth < 1 || itemHeight < 1) {
          int[] size = fixItemViewSize(itemENode);
          itemWidth = size[0];
          itemHeight = size[1];
          if(itemENode.boundView != null) {
            FastAdapterUtil.updateLayout(itemENode.boundView, 0, 0, itemWidth, itemHeight);
          }
        }
      }
      //measureItemNode(itemENode);
    }
    if (itemWidth < 1 || itemHeight < 1) {
      Log.e(TAG, "FIX_ITEM_SIZE 计算尺寸错误:ItemWidth:" + itemWidth + ",height:" + itemHeight + ",itemType:" + getItemViewType(position) + ",position:" + position);
    }
    Log.i(FastListView.TAG,"updateHolderLayout  itemWidth:"+itemWidth+",itemHeight:"+itemHeight+",holder:"+holder.itemView.hashCode());

    boolean changed = false;
    //更新recyclerView中的尺寸
    if (holder.itemView.getLayoutParams() == null) {
      holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(itemWidth, itemHeight));
      changed = true;
    } else {
      changed = holder.itemView.getLayoutParams().width != itemWidth || holder.itemView.getLayoutParams().height != itemHeight;
      holder.itemView.getLayoutParams().width = itemWidth;
      holder.itemView.getLayoutParams().height = itemHeight;
    }
//    if (changed && getFastPendingView() != null) {
//      getFastPendingView().notifyItemsLayoutChanged();
//    }
    if(changed && getRootListView() != null && getRootListView() != mBoundListView){
      getRootListView().notifyItemsLayoutChanged();
    }

  }



  public RecyclerView.LayoutParams getItemLayoutParams(Holder holder) {
    return holder.itemView.getLayoutParams() instanceof RecyclerView.LayoutParams ? (RecyclerView.LayoutParams) holder.itemView.getLayoutParams() : null;
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, @SuppressLint("RecyclerView") int position) {
    this.doBindViewHolder(holder, position, null);
  }


  public void doBindViewHolder(@NonNull Holder holder, @SuppressLint("RecyclerView") int position, @Nullable Payload payload) {
    if (LogUtils.isDebug()) {
      Log.e(TAG_POST, "START>>>---------------------------------------------onBindViewHolder position:"+position+",template:"+(holder.tag.getRootNode() != null ? holder.tag.getRootNode().templateNode : null) +",type:"+holder.type);
      Log.i(TAG, "FastAdapterAttach onBindViewHolder  position:" + position + ",holder:" + Utils.hashCode(holder)+",tag:"+holder.itemView.getTag());
      Log.i(TAG, "PlaceholderToContent onBindViewHolder  position:" + position + ",holder:" + Utils.hashCode(holder)+",tag:"+holder.itemView.getTag());
    }
    if(LogUtils.isDebug()) {
      Log.d("BindEvent", "inner onBindViewHolder  position:" + position + ",holder:" + holder.itemView.hashCode());
    }
    holder.adapterPosition = position;
    int itemWidth = 0;
    int itemHeight = 0;
    if(holder.isErrorView){
      return;
    }
    removeDetached(position);
    final ViewTag vt = holder.tag;
    final ElementNode itemENode = vt.getRootNode();

    if(enablePlaceholder && holder.itemView instanceof ItemContainer) {
      //先展示placeholder
      if(!holder.hasOnBind || payload == null) {
        ((ItemContainer) holder.itemView).toggle(true);
      }else{
        if(holder.itemView.isFocused()){
          holder.itemView.refreshDrawableState();
        }
      }
      holder.placeholderState = true;
    }
    holder.hasOnBind = true;
    clearPostTask(holder);
    itemENode.recycled = false;
    itemENode.adapterPosition = position;
    if (holder.singleton) {
      //单例的holder绑定
      onSingletBindViewHolder(holder,position,itemENode);
      itemWidth = (int) itemENode.getStyleWidth();
      itemHeight =  (int) itemENode.getStyleHeight();
    } else {
      if(!(getRawObject(position) instanceof HippyMap)){
        return;
      }
      HippyMap itemData = (HippyMap) getRawObject(position);
      //bind之前将节点内一些属性reset
      notifyElementBeforeOnBind(itemENode, itemData);
      //将所有数据与模版建立的view进行一一绑定
      preUpdatePropsRecursive(holder, position,true); // 属性赋值的主要方法
      measureAllCreatedView(itemENode);
      //重新计算itemNode中的布局
      itemENode.calculateLayout();
      //itemView大小由itemENode的大小决定
      itemWidth = (int) itemENode.getLayoutWidth();
      itemHeight = (int) itemENode.getLayoutHeight();
      if(enablePlaceholder){
        requestPlaceholderToContent(holder,itemENode,position,itemData);
        if(holder.itemView instanceof ItemContainer) {
          updateItemLayout4ItemRootView(holder.itemView, itemENode);
        }else{
          updateItemLayout(itemENode);
        }
      }else{
        //将itemView中的所有的View遍历，根据绑定的node节点来更新Layout
        updateItemLayout(itemENode);
        //measureItemTextView(itemENode);
        if (itemWidth < 1 || itemHeight < 1) {
          int[] size = fixItemViewSize(itemENode);
          itemWidth = size[0];
          itemHeight = size[1];
          if(itemENode.boundView != null) {
            FastAdapterUtil.updateLayout(itemENode.boundView, 0, 0, itemWidth, itemHeight);
          }
        }
      }
      //measureItemNode(itemENode);
    if (LogUtils.isDebug()) {
      Log.i(TAG, "FastAdapterEvent DEAL_LAYOUT BeforeCalculate pos:" + position + ",rootElement:" + vt.getRootNode());
      Log.i(TAG, "FastAdapterEvent FIX_ITEM_SIZE  pos:" + position + ",width:" + itemWidth + ",height:" + itemHeight + ",type:" + getItemViewType(position) + ",this:" + Utils.hashCode(this));
      }
    }
    if (itemWidth < 1 || itemHeight < 1) {
      Log.e(TAG, "FIX_ITEM_SIZE 计算尺寸错误:ItemWidth:" + itemWidth + ",height:" + itemHeight + ",itemType:" + getItemViewType(position) + ",position:" + position);
    }
    //更新recyclerView中的尺寸
    if (holder.itemView.getLayoutParams() == null) {
      holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(itemWidth, itemHeight));
    } else {
      holder.itemView.getLayoutParams().width = itemWidth;
      holder.itemView.getLayoutParams().height = itemHeight;
    }
    /**  点击、长按、焦点事件*/
    if (onFastItemClickListener != null) {
      holder.itemView.setOnClickListener(v -> {
        Log.i("DebugClick", "onFastItemClickListener onItemClick  position:" + position + ",rootElement:" + vt.getRootNode());
        onFastItemClickListener.onItemClickListener(v, position);
      });
      holder.itemView.setOnLongClickListener(v -> onFastItemClickListener.onItemLongClickListener(v, position));
    }
    if (onFastItemFocusChangeListener != null) {
      //焦点事件
      holder.itemView.setOnFocusChangeListener((v, hasFocus) -> onFastItemFocusChangeListener.onFocusChange(v, hasFocus, position));
    }

    if (isListenBoundEvent) {
      sendOnBindEvent(rootListNodeID, position, listNode.getNode(), holder);
    }
    if (holder.itemView instanceof IRecyclerItemView) {
      //通知
      IRecyclerItemView itemView = (IRecyclerItemView) holder.itemView;
      final ItemEntity ie = getItemEntity(position);
      itemView.onBind(getRootListView(), position, ie == null ? null : ie.raw);
    }
    if(LogUtils.isDebug()) {
      Log.e(TAG_POST, "END>>>>---------------------------------------------onBindViewHolder position:" + position + "---------------------------------------------");
    }
    if (holder.itemView instanceof ItemRootView) {
      ((ItemRootView) holder.itemView).afterPreBind();
    }else{
        //当前的itemView不是ItemContainer时，设置contentView的itemSID
      if(itemENode.itemSID != null){
        ExtendUtil.putViewSID(holder.itemView, itemENode.itemSID);
      }
    }
  }

  private int[] fixItemViewSize(ElementNode itemNode) {
    int[] size = new int[2];
    if (itemNode.getChildCount() == 1) {
      size[0] = (int) itemNode.getChildAt(0).getLayoutWidth();
      size[1] = (int) itemNode.getChildAt(0).getLayoutHeight();
      if (LogUtils.isDebug()) {
        Log.v(TAG, "fixItemViewSize FIX_ITEM_SIZE  ,width:" + itemNode.getLayoutWidth() + ",height:" + itemNode.getLayoutHeight() + ",this:" + Utils.hashCode(this));
        if (itemNode.getLayoutWidth() < 1 || itemNode.getLayoutHeight() < 1) {
          Log.e(TAG, "fixItemViewSize error FIX_ITEM_SIZE  ,width:" + itemNode.getLayoutWidth() + ",height:" + itemNode.getLayoutHeight() + ",this:" + Utils.hashCode(this) + ",itemNode:" + itemNode);
        }
      }
    } else {
      if (itemNode.getChildCount() > 1) {
        //简单的measure
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < itemNode.getChildCount(); i++) {
          int cw = (int) itemNode.getChildAt(i).getLayoutWidth();
          int ch = (int) itemNode.getChildAt(i).getLayoutHeight();
          maxWidth = Math.max(maxWidth, cw);
          maxHeight = Math.max(maxHeight, ch);
        }
        size[0] = maxWidth;
        size[1] = maxHeight;
      } else {
        Log.e(TAG, "fixItemViewSize FIX_ITEM_SIZE 错误,node info:" + itemNode);
      }
    }
    return size;
  }



  @Override
  public int getItemViewType(int pos) {
    int position = getLoopPosition(pos);
//    if (LogUtils.isDebug()) {
//      Log.v(TAG, "FastAdapterEvent getItemViewType position " + position);
//    }

    if (dataList == null || dataList.size() < position) {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "FastAdapter getItemViewType dataList is empty position" + position);
      }
      return DEFAULT_ITEM_TYPE;
    }
    int type;
    final HippyMap map = (HippyMap) getRawObject(position);
    if (map == null || !map.containsKey("type")) {
      if (templateNodes.size() > (1 + (itemStoreNodes == null ? 0 : itemStoreNodes.size()))) { //如果template中的列表包含多个模版，则必须要传type
        Log.e(TAG, "FastAdapter getItemViewType map is " +
          "empty or type missing dataList" + dataList + ",this:" + Utils.hashCode(this) + ",itemData:" + map + ",pos:" + position);
        throw new IllegalArgumentException("getItemViewType error :确认每个item包含type参数  data:" + map);
      } else {
        type = getDefaultItemType();
      }
    } else {
      type = Integer.parseInt(map.getString("type"));
    }
    if (LogUtils.isDebug()) {
      Log.d(TAG, "FastAdapter getItemViewType return " + type);
    }
    return type;
  }

  @Override
  public int getItemCount() {
    if (templateNodes == null) {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "FastAdapter getItemCount return 0" + ",this:" + Utils.hashCode(this));
      }
      return 0;
    }
    final int count = getItemDataCount();
//    if (LogUtils.isDebug()) {
//      Log.d(TAG, "FastAdapter getItemCount return " + count + ",this:" + Utils.hashCode(this));
//    }
    return isInfiniteLoopEnabled() ? Integer.MAX_VALUE : count;
  }

  public int getItemDataCount(){
    return dataList != null ? dataList.size() : 0;
  }



  public void setData(HippyArray array) {
    if (this.dataList == null) {
      this.dataList = new HippyArray();
    } else {
      this.dataList.clear();
    }
    if (array != null) {
      for (int i = 0; i < array.size(); i++) {
        this.dataList.pushObject(new ItemEntity(array.get(i), i));
      }
    }
    clearDetachCaches();
    if (LogUtils.isDebug()) {
      Log.e(TAG, "FastAdapter setData array size: " + (array == null ? 0 : array.size()) + ",this:" + Utils.hashCode(this));
    }
//    if(getRootListView() != null && getRootListView() != mBoundListView){
//      getRootListView().notifyItemsLayoutChanged();
//    }
  }

  public void replaceItemData(int pos,Object data){
    if(this.dataList != null && this.dataList.size() > pos && pos > -1){
      this.dataList.setObject(pos,new ItemEntity(data,pos));
    }
  }

  public void addData(HippyArray array) {
    assert dataList != null;
    if (array != null) {
      int index = dataList.size();
      for (int i = 0; i < array.size(); i++) {
        this.dataList.pushObject(new ItemEntity(array.get(i), index++));
      }
    }
    if (LogUtils.isDebug()) {
      Log.e(TAG, "FastAdapter setData array size: " + (array == null ? 0 : array.size()) + ",this:" + Utils.hashCode(this));
    }
  }

  public void deleteData(int count) {
    if (dataList != null && this.dataList.size() >= count) {
      for (int i = 0; i < count; i++) {
        this.dataList.removeAt(this.dataList.size() - 1);
      }
    }
  }

  public void deleteData(int start, int count) {
//    if(dataList != null && this.dataList.size() >= count){
//      for(int i= 0; i < count; i ++){
//        this.dataList.removeAt(this.dataList.size() -1);
//      }
//    }
    if (dataList != null && dataList.size() > 0) { //3
      //start 0, count 2
      //start 1 ,count 3 0 1 2 3
      final int last = start + count - 1;
      for (int i = last; i >= start; i--) {
        this.dataList.removeAt(i);
      }
    }
  }

  public void clearData() {
    if (this.dataList != null) {
      this.dataList.clear();
    }
  }

  public void recycleAll() {

  }

  public void clearCache() {
    if (findOldCacheWorker() != null) {
      findOldCacheWorker().clear();
    }
    if (gSharedCachePools != null) {
      if(getCacheRootView() != null) {
        gSharedCachePools.remove(getPoolCacheKey(viewContext,getCacheRootView()));
      }
    }
  }

  public static class Holder extends RecyclerView.ViewHolder {

    ViewTag tag;
    boolean attached = false;
    boolean patched = false;
    boolean singleton;
    boolean hasOnBind = false;
    int adapterPosition = -1;
    boolean isErrorView = false;
    boolean placeholderState = true;
    PostTask postTask;

    int type;

    public Holder(@NonNull View itemView, ViewTag tag, int type, boolean singleton) {
      super(itemView);
      this.type = type;
      this.tag = tag;
      this.singleton = singleton;
    }

    public ElementNode getItemRootNode(){
      return tag.getRootNode();
    }

    public @Nullable ItemRootView findItemRootView(){
      if(itemView instanceof ItemRootView){
        return (ItemRootView) itemView;
      }
      return null;
    }


    void reset() {
//      hasInit = false;
      //adapterPosition = -1;
      hasOnBind = false;
      tag.reset();
    }

    void resetTextView(){
      tag.resetTextView();
    }

  }


  static class PostTask implements Runnable {
    boolean cancel = false;
    final Runnable task;
    ElementNode node;

    public PostTask(FastAdapter.ElementNode node,@NonNull Runnable task) {
      this.task = task;
      this.node = node;
    }

    @Override
    public void run() {
      if(cancel || node == null || node.rootNode.recycled){
        if(LogUtils.isDebug()){
          if (node == null) {
            Log.e(TAG_POST,"PostTask return on cancel:"+cancel+",node:"+ null+",this:"+this);
          }else{
            Log.e(TAG_POST,"PostTask return on cancel:"+cancel+",node recycled:"+node.rootNode.recycled+",this:"+this);
          }
        }
      }else{
        task.run();
      }
    }
  }

  void markCrateOnParent(ElementNode node,boolean create){
    if(node != null){
      node.initConfig.createViewOnInit = create;
      if (node.getParent() != null) {
        markCrateOnParent((ElementNode) node.getParent(),create);
      }
    }
  }

  /**
   *根据模版创建所有的Element节点
   * @param templateNode
   * @param tag
   * @param parentElement
   */
  void createAllElementNodeRecursive(@NonNull RenderNode templateNode, ViewTag tag, ElementNode parentElement) {
    DomNode templateDomNode = context.getDomManager().getNode(templateNode.getId());
    if (templateDomNode != null) {
      ElementNode en;
      if (parentElement == null) {
        en = tag.getRootNode();
        en.isPlaceholderEnable = this.enablePlaceholder;
      } else {
        en = new ElementNode();
      }
      Utils.cloneNode(en, templateDomNode);
      if (parentElement != null) {
        en.rootNode = parentElement.rootNode;
        parentElement.addChildAt(en, parentElement.getChildCount());
      }
      //用来标识一个节点的名称
      en.onNodeInit(templateNode);
      if(en.initConfig.isViewFocusable && en.rootNode != null){
        en.rootNode.initConfig.isViewFocusable = true;
      }
      //如果enablePlaceholder为true，则所有的view默认都不创建
      boolean createView = !this.enablePlaceholder;
     // final boolean isDelayLoad = en.isMarkToDelay || (parentElement != null && parentElement.isMarkToDelay);
      if(parentElement != null && parentElement.isMarkToDelay){
          en.loadDelay = parentElement.loadDelay + 20;
          //parent是delayLoad，则子节点同意是delayLoad
          tag.addDelayLoad(en);
          if(LogUtils.isDebug()) {
            Log.e("MakeContentLog", "add load delay node:" + en.templateNode + ",elementNode:" + en.hashCode() + ",root:" + en.rootNode.hashCode());
          }
          createView = false;
          en.isMarkToDelay = true;
      }else{
        final boolean isDelayLoad = en.isMarkToDelay;
        if (isDelayLoad) {
          //被delayLoad标识的view这里都不创建
          tag.enableLoadDelay = true;
          if (en.loadDelay > 0) {
            tag.addDelayLoad(en);
            if(LogUtils.isDebug()) {
              Log.e("MakeContentLog", "add load delay node:" + en.templateNode + ",elementNode:" + en.hashCode() + ",root:" + en.rootNode.hashCode());
            }
            //delayLoad的view，没有必要创建view
            //return;
            createView = false;
          }else{
            en.isMarkToDelay = false;
          }
        }
      }

      final boolean isPlaceholder = en.markAsPlaceholder;
//      if(LogUtils.isDebug() && this.enablePlaceholder){
//        if (templateNode.getProps() != null) {
//          Log.d(TAG,"-----------createOnCreateViewHolder isPlaceholder :"+isPlaceholder);
//          for(String s : templateNode.getProps().keySet()){
//            Log.d(TAG,"createOnCreateViewHolder prop s :"+s);
//          }
//          if (isPlaceholder) {
//            Log.e(TAG,"createOnCreateViewHolder isPlaceholder templateNode:"+templateNode);
//          }
//        }
//      }
      if (isPlaceholder) {
        tag.placeHolderNode = en;
        //被标记为placeholder的view不创建，在placeholder转变为content的时候，再进行创建
        createView = true;
      }
      //只创建嵌套的node
      if(!createView) {
        //简便FastPendingListView类型的被标记为不创建，这个强制创建
        boolean createOnPendingList = Utils.isPendingListNode(templateNode);
        if (en.rootNode != null && createOnPendingList) {
          en.rootNode.initConfig.hasNestList = true;
          //如果包含嵌套的List,它所有的节点及父节点都要直接创建
          markCrateOnParent(en,true);
        }
        createView = createOnPendingList;
      }
      if(!createView) {
        //根view一定要创建
        createView = en.isRootItem;
        if (en.isWidthWrapContent || en.isHeightWrapContent) {
          createView = true;
        }
      }
      if(createView && en.initConfig.needCheckVIF || (parentElement != null && parentElement.initConfig.needCheckVIF)) {
        createView = false;
      }
      if (parentElement != null) {
        //如果父组件不创建，子组件也不可创建view
        en.initConfig.createViewOnInit = createView && parentElement.initConfig.createViewOnInit;
      }else{
        en.initConfig.createViewOnInit = createView;
      }

      if (templateNode.getChildCount() > 0) {
        for (int i = 0; i < templateNode.getChildCount(); i++) {
          RenderNode child = templateNode.getChildAt(i);
          if (Utils.isPendingListNode(templateNode) && Utils.isPendingItemNode(child)) {
            if (LogUtils.isDebug()) {
              Log.e(TAG, "addViewOnCreateViewRecursive return,子节点是一个ListNode,跳过。node:" + templateNode);
            }
            break;
          }
          createAllElementNodeRecursive( child, tag, en);
        }
      }
    }
  }

  /**
   * 在创建ViewHolder时，为所有elementNode创建view
   * @param en
   * @param parent
   */
  void createViewRecursiveOnCreateViewHolder(ElementNode en,View parent){
    RenderNode templateNode = en.templateNode;
    if(en.boundView == null && en.initConfig.createViewOnInit){
      en.boundView = createView4ElementNode(getControllerManager(),en,getRootListView(),parent, STEP_INIT);
      onElementViewCreated(en);
    }
    if (en.getChildCount() > 0 && en.initConfig.createViewOnInit) {
      for (int i = 0; i < en.getChildCount(); i++) {
        ElementNode child = (ElementNode) en.getChildAt(i);
        if (Utils.isPendingListNode(templateNode) && Utils.isPendingItemNode(child.templateNode)) {
          if (LogUtils.isDebug()) {
            Log.e(TAG, "createItemElementNodeRecursive addViewOnCreateViewRecursive return,子节点是一个ListNode,跳过。node:" + templateNode);
          }
          break;
        }
        createViewRecursiveOnCreateViewHolder(child,en.boundView);
      }
    }
  }

  /**
   * 在placeholder转变为content时，为所有elementNode创建view
   * @param en
   * @param itemData
   * @param parent
   */
  void createViewRecursiveOnPostContent(ElementNode en, HippyMap itemData, View parent) {
    //assert templateDomNode != null;
    RenderNode templateNode = en.templateNode;

    //dealVif4ElementNode()
    if(en.boundView == null ){
      if(!en.isMarkToDelay) {
        if(en.initConfig.needCheckVIF){
          createViewRecursiveOnVIF(en,itemData,parent);
        }else {
          en.boundView = createView4ElementNode(getControllerManager(), en, getRootListView(), parent,STEP_PLACEHOLDER_TO_CONTENT);
          onElementViewCreated(en);
        }
      }
    }else{
      if(en.isMarkToDelay){
        changeVisibility(en,false);
      }
    }
    if (en.boundView instanceof FastListView) {
      ((FastListView) en.boundView).resumePostTask();
    }
    if (en.getChildCount() > 0) {
      for (int i = 0; i < en.getChildCount(); i++) {
        ElementNode child = (ElementNode) en.getChildAt(i);
        if (en.initConfig.needCheckVIF) {
          child.hasInit = false;
        }
//        if(en.isRootItem){
//          if (child.boundView != null && child.needRestoreVisible) {
//            child.needRestoreVisible = false;
//            //changeVisibility(child,true);
//          }
//        }
        if (Utils.isPendingListNode(templateNode) && Utils.isPendingItemNode(child.templateNode)) {
          if (LogUtils.isDebug()) {
            Log.e(TAG, "createItemElementNodeRecursive addViewOnCreateViewRecursive return,子节点是一个ListNode,跳过。node:" + templateNode);
          }
          break;
        }
        createViewRecursiveOnPostContent(child,itemData,en.boundView);
      }
    }
  }

  static int getUpdateLayoutType(ElementNode en) {
    return en.hashCode();
  }

  void postAllDelayContent(final ViewTag vt,final  ElementNode itemENode, Holder holder, int position) {
    if (vt.getDelayLoadNodes() != null) {
//      if (LogUtils.isDebug()) {
//        Log.d(TAG, "postAllDelayContent run,position:" + position + ",size:" + vt.getDelayLoadNodes().size());
//        Log.d(TAG, "MakeContentLog run,position:" + position + ",size:" + vt.getDelayLoadNodes().size()+",root:"+itemENode.rootNode.hashCode());
//      }
      for (int i = 0; i < vt.getDelayLoadNodes().size(); i++) {
        final ElementNode en = vt.getDelayLoadNodes().get(i);
        //为了不要频繁更新layout
        if (en.pendingWorker != null) {
          en.pendingWorker.cancel();
          en.pendingWorker = null;
        }
        int postType = getUpdateLayoutType(en);
//          if (LogUtils.isDebug()) {
//            Log.d(TAG, "postAllDelayContent run,position:" + position+",getRootListView:"+getRootListView()+",en templateNode :"+en.templateNode+",boundView:"+en.boundView);
//          }
          getRootListView().postTask(POST_TASK_CATEGORY_DELAY_LOAD, postType, new PostTask(en, () -> {
            if (enablePlaceholder) {
              if(en.rootNode != null && en.rootNode.recycled){
                if (LogUtils.isDebug()) {
                  Log.e(TAG, "postAllDelayContent SCROLL_POSTER return on en.rootNode.recycled pos:" + position);
                }
                return;
              }
            }else{
              if(!holder.attached){
                if (LogUtils.isDebug()) {
                  Log.e(TAG, "postAllDelayContent SCROLL_POSTER return on Holder is detached pos:" + position);
                }
                return;
              }
            }
            if (LogUtils.isDebug()) {
//              Log.e(TAG, "postAllDelayContent SCROLL_POSTER exeTask,holder:" + getHolderTaskType(holder) + ",attached:" + holder.attached + ",dataList.size():" + dataList.size() + ",position:" + position);
//              Log.v("MakeContentLog", "postAllDelayContent SCROLL_POSTER exeTask,holder:" + getHolderTaskType(holder) + ",attached:" + holder.attached + ",dataList.size():" + dataList.size() + ",position:" + position+",rootNode:"+en.rootNode.hashCode());
            }
            //updateItemContentRecursive(getControllerManager(), null, holder.tag, itemENode);
            if (dataList.size() > position) {
//              if (LogUtils.isDebug()) {
//                Log.i(TAG, "FastAdapterEvent createContent  BeforeCalculate pos:" + position + ",templateNode:" + en.templateNode);
//              }
              if (en.getParent() instanceof ElementNode) {
                makeContentReady(getControllerManager(), holder.tag, en, getRootListView(),((ElementNode) en.getParent()).boundView,"");
              }else{
                makeContentReady(getControllerManager(), holder.tag, en, getRootListView(),null,"");
              }
              if (en.boundView instanceof PostTaskHolder) {
                ((PostTaskHolder) en.boundView).setRootPostHandlerView(getRootListView());
              }
              final HippyMap map = (HippyMap) getRawObject(position);
              doDelayUpdatePropsRecursive(en,map,position);

//              itemENode.calculateLayout();
              if (LogUtils.isDebug()) {
//        Log.e(TAG, "FastAdapterEvent DEAL_LAYOUT AfterCalculate pos:" + position + ",rootElement:" + vt.getRootNode() + "getItemType:" + getItemViewType(position));
//                Log.i(TAG, "FastAdapterEvent createContent DEAL_LAYOUT AfterCalculate pos:" + position + ",Style" + vt.getRootNode().styleToString());
              }
              //将itemView中的所有的View遍历，根据绑定的node节点来更新Layout
              //updateItemLayout(vt.getDelayLoadNodes());
              en.hasCreateView = true;
            } else {
              if (LogUtils.isDebug()) {
                Log.e(TAG, "postAllDelayContent SCROLL_POSTER return on position invalid: count is " + dataList.size() + ",position:" + position);
              }
            }
          }) , en.loadDelay);
          //updateLayout
          getRootListView().postTask(POST_TASK_CATEGORY_UPDATE_LAYOUT, postType, () -> {
            //重新计算布局
            if (enablePlaceholder) {
              if(en.rootNode != null && en.rootNode.recycled){
                if (LogUtils.isDebug()) {
                  Log.e(TAG, "postAllDelayContent SCROLL_POSTER  updateLayout return on en.rootNode.recycled pos:" + position);
                }
                return;
              }
            }else{
              if(!holder.attached){
                if (LogUtils.isDebug()) {
                  Log.e(TAG, "postAllDelayContent SCROLL_POSTER updateLayout return on Holder is detached pos:" + position);
                }
                return;
              }
            }
            if (en.boundView != null) {
              measureAllCreatedView(en);
              itemENode.calculateLayout();
              //layout所有delay的View
//              if (holder.itemView instanceof ItemRootView) {
//                  updateItemLayout4ItemRootView( holder.itemView,en);
//              }else {
//                updateItemLayout(en);
//                en.hasUpdateLayout = true;
//              }
              updateItemLayout(en);

            }
          }, en.loadDelay + 20);
      }

    } else {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "postAllDelayContent return on getDelayLoadNodes,position:" + position);
      }
    }


  }

  final static int STEP_INIT = 0;
  final static int STEP_ON_BIND = 1;
  final static int STEP_PLACEHOLDER_TO_CONTENT = 2;
  final static int STEP_VIF = 3;
  final static int STEP_DELAY_POST = 4;
  final static int STEP_UPDATE_ITEM = 5;

  static boolean attachViewToParent(ElementNode cn){
    if (cn.boundView == null) {
      return false;
    }
    View parent = null;
    parent = cn.getParent() != null ? ((ElementNode) cn.getParent()).boundView : null;
    if (parent instanceof ViewGroup && !(parent instanceof FastPendingView)) {
      ((ViewGroup) parent).addView(cn.boundView);
      if (LogUtils.isDebug()) {
        Log.e(TAG, "createViewRecursiveOnVIF attachToParent  :" + cn.boundView + ",parent:" + parent);
      }
      return true;
    }
    return false;
  }

  /**
   * 为ElementNode创建view
   *
   * @param m
   * @param cn
   * @param rootList
   * @return
   */
  private static View createView4ElementNode(ControllerManager m, ElementNode cn, FastListView rootList,final View parentView,boolean addToParent,int step){
     String tag = "";
    switch (step){
      case STEP_VIF:
        tag = "CheckVIF";
        break;
      case STEP_DELAY_POST:
        tag = "MakeContentLog";
        break;
    }
    View view = null;
    final RenderNode templateNode = cn.templateNode;
    View parent = parentView;
    if(parent == null) {
      parent = cn.getParent() != null ? ((ElementNode) cn.getParent()).boundView : null;
    }
    final HippyViewController viewComponent = CustomControllerHelper.getViewController(m, cn.templateNode);

//    if (step == STEP_PLACEHOLDER_TO_CONTENT) {
//      //在第二阶段，检查 v-if判断
//      boolean needCreate = dealVif4ElementNode(cn,)
//    }

    if (viewComponent != null) {
      view = Utils.createViewImpl4FastList(CustomControllerHelper.getContext(templateNode),
        cn.pureProps != null ? cn.pureProps : templateNode.getProps(), viewComponent);
      if (view == null) {
        view = CustomControllerHelper.createViewImpl(CustomControllerHelper.getContext(templateNode), viewComponent);
      }
      if (view == null) {
        Log.e(tag,"createView4ElementNode error view is null");
      }
      cn.onViewInit(view,cn);
      if (cn.boundView instanceof PostTaskHolder) {
        //Image和textView
        ((PostTaskHolder) cn.boundView).setRootPostHandlerView(rootList);
      }
      CustomControllerHelper.dealCustomProp(view, templateNode.getProps());
      //添加view
      if (addToParent && parent instanceof ViewGroup && !(parent instanceof FastPendingView)) {
        ((ViewGroup) parent).addView(view);
//        if (LogUtils.isDebug()) {
//          Log.e(TAG, "createView4ElementNode addViewOnCreateViewRecursive  :" + view + ",parent:" + parent);
//          Log.i(tag, "makeContentReady addViewOnCreateViewRecursive  :" + view + ",parent:" + parent+",this.:"+cn.templateNode+",parentNode:"+ ((ElementNode) cn.getParent()).templateNode);
//        }
      }else{
        if(LogUtils.isDebug() && parent == null && cn.getParent() instanceof ElementNode) {
          if(step == STEP_VIF && !cn.isRootItem){
            Log.e("CheckVIF", "createView4ElementNode addViewOnCreateViewRecursive return !   :" + view + ",parentView :" + parent + ",template:" + cn.templateNode + ",parentNode:" + ((ElementNode) cn.getParent()).templateNode+",root:"+cn.rootNode.hashCode());
          }
//          Log.e(TAG, "makeContentReady addViewOnCreateViewRecursive return !   :" + view + ",parentView :" + parent + ",template:" + cn.templateNode + ",parentNode:" + cn.getParent());
        }
      }
    }else{
      Log.e(tag, "createView4ElementNode error  viewComponent is null :" + ",node:" + cn.templateNode);
    }
    return view;
  }
  /**
   * 为ElementNode创建view
   * @param m
   * @param cn
   * @param rootList
   * @return
   */
  static View createView4ElementNode(ControllerManager m, ElementNode cn, FastListView rootList,View parent,int type){
    return createView4ElementNode(m,cn,rootList,parent,true,type);
  }

  static void makeContentReady(ControllerManager m, ViewTag tag, ElementNode cn, FastListView rootList,View parent,String pref) {
    if (cn != null) {
      if(LogUtils.isDebug()) {
        if ("".equals(pref)) {
          Log.i("MakeContentLog", pref + "-------------------------------------");
        }
        Log.i("MakeContentLog",pref+"makeContentReady node view :"+cn.boundView+",node:"+cn.templateNode);
      }

      if (cn.boundView == null) {
        cn.boundView = createView4ElementNode(m, cn, rootList,parent, STEP_DELAY_POST);
        if (rootList != null && rootList.getFastAdapter() != null) {
          rootList.getFastAdapter().onElementViewCreated(cn);
        }
      }
//      if (LogUtils.isDebug() && cn.getChildCount() > 0) {
//        Log.e(TAG, "TestMakeContent child count > 0  :" + cn.getChildCount());
//      }
      if(LogUtils.isDebug()) {
        pref += ">>>";
      }
      for (int i = 0; i < cn.getChildCount(); i++) {
        ElementNode child = (ElementNode) cn.getChildAt(i);
        makeContentReady(m, tag, child, rootList,cn.boundView,pref);
      }
    }
  }





  //zhaopeng add
  private HippyViewEvent mBindEvent, mUnbindEvent;

  private void sendAdapterEvent(int id, int position, RenderNode node, Holder holder, String eventName) {

    final HippyViewEvent event = new HippyViewEvent(eventName);

    HippyMap map = new HippyMap();
    map.pushInt("position", position);
    if (node != null) {
      map.pushObject("name", Utils.getNameFromProps(node.getProps()));
    }
    if (LogUtils.isDebug()) {
      Log.i("BindEvent", "+++++sendAdapterEvent position:" + position + ",eventName:" + eventName+", id :"+id);
    }
    //event.send(view, map);
    eventDeliverer.sendEvent(event, id, map);
//    if (LogUtils.isDebug()) {
//      for (String s : holder.tag.template.getProps().keySet()) {
//        Log.d(TAG, "BindEvent item :" + position + ",prop:" + s);
//      }
//    }
    if (holder.tag.template.getProps().containsKey(eventName)) {
      final View templateItemView = Utils.findBoundView(context, holder.tag.template);
//      Log.d(TAG, "BindEvent sendAdapterEvent : eventName:" + eventName + ",id:" + (templateItemView == null ? -1 : templateItemView.getId()));
      if (templateItemView != null && this.dataList.size() > position && position > -1) {
        eventAppendRawItem(position,map);
        //event.send(templateItemView, map);
        eventDeliverer.sendEvent(event, templateItemView, map);
      } else {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "BindEvent sendAdapterEvent error on templateView is null or dataList size 0, position:" + position + ",templateItemView:" + templateItemView);
        }
      }
    }
  }


  //	private HippyMap mBindEventMap,mUnbindEventMap;
  private void sendOnBindEvent(int id, int position, RenderNode node, Holder holder) {
    if (mBindEvent == null) {
      mBindEvent = new HippyViewEvent("onBindItem");
    }

    HippyMap map = new HippyMap();
    map.pushInt("position", position);
    if (node != null) {
      map.pushObject("name", Utils.getNameFromProps(node.getProps()));
    }
    if (LogUtils.isDebug()) {
      Log.i("BindEvent", "+++++sendOnBindEvent position:" + position+",id:"+id);
    }
    //mBindEvent.send(view, map);
    eventDeliverer.sendEvent(mBindEvent, id, map);
//    if (LogUtils.isDebug()) {
//      for (String s : holder.tag.template.getProps().keySet()) {
//        Log.d(TAG, "BindEvent item :" + position + ",prop:" + s);
//      }
//    }
    if (holder.tag.template.getProps().containsKey("onBindItem")) {
      final View templateItemView = Utils.findBoundView(context, holder.tag.template);
      if (LogUtils.isDebug()) {
        Log.d(TAG, "BindEvent send4TemplateView : templateItemView:" + templateItemView + ",id:" + (templateItemView == null ? -1 : templateItemView.getId()));
      }
      if (templateItemView != null && this.dataList.size() > position && position > -1) {
        eventAppendRawItem(position,map);
        //mBindEvent.send(templateItemView, map);
        eventDeliverer.sendEvent(mBindEvent, templateItemView, map);
      } else {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "BindEvent send4TemplateView error on templateView is null or dataList size 0, position:" + position + ",templateItemView:" + templateItemView);
        }
      }
    }
  }

  private void eventAppendRawItem(int position,HippyMap map){
    if(isEventSendItem) {
      if (LogUtils.isDebug()) {
        final Object item = getRawObject(position);
        Log.i(TAG, "BindEvent send4TemplateView sendUnBindEvent success position:" + position + ",itemData:" + item);
      }
      map.pushObject("item", getRawObject(position));
    }
  }

  private void sendUnBindEvent(int id, int position, Holder holder) {
    if (mUnbindEvent == null) {
      mUnbindEvent = new HippyViewEvent("onItemRecycled");
    }
    HippyMap map = new HippyMap();
    map.pushInt("position", position);
    if (LogUtils.isDebug()) {
      Log.e("BindEvent", "-----unBind position:" + position);
    }
    //mUnbindEvent.send(view, map);
    eventDeliverer.sendEvent(mUnbindEvent, id, map);
    if (holder.tag.template.getProps().containsKey("onItemRecycled")) {
      final View templateItemView = Utils.findBoundView(context, holder.tag.template);
      if (LogUtils.isDebug()) {
        Log.d(TAG, "BindEvent sendUnBindEvent : templateItemView:" + templateItemView + ",id:" + (templateItemView == null ? -1 : templateItemView.getId()));
      }
      if (templateItemView != null && this.dataList.size() > position && position > -1) {
        eventAppendRawItem(position,map);
        //mUnbindEvent.send(templateItemView, map);
        eventDeliverer.sendEvent(mUnbindEvent, templateItemView, map);
      } else {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "BindEvent send4TemplateView sendUnBindEvent error on position:" + position + ",dataSize:" + (this.dataList == null ? 0 : this.dataList.size()));
        }
      }
    }
  }

  interface ListNode {

    int getChildCount();

    RenderNode getNode();

    void setBoundTag(ListNodeTag tag);

    ListNodeTag getBoundTag();

    RenderNode getChildAt(int i);
  }

  static class ListNodeTag {

    public int position;
    public ListNode parent;
    public ElementNode listElementNode;
  }

  void recycleItem(Holder holder) {
    final View v = holder.itemView;
    if(v.getParent() != null){
      ((ViewGroup)v.getParent()).removeView(v);
    }
    if (LogUtils.isDebug()) {
      Log.i(TAG, "WorkLOG recycleItem holder:" + Utils.hashCode(holder));
    }
    holder.tag.cancelWork();
    getCacheWorker(holder.itemView.getContext(),getCacheRootView()).put(holder);
  }


  final static class CachePool extends RecyclerView.RecycledViewPool {
//    Map<Integer, CacheViewList> map = new HashMap<>();

    Holder get(int type) {
      final Holder h = (Holder) getRecycledView(type);
      if (h == null) {
        if(LogUtils.isDebug()) {
          Log.d(TAG, "！！！！DebugPool CachePool get return null,type:" + type + ",pool :" + Utils.hashCode(this) + "remain:" + getRecycledViewCount(type));
        }
      }
      return h;
    }

    public void setMaxCacheSize(int type, int max) {
      setMaxRecycledViews(type, max);
    }

    void put(Holder view) {

      if (view != null && LogUtils.isDebug()) {
        Log.d(TAG, "++++DebugPool CachePool put new ,type:" + view.type + ",pool :" + Utils.hashCode(this) + ",cacheCount::" + getRecycledViewCount(view.type));
      }
      putRecycledView(view);
    }
  }


  private final static class CacheViewList {
    final int type;
    CacheList list;


    private CacheViewList(int type) {
      this.type = type;
    }


    public void setMaxCacheSize(int max) {
      if (list == null) {
        list = new CacheList();
      }
      list.max = max;
    }

    void put(Holder view) {
      if (list == null) {
        list = new CacheList();
      }
      final boolean add = list.add(view);
      if (add) {
        if (LogUtils.isDebug()) {
          Log.d(TAG, "+++CacheViewList put size:" + list.size() + ",type:" + type);
        }
      } else {
        if (LogUtils.isDebug()) {
          Log.e(TAG, "+++CacheViewList put max items " + ",type:" + type);
        }
      }
    }

    Holder get() {
      if (list != null && list.size() > 0) {
        if (LogUtils.isDebug()) {
          Log.d(TAG, "---CacheViewList popup size:" + list.size() + ",type:" + type);
        }
        return list.get();
      }
      return null;
    }
  }

  public static class CacheList {
    int max = 20;
    List<Holder> list;

    boolean add(Holder view) {
      if (list == null) {
        list = new ArrayList<>();
      }
      if (size() < max) {


        final boolean b = list.add(view);
//        if(b){
//          if(view instanceof HippyRecycler){
//            ((HippyRecycler) view).onResetBeforeCache();
//          }
//        }
        return b;
      } else {
        return false;
      }
    }

    Holder get() {
      if (list != null && list.size() > 0) {
        return list.remove(0);
      }
      return null;
    }

    int size() {
      return list.size();
    }

  }

  private static class initConfig{
    public boolean enablePostDelay = false;
    public boolean disablePlaceholder = false;
    public HippyArray placeholderLayout = null;
    public float placeholderScale = -1;
    public boolean disablePlaceholderFocus = false;
    public String placeholderIcon = null;
    public HippyArray placeholderIconSize = null;
    private boolean needCheckVIF = false;
    private boolean isViewFocusable = false;
    private boolean hasNestList = false;
    private boolean createViewOnInit = false;
    private boolean enableDelayLoad = false;
    private boolean enableCoverRole = false;
  }

  /**
   * 主要功能相当于RenderNode及DomNode二合一。不等于每个position都拥有一个绑定的view，面是ItemView绑定一个element；
   * 例如当一个列表中可见三个item时，可能最多有5个ElementNode存在
   */
  public static class ElementNode extends StyleNode {
    final FastAdapter.initConfig initConfig;
    public View boundView;
    String name;
    RenderNode templateNode;
    HippyMap pureProps;
    ElementNode rootNode;
    Runnable layoutTask;
    ElementNode focusableNode;
    ViewTag.Worker pendingWorker;
    boolean markAsPlaceholder = false;
    //是否初始化过
    boolean hasInit = false;
    //是否创建过绑定的view
    boolean hasCreateView = false;
    boolean hasUpdateLayout = false;
    //是否请求layout,该请求来自于子节点
    boolean isLayoutDirty = false;
    //宽度自适应
    public boolean isWidthWrapContent = false;
    //高度自适应
    public boolean isHeightWrapContent = false;
    int adapterPosition = -1;
    HippyMap pendingProps;
    //是否是根的item
    boolean isRootItem = false;
    ArrayList<Runnable> mPostTasks;
    //延迟加载
    int loadDelay = 0;
    //延迟加载的view
    boolean isMarkToDelay = false;
    boolean recycled = false;
    //检查v-if是否生效
    boolean  isPlaceholderEnable =  false;

    public HippyMap placeholderStyle = null;
    public String itemSID = null;

    public RenderNode getTemplateNode() {
      return templateNode;
    }

    public ElementNode() {
      this.initConfig = new initConfig();
    }

    void resetState() {
//      this.hasInit = false;
      this.isLayoutDirty = true;
      //this.adapterPosition = -1;
    }

    static void updateItemLayoutNegative(DomNode node) {
      if (node instanceof ElementNode) {
        final ElementNode en = (ElementNode) node;
        if (!en.isRootItem) {
          final View view = en.boundView;
          if (view != null) {
            if (LogUtils.isDebug()) {
              Log.i("AutoMeasure", "updateItemLayoutNegative this:" + en);
            }
            FastAdapterUtil.updateLayout(view, en);
          }
        }
      }
      for (int i = 0; i < node.getChildCount(); i++) {
        updateItemLayoutNegative(node.getChildAt(i));
      }
    }

    void postTask(PostTask postTask){
      if (mPostTasks == null) {
        mPostTasks = new ArrayList();
      }
      mPostTasks.add(postTask);
      if (boundView != null) {
        if(LogUtils.isDebug()){
          Log.i(TAG_POST,"PostTask +++++++++add task t :"+postTask);
        }
        boundView.post(postTask);
      }
    }

    void clearPostTask(){
      if (mPostTasks != null) {
        if(boundView != null){
          for(Runnable t : mPostTasks) {
            if(LogUtils.isDebug()){
              Log.i(TAG_POST,"PostTask ---------remove task t :"+t);
            }
            boundView.removeCallbacks(t);
          }
        }
        mPostTasks.clear();
      }
      for(int i = 0; i < getChildCount(); i ++){
        Object child = getChildAt(i);
        if(child instanceof ElementNode){
          ((ElementNode) child).clearPostTask();
        }
      }
    }

    void requestItemLayout() {
      if (boundView != null) {
        if (layoutTask != null) {
          boundView.removeCallbacks(layoutTask);
        }
        layoutTask = () -> {
          calculateLayout();
          updateItemLayoutNegative(ElementNode.this);
        };
        boundView.postDelayed(layoutTask, 16);
      }
    }

    public void requestMeasure() {
      if (LogUtils.isDebug()) {
        Log.i("AutoMeasure", "~~~~~~~~~~~~requestMeasure this:" + this);
      }
      isLayoutDirty = true;
      if (isRootItem) {
        requestItemLayout();
      }
      if (getParent() instanceof ElementNode) {
        final ElementNode pNode = (ElementNode) getParent();
        if (pNode.isHeightWrapContent) {
          pNode.setStyleHeight(FlexConstants.UNDEFINED);
        }
        pNode.requestMeasure();
      }
    }


    public float getStylePadding(int spacingType) {
      return Style().getPadding(FlexNodeStyle.Edge.fromInt(spacingType)).value();
    }

    public float getStyleMargin(int spacingType) {
      return Style().getMargin(FlexNodeStyle.Edge.fromInt(spacingType)).value();
    }

    public float getStyleBorder(int spacingType) {
      return Style().getBorder(FlexNodeStyle.Edge.fromInt(spacingType)).value();
    }

    @Override
    public String toString() {
      return styleToString();
    }

    //    @Override
    public String styleToString() {
      return "\nSTYLE:\n" +
        "{left: " + getLayoutX() + ", " +
        "top: " + getLayoutY() + ", " +
        "width: " + getLayoutWidth() + ", " +
        "height: " + getLayoutHeight() + ", " +
        "styleWidth: " + getStyleWidth() + ", " +
        "styleHeight: " + getStyleHeight() + ", " +
        "className: " + getViewClass() + ", " +
        "this: " + (boundView != null ? boundView.hashCode() : null) + ", " +
        "}";
    }

    public void onNodeInitOnSingleton(RenderNode templateNode) {
        this.onConfigFix(templateNode,true);
    }

    void onConfigFix(RenderNode templateNode,boolean singleton){
      this.templateNode = templateNode;
//      Log.i("FocusableNode","++++onNodeInit false templateNode : "+templateNode);
      this.pureProps = new HippyMap();
      //如果数据包含${xx},将这种prop排除
//        if(getStyleWidth() < 1){
//          Log.e(TAG,"onNodeInit getStyleWidth < 1 this:"+templateNode);
//        }
//        if(getStyleHeight() < 1){
//          Log.e(TAG,"onNodeInit getStyleHeight < 1 this:"+templateNode);
//        }
      if(HippyImageViewController.CLASS_NAME.equals(templateNode.getClassName())){
        boolean isCoverRole = "cover".equals( templateNode.getProps().getString("roleType"));
        if(isCoverRole){
          this.initConfig.enableCoverRole = true;
          if (rootNode != null) {
            rootNode.initConfig.enableCoverRole = true;
          }
        }
      }

      for(String prop : templateNode.getProps().keySet()){
        final Object value = templateNode.getProps().get(prop);
        if (value != null) {
          if (!TemplateCodeParser.isPendingProForce(value)) {
            this.pureProps.pushObject(prop, value);
          } else {
            if (TemplateCodeParser.PENDING_PROP_CREATE_IF.equals(prop)) {
              this.initConfig.needCheckVIF = true;
            }
          }
        }
        switch (prop){
          case "autoWidth":
            isWidthWrapContent = true;
            break;
          case "autoHeight":
            isHeightWrapContent = true;
            break;
          case Utils.KEY_CONTENT_DELAY:
            isMarkToDelay = true;
            if (rootNode != null) {
              rootNode.initConfig.enableDelayLoad = true;
            }
            if(value instanceof Integer) {
              loadDelay = (int) value;
            }
            break;
          case Utils.KEY_PLACEHOLDER_VIEW:
            markAsPlaceholder = true;
            break;
          case "name":
            if(value instanceof String){
              name = (String) value;
            }
            break;
          case NodeProps.FOCUSABLE:
            if( FastAdapterUtil.parseBoolean(value)){
              this.initConfig.isViewFocusable = true;
              if (this.rootNode != null) {
                this.rootNode.focusableNode = this;
//                  Log.e("FocusableNode","++++focusableNode firm : "+templateNode+",rootNode:"+this.rootNode.templateNode);
              }
            }
            break;
//            case "eventFocus":
//            case "eventClick":
//              this.initConfig.isViewFocusable = true;
//              if (this.rootNode != null) {
//                this.rootNode.focusableNode = this;
//                Log.e("FocusableNode","focusableNode firm on eventClick: "+focusableNode.templateNode+",rootNode:"+this.rootNode.templateNode);
//              }
//              break;
          case "postDelay":
            this.initConfig.enablePostDelay = true;
            break;
          case "disablePlaceholder":
            initConfig.disablePlaceholder = true;
            if (rootNode != null) {
              rootNode.initConfig.disablePlaceholder = true;
            }
            break;
          case "placeholderLayout":
            if (rootNode != null && value instanceof HippyArray) {
              rootNode.initConfig.placeholderLayout = (HippyArray) value;
            }
            break;
          case "placeholderScale":
            if (rootNode != null && value != null) {
              //Log.i("placeholderScale","placeholderScale set on float "+value+",class:"+value.getClass());
              if(value instanceof Float) {
                rootNode.initConfig.placeholderScale = (Float) value;
              }
              if (value instanceof Double) {
                rootNode.initConfig.placeholderScale = ((Double) value).floatValue();
              }
              if (value instanceof Integer) {
                rootNode.initConfig.placeholderScale = ((Integer) value).floatValue();
              }
            }else{
              //Log.e("placeholderScale","placeholderScale value null");
            }
            break;
          case "disablePlaceholderFocus":
            initConfig.disablePlaceholderFocus = true;
            if (rootNode != null) {
              rootNode.initConfig.disablePlaceholderFocus = true;
            }
            break;
          case "placeholderIcon":
            if(value instanceof String) {
              initConfig.placeholderIcon = (String) value;
              if (rootNode != null) {
                rootNode.initConfig.placeholderIcon = (String) value;
              }
            }
            break;
          case "placeholderIconSize":
            if(value instanceof HippyArray) {
              initConfig.placeholderIconSize = (HippyArray) value;
              if (rootNode != null) {
                rootNode.initConfig.placeholderIconSize = (HippyArray) value;
              }
            }
            break;
        }

      }

    }

    public void onNodeInit(RenderNode templateNode) {
      this.onConfigFix(templateNode,false);

    }

    /**
     * 当item中每个view创建完成后，调用该方法
     * @param view
     */
    public void onViewInit(View view,ElementNode el) {
      if (view instanceof TVTextView) {
        ((TVTextView) view).bindNode(this);
        if (isHeightWrapContent) {
          setStyleHeight(FlexConstants.UNDEFINED);
          view.setLayoutParams(new ViewGroup.LayoutParams((int) getStyleWidth(), -2));
        }
      }
      if (view instanceof HippyImageView) {

      }
//      Log.v("ItemContainerLog","---onViewInit view : rootNode.isPlaceholderEnable"+rootNode.isPlaceholderEnable+",this == rootNode.focusableNode"+(this == rootNode.focusableNode)+",!initConfig.disablePlaceholderFocus:"+(!initConfig.disablePlaceholderFocus)+",view:"+view);
//      if (rootNode.isPlaceholderEnable && this == rootNode.focusableNode && !initConfig.disablePlaceholderFocus) {
//        Log.i("ItemContainerLog","onViewInit view : "+view);
//        FastAdapterUtil.setDuplicateParentStateEnabled4AllParent(view,true);
//      }
      if (view instanceof FastListView) {
        //Log.i("ZHAOPENG", "FastAdapter onViewInit  setTemplateNode :" + templateNode);
        ((FastListView) view).setTemplateNode(templateNode);
        ((FastListView) view).getFastAdapter().setEnablePlaceholder(CHILD_LIST_PLACEHOLDER_ENABLE);
      }
      if(view instanceof JSEventHandleView){
        ((JSEventHandleView) view).setJSEventViewID(templateNode.getId());
      }
      boundView = view;
      //设置Name,zhaopeng 这里如果不设置Name，会导致一些基于name而设置的api无效，比如firstSearchTarget等
//      ItemTag tagObj = new ItemTag();
//      tagObj.pushString(NodeProps.NAME, name);
//      view.setTag(tagObj);
      final ExtendTag extTag = new ClonedViewTag(templateNode);
      extTag.nodeClassName = templateNode.getClassName();
      extTag.name = name;
      ExtendTag.putTag(view,extTag);
    }

    void measureParentWidthIfNeed(ElementNode node, float width) {
      if (node.isWidthWrapContent) {
        final float padding = node.getStylePadding(0) + node.getStylePadding(2);
        node.setStyleWidth(width + padding);
      }
      if (node.getParent() instanceof ElementNode) {
        measureParentWidthIfNeed((ElementNode) node.getParent(), width);
      }
    }

//    public String styleToString() {
//      return  "\nSTYLE:\n" +
//        "flexDirection: {" +getFlexDirection()+", "+
//        "getJustifyContent: {" +getJustifyContent()+", "+
//        "getAlignItems: {" +getAlignItems()+", \n"+
//        "margin: " + getMargin(0) + ", " +getMargin(1) + ", "+ +getMargin(2) + ", "+ +getMargin(3) + ", "+
//        "padding: " + getPadding(0) + ", " +getPadding(1) + ", "+ +getPadding(2) + ", "+ +getPadding(3) + ", \n"+
//        "stylePadding: " + getStylePadding(0) + ", " +getStylePadding(1) + ", "+ +getStylePadding(2) + ", "+ +getStylePadding(3) + ", \n"+
//        "left: " + getLayoutX() + ", " +
//        "top: " + getLayoutY() + ", " +
//        "width: " + getLayoutWidth() + ", " +
//        "height: " + getLayoutHeight() + ", " +
//        "styleWidth: " + getStyleWidth() + ", " +
//        "styleHeight: " + getStyleHeight() + ", " +
//        "className: " + getViewClass() + ", " +
//        "this: " + Utils.hashCode(this) + ", " +
//        "}";
//    }

  }

  public void setOnFastItemClickListener(OnFastItemClickListener onFastItemClickListener) {
    this.onFastItemClickListener = onFastItemClickListener;
  }

  public void setOnFastItemFocusChangeListener(OnFastItemFocusChangeListener onFastItemFocusChangeListener) {
    this.onFastItemFocusChangeListener = onFastItemFocusChangeListener;
  }

  public interface ScrollTaskHandler {
    void clearPostTask(int type);

    void notifyDetachFromParent();

    void notifyAttachToParent();

    void notifyPauseTask();

    void notifyResumeTask();

    void notifyBringToFront(boolean front);

  }


  public static final class ItemEntity {
    int position;
    final Object raw;
//    boolean updateItemDirty = false;


    //    HippyMap rawPendingPropsReverse;
    ItemEntity(Object raw, int position) {
      this.raw = raw;
      this.position = position;
    }

//    @Override
//    public String toString() {
//      return "ItemEntity{" +
//        "position=" + position +
//        ", updateItemDirty=" + updateItemDirty +
//        '}';
//    }

    HippyMap getMap() {
      return raw instanceof HippyMap ? (HippyMap) raw : null;
    }

    Object getObject() {
      return raw;
    }
  }


//  public static final class ItemTag extends HippyMap{
//     private Object id;
//     public void setID(Object id){
//       if(LogUtils.isDebug()){
//         Log.v(TAG,"InnerTag setID:"+id);
//       }
//       this.id = id;
//     }
//
//    public Object getId() {
//      return id;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//      if (this == o) return true;
//      if(o instanceof String){
//        return o.equals(id);
//      }
//      if (o == null || getClass() != o.getClass()) return false;
//      ItemTag innerTag = (ItemTag) o;
//      return Objects.equals(id, innerTag.id);
//    }
//
//    @Override
//    public String toString() {
//      return "InnerTag{" +
//        "id=" + id +
//        '}'+"super:"+super.toString();
//    }
//
//    @Override
//    public int hashCode() {
//      return Objects.hash(id);
//    }
//  }
  static final class IconPlaceholderView extends ViewGroup {

    private final HippyImageView iconView;
    private int iconWidth = 50;
    private int iconHeight = 50;

  final int viewType;
  final Paint paint;
  final RectF rect;
  final @Nullable HippyArray drawRect;
  final float border;


    public IconPlaceholderView(Context context, String icon,int viewType, int color, float border, @Nullable HippyArray drawRect, @Nullable HippyArray iconSize){
    super(context);
      iconView = new HippyImageView(context);
      iconView.setDelayLoad(100);
    iconView.setUrl(icon);
    addView(iconView);
    if(iconSize != null && iconSize.size() == 2){
      iconWidth = iconSize.getInt(0);
      iconHeight = iconSize.getInt(1);
    }

      this.viewType = viewType;
      setFocusable(false);
      paint = new Paint();
      paint.setColor(color);
      if (border > 0) {
        paint.setAntiAlias(true);
      }
      this.drawRect = drawRect;
      rect = new RectF();
      this.border = Utils.toPX((int) border);

  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if(drawRect == null){
      rect.set(0, 0, w, h);
    }else {
      rect.set(drawRect.getInt(0),drawRect.getInt(1),
        drawRect.getInt(2),drawRect.getInt(3));
    }
    postInvalidateDelayed(16);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int cl = (int) (((r - l) - iconWidth) * 0.5);
    int ct = (int) (((b - t) - iconHeight) * 0.5);
    iconView.layout(cl, ct, cl + iconWidth, ct + iconHeight);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if(border > 0){
      canvas.drawRoundRect(rect,border,border,paint);
    }else{
      canvas.drawRect(rect,paint);
    }
    super.dispatchDraw(canvas);
  }

//  @Override
//  public void draw(Canvas canvas) {
//
//    super.draw(canvas);
//  }
}

  static final class PendingPlaceHolderView extends View{
    final int viewType;
    final Paint paint;
    final RectF rect;
    @Nullable HippyArray drawRect;
    final float border;
    public PendingPlaceHolderView(Context context, int viewType, int color, float border, @Nullable HippyArray drawRect) {
      super(context);
      this.viewType = viewType;
      setFocusable(false);
      paint = new Paint();
      paint.setColor(color);
      if (border > 0) {
        paint.setAntiAlias(true);
      }
      this.drawRect = drawRect;
      rect = new RectF();
      this.border = Utils.toPX((int) border);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);
      if(drawRect == null){
        rect.set(0, 0, w, h);
      }else {
        rect.set(drawRect.getInt(0),drawRect.getInt(1),
          drawRect.getInt(2),drawRect.getInt(3));
      }
      postInvalidate();
    }

    public void setDrawRect(@Nullable HippyArray drawRect){
      this.drawRect = drawRect;
      if(drawRect != null && drawRect.size() == 4){
        rect.set(drawRect.getInt(0),drawRect.getInt(1),
          drawRect.getInt(2),drawRect.getInt(3));
      }
      postInvalidate();
    }

    @Nullable
    public HippyArray getDrawRect() {
      return drawRect;
    }

    @Override
    public void draw(Canvas canvas) {
      super.draw(canvas);
      if(border > 0){
        canvas.drawRoundRect(rect,border,border,paint);
      }else{
        canvas.drawRect(rect,paint);
      }
    }
  }

  public interface ItemContainer {
    View getItemView();
    View getContentView();
    void toggle(boolean toPlaceholder);

    //void setContentView(View nodeView);
  }

  @SuppressLint("ViewConstructor")
  static final class ItemRootView extends HippyViewGroup implements View.OnClickListener, View.OnFocusChangeListener, ItemContainer, IImageStateListener<HippyImageView> {
    View placeholderView;
    View contentView;
    ElementNode coverNode;
    final int viewType;
    final boolean isSingleton;
    Runnable changeDisplayRunnable;
    final ElementNode itemNode;
    boolean isPlaceholderState = false;
    public ItemRootView(Context context,int viewType,View placeholderView,boolean isSingleton,ElementNode itemNode) {
      super(context);
      this.viewType = viewType;
      this.placeholderView = placeholderView;
      setFocusable(true);
      setClipChildren(false);
      addView(placeholderView);
      setOnClickListener(this);
      setOnFocusChangeListener(this);
//      setBackgroundColor(Color.GREEN);
      this.isSingleton = isSingleton;
      this.itemNode = itemNode;
    }

    public void registerCoverView(ElementNode coverNode){
      this.coverNode = coverNode;
      if(getCoverView() != null) {
        getCoverView().setCustomImageRequestListener(this);
      }
    }

    public void afterPostBind(){

    }

    public void afterPreBind(){
      if(itemNode.itemSID != null){
//        Log.e("configID4Item","configID4Item itemSID "+itemNode.itemSID +",contentView:"+ExtendUtil.debugViewLite(contentView));
        ExtendUtil.putViewSID(this,itemNode.itemSID);
      }
      if (itemNode.placeholderStyle != null && placeholderView instanceof PendingPlaceHolderView) {
        int left = PixelUtil.dp2pxInt(itemNode.placeholderStyle.getInt("left"));
        int top = PixelUtil.dp2pxInt(itemNode.placeholderStyle.getInt("top"));
        int right = PixelUtil.dp2pxInt(itemNode.placeholderStyle.getInt("width")) +  left;
        int bottom = PixelUtil.dp2pxInt(itemNode.placeholderStyle.getInt("height")) + top;

        HippyArray drawArray = ((PendingPlaceHolderView) placeholderView).getDrawRect();
        boolean changed = false;
        if (drawArray == null) {
          drawArray = new HippyArray();
          changed = true;
        }
        if(!changed && drawArray.size() == 4) {
          changed = left != drawArray.getInt(0) || top != drawArray.getInt(1);
          if(!changed) {
            changed = right != drawArray.getInt(2) || bottom != drawArray.getInt(3);
          }
        }
        if(changed) {
          drawArray.clear();
          drawArray.pushInt(left);
          drawArray.pushInt(top);
          drawArray.pushInt(right);
          drawArray.pushInt(bottom);
          ((PendingPlaceHolderView) placeholderView).setDrawRect(drawArray);
        }
      }
    }

    public HippyImageView getCoverView(){
      return  coverNode != null &&  coverNode.boundView instanceof HippyImageView ? (HippyImageView) coverNode.boundView : null;
    }

    @Override
    public HippyMap getScrollOverride() {
      if(getContentView() instanceof HippyViewGroup){
        return ((HippyViewGroup) getContentView()).getScrollOverride();
      }
      return super.getScrollOverride();
    }

    public void setContentView(View contentView, boolean forceDetach) {
      if(forceDetach && contentView != null){
        FastAdapterUtil.removeFromParentIfNeed(contentView);
      }
      if(contentView == null || contentView.getParent() == this){
        return;
      }
      if (this.contentView != null) {
        removeView(this.contentView);
      }
      this.contentView = contentView;
      if (isSingleton && contentView instanceof FastItemView) {

      }
      contentView.setDuplicateParentStateEnabled(true);
      this.addView(contentView);
    }

    @Override
    protected void onDetachedFromWindow() {
      super.onDetachedFromWindow();
      if(this.changeDisplayRunnable != null) {
        this.removeCallbacks(this.changeDisplayRunnable);
        this.changeDisplayRunnable = null;
      }
    }

    @Override
    public View getItemView() {
      return this;
    }

    public View getContentView() {
      return contentView;
    }

    @Override
    public void toggle(boolean toPlaceholder){
      if(isSingleton){
        //单例暂不处理
        return;
      }
      this.isPlaceholderState = toPlaceholder;
      if(toPlaceholder){
        if (this.contentView != null) {
          if (placeholderView != null) {
            placeholderView.setAlpha(1);
          }
          this.contentView.setAlpha(0);
        }
      }else{
        if (itemNode.initConfig.enableCoverRole) {
          if (contentView != null) {
            contentView.setAlpha(1);
          }
          if(getCoverView() != null){
//            Log.e("ZHAOPENG","ItemRootViewLog toggle on coverView loadState:"
//              +getCoverView().getFetchState()+",url:"+getCoverView().getUrl());
            if(getCoverView().getFetchState() == 2){
              if (placeholderView != null) {
                placeholderView.setAlpha(0);
              }
            }else if(getCoverView().getFetchState() == 1){
              if (placeholderView != null) {
                placeholderView.setAlpha(1);
              }
            }
          }else{
            if (placeholderView != null) {
              placeholderView.setAlpha(1);
            }
          }
        }else {
          if (placeholderView != null) {
            if (this.contentView != null) {
              this.contentView.setAlpha(1);
            }
            placeholderView.setAlpha(0);
          }
        }
      }
      if(isFocused()) {
        refreshDrawableState();
      }
//      }
      if(this.changeDisplayRunnable != null) {
        this.removeCallbacks(this.changeDisplayRunnable);
      }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
      super.onLayout(changed,left,top,right,bottom);
      if (placeholderView != null) {
        placeholderView.layout(0, 0, right - left, bottom - top);
      }
      if(contentView != null) {
        contentView.layout(0, 0, right - left, bottom - top);
      }
    }

    private OnClickListener mContentClickListener;
    private ItemFocusListener mContentFocusListener;
    public void setContentOnClickListener(OnClickListener listener) {
      this.mContentClickListener = listener;
    }

    public void setContentItemFocusListener(ItemFocusListener listener) {
      this.mContentFocusListener = listener;
    }

    View getFocusableView(){
      if (itemNode.focusableNode != null) {
        return itemNode.focusableNode.boundView;
      }
      return null;
    }

    @Override
    public void onClick(View v) {
      View focusableView = getFocusableView();
      if (LogUtils.isDebug()) {
        Log.i("DebugClick","ItemRootView onClick contentView:"+contentView+",focusableView:"+focusableView);
      }
      if (mContentClickListener != null && focusableView != null) {
        mContentClickListener.onClick(focusableView);
      }
    }


    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      View focusableView = getFocusableView();
      if (LogUtils.isDebug()) {
        Log.i("ItemRootViewLog","ItemRootView onFocusChange contentView:"+contentView+",focusableView:"+focusableView);
      }
      if (mContentFocusListener != null && focusableView != null) {
        mContentFocusListener.onFocusChange(focusableView,hasFocus);
      }
    }

    @Override
    public void onRequestStart(HippyImageView drawableTarget) {
//      if(placeholderView != null) {
//        placeholderView.setAlpha(1);
//      }
//      if (contentView != null) {
//        contentView.setAlpha(0);
    }

    @Override
    public void onImageLayout(HippyImageView drawableTarget, int left, int top, int right, int bottom) {
    }


    @Override
    public void onRequestSuccess(HippyImageView drawableTarget) {
      if(isPlaceholderState){
        return;
      }
      if(placeholderView != null) {
        placeholderView.setAlpha(0);
      }
      if (contentView != null) {
        contentView.setAlpha(1);
      }
    }

    @Override
    public void onRequestFail(Throwable cause, String source) {
      if(isPlaceholderState){
        return;
      }
      if(placeholderView != null) {
        placeholderView.setAlpha(0);
      }
      if (contentView != null) {
        contentView.setAlpha(1);
      }
    }
  }

  public static final class ErrorPlaceHolderView extends View{

    final int viewType;
    public ErrorPlaceHolderView(Context context,int viewType) {
      super(context);
      this.viewType = viewType;
      setBackgroundColor(Color.TRANSPARENT);
      setFocusable(false);
    }
  }

  public static class Payload{
    final PayloadType type;
    final HippyMap params;
    public Payload(PayloadType type,@Nullable HippyMap params) {
      this.type = type;
      this.params = params;
    }
  }

  public enum PayloadType {
    TYPE_UPDATE,
    TYPE_LAYOUT,
    TYPE_LAYOUT_ROOT,
  }




}
