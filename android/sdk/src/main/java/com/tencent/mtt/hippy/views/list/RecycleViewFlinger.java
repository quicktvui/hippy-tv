package com.tencent.mtt.hippy.views.list;

import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Interpolator;


import android.support.v7.widget.LinearLayoutManager;



/**
 * @author zhaopeng
 * @version 1.0
 * @title
 * @description
 * @company
 * @created 2017/8/9
 * @changeRecord [修改记录] <br/>
 * 2017/8/9 ：created
 */


public abstract class RecycleViewFlinger {

    public static final String TAG = "SmoothFlinger";

    boolean isVertical = true;

    public long lastKeyTime;


    Interpolator mInterpolator;


    long fullFlingDuration = 1 * 1000;

    long singleKeyDuration = 100;

    LongPress mLongPress;

    IRecyclerView mIFlyableView;

    float  flingSmooth = 1;

    public RecycleViewFlinger(IRecyclerView view) {
        this.mIFlyableView = view;
    }

    public void setVertical(boolean isVertical) {
        this.isVertical = isVertical;
    }

    public abstract boolean dispatchKeyEventPreIme(KeyEvent event);

  public abstract View focusSearch(LinearLayoutManager tvListView, View focused, int direction);


  static class LongPress{

        static int FLING_VELOCITY = 1000;

        private long mLastKeyDownTime = 0;

        final long startTime;

        long pressedTime;

        long downDeltaTime = 0;

        boolean discarded = false;

        LongPress(long startTime) {
            this.startTime = startTime;
            mLastKeyDownTime = startTime;
        }

        LongPress down(long now){
            downDeltaTime = (now - mLastKeyDownTime);

            pressedTime += downDeltaTime;

            mLastKeyDownTime = now;
            return this;
        }

        public long getPressedTime() {
            return pressedTime;
        }

        long getLastDownTimeDelta(){
            return downDeltaTime;
        }


        int getFlingVelocity(){
            return FLING_VELOCITY;
        }

        LongPress up(long now){
            reset();
            discarded = true;
            return this;
        }

        void reset(){
            pressedTime = 0;
        }

        @Override
        public String toString() {
            return "LongPress{" +
                    ", startTime=" + startTime +
                    ", pressedTime=" + pressedTime +
                    ", downDeltaTime=" + downDeltaTime +
                    '}';
        }
    }

  public interface IRecyclerView{
    boolean isInLayout();
  }



}
