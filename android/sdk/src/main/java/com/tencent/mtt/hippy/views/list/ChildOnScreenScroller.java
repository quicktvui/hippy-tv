package com.tencent.mtt.hippy.views.list;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import android.support.annotation.NonNull;

import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;
import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewBase;

import static android.view.View.FOCUS_BACKWARD;
import static android.view.View.FOCUS_DOWN;
import static android.view.View.FOCUS_FORWARD;
import static android.view.View.FOCUS_LEFT;
import static android.view.View.FOCUS_RIGHT;
import static android.view.View.FOCUS_UP;

public abstract class ChildOnScreenScroller {


    public boolean requestChildRectangleOnScreen(@NonNull RecyclerViewBase parent, @NonNull View child, @NonNull Rect rect, boolean immediate, int direction){
        return false;
    }

    public boolean smoothScrollToPosition(RecyclerViewBase recyclerView, RecyclerViewBase.State state, int position) {
      return false;
    }

    public void setScrollThresholdVertical(int threshold){

    }

    public void setScrollThresholdHorizontal(int threshold){

    }

    public static class Default extends ChildOnScreenScroller {
        final int orientation ;
        int scrollOffset;
        int clampForward = 0;
        int clampBackward = 0;
        public int thresholdScrollVertical = 50;
        public int thresholdScrollHorizontal = 300;
        public int type = TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER;


        public Default(int orientation, int scrollOffset) {
            this.orientation = orientation;
            this.scrollOffset = scrollOffset;
        }

      public void setType(int type) {
        this.type = type;
      }

      public void setClampBackward(int clampBackward) {
        this.clampBackward = clampBackward;
      }

      public void setClampForward(int clampForward) {
        this.clampForward = clampForward;
      }

      @Override
      public void setScrollThresholdHorizontal(int threshold) {
        this.thresholdScrollHorizontal = threshold;
      }

      @Override
      public void setScrollThresholdVertical(int threshold) {
        this.thresholdScrollVertical = threshold;
      }

      public int getScrollOffset() {
            return scrollOffset;
        }

        public void setScrollOffset(int scrollOffset) {
            this.scrollOffset = scrollOffset;
        }

        public Default(int orientation) {
            this(orientation,0);
        }

        @Override
        public boolean requestChildRectangleOnScreen(@NonNull RecyclerViewBase parent, @NonNull View child, @NonNull Rect rect, boolean immediate,int direction) {

            if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_NONE){
              if(LogUtils.isDebug()) {
                LogUtils.e("ChildOnScreenScroller", "requestChildRectangleOnScreen makeChildVisibleType none scroll Type return ");
              }
              return true;
            }

            if (orientation == RecyclerViewBase.VERTICAL) {
                final int parentTop = parent.getPaddingTop();
                int childTop = child.getTop() + rect.top - child.getScrollY();
              final int parentBottom = parent.getHeight() - parent.getPaddingBottom();
              int childBottom = childTop + rect.bottom;
              boolean isChildVisible = childTop > parentTop && childBottom < parentBottom;
              if(LogUtils.isDebug()) {
                LogUtils.d("ChildOnScreenScroller", "requestChildRectangleOnScreen isChildVisible:" + isChildVisible);
              }

              final int parentCenter = (int) (parentTop + parent.getHeight() * 0.5f);
                View focused = null;

                if (focused == null) {
                  if(child instanceof HippyViewGroup &&((HippyViewGroup) child).isFocusScrollTarget()){
                    focused = child;
                    childTop = child.getTop() - child.getScrollY();
                    childBottom = childTop + rect.bottom;
//                      Log.e("ZHAOPENG","11111 child : "+child);
                  }else {
                    focused = child.findFocus();
                    focused = focused != null ? focused : child;
//                      Log.e("ZHAOPENG","22222 child : "+child+",focused:"+focused);
                  }
                }
                if (focused != null) {

                  if(type == TVRecyclerView.REQUEST_CHILD_ON_SCREEN_TYPE_CENTER) {
                    final int childCenter = (int) (childTop + focused.getHeight() * 0.5f);

                    final int dy = childCenter - parentCenter + scrollOffset;

                    final int deltaY = Math.abs(dy);
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
                      LogUtils.d("ChildOnScreenScroller", "handle Scroll deltaY : " + deltaY + ",threshold:" + threshold);
                    }
                    if (Math.abs(deltaY) < threshold) {
                      return true;
                    }
                    exeScrollRecyclerView(parent, child, 0, dy, immediate);
                    return dy != 0;
                  }else{
                    final int dy;
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

                      if(LogUtils.isDebug()) {
                        LogUtils.d("ChildOnScreenScroller", "handle Scroll deltaY : " + dy + ",threshold:" + thresholdScrollVertical);
                        LogUtils.d("ScrollLog", "handle Scroll deltaY : " + dy + ",threshold:" + thresholdScrollVertical);
                      }

                      if(Math.abs(dy) < thresholdScrollVertical){
                        LogUtils.d("ChildOnScreenScroller","return on dy < thresholdScrollVertical dy:"+dy+",thresholdScrollVertical:"+thresholdScrollVertical);
                        return true;
                      }
                      exeScrollRecyclerView(parent, child, 0, dy + scrollOffset, immediate);
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

                      final int dx = childCenter - parentCenter + scrollOffset;

                      exeScrollRecyclerView(parent, child, dx, 0, immediate);
                      return dx != 0;
                    }else{
                      final int dx;
                      {
                        if(direction == FOCUS_LEFT || direction == FOCUS_BACKWARD){
                          //后退
                          dx = Math.min(0,childLeft - (parentLeft + clampBackward) + scrollOffset) ;
                        }else if(direction == FOCUS_RIGHT || direction == FOCUS_FORWARD){
                          //前进
                          dx = Math.max(0,childRight - (parentRight - clampForward) + scrollOffset);
                        }else{
                          dx = 0;
                        }
                        exeScrollRecyclerView(parent, child, dx, 0, immediate);
                        return dx != 0;
                      }
                    }
                }

            }

            return false;
        }


        private void exeScrollRecyclerView(RecyclerViewBase parent, View child, int sx, int sy, boolean immediate){
            if (immediate) {
                parent.scrollBy(sx, sy);
            } else {
                parent.smoothScrollBy(sx, sy);
            }
        }

      @Override
      public boolean smoothScrollToPosition(RecyclerViewBase recyclerView, RecyclerViewBase.State state, int position) {
        // super.smoothScrollToPosition(recyclerView, state, position);
        return false;
      }



    }

}
