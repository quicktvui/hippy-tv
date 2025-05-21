package com.quicktvui.hippyext.views.fastlist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LayoutAnimationController;

import com.quicktvui.base.ui.AnimationStore;
import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.base.ui.FocusUtils;
import com.quicktvui.base.ui.ITVView;
import com.quicktvui.hippyext.RenderUtil;
import com.quicktvui.base.ui.StateListPresenter;
import com.quicktvui.base.ui.TVViewUtil;
import com.quicktvui.base.ui.TriggerTaskHost;
import com.quicktvui.hippyext.TriggerTaskManagerModule;
import com.quicktvui.base.ui.anim.FastOutLinearInInterpolator;
import com.quicktvui.base.ui.anim.LinearOutSlowInInterpolator;
import com.quicktvui.base.ui.ExtendTag;
import com.quicktvui.hippyext.views.FocusSearchHelper;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.list.NegativeLongKeyFlinger;
import com.tencent.mtt.hippy.views.list.RecycleViewFlinger;
import com.tencent.mtt.hippy.views.list.TVSingleLineListView;
import com.tencent.mtt.hippy.views.scroll.HippyScrollViewEventHelper;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.hippy.views.view.HippyViewGroupController;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class TVListView extends MouseRecycleView implements TriggerTaskHost, RecycleViewFlinger.IRecyclerView, ExtendViewGroup, TVSingleLineListView, PostHandlerView, ChildOnScreenScroller.IRecyclerView {
  static final String TAG = "DebugFastList";
  private static final boolean DEBUG = LogUtils.isDebug();
  int mScrollOffset;
  private ChildOnScreenScroller mCustomChildOnScreenScroller;
  private int[] mBlockFocus;
  protected boolean useAdvancedFocusSearch = true;
  protected int mFocusChildPosition = -1;
  private View[] mFocusSearchTargets;
  private boolean mListenFocusSearchOnFail;
  private int[] mBlockFocusOnFail;
  private boolean mShakeEndEnable = true;
  protected int shakePreCheckNumber = 2;
  protected int preloadItemNumber = 0;
  protected int defaultSectionPosition = -1;
  protected boolean enableFocusMemory = true;
  protected int mTargetFocusChildPosition = -1;
  private boolean enableSelectOnFocus = true;
  private int negativeKeyTime = -1;
  private boolean isDetached = false;
  private boolean isAttached = false;
  private boolean skipFocusOnPause = false;
  private boolean enableGridLoading = false;
  private int loadDelayTime = 300;
  //寻焦失败后，强制锁住焦点
  private boolean forceBlockFocusOnFail = false;


  @Deprecated
  private int initFocusPositionAfterLayout = -1;
  private FocusDispatchView rootView;
  protected EventDeliverer eventDeliverer;
  protected OnScrollListener myOnScrollListener;
  protected OnScrollListener mScrollToTopListener;
  protected OnLoadMoreListener mLoadMoreListener;

  protected OnLayoutListener mOnLayoutListener;

  private boolean postTaskPaused = false;
  protected int mManagerType = 1;//1默认LinearLayoutManager 2GridLayoutManager
  private final HippyEngineContext hippyEngineContext;
  protected boolean listShakeSelf;//焦点抖动开关

  private ListViewControlProp listViewControlProp;
  //列表滚动是否开启
  private boolean touchScrollEnabled = true;

  private Method scrollByMethod;
  private Object viewFlinger;
  final FocusSearchHelper mFocusSearchHelper;



  @Override
  public ListViewControlProp getControlProps() {
    if (listViewControlProp == null) {
      listViewControlProp = new ListViewControlProp();
    }
    return listViewControlProp;
  }

  @Override
  public void diffSetScrollToPosition(int position, int offset) {

  }

  private boolean skipRequestFocus = false;

  @Override
  public void setSkipRequestFocus(boolean b) {
    this.skipRequestFocus = b;
  }

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    if(skipRequestFocus){
      Log.e(AutoFocusManager.TAG,"requestFocus return false on skipRequestFocus this:"+ExtendUtil.debugViewLite(this));
      return false;
    }
    boolean b = false;
    View view = getFirstFocusHelper().findFirstFocusChildByDirection(direction);
    if (view != null) {
      b =  view.requestFocus();
      if(LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "!!!!!! tvList requestFocus by firstFocusHelper view " + ExtendUtil.debugViewLite(view) + ",this:" + ExtendUtil.debugViewLite(this));
      }
    }
    if(!b) {
      b = super.requestFocus(direction, previouslyFocusedRect);
      if(LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "!!!!!! tvList requestFocus return " + b + ",this:" + ExtendUtil.debugViewLite(this));
      }
    }
    return b;
  }


  public TVListView(@NonNull Context context) {
    super(context);
    this.eventDeliverer = new EventDeliverer(Utils.getHippyContext(context));
    hippyEngineContext = ((HippyInstanceContext) context).getEngineContext();
    if (hippyEngineContext != null) {
      listShakeSelf = hippyEngineContext.getGlobalConfigs().getEsBaseConfigManager().IsListShakeSelf();
      scrollFactor = FastListModule.getGlobalConfig(hippyEngineContext).scrollFactor;
    }

    try {
      Field viewFlingerField =  RecyclerView.class.getDeclaredField( "mViewFlinger");
      viewFlingerField.setAccessible(true);
      viewFlinger = viewFlingerField.get(this);
      if (viewFlinger != null) {
        Class viewFlingerClass = viewFlinger.getClass();
        Method method = viewFlingerClass.getDeclaredMethod("smoothScrollBy", int.class, int.class,int.class, Interpolator.class);
        method.setAccessible(true);
        scrollByMethod = method;
      }

    } catch (Exception e) {
      Log.e(TAG,"scrollByMethod error msg:"+e.getMessage()+",override scroll failed!");
      e.printStackTrace();
    }
    mFocusSearchHelper = new FocusSearchHelper(this);
  }

  public void setForceBlockFocusOnFail(boolean forceBlockFocusOnFail) {
    this.forceBlockFocusOnFail = forceBlockFocusOnFail;
  }

  @Override
  public void setAdapter(@Nullable Adapter adapter) {
    super.setAdapter(adapter);
    preventSearch = false;
    requestLayoutManual();
  }

  public void setOnLayoutListener(OnLayoutListener listener) {
    this.mOnLayoutListener = listener;
  }

  /**
   * @param skipFocusOnPause
   */
  public void setSkipFocusOnPause(boolean skipFocusOnPause) {
    this.skipFocusOnPause = skipFocusOnPause;
  }

  public void setOnLoadMoreListener(OnLoadMoreListener listener) {
    this.mLoadMoreListener = listener;
  }

  public interface OnLoadMoreListener {
    void onLoadMore(int checkPos, int itemCount);
  }

  public interface OnLayoutListener {
    void onBeforeLayout(TVListView recyclerView, RecyclerView.State state);

    void onLayoutComplete(TVListView recyclerView, RecyclerView.State state);
  }

  @Override
  public void setOnScrollListener(@Nullable OnScrollListener listener) {
    super.setOnScrollListener(listener);
    this.myOnScrollListener = listener;
  }

  public void setScrollToTopListener(@Nullable OnScrollListener listener) {
    this.mScrollToTopListener = listener;
  }

  public EventDeliverer getEventDeliverer() {
    return eventDeliverer;
  }

  public void pausePostTask() {
    if (LogUtils.isDebug()) {
      Log.i(TAG, "HippyImage pausePostTask called this:" + Utils.hashCode(this) + ",id:" + getId());
      if (mTaskHub != null) {
        for (int i = 0; i < mTaskHub.size(); i++) {
          final SparseArray<Task> tasks = mTaskHub.valueAt(i);
          if (LogUtils.isDebug()) {
            Log.i(TAG, "HippyImage pausePostTask tasks cate " + mTaskHub.keyAt(i) + ",size:" + (tasks == null ? 0 : tasks.size()));
          }
        }
      }
    }
    this.postTaskPaused = true;
  }


  public void resumePostTask() {
    if (LogUtils.isDebug()) {
      Log.i(TAG, "HippyImage resumePostTask called this:" + Utils.hashCode(this) + ",id:" + getId());
      if (mTaskHub != null) {
        for (int i = 0; i < mTaskHub.size(); i++) {
          final SparseArray<Task> tasks = mTaskHub.valueAt(i);
          Log.i(TAG, "HippyImage resumePostTask tasks cate " + mTaskHub.keyAt(i) + ",size:" + (tasks == null ? 0 : tasks.size()));
        }
      }
    }
    this.postTaskPaused = false;
    batchTask();
  }

  public void setNegativeKeyTime(int time) {
    if (mFlinger != null) {
      mFlinger.setKeyReleaseTimeInterval(time);
    } else {
      this.negativeKeyTime = time;
    }
  }

  public void init(int orientation, FastAdapter adapter) {
    setLayoutManager(createLinearLayoutManager(orientation, adapter));
  }

  protected RecyclerView.LayoutManager createLinearLayoutManager(int orientation, FastAdapter adapter) {
    return new MyLinearLayoutManager(this, orientation, adapter);
  }

  public void setGridViewLayout(int spanCount, int orientation, FastAdapter adapter) {
    setLayoutManager(createGridLayoutManager(this, spanCount, orientation, adapter));
  }

  public void setSpanCount(int spanCount) {
    if (getLayoutManagerCompat().getRealLayout() instanceof MyGridLayoutManager) {
      ((MyGridLayoutManager) getLayoutManagerCompat().getRealLayout()).setSpanCount(spanCount);
      requestLayoutManual();
    }
  }

  protected GridLayoutManager createGridLayoutManager(TVListView tvListView, int spanCount, int orientation, FastAdapter adapter) {
    MyGridLayoutManager gridLayoutManager = new MyGridLayoutManager(tvListView, this.getContext(), spanCount, orientation, false, adapter);
    gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override
      public int getSpanSize(int position) {
        if (adapter != null) {
          final FastAdapter.ItemEntity ie = adapter.getItemEntity(position);
          if (ie != null && ie.getMap() != null) {
            //zhaopeng add 使用span字段，可使用户自定义
            int span = ie.getMap().getInt("span");
            if (span > 0) {
              if (LogUtils.isDebug()) {
                Log.d(TAG, "getSpanSize return custom span :" + span + ",pos:" + position);
              }
              return span;
            }
          }
          int type = adapter.getItemViewType(position);
          if (type == FastAdapter.TYPE_HEADER || type == FastAdapter.TYPE_FOOTER ||
            type == FastAdapter.TYPE_ONE_LINE || type == FastAdapter.TYPE_EMPTY) {
            return gridLayoutManager.getSpanCount();
          } else if (type == FastAdapter.TYPE_SPAN_COUNT_TWO) {
            return 2;
          } else if (type == FastAdapter.TYPE_SPAN_COUNT_THREE) {
            return 3;
          } else if (type == FastAdapter.TYPE_SPAN_COUNT_FOUR) {
            return 4;
          } else if (type == FastAdapter.TYPE_SPAN_COUNT_FIVE) {
            return 5;
          } else if (type == FastAdapter.TYPE_SPAN_COUNT_SIX) {
            return 6;
          } else {
            return 1;
          }
        } else {
          return 1;
        }
      }
    });
    return gridLayoutManager;
  }

  public int getOrientation() {
    return getLayoutManagerCompat().getOrientation();
  }

  @Deprecated
  public void setInitFocusPositionAfterLayout(int position) {
    this.initFocusPositionAfterLayout = position;
  }

  //zhaopeng add
  private boolean mEnableChildFocusEvent = false;


  private HippyViewEvent mChildFocusEvent;

  public void setDispatchChildFocusEvent(boolean enable) {
    //
    this.mEnableChildFocusEvent = enable;
  }

  public void setUseAdvancedFocusSearch(boolean useAdvancedFocusSearch) {
    this.useAdvancedFocusSearch = useAdvancedFocusSearch;
  }

  public void setBlockFocusOnFail(int[] directions) {
    this.mBlockFocusOnFail = directions;
  }

  public void setBlockFocusOn(int[] directions) {
    this.mBlockFocus = directions;
  }

  public void setShakeEndEnable(boolean shakeEndEnable) {
    this.mShakeEndEnable = shakeEndEnable;
  }

  public void setShakePreCheckNumber(int shakePreCheckNumber) {
    this.shakePreCheckNumber = shakePreCheckNumber;
  }

  public void setPreloadItemNumber(int preLoadItemNumber) {
    this.preloadItemNumber = preLoadItemNumber;
  }

  public void setListenFocusSearchOnFail(boolean mListenFocusSearchOnFail) {
    this.mListenFocusSearchOnFail = mListenFocusSearchOnFail;
  }

  public void setGridLoading(boolean isLoading) {
    enableGridLoading = isLoading;
  }

  public void setLoadDelayTime(int time) {
    loadDelayTime = time;
  }

  @Override
  public void requestChildFocus(View child, View focused) {
    final View mSelectedChild = findSelectedChild();
    final View lastFocus = mSelectedChild;
    if (mSelectedChild != null) {
      callItemStateChangeIfNeed(lastFocus, StateListPresenter.STATE_NORMAL);
    }
    try {
      super.requestChildFocus(child, focused);
      if (mEnableChildFocusEvent) {
        if (mChildFocusEvent == null) {
          mChildFocusEvent = new HippyViewEvent(InternalExtendViewUtil.CHILD_FOCUS_EVENT_NAME);
        }
        InternalExtendViewUtil.sendEventOnRequestChildFocus(this, child, focused, mChildFocusEvent);
      }
    } catch (Throwable t) {
      Log.e(TAG, "requestChildFocus error :" + t.getMessage() + " focused:" + focused);
      t.printStackTrace();
    }
    callItemStateChangeIfNeed(child, StateListPresenter.STATE_FOCUSED);
    final int focusedChildPosition = getChildAdapterPosition(child);



    changeSelectState(defaultSectionPosition, false);
    if (enableSelectOnFocus) {
      defaultSectionPosition = focusedChildPosition;
    } else {
      changeSelectState(defaultSectionPosition, true);
    }
    mTargetFocusChildPosition = focusedChildPosition;
    if(mFocusChildPosition != focusedChildPosition){
      postInvalidateDelayed(16);
    }
    mFocusChildPosition = focusedChildPosition;
  }

  void requestFocusSearchInChild(View[] targets, View focused, int direction) {
    if (LogUtils.isDebug()) {
      Log.d(FocusDispatchView.TAG, "+++requestFocusSearchInChild");
    }
    mFocusSearchTargets = targets;
  }

  public void enableFocusMemory(boolean enableFocusMemory) {
    this.enableFocusMemory = enableFocusMemory;
  }

  public void setEnableSelectOnFocus(boolean enableSelectOnFocus) {
    this.enableSelectOnFocus = enableSelectOnFocus;
  }

  /*** add by zhaopeng ***/
  private boolean mBringToFrontOnFocus = true;

  public void setBringToFrontOnFocus(boolean mBringToFrontOnFocus) {
    this.mBringToFrontOnFocus = mBringToFrontOnFocus;
    postInvalidateDelayed(16);
  }


  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (mBringToFrontOnFocus && getFocusedChild() != null)
      super.drawChild(canvas, getFocusedChild(), getDrawingTime());
  }

  @Override
  public boolean drawChild(Canvas canvas, View child, long drawingTime) {
    if (mBringToFrontOnFocus && child == getFocusedChild())
      return true;
    return super.drawChild(canvas, child, drawingTime);
  }

  void removeSearchInChildRequest() {
    if (LogUtils.isDebug()) {
      Log.d(FocusDispatchView.TAG, "---removeSearchInChildRequest");
    }
    this.mFocusSearchTargets = null;
  }

  View searchInTargets(View[] targets, View focused, int direction) {
    if (LogUtils.isDebug()) {
      Log.d(FocusDispatchView.TAG, "searchInTargets targets size : " + (targets == null ? 0 : targets.length));
    }
    requestFocusSearchInChild(targets, focused, direction);
    View result = null;
    try {
      result = FocusFinder.getInstance().findNextFocus(this, focused, direction);
    } catch (Throwable t) {
      result = null;
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "searchInTargets return null on error:" + t.getMessage());
      }
    }
    removeSearchInChildRequest();
    return result;
  }

  View searchInTargetsOnRootView(View focused, int direction) {

    final View root = HippyViewGroup.findPageRootView(this);
    if(LogUtils.isDebug()){
      Log.i(FocusDispatchView.TAG,"searchInTargetsOnRootView root :"+ExtendUtil.debugView(root));
    }
    if(root instanceof HippyViewGroup){
      return ((HippyViewGroup) root).findNextSpecialFocusView(focused,direction);
    }
    return null;
  }

  @Override
  public View focusSearch(int direction) {

    preventSearch = false;
    View view =  super.focusSearch(direction);
    if (LogUtils.isDebug()) {
      LogUtils.e(FocusDispatchView.TAG, "!!!!!focusSearch in RecyclerView direction " + direction + ",result:" + ExtendUtil.debugViewLite(view)+",this:"+ExtendUtil.debugViewLite(this));
    }
    return view;
  }



  @Override
  public View focusSearch(View focused, int direction) {
    if (LogUtils.isDebug()) {
      LogUtils.e(FocusDispatchView.TAG, "focusSearch with focused before in RecyclerView direction " + direction + ",this:" + ExtendUtil.debugViewLite(this));
    }
    preventSearch = false;
//    int vector = getMyLayoutManager().getVectorByDirection(direction);
//    final int focusPosition = mFocusChildPosition;
//    int targetPosition = focusPosition + vector;
//    if (targetPosition < 0) {
//      Log.e(FocusDispatchView.TAG,"onFocusSearchFailed return on targetPosition < 0");
//      return null;
//    }
    View v = null;
    try {
      v = super.focusSearch(focused, direction);
      if(InternalExtendViewUtil.isContainBlockDirection(direction,mBlockFocus) || forceBlockFocusOnFail){
        boolean blockedOnFail = v != null && ExtendTag.obtainExtendTag(v).getBooleanExtraValue("blockedOnFail");
        boolean testFocusable = InternalExtendViewUtil.testFocusable(v);
        if(v== null ||  (blockedOnFail &&  !testFocusable)){
          Log.e(FocusDispatchView.TAG,"focusSearch blockedOnFail testFocusable false v:"+ExtendUtil.debugViewLite(v));
          return focused;
        }else{
          Log.e(FocusDispatchView.TAG,"focusSearch return super, v:"+ExtendUtil.debugViewLite(v)+",testFocusable:"+testFocusable+",blockedOnFail:"+blockedOnFail);
        }
      }

    }catch (IllegalArgumentException e){
      Log.e(FocusDispatchView.TAG,"focusSearch error focused "+ExtendUtil.debugViewLite(focused)
        +",this:"+ExtendUtil.debugViewLite(this));
      e.printStackTrace();
    }
    if (LogUtils.isDebug()) {
      LogUtils.e(FocusDispatchView.TAG, "focusSearch after with focused,in RecyclerView direction " + direction + ", result = " + v + ",this:" + ExtendUtil.debugViewLite(this));
    }

    /**
     * 修改触底动画为抖动动画
     */
    if ((focused == v || v == null)) {
      if (mManagerType == 1) {
        getLayoutManagerCompat().getExecutor().shakeEndIfNeed(focused, v, direction);
      }
    }
    if ((focused == v || v == null) && listShakeSelf) {
      if (mManagerType == 1) {
        shakeSelf(focused, direction);
      }
    }
    if(v== null){
      Log.e(FocusDispatchView.TAG,"！！TVList focusSearch return null on this:"+ExtendUtil.debugViewLite(this));
    }

    return v;
  }

  private boolean animRunning = false;

  private void shakeSelf(View view, int direction) {
    if (!animRunning) {
      animRunning = true;
      if (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP) {
        ObjectAnimator shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y.getName(), 0, 5f);//抖动幅度0到5
        shakeSelfAnim.setDuration(250);//持续时间
        shakeSelfAnim.setInterpolator(new CycleInterpolator(2));//抖动次数
        shakeSelfAnim.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationCancel(Animator animation) {
            animRunning = false;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            animRunning = false;
          }
        });
        shakeSelfAnim.start();//开始动画
      } else {
        ObjectAnimator shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X.getName(), 0, 5f);//抖动幅度0到5
        shakeSelfAnim.setDuration(250);//持续时间
        shakeSelfAnim.setInterpolator(new CycleInterpolator(2));//抖动次数
        shakeSelfAnim.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationCancel(Animator animation) {
            animRunning = false;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            animRunning = false;
          }
        });
        shakeSelfAnim.start();//开始动画
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (LogUtils.isDebug()) {
      Log.d(FastListView.TAG, "FastRecyclerView onLayout r:" + r + ",b:" + b + ",id:" + getId());
    }
    super.onLayout(changed, l, t, r, b);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
//    Log.e(FastListView.TAG, "FastRecyclerView onSizeChanged w:" + w + ",h:" + h);
  }

  public LayoutManagerCompat getLayoutManagerCompat() {
    return (LayoutManagerCompat) getLayoutManager();
  }

  @Deprecated
  public TVListView.MyLinearLayoutManager getMyLayoutManager() {
    return (TVListView.MyLinearLayoutManager) this.getLayoutManager();
  }

  @Deprecated
  public MyGridLayoutManager getMyGridLayoutManager() {
    return (MyGridLayoutManager) getLayoutManager();
  }

  protected boolean onInterceptRequestChildRectangleOnScreen(@NonNull View child, @NonNull Rect rect, boolean immediate, int direction, boolean focusedChildVisible) {
    return false;
  }

  View findTargetChildFocus(int position, String name) {
    View targetChild = null;
    targetChild = getLayoutManagerCompat().findViewByPosition(position);
    View result;
    if (name == null) {
      result = targetChild;
    } else {
      result = ControllerManager.findViewByName(this, name);
    }
    return result;
  }

  public View findSelectedChild() {
    final int select = defaultSectionPosition;
    if (mManagerType == 1 && select > -1) {
      return getLayoutManagerCompat().findViewByPosition(select);
    } else if (mManagerType == 2 && select > -1) {
      return getLayoutManagerCompat().findViewByPosition(select);
    } else {
      return null;
    }
  }

  protected boolean isSelectedChildValid(View child) {
    boolean b = child != null;
    if (b) {
      b &= child.getVisibility() == View.VISIBLE;
      if (DEBUG) {
        Log.d(TAG, "isSelectedChildValid Visibility():" + b);
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        b &= child.isAttachedToWindow();
        if (DEBUG) {
          Log.d(TAG, "isSelectedChildValid isAttachedToWindow:" + b);
        }
      }
      b &= TVViewUtil.isViewDescendantOf(child, this);
      if (DEBUG) {
        Log.d(TAG, "isSelectedChildValid isViewDescendantOf:" + b);
      }
      final int visibility = child.getWindowVisibility();
      if (DEBUG) {
        Log.d(TAG, "isSelectedChildValid child visibility:" + visibility);
      }
    }
    return b;
  }

  protected void onAddDefaultItemDecoration() {
    addItemDecoration(new ItemDecorations.ListEndBlank(getLayoutManagerCompat().getOrientation()));
  }

  public void setTargetFocusChildPosition(int mTargetFocusChildPosition) {
    this.mTargetFocusChildPosition = mTargetFocusChildPosition;
  }


  final static class MyLinearScroller extends LinearSmoothScroller {
    private TVListView recyclerView;
    int offset;
    private View targetTemp;
    private static final String TAG = "TVListScroller";

    public MyLinearScroller(TVListView recyclerView, int offset) {
      super(recyclerView.getContext());
      this.recyclerView = recyclerView;
      this.offset = offset;
    }

    @Override
    protected void onTargetFound(View targetView, State state, Action action) {
      if (LogUtils.isDebug()) {
        Log.d(TAG, "TestScroll onTargetFound getRemainingScrollVertical:" + state.getRemainingScrollVertical());
      }
      targetTemp = targetView;
      super.onTargetFound(targetView, state, action);
    }

    @Override
    protected void onStart() {
      super.onStart();
//      Log.d(TAG, "TestScroll MyLinearScroller onStart offset:" + offset);
      recyclerView.requestLayoutManual();
    }

    @Override
    protected void onStop() {
      super.onStop();
//      Log.d(TAG, "TestScroll MyLinearScroller onStop");
    }

    @Override
    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
//      int type = recyclerView.getChildOnScreenScroller().getScrollType();
//      int result =  0;
//      if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER) {
//        int offsetOnCenterType = 0;
//        //由于只有smooth滚动中才能实现居中功能，这里暂不实现
//        if (targetTemp != null) {
//          offsetOnCenterType = ChildOnScreenScroller.getScrollToPositionOffsetOnCenterType(recyclerView,targetTemp,recyclerView.getOrientation());
//        }
//        result =  boxStart - viewStart + offset + offsetOnCenterType;
//      }else if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_NONE){
//        result = 0;
//      }else{
//        result =  boxStart - viewStart + offset;
//      }
//      if (LogUtils.isDebug()) {
//        Log.d(TAG, "TestScroll viewStart " + viewStart + ",viewEnd:" + viewEnd + ",boxStart:" + boxStart + ",boxEnd:" + boxEnd);
//        Log.d(TAG, "TestScroll return" + result+",type:"+type +",targetView:"+targetTemp);
//      }
//      return result;
      return boxStart - viewStart + offset;
    }

    @Override
    public int calculateDyToMakeVisible(View view, int snapPreference) {
      return super.calculateDyToMakeVisible(view, snapPreference);
    }

    @Override
    public int calculateDxToMakeVisible(View view, int snapPreference) {
      return super.calculateDxToMakeVisible(view, snapPreference);
    }

//    @Override
//    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
//      float speed =  super.calculateSpeedPerPixel(displayMetrics);
//      if (LogUtils.isDebug()) {
//        Log.d(TAG, "TestScroll calculateSpeedPerPixel speed"+time+",dx:"+dx);
//      }
//      return speed;
//    }
//
//    @Override
//    protected int calculateTimeForScrolling(int dx) {
//      int time  = super.calculateTimeForScrolling(dx);
//      if (LogUtils.isDebug()) {
//        Log.d(TAG, "TestScroll calculateTimeForScrolling"+time+",dx:"+dx);
//      }
//      return time;
//    }
  }


//  public MyLinearLayoutManager getSingleLineLayoutManager() {
//    return (MyLinearLayoutManager) getMyLayoutManager().getRealManager();
//  }


  private ArrayList<View> mTempList;
  private boolean preventSearch = false;

  @Override
  public void addFocusables(ArrayList<View> views, int direction) {
    if (LogUtils.isDebug()) {
      LogUtils.d(FocusDispatchView.TAG, "addFocusables views size :" + (views == null ? 0 : views.size()));
    }
    super.addFocusables(views, direction);
  }

  @Override
  public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
    //FIXME 这里需要优化性能、return focused后，仍执行这里的代码
    if (LogUtils.isDebug()) {
      LogUtils.d(FocusDispatchView.TAG, "addFocusables start inTVListView " + ",enableFocusMemory:" +enableFocusMemory + ",this:" + ExtendUtil.debugView(this));
    }
    if(mFocusSearchHelper.addFocusables(views,direction)){
      return ;
    }
    if (isPageHidden || (skipFocusOnPause && postTaskPaused)) {
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "addFocusables return on isPageHidden  :" + isPageHidden + ",postTaskPaused:" + postTaskPaused + ",this:" + this);
      }
      return;
    }
    if (preventSearch && hasFocus()) {
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "addFocusables prevantSearch for one time");
      }
//      views.clear();
      return;
    }
    if (mFocusSearchTargets != null) {
      for (View v : mFocusSearchTargets) {
        if (v != null) {
          v.addFocusables(views, direction, focusableMode);
        }
      }

      if (LogUtils.isDebug()) {
        for (View v : views) {
          Log.d(FocusDispatchView.TAG, "addFocusables In ListView id:" + this.getId() + ",toSearchView:" + v);
        }
        Log.d(FocusDispatchView.TAG, "addFocusables In Target:" + Arrays.toString(mFocusSearchTargets) + " views size:" + views.size() + ",ListView id:" + this.getId());
      }
      return;
    } else {
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "addFocusables mFocusSearchTargets is null");
      }
    }

    if (!hasFocus() && enableFocusMemory) {

      View target = null;
      target = getLayoutManagerCompat().findViewByPosition(mTargetFocusChildPosition);
      if (!isSelectedChildValid(target)) {
        target = null;
      }
      if (target != null) {
        //没有View被选中
        target.addFocusables(views, direction, focusableMode);
        if (LogUtils.isDebug()) {
          Log.d("SingleLineRecyclerView", "addFocusables on mTargetFocusChildPosition：" + mTargetFocusChildPosition);
        }
        return;
      }

      View mSelectedChild = findSelectedChild();

      if (!isSelectedChildValid(findSelectedChild())) {
        mSelectedChild = null;
      }
      final int childCount = getChildCount();
      if (childCount > 0) {
        if (mSelectedChild == null) {
          //没有View被选中
          final View v = getChildAt(0);
          if (v != null && v.getVisibility() == View.VISIBLE) {
            if (LogUtils.isDebug()) {
              Log.d("SingleLineRecyclerView", "没有过焦点的列表，焦点给第一个view：" + v);
            }
            if (mTempList == null) {
              mTempList = new ArrayList();
            } else {
              mTempList.clear();
            }
            v.addFocusables(mTempList, direction, focusableMode);
            if (mTempList.size() > 0) {
              views.addAll(mTempList);
            } else {
              if (LogUtils.isDebug()) {
                Log.e(TAG, "RecyclerView add addFocusables empty,call super.addFocusables(), first child:" + v);
              }
              super.addFocusables(views, direction, focusableMode);
            }
            mTempList.clear();
          } else {
            super.addFocusables(views, direction, focusableMode);
          }
        } else {
          if (LogUtils.isDebug()) {
            Log.d("SingleLineRecyclerView", "有过焦点的列表，焦点给曾经有过焦点的View：mLastFocusedChild：");
          }
          mSelectedChild.addFocusables(views, direction, focusableMode);
        }
      }
    } else {
      super.addFocusables(views, direction, focusableMode);
      if (LogUtils.isDebug()) {
        LogUtils.e(FocusDispatchView.TAG, "addFocusables by super views size:" + views.size() + ",this:" + this);
      }
    }
    if (LogUtils.isDebug()) {
      LogUtils.d(FocusDispatchView.TAG, "addFocusables end TVListView " + ",resultCount:" + views.size());
//      for (View v : views) {
//        Log.d(FocusDispatchView.TAG, "----addFocusables by super add view:" + v);
//      }
    }
  }

  /**
   * 设置选中的子view,注意此方法只在view已经显示出来以后调用才有效。
   *
   * @param position
   */
  public void setSelectChildPosition(int position, boolean changeTargetFocusChild) {
    if(LogUtils.isDebug()) {
      Log.i(TAG, "setSelectChildPosition position:" + position + " ,changeTargetFocusChild:" + changeTargetFocusChild + ",hasCode:" + ExtendUtil.debugViewLite(this) + ",enableSelectOnFocus:" + enableSelectOnFocus);
    }
    final View selectedChild = findSelectedChild();
    if (selectedChild != null && selectedChild.isSelected()) {
      selectedChild.setSelected(false);
    }
    if (changeTargetFocusChild) {
      setTargetFocusChildPosition(position);
    }
    this.defaultSectionPosition = position;
    if (!hasFocus() || !enableSelectOnFocus) {
      final View next = findViewByPosition(position);
      changeSelectState(next, true);
    }
  }

  public void clearSelectChild(){
    final View selectedChild = findSelectedChild() == null ? findViewByPosition(defaultSectionPosition) : findSelectedChild();
//    if(LogUtils.isDebug()) {
//      Log.i(TAG, "clearSelectChild selectedChild:" + ExtendUtil.debugViewLite(selectedChild) + " defaultSectionPosition:" + defaultSectionPosition + ",singleSelectPosition:" + getControlProps().singleSelectPosition);
//    }
    if (selectedChild != null) {
      selectedChild.setSelected(false);
    }
    defaultSectionPosition = -1;
  }

  //获取当前selection状态
  public int getSelectChildPosition() {
    return defaultSectionPosition;
  }


  void changeSelectState(final int position, boolean select) {
    final View child = findViewByPosition(position);
    if (child != null) {
      changeSelectState(child, select);
    }
  }

  void changeSelectState(View child, boolean select) {
//      if(child != null && child != getFocusedChild()){
    if (child != null) {
      if (select) {
        if (child.isSelected()) {
          child.setSelected(false);
        }
        child.setSelected(true);
      } else {
        child.setSelected(false);
      }
    }
  }

  public View findViewByPosition(int position) {
    return getLayoutManagerCompat().findViewByPosition(position);
  }

  public void setEnableReFocus(boolean enableReFocus) {
    getLayoutManagerCompat().getExecutor().enableReFocus = enableReFocus;
  }


  public void destroy() {
    if (LogUtils.isDebug()) {
      Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER clearAllTask on destroy this:"+this);
    }
    clearAllTask();
    mLoadMoreListener = null;
    mScrollToTopListener = null;
  }


  @Override
  public void requestLayout() {
    super.requestLayout();
  }

  void requestLayoutManual() {
    if (getId() != -1) {
      RenderUtil.requestNodeLayout(this);
    } else {
      RenderUtil.reLayoutView(this, (int) getX(), (int) getY(), getWidth(), getHeight());
    }
  }

  @Override
  public View getHostView() {
    return this;
  }

  private NegativeLongKeyFlinger mFlinger;

  @Override
  public boolean dispatchKeyEventPreIme(KeyEvent event) {

    if(event.getAction() == KeyEvent.ACTION_DOWN && getLayoutExecutor() != null){
//      Log.i("preventChildOnScreen","dispatchKeyEventPreIme event key:"+ event.getKeyCode()+"-----------------------");
      switch (event.getKeyCode()) {
          case KeyEvent.KEYCODE_DPAD_LEFT:
            getLayoutExecutor().mCurrentDirection = View.FOCUS_LEFT;
            break;
          case KeyEvent.KEYCODE_DPAD_RIGHT:
            getLayoutExecutor().mCurrentDirection = View.FOCUS_RIGHT;
            break;
          case KeyEvent.KEYCODE_DPAD_UP:
            getLayoutExecutor().mCurrentDirection = View.FOCUS_UP;
            break;
          case KeyEvent.KEYCODE_DPAD_DOWN:
            getLayoutExecutor().mCurrentDirection = View.FOCUS_DOWN;
            break;
        }
    }
    if (negativeKeyTime > 0) {
      if (mFlinger == null && getLayoutManagerCompat() != null) {
        mFlinger = new NegativeLongKeyFlinger(this, negativeKeyTime);
        mFlinger.setVertical(getLayoutManagerCompat().getOrientation() == RecyclerView.VERTICAL);
      }
      if (mFlinger != null) {
        if (mFlinger.dispatchKeyEventPreIme(event)) {
          return true;
        }
      }
      //接收事件
    }
    return super.dispatchKeyEventPreIme(event);

  }

  public ChildOnScreenScroller getChildOnScreenScroller(){
    return getLayoutExecutor().getChildOnScreenScroller();
  }

  @Override
  public int getOffsetX() {
    return mOffsetX;
  }

  @Override
  public int getOffsetY() {
    return mOffsetY;
  }

  public void setScrollOffset(int mScrollOffset) {
    this.mScrollOffset = mScrollOffset;
    if (getLayoutExecutor().getChildOnScreenScroller() != null) {
      getLayoutExecutor().getChildOnScreenScroller().setScrollOffset(mScrollOffset);
    }
  }

  int advancedFocusSearchMore = 0;

  protected void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter){

  }

//  public void setEnableAutoFocus(boolean enableAutoFocus) {
//    Log.d(TAG,"autoFocus setEnableAutoFocus enableAutoFocus:"+enableAutoFocus);
//    this.enableAutoFocus = enableAutoFocus;
//    setFocusable(enableAutoFocus);
//  }

  /**
   * 执行layoutManager中一些方法的实际执行类，用来将gridLayout与linearLayout的逻辑整合
   */
  public static class LayoutManagerExecutor {

    private final int orientation;
    final ChildOnScreenScroller.Default mGridChildOnScreenScroller;
    private FocusEventListener mFocusListener;
    private final TVListView boundView;
    private final FastAdapter mAdapter;
    private int mCurrentDirection = -1;
    private PendingWork mPendingWork;
    private ReFocus mReFocus;
    private boolean enableReFocus = false;
    private int mSpanCount = 0;
    private LayoutManagerCompat mLayoutManagerCompat;
    private boolean isLoading = false;
    private int mdx, mdy;

    public LayoutManagerExecutor(TVListView tvListView, int mSpanCount, int orientation, LayoutManagerCompat layoutManager, FastAdapter adapter) {
      this.boundView = tvListView;
      this.orientation = orientation;
      this.mSpanCount = mSpanCount;
      this.mAdapter = adapter;
      this.setFocusEventListener(new FocusEventListener());
      mGridChildOnScreenScroller = new ChildOnScreenScroller.Default(orientation);
      mGridChildOnScreenScroller.setScrollOffset(boundView.mScrollOffset);
      this.mLayoutManagerCompat = layoutManager;
      this.boundView.addOnScrollListener(new OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
          super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
          super.onScrolled(recyclerView, dx, dy);
          mdx = dx;
          mdy = dy;
        }
      });
    }

    //是否向上滚动
    public boolean isScrollUp() {
      return mdy > 0;
    }

    //是否向左滚动
    public boolean isScrollLeft() {
      return mdx > 0;
    }

    public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child, @NonNull Rect rect, boolean immediate, boolean focusedChildVisible) {
//      if (boundView.rootView == null) {
//        this.boundView.rootView = FocusDispatchView.findRootView(parent);
//      }
//      if (boundView.rootView != null) {
//        if (LogUtils.isDebug()) {
//          Log.e(TAG, "preventChildOnScreen lastKey :" + boundView.rootView.getLastKeyCode());
//        }
//        switch (boundView.rootView.getLastKeyCode()) {
//          case KeyEvent.KEYCODE_DPAD_LEFT:
//            mCurrentDirection = View.FOCUS_LEFT;
//            break;
//          case KeyEvent.KEYCODE_DPAD_RIGHT:
//            mCurrentDirection = View.FOCUS_RIGHT;
//            break;
//          case KeyEvent.KEYCODE_DPAD_UP:
//            mCurrentDirection = View.FOCUS_UP;
//            break;
//          case KeyEvent.KEYCODE_DPAD_DOWN:
//            mCurrentDirection = View.FOCUS_DOWN;
//            break;
//        }
//      } else {
//        if (LogUtils.isDebug()) {
//          Log.e(TAG, "preventChildOnScreen root null");
//        }
//      }
      if (boundView.onInterceptRequestChildRectangleOnScreen(child, rect, immediate, mCurrentDirection, focusedChildVisible)) {
        Log.e(TAG, "requestChildRectangleOnScreen return on prevent focus direction,direction:" + mCurrentDirection);
        return true;
      }
      final ChildOnScreenScroller scroller = getChildOnScreenScroller();
      if (scroller != null && scroller.requestChildRectangleOnScreen(parent, child, rect, immediate, mCurrentDirection, focusedChildVisible, parent.getChildAdapterPosition(child))) {
        return true;
      }
      return false;
    }

    boolean isInReFocus() {
//          return mReFocus != null && mReFocus.inReFocus;
      return false;
    }

    public void setFocusEventListener(FocusEventListener mFocusListener) {
      this.mFocusListener = mFocusListener;
    }

    public int getFirstFocusChildPosition(int direction){
      return boundView.getFirstFocusChildPosition(direction);
    }

    public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
      final ChildOnScreenScroller scroller = boundView.mCustomChildOnScreenScroller != null ? boundView.mCustomChildOnScreenScroller : mGridChildOnScreenScroller;
      if (scroller != null && scroller.smoothScrollToPosition(recyclerView, state, position)) {
      } else {
        mLayoutManagerCompat.superSoothScrollToPosition(recyclerView, state, position);
      }
    }

    public void startSmoothScroll(SmoothScroller smoothScroller) {
      mLayoutManagerCompat.superStartSmoothScroll(smoothScroller);
    }

    public void scrollToPositionWithOffset(int position, int offset) {
      mLayoutManagerCompat.superScrollToPositionWithOffset(position, offset);
    }

    public void scrollToPosition(int position) {
      mLayoutManagerCompat.superScrollToPosition(position);
    }

    public void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter) {
      boundView.defaultSectionPosition = -1;
      boundView.mOffsetY = 0;
      boundView.resetScrollOffset();
      boundView.preventSearch = false;
      //Log.i(TAG, "onAdapterChanged");
      mReFocus = null;
      boundView.onAdapterChanged(oldAdapter,newAdapter);
    }

    int getItemCount() {
      return mLayoutManagerCompat.getRealLayout().getItemCount();
    }

    private boolean searchFocusInItem = false;

    public void setSearchFocusInItem(boolean searchFocusInItem) {
      this.searchFocusInItem = searchFocusInItem;
    }



    boolean firstTime = true;

    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
      final int focusPosition = boundView.mFocusChildPosition;
      int vector = mLayoutManagerCompat.getVectorByDirection(focusDirection, mSpanCount, focusPosition);
      int targetPosition = focusPosition + vector;
      if (targetPosition < 0 && !mLayoutManagerCompat.isGridLayout()) {
        //只有linear模式，这种判断才准确
        Log.e(FocusDispatchView.TAG, "onFocusSearchFailed return on targetPosition < 0");
        return null;
      }
//      else if (mLayoutManagerCompat.isGridLayout()) {//fix by xxd
//        //grid模式使用
//        Log.e(FocusDispatchView.TAG, "onFocusSearchFailed isGridLayout");
//        return null;
//      }
      View resultFromParent = mLayoutManagerCompat.superFocusSearchFailed(focused, focusDirection, recycler, state);
      View filledByFocusFail = resultFromParent;
      //从FirstFocusHelper中寻找
      if (resultFromParent != null) {
        if (!InternalExtendViewUtil.testFocusable(resultFromParent)) {
          Log.e(FocusDispatchView.TAG, "resultFromParent no focusable children resultFromParent null");
          resultFromParent = null;
        }
      }

      if (LogUtils.isDebug()) {
        LogUtils.e(FocusDispatchView.TAG, "onFocusSearchFailed , direction:" + focusDirection + ",resultFromParent  :" + resultFromParent+",this:"+ExtendUtil.debugViewLite(boundView));
      }
      if (boundView.mListenFocusSearchOnFail && resultFromParent == null) {
        InternalExtendViewUtil.sendEventOnListFocusSearchFailed(boundView, boundView.getFocusedChild(), focused, focusDirection, getFocusSearchFailEvent());
        if (LogUtils.isDebug()) {
          LogUtils.e(FocusDispatchView.TAG, "ul: mListenFocusSearchOnFail == true , return focused direction:" + focusDirection + ",this ID :" + boundView.getId());
        }
        boundView.preventSearch = true;
        return focused;
      }

      if (InternalExtendViewUtil.isContainBlockDirection(focusDirection, boundView.mBlockFocusOnFail)) {
        if (resultFromParent == null || boundView.forceBlockFocusOnFail) {
          if (LogUtils.isDebug()) {
            Log.e(FocusDispatchView.TAG, "ul 3: mBlockFocusOnFail is true return focused ,direction:" + focusDirection + ",this ID :" + boundView.getId() + ",forceBlockFocusOnFail:" + boundView.forceBlockFocusOnFail);
          }
//          if(firstTime){
//            //2025 zhaopeng 这里简单的处理一下，防止秒懂百科等数据正好卡在屏幕外，导致焦点无法移动
//            if(LogUtils.isDebug()){
//              Log.e(FocusDispatchView.TAG,"return on firstTime");
//            }
//            firstTime = false;
//            return null;
//          }
          boundView.preventSearch = true;
          if(filledByFocusFail != null){
            ExtendTag.putExtraValue(filledByFocusFail,"blockedOnFail",true);
            Log.e(FocusDispatchView.TAG,"return on resultFromParent == null || boundView.forceBlockFocusOnFail,filledByFocusFail: "+ExtendUtil.debugViewLite(filledByFocusFail));
          }
          return filledByFocusFail == null ? focused : filledByFocusFail;
        } else {
          if (LogUtils.isDebug()) {
            Log.e(FocusDispatchView.TAG, "ul 2: mBlockFocusOnFail is true resultFromParent !null ,direction:" + focusDirection + ",this ID :" + boundView.getId());
          }
          return resultFromParent;
        }
      }
      if (mFocusListener != null && resultFromParent == null) {
        final View v = mFocusListener.onFocusSearchFailed(focused, focusDirection, recycler, state);
        if (v != null) {
          return v;
        }
      }
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "onFocusSearchFailed return super result:" + resultFromParent + ",this ID :" + boundView.getId());
      }
      return resultFromParent;
    }

    private HippyViewEvent mFocusSearchFailEvent;

    private HippyViewEvent getFocusSearchFailEvent() {
      if (mFocusSearchFailEvent == null) {
        mFocusSearchFailEvent = new HippyViewEvent(InternalExtendViewUtil.LIST_FOCUS_SEARCH_FAIL_EVENT_NAME);
      }
      return mFocusSearchFailEvent;
    }

    @Nullable
    public View onInterceptFocusSearch(@NonNull View focused, int direction) {
      mCurrentDirection = direction;
      if (InternalExtendViewUtil.isContainBlockDirection(direction, boundView.mBlockFocus)) {
        LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch ul 1: containBlockDirection is true return focused ,direction:" + direction + ",this ID :" + boundView.getId());
        boundView.preventSearch = true;
        return focused;
      }
      if (!boundView.useAdvancedFocusSearch) {
        return null;
      }
      View result = null;
      if (mFocusListener != null) {
        result = mFocusListener.onInterceptFocusSearch(focused, direction);
      }
      if(result == null){
        //用户自定义的目标viewID
        //这里需要从瀑布流内部寻找，不能只在组件内寻找，否则焦点只能在第一个位置出现
        View focusedDePlaceholder = FastAdapterUtil.finalRealContent(focused);
        //这里只在此tv-list中寻找，
        FocusUtils.FocusParams fp = ExtendUtil.findUserSpecifiedNextFocusViewIdTraverse(boundView, focusedDePlaceholder, direction,boundView.getParent() instanceof View ? (View) boundView.getParent() : null);
//        if(fp.specifiedTargetSID != null && fp.specifiedTargetSID.equals(ExtendUtil.getViewSID(boundView))
//          || fp.specifiedTargetViewName != null && fp.specifiedTargetViewName.equals(ExtendUtil.getViewName(boundView))){
//          return focused;
//        }
        //Log.e(FocusDispatchView.TAG,"searchInTargets fp:"+fp);
        if(LogUtils.isDebug()){
          Log.i(FocusDispatchView.TAG,"advanceFocusSearch in tvList on onInterceptFocusSearch focused:"+focusedDePlaceholder+",sid:"+ExtendUtil.getViewSID(focusedDePlaceholder)+",FocusParams:"+fp);
        }
        View target = ExtendUtil.findViewByNameOrSID(focused,fp, boundView.getRootView());
        if(LogUtils.isDebug()){
          Log.i(FocusDispatchView.TAG,"findViewByNameOrSID target:"+ExtendUtil.debugView(target));
        }
        if(target != null){
          if(LogUtils.isDebug()){
            Log.i(FocusDispatchView.TAG,"advanceFocusSearch in target :"+ExtendUtil.debugView(target)+",FocusParams:"+fp);
          }
          result = boundView.searchInTargetsOnRootView(focused, direction);
          if(result != null && result.getVisibility() != View.VISIBLE){
            result = null;
          }
          if(LogUtils.isDebug() && result != null) {
            Log.e(FocusDispatchView.TAG, "advanceFocusSearch in tvList return userSpecifiedTarget sid:" + fp.specifiedTargetSID + ",result:" + ExtendUtil.debugView(result));
          }
        }
      }
      if (result == null) {
        final int focusPosition = boundView.mFocusChildPosition;
        int vector = mLayoutManagerCompat.getVectorByDirection(direction, mSpanCount, focusPosition);
        if (vector != 0 || searchFocusInItem) {
          result = mLayoutManagerCompat.onInterceptSearchInside(boundView, focusPosition, vector, focused, direction);
          if(LogUtils.isDebug()) {
            Log.d(FocusDispatchView.TAG, "mLayoutManagerCompat onInterceptSearchInside result:" + result);
          }
          if (result != null) {
            return result;
          }
        }
      }
      if (result != null) {
        boundView.preventSearch = true;
      }

      if (result == null && InternalExtendViewUtil.isContainBlockDirection(direction, boundView.mBlockFocusOnFail)) {
        if (mLayoutManagerCompat.isGridLayout()) {
          boundView.preventSearch = true;
          return focused;
        }
        //这里不等到onFocusSearchFailed了，直接在这里将焦点锁定住
        final int focusPosition = boundView.mFocusChildPosition;
        int vector = mLayoutManagerCompat.getVectorByDirection(direction, mSpanCount, focusPosition);
        int targetPosition = focusPosition + vector;
        final int itemCount = getItemCount();
        if ((boundView.shakePreCheckNumber < itemCount && targetPosition > itemCount - boundView.shakePreCheckNumber) || targetPosition < 0) {
          if (LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch mBlockFocusOnFail is true return focused ,direction:" + direction + ",this ID :" + boundView.getId() + ",boundView.shakePreCheckNumber:" + boundView.shakePreCheckNumber + ",itemCount:" + itemCount + ",targetPosition:" + targetPosition);
          }
          boundView.preventSearch = true;
          return focused;
        }
        //return focused;
      }


      if(result == null){
        result = boundView.onInterceptFocusSearchFailed(focused,direction);
        if (result != null) {
          return result;
        }
      }

      if (result == null && mFocusListener != null) {
        final int focusPosition = boundView.mFocusChildPosition;
        final View v = mFocusListener.onInterceptFocusSearchFailed(focused, direction, mLayoutManagerCompat.getVectorByDirection(direction, mSpanCount, focusPosition));
        if (v != null) {
          return v;
        }
      }
      return result;
    }

    int getPosition(View view) {
      return mLayoutManagerCompat.getRealLayout().getPosition(view);
    }

    boolean isPositionShakeNeed(int pos, int itemCount, int spanCount) {
      if (mLayoutManagerCompat.isGridLayout()) {
        final int contentCount = getItemCount() - boundView.shakePreCheckNumber;
        int rowLine = contentCount / mSpanCount;
        boolean isLastLine = pos >= mSpanCount * rowLine;
        return isLastLine && pos > (itemCount - boundView.shakePreCheckNumber);
      } else {
        return pos > (itemCount - boundView.shakePreCheckNumber);
      }
    }

    void shakeEndIfNeed(View focused, View result, int focusDirection) {
      if (LogUtils.isDebug()) {
        LogUtils.d(FocusDispatchView.TAG, "shakeEndIfNeed mShakeEndEnable" + boundView.mShakeEndEnable);
      }
      if (boundView.mShakeEndEnable) {
        final View focusedChild = boundView.getFocusedChild();
        boolean isDirectionRight;
        if (orientation == HORIZONTAL) {
          isDirectionRight = focusDirection == FOCUS_RIGHT;
        } else {
          isDirectionRight = focusDirection == FOCUS_DOWN;
        }
        if (focusedChild != null) {
          if (isDirectionRight) {
            final int pos = getPosition(focusedChild);
            final int itemCount = getItemCount();
            final boolean shake = isPositionShakeNeed(pos, itemCount, mSpanCount) && (focused == result || result == null);
            if (LogUtils.isDebug()) {
              LogUtils.d(FocusDispatchView.TAG, "MyGridLayout shakeEndIfNeed pos:" + pos + " ,itemCount:" + itemCount + " focused == result:" + (focused == result) + ",result:" + result);
            }
            if (shake) {
//              boundView.exeShakeRecycleView();
              boundView.exeShakeSelf(focused, focusDirection);
            } else {
              if (LogUtils.isDebug()) {
                LogUtils.d(FocusDispatchView.TAG, "MyGridLayout shakeEndIfNeed no shake");
              }
            }
          }
        } else {
          if (LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "MyGridLayout shakeEndIfNeed !(focusedChild != null && isDirectionRight)");
          }
        }
      }
    }


    public View findViewByPosition(int pos) {
      return boundView.findViewByPosition(pos);
    }

    public void onLayoutCompleted(State state) {
      if (LogUtils.isDebug()) {
        Log.e(TAG, "onLayoutCompleted state:" + state + ",mPendingWork:" + mPendingWork + ",this:" + this);
        Log.i(AutoFocusManager.TAG, "onLayoutCompleted state:" + state + ",mPendingWork:" + mPendingWork + ",this:" + this);
        if("WaterfallListView".equals(boundView.getClass().getSimpleName())) {
          Log.e(FocusDispatchView.TAG, "onLayoutCompleted state:" + state + ",mPendingWork:" + mPendingWork + ",this:" + this);
        }
      }
      if (mPendingWork != null) {
        final int target = mPendingWork.position;
        final View child = findViewByPosition(target);

        if (child != null && mPendingWork.position == target && mPendingWork.position > -1) {
          //定位
          mPendingWork.position = -1;
          boundView.stopScroll();
            final int offset = ChildOnScreenScroller.getScrollToPositionOffset(target, boundView, child, orientation, getItemCount(), mGridChildOnScreenScroller.type, 0) + mPendingWork.offset;
            if (LogUtils.isDebug()) {
              Log.i(TAG, "!!!!onLayoutCompleted on pendingWork exe scrollBy offset:" + offset);
              Log.e(AutoFocusManager.TAG, "!!!!onLayoutCompleted on pendingWork exe scrollBy offset:" + offset+",getRemainingScrollVertical :"+state.getRemainingScrollVertical());
            }
//            mReFocus = new ReFocus(target,null,null);
            if(target == 0){
              //0的时候不需要滚动
              boundView.resetScrollOffset();
              Log.e(TAG,"!!!!onLayoutCompleted on pendingWork return on getRemainingScrollVertical :"+state.getRemainingScrollVertical());
            }else {
              scrollToPositionWithOffset(target, offset);
              boundView.requestLayoutManual();
            }
//          if (orientation == VERTICAL) {
//            //* -1是由于与系统scrollToPositionWithOffset的方向一致
//            boundView.scrollBy(0, offset * -1);
////            boundView.overScrollBy()
//          } else {
//            boundView.scrollBy(offset * -1, 0);
//          }
//          RenderUtil.reLayoutView(boundView);
        }
      }
      //发送滚动事件
      boundView.handleScrollValue();
      handleFocusAfterLayout(state);
      boundView.scaleChildren4Coverflow();
    }

    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
      handleFocusBeforeLayout(recycler, state);
      if (LogUtils.isDebug()) {
        Log.e(TAG, "onLayoutChildren childCount:" + boundView.getChildCount());
      }
    }

    boolean takeoverFocusIfNeed() {
      if (boundView.hasFocus()) {
        boundView.setFocusable(true);
        boundView.requestFocus();
        if (LogUtils.isDebug()) {
          Log.e(FocusDispatchView.TAG, "+++takeoverFocusIfNeed takeover recyclerView focused:" + boundView.isFocused() + ",this:" + boundView);
        }
        return true;
      }
      return false;
    }

    public void onDetachedFromWindow(RecyclerView view, Recycler recycler) {
     // Log.i(TAG,"onDetachedFromWindow view:"+ExtendUtil.debugViewLite(view));
      clearReFocus();
    }


    protected void handleFocusBeforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
//      if(boundView.getItemAnimator() != null &&  !state.isPreLayout()){
//        //有动画时，只在preLayout阶段处理 //fixme 这里夸克会有bug,暂时注掉
//        Log.w("ReFocus","handleFocusBeforeLayout state.isPreLayout true return");
//        return;
//      }
      if (LogUtils.isDebug()) {
        Log.i("ReFocus", "+++handleFocusBeforeLayout state:" + state + " ,hasFocus:" + boundView.hasFocus()+",refocusType:"+boundView.getRefocusType()+",this:"+ExtendUtil.debugViewLite(boundView));
      }

      if (boundView.mOnLayoutListener != null) {
        boundView.mOnLayoutListener.onBeforeLayout(boundView, state);
      }
      if (enableReFocus) {
        if (boundView.initFocusPositionAfterLayout > -1) {
          if (LogUtils.isDebug()) {
            LogUtils.d("ReFocus", "handleFocusBeforeLayout initFocusPositionAfterLayout:" + boundView.initFocusPositionAfterLayout);
          }
          mReFocus = new ReFocus(boundView.initFocusPositionAfterLayout, null, null,null,null);
          boundView.initFocusPositionAfterLayout = -1;
        } else {
          final View child = boundView.getFocusedChild();
          final int focusPosition = boundView.mFocusChildPosition;

          if (boundView.hasFocus() && child != null) {
            final View focused = child.findFocus();
            //上面判断一下类型，兼容一下旧版代码z2
            final int oldPos = focusPosition > -1 ? focusPosition : getPosition(child);
//            takeoverFocusIfNeed();
//            InternalExtendViewUtil.blockRootFocus(boundView);
            //2025 04 05 上面注掉，改由直接将全局的焦点锁定在当前view上,否则还是会有焦点丢失的问题
            InternalExtendViewUtil.blockRootFocus(boundView.getRootView());
            mReFocus = new ReFocus(oldPos,
              boundView.getRefocusType() == RefocusType.LEGACY ? focused : null, child,
              ExtendUtil.getViewSID(focused),ExtendUtil.getViewName(focused));
            if (LogUtils.isDebug()) {
              LogUtils.d("ReFocus", "+++handleFocusBeforeLayout mReFocus:" + mReFocus);
            }
          } else {
            if (!boundView.hasFocus() ) {
              //如果当前View没有焦点，刚
              if (LogUtils.isDebug()) {
                LogUtils.d("ReFocus", "handleFocusBeforeLayout no focus clear mReFocus hasFocus");
              }
              mReFocus = null;
            }else if(mReFocus != null){
              mReFocus = new ReFocus(mReFocus.oldPosition, null, child,mReFocus.oldFocusSid, mReFocus.oldName);
            }
          }
        }
      }//自动保存焦点
//        prepareAutoFocus();

    }

    protected void handleFocusAfterLayout(RecyclerView.State state) {
      if (LogUtils.isDebug()) {
        Log.i("ReFocus", "---handleFocusAfterLayout state:" + state + " ,hasFocus:" + boundView.hasFocus());
      }
      if (boundView != null) {
        boundView.onLayoutComplete(state);
      }
//      if(boundView != null && boundView.hasFocus()){
//        Log.w("ReFocus","return on hasFocus, no need to doReFocus "+mReFocus+",this:"+ExtendUtil.debugViewLite(boundView));
//        InternalExtendViewUtil.unBlockRootFocus(boundView.getRootView());
//        eatReFocus();
//        return;
//      }
      doReFocus();
    }

    public int findFirstVisibleChild(boolean completelyVisible) {
      return  mLayoutManagerCompat.findFirstVisibleChild(completelyVisible);
    }

    void doReFocus() {
      if(LogUtils.isDebug()) {
        if(mReFocus != null) {
          Log.e("ReFocus", "doReFocus mReFocus:" + mReFocus + ",boundView:" + ExtendUtil.debugViewLite(boundView) + ",type:" + boundView.getRefocusType());
        }else{
          Log.d("ReFocus", "doReFocus mReFocus:" + null + ",boundView:" + ExtendUtil.debugViewLite(boundView) + ",type:" + boundView.getRefocusType());
        }
      }
      if (mReFocus != null && mReFocus.valid && !isInReFocus()) {
        //1. 尝试找到上次
        mReFocus.inReFocus = true;
        final RefocusType refocusType = boundView.getRefocusType();

        final int oldPos = mReFocus.oldPosition;

        View target = null;
        View child = null;

        switch (refocusType) {
          case LEGACY:
          case KEEP_POSITION:
            target = findViewByPosition(oldPos);
            if(!InternalExtendViewUtil.testFocusable(target)){
              int keepPosition = findFocusableChildBackward(oldPos);
              target = findViewByPosition(keepPosition);
              if(LogUtils.isDebug()) {
                Log.d("ReFocus", "find backward, keepPosition:" + keepPosition + ",target:" + ExtendUtil.debugView(target));
              }
            }
            final View f = target;
            if ( f != null && !f.isFocused()) {
              final View oldFocused = mReFocus.oldFocus;
              child = f;
              target = f;
              if (f instanceof ViewGroup) {
                if (TVViewUtil.isViewDescendantOf(oldFocused, (ViewGroup) f) && oldFocused != f) {
                  if (LogUtils.isDebug()) {
                    LogUtils.d("ReFocus", "focus isViewDescendantOf child true");
                  }
                  target = oldFocused;
                }
              }
              if (LogUtils.isDebug()) {
                LogUtils.d("ReFocus", "---handleFocusAfterLayout exeRequestFocus oldFocusPos:" + oldPos + " target:" + target);
                LogUtils.d("PendingFocus", "---handleFocusAfterLayout exeRequestFocus oldFocusPos:" + oldPos + " target:" + target);
              }
            }
            break;
          case KEEP_SID:
            target = ExtendUtil.findViewBySID(mReFocus.oldFocusSid, boundView);
            break;
          case FIRST_VISIBLE:
            if(mReFocus.oldPosition > -1){
              target = findViewByPosition(mReFocus.oldPosition);
              Log.d("ReFocus", "FIRST_VISIBLE oldPosition:" + mReFocus.oldPosition + ",target:" + ExtendUtil.debugViewLite(target));
            }
            if(target == null) {
              int firstVisibleChild = findFirstVisibleChild(true);
              Log.d("ReFocus", "FIRST_VISIBLE firstVisibleChild:" + firstVisibleChild + ",mReFocus:" + mReFocus);
              target = findViewByPosition(firstVisibleChild);
            }
            break;
          case AT_START:
            if(mReFocus.oldPosition > -1){
              target = findViewByPosition(mReFocus.oldPosition);
            }
            if(target == null) {
              target = findViewByPosition(0);
            }
            break;
          default:
            break;
        }
        if (target != null) {
          if (LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 4 and requestFocus");
          }
          if(mReFocus.oldName != null){
            View viewByName = ExtendUtil.findViewByName(mReFocus.oldName, target);
            if(viewByName != null){
              Log.d("ReFocus","findViewByName target:"+ExtendUtil.debugView(target));
              target = viewByName;
            }
          }
          InternalExtendViewUtil.unBlockRootFocus(boundView.getRootView());
          callNotifyInReFocus(child != null ? child : target, true);
          target.requestFocus();
          callNotifyInReFocus(child != null ? child : target, false);
        }else{
          if (LogUtils.isDebug()) {
            LogUtils.d("ReFocus", "cant find oldFocus unBlockFocus");
            LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 5 ");
          }
          if (mReFocus != null && mReFocus.valid) {
            if (LogUtils.isDebug()) {
              Log.d(FocusDispatchView.TAG, "postReFocus 5," + mReFocus);
            }
//                postReFocus();
          }
          InternalExtendViewUtil.unBlockRootFocus(boundView.getRootView());
          if (boundView.isFocused()) {
            if (LogUtils.isDebug()) {
              Log.i(FocusDispatchView.TAG, "---takeoverFocusIfNeed false recyclerView focused:" + boundView.isFocused() + ",this:" + boundView);
            }
          }
//          if (boundView.isFocusable()) {
//            boundView.setFocusable(false);
//          }
        }
      }else{
        if (LogUtils.isDebug()) {
          LogUtils.d("ReFocus", "doReFocus mReFocus null or invalid");
        }
      }
      eatReFocus();
    }

    private void eatReFocus() {
      mReFocus = null;
//      boundView.invalidate();
    }


    public int findFocusableChildBackward(int pos) {
      View view = findViewByPosition(pos);
      if(InternalExtendViewUtil.testFocusable(view)){
        return pos;
      }
      for (int i = pos - 1; i >= 0; i--) {
        int result = findFocusableChildBackward(i);
        if (result != -1) {
          return result;
        }
      }
      return -1;
    }

    public void clearReFocus() {
      this.mReFocus = null;
    }

    void callNotifyInReFocus(View child, boolean inFocus) {
      if (child instanceof ITVView) {
        ((ITVView) child).notifyInReFocus(inFocus);
      } else if (child instanceof RecyclerViewItem) {
        ((RecyclerViewItem) child).notifyInReFocus(inFocus);
      }
    }

    public void setPendingScroll(int pos, int offset) {
      if (mPendingWork == null) {
        mPendingWork = new PendingWork();
      }
      mPendingWork.position = pos;
      mPendingWork.offset = offset;
    }

    public void layoutDecoratedWithMargins(@NonNull View child, int left, int top, int right, int bottom) {
      final int pos = getPosition(child);
      if (LogUtils.isDebug()) {
        Log.d(TAG, "layoutDecoratedWithMargins pos:" + pos + "view:" + child.getId() + ",parenID:" + boundView.getId() + ",defaultSectionPosition:" + boundView.defaultSectionPosition + ",hasCode:" + boundView.hashCode());
        if("WaterfallListView".equals(boundView.getClass().getSimpleName())) {
          Log.d(FocusDispatchView.TAG, "layoutDecoratedWithMargins pos:" + pos + "view:" + child.getId() + ",parenID:" + boundView.getId() + ",defaultSectionPosition:" + boundView.defaultSectionPosition + ",hasCode:" + boundView.hashCode());
        }
      }
      if (getItemCount() - 1 > 0) {
        if (pos + boundView.preloadItemNumber >= getItemCount() - 1) {

          boundView.postDelayed(() -> {
            HippyMap map = new HippyMap();
            map.pushBoolean("isLastLine", true);
            map.pushInt("itemCount", getItemCount());
            map.pushInt("position", pos);
            if (boundView.mLoadMoreListener != null) {
              boundView.mLoadMoreListener.onLoadMore(pos, getItemCount());
            }
            if (boundView.getHandleEventNodeId() > -1) {
              boundView.getEventDeliverer().sendEvent("onLoadMore", boundView.getHandleEventNodeId(), map);
            } else {
              boundView.getEventDeliverer().sendEvent("onLoadMore", mAdapter.getRootListNodeID(), map);
            }
          }, boundView.loadDelayTime);
        }
      }
      if (boundView.defaultSectionPosition == pos && pos > -1) {
        if (LogUtils.isDebug()) {
          Log.i(TAG, "layoutDecoratedWithMargins change defaultSectionPosition pos:" + pos + "view:" + child.getId() + ",parenID:" + boundView.getId());
        }
        boundView.changeSelectState(child, true);
      } else {
        boundView.changeSelectState(child, false);
      }
      if (boundView.layoutTriggerTargetPosition > -1) {
        if (boundView.layoutTriggerTargetPosition == pos) {
          TriggerTaskManagerModule.dispatchTriggerTask(boundView, "onTargetChildLayout");
        }
      }
    }

    public void handleScrollValue(int vector, int sy) {
      getChildOnScreenScroller().notifyOnScroll(boundView, vector, 0, sy);
    }

    public void setNoScrollOnFirstScreen(boolean enable) {
      final ChildOnScreenScroller scroller = getChildOnScreenScroller();
      if (scroller != null) {
        scroller.setNoScrollOnFirstScreen(enable);
      }
    }

    private ChildOnScreenScroller getChildOnScreenScroller() {
      return boundView.mCustomChildOnScreenScroller != null ? boundView.mCustomChildOnScreenScroller : mGridChildOnScreenScroller;
    }
  }

  protected int getFirstFocusChildPosition(int direction) {
    return -1;
  }

  protected View onInterceptFocusSearchFailed(View focused, int direction) {
      return null;
  }

  public void setScrollEnable(boolean enable) {
    touchScrollEnabled = enable;
    getLayoutExecutor().getChildOnScreenScroller().setEnableScroll(enable);
  }

  public void setTouchScrollEnable(boolean enable) {
    touchScrollEnabled = enable;
  }

  @Override
  public boolean onTouchEvent(MotionEvent motionEvent) {
    if (!touchScrollEnabled) {
//      Log.i("onTouchEvent","onTouchEvent return on !mScrollEnable this:"+this);
      return false;
    }
    //    Log.i("onTouchEvent","onTouchEvent x : "+motionEvent.getX()+",y:"+motionEvent.getY()+",this:"+this+",return b:"+b);
    return super.onTouchEvent(motionEvent);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
    if (!touchScrollEnabled) {
//      Log.i("onInterceptTouchEvent","onTouchEvent return on !mScrollEnable this："+this);
      return false;
    }
    return super.onInterceptTouchEvent(motionEvent);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return super.dispatchTouchEvent(ev);
  }

  public class MyGridLayoutManager extends GridLayoutManager implements LayoutManagerCompat {
    LayoutManagerExecutor mExecutor;
    long lastDownTime = 0;

    @Override
    public LayoutManagerExecutor getExecutor() {
      return mExecutor;
    }

    @Override
    public void setNoScrollOnFirstScreen(boolean b) {
      mExecutor.setNoScrollOnFirstScreen(b);
    }

    @Override
    public void setSearchFocusInItem(boolean b) {
      mExecutor.setSearchFocusInItem(b);
    }

    @Override
    public void setFocusEventListener(FocusEventListener eventListener) {
      mExecutor.setFocusEventListener(eventListener);
    }

    @Override
    public View findFirstFocusByDirection(int direction) {
      if (direction == View.FOCUS_DOWN) {
        return findViewByPosition(mExecutor.getFirstFocusChildPosition(direction));
      }
      return null;
    }

    @Override
    public int findFirstVisibleChild(boolean completelyVisible) {
      return  completelyVisible ? findFirstCompletelyVisibleItemPosition() :  findFirstVisibleItemPosition();
    }

    @Override
    public void superSoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
      super.smoothScrollToPosition(recyclerView, state, position);
    }

    @Override
    public void superScrollToPosition(int position) {
      super.scrollToPosition(position);
    }

    @Override
    public void superScrollToPositionWithOffset(int position, int offset) {
      super.scrollToPositionWithOffset(position, offset);
    }

    @Override
    public void superStartSmoothScroll(SmoothScroller ss) {
      super.startSmoothScroll(ss);
    }

    @Override
    public void setPendingScroll(int scrollToPosition, int scrollOffset) {
      mExecutor.setPendingScroll(scrollOffset, scrollOffset);
    }

    @Override
    public boolean isGridLayout() {
      return true;
    }

    public MyGridLayoutManager(TVListView tvListView, Context context, int spanCount, int orientation, boolean reverseLayout, FastAdapter adapter) {
      super(context, spanCount, orientation, reverseLayout);
      mExecutor = new LayoutManagerExecutor(tvListView, spanCount, orientation, this, adapter);
    }

    @Override
    public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child, @NonNull Rect rect, boolean immediate, boolean focusedChildVisible) {
      return mExecutor.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible) || super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible);
    }

    @Override
    public void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter) {
      super.onAdapterChanged(oldAdapter, newAdapter);
      mExecutor.onAdapterChanged(oldAdapter, newAdapter);
    }

    @Nullable
    @Override
    public View onInterceptFocusSearch(@NonNull View focused, int direction) {
      final View v = mExecutor.onInterceptFocusSearch(focused, direction);
      return v != null ? v : super.onInterceptFocusSearch(focused, direction);
    }

    ArrayList<View> tempList = new ArrayList<>();

    //Grid模式寻焦处理
    @Override
    public View onInterceptSearchInside(TVListView tvList, int focusPosition, int vector, @NonNull View focused, int direction) {
      View result = null;
      final int itemCount = getItemCount();
      final int childCount = tvList.getChildCount();
      if (focusPosition > -1 && focusPosition < itemCount) {
        View[] targets = new View[childCount];
        for (int i = 0; i < childCount; i++) {
          targets[i] = tvList.getChildAt(i);
        }
        if(TVViewUtil.getVectorByDirection(direction,getOrientation()) == 0) {
          View focusedChild = getFocusedChild();
          if (focusedChild != null) {
            Utils.findSameRowChildrenInGridLayout(targets, focusedChild, tempList, 2, getOrientation() == HORIZONTAL);
            if (!tempList.isEmpty()) {
              targets = new View[tempList.size()];
              for (int i = 0; i < tempList.size(); i++) {
                targets[i] = tempList.get(i);
//                Log.i("ZHAOPENG","findSameRowChildrenInGridLayout targets:" + ExtendUtil.debugViewLite(targets[i]) );
              }
              tempList.clear();
            } else {
//              targets = new View[0];
            }
          }
        }

        result = tvList.searchInTargets(targets, focused, direction);
        if (LogUtils.isDebug()) {
          LogUtils.d(FocusDispatchView.TAG, "onSearchInside result 1:" + result + ",this ID :" + tvList.getId());
        }

        long now = SystemClock.uptimeMillis();
        long diff = now - lastDownTime;
        boolean returnOnToFast = diff < 100;
//        boolean returnOnToFast = false;
        if (!returnOnToFast) {
          lastDownTime = now;
        }
        if (LogUtils.isDebug()) {
          Log.v(FocusDispatchView.TAG, "onInterceptFocusSearch diff " + diff + ",returnOnToFast : " + returnOnToFast);
        }
        if (returnOnToFast && result == null) {
          if (LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch return focused on RecyclerView inside focusPosition:" + focusPosition + ",total:" + itemCount + ",this ID :" + tvList.getId());
          }
          return focused;
        } else if (result == null && getItemCount() > 0) {
          //zhaopeng 20230107,简单的防止按键过快时焦点跳出
          int vectorNormal = FocusUtils.getVectorByDirection(direction, tvList.getOrientation());
          int target = focusPosition + getSpanCount() * vectorNormal;
          if (vectorNormal < 0) {
            if (target > 0) {
              Log.e(FocusDispatchView.TAG, "return on grid target " + target + ",itemCount:" + itemCount);
              return focused;
            }
          } else if (vectorNormal > 0) {
            if (target < getItemCount() - 1) {
              Log.e(FocusDispatchView.TAG, "return on grid target " + target + ",itemCount:" + itemCount);
              return focused;
            }
          }
        }
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch fail return super focusPosition:" + focusPosition + ",this ID :" + tvList.getId());
        }
      }
      return result;
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
      if (LogUtils.isDebug()) {
        LogUtils.e(FocusDispatchView.TAG, "----onFocusSearchFailed start");
      }
      return mExecutor.onFocusSearchFailed(focused, focusDirection, recycler, state);
    }

    @Override
    public void onLayoutCompleted(State state) {
      super.onLayoutCompleted(state);
      mExecutor.onLayoutCompleted(state);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
      mExecutor.onLayoutChildren(recycler, state);
      super.onLayoutChildren(recycler, state);
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, Recycler recycler) {
      super.onDetachedFromWindow(view, recycler);
      mExecutor.onDetachedFromWindow(view, recycler);
    }

    @Override
    public View superFocusSearchFailed(View focused, int focusDirection, Recycler recycler, State state) {
      return super.onFocusSearchFailed(focused, focusDirection, recycler, state);
    }

    @Override
    public LayoutManager getRealLayout() {
      return this;
    }

    @Override
    public int getVectorByDirection(int direction, int spanCount, int pos) {
      return this.getGridVectorByDirection(direction, spanCount, pos);
    }

    @Override
    public void startSmoothScroll(SmoothScroller smoothScroller) {
      mExecutor.startSmoothScroll(smoothScroller);
    }

    private int getGridVectorByDirection(int direction, int spanCount, int pos) {
      int vector = 0;
      boolean hasHeader = this.mExecutor.mAdapter.getItemViewType(0) == FastAdapter.TYPE_HEADER;
      int lastVisibleItemPosition = ((LinearLayoutManager) mExecutor.mLayoutManagerCompat.getRealLayout()).findLastVisibleItemPosition();
      int isHeaderCount = hasHeader ? 0 : 1;//有头部 pos判断是否第一行时不需要-1 没有头部 pos判断是否第一行时需要-1
      if (direction == FOCUS_UP) {
        if (pos <= spanCount - isHeaderCount) {
          vector = -1;
        } else {
          vector = vector - spanCount;
        }
      } else if (direction == FOCUS_DOWN) {
        if (hasHeader && pos == 0) {
          vector = 1;
        } else {
          vector = vector + spanCount;
          if (vector + pos >= lastVisibleItemPosition) {
            vector = 1;
          }
        }
      } else if (direction == FOCUS_LEFT) {
        vector = -1;
      } else if (direction == FOCUS_RIGHT) {
        vector = 1;
      }
      return vector;
    }

    @Override
    public void layoutDecoratedWithMargins(@NonNull View child, int left, int top, int right, int bottom) {
      super.layoutDecoratedWithMargins(child, left, top, right, bottom);
      mExecutor.layoutDecoratedWithMargins(child, left, top, right, bottom);
    }
  }

  /**
   * 对外替代LayoutManager的接口类
   */
  public interface LayoutManagerCompat {
    View superFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state);

    LayoutManager getRealLayout();

    boolean isGridLayout();

    int getVectorByDirection(int direction, int spanCount, int pos);

    LayoutManagerExecutor getExecutor();

    View findViewByPosition(int position);

    View onInterceptSearchInside(TVListView recyclerView, int focusPosition, int vector, @NonNull View focused, int direction);

    int getOrientation();

    void superSoothScrollToPosition(RecyclerView recyclerView, State state, int position);

    void superScrollToPosition(int position);

    void superScrollToPositionWithOffset(int position, int offset);

    void superStartSmoothScroll(SmoothScroller ss);


    void scrollToPositionWithOffset(int yIndex, int offset);

    int getPosition(View child);

    void setPendingScroll(int scrollToPosition, int scrollOffset);

    void startSmoothScroll(SmoothScroller ss);

    void setNoScrollOnFirstScreen(boolean b);

    void setSearchFocusInItem(boolean b);

    void setFocusEventListener(FocusEventListener eventListener);

    View findFirstFocusByDirection(int direction);

    int findFirstVisibleChild(boolean completelyVisible);
  }

  public static class MyLinearLayoutManager extends LinearLayoutManager implements LayoutManagerCompat {


    @Override
    public void superSoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
      super.smoothScrollToPosition(recyclerView, state, position);
    }

    @Override
    public void superScrollToPositionWithOffset(int position, int offset) {
      super.scrollToPositionWithOffset(position, offset);
    }

    @Override
    public void superStartSmoothScroll(SmoothScroller ss) {
      super.startSmoothScroll(ss);
    }

    @Override
    public void superScrollToPosition(int position) {
      super.scrollToPosition(position);
    }

    @Override
    public LayoutManagerExecutor getExecutor() {
      return mExecutor;
    }

    private LayoutManagerExecutor mExecutor;


    @Override
    public View findFirstFocusByDirection(int direction) {
      if (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP) {
        return findViewByPosition(mExecutor.getFirstFocusChildPosition(direction));
      }
      return null;
    }

    @Override
    public int findFirstVisibleChild(boolean completelyVisible) {
      return completelyVisible ? findFirstCompletelyVisibleItemPosition() : findFirstVisibleItemPosition();
    }

    @Override
    public void startSmoothScroll(SmoothScroller smoothScroller) {
      mExecutor.startSmoothScroll(smoothScroller);
    }

    public void setPendingScroll(int pos, int offset) {
      mExecutor.setPendingScroll(pos, offset);
    }

    @Override
    public void setNoScrollOnFirstScreen(boolean b) {
      mExecutor.setNoScrollOnFirstScreen(b);
    }

    @Override
    public void setSearchFocusInItem(boolean b) {
      mExecutor.setSearchFocusInItem(b);
    }

    @Override
    public void setFocusEventListener(FocusEventListener eventListener) {
      mExecutor.setFocusEventListener(eventListener);
    }

    @Override
    public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child, @NonNull Rect rect, boolean immediate, boolean focusedChildVisible) {
      return mExecutor.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible) || super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible);
    }


    @Override
    public void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter) {
      super.onAdapterChanged(oldAdapter, newAdapter);
      mExecutor.onAdapterChanged(oldAdapter, newAdapter);
    }

    @Nullable
    @Override
    public View onInterceptFocusSearch(@NonNull View focused, int direction) {
      final View v = mExecutor.onInterceptFocusSearch(focused, direction);
      return v != null ? v : super.onInterceptFocusSearch(focused, direction);
    }

    long lastDownTime = 0;

    //linear
    @Override
    public View onInterceptSearchInside(TVListView tvList, int focusPosition, int vector, @NonNull View focused, int direction) {
      View result = null;
      int targetPosition = focusPosition + vector;
      final int itemCount = getItemCount();
      if (focusPosition > -1 && focusPosition < itemCount) {
        View current = findViewByPosition(focusPosition);
        View next = findViewByPosition(targetPosition);
//        if (next != null) {
//          View firstChild = getExecutor().boundView.getFirstFocusHelper().findFirstFocusChildByDirection(direction);
//        }
        if (current != null) {
          View[] targets = new View[next == null ? 1 : 2];
          targets[0] = current;
          if (next != null) {
            targets[1] = next;
          }
          result = tvList.searchInTargets(targets, focused, direction);
          if (LogUtils.isDebug()) {
            LogUtils.d(FocusDispatchView.TAG, "onSearchInside result 1:" + result + ",this ID :" + tvList.getId());
          }
        }
        int nextFocusPos = targetPosition;
        if (result == null && tvList.advancedFocusSearchMore > 0) {
          for (int i = 0; i < tvList.advancedFocusSearchMore; i++) {
            nextFocusPos = targetPosition + ((1 + i) * vector);
            if (nextFocusPos > itemCount - 1 || nextFocusPos < 0) {
              break;
            }
            View nextFocus = findViewByPosition(nextFocusPos);
            if (current != null) {
              View[] targets = new View[nextFocus == null ? 1 : 2];
              targets[0] = current;
              if (nextFocus != null) {
                targets[1] = nextFocus;
              }
              result = tvList.searchInTargets(targets, focused, direction);

              if (LogUtils.isDebug()) {
                LogUtils.d(FocusDispatchView.TAG, "onInterceptFocusSearch result from nextFocusPos:" + nextFocusPos + ",current:" + focusPosition + ",result:" + result + ",this ID :" + tvList.getId());
              }
              if (result != null) {
                break;
              }
            }
          }
        }
        long now = SystemClock.uptimeMillis();
        long diff = now - lastDownTime;
        boolean returnOnToFast = diff < 400;
        if (!returnOnToFast) {
          lastDownTime = now;
        }
        if (LogUtils.isDebug()) {
          Log.v(FocusDispatchView.TAG, "onInterceptFocusSearch diff " + diff + ",returnOnToFast : " + returnOnToFast);
        }
        if (returnOnToFast && result == null && nextFocusPos > -1 && nextFocusPos < itemCount - 1) {
          if (LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch return focused on RecyclerView inside focusPosition:" + focusPosition + ",total:" + itemCount + ",this ID :" + tvList.getId());
            LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch return focused 可以通过设置advancedFocusSearchSpan=n 来扩大搜索跨度，或者通过将useAdvancedFocusSearch=false，将高级搜索取消");
          }
          return focused;
        }
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch fail return super focusPosition:" + focusPosition + ",this ID :" + tvList.getId());
        }

      }
      return result;
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
      return mExecutor.onFocusSearchFailed(focused, focusDirection, recycler, state);
    }

    @Override
    public void onLayoutCompleted(State state) {
      super.onLayoutCompleted(state);
      mExecutor.onLayoutCompleted(state);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
      mExecutor.onLayoutChildren(recycler, state);
      super.onLayoutChildren(recycler, state);
    }


    @Override
    public void onDetachedFromWindow(RecyclerView view, Recycler recycler) {
      super.onDetachedFromWindow(view, recycler);
      mExecutor.onDetachedFromWindow(view, recycler);
    }

    @Override
    public View superFocusSearchFailed(View focused, int focusDirection, Recycler recycler, State state) {
      return super.onFocusSearchFailed(focused, focusDirection, recycler, state);
    }


    @Override
    public boolean isGridLayout() {
      return false;
    }


    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
      mExecutor.smoothScrollToPosition(recyclerView, state, position);
    }

    @Override
    public void scrollToPositionWithOffset(int position, int offset) {
      mExecutor.scrollToPositionWithOffset(position, offset);
    }

    public MyLinearLayoutManager(final TVListView mRecyclerView, int orientation, FastAdapter adapter) {
      super(mRecyclerView.getContext(), orientation, false);
      mExecutor = new LayoutManagerExecutor(mRecyclerView, 1, orientation, this, adapter);
    }

    @Override
    public void scrollToPosition(int position) {
      mExecutor.scrollToPosition(position);
    }


    @Override
    public int getVectorByDirection(int direction, int spanCount, int pos) {
      return FocusUtils.getVectorByDirection(direction, getOrientation());
    }


    @Override
    public LayoutManager getRealLayout() {
      return this;
    }

    @Override
    public void layoutDecoratedWithMargins(@NonNull View child, int left, int top, int right, int bottom) {
      super.layoutDecoratedWithMargins(child, left, top, right, bottom);
      mExecutor.layoutDecoratedWithMargins(child, left, top, right, bottom);
    }
  }

  protected void onLayoutComplete(State state) {
    if (getEventDeliverer() != null) {
      HippyMap map = new HippyMap();
      map.pushInt("itemCount", state.getItemCount());
      map.pushString("sid", ExtendUtil.getViewSID(this));
      getEventDeliverer().sendEvent("onLayoutComplete", getHandleEventNodeId(), map);
    }
    if (mOnLayoutListener != null) {
      mOnLayoutListener.onLayoutComplete(this, state);
    }
  }


  protected Animator mShakeEndAnimator;

  protected void exeShakeRecycleView() {

    if (mShakeEndAnimator == null) {
      final Animator shake = AnimationStore.defaultShakeEndAnimator(this, getLayoutManagerCompat().getOrientation());
      mShakeEndAnimator = shake;
      notifyShakeEnd();
      shake.start();
      shake.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          super.onAnimationEnd(animation);
          mShakeEndAnimator = null;
        }
      });
    }
  }

  protected boolean exeAnimRunning = false;

  protected void exeShakeSelf(View view, int direction) {
    if (!exeAnimRunning) {
      exeAnimRunning = true;
      if (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP) {
        ObjectAnimator shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y.getName(), 0, 5f);//抖动幅度0到5
        shakeSelfAnim.setDuration(250);//持续时间
        shakeSelfAnim.setInterpolator(new CycleInterpolator(2));//抖动次数
        shakeSelfAnim.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationCancel(Animator animation) {
            exeAnimRunning = false;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            exeAnimRunning = false;
          }
        });
        shakeSelfAnim.start();//开始动画
      } else {
        ObjectAnimator shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X.getName(), 0, 5f);//抖动幅度0到5
        shakeSelfAnim.setDuration(250);//持续时间
        shakeSelfAnim.setInterpolator(new CycleInterpolator(2));//抖动次数
        shakeSelfAnim.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationCancel(Animator animation) {
            exeAnimRunning = false;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            exeAnimRunning = false;
          }
        });
        shakeSelfAnim.start();//开始动画
      }
    }
  }

  protected void notifyShakeEnd() {

  }

  public static class FocusEventListener {

    public View onFocusSearchFailedAtEnd(@NonNull View focused, int direction) {
      return focused;
    }

    public View onInterceptFocusSearch(@NonNull View focused, int direction) {
      return null;
    }

    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler, RecyclerView.State state) {
      return null;
    }

    public View onInterceptFocusSearchFailed(View focused, int focusDirection, int vector) {
      return null;
    }

  }

  final static class PendingWork {
    public int offset = 0;
    int type = 0;
    int position = -1;

    public PendingWork() {
      this.type = 0;
    }

    @Override
    public String toString() {
      return "PendingWork{" +
        "type=" + type +
        ", position=" + position +
        ", offset=" + offset +
        '}';
    }
  }

  private static final class ReFocus {
    final int oldPosition;
    @Deprecated
    final View oldFocus;
    final View oldChild;
    final String oldName;
    boolean valid = true;

    String oldFocusSid = null;
    boolean inReFocus = false;

    private ReFocus(int oldPosition, View oldFocus, View oldChild, String oldFocusSid, String oldName) {
      this.oldPosition = oldPosition;
      this.oldFocus = oldFocus;
      this.oldChild = oldChild;
      this.oldFocusSid = oldFocusSid;
      this.oldName = oldName;
    }

    @Override
    public String toString() {
      return "ReFocus{" +
        "oldPosition=" + oldPosition +
        ", oldFocus=" + oldFocus +
        ", oldChild=" + oldChild +
        ", valid=" + valid +
        ", oldFocusSID=" + oldFocusSid +
        '}';
    }
  }


  private void stopListenGlobalFocusChange() {
    if (mOnGlobalFocusChangeListener != null) {
      getViewTreeObserver().removeOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    listenGlobalFocusChangeIfNeed();
    if (LogUtils.isDebug()) {
      Log.d(TAG, "TVListAttachEvent ++ onAttachedToWindow : " + hasFocus() + " this :" + this);
    }
//    Log.i(TAG,"onAttachedToWindow");
    if (isDetached) {
      //20220731 zhaopeng 这里如果不手动刷新，在TVList嵌套TVList并且detach后，会因列表没有重新layout而导致焦点寻焦错乱
      onReAttached();
    }
    isAttached = true;
    scaleChildren4Coverflow();
  }

  protected void onReAttached() {
    requestLayoutManual();
  }

  public boolean isAttached() {
    return isAttached;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopListenGlobalFocusChange();
    if (mManagerType == 1 && getLayoutManagerCompat() != null) {
      getLayoutManagerCompat().getExecutor().clearReFocus();
    }
    //FIXME 这里因为整个list都detach了,所以焦点的view一定不存在了，这里需要验证
    mFocusChildPosition = -1;
    isDetached = true;
    isAttached = false;
    if (LogUtils.isDebug()) {
      Log.d(TAG, "TVListAttachEvent -- onDetachedFromWindow : " + hasFocus() + " this :" + this);
    }
  }

  private void listenGlobalFocusChangeIfNeed() {
    stopListenGlobalFocusChange();
    if (forceListenGlobalFocusChange) {
      mOnGlobalFocusChangeListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
          if (LogUtils.isDebug()) {
            Log.d(TAG, "onGlobalFocusChanged hasFocus : " + hasFocus() + " this :" + this);
          }
          if (hasFocus()) {
            if (oldFocus == null) {
              //首次获得焦点
              notifyRecyclerViewFocusChanged(true, false, null, newFocus, false);
            } else {
              //焦点在内部，但上一个view不属于内部
              final boolean isOldFocusDescendantOf = TVViewUtil.isViewDescendantOf(oldFocus, TVListView.this);
              if (!isOldFocusDescendantOf) {
                notifyRecyclerViewFocusChanged(true, false, oldFocus, newFocus, false);
              }
            }
          } else {
            final boolean isNewFocusDescendantOf = TVViewUtil.isViewDescendantOf(newFocus, TVListView.this);
            if (LogUtils.isDebug()) {
              Log.d(TAG, "onGlobalFocusChanged  hasFocus : " + hasFocus() + " isNewFocusDescendantOf : " + isNewFocusDescendantOf);
            }
            if (!isNewFocusDescendantOf) {
              //焦点丢失
              final boolean isOldFocusDescendantOf = TVViewUtil.isViewDescendantOf(oldFocus, TVListView.this);

              if (isOldFocusDescendantOf) {
                notifyRecyclerViewFocusChanged(false, true, oldFocus, newFocus, true);
              }
            }
          }
        }
      };
      getViewTreeObserver().addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
    }
  }

  protected boolean forceListenGlobalFocusChange = true;
  private ViewTreeObserver.OnGlobalFocusChangeListener mOnGlobalFocusChangeListener;

  boolean lastFocusState = false;

  private void notifyRecyclerViewFocusChanged(boolean hasFocus, boolean isOldFocusDescendantOf, View oldFocus, View focused, boolean loseFocus) {
    //
    if (LogUtils.isDebug()) {
      Log.d(TAG, "notifyRecyclerViewFocusChanged lastFocusState != hasFocus:" + (lastFocusState != hasFocus) + " ,enableSelectOnFocus:" + enableSelectOnFocus + ",loseFocus:" + loseFocus + ",isOldFocusDescendantOf:" + isOldFocusDescendantOf);
    }

    if (lastFocusState != hasFocus) {
      onRecyclerViewFocusChanged(hasFocus, focused);
    }
    if (lastFocusState != hasFocus && (enableSelectOnFocus || loseFocus)) {
      final View selectedChild = findSelectedChild();
      if (!hasFocus && isOldFocusDescendantOf) {
        if (selectedChild != null) {
          if (focused != selectedChild) {
            changeSelectState(selectedChild, true);
          }
          if (LogUtils.isDebug()) {
            LogUtils.v("hippyState", "+++setState true child:" + selectedChild);
          }
          callItemStateChangeIfNeed(selectedChild, StateListPresenter.STATE_SELECTED);
        }
      }
      lastFocusState = hasFocus;
    }
  }

  protected void onRecyclerViewFocusChanged(boolean hasFocus, View focused) {
    //
    if (LogUtils.isDebug()) {
      Log.d(TAG, "onRecyclerViewFocusChanged hasFocus : " + hasFocus + " this :" + this);
    }
    if (LogUtils.isDebug()) {
      LogUtils.d(TAG, "onRecyclerViewFocusChanged context:" + getContext());
    }
    if(hasFocus){
      //mFocusChildPosition = -1;
      postInvalidateDelayed(16);
    }
    if (getContext() instanceof HippyInstanceContext) {
      if (hasFocus) {
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onFocusAcquired");
      } else {
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onFocusLost");
      }
    }
  }

  protected void callItemStateChangeIfNeed(View child, int state) {
    if (state == StateListPresenter.STATE_SELECTED) {
      InternalExtendViewUtil.sendEventOnRequestListChildSelect(this, child);
    }
  }


  void handleScrollValue() {
    handleScrollValue(false);
  }

  private int mHandleEventNodeId = -1;

  public void setHandleEventNodeId(int id) {
    this.mHandleEventNodeId = id;
  }

  public int getHandleEventNodeId() {
    return mHandleEventNodeId > -1 ? mHandleEventNodeId : getId();
  }

  protected void onScrollYChange(int scrollY) {
    if (mEnableScrollOffsetEvent) {
      //getOnScrollYOffsetChanged().send(this, generateScrollOffsetEvent(scrollY));
      eventDeliverer.sendEvent(getOnScrollYOffsetChanged(), getHandleEventNodeId(), generateScrollOffsetEvent(scrollY));
    }
  }

  HippyViewEvent mOnScrollYChanged;

  protected HippyViewEvent getOnScrollYOffsetChanged() {
    if (mOnScrollYChanged == null) {
      mOnScrollYChanged = new HippyViewEvent("onScrollOffset");
    }
    return mOnScrollYChanged;
  }

  protected HippyMap generateScrollOffsetEvent(int offsetY) {
    HippyMap event = new HippyMap();
    event.pushDouble("y", offsetY);
    event.pushString("sid", ExtendUtil.getViewSID(this));
    return event;
  }


  @Override
  public void onScrolled(int x, int y) {
    super.onScrolled(x, y);
    mOffsetY += y;
    mOffsetX += x;
    if (LogUtils.isDebug()) {
      LogUtils.w("ScrollLog", "onScrolled:y" + y + ",this:" + this + ",eventID:" + getHandleEventNodeId());
    }
    sendOnScrollEvent();
    handleScrollValue();

    //for(int i = 0; i < )
    if (x != 0 || y != 0) {
      scaleChildren4Coverflow();
    }

  }

  protected float minScale = 0;

  /**
   * 遍历所有children，根据child离参照线(parent的中心）的远近，缩放child的大小
   */
  protected void scaleChildren4Coverflow(){
    if (minScale > 0) {
      boolean isHorizontal =  getOrientation() == HORIZONTAL;
        if(isHorizontal){
          float refLine = getWidth() / 2f;
          for(int i= 0;i < getChildCount(); i ++){
            View child = getChildAt(i);
            //Log.i("DebugCoverFlow","pos: "+pos+",left:"+child.getLeft()+",top:"+child.getTop()+",offsetX:"+mOffsetX+",offsetY:"+mOffsetY);
            int childCenter = child.getLeft() + child.getWidth() / 2;
            float distance = Math.abs(childCenter - refLine);
            float rateOfDistance = 1 - (distance / refLine);
            float childScale = minScale + ((1- minScale) * rateOfDistance);
            if (childScale > 0.99) {
              childScale = 1;
            }
            childScale = Math.max(minScale,childScale);
//            Log.i(TAG,"child index:"+i+",childScale:"+childScale);
            //0.5 0.6 0.7
            //0  0.
            if(child.getLeft() > refLine){
              //在右
              child.setPivotX(0);
            }else{
              //在左
              if(child.getRight() <= refLine){
                child.setPivotX(child.getWidth());
              }else{
                child.setPivotX(child.getWidth() / 2f);
              }
            }
            child.setPivotY(child.getHeight() / 2f);
            child.setScaleX(childScale);
            child.setScaleY(childScale);
          }
        }else{
          float refLine = getHeight() / 2f;
          for(int i= 0;i < getChildCount(); i ++){
            View child = getChildAt(i);
            //Log.i("DebugCoverFlow","pos: "+pos+",left:"+child.getLeft()+",top:"+child.getTop()+",offsetX:"+mOffsetX+",offsetY:"+mOffsetY);
            int childCenter = child.getTop() + child.getHeight() / 2;
            float distance = Math.abs(childCenter - refLine);
            float rateOfDistance = 1 - (distance / refLine);
            float childScale = minScale + ((1- minScale) * rateOfDistance);
            if (childScale > 0.99) {
              childScale = 1;
            }
            childScale = Math.max(minScale,childScale);
            //0.5 0.6 0.7
            //0  0.
            if(child.getTop() > refLine){
              //在右
              child.setPivotY(0);
            }else{
              //在左
              if(child.getBottom() <= refLine){
                child.setPivotY(child.getHeight());
              }else{
                child.setPivotY(child.getHeight() / 2f);
              }
            }
            child.setPivotX(child.getWidth() / 2f);
            child.setScaleX(childScale);
            child.setScaleY(childScale);
          }
        }
    }
  }


  protected long mLastScrollEventTimeStamp = -1;
  protected int mScrollEventThrottle = 400;  // 400ms最多回调一次


  /**
   * wanglei 发送滑动事件
   */
  public void sendScrollEvent(View view, String eventName, HippyMap params) {
    //HippyViewEvent event = new HippyViewEvent(eventName);
    //event.send(view, params != null ? params : "");
    eventDeliverer.sendEvent(eventName, view, params);
  }


  protected void sendOnScrollEvent() {
    if (mScrollEventEnable) {
      long currTime = System.currentTimeMillis();
      if (currTime - mLastScrollEventTimeStamp < mScrollEventThrottle) {
        return;
      }
      onSendScrollEvent();
      mLastScrollEventTimeStamp = currTime;
      //getOnScrollEvent().send(this, generateScrollEvent());
      eventDeliverer.sendEvent(getOnScrollEvent(), getHandleEventNodeId(), generateScrollEvent());
    }
  }

  private HippyViewEvent mOnScrollEvent;

  protected HippyViewEvent getOnScrollEvent() {
    if (mOnScrollEvent == null) {
      mOnScrollEvent = new HippyViewEvent(HippyScrollViewEventHelper.EVENT_TYPE_SCROLL);
    }
    return mOnScrollEvent;
  }

  public void setScrollEventThrottle(int scrollEventThrottle) {
    mScrollEventThrottle = scrollEventThrottle;
  }

  protected void onSendScrollEvent() {

  }

//  int scrollVector = 0;
//  @Override
//  public boolean dispatchKeyEvent(KeyEvent event) {
//    if(event.getAction() == KeyEvent.ACTION_DOWN){
//      if(getMyLayoutManager().orientation == VERTICAL){
//        //纵向
//      }else{
//
//      }
//    }
//    return super.dispatchKeyEvent(event);
//  }

  private int lastOffsetY = 0;
  public int mOffsetY = 0;
  public int mOffsetX = 0;

  void handleScrollValue(boolean force) {
    final int sy = mOffsetY;
    final int delta = sy - lastOffsetY;
    if (LogUtils.isDebug()) {
      LogUtils.d("ScrollLog", "handleScrollValue ,force:" + force + ",offsetY:" + mOffsetY + ",lastY：" + lastOffsetY);
    }
    lastOffsetY = sy;
    int vector = 0;
    if (delta > 0) {
      vector = 1;
    }
    if (delta < 0) {
      vector = -1;
    }
    handleScrollValue(force, vector, sy);
  }

  protected void onTriggerScrollYGreater() {

  }

  protected void onTriggerScrollYLesser() {

  }

  void handleScrollValue(boolean force, int vector, int sy) {

    if (LogUtils.isDebug()) {
      LogUtils.d("ScrollLog", "handleScrollValue vector" + vector + ",force:" + force + ",scrollYGreaterReferenceValue:" + scrollYGreaterReferenceValue);
    }
    if (scrollYGreaterReferenceValue > -1 && vector > 0) {
      if (LogUtils.isDebug()) {
        LogUtils.v("ScrollLog", "scrollYGreaterCheckPoint:" + scrollYGreaterCheckPoint + ",offsetY:" + sy + ",scrollYGreaterReferenceValue:" + scrollYGreaterReferenceValue);
      }
      if (sy >= scrollYGreaterReferenceValue && (force || (scrollYGreaterCheckPoint <= scrollYGreaterReferenceValue))) {
        //第一次触发
        scrollYGreaterCheckPoint = sy;
        scrollYLesserCheckPoint = sy;
        if (LogUtils.isDebug()) {
          LogUtils.e("ScrollLog", "++++onScrollValueGreater sy:" + sy + ",force:" + force + "getHandleEventNodeId():" + getHandleEventNodeId() + ",this:" + this);
        }
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onScrollYGreater", getHandleEventNodeId());
        onTriggerScrollYGreater();
        //new HippyViewEvent("onScrollYGreaterReference").send(this, null);
        eventDeliverer.sendEvent("onScrollYGreaterReference", getHandleEventNodeId(), null);
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.d("ScrollLog", "scrollYGreaterCheckPoint fail  sy:" + sy + ",force:" + force + ",scrollYGreaterCheckPoint:" + scrollYGreaterCheckPoint);
        }
      }
    }

    if (scrollYLesserReferenceValue > -1 && vector < 0) {
      if (LogUtils.isDebug()) {
        LogUtils.v("ScrollLog", "scrollYLesserCheckPoint:" + scrollYLesserCheckPoint + ",offsetY:" + sy + ",scrollYLesserReferenceValue:" + scrollYLesserReferenceValue);
      }
      if (sy <= scrollYLesserReferenceValue && (force || (scrollYLesserCheckPoint >= scrollYLesserReferenceValue))) {
        if (LogUtils.isDebug()) {
          LogUtils.e("ScrollLog", "---onScrollValueLesser sy:" + sy + ",force:" + force + "getHandleEventNodeId:" + getHandleEventNodeId() + ",this：" + this);
        }
        scrollYLesserCheckPoint = sy;
        scrollYGreaterCheckPoint = sy;
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onScrollYLesser", getHandleEventNodeId());
        onTriggerScrollYLesser();
        //new HippyViewEvent("onScrollYLesserReference").send(this, null);
        eventDeliverer.sendEvent("onScrollYLesserReference", getHandleEventNodeId(), null);
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.d("ScrollLog", "scrollYLesserCheckPoint fail  sy:" + sy + ",force:" + force + ",scrollYLesserCheckPoint:" + scrollYLesserCheckPoint);
        }
      }
    }
    if (mManagerType == 1) {
      getLayoutManagerCompat().getExecutor().handleScrollValue(vector, sy);
    }
  }

  public void setCheckScrollOffsetOnStateChanged(boolean b) {
    this.checkScrollOffsetOnStateChanged = b;
  }

  private boolean checkScrollOffsetOnStateChanged = false;

  int lastState = SCROLL_STATE_IDLE;
  public static int DEFAULT_CATEGORY = 0;

  @Override
  public void onScrollStateChanged(int state) {
    super.onScrollStateChanged(state);
    if (checkScrollOffsetOnStateChanged && state == SCROLL_STATE_IDLE) {
      LogUtils.d("ScrollLog", "onScrollState Changed handleScrollValue offsetY:" + mOffsetY + ",state:" + state);
      handleScrollValue(true);
    }
    if (lastState != SCROLL_STATE_IDLE && !postTaskPaused) {
//      batchTask();
      this.resumePostTask();
    }
    lastState = state;
    if (getEventDeliverer() != null) {
      HippyMap stateMap = new HippyMap();
      stateMap.pushInt("oldState", lastState);
      stateMap.pushInt("newState", state);
      HippyMap contentOffset = new HippyMap();
      contentOffset.pushDouble("x", PixelUtil.px2dp(getOffsetX()));
      contentOffset.pushDouble("y", PixelUtil.px2dp(getOffsetY()));
      HippyMap map = new HippyMap();
      map.pushMap("state", stateMap);
      map.pushMap("contentOffset", contentOffset);
      map.pushMap("sid", ExtendUtil.getViewState(this));
      getEventDeliverer().sendEvent("onScrollStateChanged", getHandleEventNodeId(), map);
    }

  }

  private SparseArray<SparseArray<Task>> mTaskHub;

  final static class Task {
    Runnable runnable;
    int delay;
    int key;
    boolean isDone = false;

    public Task(Runnable runnable, int delay, int key) {
      this.runnable = runnable;
      this.delay = delay;
      this.key = key;
    }
  }

  public void clearAllTask() {
    if (LogUtils.isDebug()) {
      Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER clearAllTask called !! mTaskHub size: " + (mTaskHub == null ? 0 : mTaskHub.size()) + ",this:" + this);
    }
    if (mTaskHub != null) {
      for (int i = 0; i < mTaskHub.size(); i++) {
        SparseArray<Task> mTasks = mTaskHub.get(mTaskHub.keyAt(i));
        if (mTasks != null) {
          for (int j = 0; j < mTasks.size(); j++) {
            removeCallbacks(mTasks.valueAt(j).runnable);
            if (LogUtils.isDebug()) {
              Log.e("HippyImage", "HippyImage clearAllTask clearTasks ");
            }
          }
          if (LogUtils.isDebug()) {
            Log.w(FastAdapter.TAG_POST, "SCROLL_POSTER clearAllTask clearTasks category:" + mTaskHub.keyAt(i) + ",size:" + mTasks.size());
          }
          mTasks.clear();
        }
      }
      mTaskHub.clear();
    }
  }

  public void clearTaskByCate(int cate) {
    SparseArray<Task> mTasks = mTaskHub == null ? null : mTaskHub.get(cate);
    if (mTasks != null) {
      for (int i = 0; i < mTasks.size(); i++) {
        int type = mTasks.keyAt(i);
        final Task current = mTasks.get(type);
        if (current != null) {
          if (LogUtils.isDebug()) {
            Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER clearTaskByCate remove last category:" + cate + ",type:" + type);
            Log.e("HippyImage", "HippyImage clearTaskByCate remove last category:" + cate + ",type:" + type);
          }
          removeCallbacks(current.runnable);
          mTasks.remove(type);
        }
      }
    }
  }


  public void clearTask(int cate, int type) {
    SparseArray<Task> mTasks = mTaskHub == null ? null : mTaskHub.get(cate);
    if (mTasks != null) {
      final Task current = mTasks.get(type);
      if (current != null) {
        if (LogUtils.isDebug()) {
          Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER clearTask remove last category:" + cate + ",type:" + type+",this:"+this);
          Log.e("HippyImage", "HippyImage clearTask remove last category:" + cate + ",type:" + type);
        }
        removeCallbacks(current.runnable);
        mTasks.remove(type);
      }
    }
  }

  public void clearTask(int type) {
    this.clearTask(DEFAULT_CATEGORY, type);
  }

  public void postTask(int type, Runnable r, int delay) {
    this.postTask(DEFAULT_CATEGORY, type, r, delay);
  }

  public void postTask(int category, int type, Runnable r, int delay) {
    if (LogUtils.isDebug()) {
      Log.d(FastAdapter.TAG_POST, "SCROLL_POSTER postTask category:" + category + ",type:" + type + ",delay:" + delay+",this:"+this);
    }
    SparseArray<Task> mTasks;
    if (mTaskHub == null) {
      mTaskHub = new SparseArray<>();
      mTasks = new SparseArray<>();
      mTaskHub.put(category, mTasks);
    } else {
      mTasks = mTaskHub.get(category);
    }
    if (mTasks == null) {
      mTasks = new SparseArray<>();
      mTaskHub.put(category, mTasks);
    }

    clearTask(category, type);
    if (!postTaskPaused && (lastState == SCROLL_STATE_IDLE || getScrollState() == SCROLL_STATE_IDLE)) {
      boolean b = postDelayed(r, delay);
      if (LogUtils.isDebug()) {
        boolean att = isAttachedToWindow();
        Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER postTask IDLE_STATE post directly success:" + b + ",this:" + this + ",isAttachedToWindow:" + att + ",type:" + type);
      }
    } else {
      if (postTaskPaused) {
        if (LogUtils.isDebug()) {
          Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER postTask on postTaskPaused put in Queue size:" + mTasks.size()+ ",category:"+category+",this:"+this);
        }
      }
      mTasks.put(type, new Task(r, delay, type));
      if (LogUtils.isDebug()) {
        Log.i(FastAdapter.TAG_POST, "SCROLL_POSTER postTask !SCROLL_STATE_IDLE put in Queue size:" + mTasks.size()+",this:"+this);
      }
    }
  }

  public void batchTask() {
    if (LogUtils.isDebug()) {
      Log.w(FastAdapter.TAG_POST, "SCROLL_POSTER start batchTask ALL mTaskHub size:" + (mTaskHub == null ? 0 : mTaskHub.size())+",this:"+this);
    }
    if (mTaskHub != null) {
      for (int i = 0; i < mTaskHub.size(); i++) {
        batchTask(mTaskHub.keyAt(i));
      }
      mTaskHub.clear();
    }
  }

  public void batchTask(int cate) {
    SparseArray<Task> mTasks = mTaskHub == null ? null : mTaskHub.get(cate);
    if (LogUtils.isDebug() && mTasks != null && mTasks.size() > 0) {
      Log.i(FastAdapter.TAG_POST, "START---------------SCROLL_POSTER exe batchTask category:" + cate + ",batchTask size:" + mTasks.size()+",this:"+this);
    }
    if (mTasks != null && mTasks.size() > 0) {
      for (int i = 0; i < mTasks.size(); i++) {
        Task t = mTasks.valueAt(i);
        //这里16L是为了尽量将任务均摊，避免集中式执行
        postDelayed(t.runnable, t.delay);
        if (LogUtils.isDebug()) {
          Log.e(FastAdapter.TAG_POST, "SCROLL_POSTER exe batchTask category:" + cate + ",batchTask size:" + mTasks.size() + ",type:" + t.key+",this:"+this);
        }
      }
      mTasks.clear();
    }
    if (LogUtils.isDebug()) {
      Log.i(FastAdapter.TAG_POST, "END---------------SCROLL_POSTER exe batchTask category:" + cate);
    }
  }

  @Override
  public void scrollToTop() {
    Log.i(TAG, "scrollToTop called this ID:" + getId()+",this:"+ExtendUtil.debugViewLite(this));
    scrollToPosition(0);
    if (mScrollToTopListener != null) {
      mScrollToTopListener.onScrolled(this, 0, 0);
    }
    resetScrollOffset();
  }

  public boolean isPostTaskPaused() {
    return postTaskPaused;
  }

  public void changePauseTaskState(boolean b){
    Log.i(TAG,"changePauseTaskState b "+b+",current:"+this.postTaskPaused);
    if(b != this.postTaskPaused){
      if(b){
        pausePostTask();
      }else{
        resumePostTask();
      }
    }
    this.postTaskPaused = b;
  }

  public boolean isSkipFocusOnPause() {
    return skipFocusOnPause;
  }

  @Override
  public void smoothScrollToTop() {
    smoothScrollToPosition(0);
    resetScrollOffset();
  }

  Interpolator scrollInterpolator = null;

  private float distanceInfluenceForSnapDuration(float f) {
    f -= 0.5F;
    f *= 0.47123894F;
    return (float)Math.sin((double)f);
  }

  private int computeScrollDuration(int dx, int dy, int vx, int vy) {
    int absDx = Math.abs(dx);
    int absDy = Math.abs(dy);
    boolean horizontal = absDx > absDy;
    int velocity = (int)Math.sqrt((double)(vx * vx + vy * vy));
    int delta = (int)Math.sqrt((double)(dx * dx + dy * dy));
    int containerSize = horizontal ? this.getWidth() : this.getHeight();
    int halfContainerSize = containerSize / 2;
    float distanceRatio = Math.min(1.0F, 1.0F * (float)delta / (float)containerSize);
    float distance = (float)halfContainerSize + (float)halfContainerSize * this.distanceInfluenceForSnapDuration(distanceRatio);
    int duration;
    if (velocity > 0) {
      duration = 4 * Math.round(1000.0F * Math.abs(distance / (float)velocity));
    } else {
      float absDelta = (float)(horizontal ? absDx : absDy);
      duration = (int)((absDelta / (float)containerSize + 1.0F) * 300.0F);
    }
    return (int) Math.min(duration * scrollFactor, 2000);
  }

  float scrollFactor = 1f;
  public void setScrollFactor(float factor){
    this.scrollFactor = factor;
  }

  @Override
  public void smoothScrollBy(int dx, int dy) {
    if (scrollByMethod != null && scrollFactor != 1) {
      try {
        if(scrollInterpolator == null){
          scrollInterpolator = t -> {
            --t;
            return t * t * t * t * t + 1.0F;
          };
        }
//        Log.i(TAG,"smoothScrollBy reflect dx:"+dx+",dy:"+dy+",longClick :"+(mFlinger == null ? "null" : mFlinger.isOnLongKeyPressed()));
        scrollByMethod.invoke(viewFlinger,dx,dy,computeScrollDuration(dx,dy,0,0),scrollInterpolator);
      } catch (Exception e) {
        if(LogUtils.isDebug()) {
          e.printStackTrace();
        }
        super.smoothScrollBy(dx, dy);
      }
    }else{
      super.smoothScrollBy(dx, dy);
    }
  }

  protected void resetScrollOffset() {
    LogUtils.w("ScrollLog", "resetScrollY");
    int dy = mOffsetY * -1;
    int dx = mOffsetX * -1;
    mOffsetY = 0;
    mOffsetX = 0;
    //fixme    mOffsetX = 0; x方向暂未处理
    scrollYGreaterCheckPoint = -1;
    scrollYLesserCheckPoint = -1;
    if (myOnScrollListener != null) {
      myOnScrollListener.onScrolled(this, dx, dy);
    }

  }

  @Override
  public void scrollToPosition(int position) {
    super.scrollToPosition(position);
    if (LogUtils.isDebug()) {
      LogUtils.e("ScrollLog", "scrollToPosition called :position" + position);
    }
    getLayoutManagerCompat().getExecutor().clearReFocus();
    if (position == 0) {
      mOffsetY = 0;
      mOffsetX = 0;
      handleScrollValue(true);
      resetScrollOffset();
    }
  }

  @Override
  public void smoothScrollToPosition(int position) {
    super.smoothScrollToPosition(position);
    if (LogUtils.isDebug()) {
      LogUtils.e("ScrollLog", "smoothScrollToPosition called :position" + position);
    }
    if (position == 0) {
      mOffsetY = 0;
      mOffsetX = 0;
      handleScrollValue(true);
      resetScrollOffset();
    }
  }

  int scrollYGreaterCheckPoint = 0;
  int scrollYLesserCheckPoint = 0;
  protected boolean mScrollEventEnable = true;

  private boolean mEnableScrollOffsetEvent = false;

  public void setEnableScrollOffsetEvent(boolean mEnableScrollOffsetEvent) {
    this.mEnableScrollOffsetEvent = mEnableScrollOffsetEvent;
  }

  public void setOnScrollEventEnable(boolean enable) {
    mScrollEventEnable = enable;
  }

  int scrollYLesserReferenceValue = -1;
  int scrollYGreaterReferenceValue = -1;

  public void setScrollYLesserReferenceValue(int scrollYLesserReferenceValue) {
    this.scrollYLesserReferenceValue = scrollYLesserReferenceValue;
  }

  public void setScrollYGreaterReferenceValue(int scrollYGreaterReferenceValue) {
    this.scrollYGreaterReferenceValue = scrollYGreaterReferenceValue;
  }

  public void setRequestChildOnScreenType(int requestChildOnScreenType) {
    getLayoutExecutor().getChildOnScreenScroller().setType(requestChildOnScreenType);
  }

  protected LayoutManagerExecutor getLayoutExecutor() {
    return getLayoutManagerCompat().getExecutor();
  }

  public void setRequestChildOnScreenClampBackward(int clampBackward) {
    getLayoutExecutor().getChildOnScreenScroller().setClampBackward(clampBackward);
  }

  public void setRequestChildOnScreenClampForward(int clampForward) {
    getLayoutExecutor().getChildOnScreenScroller().setClampForward(clampForward);
  }

  public void setScrollThresholdVertical(int threshold) {
    getLayoutExecutor().getChildOnScreenScroller().setScrollThresholdVertical(threshold);
  }

  public void setScrollThresholdHorizontal(int threshold) {
    getLayoutExecutor().getChildOnScreenScroller().setScrollThresholdHorizontal(threshold);
  }

  protected HippyMap generateScrollEvent() {
    float value;
    HippyMap contentOffset = new HippyMap();
    if (mManagerType == 1 && getLayoutManagerCompat() != null && getLayoutManagerCompat().getRealLayout().canScrollHorizontally()) {
      value = (mOffsetX);
      contentOffset.pushDouble("x", value);
      contentOffset.pushDouble("y", 0.0f);
    } else {
      value = (mOffsetY);
      contentOffset.pushDouble("x", 0.0f);
      contentOffset.pushDouble("y", value);
    }
    HippyMap event = new HippyMap();
    event.pushMap("contentOffset", contentOffset);
    return event;
  }

  private int layoutTriggerTargetPosition = -1;

  public void setLayoutTriggerTargetPosition(int position) {
    this.layoutTriggerTargetPosition = position;
  }

  public interface PostContentHolder extends PostTaskHolder {

  }

  /**
   * zhaopeng add
   */
  private boolean isPageHidden = false;

  protected void onPageHiddenChanged(boolean hidden) {
    this.isPageHidden = hidden;
    if (hidden) {
      stopListenGlobalFocusChange();
    } else {
      listenGlobalFocusChangeIfNeed();
    }
  }

  @Override
  public boolean isPageHidden() {
    return isPageHidden;
  }

  @Override
  public void changePageHidden(boolean hidden) {
    onPageHiddenChanged(hidden);
    for (int i = 0; i < getChildCount(); i++) {
      final View v = getChildAt(i);
      if (v instanceof ExtendViewGroup) {
        ((ExtendViewGroup) v).changePageHidden(hidden);
      }
    }
  }

  /**
   * zhaopeng add
   */

  public int[] getBlockFocusOnFail() {
    return mBlockFocusOnFail;
  }

  public void setListShakeSelf(boolean shakeSelf) {
    this.listShakeSelf = shakeSelf;
  }

  @Override
  public void onRequestAutofocus(View child, View target, int type) {
    if(this.getVisibility() != View.VISIBLE){
      Log.e(AutoFocusManager.TAG,"onRequestAutofocus return on parent visibility != View.VISIBLE,"+ExtendUtil.debugView(this)+",target:"+ExtendUtil.debugView(target));
      return;
    }
//    if(getWidth() < 1 || getHeight() < 1){
//      Log.e(AutoFocusManager.TAG,"onRequestAutofocus return on parent size invalid "+ExtendUtil.debugView(this)+",target:"+ExtendUtil.debugView(target));
//      return;
//    }
    if (getParent() instanceof ExtendViewGroup) {
      ExtendViewGroup parent = (ExtendViewGroup) getParent();
      parent.onRequestAutofocus(this, target,type);
    } else {
      Log.i(AutoFocusManager.TAG, "onRequestAutofocus parent is not a instance of ExtendViewGroup parent: " + getParent());
      //AutoFocusManager.globalRequestFocus(target);
      final AutoFocusManager af = AutoFocusManager.findAutoFocusManagerFromRoot(this);
      if (af != null) {
        af.requestGlobalRequestFocus(this,target,type);
      }else{
        Log.e(AutoFocusManager.TAG, "onRequestAutofocus requestFocus on PageRoot af is null");
      }
    }
  }

  public FocusSearchHelper getFirstFocusHelper() {
    return mFocusSearchHelper;
  }

  @Override
  public void setDescendantFocusability(int focusability) {
    final int last = getDescendantFocusability();
    super.setDescendantFocusability(focusability);
    if(LogUtils.isDebug()) {
      if(focusability == FOCUS_BLOCK_DESCENDANTS) {
        Log.e(FocusDispatchView.TAG, "block Focus , this:" + ExtendUtil.debugViewLite(this));
      }else if(last == FOCUS_BLOCK_DESCENDANTS){
        Log.i(FocusDispatchView.TAG, "release block , this:" + ExtendUtil.debugViewLite(this));
      }
    }
  }

  protected boolean isLayoutAnimationAutoPlay = false;

  public void startCustomLayoutAnimation(int type,int duration,int interpolator, float delay,boolean autoPlay){
    if (LogUtils.isDebug()) {
      Log.i(TAG, "startCustomLayoutAnimation type:" + type + ",duration:" + duration + ",interpolator:" + interpolator + ",delay:" + delay);
    }
    Animation animation = null;
    int anim = -1;
    switch (type) {
      case HippyViewGroupController.FADE_IN:
        anim = R.anim.fade_in;
        break;
      case HippyViewGroupController.FADE_OUT:
        anim = R.anim.fade_out;
        break;
      case HippyViewGroupController.LEFT_IN:
        anim = R.anim.left_in;
        break;
      case HippyViewGroupController.LEFT_OUT:
        anim = R.anim.left_out;
        break;
      case HippyViewGroupController.RIGHT_IN:
        anim = R.anim.right_in;
        break;
      case HippyViewGroupController.RIGHT_OUT:
        anim = R.anim.right_out;
        break;
      case HippyViewGroupController.TOP_IN:
        anim = R.anim.top_in;
        break;
      case HippyViewGroupController.TOP_OUT:
        anim = R.anim.top_out;
        break;
      case HippyViewGroupController.BOTTOM_IN:
        anim = R.anim.bottom_in;
        break;
      case HippyViewGroupController.BOTTOM_OUT:
        anim = R.anim.bottom_out;
        break;
      case FastListViewController.LEFT_IN:
        anim =  R.anim.fast_list_layout_fly_left_in;
        break;
      case FastListViewController.RIGHT_IN:
        anim = R.anim.fast_list_layout_fly_right_in;
        break;
      case FastListViewController.TOP_IN:
        anim =  R.anim.fast_list_layout_fly_top_in;
        break;
      case FastListViewController.BOTTOM_IN:
        anim = R.anim.fast_list_layout_fly_bottom_in;
        break;
    }
    if(anim != -1){
      animation = AnimationUtils.loadAnimation(getContext(), anim);
    }
    if(animation != null) {
      if(interpolator == HippyViewGroupController.INTERPOLATOR_FAST_OUT_LINEAR) {
        animation.setInterpolator(new FastOutLinearInInterpolator());
      }else if(interpolator == HippyViewGroupController.INTERPOLATOR_OUT_SLOW_LINEAR){
        animation.setInterpolator(new LinearOutSlowInInterpolator());
      }else if(interpolator == 3){
        animation.setInterpolator(new FastOutSlowInInterpolator());
      }else if(interpolator == 4) {
        animation.setInterpolator(new DecelerateInterpolator());
      }else if(interpolator == 5){
        animation.setInterpolator(new AccelerateInterpolator());
      }
      animation.setDuration(duration);
      LayoutAnimationController controller = new LayoutAnimationController(animation,delay);
      setLayoutAnimation(controller);
      isLayoutAnimationAutoPlay = autoPlay;
      if (!autoPlay) {
        scheduleLayoutAnimation();
        requestLayoutManual();
        postInvalidate();
      }
    }else{
      Log.e(TAG,"startCustomLayoutAnimation animation is null type:"+type+",duration:"+duration+",interpolator:"+interpolator+",delay:"+delay);
    }
  }


  RefocusType mRefocusTypeAfterDataChanged = RefocusType.KEEP_POSITION;

  public void setRefocusType(RefocusType type) {
    mRefocusTypeAfterDataChanged = type;
  }
  public RefocusType getRefocusType() {
    return mRefocusTypeAfterDataChanged;
  }

  public enum RefocusType{
    NONE,
    AT_START,
    LEGACY,
    FIRST_VISIBLE,
    KEEP_POSITION,
    KEEP_SID,
  }
}
