package com.tencent.mtt.hippy.utils.mouse;

import android.view.MotionEvent;

/**
 * 功能描述：HoverManager
 *
 * @author yuanyuan.lei
 * @since 2021-01-21 14:35
 */
public class HoverManager {

    private static HoverManager sHoverManager = null;
    private MotionEvent mMotionEvent = null;
    private boolean mIsMouseEnter = false;

    public synchronized static HoverManager getInstance() {
        if (sHoverManager == null) {
            sHoverManager = new HoverManager();
        }
        return sHoverManager;
    }

    public void setMouseEnter(boolean isMouseEnter) {
        mIsMouseEnter = isMouseEnter;
    }

    /**
     * 获取空鼠状态
     *
     * @return true表示空鼠进入，false表示空鼠退出
     */
    public boolean getMouseStatus() {
        return mIsMouseEnter;
    }

    public void dispatchGenericMotionEvent(MotionEvent event) {
        mIsMouseEnter = true;
        mMotionEvent = event;
    }
}
