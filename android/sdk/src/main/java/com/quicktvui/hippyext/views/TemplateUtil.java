package com.quicktvui.hippyext.views;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;

import java.util.ArrayList;
import java.util.List;


public class TemplateUtil {

    public static ColorStateList createColorStateList(HippyMap map,String mapName){


    //            val mNormalState = intArrayOf()
    //            val mFocusedSate = intArrayOf(R.attr.state_focused, R.attr.state_enabled)
            // val [0][R.attr.state_focused, R.attr.state_enabled] : Color.RED
            // val [1][R.attr.state_selected, R.attr.state_enabled] : Color.GREEN
            // val [1][R.attr.state_selected, R.attr.state_enabled] : Color.GREEN
            if(map != null && map.containsKey(mapName)){
                final HippyMap cm = map.getMap(mapName);

                final int size = 5;
                int[][] states  = new int[size][];
                int[] colors = new int[size];

                if(cm.containsKey("selected")){
                    colors[2] = Color.parseColor(cm.getString("selected"));
                    states[2] = new int[]{android.R.attr.state_selected, android.R.attr.state_enabled};
                }

                if(cm.containsKey("focused")){
                    states[0] = new int[]{android.R.attr.state_focused};
                    states[1] = new int[]{android.R.attr.state_focused,android.R.attr.state_enabled};

                    final int colorFocused = Color.parseColor(cm.getString("focused"));
                    colors[0] = colorFocused;
                    colors[1] = colorFocused;
                }

                if(cm.containsKey("normal")){
                    states[3] = new int[]{android.R.attr.state_window_focused};
                    states[4] = new int[]{};
                    final int colorNormal = Color.parseColor(cm.getString("normal"));
                    colors[3] = colorNormal;
                    colors[4] = colorNormal;
                }
                return new ColorStateList(states,colors);
            }
            return null;
        }

    public  static ColorStateList createColorStateListByMap(HippyMap cm){
        if(cm == null){
            return null;
        }
        final int size = 5;
        int[][] states  = new int[size][];
        int[] colors = new int[size];

        if(cm.containsKey("selected")){
            colors[2] = Color.parseColor(cm.getString("selected"));
            states[2] = new int[]{android.R.attr.state_selected, android.R.attr.state_enabled};
        }

        if(cm.containsKey("focused")){
            states[0] = new int[]{android.R.attr.state_focused};
            states[1] = new int[]{android.R.attr.state_focused,android.R.attr.state_enabled};

            final int colorFocused = Color.parseColor(cm.getString("focused"));
            colors[0] = colorFocused;
            colors[1] = colorFocused;
        }

        if(cm.containsKey("normal")){
            states[3] = new int[]{android.R.attr.state_window_focused};
            states[4] = new int[]{};
            final int colorNormal = Color.parseColor(cm.getString("normal"));
            colors[3] = colorNormal;
            colors[4] = colorNormal;
        }
        return new ColorStateList(states,colors);
    }

    public static List<GradientDrawable> createStateListDrawableByMap(HippyMap cm){
            if(cm == null){
                return null;
            }
            List<GradientDrawable> list = new ArrayList<>(2);
            GradientDrawable.Orientation ot = GradientDrawable.Orientation.LEFT_RIGHT;
            if (cm.containsKey("orientation")){
                String orientation = cm.getString("orientation");
                switch (orientation){
                    case "TOP_BOTTOM":
                        ot = GradientDrawable.Orientation.TOP_BOTTOM;
                        break;
                    case "TR_BL":
                        ot = GradientDrawable.Orientation.TR_BL;
                        break;
                    case "RIGHT_LEFT":
                        ot = GradientDrawable.Orientation.RIGHT_LEFT;
                        break;
                    case "BR_TL":
                        ot = GradientDrawable.Orientation.BR_TL;
                        break;case "BOTTOM_TOP":
                        ot = GradientDrawable.Orientation.BOTTOM_TOP;
                        break;
                    case "BL_TR":
                        ot = GradientDrawable.Orientation.BL_TR;
                        break;
                    case "LEFT_RIGHT":
                        ot = GradientDrawable.Orientation.LEFT_RIGHT;
                        break;
                    case "TL_BR":
                        ot = GradientDrawable.Orientation.TL_BR;
                        break;
                }
            }

            float[] radius = new float[4];
            if(cm.containsKey("cornerRadius")){
                HippyArray ha = cm.getArray("cornerRadius");
                if (ha != null && ha.size() == 4){

                    for (int i = 0; i < ha.size(); i++) {
                        radius[i] = ha.getInt(i);
                    }
                }
            }
            int[] colorsNormal = new int[2];
            int[] colorsFocus = new int[2];
            if(cm.containsKey("normal")){
                HippyArray ha = cm.getArray("normal");
                if (ha != null && ha.size()==2){
                    for (int i = 0; i < ha.size(); i++) {
                        colorsNormal[i] = Color.parseColor(ha.getString(i));
                    }
                }

            }
            if(cm.containsKey("focused")){
                HippyArray ha = cm.getArray("focused");
                if (ha != null && ha.size()==2){
                    for (int i = 0; i < ha.size(); i++) {
                        colorsFocus[i] = Color.parseColor(ha.getString(i));
                    }
                }
            }

            GradientDrawable gdNormal = new GradientDrawable(ot,colorsNormal);
            GradientDrawable gdFocus = new GradientDrawable(ot,colorsFocus);

            final float[] radii = new float[]{radius[0],radius[0],radius[1],radius[1],radius[2],radius[2],radius[3],radius[3]};
            gdNormal.setShape(GradientDrawable.RECTANGLE);
            gdNormal.setCornerRadii(radii);

            gdFocus.setShape(GradientDrawable.RECTANGLE);
            gdFocus.setCornerRadii(radii);

            list.add(gdNormal);
            list.add(gdFocus);

            return list;
    }

    public static List<GradientDrawable> createStateListDrawable(HippyMap map, String mapName){
        if(map != null && map.containsKey(mapName)) {
            final HippyMap cm = map.getMap(mapName);
            List<GradientDrawable> list = new ArrayList<>(2);
            GradientDrawable.Orientation ot = GradientDrawable.Orientation.LEFT_RIGHT;
            if (cm.containsKey("orientation")){
                String orientation = cm.getString("orientation");
                switch (orientation){
                    case "TOP_BOTTOM":
                        ot = GradientDrawable.Orientation.TOP_BOTTOM;
                        break;
                    case "TR_BL":
                        ot = GradientDrawable.Orientation.TR_BL;
                        break;
                    case "RIGHT_LEFT":
                        ot = GradientDrawable.Orientation.RIGHT_LEFT;
                        break;
                    case "BR_TL":
                        ot = GradientDrawable.Orientation.BR_TL;
                        break;case "BOTTOM_TOP":
                        ot = GradientDrawable.Orientation.BOTTOM_TOP;
                        break;
                    case "BL_TR":
                        ot = GradientDrawable.Orientation.BL_TR;
                        break;
                    case "LEFT_RIGHT":
                        ot = GradientDrawable.Orientation.LEFT_RIGHT;
                        break;
                    case "TL_BR":
                        ot = GradientDrawable.Orientation.TL_BR;
                        break;
                }
            }

            float[] radius = new float[4];
            if(cm.containsKey("cornerRadius")){
                HippyArray ha = cm.getArray("cornerRadius");
                if (ha != null && ha.size() == 4){

                    for (int i = 0; i < ha.size(); i++) {
                        radius[i] = ha.getInt(i);
                    }
                }
            }
            int[] colorsNormal = new int[2];
            int[] colorsFocus = new int[2];
            if(cm.containsKey("normal")){
                HippyArray ha = cm.getArray("normal");
                if (ha != null && ha.size()==2){
                    for (int i = 0; i < ha.size(); i++) {
                        colorsNormal[i] = Color.parseColor(ha.getString(i));
                    }
                }

            }
            if(cm.containsKey("focused")){
                HippyArray ha = cm.getArray("focused");
                if (ha != null && ha.size()==2){
                    for (int i = 0; i < ha.size(); i++) {
                        colorsFocus[i] = Color.parseColor(ha.getString(i));
                    }
                }
            }

            GradientDrawable gdNormal = new GradientDrawable(ot,colorsNormal);
            GradientDrawable gdFocus = new GradientDrawable(ot,colorsFocus);

            final float[] radii = new float[]{radius[0],radius[0],radius[1],radius[1],radius[2],radius[2],radius[3],radius[3]};
            gdNormal.setShape(GradientDrawable.RECTANGLE);
            gdNormal.setCornerRadii(radii);

            gdFocus.setShape(GradientDrawable.RECTANGLE);
            gdFocus.setCornerRadii(radii);

            list.add(gdNormal);
            list.add(gdFocus);

            return list;
        }
        return null;
    }

    public static Drawable createGradientDrawableDrawable(HippyMap map, String mapName){
        if(map != null && map.containsKey(mapName)) {
            final HippyMap cm = map.getMap(mapName);

            GradientDrawable.Orientation ot = GradientDrawable.Orientation.LEFT_RIGHT;
            if (cm.containsKey("orientation")){
                String orientation = cm.getString("orientation");
                switch (orientation){
                    case "TOP_BOTTOM":
                        ot = GradientDrawable.Orientation.TOP_BOTTOM;
                        break;
                    case "TR_BL":
                        ot = GradientDrawable.Orientation.TR_BL;
                        break;
                    case "RIGHT_LEFT":
                        ot = GradientDrawable.Orientation.RIGHT_LEFT;
                        break;
                    case "BR_TL":
                        ot = GradientDrawable.Orientation.BR_TL;
                        break;case "BOTTOM_TOP":
                        ot = GradientDrawable.Orientation.BOTTOM_TOP;
                        break;
                    case "BL_TR":
                        ot = GradientDrawable.Orientation.BL_TR;
                        break;
                    case "LEFT_RIGHT":
                        ot = GradientDrawable.Orientation.LEFT_RIGHT;
                        break;
                    case "TL_BR":
                        ot = GradientDrawable.Orientation.TL_BR;
                        break;
                }
            }

            float[] radius = new float[4];
            if(cm.containsKey("cornerRadius")){
                HippyArray ha = cm.getArray("cornerRadius");
                if (ha != null && ha.size() == 4){
                    for (int i = 0; i < ha.size(); i++) {
                        radius[i] = ha.getInt(i);
                    }
                }
            }
            int[] colorsFocus = new int[2];

            if(cm.containsKey("color")){
                HippyArray ha = cm.getArray("color");
                if (ha != null && ha.size()==2){
                    for (int i = 0; i < ha.size(); i++) {
                        colorsFocus[i] = Color.parseColor(ha.getString(i));
                    }
                }
            }
            GradientDrawable gdFocus = new GradientDrawable(ot,colorsFocus);
            gdFocus.setShape(GradientDrawable.RECTANGLE);
            gdFocus.setCornerRadii(new float[]{radius[0],radius[0],radius[1],radius[1],radius[2],radius[2],radius[3],radius[3]});
            return gdFocus;
        }
        return null;
    }

    public static int[]  createStateColor(HippyMap map,String name){
        final HippyMap cm = map.getMap(name);
        if(cm != null && cm.size() > 0) {
            int size = 1;
            if(cm.containsKey("focusedColor")){
                size = 2;
            }
            int[] colors = new int[size];
            if(cm.containsKey("normalColor")){
                colors[0] = Color.parseColor(cm.getString("normalColor"));
            }
            if(size == 2){
                colors[1] = Color.parseColor(cm.getString("focusedColor"));
            }
            return colors;
        }else{
            return null;
        }
    }



}
