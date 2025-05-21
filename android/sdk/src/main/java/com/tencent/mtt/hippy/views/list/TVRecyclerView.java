package com.tencent.mtt.hippy.views.list;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.CycleInterpolator;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.quicktvui.base.ui.AnimationStore;
import com.quicktvui.hippyext.AutoFocusManager;
import com.quicktvui.base.ui.ITVView;
import com.quicktvui.base.ui.StateListPresenter;
import com.quicktvui.base.ui.TVViewUtil;
import com.quicktvui.base.ui.TriggerTaskHost;
import com.quicktvui.hippyext.TriggerTaskManagerModule;
import com.quicktvui.hippyext.views.fastlist.ListViewControlProp;
import com.quicktvui.hippyext.views.fastlist.Utils;
import com.quicktvui.base.ui.FocusDispatchView;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.quicktvui.base.ui.ExtendViewGroup;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.InternalExtendViewUtil;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.uimanager.ViewStateProvider;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.supportui.views.recyclerview.BaseLayoutManager;
import com.tencent.mtt.supportui.views.recyclerview.LinearLayoutManager;
import com.tencent.mtt.supportui.views.recyclerview.LinearSmoothScroller;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewBase;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewItem;

import java.util.ArrayList;


public class TVRecyclerView extends HippyListView implements ExtendViewGroup, TriggerTaskHost, RecycleViewFlinger.IRecyclerView, TVSingleLineListView, ViewStateProvider {

  protected SingleLineLayoutManager singleLineLayoutManager;

  protected int orientation = HORIZONTAL;

  FocusEventListener mFocusListener;

  ChildOnScreenScroller mCustomChildOnScreenScroller;

  protected int defaultSectionPosition = -1;
  protected int mTargetFocusChildPosition = -1;

  protected int activatedPosition = -1;

  protected boolean disableFocusIntercept = false;

  protected int mFocusChildPosition = 0;

  public final static String TAG = "DebugHippyList";


  private Rect clipOutset;

  private boolean enableFocusMemory = true;

  private int[] mBlockFocusOnFail;
  private int[] mBlockFocus;

  private int initFocusPositionAfterLayout = -1;
  public static final int REQUEST_CHILD_ON_SCREEN_TYPE_CENTER = 0;
  public static final int REQUEST_CHILD_ON_SCREEN_TYPE_NONE = 2;
  public static final int REQUEST_CHILD_ON_SCREEN_TYPE_ANDROID = 1;


  private boolean enableSelectOnFocus = true;

  protected boolean mShakeEndEnable = true;

  boolean useAdvancedFocusSearch = true;

  private boolean isUseNegativeLayout = false;

  private ItemDecoration blankItemDecoration;


  protected Animator mShakeEndAnimator;

  private InitParams mInitParams;
  private boolean forceBlockFocusOnFail = false;

  private View currentFocusView;

  public void setForceBlockFocusOnFail(boolean forceBlockFocusOnFail) {
    this.forceBlockFocusOnFail = forceBlockFocusOnFail;
  }


  public void setEnableSelectOnFocus(boolean enableSelectOnFocus) {
    this.enableSelectOnFocus = enableSelectOnFocus;
  }

  public void setUseAdvancedFocusSearch(boolean useAdvancedFocusSearch) {
    this.useAdvancedFocusSearch = useAdvancedFocusSearch;
  }

  public void setUseNegativeLayout(boolean useNegativeLayout) {
    isUseNegativeLayout = useNegativeLayout;
  }

  private boolean mListenFocusSearchOnFail = false;
  private boolean mListenBoundEvent = false;

  public void setListenBoundEvent(boolean mListenBoundEvent) {
    this.mListenBoundEvent = mListenBoundEvent;
  }

  public boolean isListenBoundEvent() {
    return mListenBoundEvent;
  }

  public void setListenFocusSearchOnFail(boolean mListenFocusSearchOnFail) {
    this.mListenFocusSearchOnFail = mListenFocusSearchOnFail;
  }


  public void setShakeEndEnable(boolean shakeEndEnable) {
    this.mShakeEndEnable = shakeEndEnable;
  }

  public void setBlockFocusOnFail(int[] directions) {
    this.mBlockFocusOnFail = directions;
  }

  @Override
  public void diffSetScrollToPosition(int position, int offset) {

  }

  public void setBlockFocusOn(int[] directions) {
    this.mBlockFocus = directions;
  }

  public void setRequestChildOnScreenType(int requestChildOnScreenType) {
    if (getSingleLineLayoutManager().childOnScreenScroller != null) {
      getSingleLineLayoutManager().childOnScreenScroller.setType(requestChildOnScreenType);
    }
  }

  public void setRequestChildOnScreenClampBackward(int clampBackward) {
    if (getSingleLineLayoutManager().childOnScreenScroller != null) {
      getSingleLineLayoutManager().childOnScreenScroller.setClampBackward(clampBackward);
    }
  }

  public void setRequestChildOnScreenClampForward(int clampForward) {
    if (getSingleLineLayoutManager().childOnScreenScroller != null) {
      getSingleLineLayoutManager().childOnScreenScroller.setClampForward(clampForward);
    }
  }

  public void setScrollThresholdVertical(int threshold) {
    if (getSingleLineLayoutManager().childOnScreenScroller != null) {
      getSingleLineLayoutManager().childOnScreenScroller.setScrollThresholdVertical(threshold);
    }
  }

  public void setScrollThresholdHorizontal(int threshold) {
    if (getSingleLineLayoutManager().childOnScreenScroller != null) {
      getSingleLineLayoutManager().childOnScreenScroller.setScrollThresholdHorizontal(threshold);
    }
  }

  public void setBlankItemDecoration(HippyArray array) {
    if (blankItemDecoration != null) {
      removeItemDecoration(blankItemDecoration);
    }
    final int head = array.getInt(0);
    final int end = array.getInt(1);
    if (LogUtils.isDebug()) {
      LogUtils.d(TAG, "setBlankItemDecoration vertical head:" + head + ",end:" + end);
    }
    blankItemDecoration = new ItemDecoration() {
      @Override
      public void getItemOffsets(Rect outRect, int itemPosition, RecyclerViewBase parent) {
        super.getItemOffsets(outRect, itemPosition, parent);
        if (getOrientation() == VERTICAL) {
          if (itemPosition == 0) {
            outRect.top = head;
          } else if (itemPosition == parent.getLayoutManager().getItemCount() - 1) {
            outRect.bottom = end;
          }
        } else {
          if (itemPosition == 0) {
            outRect.left = head;
          } else if (itemPosition == parent.getLayoutManager().getItemCount() - 1) {
            outRect.right = end;
          }
        }
      }
    };
    addItemDecoration(blankItemDecoration);
  }

  @Override
  public void getState(@NonNull HippyMap map) {
    map.pushInt(Utils.ORIENTATION, getOrientation());
    map.pushInt(Utils.ITEMCOUNT, getAdapter() != null ? getAdapter().getItemCount() : 0);
    map.pushInt(Utils.FOCUS_POSITION, getFocusedChild() != null ? getChildPosition(getFocusedChild()) : -1);
    map.pushInt(Utils.SELECT_POSITION, getSelectChildPosition());
    map.pushInt(Utils.OFFSETX, getOffsetX());
    map.pushInt(Utils.OFFSETY, getOffsetY());
    map.pushInt(Utils.SCROLLSTATE, getScrollState());
  }

  public interface OnLayoutManagerCallback {
    void onLayoutCompleted(State state);
  }

  public void setInitFocusPositionAfterLayout(int requestFocusPositionAfterLayout) {
    this.initFocusPositionAfterLayout = requestFocusPositionAfterLayout;
  }

  public void enableFocusMemory(boolean enableFocusMemory) {
    this.enableFocusMemory = enableFocusMemory;
  }


  @Override
  public void setListData() {

    //zhaopeng 防止列表刷新过多的item
    if (isUseNegativeLayout) {
      final HippyEngineContext context = getHippyContext();
      if (context != null) {
        final RenderNode node = context.getRenderManager().getRenderNode(getId());
        if (node != null) {
          final int totalCount = node.getChildCount();
          final int lastItemPosition = getSingleLineLayoutManager().findLastCompletelyVisibleItemPosition();

          if (totalCount > 0 && lastItemPosition > -1 && lastItemPosition < totalCount - 1) {
            final int diff = totalCount - lastItemPosition;
            getListAdapter().notifyItemRangeChanged(lastItemPosition + 1, diff);
            dispatchLayout();
            Log.e(TAG, "setListData : notifyItemRangeChanged ->>>totalCount :" + totalCount + ",lastItemPosition:" + lastItemPosition + ",diff:" + diff);
            return;
          }
        }
      }
    }
    if (LogUtils.isDebug()) {
      Log.e("PendingFocus", "###########setListData########## pendingWork:" + mInitParams + ",id:" + getId());
    }
    super.setListData();
    if (mInitParams != null) {
      if (LogUtils.isDebug()) {
        Log.d("PendingFocus", "after setListData  scrollToPosition:" + mInitParams + ",id:" + getId());
      }
      scrollToPosition(mInitParams.scrollToPosition, mInitParams.scrollOffset);
//      RenderUtil.requestNodeLayout(this);
    }
  }


  @Override
  protected int getScrollToPosition() {
    return mInitParams == null ? -1 : mInitParams.scrollToPosition;
  }

  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    super.onWindowFocusChanged(hasWindowFocus);
    if (LogUtils.isDebug()) {
      Log.d("SingleLineRecyclerView", "onWindowVisibilityChanged onWindowFocusChanged:" + hasWindowFocus);
    }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (LogUtils.isDebug()) {
      Log.d("SingleLineRecyclerView", "onWindowVisibilityChanged visibility:" + visibility);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (LogUtils.isDebug()) {
      Log.d("SingleLineRecyclerView", "onAttachedToWindow");
    }
    listenGlobalFocusChangeIfNeed();
  }


  Rect temp;

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (clipOutset != null) {
        if (temp == null) {
          temp = new Rect();
        }
        temp.set(-clipOutset.left, -clipOutset.top, getWidth() + clipOutset.right, getHeight() + clipOutset.bottom);
        setClipBounds(temp);
      }
    }
  }


  private View[] mFocusSearchTargets;


  public void setClipOutset(int left, int top, int right, int bottom) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (clipOutset == null) {
        clipOutset = new Rect();
      }
      clipOutset.set(left, top, right, bottom);
      requestLayout();
      setClipChildren(false);
    }
  }

  public View findSelectedChild() {
    final int select = defaultSectionPosition;
    if (select > -1) {
      return getSingleLineLayoutManager().findViewByPosition(select);
    }
    return null;
  }


  public void setActivatedPosition(int activatedPosition) {
//        final int last = activatedPosition;
    this.activatedPosition = activatedPosition;

    if (getChildCount() > 0) {


      for (int i = 0; i < getChildCount(); i++) {
        final View child = singleLineLayoutManager.getChildAt(i);
        if (child != null) {
          callItemStateChangeIfNeed(child, child.isFocused() ? StateListPresenter.STATE_FOCUSED : StateListPresenter.STATE_NORMAL);
        }
      }
      if (activatedPosition > -1) {
        final View newOne = singleLineLayoutManager.findViewByPosition(activatedPosition);
        if (newOne != null) {
          callItemStateChangeIfNeed(newOne, StateListPresenter.STATE_ACTIVATED);
        }
      }
    }
  }

  public void setTargetFocusChildPosition(int mTargetFocusChildPosition) {
    this.mTargetFocusChildPosition = mTargetFocusChildPosition;
  }

  OnLayoutManagerCallback onLayoutManagerCallback;


  public void setOnLayoutManagerCallback(OnLayoutManagerCallback onLayoutManagerCallback) {
    this.onLayoutManagerCallback = onLayoutManagerCallback;
  }


  public void setChildOnScreenScroller(@Nullable ChildOnScreenScroller scroller) {
    this.mCustomChildOnScreenScroller = scroller;
  }

  private int mScrollOffset = 0;

  public TVRecyclerView(Context context, int orientation) {
    super(context, orientation);
    this.orientation = orientation;
  }

  public TVRecyclerView(Context context) {
    this(context, BaseLayoutManager.VERTICAL);
  }


  public int getOrientation() {
    return orientation;
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
    final int focusedChildPosition = getChildPosition(child);

    changeSelectState(defaultSectionPosition, false);
    if (enableSelectOnFocus) {
      defaultSectionPosition = focusedChildPosition;
    } else {
      changeSelectState(defaultSectionPosition, true);
    }
    mTargetFocusChildPosition = focusedChildPosition;
    mFocusChildPosition = focusedChildPosition;
    currentFocusView = focused;
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

  private boolean lockFocusOnLayout = true;

  public void setLockFocusOnLayout(boolean lockFocusOnLayout) {
    this.lockFocusOnLayout = lockFocusOnLayout;
  }

  public void disableFocusIntercept(boolean disable) {
    this.disableFocusIntercept = disable;
  }

  public void applyChildOnScreenScroller() {
  }

  public int getSelectChildPosition() {
    return defaultSectionPosition;
  }

  protected void callItemStateChangeIfNeed(View child, int state) {
    if (state == StateListPresenter.STATE_SELECTED) {
      InternalExtendViewUtil.sendEventOnRequestListChildSelect(this, child);
    }

  }


  protected void exeShakeRecycleView() {

    LogUtils.v(TAG, "exeShakeRecycleView orientation is " + orientation);

    if (mShakeEndAnimator == null) {
      final Animator shake = AnimationStore.defaultShakeEndAnimator(this, getOrientation());
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
      ObjectAnimator shakeSelfAnim;//抖动幅度0到5
      if (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP) {
        shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y.getName(), 0, 5f);
      } else {
        shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X.getName(), 0, 5f);
      }
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


  protected void notifyShakeEnd() {

  }


  /**
   * 设置选中的子view,注意此方法只在view已经显示出来以后调用才有效。
   *
   * @param position
   */
  public void setSelectChildPosition(int position) {
    this.setSelectChildPosition(position, true);
  }

  public void setupInitScrollWork(int position, int scrollToPosition, int offset, String focusName, boolean block, int delay) {
    if (position < 0 && scrollToPosition < 0) {
      clearInitFocusPosition();
    } else {
      mInitParams = new InitParams(position, scrollToPosition, offset, focusName, block, delay);
      if (block) {
        Log.e("PendingFocus", "setPendingFocusChild blockRootFocus!!");
        InternalExtendViewUtil.blockRootFocus(this);
      }
    }

  }

  public void clearInitFocusPosition() {
    if (mInitParams != null) {
      mInitParams.targetFocusPosition = -1;
    }
  }

  public void requestTargetFocus(boolean block, int delay, int position, String target) {
    if (block) {
      InternalExtendViewUtil.blockRootFocus(this);
    }
    if (delay > 0) {
      getRootView().postDelayed(() -> {
        if (LogUtils.isDebug()) {
          Log.e("PendingFocus", "setInitPositionInfo found focus requestFocusDirectly!!");
        }
        if (LogUtils.isDebug()) {
          LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 1");
        }
        InternalExtendViewUtil.unBlockRootFocus(TVRecyclerView.this);
        if (getSingleLineLayoutManager().requestTargetChildFocus(position, target)) {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "clear mPendingFocus");
          }
          //this.clearInitFocusPosition();
        }
      }, delay);
    } else {
      if (LogUtils.isDebug()) {
        LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 2");
      }
      InternalExtendViewUtil.unBlockRootFocus(this);
      if (LogUtils.isDebug()) {
        Log.e("PendingFocus", "setInitPositionInfo found focus requestFocusDirectly!!");
      }
      if (getSingleLineLayoutManager().requestTargetChildFocus(position, target)) {
        if (LogUtils.isDebug()) {
          Log.e("PendingFocus", "clear mPendingFocus");
        }
        //this.clearInitFocusPosition();
      }
    }
  }


  public void setInitPositionInfo(HippyMap map) {
    /*
     :initPosition=
     {
     // 请求焦点的位置可以不传，不传不会请求焦点
     focusPosition:5,
     //首次滚动的位置, 可以不传
     scrollToPosition:3,
     //首次滚动的偏移值, 可以不传
     scrollOffset:200,
     }
     *
     **/

    if (map == null || map.size() < 1) {
      clearInitFocusPosition();
      return;
    }
    int position = map.containsKey("focusPosition") ? map.getInt("focusPosition") : -1;
    if (position < 0) {
      position = map.containsKey("position") ? map.getInt("position") : -1;
    }
    final int scrollToPosition = map.containsKey("scrollToPosition") ? map.getInt("scrollToPosition") : -1;
    final int scrollOffset = map.containsKey("scrollOffset") ? map.getInt("scrollOffset") : 0;

    final String target = map.containsKey("target") ? map.getString("target") : null;
//    final boolean oneShot = map.getBoolean("oneShot");
    final boolean block = map.containsKey("blockOthers") && map.getBoolean("blockOthers");
    final boolean force = map.containsKey("force") && map.getBoolean("force");
    final int delay = map.containsKey("delay") ? map.getInt("delay") : -1;
    if (position < 0 && scrollToPosition < 0) {
      this.clearInitFocusPosition();
      return;
    }

    boolean requestFocus = position > -1;
    if (LogUtils.isDebug()) {
      Log.d("PendingFocus", "begin initPosition requestFocus:" + requestFocus);
    }
    if (requestFocus) {
      final View targetView = getSingleLineLayoutManager().findTargetChildFocus(position, target);
      if (LogUtils.isDebug()) {
        Log.d("PendingFocus", "findTargetChildFocus findTargetChildFocus:" + targetView);
      }
      if (targetView != null && targetView.getVisibility() == View.VISIBLE) {
//        requestTargetFocus(block,delay,position,target);
        setupInitScrollWork(position, scrollToPosition, scrollOffset, target, block, delay);
        if (force) {
          hideAWhile(300);
          setListData();
        }
      } else {
        setupInitScrollWork(position, scrollToPosition, scrollOffset, target, block, delay);
        if (force) {
          hideAWhile(300);
          setListData();
        }
        if (scrollToPosition > -1) {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "scrollToPositionWithScrollType Directly");
          }
//          scrollToPositionWithScrollType(scrollToPosition,scrollOffset);
        } else {
//          scrollToPositionWithScrollType(position,scrollOffset);
        }
      }
    } else {
      setupInitScrollWork(-1, scrollToPosition, scrollOffset, target, block, delay);
      if (LogUtils.isDebug()) {
        Log.e("PendingFocus", "scrollToPositionWithScrollType Directly!! scrollToPosition:" + scrollToPosition + ",offset:" + scrollOffset + ",id:" + getId());
      }
      if (force) {
        hideAWhile(300);
        setListData();
      }
//      scrollToPositionWithScrollType(scrollToPosition,scrollOffset);
    }


  }

  private Runnable hideTask;

  private void hideAWhile(int time) {
    //暂时将自己隐藏一会，主要用来解决列表重设数据，多次变更的问题
    if (hideTask != null) {
      removeCallbacks(hideTask);
    }
    setAlpha(0);
    hideTask = () -> setAlpha(1);
    postDelayed(hideTask, time);
  }


  @Override
  protected void scrollToPositionBeforeSetListData() {
    super.scrollToPositionBeforeSetListData();

    int position = getScrollToPosition();
    int itemHeight;
    int offset = 0;
    if (getListAdapter() == null) {
      return;
    }

    itemHeight = getListAdapter().getItemHeight(position);

    int parentSize = getHeight() - getPaddingBottom() - getPaddingTop();

    final int lastIndex = getListAdapter().getItemCount() - 1;
    if (getSingleLineLayoutManager().childOnScreenScroller.type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER) {
      offset = (int) ((parentSize - itemHeight) * 0.5f);
    }
    if (mInitParams != null) {
      offset += mInitParams.scrollOffset;
    }

    Log.i(TAG, "scrollToPositionBeforeSetListData parentSize:" + parentSize + ",itemHeight:" + itemHeight + ",offset:" + offset);
    //设置的offset超过边界，跳转到最后一个Item上去
    if (position >= lastIndex) {
      position = lastIndex;
      //不能划出内容高度
      if (offset < 0) {
        offset = 0;
      }
    }
    if (position > -1) {
      scrollToPosition(position, offset);
    }
  }

  public void scrollToPositionWithScrollType(int position, int offset) {
    Log.e("PendingFocus", "scrollToPositionWithScrollType position:" + position + ",offset:" + offset);
    scrollToPosition(position, offset);
//      dispatchLayout();
  }


  /**
   * 设置选中的子view,注意此方法只在view已经显示出来以后调用才有效。
   *
   * @param position
   */
  public void setTargetChildPosition(int position) {
    this.setSelectChildPosition(position, true);
  }

  /**
   * 设置选中的子view,注意此方法只在view已经显示出来以后调用才有效。
   *
   * @param position
   */
  public void setSelectChildPosition(int position, boolean changeTargetFocusChild) {
    Log.d(TAG, "setSelectChildPosition position:" + position + " ,changeTargetFocusChild:" + changeTargetFocusChild);
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

  private int layoutTriggerTargetPosition = -1;

  public void setLayoutTriggerTargetPosition(int position) {
    this.layoutTriggerTargetPosition = position;
  }


  public void setScrollOffset(int mScrollOffset) {
    this.mScrollOffset = mScrollOffset;
    if (singleLineLayoutManager != null) {
      singleLineLayoutManager.childOnScreenScroller.setScrollOffset(mScrollOffset);
    }
  }


  @Override
  protected LinearLayoutManager onCreateLayoutManager(Context context, int orientation, boolean reverseLayout) {
    singleLineLayoutManager = new SingleLineLayoutManager(this, orientation);
    singleLineLayoutManager.setFocusEventListener(mFocusListener);
    //onAddDefaultItemDecoration();
    return singleLineLayoutManager;
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
    addItemDecoration(new ItemDecorations.ListEndBlank(orientation));
  }

  public SingleLineLayoutManager getSingleLineLayoutManager() {
    return singleLineLayoutManager;
  }


  private ArrayList mTempList;

  @Override
  public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
    LogUtils.d(FocusDispatchView.TAG, "addFocusables start inTVRecyclerView " + ",childCount:" + getChildCount());
    if (mFocusSearchTargets != null) {
      for (View v : mFocusSearchTargets) {
        v.addFocusables(views, direction, focusableMode);
      }
      if (LogUtils.isDebug()) {
        LogUtils.d("SingleLineRecyclerView", "addFocusables In Target:" + mFocusSearchTargets + " views size:" + views.size());
      }
      return;
    }

    if (!hasFocus() && enableFocusMemory) {

      View target = null;
      if (mTargetFocusChildPosition > -1) {
        target = getSingleLineLayoutManager().findViewByPosition(mTargetFocusChildPosition);
      }
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
        LogUtils.d("SingleLineRecyclerView", "addFocusables by super views size:" + views.size());
      }
    }
    if (LogUtils.isDebug()) {
      LogUtils.d(FocusDispatchView.TAG, "addFocusables end inTVRecyclerView " + ",resultCount:" + views.size());
    }
  }


  @Override
  protected void onDetachedFromWindow() {
    if (LogUtils.isDebug()) {
      Log.d("SingleLineRecyclerView", "onDetachedFromWindow");
    }
    super.onDetachedFromWindow();
    stopListenGlobalFocusChange();

  }


  public void setFocusEventListener(FocusEventListener mFocusListener) {
    this.mFocusListener = mFocusListener;
    if (singleLineLayoutManager != null) {
      singleLineLayoutManager.setFocusEventListener(mFocusListener);
    }


  }


  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (singleLineLayoutManager.isInReFocus()) {
//        Log.e("zhaopeng","dispatchDraw inReFocus return!!");
      return;
    }
    super.dispatchDraw(canvas);
  }

  @Override
  public void draw(Canvas c) {
    if (singleLineLayoutManager.isInReFocus()) {
//      Log.e("zhaopeng","draw inReFocus return!!");
      return;
    }
    super.draw(c);
  }

  void requestFocusSearchInChild(View[] targets, View focused, int direction) {
    mFocusSearchTargets = targets;
  }

  void removeSearchInChildRequest() {
    this.mFocusSearchTargets = null;
  }

  View searchInTargets(View[] targets, View focused, int direction) {
    requestFocusSearchInChild(targets, focused, direction);
    View result = null;
    try {
      //这里需要从瀑布流内部寻找，不能只在组件内寻找，否则焦点只能在第一个位置出现
      result = FocusFinder.getInstance().findNextFocus(this, focused, direction);
    } catch (Throwable t) {
      result = null;
    }
    removeSearchInChildRequest();
    return result;
  }

  @Override
  public View focusSearch(View focused, int direction) {
    View v = super.focusSearch(focused, direction);
//    if (v != null) {
//      currentFocusView = v;
//    }
    //wanglei add : ul添加焦点抖动
    if ((focused == v || v == null) && listShakeSelf) {
      singleLineLayoutManager.shakeSelf(focused, direction);
    }
    //修改触底回弹为焦点抖动
    singleLineLayoutManager.shakeEndIfNeed(focused, v, direction);
    return v;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    return super.dispatchKeyEvent(event);
  }

  public static class FocusEventListener {

    public View onFocusSearchFailedAtEnd(@NonNull View focused, int direction) {
      return focused;
    }

    public View onInterceptFocusSearch(@NonNull View focused, int direction) {
      return null;
    }

    public View onFocusSearchFailed(View focused, int focusDirection, Recycler recycler, State state) {
      return null;
    }

  }

  protected View onFocusSearchFailedAtEnd(@NonNull View focused, int direction) {
    return null;
  }


  @Override
  public void smoothScrollBy(int dx, int dy, boolean careSpringBackMaxDistance, boolean forceScroll) {
    super.smoothScrollBy(dx, dy, careSpringBackMaxDistance, forceScroll);
    if(LogUtils.isDebug()) {
      Log.i(AutoFocusManager.TAG, " ul smoothScrollBy dx:" + dx + ",dy:" + dy + ",hasFocus:" + hasFocus() + ",initFocusPositionAfterLayout" + initFocusPositionAfterLayout);
    }

  }

  private class CenterSmoothScroller extends LinearSmoothScroller {

    public CenterSmoothScroller(Context context) {
      super(context);
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
      return getSingleLineLayoutManager().computeScrollVectorForPosition(targetPosition);
    }

    @Override
    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
      return super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference);
    }
  }

  int shakePreCheckNumber = 2;

  public void setShakePreCheckNumber(int shakePreCheckNumber) {
    this.shakePreCheckNumber = shakePreCheckNumber;
  }


  public static class SingleLineLayoutManager extends LinearLayoutManager {

    private final int orientation;

    final ChildOnScreenScroller.Default childOnScreenScroller;
    private FocusEventListener mFocusListener;
    private final TVRecyclerView mRecyclerView;
    private int mCurrentDirection = -1;
    private ReFocus mReFocus;

    @Override
    public boolean requestChildRectangleOnScreen(RecyclerViewBase parent, View child, Rect rect, boolean immediate) {
      final ChildOnScreenScroller scroller = mRecyclerView.mCustomChildOnScreenScroller != null ? mRecyclerView.mCustomChildOnScreenScroller : childOnScreenScroller;
      if (scroller != null && scroller.requestChildRectangleOnScreen(parent, child, rect, immediate, mCurrentDirection)) {
        return true;
      }
      return super.requestChildRectangleOnScreen(parent, child, rect, immediate);
    }


    @Override
    public void smoothScrollToPosition(RecyclerViewBase recyclerView, State state, int position) {


      final ChildOnScreenScroller scroller = mRecyclerView.mCustomChildOnScreenScroller != null ? mRecyclerView.mCustomChildOnScreenScroller : childOnScreenScroller;
      if (scroller != null && scroller.smoothScrollToPosition(recyclerView, state, position)) {
      } else {
        super.smoothScrollToPosition(recyclerView, state, position);
      }
    }


    public SingleLineLayoutManager(final TVRecyclerView mRecyclerView, int orientation) {
      super(mRecyclerView.getContext(), orientation, false);
      this.mRecyclerView = mRecyclerView;
      childOnScreenScroller = new ChildOnScreenScroller.Default(orientation);
      childOnScreenScroller.setScrollOffset(mRecyclerView.mScrollOffset);
      this.orientation = orientation;
    }

    boolean isInReFocus() {
//          return mReFocus != null && mReFocus.inReFocus;
      return false;
    }

    public void setFocusEventListener(FocusEventListener mFocusListener) {
      this.mFocusListener = mFocusListener;
    }


    @Override
    public void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter) {
      super.onAdapterChanged(oldAdapter, newAdapter);
      mRecyclerView.defaultSectionPosition = -1;
      mRecyclerView.resetScrollY();
    }


    @Override
    public void scrollToPosition(int position) {
      super.scrollToPosition(position);
    }


    @Override
    public boolean onRequestChildFocus(RecyclerViewBase parent, View child, View focused) {
      return super.onRequestChildFocus(parent, child, focused);
    }


    private HippyViewEvent mFocusSearchFailEvent;

    private HippyViewEvent getFocusSearchFailEvent() {
      if (mFocusSearchFailEvent == null) {
        mFocusSearchFailEvent = new HippyViewEvent(InternalExtendViewUtil.LIST_FOCUS_SEARCH_FAIL_EVENT_NAME);
      }
      return mFocusSearchFailEvent;
    }

    private void eatReFocus() {
      mReFocus = null;
    }

    @Override
    public void handleFocusBeforeLayout(Recycler recycler, State state, boolean dataChanged) {
      if (LogUtils.isDebug()) {
        LogUtils.d("ReFocus", "+++handleFocusBeforeLayout state:" + state + " ,hasFocus:" + hasFocus() + ",dataChanged: " + dataChanged + ",id:" + mRecyclerView.getId());
      }
      boolean forPendingWork = mRecyclerView.mInitParams != null && mRecyclerView.mInitParams.targetFocusPosition > -1;
      if (forPendingWork) {
        if (LogUtils.isDebug()) {
          LogUtils.d("ReFocus", "+++handleFocusBeforeLayout forPendingWork ignore ReFocus");
        }
        return;
      }
      if (dataChanged) {
        //zhaopeng 如果数据没有改变而记住焦点，会造成scrollToIndex时无法回收导致的绘制残留
        if (mRecyclerView.initFocusPositionAfterLayout > -1) {
          if (LogUtils.isDebug()) {
            LogUtils.d("ReFocus", "handleFocusBeforeLayout initFocusPositionAfterLayout:" + mRecyclerView.initFocusPositionAfterLayout);
          }
          mReFocus = new ReFocus(mRecyclerView.initFocusPositionAfterLayout, null, null);
          mRecyclerView.initFocusPositionAfterLayout = -1;
        } else {
          final View child = mRecyclerView.getFocusedChild();
          if (hasFocus() && child != null) {
            final View focused = child.findFocus();
            final int oldPos = getPosition(mRecyclerView.getFocusedChild());
            InternalExtendViewUtil.blockRootFocus(mRecyclerView);
            mReFocus = new ReFocus(oldPos, focused, child);
            if (LogUtils.isDebug()) {
              LogUtils.d("ReFocus", "+++handleFocusBeforeLayout mReFocus:" + mReFocus);
            }
          } else {
            mReFocus = null;
          }
        }
      }
      if (LogUtils.isDebug()) {
        Log.d(FocusDispatchView.TAG, "+++++handleFocusBeforeLayout mReFocus:" + mReFocus);
      }
    }

    boolean requestTargetChildFocus(int position, String name) {
      final View targetChild = findViewByPosition(position);
      boolean b = false;
      final int itemCount = getItemCount();
      final int lastVisible = findLastVisibleItemPosition();
      if (name == null) {
        if (targetChild instanceof RecyclerViewItem && targetChild.getVisibility() == View.VISIBLE) {
          ((RecyclerViewItem) targetChild).requestContentFocus();
          b = true;
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "requestTargetChildFocus targetChild:" + targetChild + ",pos:" + position + ",itemCount:" + itemCount + ",lastVisible:" + lastVisible + ",id:" + mRecyclerView.getId());
          }
        } else if (targetChild != null && targetChild.getVisibility() == View.VISIBLE) {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "requestTargetChildFocus targetChild:" + targetChild + ",pos:" + position + ",itemCount:" + itemCount + ",lastVisible:" + lastVisible + ",id:" + mRecyclerView.getId());
          }
          targetChild.requestFocus();
          b = true;
        }
      } else {
        View view = ControllerManager.findViewByName(mRecyclerView, name);
        if (view != null && view.getVisibility() == View.VISIBLE) {
          view.requestFocus();
          b = true;
        } else {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "requestTargetChildFocus error:  targetView is null");
          }
        }
      }
      return b;
    }

    View findTargetChildFocus(int position, String name) {
      final View targetChild = findViewByPosition(position);

      View result;
      if (name == null) {
        if (targetChild instanceof RecyclerViewItem) {
          result = ((RecyclerViewItem) targetChild).getContentView();
        } else {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "requestTargetChildFocus targetChild:" + targetChild);
          }
          result = targetChild;
        }
      } else {
        result = ControllerManager.findViewByName(mRecyclerView, name);
      }
      return result;
    }


    void consumePendingWork() {
      final InitParams pf = mRecyclerView.mInitParams;
      final View targetChild = findViewByPosition(pf.targetFocusPosition);
      if (targetChild != null) {
        if (LogUtils.isDebug()) {
          LogUtils.d("PendingFocus", "handleFocusAfterLayout targetChild:" + targetChild + ",PendingFocus:" + pf);
        }
        //child 已经显示出来
        if (pf.blockOthers) {
          if (LogUtils.isDebug()) {
            Log.e("PendingFocus", "handleFocusAfterLayout PendingFocus unBlockRootFocus");
          }
          if (LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 3");
          }
          InternalExtendViewUtil.unBlockRootFocus(mRecyclerView);
        }
        boolean b;
        callNotifyInReFocus(targetChild, true);
        if (pf.delay > 0) {
          mRecyclerView.getRootView().postDelayed(new Runnable() {
            @Override
            public void run() {
              final boolean requested = requestTargetChildFocus(pf.targetFocusPosition, null);
              if (requested) {
                mRecyclerView.clearInitFocusPosition();
                if (LogUtils.isDebug()) {
                  Log.e("PendingFocus", "handleFocusAfterLayout PendingFocus one-shot clear");
                }
              }
              callNotifyInReFocus(targetChild, false);
            }
          }, pf.delay);
        } else {
          b = requestTargetChildFocus(pf.targetFocusPosition, pf.targetName);
          callNotifyInReFocus(targetChild, false);

          if (b) {
            mRecyclerView.clearInitFocusPosition();
            if (LogUtils.isDebug()) {
              Log.e("PendingFocus", "handleFocusAfterLayout PendingFocus one-shot clear");
            }
          }
        }

      }
    }

    @Override
    public void handleFocusAfterLayout(Recycler recycler, State state, boolean dataChanged) {
      if (LogUtils.isDebug()) {
        LogUtils.d("ReFocus", "---handleFocusAfterLayout state:" + state + " ,hasFocus:" + hasFocus() + ",dataChanged:" + dataChanged + "mReFocus:" + mReFocus + ",id:" + mRecyclerView.getId());
      }
      if (LogUtils.isDebug()) {
        Log.e(FocusDispatchView.TAG, "-----handleFocusAfterLayout mReFocus:" + mReFocus);
      }
      doReFocus(dataChanged);
    }

    void doReFocus(boolean dataChanged) {
      if (mRecyclerView.mInitParams != null && mRecyclerView.mInitParams.targetFocusPosition > -1) {
        consumePendingWork();
      } else {
        if (mReFocus != null && mReFocus.valid && !isInReFocus() && dataChanged) {
          //1. 尝试找到上次
          mReFocus.inReFocus = true;

          final int oldPos = mReFocus.oldPosition;

          final View f = findViewByPosition(oldPos);
          if (f != null && !f.isFocused()) {


            final View oldFocused = mReFocus.oldFocus;
            View target = f;

            if (f instanceof ViewGroup) {
              if (TVViewUtil.isViewDescendantOf(oldFocused, (ViewGroup) f) && oldFocused != f) {
                if (LogUtils.isDebug()) {
                  LogUtils.d("ReFocus", "focus isViewDescendantOf child true");
                }
                target = oldFocused;
              }
            }
            InternalExtendViewUtil.unBlockRootFocus(mRecyclerView);
            if (target != null) {
              if (LogUtils.isDebug()) {
                LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 4");
              }
              callNotifyInReFocus(f, true);
              target.requestFocus();
              callNotifyInReFocus(f, false);
            } else {
              if (LogUtils.isDebug()) {
                LogUtils.e(FocusDispatchView.TAG, "postReFocus 4");
              }
//                postReFocus();
            }
            if (LogUtils.isDebug()) {
              LogUtils.d("ReFocus", "---handleFocusAfterLayout exeRequestFocus oldFocusPos:" + oldPos + " target:" + target);
              LogUtils.d("PendingFocus", "---handleFocusAfterLayout exeRequestFocus oldFocusPos:" + oldPos + " target:" + target);
            }
          } else {
            if (LogUtils.isDebug()) {
              LogUtils.d("ReFocus", "cant find oldFocus unBlockFocus");
              LogUtils.e(FocusDispatchView.TAG, "unBlockRootFocus 5 ");
            }
            if (mReFocus != null && mReFocus.valid) {
              if (LogUtils.isDebug()) {
                Log.d(FocusDispatchView.TAG, "postReFocus 5," + mReFocus);
              }
//                postReFocus();
              InternalExtendViewUtil.unBlockRootFocus(mRecyclerView);
            } else {
              InternalExtendViewUtil.unBlockRootFocus(mRecyclerView);
            }

          }
        }
        eatReFocus();
      }
    }

    Runnable reFocusTask;

    void postReFocus() {

      if (mRecyclerView != null) {
        mRecyclerView.removeCallbacks(reFocusTask);
      }
      reFocusTask = new Runnable() {
        @Override
        public void run() {
          if (LogUtils.isDebug()) {
            Log.e(FocusDispatchView.TAG, "exe doReFocus on postReFocus !!! Refoucs:" + mReFocus);
          }
          doReFocus(true);
          eatReFocus();
        }
      };
      mRecyclerView.postDelayed(reFocusTask, 100);
    }

    void callNotifyInReFocus(View child, boolean inFocus) {
      if (child instanceof ITVView) {
        ((ITVView) child).notifyInReFocus(inFocus);
      } else if (child instanceof RecyclerViewItem) {
        ((RecyclerViewItem) child).notifyInReFocus(inFocus);
      }
    }

    private int getVectorByDirection(int direction) {
      int vector = 0;
      boolean vertical = orientation == RecyclerViewBase.VERTICAL;
      if (vertical) {
        if (direction == FOCUS_UP) {
          vector = -1;
        } else if (direction == FOCUS_DOWN) {
          vector = 1;
        }
      } else {
        if (direction == FOCUS_LEFT) {
          vector = -1;
        } else if (direction == FOCUS_RIGHT) {
          vector = 1;
        }
      }
      return vector;
    }

    //        @Nullable
    @Override
    public View onInterceptFocusSearch(@NonNull View focused, int direction) {
      mCurrentDirection = direction;
      if (InternalExtendViewUtil.isContainBlockDirection(direction, mRecyclerView.mBlockFocus)) {
        LogUtils.e(FocusDispatchView.TAG, "onInterceptFocusSearch ul: containBlockDirection is true return focused ,direction:" + direction);
        return focused;
      }
      if (!mRecyclerView.useAdvancedFocusSearch) {
        return super.onInterceptFocusSearch(focused, direction);
      }
      View result = null;
      if (mFocusListener != null) {
        result = mFocusListener.onInterceptFocusSearch(focused, direction);
      }
      if (result == null) {
        int vector = getVectorByDirection(direction);
        final int focusPosition = mRecyclerView.mFocusChildPosition;

        if (result == null && vector != 0) {
          int targetPosition = focusPosition + vector;
          final int itemCount = getItemCount();
          if (focusPosition > -1 && focusPosition < itemCount - 1) {
            View current = findViewByPosition(focusPosition);
            View next = findViewByPosition(targetPosition);
            if (current != null) {
              View[] targets = new View[next == null ? 1 : 2];
              targets[0] = current;
              if (next != null) {
                targets[1] = next;
              }
              result = mRecyclerView.searchInTargets(targets, focused, direction);
              LogUtils.d(FocusDispatchView.TAG, "onInterceptFocusSearch result:" + result);

            }
          }
        }
      }
      return result;
    }

    @Override
    protected View findNextFocusAfterFill(View focused, int focusDirection, Recycler recycler, State state) {
      int vector = getVectorByDirection(focusDirection);
      final int focusPosition = mRecyclerView.mFocusChildPosition;

      View result = null;
      if (vector != 0) {
        int targetPosition = focusPosition + vector;
        final int itemCount = getItemCount();
        if (focusPosition > -1 && focusPosition < itemCount - 1) {
          View current = findViewByPosition(focusPosition);
          View next = findViewByPosition(targetPosition);
          if (current != null) {
            View[] targets = new View[next == null ? 1 : 2];
            targets[0] = current;
            if (next != null) {
              targets[1] = next;
            }
            result = mRecyclerView.searchInTargets(targets, focused, focusDirection);
            LogUtils.d(TAG, "findNextFocusAfterFill result:" + result);
          }
        }
      }
      if (result == null) {
        LogUtils.e(TAG, "findNextFocusAfterFill return null");
        return super.findNextFocusAfterFill(focused, focusDirection, recycler, state);
      } else {
        return result;
      }
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, Recycler recycler, State state) {
      final View resultFromParent  =  super.onFocusSearchFailed(focused, focusDirection, recycler, state);

      if(mRecyclerView.mListenFocusSearchOnFail && resultFromParent == null){
        InternalExtendViewUtil.sendEventOnListFocusSearchFailed(mRecyclerView,mRecyclerView.getFocusedChild(),focused,focusDirection,getFocusSearchFailEvent());
        if(LogUtils.isDebug()) {
          LogUtils.e(FocusDispatchView.TAG, "ul: mListenFocusSearchOnFail == true , return focused direction:" + focusDirection);
        }
        return focused;
      }
      if(InternalExtendViewUtil.isContainBlockDirection(focusDirection,mRecyclerView.mBlockFocusOnFail)){
        if(resultFromParent  == null ||  mRecyclerView.forceBlockFocusOnFail) {
          if(LogUtils.isDebug()) {
            LogUtils.e(FocusDispatchView.TAG, "ul: containBlockDirection is true return focused ,direction:" + focusDirection);
          }
          return focused;
        }else{
          if(LogUtils.isDebug()) {
            LogUtils.d(FocusDispatchView.TAG, "ul: containBlockDirection is true resultFromParent !null ,direction:" + focusDirection);
          }
          return resultFromParent;
        }
      }
      if(mFocusListener != null && resultFromParent == null){
        final View v = mFocusListener.onFocusSearchFailed(focused,focusDirection,recycler,state);
        if(v != null){
          return v;
        }
      }
      if(LogUtils.isDebug()) {
        LogUtils.d("SingleList", "onFocusSearchFailed return super result:" + resultFromParent);
      }
      return resultFromParent;

    }

    void shakeEndIfNeed(View focused, View result, int focusDirection) {
      if (mRecyclerView.mShakeEndEnable) {
        final View focusedChild = mRecyclerView.getFocusedChild();
        boolean isDirectionRight;
        if (orientation == HORIZONTAL) {
          isDirectionRight = focusDirection == FOCUS_RIGHT;
        } else {
          isDirectionRight = focusDirection == FOCUS_DOWN;
        }
        if (focusedChild != null && isDirectionRight) {
          final int pos = getPosition(focusedChild);
          final int itemCount = getItemCount();
          final boolean shake = pos > itemCount - mRecyclerView.shakePreCheckNumber && (focused == result || result == null);
          if (LogUtils.isDebug()) {
            LogUtils.d(TAG, "shakeEndIfNeed pos:" + pos + " ,itemCount:" + itemCount + " focused == result:" + (focused == result) + ",result:" + result);
          }
          if (shake) {
//            mRecyclerView.exeShakeRecycleView();
            mRecyclerView.exeShakeSelf(focused, focusDirection);
          }
        }
      }
    }

    private boolean animRunning = false;

    private void shakeSelf(View view, int direction) {
      if (!animRunning) {
        animRunning = true;
        ObjectAnimator shakeSelfAnim;//抖动幅度0到5
        if (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP) {
          shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y.getName(), 0, 5f);
        } else {
          shakeSelfAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X.getName(), 0, 5f);
        }
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

    @Override
    public void onLayoutChildren(Recycler recycler, State state) {
      super.onLayoutChildren(recycler, state);
    }

    @Override
    public void layoutDecorated(View child, int left, int top, int right, int bottom) {
      super.layoutDecorated(child, left, top, right, bottom);
      final int pos = getPosition(child);
      if (LogUtils.isDebug()) {
        LogUtils.d(TAG, "layoutDecorated pos:" + pos + ",defaultSectionPosition:" + mRecyclerView.defaultSectionPosition);
      }


      if (mRecyclerView.defaultSectionPosition == pos && pos > -1) {
        mRecyclerView.changeSelectState(child, true);
      } else {
        mRecyclerView.changeSelectState(child, false);
      }
      if (mRecyclerView.layoutTriggerTargetPosition > -1) {
        if (mRecyclerView.layoutTriggerTargetPosition == pos) {
          TriggerTaskManagerModule.dispatchTriggerTask(mRecyclerView, "onTargetChildLayout");
        }
      }

      if (LogUtils.isDebug()) {
        Log.d(FocusDispatchView.TAG, "layoutDecorated pos:" + pos + "view:" + child.getId() + ",parenID:" + mRecyclerView.getId());
      }

    }
  }

  private static final class ReFocus {
    final int oldPosition;
    final View oldFocus;
    final View oldChild;
    boolean valid = true;
    boolean inReFocus = false;

    private ReFocus(int oldPosition, View oldFocus, View oldChild) {
      this.oldPosition = oldPosition;
      this.oldFocus = oldFocus;
      this.oldChild = oldChild;
    }

    @Override
    public String toString() {
      return "ReFocus{" +
        "oldPosition=" + oldPosition +
        ", oldFocus=" + oldFocus +
        ", oldChild=" + oldChild +
        ", valid=" + valid +
        '}';
    }
  }

  private static final class InitParams {
    int customFocusPosition;
    int targetFocusPosition;
    int scrollToPosition;
    int scrollOffset;
    String targetName;
    boolean valid = true;
    final boolean blockOthers;
    int delay;

    private InitParams(int targetFocusPosition, int scrollToPosition, int scrollOffset, String targetName, boolean blockOthers, int delay) {
      this.targetFocusPosition = targetFocusPosition;
      this.customFocusPosition = targetFocusPosition;
      this.scrollToPosition = scrollToPosition;
      this.scrollOffset = scrollOffset;
      this.targetName = targetName;
      this.blockOthers = blockOthers;
      this.delay = delay;
    }

    @Override
    public String toString() {
      return "PendingFocus{" +
        "targetPosition=" + targetFocusPosition +
        ",customFocusPosition=" + customFocusPosition +
        ",scrollToPosition=" + scrollToPosition +
        ",scrollOffset=" + scrollOffset +
        ", targetName='" + targetName + '\'' +
        ", valid=" + valid +
        ", blockOthers=" + blockOthers +
        ", delay=" + delay +
        '}';
    }
  }


  private void stopListenGlobalFocusChange() {
    if (mOnGlobalFocusChangeListener != null) {
      getViewTreeObserver().removeOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
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
              final boolean isOldFocusDescendantOf = TVViewUtil.isViewDescendantOf(oldFocus, TVRecyclerView.this);
              if (!isOldFocusDescendantOf) {
                notifyRecyclerViewFocusChanged(true, false, oldFocus, newFocus, false);
              }
            }
          } else {
            final boolean isNewFocusDescendantOf = TVViewUtil.isViewDescendantOf(newFocus, TVRecyclerView.this);
            if (LogUtils.isDebug()) {
              Log.d(TAG, "onGlobalFocusChanged  hasFocus : " + hasFocus() + " isNewFocusDescendantOf : " + isNewFocusDescendantOf);
            }
            if (!isNewFocusDescendantOf) {
              //焦点丢失
              final boolean isOldFocusDescendantOf = TVViewUtil.isViewDescendantOf(oldFocus, TVRecyclerView.this);

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
      LogUtils.d(TAG, "onRecyclerViewFocusChanged context:" + getHippyContext());
    }
    if (getHippyContext() != null) {
      if (hasFocus) {
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onFocusAcquired");
      } else {
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onFocusLost");
      }
    }
  }


  /****edit by zhaopeng 20201117 TV端取消滚动*/
  @Override
  public void setOverScrollEnabled(boolean up, boolean down) {
    super.setOverScrollEnabled(false, false);
  }

  @Override
  public void setOverScrollEnabled(boolean enable) {
    super.setOverScrollEnabled(false);
  }


  private RecycleViewFlinger mFlinger;

  @Override
  public boolean dispatchKeyEventPreIme(KeyEvent event) {
    if (mFlinger == null) {
      mFlinger = new NegativeLongKeyFlinger(this);
      mFlinger.setVertical(this.orientation == BaseLayoutManager.VERTICAL);
    }
    //接收事件
    return mFlinger != null && mFlinger.dispatchKeyEventPreIme(event) || super.dispatchKeyEventPreIme(event);
  }

  //zhaopeng add
  private boolean mEnableChildFocusEvent = false;
  private boolean mEnableScrollOffsetEvent = false;

  public void setEnableScrollOffsetEvent(boolean mEnableScrollOffsetEvent) {
    this.mEnableScrollOffsetEvent = mEnableScrollOffsetEvent;
  }

  private HippyViewEvent mChildFocusEvent;

  @Override
  public void setDispatchChildFocusEvent(boolean enable) {
    //
    this.mEnableChildFocusEvent = enable;
  }

  @Override
  public View getHostView() {
    return this;
  }


  @Override
  protected HippyMap generateScrollEvent() {

    return super.generateScrollEvent();
  }

  @Override
  protected void dispatchLayout() {
    super.dispatchLayout();
    handleScrollValue();
  }

  @Override
  protected void handleFocusAfterLayout(Recycler recycler, State state, boolean dataChanged) {
    super.handleFocusAfterLayout(recycler, state, dataChanged);
    handleScrollValue();
  }

  private void handleScrollValue() {
    handleScrollValue(false);
  }


  protected void onScrollYChange(int scrollY) {
    if (mEnableScrollOffsetEvent) {
      getOnScrollYOffsetChanged().send(this, generateScrollOffsetEvent(scrollY));
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
    return event;
  }


  @Override
  public void onScrolled(int x, int y) {
    super.onScrolled(x, y);
    handleScrollValue();
  }

  @Override
  protected void onSendScrollEvent() {
    super.onSendScrollEvent();
    onScrollYChange(mOffsetY);
  }

  private int lastOffsetY = 0;

  private void handleScrollValue(boolean force) {
    final int sy = mOffsetY;
    final int delta = sy - lastOffsetY;
    lastOffsetY = sy;
    int vector = 0;
    if (delta > 0) {
      vector = 1;
    }
    if (delta < 0) {
      vector = -1;
    }
    if (LogUtils.isDebug()) {
      LogUtils.d("ScrollLog", "handleScrollValue vector" + vector + ",force:" + force);
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
          LogUtils.d("ScrollLog", "++++onScrollValueGreater sy:" + sy + ",force:" + force);
        }
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onScrollYGreater");
        new HippyViewEvent("onScrollYGreaterReference").send(this, null);
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
          LogUtils.d("ScrollLog", "---onScrollValueLesser sy:" + sy + ",force:" + force);
        }
        scrollYLesserCheckPoint = sy;
        scrollYGreaterCheckPoint = sy;
        TriggerTaskManagerModule.dispatchTriggerTask(this, "onScrollYLesser");
        new HippyViewEvent("onScrollYLesserReference").send(this, null);
      } else {
        if (LogUtils.isDebug()) {
          LogUtils.d("ScrollLog", "scrollYLesserCheckPoint fail  sy:" + sy + ",force:" + force + ",scrollYLesserCheckPoint:" + scrollYLesserCheckPoint);
        }
      }

    }
  }

  public void setCheckScrollOffsetOnStateChanged(boolean b) {
    this.checkScrollOffsetOnStateChanged = b;
  }

  private boolean checkScrollOffsetOnStateChanged = false;

  @Override
  public void onScrollStateChanged(int oldState, int newState) {
    super.onScrollStateChanged(oldState, newState);
    if (checkScrollOffsetOnStateChanged && newState == SCROLL_STATE_IDLE) {
      LogUtils.d("ScrollLog", "onScrollState Changed handleScrollValue offsetY:" + mOffsetY + ",oldState:" + oldState);
      handleScrollValue(true);
    }
  }

  void resetScrollY() {
    LogUtils.w("ScrollLog", "resetScrollY");
    scrollYGreaterCheckPoint = -1;
    scrollYLesserCheckPoint = -1;
  }


  private int scrollYGreaterCheckPoint = 0;
  private int scrollYLesserCheckPoint = 0;


  private int scrollYLesserReferenceValue = -1;
  private int scrollYGreaterReferenceValue = -1;

  public void setScrollYLesserReferenceValue(int scrollYLesserReferenceValue) {
    this.scrollYLesserReferenceValue = scrollYLesserReferenceValue;
  }

  public void setScrollYGreaterReferenceValue(int scrollYGreaterReferenceValue) {
    this.scrollYGreaterReferenceValue = scrollYGreaterReferenceValue;
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

  @Override
  public void onRequestAutofocus(View child, View target, int type) {
    if(this.getVisibility() != View.VISIBLE){
      Log.e(AutoFocusManager.TAG,"onRequestAutofocus return on parent visibility != View.VISIBLE,"+ExtendUtil.debugView(this)+",target:"+ExtendUtil.debugView(target));
      return;
    }
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

  private boolean skipRequestFocus = false;

  @Override
  public void setSkipRequestFocus(boolean b) {
    this.skipRequestFocus = b;
  }

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    if(skipRequestFocus){
      Log.e(AutoFocusManager.TAG,"requestFocus return false on skipRequestFocus this:"+this);
      return false;
    }
    return super.requestFocus(direction, previouslyFocusedRect);
  }

  private ListViewControlProp listViewControlProp;

  @Override
  public ListViewControlProp getControlProps() {
    if (listViewControlProp == null) {
      listViewControlProp = new ListViewControlProp();
    }
    return listViewControlProp;
  }

  public void setAutoscrollPosition(int position,boolean force,int offset){
    Log.i(AutoFocusManager.TAG, ">list setAutoscrollPosition :" + position+",focusChildPos:"+mFocusChildPosition);
    if(getControlProps().autoScrollToPosition != position || force){
      getControlProps().autoScrollToPosition = position;
      getControlProps().scrollOffset = offset;
      if(getControlProps().autofocusPosition == position && hasFocus()){
        Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on autofocusPosition == position :" + position);
        return;
      }
      if (getControlProps().autoScrollToPosition == -1) {
        return;
      }
      final int focusChildPos = mFocusChildPosition;
      if(focusChildPos == position && hasFocus()){
        Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on focusChildPos == position :" + position);
        return;
      }
//      HippyMap hippyMap = new HippyMap();setAutoscrollPosition
//      hippyMap.pushInt("scrollToPosition",position);
//      hippyMap.pushBoolean("alignCenter",true);
//      setInitPositionInfo(hippyMap);
      if(!hasFocus() || getChildCount() < 1) {
        setTargetFocusChildPosition(position);
//        int diffScroll = Math.abs(getControlProps().currentScrollToPosition - position);
        boolean anim = false;
//        final View view = findViewByPosition(position);
//        if (view != null) {
//          anim = true;
//        }
        scrollToPosition4Autoscroll(position, anim);
//        HippyMap hippyMap = new HippyMap();
//        hippyMap.pushInt("scrollToPosition",position);
//        hippyMap.pushBoolean("alignCenter",true);
//        setInitPositionInfo(hippyMap);
      }else{
        Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on hasFocus position :" + position);
      }
      //scrollToPosition(position);
    }else{
      Log.e(AutoFocusManager.TAG, ">list setAutoscrollPosition return on autoScrollToPosition == position :" + position);
    }
  }

  public void scrollToPosition4Autoscroll(int pos, boolean anim){
    if(getSingleLineLayoutManager().childOnScreenScroller.type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER){
      getControlProps().pendingScrollToPosition = pos;
    }
    int offset = getControlProps().scrollOffset;
    if(LogUtils.isDebug()) {
      Log.i(AutoFocusManager.TAG, "handleAutoscroll scrollToPositionWithAlignCenter pos:" + pos + ",offset:" + offset + ",getControlProps().pendingScrollToPosition:" + getControlProps().pendingScrollToPosition);
    }
    //setupInitScrollWork(-1,pos,offset,null,false,0,false);
    this.scrollToPosition(pos,offset);
//    this.updateList(anim);
//    RenderUtil.req
//    RenderUtil.requestNodeLayout(this);
  }
  /** zhaopeng add*/

}
