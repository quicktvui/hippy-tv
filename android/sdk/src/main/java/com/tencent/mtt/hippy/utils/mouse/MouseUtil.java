package com.tencent.mtt.hippy.utils.mouse;

import android.view.MotionEvent;
import android.view.View;

/**
 * 空鼠工具类
 *
 * @author zy
 */
public class MouseUtil {

    /**
     * 设置view的click、focus、touch的状态
     *
     * @param view  需要设置的view
     * @param focus true；上焦，false：不上焦
     */
    public static void setViewMouseStatus(View view, Boolean focus) {
        if (view != null) {
            view.setFocusable(focus);
            view.setFocusableInTouchMode(focus);
            view.setClickable(focus);
        }
    }

    /**
     * 设置view的默认touch事件，某需要点击两次才能响应时可设置一下。
     * 注：该方法会占用Touch事件，若需要自己处理touch事件则不要设置该方法
     *
     * @param view 需要设置的view
     */
    public static void setViewDefaultTouch(View view) {
        if (view != null) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.requestFocus();
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 设置默认的Hover监听，默认Enter时进行上焦
     *
     * @param view           需要适配的view
     * @param interceptHover 是否需要消费事件
     */
    public static void setViewDefaultHover(View view, boolean interceptHover) {
        if (view != null) {
            view.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                        v.requestFocus();
                        return interceptHover;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 设置默认的Hover监听，默认Enter时进行上焦
     *
     * @param view 需要适配的view
     */
    public static void setViewDefaultHover(View view) {
        setViewDefaultHover(view, false);
    }

    /**
     * 设置默认的GenericMotion监听，默认Enter时进行上焦
     *
     * @param view            需要适配的view
     * @param interceptMotion 是否需要消费事件
     */
    public static void setViewDefaultGenericMotion(View view, boolean interceptMotion) {
        if (view != null) {
            view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
                @Override
                public boolean onGenericMotion(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                        v.requestFocus();
                        return interceptMotion;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 设置默认的GenericMotion监听，默认Enter时进行上焦
     *
     * @param view 需要适配的view
     */
    public static void setViewDefaultGenericMotion(View view) {
        setViewDefaultGenericMotion(view, false);
    }

}
