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
package com.tencent.mtt.hippy.views.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.adapter.exception.HippyExceptionHandlerAdapter;
import com.tencent.mtt.hippy.dom.node.DomNode;
import com.tencent.mtt.hippy.dom.node.HippyNativeGestureSpan;
import com.tencent.mtt.hippy.dom.node.TextNode;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.common.CommonBackgroundDrawable;
import com.tencent.mtt.hippy.views.common.CommonBorder;
import com.tencent.mtt.hippy.views.list.HippyRecycler;
import com.tencent.mtt.supportui.views.IShadow;

@SuppressWarnings({"unused"})
public class HippyTextView extends View implements CommonBorder, HippyViewBase, HippyRecycler, IShadow {

  private CommonBackgroundDrawable mBGDrawable;

  private boolean mNativeGestureEnable = false;

  private HippyNativeGestureSpan mNativeGestureSpan;

  private NativeGestureDispatcher mGestureDispatcher;

  protected Layout mLayout = null;

  private boolean mTextBold = false;

  private int mNativeTextColor = 0;
  private boolean mHasSetNativeTextColor = false;

  //zhaopeng add
  private int mFocusColor = Color.TRANSPARENT;
  private int mSelectColor = Color.TRANSPARENT;
  private boolean selectState = false;

  // add by WP
  private int mShadowColor;
  private float mShadowOffsetX;
  private float mShadowOffsetY;
  private float mShadowOpacity;
  private float mShadowRadius;
  private float mShadowSpread;

  @Override
  public void resetProps() {
    setPadding(0, 0, 0, 0);
    mNativeGestureEnable = false;
    mBGDrawable = null;
    //noinspection deprecation
    setBackgroundDrawable(null);
    mTextBold = false;
    mGestureDispatcher = null;
    mNativeGestureSpan = null;
    mNativeTextColor = 0;
    mHasSetNativeTextColor = false;
    mLayout = null;
  }

  @Override
  public void clear() {
    mLayout = null;
  }

  @Override
  public void onResetBeforeCache() {

  }

  @Override
  public void notifySaveState() {

  }

  @Override
  public void notifyRestoreState() {

  }

  @Override
  public void setId(int id) {
    super.setId(id);

    Context context = getContext();
    if (context instanceof HippyInstanceContext) {
      HippyEngineContext engineContext = ((HippyInstanceContext) context).getEngineContext();
      if (engineContext != null) {
        DomNode node = engineContext.getDomManager().getNode(id);
        if (node instanceof TextNode) {
          ((TextNode) node).setTextView(this);
        }
      }
    }
  }

  public HippyTextView(Context context) {
    super(context);
  }

  public void setTextBold(boolean bold) {
    mTextBold = bold;
    postInvalidate();
  }


  public void setLayout(Layout layout) {
    if (mLayout != null) {
      invalidate();
    }
    mLayout = layout;
    if (mHasSetNativeTextColor && mNativeTextColor != 0) {
//      setTextColor(mNativeTextColor);
      drawableStateChanged();
    }

  }

  //user in tabHost
  public void setCustomColor(int color) {
    mHasSetNativeTextColor = true;
    mNativeTextColor = color;
    setTextColor(color);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    try {
      super.onDraw(canvas);
      if (mLayout != null) {
        if(LogUtils.isDebug()) {
          LogUtils.d("HippyText",
            "id: " + getId() + " mExtra : " + mLayout.getText() + "layout : w:" + mLayout.getWidth()
              + " h:" + mLayout.getHeight()
              + " view : w:" + getWidth() + " h:" + getHeight() + " textColor:" + mLayout
              .getPaint().getColor());
        }
        canvas.save();
        switch (mLayout.getAlignment()) {
          case ALIGN_CENTER:
            int totalHeight =
                getHeight() + getPaddingTop() + getPaddingBottom() - mLayout.getHeight();
            int width = (getWidth() - mLayout.getWidth()) / 2;
            canvas.translate((float) width, totalHeight / 2.0f);
            break;
          case ALIGN_OPPOSITE:
            int x = getWidth() - getPaddingRight() - mLayout.getWidth();
            canvas.translate(x, 0);
            break;
          default:
            canvas.translate(getPaddingLeft(), getPaddingTop());
            break;
        }
        Paint paint = mLayout.getPaint();
        if (paint != null) {
          paint.setFakeBoldText(mTextBold);
          if(mShadowColor != Color.TRANSPARENT && (mShadowOffsetX != 0 || mShadowOffsetY != 0)) {
            paint.setShadowLayer(mShadowRadius, mShadowOffsetX, mShadowOffsetY, mShadowColor);
//						paint.setShadowLayer(mShadowRadius, mShadowOffsetX, mShadowOffsetY, ((mShadowColor & 0x00FFFFFF) | (0x40 << 24)));
          }
        }

        mLayout.draw(canvas);

        canvas.restore();
      } else {
        if(LogUtils.isDebug()) {
          LogUtils.d("HippyText", "id: " + getId() + " mExtra :  is  null ");
        }
      }
    } catch (Throwable e) {
      Context context = getContext();
      if (context instanceof HippyInstanceContext) {
        HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) context;
        HippyExceptionHandlerAdapter defaultExceptionHandler = hippyInstanceContext
            .getEngineContext().getGlobalConfigs()
            .getExceptionHandler();
        if (defaultExceptionHandler != null) {
          defaultExceptionHandler
              .handleNativeException(new RuntimeException("hippyTextView onDraw" + e.getMessage()),
                  true);
        }
      }
    }
  }

  @Override
  public void setSelected(boolean selected) {
    super.setSelected(selected);
    if(LogUtils.isDebug()) {
      LogUtils.d("HippyTextViewLog", "setSelected :" + selected + "，" + this);
    }
  }


  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    refreshDrawableState();
    if(selectState){
      setSelected(true);
    }
    if(LogUtils.isDebug()) {
      LogUtils.d("HippyTextViewLog", "isSelected :" + isSelected() + ",selectState " + selectState + "，" + this);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
  }

  public void setBorderRadius(float radius, int position) {
    getBackGround().setBorderRadius(radius, position);
  }

  public void setBorderWidth(float width, int position) {
    getBackGround().setBorderWidth(width, position);
  }

  public void setBorderColor(int color, int position) {
    getBackGround().setBorderColor(color, position);
  }

  @Override
  public void setBorderStyle(int borderStyle) {
  }

  @Override
  public void setBackgroundColor(int color) {
    getBackGround().setBackgroundColor(color);
  }

  private CommonBackgroundDrawable getBackGround() {
    if (mBGDrawable == null) {
      mBGDrawable = new CommonBackgroundDrawable();
      Drawable currBGDrawable = getBackground();
      //noinspection deprecation
      super.setBackgroundDrawable(null);
      if (currBGDrawable == null) {
        //noinspection deprecation
        super.setBackgroundDrawable(mBGDrawable);
      } else {
        LayerDrawable layerDrawable = new LayerDrawable(
            new Drawable[]{mBGDrawable, currBGDrawable});
        //noinspection deprecation
        super.setBackgroundDrawable(layerDrawable);
      }
    }
    return mBGDrawable;
  }



  protected void setTextColor(int textColor) {
    if (mLayout != null && mLayout.getText() instanceof SpannableStringBuilder) {
      SpannableStringBuilder textSpan = (SpannableStringBuilder) mLayout.getText();
      ForegroundColorSpan[] spans = textSpan
        .getSpans(0, mLayout.getText().length(), ForegroundColorSpan.class);
      boolean hasSpans = false;
      if (spans != null) {
        for (ForegroundColorSpan span : spans) {
          int start = textSpan.getSpanStart(span);
          int end = textSpan.getSpanEnd(span);
          textSpan.removeSpan(span);
          int spanFlags = Spannable.SPAN_EXCLUSIVE_INCLUSIVE;
          if (start == 0) {
            spanFlags = Spannable.SPAN_INCLUSIVE_INCLUSIVE;
          }
          textSpan.setSpan(new ForegroundColorSpan(textColor), start, end, spanFlags);
        }
      }
      if (spans == null || spans.length == 0) {
        textSpan.setSpan(new ForegroundColorSpan(textColor), 0, textSpan.toString().length(),
          Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
      }
    }
  }

  @Override
  public NativeGestureDispatcher getGestureDispatcher() {
    return mGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {
    mGestureDispatcher = dispatcher;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {

    if (!mNativeGestureEnable) {
      return super.dispatchTouchEvent(event);
    }
    int action = event.getAction();

    if (action == MotionEvent.ACTION_DOWN) {
      mNativeGestureSpan = findNativeGestureSpanForTouch(event);
    }
    if (mNativeGestureSpan != null) {
      boolean flag = mNativeGestureSpan.handleDispatchTouchEvent(this, event);
      if (flag) {
        super.dispatchTouchEvent(event);
        return true;
      } else {
        return super.dispatchTouchEvent(event);
      }
    }
    return super.dispatchTouchEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = super.onTouchEvent(event);
    if (mNativeGestureSpan != null) {
      result |= mNativeGestureSpan.handleTouchEvent(this, event);
    }
    return result;
  }

  public void setNativeGestureEnable(boolean nativeGestureEnable) {
    this.mNativeGestureEnable = nativeGestureEnable;
  }


  private HippyNativeGestureSpan findNativeGestureSpanForTouch(MotionEvent event) {
    HippyNativeGestureSpan span = null;
    if (mLayout == null) {
      return null;
    }

    int x = (int) event.getX();
    int y = (int) event.getY();

    switch (mLayout.getAlignment()) {
      case ALIGN_CENTER: {
        int totalHeight = getHeight() + getPaddingTop() + getPaddingBottom() - mLayout.getHeight();
        int width = (getWidth() - mLayout.getWidth()) / 2;
        x -= width;
        y -= totalHeight / 2;
      }
      break;
      case ALIGN_OPPOSITE: {
        int width = getWidth() - getPaddingRight() - mLayout.getWidth();
        x -= width;
      }

    }
    Layout layout = mLayout;
    int line = layout.getLineForVertical(y);

    int lineStartX = (int) layout.getLineLeft(line);
    int lineEndX = (int) layout.getLineRight(line);

    CharSequence charSequence = layout.getText();
    if (charSequence instanceof Spanned && x >= lineStartX && x <= lineEndX) {
      Spanned spannedText = (Spanned) charSequence;
      int index = mLayout.getOffsetForHorizontal(line, x);

      HippyNativeGestureSpan[] spans = spannedText
          .getSpans(index, index, HippyNativeGestureSpan.class);

      if (spans != null && spans.length > 0) {
        int targetSpanTextLength = charSequence.length();
        for (HippyNativeGestureSpan hippyNativeGestureSpan : spans) {
          int spanStart = spannedText.getSpanStart(hippyNativeGestureSpan);
          int spanEnd = spannedText.getSpanEnd(hippyNativeGestureSpan);
          if (spanEnd > index && (spanEnd - spanStart) <= targetSpanTextLength) {
            span = hippyNativeGestureSpan;
            targetSpanTextLength = (spanEnd - spanStart);
          }
        }
      }
    }
    //extend touch area ,if there is no touch event on the text ,use the first node ,it must be the real node
    if (span == null && charSequence instanceof Spanned) {
      Spanned spanned = (Spanned) charSequence;
      HippyNativeGestureSpan[] spans = spanned
          .getSpans(0, spanned.length(), HippyNativeGestureSpan.class);
      if (spans.length == 1) //only support one text node
      {
        AbsoluteSizeSpan[] absoluteSizeSpan = spanned
            .getSpans(0, spanned.length(), AbsoluteSizeSpan.class);
        if (!spans[0].isVirtual() && absoluteSizeSpan.length == 1) {
          span = spans[0];
        }
      }
    }
    return span;
  }

  private Runnable updateTextTask;
  private void postSetTextColor(final int textColor){
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH){
      //zhaopeng 2023 19以下设备存在文本颜色不能正常变化的问题，所以这里兼容一下
      setTextColor(textColor);
      postInvalidateDelayed(16);
    }else{
      if(updateTextTask != null){
        removeCallbacks(updateTextTask);
      }
      updateTextTask = () -> {
        setTextColor(textColor);
        invalidate();
      };
      postDelayed(updateTextTask,16);
    }
  }

  //zhaopeng add
  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
//
    if(isFocusable() || isDuplicateParentStateEnabled() ) {
      if(mFocusColor != 0){
        final boolean focused = ExtendUtil.stateContainsAttribute(getDrawableState(), android.R.attr.state_focused);
//        if(LogUtils.isDebug()) {
//          Log.d("DrawableStateTAG", "text drawableStateChanged:this:" + getId() + ",color：" + mFocusColor + ",focusedState:" + focused + ",isFocused:" + isFocused());
//        }
        if(focused || isFocused()){
          postSetTextColor(mFocusColor);
          return;
        }
      }
      if(mSelectColor != 0){
        final boolean selected = ExtendUtil.stateContainsAttribute(getDrawableState(), android.R.attr.state_selected);
//        if(LogUtils.isDebug()) {
//          LogUtils.d("HippyTextViewLog", "drawableStateChanged selected:" + (selected || isSelected()) + "，" + this);
//        }
        if(selected || isSelected()){
          postSetTextColor(mSelectColor);
          return;
        }
      }
      postSetTextColor(mNativeTextColor);
    }
  }


    public void setFocusColor(int color) {
      this.mFocusColor = color;
      invalidate();
    }

  public void setSelectColor(int color) {
    this.mSelectColor = color;
    invalidate();
  }

  public void setSelectState(boolean selectState) {
    this.selectState = selectState;
    setSelected(selectState);
  }


  public void setShadowOffsetY(float y){
    mShadowOffsetY = y;
  }

  public void setShadowOffsetX(float x){ mShadowOffsetX = x;}

  public void setShadowOpacity(float opacity){ mShadowOpacity = opacity;}

  public void setShadowRadius(float radius){ mShadowRadius = radius;}

  public void setShadowSpread(float spread){mShadowSpread = spread;}

  public void setShadowColor(int color){ mShadowColor = color;}
}
