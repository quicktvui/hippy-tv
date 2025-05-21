/* 	Copyright (C) 2018 Tencent, Inc.
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

package com.tencent.mtt.supportui.views.asyncimage;

import android.animation.Animator;
import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.quicktvui.base.ui.graphic.BaseBorderDrawable;
import com.quicktvui.base.ui.graphic.BorderFrontDrawable;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.mouse.GenericMotionUtil;
import com.tencent.mtt.hippy.utils.mouse.HoverManager;
import com.tencent.mtt.hippy.utils.mouse.MouseUtil;
import com.tencent.mtt.supportui.adapters.image.IDrawableTarget;
import com.tencent.mtt.supportui.adapters.image.IImageLoaderAdapter;
import com.tencent.mtt.supportui.adapters.image.IImageRequestListener;
import com.tencent.mtt.supportui.views.IBorder;
import com.tencent.mtt.supportui.views.IGradient;
import com.tencent.mtt.supportui.views.IShadow;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by leonardgong on 2017/12/7 0007.
 */

public class AsyncImageView extends ViewGroup implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener, IBorder, IShadow,
  IGradient {
  public static final int FADE_DURATION = 250;
  public final static int IMAGE_UNLOAD = 0;
  public final static int IMAGE_LOADING = 1;
  public final static int IMAGE_LOADED = 2;

  protected final static int SOURCE_TYPE_SRC = 1;
  protected final static int SOURCE_TYPE_DEFAULT_SRC = 2;
  protected IDrawableTarget mSourceDrawable;
  private IDrawableTarget mDefaultSourceDrawable;

  protected String mUrl;
  protected String mDefaultSourceUrl;
  protected String mImageType;

  // the 'mURL' is fetched succeed
  protected int mUrlFetchState = IMAGE_UNLOAD;

  protected int mTintColor;
  protected ScaleType mScaleType;
  protected Drawable mContentDrawable;

  private boolean mIsAttached;
  protected IImageLoaderAdapter mImageAdapter;
  //添加自定义borderDrawable
  protected BaseBorderDrawable borderDrawable;
  //自定义边框type 默认type = 0
  protected int borderType;
  private boolean mFadeEnable = false;
  private long mFadeDuration = 200;
  private ValueAnimator mAlphaAnimator;

  protected BackgroundDrawable mBGDrawable;

  private int mImagePositionX;
  private int mImagePositionY;

  //zhaopeng add
  private int delayLoad = -1;


  public void setDelayLoad(int delayLoad) {
    this.delayLoad = delayLoad;
  }

  private LoadTask loadTask;

  final static class LoadTask implements Runnable {
    final AsyncImageView target;
    final String url;
    final int sourceType;
    boolean done = false;

    LoadTask(AsyncImageView target, String url, int sourceType) {
      this.target = target;
      this.url = url;
      this.sourceType = sourceType;
    }

    @Override
    public void run() {
      if (url != null && url.equals(target.mUrl)) {
        target.fetchImageByUrl(url, sourceType);
        done = true;
        target.loadTask = null;
      } else {
        if (LogUtils.isDebug()) {
          Log.e("FastImageLog", "LoadTask return on url not same:  url:" + url);
        }
        if (url != null && LogUtils.isDebug()) {
          LogUtils.v("hippy", "image cancelLoad:" + url);
        }
      }
    }
  }

  public enum ScaleType {
    FIT_XY,
    CENTER,
    CENTER_INSIDE,
    CENTER_CROP,
    ORIGIN,
    REPEAT // Robinsli add for hippy
  }

  public AsyncImageView(Context context) {
    super(context);
    mUrl = null;
    mDefaultSourceUrl = null;
    mImageType = null;
    setFadeEnabled(false);
    setFadeDuration(FADE_DURATION);
  }

  public void setImageAdapter(IImageLoaderAdapter imageAdapter) {
    mImageAdapter = imageAdapter;
  }

  public void setImageType(String type) {
    mImageType = type;
  }

  public void setUrl(String url) {
    if (!TextUtils.equals(url, mUrl)) {
      mUrl = url;
      mUrlFetchState = IMAGE_UNLOAD;
      if (isAttached()) {
        onDrawableDetached();
        // zhaopeng edit
        //fetchImageByUrl(mUrl, SOURCE_TYPE_SRC);
        fetchImageByUrlDelayed(mUrl, SOURCE_TYPE_SRC, delayLoad);
      }
    }
  }

  public String getUrl() {
    return mUrl;
  }

  public void setFadeEnabled(boolean enable) {
    mFadeEnable = enable;
  }

  public boolean isFadeEnabled() {
    return mFadeEnable;
  }

  public void setFadeDuration(long duration) {
    mFadeDuration = duration;
  }

  protected void resetFadeAnimation() {
    if (mFadeEnable) {
      if (mAlphaAnimator != null && mAlphaAnimator.isRunning()) {
        mAlphaAnimator.cancel();
      }
      mAlphaAnimator = null;
    }
  }

  protected void startFadeAnimation() {
    if (mFadeEnable) {
      if (mAlphaAnimator != null && this.mAlphaAnimator.isRunning()) {
        this.mAlphaAnimator.cancel();
        this.mAlphaAnimator = null;
      }
      if (this.mFadeDuration > 0 && this.mAlphaAnimator == null) {
        this.mAlphaAnimator = ValueAnimator.ofInt(0, 255);
        this.mAlphaAnimator.setEvaluator(new IntEvaluator());
        this.mAlphaAnimator.addUpdateListener(this);
        this.mAlphaAnimator.addListener(this);
        this.mAlphaAnimator.setDuration((long) this.mFadeDuration);
      }
      if (this.mAlphaAnimator != null) {
        this.mAlphaAnimator.setCurrentPlayTime(0L);
        this.mAlphaAnimator.start();
      }
    }
  }

  protected void performFetchImage() {
//    Log.d("HippyImage","fetchImageByUrlDelayed bbbbbbbbbbb :url"+mUrl+",this:"+this);
    fetchImageByUrlDelayed(mUrl, SOURCE_TYPE_SRC, delayLoad);
  }

  public void setDefaultSource(String defaultSource) {
    if (!TextUtils.equals(mDefaultSourceUrl, defaultSource)) {
      mDefaultSourceUrl = defaultSource;
//      Log.d("HippyImage","fetchImageByUrlDelayed ccccccccccc :url"+mDefaultSourceUrl+",this:"+this);
      fetchImageByUrlDelayed(mDefaultSourceUrl, SOURCE_TYPE_DEFAULT_SRC, delayLoad);
    }
  }

  public void setTintColor(int tintColor) {
    mTintColor = tintColor;
    applyTintColor(mTintColor);
  }

  protected void applyTintColor(int tintColor) {
    if (mContentDrawable instanceof ContentDrawable) {
      ((ContentDrawable) mContentDrawable).setTintColor(tintColor);
      invalidate();
    }
  }

  protected int getTintColor() {
    return mTintColor;
  }

  public void setScaleType(ScaleType scaleType) {
    mScaleType = scaleType;
  }

  public void setImagePositionX(int positionX) {
    mImagePositionX = positionX;
  }

  public void setImagePositionY(int positionY) {
    mImagePositionY = positionY;
  }

  protected void onFetchImage(String url) {

  }

  protected boolean shouldFetchImage() {
    return true;
  }

  protected boolean isAttached() {
    return mIsAttached;
  }


  private void fetchImageByUrlDelayed(final String url, final int sourceType, int delayLoad) {
    if (loadTask != null) {
      if (TextUtils.equals(url, loadTask.url)) {
        if(LogUtils.isDebug()) {
          Log.e("HippyImage", "fetchImageByUrlDelayed return on same url" + url + ",this:" + this);
        }
        return;
      } else {
        removeCallbacks(loadTask);
      }
    }
    loadTask = null;
    if (delayLoad > 0) {
      if (mBGDrawable != null) {
        if (getBackgroundDrawable() == null) setBackgroundDrawable(mBGDrawable);
      } else {
        setBackgroundDrawable(null);
      }
      if (LogUtils.isDebug()) {
        Log.i("HippyImage", "fetchImageByUrlDelayed 11111 :url" + url + ",this:" + hashCode());
      }
      loadTask = new LoadTask(this, url, sourceType);
      postTask(loadTask, delayLoad);
      invalidate();
    } else {
      fetchImageByUrl(url, sourceType);
    }
  }

  protected void postTask(Runnable task, int delayLoad) {
    postDelayed(task, delayLoad);
  }

  private void fetchImageByUrl(String url, final int sourceType) {
    if (url == null) {
//      if (LogUtils.isDebug()) {
//        LogUtils.d("HippyImage", "++++HippyImage fetchImageByUrl return on url null this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mSourceDrawable:" + mSourceDrawable + ",url:" + mUrl);
//      }
      return;
    }

    if (LogUtils.isDebug() && mUrl != null) {
      LogUtils.i("HippyImage", "++++HippyImage fetchImageByUrl this:" + getId() + ",mUrlFetchState:" + mUrlFetchState + ",mSourceDrawable:" + mSourceDrawable + ",url:" + mUrl);
    }
    if (mImageAdapter != null) {
      // fetch or get image, depending on url type

      // http or https => async fetch
      // eg. <Image source={{uri: 'https://abc.png'}} />
      if (shouldUseFetchImageMode(url)) {
        url = url.trim().replaceAll(" ", "%20");
        if (sourceType == SOURCE_TYPE_SRC) {
          if (!shouldFetchImage()) {
            if (LogUtils.isDebug() && mUrl != null) {
              LogUtils.e("HippyImage", "++++HippyImage fetchImageByUrl , return on !shouldFetchImage url:" + mUrl);
              LogUtils.e("FastImageLog", "++++HippyImage fetchImageByUrl , return on !shouldFetchImage url:" + mUrl);
            }
            return;
          }
          mUrlFetchState = IMAGE_LOADING;
        }

        onFetchImage(url);
        handleGetImageStart();
        doFetchImage(getFetchParam(), sourceType);
      } else {
        // [file/resource/base64] => direct get
        // eg. <Image source={require('./abc.png')} />
        // eg. <Image source={{uri: 'icon'}} />
        // eg. <Image source={{uri: 'data:image/png;base64,iVBORTJRU5Erk=='}} />

        handleGetImageStart();
        handleImageRequest(mImageAdapter.getImage(url, null), sourceType, null);
      }
    }
  }

  protected boolean shouldUseFetchImageMode(String url) {
    return true;
  }

  protected Object getFetchParam() {
    return mSourceDrawable != null ? mSourceDrawable.getExtraData() : null;
  }

  protected void doFetchImage(Object param, final int sourceType) {
    if (LogUtils.isDebug()) {
      LogUtils.v("HippyImage", "++++HippyImage doFetchImage this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mSourceDrawable:" + mSourceDrawable + ",url:" + mUrl);
    }
    if (mImageAdapter != null) {
      // 这里不判断下是取背景图片还是取当前图片怎么行？
      String url = sourceType == SOURCE_TYPE_SRC ? mUrl : mDefaultSourceUrl;
      mImageAdapter.fetchImage(url, new IImageRequestListener<IDrawableTarget>() {
        @Override
        public void onRequestStart(IDrawableTarget IDrawableTarget) {
          mSourceDrawable = IDrawableTarget;
        }

        @Override
        public void onRequestSuccess(IDrawableTarget IDrawableTarget) {
          if (LogUtils.isDebug()) {
            LogUtils.i("HippyImage", "++++HippyImage doFetchImage onRequestSuccess this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mSourceDrawable:" + mSourceDrawable + ",url:" + mUrl);
          }
          handleImageRequest(IDrawableTarget, sourceType, null);
        }

        @Override
        public void onRequestFail(Throwable throwable, String source) {
          if (LogUtils.isDebug()) {
            LogUtils.e("HippyImage", "++++HippyImage doFetchImage onRequestFail this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mSourceDrawable:" + mSourceDrawable + ",url:" + mUrl);
          }
          handleImageRequest(null, sourceType, throwable);
        }
      }, param);
    }
  }

  protected void handleImageRequest(IDrawableTarget resultDrawable, int sourceType, Object requestInfo) {

    if (LogUtils.isDebug()) {
      LogUtils.v("HippyImage", "++++HippyImage handleImageRequest this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",resultDrawable:" + resultDrawable + ",url:" + mUrl + ",sourceType:" + sourceType);
    }
    if (resultDrawable == null) {
      if (sourceType == SOURCE_TYPE_SRC) {
        mSourceDrawable = null;
        if (mDefaultSourceDrawable != null) {
          if (mContentDrawable == null) {
            mContentDrawable = generateContentDrawable();
          }
          setContent(SOURCE_TYPE_DEFAULT_SRC);
        } else {
          mContentDrawable = null;
        }
        handleGetImageFail(requestInfo instanceof Throwable ? (Throwable) requestInfo : null);
      } else if (sourceType == SOURCE_TYPE_DEFAULT_SRC) {
        mDefaultSourceDrawable = null;
      }
    } else {
      mContentDrawable = generateContentDrawable();
      if (sourceType == SOURCE_TYPE_SRC) {
        mSourceDrawable = resultDrawable;
        handleGetImageSuccess();
      } else if (sourceType == SOURCE_TYPE_DEFAULT_SRC) {
        mDefaultSourceDrawable = resultDrawable;
      }
      setContent(sourceType);
    }
  }

  protected ContentDrawable generateContentDrawable() {
    return new ContentDrawable();
  }

  protected BackgroundDrawable generateBackgroundDrawable() {
    return new BackgroundDrawable();
  }

  @Override
  protected void onDetachedFromWindow() {
    mIsAttached = false;
    if (mFadeEnable) {
      if (mAlphaAnimator != null) {
        mAlphaAnimator.cancel();
      }
    }
    //zhaopeng add
    if (loadTask != null) {
      removeCallbacks(loadTask);
      loadTask = null;
    }
    super.onDetachedFromWindow();
    onDrawableDetached();
    if (mDefaultSourceDrawable != null) {
      mDefaultSourceDrawable.onDrawableDetached();
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

  }

  @Override
  protected void onAttachedToWindow() {
    mIsAttached = true;
    super.onAttachedToWindow();

    if (toRecycleTask != null && getRootView() != null) {
      getRootView().removeCallbacks(toRecycleTask);
    }
    if (mDefaultSourceDrawable != null && shouldFetchImage()) {
      mDefaultSourceDrawable.onDrawableAttached();
      setContent(SOURCE_TYPE_DEFAULT_SRC);
      setUrl(mUrl);
    }

//    if (getBitmap() == null || !mIsUrlFetchSucceed) {
////      Log.d("HippyImage","fetchImageByUrlDelayed dddddddddd :url"+mUrl+",this:"+this);
    fetchImageByUrlDelayed(mUrl, SOURCE_TYPE_SRC, delayLoad);
//    }
    onDrawableAttached();
  }

  private boolean isRecycleOnDetach = false;
  private final int delayToRecycle = 300;
  private Runnable toRecycleTask;

  public void setRecycleOnDetach(boolean recycleOnDetach) {
    isRecycleOnDetach = recycleOnDetach;
  }

  protected void onDrawableAttached() {
    if (LogUtils.isDebug()) {
      LogUtils.v("HippyImage", "++++HippyImage onAttachedToWindow fetchImageByUrlDelayed this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mSourceDrawable:" + mSourceDrawable + ",url:" + mUrl);
    }
    if (mSourceDrawable != null) {
      mSourceDrawable.onDrawableAttached();
    }
  }

  protected void onDrawableDetached() {
//    if (LogUtils.isDebug() && mUrl != null) {
//      LogUtils.v("HippyImage", "----HippyImage onDrawableDetached this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",url:" + mUrl);
//    }

    if (mSourceDrawable != null) {
      mSourceDrawable.onDrawableDetached();
      if (isRecycleOnDetach) {
        if (getRootView() != null) {
          if (toRecycleTask != null) {
            getRootView().removeCallbacks(toRecycleTask);
          }
          toRecycleTask = () -> {

            if (mSourceDrawable != null) {
//                if(mSourceDrawable.getBitmap() != null && !mSourceDrawable.getBitmap().isRecycled()){
//                  mSourceDrawable.getBitmap().recycle();
//                }
              //              mSourceDrawable.getBitmap().recycle();
//              if (LogUtils.isDebug()) {
//                LogUtils.e("HippyImage", "----HippyImage onDrawableDetached recycleBitmap this:" + hashCode() + ",mSourceDrawable:" + mSourceDrawable + "url:" + mUrl);
//              }
              mSourceDrawable = null;
              resetContent();
              toRecycleTask = null;
            }
          };
        }
        getRootView().postDelayed(toRecycleTask, delayToRecycle);
      }

    }
  }


  protected void resetContent() {
    mContentDrawable = null;
    mBGDrawable = null;
//    resetFadeAnimation();
    super.setBackgroundDrawable(null);
  }

  protected void onSetContent(String url) {

  }



  protected void afterSetContent(String url) {
    postInvalidateDelayed(16);
    if (LogUtils.isDebug()) {
      LogUtils.v("HippyImage", "++++HippyImage afterSetContent this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mContentDrawable:" + mContentDrawable + ",url:" + mUrl);
    }
  }

  protected void performSetContent() {
    setContent(SOURCE_TYPE_SRC);
  }

  protected boolean shouldSetContent() {
    return true;
  }

  protected Bitmap getBitmap() {
    if (mSourceDrawable != null) {
      return mSourceDrawable.getBitmap();
    }
    return null;
  }

  protected void setContent(int sourceType) {
    if (LogUtils.isDebug()) {
      LogUtils.v("HippyImage", "++++HippyImage setContent this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mContentDrawable:" + mContentDrawable + ",url:" + mUrl + ",sourceType:" + sourceType);
    }
    if (mContentDrawable != null) {
      if (!shouldSetContent()) {
        return;
      }

      onSetContent(mUrl);
      updateContentDrawableProperty(sourceType);

      if (mBGDrawable != null) {
        if (mContentDrawable instanceof ContentDrawable) {
          ((ContentDrawable) mContentDrawable).setBorder(mBGDrawable.getBorderRadiusArray(), mBGDrawable.getBorderWidthArray());
          ((ContentDrawable) mContentDrawable).setShadowOffsetX(mBGDrawable.getShadowOffsetX());
          ((ContentDrawable) mContentDrawable).setShadowOffsetY(mBGDrawable.getShadowOffsetY());
          ((ContentDrawable) mContentDrawable).setShadowRadius(mBGDrawable.getShadowRadius());
        }
        setBackgroundDrawable(new LayerDrawable(new Drawable[]{mBGDrawable, mContentDrawable}));
      } else {
        setBackgroundDrawable(mContentDrawable);
      }
      afterSetContent(mUrl);
    }
  }

  protected void updateContentDrawableProperty(int sourceType) {
    if (!(mContentDrawable instanceof ContentDrawable)) {
      return;
    }

    Bitmap bitmap = null;
    switch (sourceType) {
      case SOURCE_TYPE_SRC:
        if (mSourceDrawable != null) {
          bitmap = mSourceDrawable.getBitmap();
        }
        break;
      case SOURCE_TYPE_DEFAULT_SRC:
        if (mDefaultSourceDrawable != null && (mUrlFetchState != IMAGE_LOADED || mSourceDrawable == null)) {
          bitmap = mDefaultSourceDrawable.getBitmap();
        }
        break;
    }

    if (bitmap != null) {
      ((ContentDrawable) mContentDrawable).setSourceType(sourceType);
      ((ContentDrawable) mContentDrawable).setBitmap(bitmap);
      ((ContentDrawable) mContentDrawable).setTintColor(getTintColor());
      ((ContentDrawable) mContentDrawable).setScaleType(mScaleType);
      ((ContentDrawable) mContentDrawable).setImagePositionX(mImagePositionX);
      ((ContentDrawable) mContentDrawable).setImagePositionY(mImagePositionY);
      if (LogUtils.isDebug()) {
        LogUtils.v("HippyImage", "++++HippyImage updateContentDrawableProperty bitmap not null, this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mContentDrawable:" + mContentDrawable + ",url:" + mUrl + ",sourceType:" + sourceType);
      }
    } else {
      if (LogUtils.isDebug()) {
        LogUtils.e("HippyImage", "++++HippyImage updateContentDrawableProperty bitmap is null, this:" + hashCode() + ",mUrlFetchState:" + mUrlFetchState + ",mContentDrawable:" + mContentDrawable + ",url:" + mUrl + ",sourceType:" + sourceType);
      }
    }
  }

  protected void handleGetImageStart() {
    startFadeAnimation();
  }

  protected void handleGetImageSuccess() {

  }

  protected void handleGetImageFail(Throwable throwable) {
  }

  @Override
  public void onAnimationStart(Animator animation, boolean isReverse) {
    this.onAnimationStart(animation);
  }

  @Override
  public void onAnimationStart(Animator animation) {
  }

  @Override
  public void onAnimationEnd(Animator animation) {
    if (mFadeEnable) {
      restoreBackgroundColorAfterSetContent();
    }
  }

  @Override
  public void onAnimationEnd(Animator animation, boolean isReverse) {
    onAnimationEnd(animation);
  }

  @Override
  public void onAnimationCancel(Animator animation) {
    if (mFadeEnable) {
      if (mContentDrawable != null) {
        mContentDrawable.setAlpha(255);
      }
      restoreBackgroundColorAfterSetContent();
    }
  }

  @Override
  public void onAnimationRepeat(Animator animation) {

  }

  @Override
  public void onAnimationUpdate(ValueAnimator animation) {
    if (mFadeEnable) {
      if (!isAttached() && mAlphaAnimator != null) {
        mAlphaAnimator.cancel();
      }
      if (mContentDrawable != null) {
        mContentDrawable.setAlpha(((Integer) animation.getAnimatedValue()).intValue());
      }
    }
  }

  protected void restoreBackgroundColorAfterSetContent() {
    setBackgroundColor(Color.TRANSPARENT);
  }

  protected void setCustomBackgroundDrawable(BackgroundDrawable commonBackgroundDrawable) {
    mBGDrawable = commonBackgroundDrawable;
    super.setBackgroundDrawable(mBGDrawable);
  }

  @Override
  public void setBorderRadius(float radius, int position) {
    getBackGround().setBorderRadius(radius, position);
    if (mContentDrawable instanceof ContentDrawable) {
      ((ContentDrawable) mContentDrawable).setBorder(mBGDrawable.getBorderRadiusArray(), mBGDrawable.getBorderWidthArray());
      invalidate();
    }
  }

  @Override
  public void setBorderWidth(float width, int position) {
    getBackGround().setBorderWidth(width, position);
    if (mContentDrawable instanceof ContentDrawable) {
      ((ContentDrawable) mContentDrawable).setBorder(mBGDrawable.getBorderRadiusArray(), mBGDrawable.getBorderWidthArray());
      invalidate();
    }
  }

  @Override
  public void setBorderColor(int color, int position) {
    getBackGround().setBorderColor(color, position);
    invalidate();
  }

  @Override
  public void setBorderStyle(int borderStyle) {
    getBackGround().setBorderStyle(borderStyle);
    invalidate();
  }

  @Override
  public void setBackgroundColor(int color) {
    getBackGround().setBackgroundColor(color);
    invalidate();
  }

  @Override
  public void setShadowOffsetX(float x) {
    getBackGround().setShadowOffsetX(x);
    invalidate();
  }

  @Override
  public void setShadowOffsetY(float y) {
    getBackGround().setShadowOffsetY(y);
    invalidate();
  }

  @Override
  public void setShadowOpacity(float opacity) {
    getBackGround().setShadowOpacity(opacity);
    invalidate();
  }

  @Override
  public void setShadowRadius(float radius) {
    getBackGround().setShadowRadius(Math.abs(radius));
    if (radius != 0) {
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    invalidate();
  }

  @Override
  public void setShadowSpread(float spread) {

  }

  @Override
  public void setShadowColor(int color) {
    getBackGround().setShadowColor(color);
    invalidate();
  }

  @Override
  public void setGradientAngle(String angle) {
    getBackGround().setGradientAngle(angle);
    invalidate();
  }

  @Override
  public void setGradientColors(ArrayList<Integer> colors) {
    getBackGround().setGradientColors(colors);
    invalidate();
  }

  @Override
  public void setGradientPositions(ArrayList<Float> positions) {
    getBackGround().setGradientPositions(positions);
    invalidate();
  }

  private BackgroundDrawable getBackGround() {
    if (mBGDrawable == null) {
      mBGDrawable = generateBackgroundDrawable();
      Drawable currBGDrawable = getBackground();
      super.setBackgroundDrawable(null);
      if (currBGDrawable == null) {
        super.setBackgroundDrawable(mBGDrawable);
      } else {
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{mBGDrawable, currBGDrawable});
        super.setBackgroundDrawable(layerDrawable);
      }
    }
    return mBGDrawable;
  }

  public BackgroundDrawable getBackgroundDrawable() {
    return getBackGround();
  }

  public BaseBorderDrawable getBorderDrawable() {
    return borderDrawable;
  }

  //设置默认borderDrawable
  public void setBorderDrawable(ConcurrentHashMap<Integer, BaseBorderDrawable> borderMap) {
    if (borderMap != null && borderMap.size() > 0) {
      for (Integer i : borderMap.keySet()) {
        if (i == borderType) {
          this.borderDrawable = borderMap.get(i);
        }
      }
      if (borderType == 0 && this.borderDrawable == null) {
        this.borderDrawable = new BorderFrontDrawable();
      }
    } else {
      this.borderDrawable = new BorderFrontDrawable();
    }
  }

  private boolean isUseMouse = false;

  public void setEnableMouse(boolean enableMouse){
    if (enableMouse){
      isUseMouse = true;
      MouseUtil.setViewDefaultHover(this);
      MouseUtil.setViewDefaultTouch(this);
      MouseUtil.setViewMouseStatus(this,true);
      MouseUtil.setViewDefaultGenericMotion(this);
      GenericMotionUtil.setOnGenericMotionListener(this);
    }
  }

  @Override
  public boolean dispatchGenericMotionEvent(MotionEvent event) {
    if (isUseMouse){
      HoverManager.getInstance().dispatchGenericMotionEvent(event);
    }
    return super.dispatchGenericMotionEvent(event);
  }
}
