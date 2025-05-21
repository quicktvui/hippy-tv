package com.quicktvui.hippyext.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.quicktvui.hippyext.RenderUtil;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;

import java.util.List;


public class TVButtonView extends FrameLayout implements HippyViewBase {

    TextView tx;

    Drawable drawable;
    Drawable drawableFocused;
    Rect padding = new Rect();

    RectF markRect;

    Paint mPaint;

    int markWidth = 36;
    int markHeight = 6;
    int rounder = 4;
    int markMargin = 2;

    boolean enableMark = false;
    boolean needUpdate = true;
    boolean isAutoWidth = false;

    public static final String TAG = "TVButtonViewLog";

    public TVButtonView(@NonNull Context context,HippyMap iniProps) {
        super(context);

        tx = new TextView(getContext());
        tx.setDuplicateParentStateEnabled(true);
        tx.setSingleLine();
//        tx.setEllipsize(TextUtils.TruncateAt);

        setClipChildren(false);
//        setBackgroundResource(R.drawable.item_background);
        if(iniProps != null && iniProps.containsKey("autoWidth")){
          this.isAutoWidth = true;
        }
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        tx.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(tx,lp);


        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);

    }

    public void setColorStateList(ColorStateList color){
        this.tx.setTextColor(color);
        invalidate();
    }

    public void setColorStateListMap(HippyMap map){
        final ColorStateList color = TemplateUtil.createColorStateListByMap(map);
        this.tx.setTextColor(color);
        invalidate();
    }

    public void setTextSize(int textSize){
        tx.setTextSize(textSize);
        updateSize();
    }

    public void setMarkWidth(int width){
        this.markWidth = width;
        updateSize();
    }

    public void setMarkHeight(int markHeight) {
        this.markHeight = markHeight;
        updateSize();
    }

    public void setMarkMargin(int margin) {
        this.markMargin = margin;
        updateSize();
    }

  @Override
  public void requestLayout() {
    super.requestLayout();
    RenderUtil.requestNodeLayout(this);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    //CustomControllerHelper.updateLayout(tx,left, top, right - left, bottom - top);

  }

  public void setEnableMark(boolean enableMark) {
        this.enableMark = enableMark;
        if(enableMark) {
            this.markRect = new RectF();
        }else{
            this.markRect = null;
        }
        updateSize();
    }

    public void setMarkRounder(int rounder) {
        this.rounder = rounder;
        invalidate();
    }

    public void setMarkColor(int color){
        this.mPaint.setColor(color);
        invalidate();
    }
//    public void setStateDrawable(@NotNull Drawable drawable) {
//        this.drawable = drawable;
//        drawable.setVisible(false,false);
//        updateSize();
//    }

    public void setStateDrawableMap(HippyMap map) {
        final List<? extends Drawable> list = TemplateUtil.createStateListDrawableByMap(map);
        ///Log.d(TAG,"setStateDrawableMap :"+list);
        if(list .size() == 2) {
            this.drawable = list.get(0);
            this.drawableFocused = list.get(1);
        }
        updateSize();
    }


    public void updateSize(){
        this.needUpdate = true;
        this.invalidate();
    }

    public void setBackgroundPadding(int[] padding){
        if(padding != null && padding.length == 2){
            final int h = padding[0];
            final int v = padding[1];
            this.padding = new Rect(-h,-v,h,v);
        }else{
            if(this.padding != null) {
                this.padding.setEmpty();
            }
        }
        updateSize();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);


        updateSize();
    }

    void validSize(){
        if(needUpdate) {
            needUpdate = false;
            final int w = getWidth();
            final int h = getHeight();
            Log.d(TAG,"validSize w:"+w+",h:"+h);
            if(drawable != null) {
                drawable.setBounds(padding.left, padding.top, w + padding.right, h + padding.bottom);
            }
            if(drawableFocused != null){
                drawableFocused.setBounds(padding.left, padding.top, w + padding.right, h + padding.bottom);
            }
            if(markRect != null){
                if (w > 0 & h > 0) {
                    int left = (int) ((w - markWidth) * 0.5f);
                    int top = h + markMargin;
                    markRect.set(left, top, left + markWidth, top + markHeight);
                }
            }
            fixSize();
        }

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        validSize();
        if(drawable != null && drawable.isVisible()) {
            drawable.draw(canvas);
        }
        if(drawableFocused != null && drawableFocused.isVisible()){
            drawableFocused.draw(canvas);
        }
        if(markRect != null && isSelected() && !isFocused()){
            canvas.drawRoundRect(markRect,rounder,rounder,mPaint);
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        refreshState();
    }

    protected void refreshState(){
        final int[] state = getDrawableState();
        if(isFocusable() || isDuplicateParentStateEnabled() ) {
            final boolean focused = ExtendUtil.stateContainsAttribute(state, android.R.attr.state_focused);
            Log.d(TAG,"refreshState focused:"+focused);
            if(focused){
                if(drawableFocused != null){
                    drawableFocused.setVisible(true,false);
                    invalidate();
                }
                if(drawable != null) {
                    drawable.setVisible(false, false);
                    invalidate();
                }
            }else{
                if(drawable != null) {
                    drawable.setVisible(true, false);
                    invalidate();
                }
                if(drawableFocused != null){
                    drawableFocused.setVisible(false,false);
                    invalidate();
                }
            }
        }
    }


    public void setText(String text){
        tx.setText(text);
        updateSize();
    }


    void fixSize(){
      if(LogUtils.isDebug()){
        Log.d(TAG,"fixSize text:"+tx.getText());
      }
      if(tx != null && isAutoWidth) {
        final TextButtonNode node = (TextButtonNode) getHippyContext().getDomManager().getNode(getId());
        if(node == null){
          needUpdate = true;
          return;
        }
        if(LogUtils.isDebug()){
          Log.e(TAG,"fixSize measure getWidth:"+getWidth()+",node getPaddingWidth:"+node.getPaddingWidth()+",text:"+tx.getText());
        }
          int width;
          if(TextUtils.isEmpty(tx.getText())){
            width = 0;
          }else{
//            width = (int) tx.getPaint().measureText(tx.getText(), 0, tx.getText().length());
            width = (int) Layout.getDesiredWidth(tx.getText(), 0, tx.getText().length(),tx.getPaint());
          }
          if(LogUtils.isDebug()){
            Log.e(TAG,"fixSize measure width:"+width+",node:"+node+",text:"+tx.getText());
          }
          node.setTextWidth(width);
          node.markUpdated();
      }
    }

    HippyEngineContext getHippyContext(){
      return ((HippyInstanceContext)getContext()).getEngineContext();
    }

    @Override
    public NativeGestureDispatcher getGestureDispatcher() {
        return null;
    }

    @Override
    public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {

    }
}
