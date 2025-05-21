package com.quicktvui.hippyext.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.uimanager.HippyViewController;

import java.lang.reflect.Field;

@HippyController(name = "ProgressBar")
public class ProgressBarViewController extends HippyViewController {
  @Override
  protected View createViewImpl(Context context) {
    return null;
  }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {

    final ProgressBarView progressBar = new ProgressBarView(context, android.R.attr.progressBarStyleHorizontal);

    final LayerDrawable drawable = (LayerDrawable) context.getResources().getDrawable(R.drawable.player_seekbar);
    fixDrawable(drawable,iniProps);

    progressBar.setProgressDrawable(drawable);


    return progressBar;
  }

  private GradientDrawable getDrawableFromClipDrawable(ClipDrawable clipDrawable){
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
        return (GradientDrawable) clipDrawable.getDrawable();
    }else{
      try {
        Field field = clipDrawable.getClass().getSuperclass().getDeclaredField("mDrawable");
        field.setAccessible(true);
        Object o = new Object();
        Log.d("hippy","o is :"+o);
        Object r = field.get(o);
        Log.d("hippy","r is :"+r);
        return null;
      }catch (Throwable t){
        //t.printStackTrace();
        Log.e("hippy","ProgressBarViewController getDrawableFromClipDrawable fail ANDROID_VERSION:"+Build.VERSION.SDK_INT);
        return null;
      }
    }
  }



  private void fixDrawable(LayerDrawable drawable, HippyMap map){
    /*
     * backGroundColor:
     * startColor:
     * endColor:
     * cornerRadius:
     * secondStartColor:
     * secondEndColor:
     */

      if(map.containsKey("backGroundColor")){
        //bg
        GradientDrawable bg = (GradientDrawable) drawable.getDrawable(0);
        int bgColor = Color.parseColor(map.getString("backGroundColor"));
        bg.setColor(bgColor);
      }

      if(map.containsKey("cornerRadius")){
        for(int i = 0; i < drawable.getNumberOfLayers(); i++){
          final Drawable d = drawable.getDrawable(i);
          if(d instanceof GradientDrawable) {
            ((GradientDrawable) d).setCornerRadius(map.getInt("cornerRadius"));
          }else if(d instanceof ClipDrawable){
            GradientDrawable gd =  getDrawableFromClipDrawable((ClipDrawable) d);
            if(gd != null){
              gd.setCornerRadius(map.getInt("cornerRadius"));
            }
          }
        }
      }

      if(map.containsKey("secondColor")){
        ClipDrawable clipDrawable = (ClipDrawable) drawable.getDrawable(1);
        int color = Color.parseColor(map.getString("secondColor"));
        GradientDrawable gd = getDrawableFromClipDrawable(clipDrawable);
        if(gd != null) {
          gd.setColor(color);
        }
      }

      if(map.containsKey("startColor")){
        ClipDrawable clipDrawable = (ClipDrawable) drawable.getDrawable(2);
        GradientDrawable gd = getDrawableFromClipDrawable(clipDrawable);
        if(gd != null) {
          int startColor = Color.parseColor(map.getString("startColor"));
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && map.containsKey("endColor")) {
            int endColor = Color.parseColor(map.getString("endColor"));
            int[] colors = new int[]{startColor, endColor};
            gd.mutate();
            gd.setColors(colors);
          } else {
            gd.setColor(startColor);
          }
        }
      }

  }

  @HippyControllerProps(name = NodeProps.PROGRESS, defaultType = HippyControllerProps.NUMBER)
  public void setProgress(ProgressBar pb,int progress){
    pb.setProgress(progress);
  }

  @HippyControllerProps(name = NodeProps.SECOND_PROGRESS, defaultType = HippyControllerProps.NUMBER)
  public void setSecondProgress(ProgressBar pb,int progress){
    pb.setSecondaryProgress(progress);
  }

  @HippyControllerProps(name = NodeProps.PROGRESS_MAX, defaultType = HippyControllerProps.NUMBER)
  public void setMaxProgress(ProgressBar pb,int max){
    pb.setMax(max);
  }






}
