package com.tencent.mtt.hippy.utils.mouse;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by jiangtao on2020/5/8
 */
public class GenericMotionUtil {

    private final static View.OnGenericMotionListener listener = (v, event) -> {
        if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER && v.isFocusable()){
            v.requestFocus();
        }
        return false;
    };

    public static void setOnGenericMotionListener(View genericView){
        try {
            if (genericView!=null) genericView.setOnGenericMotionListener(listener);
        }catch (Error e){
            e.printStackTrace();
        }

    }
}
