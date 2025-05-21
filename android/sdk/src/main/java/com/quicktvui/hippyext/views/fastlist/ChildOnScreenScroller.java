package com.quicktvui.hippyext.views.fastlist;

import static android.view.View.FOCUS_BACKWARD;
import static android.view.View.FOCUS_DOWN;
import static android.view.View.FOCUS_FORWARD;
import static android.view.View.FOCUS_LEFT;
import static android.view.View.FOCUS_RIGHT;
import static android.view.View.FOCUS_UP;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.quicktvui.base.ui.TVViewUtil;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.list.TVRecyclerView;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;


public abstract class ChildOnScreenScroller {
    protected boolean enableScrollToTopOnFirstFocus = false;
    protected boolean noScrollOnFirstScreen = false;
    int type = TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER;;
  int clampForward = 0;
  int clampBackward = 0;
  boolean enableScroll = true;
  protected int scrollOffset;

  public int getScrollType(){
    return type;
  }

  public void setScrollOffset(int scrollOffset) {
    this.scrollOffset = scrollOffset;
  }

  public int getScrollOffset() {
    return scrollOffset;
  }

  public void setEnableScroll(boolean enableScroll) {
    this.enableScroll = enableScroll;
  }

  public void setClampBackward(int clampBackward) {
    this.clampBackward = clampBackward;
  }

  public void setClampForward(int clampForward) {
    this.clampForward = clampForward;
  }

  public void setType(int type) {
    this.type = type;
  }

  public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child, @NonNull Rect rect, boolean immediate, int direction, boolean focusedChildVisible, int childPosition){
        return false;
    }

    public boolean smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
      return false;
    }

    public void setScrollThresholdVertical(int threshold){

    }

    public void setScrollThresholdHorizontal(int threshold){

    }


  public void setNoScrollOnFirstScreen(boolean noScrollOnFirstScreen) {
    this.noScrollOnFirstScreen = noScrollOnFirstScreen;
  }

  public abstract void notifyOnScroll(RecyclerView mRecyclerView, int vector, int sx, int sy);

  public static class Default extends ChildOnScreenScroller {
        final int orientation ;
//        int scrollOffset;


        public int thresholdScrollVertical = PixelUtil.dp2pxInt(50);
        public int thresholdScrollHorizontal = PixelUtil.dp2pxInt(300);
        //public int type = TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER;


      public Default(int orientation, int scrollOffset) {
          this.orientation = orientation;
          this.scrollOffset = scrollOffset;
      }




      @Override
      public void setScrollThresholdHorizontal(int threshold) {
        this.thresholdScrollHorizontal = threshold;
      }

      @Override
      public void notifyOnScroll(RecyclerView mRecyclerView, int vector, int sx, int sy) {
  //        assert (mRecyclerView instanceof IRecyclerView): "ChildOnScreenScroller recyclerView必须是IRecyclerView的实例";
//        final IRecyclerView tv = (IRecyclerView) mRecyclerView;
//        int offset = orientation == RecyclerView.HORIZONTAL ? sx : sy;
//      if (vector < 0 && autoScrollCheckLine > 0) {
//        //后退
//        if (offset < autoScrollCheckLine && offset > 0) {
//            Log.e("ChildOnScreenScroller","notifyOnScroll exe scrollToTop");
//            tv.scrollToTop();
//        }
//      }
    }


      @Override
      public void setScrollThresholdVertical(int threshold) {
        this.thresholdScrollVertical = threshold;
      }

//      public int getScrollOffset() {
//            return scrollOffset;
//        }
//        public void setScrollOffset(int scrollOffset) {
//            this.scrollOffset = scrollOffset;
//        }

        public Default(int orientation) {
            this(orientation,0);
        }

        int clampOnStart(RecyclerView parent ,View child,int scroll,boolean vertical){
        return scroll;
//          if (parent instanceof TVListView) {
//            int pos = -1;
//            if (child != null) {
//              try {
//                pos = parent.getChildAdapterPosition(child);
//              }catch (Throwable t){}
//            }
//            if (pos > 0) {
//              Log.e("ChildOnScreenScroller","clampOnStart return on pos > 0)");
//              return scroll;
//            }
//
//            TVListView fv = (TVListView) parent;
//            if (LogUtils.isDebug()) {
//              Log.d("ChildOnScreenScroller","clampOnStart parent getOffsetY() "+fv.getOffsetY()+",getOffsetX:"+fv.getOffsetX()+",scroll:"+scroll);
//            }
//            if(scroll <= 0){ // 向顶端滚动时，检查一下offset值，如果offset <= 0,则不需要再滚动
//              if (vertical) {
//                if (fv.getOffsetY() <= 0) {
//                  if (LogUtils.isDebug()) {
//                    Log.w("ChildOnScreenScroller","clampOnStart on fv.getOffsetY() "+fv.getOffsetY());
//                  }
//                  return 0;
//                }
//              }else{
//                if (fv.getOffsetX() <= 0) {
//                  if (LogUtils.isDebug()) {
//                    Log.w("ChildOnScreenScroller","clampOnStart on fv.getOffsetX() "+fv.getOffsetX());
//                  }
//                  return 0;
//                }
//              }
//            }
//          }
//          return scroll;
        }

        private boolean scrollOnOverride(View child, int direction, RecyclerView parent, boolean immediate){
          View sorView = FastAdapterUtil.finalRealContent(child);
          if(sorView instanceof HippyViewGroup && ((HippyViewGroup) sorView).getScrollOverride() != null){
            final HippyMap sor = ((HippyViewGroup) sorView).getScrollOverride();
            final String directionName = HippyViewGroup.getDirectionName(direction);
            if(sor.containsKey(directionName)){
              final float dy = PixelUtil.dp2px(sor.getInt(directionName));
              if(dy != 0){
//                if(dy < 0 && parent instanceof IRecyclerView){
//                  final int offsetY = ((IRecyclerView) parent).getOffsetY();
//                  Log.e("ChildOnScreenScroller","requestChildRectangleOnScreen  on scrollOverride offsetY : "+offsetY+",dy:"+dy);
//                  if(dy + offsetY <= 0){
//                    Log.e("ChildOnScreenScroller","requestChildRectangleOnScreen scrollToTop on scrollOverride on : "+sor+",dy:"+dy);
//                    ((IRecyclerView) parent).scrollToTop();
//                    return true;
//                  }
//                }
                if (orientation == RecyclerView.VERTICAL) {
                  exeScrollRecyclerView(parent, 0, (int) dy, immediate);
                } else {
                  exeScrollRecyclerView(parent, (int) dy, 0, immediate);
                }
              }
              Log.e("ChildOnScreenScroller","requestChildRectangleOnScreen return on scrollOverride on : "+sor+",dy:"+dy);
              return true;
            }
          }
          return false;
        }

        @Override
        public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child, @NonNull Rect rect, boolean immediate,int direction,boolean focusedChildVisible,int childPosition) {
            if(!enableScroll){
              return true;
            }
            if(LogUtils.isDebug()) {
              final Rect childRect = new Rect();
              childRect.set(child.getLeft(),child.getTop(),child.getRight(),child.getBottom());
              Log.i("ChildOnScreenScroller", "<<--------------------------");
              Log.i("ChildOnScreenScroller", "LogChildRect childRect:" + childRect+",rect:"+rect+",noScrollOnFirstScreen:"+noScrollOnFirstScreen);
              boolean isFocusScrollTarget = child instanceof HippyViewGroup && ((HippyViewGroup) child).isFocusScrollTarget();
              Log.i("ChildOnScreenScroller", "LogChildRect child width:"+
                child.getWidth()+",child height:"+child.getHeight()+",child:"+child+",isFocusScrollTarget:"+isFocusScrollTarget);
              childRect.set(parent.getLeft(),parent.getTop(),parent.getRight(),parent.getBottom());
              Log.i("ChildOnScreenScroller", "parent rect : "+childRect+",parent width:"+parent.getWidth()+",parent height:"+ childRect.height());
//              if(child instanceof FastAdapter.ItemRootView){
//                Log.i("ChildOnScreenScroller", "child is ItemRootView item,real content:"+((FastAdapter.ItemRootView) child).getContentView());
//                if(((FastAdapter.ItemRootView) child).getContentView() instanceof FastItemView){
//                  final View v = ((FastItemView) ((FastAdapter.ItemRootView) child).getContentView()).getChildAt(0);
//                  Log.e("ChildOnScreenScroller", "child View :"+v);
//                }
//              }
              Log.i("ChildOnScreenScroller", "-------------------------->>");
            }

            if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_NONE){
              LogUtils.e("ChildOnScreenScroller","requestChildRectangleOnScreen makeChildVisibleType none scroll Type return ");
              return true;
            }
            int vector = TVViewUtil.getVectorByDirection(direction,orientation);

//            if(sorView instanceof FastItemView &&  ((FastItemView) sorView).getContentView() != null){
//              sorView = ((FastItemView) sorView).getContentView();
//            }
          //Log.i("ChildOnScreenScroller", "-----------sorView:"+sorView);
            if(scrollOnOverride(child,direction,parent,immediate)){
              return true;
            }
            if(enableScrollToTopOnFirstFocus){
              if (vector < 0) {
                if (childPosition == 0) {
                  if (parent instanceof FastListView) {
                    ((FastListView) parent).scrollToTop();
                  }else{
                    parent.scrollToPosition(0);
                  }
                  LogUtils.e("ChildOnScreenScroller","requestChildRectangleOnScreen scrollTop on back to firstFocus");
                  return true;
                }
              }
            }
            if(LogUtils.isDebug()){
              LogUtils.i("ChildOnScreenScroller","requestChildRectangleOnScreen vector "+vector+",direction:"+direction+", childPos:"+childPosition+",type:"+type+",enableScrollToTopOnFirstFocus");
            }
            if (orientation == RecyclerView.VERTICAL) {
                final int parentTop = parent.getPaddingTop();
                int childTop = child.getTop() + rect.top - child.getScrollY();
//              Log.i("ZHAOPENG","0000 childTop : "+childTop+",rect.top:"+rect.top+",child.getScrollY():"+child.getScrollY()+",parentTop:"+parentTop);
              final int parentBottom = parent.getHeight() - parent.getPaddingBottom();
              int childBottom = childTop + rect.bottom;

              final int parentCenter = (int) (parentTop + parent.getHeight() * 0.5f);
                View focused = null;

                if (focused == null) {
//                  Log.i("ZHAOPENG","childScroll child : "+child);
                    if(child instanceof HippyViewGroup &&((HippyViewGroup) child).isFocusScrollTarget()){
                      focused = child;
                      childTop = child.getTop() - child.getScrollY();
                      childBottom = childTop + rect.bottom;
                      if(LogUtils.isDebug()) {
                        Log.i("ChildOnScreenScroller", "ChildOnScreenScroller isFocusScrollTarget true ,child : " + child);
                      }
                    }else {
                      focused = child.findFocus();
                      focused = focused != null ? focused : child;
                    }
                }
                if (focused != null) {

                  if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER) {
                    final int childCenter = (int) (childTop + focused.getHeight() * 0.5f);
                    final int dy = clampOnStart(parent,child,childCenter - parentCenter + scrollOffset,true);
//                    Log.e("ZHAOPENG","22222 child : "+child+",focused:"+focused+",childCenter:"+childCenter+",height:"+focused.getHeight()+",dy:"+dy);
                    final int deltaY = Math.abs(dy);
                    childBottom = childTop + focused.getHeight();
                    int threshold;
                    if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
                      threshold = thresholdScrollHorizontal;
                    } else {
                      threshold = thresholdScrollVertical;
                    }
        //isChildVisible判断有误，暂时注释
//                    if (deltaY < threshold && isChildVisible) {
//                      return true;
//                    }
                    if(LogUtils.isDebug()) {
                      LogUtils.d("ChildOnScreenScroller", "handle Scroll deltaY : " + deltaY + ",threshold:" + threshold+",dy :"+dy);
                      LogUtils.d("ScrollLog", "handle Scroll deltaY : " + dy + ",threshold:" + threshold);
                    }
                    if (Math.abs(deltaY) < threshold) {
                      return true;
                    }
                    if (noScrollOnFirstScreen) {
                      if (parent instanceof IRecyclerView) {
                        IRecyclerView tv = (IRecyclerView) parent;
                        if (tv.getOffsetY() < 5 && dy > 0) {
//                            LogUtils.d("ChildOnScreenScroller", "handle autoScrollCheckLine  childBottom: " + childBottom + ",parentBottom:" + parentBottom + ",tvOffset:" + ((IRecyclerView) parent).getOffsetY() );
                          if (childBottom < parentBottom) {
                            Log.e("ChildOnScreenScroller", "handle return on childBottom < parentBottom tv:"+tv);
                            return true;
                          }
                        }else{
                          if (dy < 0) {
                            int toY = deltaY + tv.getOffsetY();
                              if(LogUtils.isDebug()) {
                                LogUtils.d("ChildOnScreenScroller", "handle 2 toY " + toY + ",parentBottom:" + parentBottom + ",tv.getOffsetY()" + tv.getOffsetY()+",parent.getHeight():"+parent.getHeight() +",childBottom:"+childBottom);
                              }
                            if (toY < parentBottom || tv.getOffsetY() < parentBottom) {
                              //Log.i("ChildOnScreenScroller", "handle scrollToTop on toY < parentBottom tv："+tv);
                              if((childBottom  + tv.getOffsetY()) > parentBottom){
                                Log.i("ChildOnScreenScroller", "handle scrollToTop on toY < parentBottom ,skip on childBottom > parentBottom childBottom:"+childBottom+",parentBottom:"+parentBottom);
                              }else {
                                Log.i("ChildOnScreenScroller", "handle scrollToTop on toY < parentBottom tv："+tv);
                                tv.scrollToTop();
                                //parent.smoothScrollToPosition(0);
                                //tv.smoothScrollToTop();
                                return true;
                              }
                            }
                          }
                        }
                      }
                    }
                    exeScrollRecyclerView(parent, 0, dy, immediate);
                    return dy != 0;
                  }else{
                    int dy;
                    {
                      if(direction == FOCUS_UP || direction == FOCUS_BACKWARD){
                        //后退
                        dy = Math.min(0,childTop - (parentTop + clampBackward));
                      }else if(direction == FOCUS_DOWN || direction == FOCUS_FORWARD){
                        //前进
                        dy = Math.max(0,childBottom - (parentBottom - clampForward));
                      }else{
                        dy = 0;
                      }
                      dy = clampOnStart(parent,child,dy,true);
                      if(LogUtils.isDebug()) {
                        LogUtils.d("ChildOnScreenScroller", "handle Scroll deltaY : " + dy + ",threshold:" + thresholdScrollVertical);
                        LogUtils.d("ScrollLog", "handle Scroll deltaY : " + dy + ",threshold:" + thresholdScrollVertical);
                      }
                      if(Math.abs(dy) < thresholdScrollVertical){
                        LogUtils.d("ChildOnScreenScroller","return on dy < thresholdScrollVertical dy:"+dy+",thresholdScrollVertical:"+thresholdScrollVertical);
                        return true;
                      }
                      exeScrollRecyclerView(parent, 0, dy + scrollOffset, immediate);
                      return dy != 0;
                    }
                  }
                }
            }else{



                final int parentLeft = parent.getPaddingLeft();
                int childLeft = child.getLeft() + rect.left - child.getScrollX();
                final int parentRight = parent.getWidth() -  parent.getPaddingRight();
                int childRight = childLeft + rect.right;


                final int parentCenter = (int) (parentLeft + parent.getWidth() * 0.5f);
                View focused = null;

                if (focused == null) {
                  if(child instanceof HippyViewGroup &&((HippyViewGroup) child).isFocusScrollTarget()){
                    focused = child;
                    childLeft = child.getLeft() - child.getScrollY();
                    childRight = childLeft + rect.right;
//                      Log.e("ZHAOPENG","11111 child : "+child);
                  }else {
                    focused = child.findFocus();
                    focused = focused != null ? focused : child;
//                      Log.e("ZHAOPENG","22222 child : "+child+",focused:"+focused);
                  }
                }



                if (focused != null) {

                    if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER) {
                      final int childCenter = (int) (childLeft + focused.getWidth() * 0.5f);

                      final int dx = clampOnStart(parent,child,childCenter - parentCenter + scrollOffset,false);

                      exeScrollRecyclerView(parent, dx, 0, immediate);
                      return dx != 0;
                    }else{
                      int dx;
                      {
                        if(direction == FOCUS_LEFT || direction == FOCUS_BACKWARD){
                          //后退
                          dx = Math.min(0,childLeft - (parentLeft + clampBackward));
                        }else if(direction == FOCUS_RIGHT || direction == FOCUS_FORWARD){
                          //前进
                          dx = Math.max(0,childRight - (parentRight - clampForward));
                        }else{
                          dx = 0;
                        }
                        dx = clampOnStart(parent,child,dx,false);
                        exeScrollRecyclerView(parent, dx, 0, immediate);
                        return dx != 0;
                      }
                    }
                }

            }

            return false;
        }


        private void exeScrollRecyclerView(RecyclerView parent, int sx, int sy, boolean immediate){
          if(LogUtils.isDebug()){
              Log.v("ChildOnScreenScroller","exeScrollRecyclerView sx : "+sx+",sy :"+sy+",immediate:"+immediate);
          }
          if(sx == 0 && sy == 0){
              return;
          }
          if (immediate) {
              parent.scrollBy(sx, sy);
          } else {
              parent.smoothScrollBy(sx, sy);
          }
        }

      @Override
      public boolean smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        // super.smoothScrollToPosition(recyclerView, state, position);
        return false;
      }



    }

    public static int getScrollToPositionOffset(TVListView tvList,int position,int offset){
        final View child = tvList.getLayoutExecutor().findViewByPosition(position);
      if (child == null) {
        return 0;
      }
        final int childCount = tvList.getLayoutManagerCompat().getExecutor().getItemCount();
        return getScrollToPositionOffset(position,tvList,child,tvList.getOrientation(),
          childCount,tvList.getChildOnScreenScroller().getScrollType(), offset
          );
    }

  public static int getScrollToPositionOffset(TVListView tvList,int position,View child,int offset){
//    final View child = tvList.getLayoutExecutor().findViewByPosition(position);
//    if (child == null) {
//      return 0;
//    }
    final int childCount = tvList.getLayoutManagerCompat().getExecutor().getItemCount();
    return getScrollToPositionOffset(position,tvList,child,tvList.getOrientation(),
      childCount,tvList.getChildOnScreenScroller().getScrollType(), offset
    );
  }

    public static int getScrollToPositionOffset(int position,View parent,View item,int orientation,int itemCount,int type,int offset){
      final int parentSize = orientation == RecyclerView.HORIZONTAL ? parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight()
        : parent.getHeight() - parent.getPaddingBottom() - parent.getPaddingTop();
      final int itemSize = orientation == RecyclerView.HORIZONTAL ? item.getWidth() : item.getHeight();
      return getScrollToPositionOffset(position,parentSize,itemSize,itemCount,type,offset);
    }

  public static void getChildRectangleOnScreen(View parent, View child, Rect rect, boolean immediate,int[] out,int direction) {
//    int[] out = new int[2];
    int parentLeft = parent.getPaddingLeft();
    int parentTop = parent.getPaddingTop();
    int parentRight = parent.getWidth() - parent.getPaddingRight();
    int parentBottom = parent.getHeight() - parent.getPaddingBottom();
    int childLeft = child.getLeft() + rect.left - child.getScrollX();
    int childTop = child.getTop() + rect.top - child.getScrollY();
    int childRight = childLeft + rect.width();
    int childBottom = childTop + rect.height();
  }



    public static int getScrollToPositionOffsetOnCenterType(View parent,View item,int orientation){
      final int parentSize = orientation == RecyclerView.HORIZONTAL ? parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight()
        : parent.getHeight() - parent.getPaddingBottom() - parent.getPaddingTop();
      final int itemSize = orientation == RecyclerView.HORIZONTAL ? item.getWidth() : item.getHeight();

      int offset = (int) ((parentSize - itemSize) * 0.5f);

      return offset;
    }

  public static int getScrollToPositionOffset(int position,int parentSize,int itemSize,int itemCount,int type,int scrollOffset) {

    int offset = 0;

    final int lastIndex = itemCount - 1;
    if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER){
      offset = (int) ((parentSize - itemSize) * 0.5f);
    }
    if(LogUtils.isDebug()) {
      Log.i("ChildOnScreenScroller", "scrollToPositionBeforeSetListData parentSize:" + parentSize + ",itemSize:" + itemSize + ",offset:" + offset + ",scrollOffset:" + scrollOffset);
    }
    //设置的offset超过边界，跳转到最后一个Item上去
    if (position >= lastIndex) {
      position = lastIndex;
      //不能划出内容高度
      if (offset < 0) {
        offset = 0;
      }
    }

    return offset + scrollOffset;
  }

  public static int computeAlignCenterScrollOffset(int parentSize,int itemSize){
    int offset = (int) ((parentSize - itemSize) * 0.5f);
//    if(LogUtils.isDebug()) {
//      Log.i("ChildOnScreenScroller", "computeAlignCenterScrollOffset parentSize:" + parentSize + ",itemSize:" + itemSize + ",offset:" + offset );
//    }
    //设置的offset超过边界，跳转到最后一个Item上去
    return offset;
  }

  public interface IRecyclerView {
    int getOffsetX();
    int getOffsetY();
    void scrollToTop();
    void smoothScrollToTop();
  }

}
