package com.quicktvui.hippyext.views;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.utils.PixelUtil;

public class EngineRootView extends FrameLayout {

  private static int translateX = 0;
  private static int translateY = 0;

  int overrideWidth = 0;
  int overrideHeight = 0;

  public EngineRootView(@NonNull Context context) {
    super(context);
    init();
  }

  public EngineRootView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public EngineRootView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public EngineRootView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  void init(){
//    Log.i("EngineRootView","PixelUtil.getsScreenAdaptType() : "+PixelUtil.getsScreenAdaptType());
//    Toast.makeText(getContext(), "width:"+PixelUtil.getScreenWidth()+",height:"+PixelUtil.getScreenHeight()+",ratio:"+PixelUtil.getScreenAspectRatio(), Toast.LENGTH_LONG).show();
    if (PixelUtil.getsScreenAdaptType() == PixelUtil.SCREEN_ADAPT_TYPE_SCALE_WIDTH) {
      try {
        Activity activity;
        if(getContext() instanceof Activity) {
          activity = (Activity) getContext();
        }else if(getContext() instanceof HippyInstanceContext) {
          activity = (Activity) ((HippyInstanceContext) getContext()).getBaseContext();
        }else {
          activity = null;
          Log.w("EngineRootView ","screen adapter error cant find activity context:"+getContext());
          return;
        }
//        Log.i("EngineRootView"," base context:"+activity);
        if(activity != null){
//          int oldWidth = PixelUtil.getScreenWidth();
//          int oldHeight = PixelUtil.getScreenHeight();
          PixelUtil.adjustScreenSizeByActivity(activity);
//          Log.i("EngineRootView"," after adjustScreenSizeByActivity screenWidth:"+PixelUtil.getScreenWidth()
//            +" screenHeight:"+PixelUtil.getScreenHeight()+",oldWidth:"+oldWidth+",oldHeight:"+oldHeight);
        }
        final float currentAspectRatio = PixelUtil.getScreenAspectRatio();
        //因为当前适配比例是16/9,如果不是16/9，则将页面整体居中
        if (currentAspectRatio > PixelUtil.ASPECT_RATIO_16_9) {//相对16：9偏宽
          overrideHeight = PixelUtil.getScreenHeight(); // 1080
          float rate = PixelUtil.getDevHeightF() / PixelUtil.getScreenHeight(); //1.00f
          overrideWidth = (int) (PixelUtil.getDevWidthF() / rate);
          translateX = (int) ((PixelUtil.getScreenWidth() - overrideWidth) * 0.5f);
          //1.0465
//        setBackgroundColor(Color.GREEN);
        } else if (currentAspectRatio < PixelUtil.ASPECT_RATIO_16_9) {//相对16：9偏长
          overrideWidth = PixelUtil.getScreenWidth();
          float rate = PixelUtil.getDevWidthF() / PixelUtil.getScreenWidth();
          overrideHeight = (int) (PixelUtil.getDevHeightF() / rate);
          translateY = (int) ((PixelUtil.getScreenHeight() - overrideHeight) * 0.5f);
//        setBackgroundColor(Color.GREEN);
        }
      }catch (Throwable t){
        t.printStackTrace();
        Log.e("EngineRootView","init error:"+t.getMessage());
      }
    }
    Log.i("EngineRootView","init overrideWidth:"+overrideWidth+" overrideHeight:"+overrideHeight+" translateX:"+translateX+" translateY:"+translateY);
  }

  public static int getScreenTranslateX() {
    return translateX;
  }

  public static int getScreenTranslateY() {
    return translateY;
  }


//  @Override
//  protected void dispatchDraw(Canvas canvas) {
//    if (translateX != 0 || translateY != 0) {
//      canvas.save();
//      canvas.translate(translateX,translateY);
//      super.dispatchDraw(canvas);
//      canvas.restore();
//    }else{
//      super.dispatchDraw(canvas);
//    }
//  }

  @Override
  public String toString() {
    return "EngineRootView{" +
      "translateX=" + translateX +
      ", translateY=" + translateY +
      ", overrideWidth=" + overrideWidth +
      ", overrideHeight=" + overrideHeight +
      ", super=" + super.toString() +
      '}';
  }
}
