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
package com.tencent.mtt.hippy.views.modal;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.HippyInstanceLifecycleEventListener;
import com.tencent.mtt.hippy.utils.ContextHolder;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

import java.lang.reflect.Field;
import java.util.ArrayList;

@SuppressWarnings({"unused"})
public class HippyModalHostView extends HippyViewGroup implements
  HippyInstanceLifecycleEventListener {

  @Override
  public void onInstanceLoad(int instanceId) {
    try {
      showOrUpdate();
    } catch (Exception ignored) {
    }
  }

  @Override
  public void onInstanceResume(int instanceId) {
//    showOrUpdate();
  }

  @Override
  public void onInstancePause(int instanceId) {
//    dismiss();
  }

  @Override
  public void onInstanceDestroy(int instanceId) {
    try {
      HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) getContext();
      hippyInstanceContext.getEngineContext().removeInstanceLifecycleEventListener(this);
    } catch (Exception ignored) {
    }
    try {
      dismiss();
    } catch (Exception ignored) {
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    Log.i(TAG, "dialog dismiss onDetachedFromWindow");
    onInstanceDestroy(0);
    super.onDetachedFromWindow();
  }

  public interface OnRequestCloseListener {

    void onRequestClose(DialogInterface dialog);
  }

  private final int STYLE_THEME_FULL_SCREEN_DIALOG = 0;
  private final int STYLE_THEME_ANIMATED_FADE_DIALOG = 1;
  private final int STYLE_THEME_ANIMATED_SLIDE_DIALOG = 2;
  private final int STYLE_THEME_ANIMATED_SLIDE_FADE_DIALOG = 3;

  private final DialogRootViewGroup mHostView;
  private Dialog mDialog;
  private View mContentView;
  private boolean mTransparent = true;
  private boolean mPropertyRequiresNewDialog;
  private DialogInterface.OnShowListener mOnShowListener;
  OnRequestCloseListener mOnRequestCloseListener;
  private String mAnimationType;
  private int mAniType;
  private boolean mEnterImmersionStatusBar = false;
  private boolean mStatusBarTextDarkColor = false;
  private boolean mListenKeyEvent = false;

  public HippyModalHostView(Context context) {
    super(context);
    mAniType = STYLE_THEME_FULL_SCREEN_DIALOG;
    HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) context;
    hippyInstanceContext.getEngineContext().addInstanceLifecycleEventListener(this);
    mHostView = new DialogRootViewGroup(context);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

  }

  @Override
  public void addView(View child, int index) {
    mHostView.addView(child, index);
  }

  @Override
  public int getChildCount() {
    return mHostView == null ? 0 : mHostView.getChildCount();
  }

  @Override
  public View getChildAt(int index) {
    return mHostView.getChildAt(index);
  }

  @Override
  public void removeView(View child) {
    mHostView.removeView(child);
  }

  @Override
  public void removeViewAt(int index) {
    View child = getChildAt(index);
    mHostView.removeView(child);
  }

  public void addChildrenForAccessibility(ArrayList<View> outChildren) {

  }

  @Override
  public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
    return false;
  }

  private void dismiss() throws Exception {
//    if (isActivityFinishing()) {
//      return;
//    }
//
//    if (mDialog != null) {
//      mDialog.dismiss();
//      mDialog = null;
//      ViewGroup parent = (ViewGroup) mHostView.getParent();
//      parent.removeViewAt(0);
//    }

    Log.i(TAG, "dialog dismiss activity finish " + isActivityFinishing());

    if (mDialog != null) {
      try {
        ViewGroup parent = (ViewGroup) mHostView.getParent();
        parent.removeViewAt(0);
      } catch (Exception e) {
        e.printStackTrace();
      }

      try {
        mDialog.dismiss();
      } catch (Exception e) {
        e.printStackTrace();
      }
      mDialog = null;
    }

  }

  public void setOnRequestCloseListener(OnRequestCloseListener listener) {
    mOnRequestCloseListener = listener;
  }

  public void requestClose() {
    if (mOnRequestCloseListener != null) {
      mOnRequestCloseListener.onRequestClose(mDialog);
    }
  }

  public void setOnShowListener(DialogInterface.OnShowListener listener) {
    mOnShowListener = listener;
  }

  protected void setTransparent(boolean transparent) {
    mTransparent = transparent;
  }

  protected void setAnimationType(String animationType) {
    if (!TextUtils.isEmpty(animationType)) {
      switch (animationType) {
        case "fade":
          mAniType = STYLE_THEME_ANIMATED_FADE_DIALOG;
          break;
        case "slide":
          mAniType = STYLE_THEME_ANIMATED_SLIDE_DIALOG;
          break;
        case "slide_fade":
          mAniType = STYLE_THEME_ANIMATED_SLIDE_FADE_DIALOG;
          break;
        default:
          mAniType = STYLE_THEME_FULL_SCREEN_DIALOG;
      }
    }

    mAnimationType = animationType;
    mPropertyRequiresNewDialog = true;
  }

  protected String getAnimationType() {
    return mAnimationType;
  }

  protected void setEnterImmersionStatusBar(boolean fullScreen) {
    mEnterImmersionStatusBar = fullScreen;
  }

  protected void setImmersionStatusBarTextDarkColor(boolean darkColor) {
    mStatusBarTextDarkColor = darkColor;
  }

  public void setListenKeyEvent(boolean listen) {
    mListenKeyEvent = listen;
  }

  public Dialog getDialog() {
    return mDialog;
  }

  static int mStatusBarHeight = -1;
  static boolean hasCheckStatusBarHeight = false;

  public int getStatusBarHeightFixed() {
    if (mStatusBarHeight == -1) {
      mStatusBarHeight = getStatusBarHeightFromSystem();

      hasCheckStatusBarHeight = true;
    }
    return mStatusBarHeight;
  }

  private static int statusBarHeight = -1;

  @SuppressWarnings("ConstantConditions")
  public static int getStatusBarHeightFromSystem() {
    if (statusBarHeight > 0) {
      return statusBarHeight;
    }

    Class<?> c;
    Object obj;
    Field field;
    int x;
    try {
      c = Class.forName("com.android.internal.R$dimen");
      obj = c.newInstance();
      field = c.getField("status_bar_height");
      x = Integer.parseInt(field.get(obj).toString());
      statusBarHeight = ContextHolder.getAppContext().getResources().getDimensionPixelSize(x);
    } catch (Exception e1) {
      statusBarHeight = -1;
      e1.printStackTrace();
    }

    if (statusBarHeight < 1) {
      try {
        int statebarH_id = ContextHolder.getAppContext().getResources()
          .getIdentifier("statebar_height", "dimen",
            ContextHolder.getAppContext().getPackageName());
        statusBarHeight = Math
          .round(ContextHolder.getAppContext().getResources().getDimension(statebarH_id));
      } catch (NotFoundException e) {
        LogUtils.d("HippyModalHostView", "getStatusBarHeightFromSystem: " + e.getMessage());
        statusBarHeight = 0;
      }
    }
    return statusBarHeight;
  }

  public void setDialogBar(boolean isDarkIcon) {
    try {
      Window window = mDialog.getWindow();
      int sysUI = window.getDecorView().getSystemUiVisibility();
      sysUI = sysUI & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      sysUI = sysUI & ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      int extra;
      if (isDarkIcon) {
        extra = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        extra = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      }
      window.getDecorView().setSystemUiVisibility(sysUI | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | extra);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.TRANSPARENT);
      } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      }
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  private boolean isActivityFinishing() {
    HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) getContext();
    if (hippyInstanceContext == null) {
      return true;
    }

    Context context = hippyInstanceContext.getActivityContext();
    if (context == null) {
      context = hippyInstanceContext.getBaseContext();
    }
    if (!(context instanceof Activity)) {
      return true;
    }

    Activity currentActivity = (Activity) context;
    return currentActivity.isFinishing();
  }

  protected void showOrUpdate() throws Exception {
    if (isActivityFinishing()) {
      return;
    }

    if (mDialog != null) {
      if (mPropertyRequiresNewDialog) {
        dismiss();
      } else {
        updateProperties();
        return;
      }
    }

    mPropertyRequiresNewDialog = false;
    Context currentContext = getContext();
    mDialog = createDialog(currentContext);
    mContentView = createContentView(mHostView);
    mDialog.setContentView(mContentView);
    updateProperties();
    if (mDialog != null && mDialog.getWindow() != null && mEnterImmersionStatusBar) {
      setDialogBar(mStatusBarTextDarkColor);
    }

    assert mDialog != null;
    mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialogInterface) {
        mOnShowListener.onShow(dialogInterface);
        ObjectAnimator alphaAnimation = null;
        switch (mAniType) {
          case STYLE_THEME_ANIMATED_FADE_DIALOG:
            alphaAnimation = ObjectAnimator.ofFloat(mContentView, "alpha", 0.0f, 1.0f);
            break;
          case STYLE_THEME_ANIMATED_SLIDE_DIALOG:
            alphaAnimation = ObjectAnimator.ofFloat(mContentView, "translationY", 0);
            break;
          case STYLE_THEME_ANIMATED_SLIDE_FADE_DIALOG:
            PropertyValuesHolder fadeValuesHolder = PropertyValuesHolder
              .ofFloat("alpha", 0.0f, 1.0f);
            PropertyValuesHolder slideValuesHolder = PropertyValuesHolder
              .ofFloat("translationY", 0);
            alphaAnimation = ObjectAnimator
              .ofPropertyValuesHolder(mContentView, fadeValuesHolder, slideValuesHolder);
            break;
          default:
        }

        if (alphaAnimation != null) {
          alphaAnimation.setDuration(200);
          alphaAnimation.start();
        }
      }
    });
    mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(DialogInterface dialogInterface) {
      }
    });
    mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
      @Override
      public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {

          if (keyCode == KeyEvent.KEYCODE_BACK) {
            mOnRequestCloseListener.onRequestClose(dialog);
            return true;
          } else if (mListenKeyEvent) {
            if (((HippyInstanceContext) getContext()).getBaseContext() instanceof Activity) {
              Activity currentActivity = (Activity) ((HippyInstanceContext) getContext())
                .getBaseContext();
              if (currentActivity != null) {
                return currentActivity.onKeyDown(keyCode, event);
              }
            }
          }
        }
        return false;
      }
    });

    mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    mDialog.show();

    int nScreenHeight = getScreenHeight();
    switch (mAniType) {
      case STYLE_THEME_ANIMATED_FADE_DIALOG:
        mContentView.setAlpha(0);
        break;
      case STYLE_THEME_ANIMATED_SLIDE_DIALOG:
        if (nScreenHeight != -1) {
          mContentView.setTranslationY(nScreenHeight);
        }
        break;
      case STYLE_THEME_ANIMATED_SLIDE_FADE_DIALOG:
        mContentView.setAlpha(0);
        if (nScreenHeight != -1) {
          mContentView.setTranslationY(nScreenHeight);
        }
        break;
      default:
    }
  }

  private int getScreenHeight() {
    try {
      Context context = ContextHolder.getAppContext();
      android.view.WindowManager manager = (android.view.WindowManager) context
        .getSystemService(Context.WINDOW_SERVICE);
      Display display = manager.getDefaultDisplay();
      if (display != null) {
        //noinspection deprecation
        return manager.getDefaultDisplay().getHeight();
      }
    } catch (SecurityException e) {
      LogUtils.d("HippyModalHostView", "getScreenHeight: " + e.getMessage());
    }
    return -1;
  }

  @SuppressWarnings("SameReturnValue")
  protected int getThemeResId() {
    return 0;
  }

  protected Dialog createDialog(Context context) {
    if (context instanceof HippyInstanceContext) {
      Context activityContext = ((HippyInstanceContext) context).getActivityContext();
      if (activityContext != null) {
        context = activityContext;
      }
    }
    int themeResId = getThemeResId();
    if (context != null) {
      Resources res = context.getResources();
      themeResId = res.getIdentifier("HippyFullScreenDialog", "style", context.getPackageName());
    }

    assert context != null;
    Dialog dialog = new Dialog(context, themeResId);
    if (themeResId == 0) {
      Window window = dialog.getWindow();
      if (window != null) {
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams
          .MATCH_PARENT);
      }
    }

    return dialog;
  }

  protected View createContentView(View hostView) {
    FrameLayout frameLayout = new FrameLayout(getContext()) {
      @Override
      protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mEnterImmersionStatusBar && mStatusBarHeight != -1
          && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
          && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          canvas.save();
          canvas.clipRect(0, 0, getMeasuredWidth(), mStatusBarHeight);
          canvas.drawColor(0x40000000);
          canvas.restore();
        }
      }
    };
    if (mEnterImmersionStatusBar && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      params.topMargin = -1 * getStatusBarHeightFixed();
      frameLayout.addView(hostView, params);
    } else {
      frameLayout.addView(hostView);
    }
    frameLayout.setFitsSystemWindows(false);
    return frameLayout;
  }

  private void updateProperties() {
    if (mTransparent) {
      mDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    } else {
      mDialog.getWindow().setDimAmount(0.5f);
      mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
  }

  public static class DialogRootViewGroup extends HippyViewGroup {

    public DialogRootViewGroup(Context context) {
      super(context);
      setFitsSystemWindows(false);

    }


    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);
      if (getChildCount() > 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          getChildAt(0)
            .layout(getChildAt(0).getLeft(), getChildAt(0).getTop(), getChildAt(0).getLeft() + w,
              getChildAt(0).getTop() + h);
        }

        HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) getContext();
        if (hippyInstanceContext != null && hippyInstanceContext.getEngineContext() != null) {
          final HippyEngineContext engineContext = hippyInstanceContext.getEngineContext();
          if (engineContext.getThreadExecutor() != null) {
            final int id = getChildAt(0).getId();
            engineContext.getThreadExecutor().postOnDomThread(new Runnable() {
              @Override
              public void run() {
                if (engineContext.getDomManager() != null) {
                  engineContext.getDomManager().updateNodeSize(id, w, h);
                }
              }
            });

          }

        }
      }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
  }
}
