package com.tencent.mtt.hippy.views.list;

import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;


import android.support.v7.widget.LinearLayoutManager;

import com.tencent.mtt.hippy.BuildConfig;
import com.tencent.mtt.hippy.utils.LogUtils;


/**
 * @author zhaopeng
 * @version 1.0
 * @title
 * @description
 * @company
 * @created 2017/8/22
 * @changeRecord [修改记录] <br/>
 * 2017/8/22 ：created
 */


public class NegativeLongKeyFlinger extends RecycleViewFlinger {

    public static final String TAG = "LongKeyFlinger";

    private int time_interval = 500;

//    private int time_interval = 1000;

    int flingSpeed = 500;


    public void setFlingSpeed(int flingSpeed) {
        this.flingSpeed = flingSpeed;
    }

    public void setKeyReleaseTimeInterval(int time_interval) {
        this.time_interval = Math.min(100,time_interval);
    }

    public NegativeLongKeyFlinger(IRecyclerView flyableView) {
        this(flyableView,150);
    }

  public NegativeLongKeyFlinger(IRecyclerView flyableView,int interval) {
    super(flyableView);
    setKeyReleaseTimeInterval(interval);
  }


    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if(time_interval < 0){
            return false;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(mIFlyableView.isInLayout()){
                if(BuildConfig.DEBUG) {
                    Log.e(TAG, "return : isInLayout");
                }
                return true;
            }
        }

        final long now = SystemClock.uptimeMillis();
        int vector = 0;
        if(isVertical){
            if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP ){
                vector = -1;
            }else if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN){
                vector = 1;
            }
        }else{
            if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ){
                vector = -1;
            }else if( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT){
                vector = 1;
            }
        }
        if(vector == 0){
            //不是方向键
            return false;
        }


        if(event.getAction() == KeyEvent.ACTION_DOWN){
            if(BuildConfig.DEBUG){
            }
            //长按
            if(mLongPress == null){
//                if(BuildConfig.DEBUG) {
//                    Log.e(TAG, "初始key事件 : ");
//                }
                mLongPress = new LongPress(now);
                return false;
            }else{
                //长按过程中，只处理一定时间间隔内的请求
                if( mLongPress.down(now).pressedTime < time_interval){
//                    if(BuildConfig.DEBUG) {
//                        Log.e(TAG, "丢弃key事件 : "+mLongPress);
//                    }
                    return true;
                }

                mLongPress.up(now);

//                if(BuildConfig.DEBUG) {
//                    Log.e(TAG, " 长按事件，执行Fling : "+mLongPress);
//                }

                return false;
            }


        }else if(event.getAction() == KeyEvent.ACTION_UP) {
            if (mLongPress != null) {
                //抬起
                mLongPress.up(now);


//                if (BuildConfig.DEBUG) {
//                    Log.e(TAG, "按键抬起 ");
//                }
                //单键
                mLongPress = null;
                return false;

            }
        }
        return false;

    }

  @Override
  public View focusSearch(LinearLayoutManager lm, View focused, int direction) {
//      if(LogUtils.isDebug()){
//        Log.d(TAG,"FocusSearch called longKey:"+isOnLongKeyPressed());
//      }
    return null;
  }

  public boolean isOnLongKeyPressed(){
        return mLongPress != null && mLongPress.pressedTime > time_interval;
    }



}
