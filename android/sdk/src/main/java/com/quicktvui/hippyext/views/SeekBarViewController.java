package com.quicktvui.hippyext.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;

import java.lang.reflect.Field;

@HippyController(name = "SeekBar")
public class SeekBarViewController extends ProgressBarViewController {

  private HippyViewEvent mEvent;


  @Override
  protected View createViewImpl(Context context) {
    return null;
  }

  private HippyViewEvent getSeekEvent(){
    if(mEvent == null){
      mEvent = new HippyViewEvent("onSeekBarChange");
    }
    return mEvent;
  }


  protected Drawable crateThumbDrawable(Context context,HippyMap iniProps){
    return (StateListDrawable) context.getResources().getDrawable(R.drawable.seek_thumb);
  }

  protected LayerDrawable crateProgressDrawable(Context context,HippyMap iniProps){
    return (LayerDrawable) context.getResources().getDrawable(R.drawable.player_seekbar);
  }

  @HippyControllerProps(name = "listenProgress", defaultType = HippyControllerProps.BOOLEAN)
  public void setListenProgress(ProgressBar pb,boolean listen){
    if(pb instanceof SeekBarView) {
      ((SeekBarView) pb).setListenProgressEvent(listen);
    }
  }

  @HippyControllerProps(name = "interceptKeyEvent", defaultType = HippyControllerProps.BOOLEAN)
  public void setInterceptKeyEvent(ProgressBar pb,boolean listen){
    if(pb instanceof SeekBarView) {
      ((SeekBarView) pb).setInterceptKeyEvent(listen);
    }
  }



  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
    final SeekBarView seekBar = new SeekBarView(context);
    final LayerDrawable drawable = crateProgressDrawable(context,iniProps);
    fixDrawable(drawable,iniProps);
    seekBar.setProgressDrawable(drawable);
    Drawable thumb = crateThumbDrawable(context,iniProps);

    seekBar.setThumb(thumb);

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar instanceof SeekBarView && ((SeekBarView) seekBar).isListenProgressEvent()) {
          HippyMap hippyMap = new HippyMap();
          hippyMap.pushInt("progress", progress);
          hippyMap.pushBoolean("fromUser", fromUser);
          getSeekEvent().send(seekBar, hippyMap);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });

    if(iniProps.containsKey("thumbSize")){
        int size = iniProps.getInt("thumbSize");
        thumb.setBounds(0,0,size,size);
    }
    return seekBar;
  }

  @Override
  protected void updateExtra(View view, Object object) {
    super.updateExtra(view, object);
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
        Log.e("hippy","SeekBarViewController getDrawableFromClipDrawable fail ANDROID_VERSION:"+Build.VERSION.SDK_INT);
        return null;
      }
    }
  }


  @Override
  public void setProgress(ProgressBar pb, int progress) {
    super.setProgress(pb, progress);
  }


  @Override
  public void setMaxProgress(ProgressBar pb, int max) {
    super.setMaxProgress(pb, max);
  }

  @Override
  public void setSecondProgress(ProgressBar pb, int progress) {
    super.setSecondProgress(pb, progress);
  }


  @HippyControllerProps(name = "keyProgressIncrement", defaultType = HippyControllerProps.NUMBER)
  public void setKeyProgressIncrement(ProgressBar pb,int number){
    if(pb instanceof SeekBarView) {
      ((SeekBarView) pb).setKeyProgressIncrement(number);
    }
  }


}
