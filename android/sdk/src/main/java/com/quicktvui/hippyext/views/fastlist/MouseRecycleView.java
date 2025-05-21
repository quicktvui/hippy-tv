package com.quicktvui.hippyext.views.fastlist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.mouse.HoverManager;


/**
 * 实现了空鼠边缘响应滚动的 RecyclerView
 *
 * @author zy
 */
public class MouseRecycleView extends RecyclerView {
  private static final String TAG = "MouseRecycleView";

  public static final String MOUSE_ACTION = "changhong.remotecontrol.mouse.status";
  public static final String MOUSE_SHOW = "show";

  public static final int MSG_STOP_SCROLL = 0x00;
  public static final int MSG_SCROLL_TO_TOP = 0x01;
  public static final int MSG_SCROLL_TO_BOTTOM = 0x02;
  public static final int MSG_SCROLL_TO_LEFT = 0x03;
  public static final int MSG_SCROLL_TO_RIGHT = 0x04;
  public static final long MSG_STAY_TIME = 1500;
  public static final long MSG_SCROLL_DELAY = 500;

  public static final int SCROLL_DEFAULT_DISTANCE = 50;
  public static final int DEFAULT_DISTANCE = 150;
  public static final float FLOAT_ZERO = 0.0f;
  public static final int INT_ZERO = 0;

  public boolean isScrolling = false;
  public int mDistanceTop = DEFAULT_DISTANCE;
  public int mDistanceBottom = DEFAULT_DISTANCE;
  public int mDistanceLeft = DEFAULT_DISTANCE;
  public int mDistanceRight = DEFAULT_DISTANCE;
  public long mStayTime = MSG_STAY_TIME;
  public long mScrollDelay = MSG_SCROLL_DELAY;
  public int mScrollDistance = SCROLL_DEFAULT_DISTANCE;

  long requestFocusDelay = 10;
  private Runnable requestFocusRunnable = null;

  private float startX, startY;

  public final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {

      switch (msg.what) {
        case MSG_SCROLL_TO_TOP:
          changeFocus(FOCUS_UP);
          removeMessages(MSG_SCROLL_TO_TOP);
          sendEmptyMessageDelayed(MSG_SCROLL_TO_TOP, mScrollDelay);
          break;

        case MSG_SCROLL_TO_BOTTOM:
          changeFocus(FOCUS_DOWN);
          removeMessages(MSG_SCROLL_TO_BOTTOM);
          sendEmptyMessageDelayed(MSG_SCROLL_TO_BOTTOM, mScrollDelay);
          break;

        case MSG_SCROLL_TO_LEFT:
          changeFocus(FOCUS_LEFT);
          removeMessages(MSG_SCROLL_TO_LEFT);
          sendEmptyMessageDelayed(MSG_SCROLL_TO_LEFT, mScrollDelay);
          break;

        case MSG_SCROLL_TO_RIGHT:
          changeFocus(FOCUS_RIGHT);
          removeMessages(MSG_SCROLL_TO_RIGHT);
          sendEmptyMessageDelayed(MSG_SCROLL_TO_RIGHT, mScrollDelay);
          break;

        case MSG_STOP_SCROLL:
          removeMessages(MSG_STOP_SCROLL);
          removeMessages(MSG_SCROLL_TO_TOP);
          removeMessages(MSG_SCROLL_TO_BOTTOM);
          removeMessages(MSG_SCROLL_TO_LEFT);
          removeMessages(MSG_SCROLL_TO_RIGHT);
          isScrolling = false;
          break;

        default:
          break;
      }
      super.handleMessage(msg);
    }
  };

  public BroadcastReceiver mMouseReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      //空鼠退出时停止滚动
      if (intent != null && MOUSE_ACTION.equals(intent.getAction()) && !intent.getBooleanExtra(MOUSE_SHOW, true)) {
        mMainHandler.removeMessages(MSG_STOP_SCROLL);
        mMainHandler.sendEmptyMessage(MSG_STOP_SCROLL);
        HoverManager.getInstance().setMouseEnter(false);
      }
    }
  };

  public IntentFilter mMouseIntentFilter = new IntentFilter(MOUSE_ACTION);

  public MouseRecycleView(Context context) {
    super(context);
    //setOnGenericMotionListener(this);
  }

  public MouseRecycleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    //setOnGenericMotionListener(this);
  }

  public MouseRecycleView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    //setOnGenericMotionListener(this);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent e) {
    switch (e.getAction()) {
      case MotionEvent.ACTION_DOWN:
        startX = e.getX();
        startY = e.getY();
        break;
      case MotionEvent.ACTION_MOVE:
        float dx = e.getX() - startX;
        float dy = e.getY() - startY;
        // 判断是否是左右滑动
        if (Math.abs(dx) > Math.abs(dy)) {
          // 处理边界情况
          if ((dx > 0 && !canScrollHorizontally(-1)) || (dx < 0 && !canScrollHorizontally(1))) {
            // 滚动到最左/右边时，交给父 View 处理
            getParent().requestDisallowInterceptTouchEvent(false);
          } else {
            // 让 RecyclerView 处理滑动  不能放到ACTION_DOWN中会影响纵向view滚动
            getParent().requestDisallowInterceptTouchEvent(true);
          }
        } else {
          //垂直滑动，交给父 View
          getParent().requestDisallowInterceptTouchEvent(false);
        }
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        getParent().requestDisallowInterceptTouchEvent(false);
        break;
    }
    return super.onInterceptTouchEvent(e);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    try {
      getContext().registerReceiver(mMouseReceiver, mMouseIntentFilter);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    try {
      getContext().unregisterReceiver(mMouseReceiver);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
      //按键时停止空鼠滚动
      mMainHandler.removeMessages(MSG_STOP_SCROLL);
      mMainHandler.sendEmptyMessage(MSG_STOP_SCROLL);
      HoverManager.getInstance().setMouseEnter(false);
    }
    return super.dispatchKeyEvent(event);
  }

  /**
   * 在此处出来空鼠相关事件，用于计算向哪个方向滚动
   *
   * @param event
   * @return
   */
  @Override
  public boolean dispatchGenericMotionEvent(MotionEvent event) {
    HoverManager.getInstance().dispatchGenericMotionEvent(event);

    float x = event.getX();
    float y = event.getY();

    switch (event.getAction()) {
//            case MotionEvent.ACTION_HOVER_ENTER:
      case MotionEvent.ACTION_HOVER_MOVE:
        //竖向滚动
        if (isScrollVertically()) {

          if (y >= FLOAT_ZERO && y <= mDistanceTop) {
            //上方响应区
            if (!isScrolling) {
              isScrolling = true;
              mMainHandler.removeMessages(MSG_SCROLL_TO_TOP);
              mMainHandler.sendEmptyMessageDelayed(MSG_SCROLL_TO_TOP, mStayTime);
            }
          } else if (y <= getHeight() && y >= (getHeight() - mDistanceBottom)) {
            //下方响应区
            if (!isScrolling) {
              isScrolling = true;
              mMainHandler.removeMessages(MSG_SCROLL_TO_BOTTOM);
              mMainHandler.sendEmptyMessageDelayed(MSG_SCROLL_TO_BOTTOM, mStayTime);
            }

          } else {
            //未在响应区
            isScrolling = false;
            mMainHandler.removeMessages(MSG_STOP_SCROLL);
            mMainHandler.sendEmptyMessage(MSG_STOP_SCROLL);
          }
        } else {
          if (x >= FLOAT_ZERO && x <= mDistanceLeft) {
            //左侧响应区
            if (!isScrolling) {
              isScrolling = true;
              mMainHandler.removeMessages(MSG_SCROLL_TO_LEFT);
              mMainHandler.sendEmptyMessageDelayed(MSG_SCROLL_TO_LEFT, mStayTime);
            }

          } else if (x <= getWidth() && x >= (getWidth() - mDistanceRight)) {
            //右侧响应区
            if (!isScrolling) {
              isScrolling = true;
              mMainHandler.removeMessages(MSG_SCROLL_TO_RIGHT);
              mMainHandler.sendEmptyMessageDelayed(MSG_SCROLL_TO_RIGHT, mStayTime);
            }

          } else {
            //未在响应区
            isScrolling = false;
            mMainHandler.removeMessages(MSG_STOP_SCROLL);
            mMainHandler.sendEmptyMessage(MSG_STOP_SCROLL);
          }
        }
        break;
      case MotionEvent.ACTION_HOVER_EXIT:
      case MotionEvent.ACTION_BUTTON_PRESS: {
        // ACTION_HOVER_EXIT -> ACTION_BUTTON_PRESS -> ACTION_BUTTON_RELEASE -> ACTION_HOVER_ENTER
        mMainHandler.removeMessages(MSG_STOP_SCROLL);
        mMainHandler.sendEmptyMessage(MSG_STOP_SCROLL);
      }
      break;

      default:
        break;
    }

    return super.dispatchGenericMotionEvent(event);
  }


  /**
   * 设置各个方向响应空鼠滚动的距离区域
   *
   * @param distanceTop    上方
   * @param distanceBottom 下方
   * @param distanceLeft   左侧
   * @param distanceRight  右侧
   */
  public void setDistance(int distanceTop, int distanceBottom, int distanceLeft, int distanceRight) {
    this.mDistanceTop = distanceTop;
    this.mDistanceBottom = distanceBottom;
    this.mDistanceLeft = distanceLeft;
    this.mDistanceRight = distanceRight;
  }

  /**
   * 设置停留响应时间
   *
   * @param time 毫秒
   */
  public void setStayTime(long time) {
    this.mStayTime = time;
  }

  /**
   * 设置滚动时间隔时间
   *
   * @param time 毫秒
   */
  public void setScrollDelay(long time) {
    this.mScrollDelay = time;
  }

  /**
   * 设置找焦点时偏移的距离，避免 view 不在界面内而无法找到
   *
   * @param distance 像素
   */
  public void setScrollDistance(int distance) {
    this.mScrollDistance = distance;
  }

  /**
   * 进行空鼠边缘滚动时，向对应方向进行寻找焦点需要排除上焦的视图
   * 例：按理来说只
   *
   * @param view view
   * @return
   */
  public boolean isExcludeView(View view) {
    return false;
  }

  /**
   * 向对应方向寻找下一个焦点
   *
   * @param focusDirection 寻找焦点方向
   * @return
   */
  public boolean changeFocus(int focusDirection) {
    View focusView = findFocus();
    if (focusView != null) {
      View nextView = null;
      if (focusDirection == View.FOCUS_UP) {
        scrollBy(INT_ZERO, -mScrollDistance);
        nextView = focusView.focusSearch(View.FOCUS_UP);
      } else if (focusDirection == View.FOCUS_DOWN) {
        scrollBy(INT_ZERO, mScrollDistance);
        nextView = focusView.focusSearch(View.FOCUS_DOWN);
      } else if (focusDirection == View.FOCUS_LEFT) {
        scrollBy(-mScrollDistance, INT_ZERO);
        nextView = focusView.focusSearch(View.FOCUS_LEFT);
      } else if (focusDirection == View.FOCUS_RIGHT) {
        scrollBy(mScrollDistance, INT_ZERO);
        nextView = focusView.focusSearch(View.FOCUS_RIGHT);
      }
      if (nextView != null) {
        Log.d("mouseRecycleView", "nextView----->" + nextView + "nextView id" + nextView.getId() + "nextView tag--->" + nextView.getTag());
      }
      if (nextView != null && !isExcludeView(nextView)) {
        return nextView.requestFocus();
      }
    } else {
      Log.e("mouseRecycleView", " no focus find, cant scroll");
    }
    return false;
  }

  /**
   * 是否竖向滚动
   *
   * @return true-竖向，false-横向
   */
  public boolean isScrollVertically() {
    if (getLayoutManager() != null && getLayoutManager() instanceof LinearLayoutManager) {
      return getLayoutManager().canScrollVertically();
    }
    return true;
  }

  View findFocusableViewUnder(float x, float y) {
    View child = findChildViewUnder(x, y);
    if (child == null) {
      return null;
    }
    if (child.isFocusable()) {
      return child;
    } else if (child instanceof ViewGroup) {
      return findChildViewUnder((ViewGroup) child, x, y);
    }
    return null;

  }

  int[] temp = new int[2];

  public View findChildViewUnder(ViewGroup parent, float x, float y) {
    int count = parent != null && parent.getChildCount() > 0 ? parent.getChildCount() : 0;
    for (int i = count - 1; i >= 0; --i) {
      View child = parent.getChildAt(i);
      float translationX = child.getTranslationX();
      float translationY = child.getTranslationY();
      boolean contain = false;
      if (child.isFocusable()) {
        child.getLocationOnScreen(temp);
        int left = temp[0];
        int top = temp[1];
        int right = left + child.getWidth();
        int bottom = top + child.getHeight();
        contain = x >= (float) left + translationX && x <= (float) right + translationX && y >= (float) top + translationY && y <= bottom + translationY;

//          Log.i("mouseRecycleView", "findChildViewUnder: "+child+"  x: "+x+"  y: "+y+"  left: "+left+"  top: "+top+"  right: "+right+"  bottom: "+bottom+"  contain: "+contain);
      }
      if (contain && child.isFocusable()) {
        return child;
      } else if (child instanceof ViewGroup) {
        View view = findChildViewUnder((ViewGroup) child, x, y);
        if (view != null) {
          return view;
        }
      }
    }
    return null;
  }

//  @Override
//  protected boolean dispatchHoverEvent(MotionEvent event) {
//    return super.dispatchHoverEvent(event);
//  }

  void clearRequestFocusRunnable() {
    if (requestFocusRunnable != null) {
      removeCallbacks(requestFocusRunnable);
      requestFocusRunnable = null;
    }
  }


  boolean mouseRequestFocus(float x, float y, View view) {
    // 获取悬停位置

//        Log.d("mouseRecycleView", "->>>> x " + event.getX()+", y "+event.getY());
//    if (LogUtils.isDebug()) {
    Log.d("mouseRecycleView", "悬停 : " + ExtendUtil.debugViewLite(view));
//    }
    if (view != null && !view.hasFocus()) {
      Log.i("mouseRecycleView", "childView.requestFocus()");
      view.requestFocus();
      return true;
    }
//    if (childView == null) {
//      return false;
//    }else{
//      if(!childView.hasFocus()){
//        Log.i("mouseRecycleView", "childView.requestFocus()");
//        childView.requestFocus();
//        return true;
//      }
//    }
    return false;
  }

  @Override
  public boolean dispatchHoverEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_HOVER_ENTER:
      case MotionEvent.ACTION_HOVER_MOVE:

        clearRequestFocusRunnable();
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final View childView = findFocusableViewUnder(x, y);
        requestFocusRunnable = new Runnable() {
          @Override
          public void run() {
            mouseRequestFocus(x, y, childView);
          }
        };
        postDelayed(requestFocusRunnable, requestFocusDelay);
        return super.dispatchHoverEvent(event);

//        int position = getChildAdapterPosition(childView);
//        if (position != RecyclerView.NO_POSITION) {
//          // 找到悬停的项目并请求焦点
//          View view = Objects.requireNonNull(getLayoutManager()).findViewByPosition(position);
//          Log.d("mouseRecycleView", "悬停位置view: " + view + "当前view焦点状态-->" + view.hasFocus());
//          if (view != null && !view.hasFocus()) {
//            view.requestFocus();
//          }
//        }
      case MotionEvent.ACTION_HOVER_EXIT:
        // 鼠标移出 RecyclerView 时处理，例如清除焦点
        break;
    }
    return super.dispatchHoverEvent(event);
  }

  /*@Override
  public boolean onGenericMotion(View v, MotionEvent event) {
    Log.d("debugFocus", "hippy onGenericMotion:  "+v + "  v id---->"+v.getId() + "  v id---->"+v.getTag());
    if (event.getAction() == MotionEvent.ACTION_SCROLL){
      return super.onGenericMotionEvent(event);
    }
    if (v!=null && event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) {
      Log.d("debugFocus", "hippy onGenericMotion: ACTION_HOVER_ENTER  "+v + "  v id---->"+v.getId() + "  v id---->"+v.getTag());
      v.requestFocus();
    }
    return super.onGenericMotionEvent(event);
  }*/
}
