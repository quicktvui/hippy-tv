/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import java.util.Objects;

/**
 * @Description: TODO
 * @author: edsheng
 * @date: 2017/11/24 15:54
 * @version: V1.0
 */

public class PixelUtil
{
	static DisplayMetrics sMetrics = null;

	public static final int SCREEN_ADAPT_TYPE_SCALE_WIDTH = 0;

  public static final int SCREEN_ADAPT_TYPE_ANDROID_ORIGIN = 1;

  public static final int SCREEN_ADAPT_TYPE_NONE = 2;

  public static final float ASPECT_RATIO_16_9 = computeScreenAspectRatio(16,9);
  public static final float ASPECT_RATIO_9_16 = computeScreenAspectRatio(9,16);

	static int sScreenAdaptType = SCREEN_ADAPT_TYPE_SCALE_WIDTH;

	private static float scaleX = 1;

	private static float devWidth = 1920f;
	private static int screenWidth = 0;
	private static float devHeight = 1080f;
	private static int screenHeight = 0;
  private static Context context;

  public static int getsScreenAdaptType() {
    return sScreenAdaptType;
  }

  public static float computeScreenAspectRatio(int width,int height){
    if (height == 0) {
      return 0;
    }
    return (float) Math.round(((float) width / height) * 100.0f) / 100.0f;
  }


  public static float getScreenAspectRatio(){
//    int[] screen = getScreenHeight(activity);
//    return computeScreenAspectRatio(screen[0],screen[1]);
    return computeScreenAspectRatio(getScreenWidth(),getScreenHeight());
  }

  /**
   * 当屏幕缩放时translate的值不为0，代表屏幕不是16:9的比例，需要将页面整体居中展示，这时需要同步缩放尺寸
   */
  public static float scale4translate = 1f;

  public static float getScaleX() {
    return scaleX;
  }

	private static DisplayMetrics getMetrics()
	{
		if (sMetrics == null)
		{
			sMetrics = ContextHolder.getAppContext().getResources().getDisplayMetrics();
			int screenWidth = sMetrics.widthPixels;

			scaleX = screenWidth / (float)devWidth;
      Log.i("PixelUtil","init scale value:"+scaleX);
		}
		return sMetrics;
	}

  public static int getScreenWidth(){
    return screenWidth > 0 ? screenWidth :  getMetrics().widthPixels;
  }

  public static int getScreenHeight(){
    return screenHeight > 0 ? screenHeight :  getMetrics().heightPixels;
  }

  public static void setsScreenAdaptType(int sScreenAdaptType) {
    PixelUtil.sScreenAdaptType = sScreenAdaptType;
  }

  public static void adjustScreenSizeByActivity(Activity activity){
    if(activity != null) {
      int [] screen = getScreenHeight(activity);
      screenWidth = screen[0];
      screenHeight = screen[1];
    }
  }

  public static int[] getScreenHeight(Activity activity) {
    PixelUtil.context = context;
    int widthPx = 0;
    int heightPx = 0;
//    DisplayMetrics display = context.getResources().getDisplayMetrics();
    WindowManager mWindowManager = Objects.requireNonNull(activity.getWindowManager());
    Resources mResources = Objects.requireNonNull(activity).getResources();
    Display mDisplay = null;
    DisplayMetrics mDisplayMetrics = null;
    if (mWindowManager != null) {
      mDisplay = mWindowManager.getDefaultDisplay();
    }
    if (mResources != null) {
      mDisplayMetrics = mResources.getDisplayMetrics();
    }
    if (!(mDisplay == null || mDisplayMetrics == null)) {
      if (Build.VERSION.SDK_INT >= 17) {
        Point realSize = new Point();
        mDisplay.getRealSize(realSize);
        widthPx = realSize.x;
        heightPx = realSize.y;
      } else {
        try {
          widthPx = ((Integer) Display.class.getMethod("getRawWidth", new Class[0]).invoke(mDisplay, new Object[0])).intValue();
          heightPx = ((Integer) Display.class.getMethod("getRawHeight", new Class[0]).invoke(mDisplay, new Object[0])).intValue();
        } catch (Exception e) {
          widthPx = mDisplayMetrics.widthPixels;
          heightPx = mDisplayMetrics.heightPixels;
        }
      }
    }
    return new int[]{widthPx, heightPx};
  }

  public static int getDevWidth() {
    return (int) devWidth;
  }

  public static float getDevHeightF() {
    return devHeight;
  }

  public static float getDevWidthF(){
    return devWidth;
  }

  public static void setDevWidth(int devWidth) {
    PixelUtil.devWidth = devWidth;
    {
      sMetrics = ContextHolder.getAppContext().getResources().getDisplayMetrics();
      int screenWidth = sMetrics.widthPixels;

      scaleX = screenWidth / (float)devWidth;
      Log.i("PixelUtil","setDevWidth scale value:"+scaleX);
    }
  }

  public static float getDensity(){
    return getMetrics().density;
  }

  public static int dp2pxInt(float value)
  {
    return (int)dp2px(value);
  }

  public static float dp2px(float value)
	{
    if (sMetrics == null){
      getMetrics();
    }
	    if(value == 0) return 0;
    switch (sScreenAdaptType){
      case SCREEN_ADAPT_TYPE_SCALE_WIDTH :
        return scaleX * scale4translate * value;
      case SCREEN_ADAPT_TYPE_NONE :
        return value;
      default:
        return  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getMetrics());
    }
	}

	public static float dp2px(double value)
	{
		return dp2px((float)value);
	}

	public static float px2dp(float value)
	{
    switch (sScreenAdaptType){
      case SCREEN_ADAPT_TYPE_SCALE_WIDTH :
      case SCREEN_ADAPT_TYPE_NONE :
        return value ;
      default:
        return  value / getMetrics().density + 0.5f;
    }
	}

	public static float sp2px(float value)
	{
    switch (sScreenAdaptType){
      case SCREEN_ADAPT_TYPE_SCALE_WIDTH :
        return scaleX * value;
      case SCREEN_ADAPT_TYPE_NONE :
        return value;
      default:
        return  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getMetrics());
    }
	}

	public static float px2sp(float value)
	{
//		return value / getMetrics().scaledDensity + 0.5f;
    switch (sScreenAdaptType){
      case SCREEN_ADAPT_TYPE_SCALE_WIDTH :
      case SCREEN_ADAPT_TYPE_NONE :
        return value  ;
      default:
        return  value / getMetrics().scaledDensity + 0.5f;
    }
	}




}
