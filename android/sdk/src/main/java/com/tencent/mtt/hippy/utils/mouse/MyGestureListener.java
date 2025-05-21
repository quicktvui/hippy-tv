package com.tencent.mtt.hippy.utils.mouse;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * 手势检测类
 * 完成了方向检测，可直接通过fling获取手势滑动方向
 *
 * @author zy
 */
public class MyGestureListener implements GestureDetector.OnGestureListener {

    /**
     * 滑动方向枚举类
     */
    public enum Orientation {
        DOWN, UP, LEFT, RIGHT
    }

    /**
     * 默认需要滑动的距离
     */
    private static final int DEFAULT_DISTANCE = 20;

    /**
     * X轴方向滑动生效的距离
     */
    private int mDistanceX = DEFAULT_DISTANCE;

    /**
     * Y轴方向滑动生效的距离
     */
    private int mDistanceY = DEFAULT_DISTANCE;

    public MyGestureListener() {
    }

    /**
     * 构造函数
     *
     * @param distance X轴、Y轴滑动生效的距离
     */
    public MyGestureListener(int distance) {
        this.mDistanceX = distance;
        this.mDistanceY = distance;
    }

    /**
     * 构造函数
     *
     * @param distanceX X轴滑动生效的距离
     * @param distanceY Y轴滑动生效的距离
     */
    public MyGestureListener(int distanceX, int distanceY) {
        this.mDistanceX = distanceX;
        this.mDistanceY = distanceY;
    }

    /**
     * 设置滑动最小生效距离
     *
     * @param distance X轴、Y轴滑动生效的距离
     */
    public void setDistance(int distance) {
        this.mDistanceX = distance;
        this.mDistanceY = distance;
    }

    /**
     * 设置滑动最小生效距离
     *
     * @param distanceX X轴滑动生效的距离
     * @param distanceY Y轴滑动生效的距离
     */
    public void setDistance(int distanceX, int distanceY) {
        this.mDistanceX = distanceX;
        this.mDistanceY = distanceY;
    }


    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //注，up抬起的时候需要有速度才能识别成滑动。若停止滑动再抬起几乎无法识别成滑动。
        float x1 = e1.getX();
        float y1 = e1.getY();

        float x2 = e2.getX();
        float y2 = e2.getY();

        float x = x2 - x1;
        float y = y2 - y1;

        //是否横向。是否是边缘滑动就需要依据实际情况判断 x、y的大小来判断是否是边缘。
        //当前算法只能判断是向哪个方向滑动
        if (Math.abs(x) > Math.abs(y)) {
            if (x > mDistanceX) {
                return fling(Orientation.RIGHT, x, e1, e2, velocityX, velocityY);
            } else if (-x > mDistanceX) {
                return fling(Orientation.LEFT, x, e1, e2, velocityX, velocityY);
            }
        } else {
            if (y > mDistanceY) {
                return fling(Orientation.DOWN, y, e1, e2, velocityX, velocityY);
            } else if (-y > mDistanceY) {
                return fling(Orientation.UP, y, e1, e2, velocityX, velocityY);
            }
        }

        return false;
    }

    /**
     * 返回手势方向。
     * 目前只计算了方向和距离，其余参数也给返回了 可自行计算判断。
     *
     * @param orientation 滑动方向：DOWN-向下, UP-向上, LEFT-向左, RIGHT-向右
     * @param distance    滑动方向上结束点到开始点的距离
     * @param e1          MotionEvent 开始点
     * @param e2          MotionEvent 结束点
     * @param velocityX   X方向速度
     * @param velocityY   Y方向速度
     */
    public boolean fling(Orientation orientation, float distance, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}
