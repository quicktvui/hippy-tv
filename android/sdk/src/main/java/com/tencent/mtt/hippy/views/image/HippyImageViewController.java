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
package com.tencent.mtt.hippy.views.image;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.view.View;

import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.R;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.ImageNode;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.dom.node.StyleNode;
import com.tencent.mtt.hippy.uimanager.HippyViewController;

@SuppressWarnings({"deprecation", "unused"})
@HippyController(name = HippyImageViewController.CLASS_NAME)
public class HippyImageViewController extends HippyViewController<HippyImageView> {

  public static final String CLASS_NAME = "Image";

  @Override
  protected StyleNode createNode(boolean virtual) {
    return new ImageNode(virtual);
  }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
    HippyImageView imageView = new HippyImageView(context);
    if (iniProps != null) {
      imageView.setInitProps(iniProps);
      if (iniProps.containsKey("enablePostTask")) {
        imageView.setEnablePostTask(true);
      }
    }

    return imageView;
  }

  @SuppressWarnings("unused")
  @Override
  protected View createViewImpl(Context context) {
    return new HippyImageView(context);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = NodeProps.CUSTOM_PROP_IMAGE_TYPE, defaultType = HippyControllerProps.STRING)
  public void setImageType(HippyImageView hippyImageView, String type) {
    hippyImageView.setImageType(type);
  }

  /**
   * zhaopeng add 设置图片的用途
   * @param hippyImageView
   * @param type
   */
  @SuppressWarnings("unused")
  @HippyControllerProps(name = "roleType", defaultType = HippyControllerProps.STRING)
  public void setRoleType(HippyImageView hippyImageView, String type) {
    switch (type) {
      case "cover":
        hippyImageView.setRoleType(HippyImageView.RoleType.COVER);
        break;
      case "icon":
        hippyImageView.setRoleType(HippyImageView.RoleType.ICON);
        break;
      default:
        hippyImageView.setRoleType(HippyImageView.RoleType.UNDEFINE);
        break;
    }
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "src", defaultType = HippyControllerProps.STRING)
  public void setUrl(HippyImageView hippyImageView, String url) {
    if (!TextUtils.isEmpty(url) && url.startsWith("icon://")) {
      if (setApkIcon(hippyImageView, url)) {
        return;
      }
      try {
        PackageManager pm = hippyImageView.getContext().getPackageManager();
        ApplicationInfo pi = pm.getApplicationInfo(url.replace("icon://", ""), 0);
        Drawable iconDrawable = pi.loadIcon(pm);
        if (iconDrawable == null) iconDrawable = pi.loadLogo(pm);
        hippyImageView.setBackgroundDrawable(iconDrawable);
      } catch (Exception ignored) {
      }
      return;
    }
    hippyImageView.setUrl(getInnerPath((HippyInstanceContext) hippyImageView.getContext(), url));
  }

  private boolean setApkIcon(HippyImageView hippyImageView, String url) {
    if (!TextUtils.isEmpty(url) && url.startsWith("icon://") && url.endsWith(".apk")) {
      Drawable img = null;
      RoundedBitmapDrawable rbd = null;
      try {
        PackageManager pm = hippyImageView.getContext().getPackageManager();
        String apkPath = url.replace("icon://", "");
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA);
        if (info != null) {
          boolean isInstalled = isInstalled(pm, info.packageName);
          ApplicationInfo queryInfo = info.applicationInfo;
          if (isInstalled) {
            queryInfo = pm.getApplicationInfo(info.packageName, PackageManager.GET_META_DATA);
          } else {
            queryInfo.publicSourceDir = apkPath;
          }
          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            img = pm.getApplicationBanner(queryInfo);
          }
          if (img == null) {
            img = pm.getApplicationIcon(queryInfo);
          }
        }
      } catch (Exception ignored) {
      }

      try {
        if (img == null) {
          img = hippyImageView.getContext().getResources().getDrawable(R.drawable.apk_error_icon);
        }
        if (img != null) {
          rbd = RoundedBitmapDrawableFactory.create(hippyImageView.getContext().getResources(), parseBitmap(img));
        }
        if (rbd != null) {
          rbd.setCornerRadius(25);
          hippyImageView.setBackgroundDrawable(rbd);
        }
      } catch (Exception e) {
      }
      return true;
    }
    return false;
  }

  private Bitmap parseBitmap(Drawable drawable) {
    if (drawable == null) {
      return null;
    }
    Bitmap bitmap = null;
    try {
      if(drawable instanceof BitmapDrawable) {
        bitmap = ((BitmapDrawable) drawable).getBitmap();
      }
      if (bitmap == null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) drawable;
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            adaptiveIconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            adaptiveIconDrawable.draw(canvas);
          }
        }
      }
    } catch (Exception e) {
    }
    return bitmap;
  }

  private boolean isInstalled(PackageManager pm, String packageName) {
    if (packageName != null) {
      try {
        PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        if (info != null) {
          return true;
        }
      } catch (Exception e) {
      }
    }
    return false;
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "tintColor", defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setTintColor(HippyImageView hippyImageView, int tintColor) {
    hippyImageView.setTintColor(tintColor);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = NodeProps.RESIZE_MODE, defaultType = HippyControllerProps.STRING, defaultString = "fitXY")
  public void setResizeMode(HippyImageView hippyImageView, String resizeModeValue) {
    if ("contain".equals(resizeModeValue)) {
      // 在保持图片宽高比的前提下缩放图片，直到宽度和高度都小于等于容器视图的尺寸
      // 这样图片完全被包裹在容器中，容器中可能留有空白
      hippyImageView.setScaleType(HippyImageView.ScaleType.CENTER_INSIDE);
    } else if ("cover".equals(resizeModeValue)) {
      // 在保持图片宽高比的前提下缩放图片，直到宽度和高度都大于等于容器视图的尺寸
      // 这样图片完全覆盖甚至超出容器，容器中不留任何空白
      hippyImageView.setScaleType(HippyImageView.ScaleType.CENTER_CROP);
    } else if ("center".equals(resizeModeValue)) {
      // 居中不拉伸
      hippyImageView.setScaleType(HippyImageView.ScaleType.CENTER);
    } else if ("origin".equals(resizeModeValue)) {
      // 不拉伸，居左上
      hippyImageView.setScaleType(HippyImageView.ScaleType.ORIGIN);
    } else if ("repeat".equals(resizeModeValue)) {
      hippyImageView.setScaleType(HippyImageView.ScaleType.REPEAT);
    } else {
      // stretch and other mode
      // 拉伸图片且不维持宽高比，直到宽高都刚好填满容器
      hippyImageView.setScaleType(HippyImageView.ScaleType.FIT_XY);
    }
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = NodeProps.BACKGROUND_COLOR, defaultType = HippyControllerProps.NUMBER, defaultNumber = Color.TRANSPARENT)
  public void setBackgroundColor(HippyImageView view, int backgroundColor) {
    view.setBackgroundColor(backgroundColor);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "defaultSource", defaultType = HippyControllerProps.STRING)
  public void setDefaultSource(HippyImageView hippyImageView, String defaultSource) {
    hippyImageView.setHippyViewDefaultSource(
      getInnerPath((HippyInstanceContext) hippyImageView.getContext(), defaultSource));
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "capInsets", defaultType = HippyControllerProps.MAP)
  public void setCapInsets(HippyImageView hippyImageView, HippyMap capInsetsMap) {
    if (capInsetsMap == null) {
      hippyImageView.setNinePatchCoordinate(true, 0, 0, 0, 0);
    } else {
      int topCoordinate = capInsetsMap.getInt("top");
      int leftCoordinate = capInsetsMap.getInt("left");
      int bottomCoordinate = capInsetsMap.getInt("bottom");
      int rightCoordinate = capInsetsMap.getInt("right");
      hippyImageView.setNinePatchCoordinate(false, leftCoordinate, topCoordinate, rightCoordinate,
        bottomCoordinate);
    }
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "onLoad", defaultType = HippyControllerProps.BOOLEAN)
  public void setOnLoad(HippyImageView hippyImageView, boolean enable) {
    hippyImageView.setImageEventEnable(HippyImageView.ImageEvent.ONLOAD.ordinal(), enable);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "onLoadEnd", defaultType = HippyControllerProps.BOOLEAN)
  public void setOnLoadEnd(HippyImageView hippyImageView, boolean enable) {
    hippyImageView.setImageEventEnable(HippyImageView.ImageEvent.ONLOAD_END.ordinal(), enable);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "onLoadStart", defaultType = HippyControllerProps.BOOLEAN)
  public void setOnLoadStart(HippyImageView hippyImageView, boolean enable) {
    hippyImageView.setImageEventEnable(HippyImageView.ImageEvent.ONLOAD_START.ordinal(), enable);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "onError", defaultType = HippyControllerProps.BOOLEAN)
  public void setOnError(HippyImageView hippyImageView, boolean enable) {
    hippyImageView.setImageEventEnable(HippyImageView.ImageEvent.ONERROR.ordinal(), enable);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "enableFade", defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setFadeEnabled(HippyImageView hippyImageView, boolean enable) {
    hippyImageView.setFadeEnabled(enable);
  }

  @SuppressWarnings("unused")
  @HippyControllerProps(name = "fadeDuration", defaultType = HippyControllerProps.NUMBER)
  public void setFadeDuration(HippyImageView hippyImageView, int fadeDuration) {
    hippyImageView.setFadeDuration(fadeDuration);
  }

//  @SuppressWarnings("unused")
//  @HippyControllerProps(name = "enablePostTask", defaultType = HippyControllerProps.BOOLEAN)
//  public void setEnablePostTask(HippyImageView hippyImageView, boolean enable) {
//    hippyImageView.setEnablePostTask(enable);
//  }

  /*** zhaopeng add8***/
  @HippyControllerProps(name = "loadDelay", defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setDelayLoad(HippyImageView hippyImageView, int delay) {
    hippyImageView.setDelayLoad(delay);
  }

  /*** zhaopeng add8***/
  @HippyControllerProps(name = "loadImgDelay", defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setDelayLoadImage(HippyImageView hippyImageView, int delay) {
    hippyImageView.setDelayLoad(delay);
  }

  /*** zhaopeng add8***/
  @HippyControllerProps(name = "postDelay", defaultType = HippyControllerProps.NUMBER, defaultNumber = -1)
  public void setPostDelay(HippyImageView hippyImageView, int delay) {
    hippyImageView.setEnablePostTask(delay > 0);
    hippyImageView.setDelayLoad(delay);
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_STYLE, defaultType = HippyControllerProps.STRING, defaultString = "solid")
  public void setFocusBorderStyle(HippyImageView view, String style) {
    if (view != null) {
      view.setFocusBorderEnable(!"none".equals(style));
    }
  }

  /**
   * touch/click
   **/
  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_ENABLE, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = true)
  public void setFocusBorderEnable(HippyImageView view, boolean enable) {
    if (view != null) {
      view.setFocusBorderEnable(enable);
    }
  }

  @HippyControllerProps(name = NodeProps.FOCUS_BORDER_TYPE, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setFocusBorderType(HippyImageView view, int type) {
    if (view != null) {
      view.setFocusBorderType(type);
    }
  }

  @HippyControllerProps(name = NodeProps.COLOR_FILTER, defaultType = HippyControllerProps.NUMBER, defaultNumber = 0)
  public void setColorFilter(HippyImageView view, int colorType) {
    if (view != null) {
      Paint paint = new Paint();
      ColorMatrix cm = new ColorMatrix();
      if (colorType != 0) {
        cm.setSaturation(0f);
      } else {
        cm.setSaturation(1f);
      }
      paint.setColorFilter(new ColorMatrixColorFilter(cm));
      view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }
  }

  @Override
  public void dispatchFunction(HippyImageView view, String functionName, HippyArray var) {
    super.dispatchFunction(view, functionName, var);
    switch (functionName) {
      case "setSrc":
        this.setUrl(view, var.getString(0));
        break;
      case "resizeMode":
        this.setResizeMode(view, var.getString(0));
        break;
    }
  }
}
