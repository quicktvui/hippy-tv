package com.quicktvui.hippyext.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.util.ArrayMap;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.quicktvui.hippyext.views.fastlist.FastAdapter;
import com.quicktvui.hippyext.views.fastlist.ListItemHolder;
import com.quicktvui.hippyext.views.fastlist.PostHandlerView;
import com.quicktvui.hippyext.RenderUtil;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;

import com.tencent.mtt.hippy.dom.flex.FlexConstants;
import com.tencent.mtt.hippy.dom.node.TypeFaceUtil;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.quicktvui.base.ui.StateView;
import com.tencent.mtt.hippy.utils.ExtendUtil;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

public class TVTextView extends TextView implements HippyViewBase , StateView, ListItemHolder {
//  private final boolean isAutoWidth;
//  private final boolean isAutoHeight;
  HippyMap iniProps;
  private GradientDrawable mBackDrawable;
  private boolean isMeasureDirty = false;
  private FastAdapter.ElementNode mDomNode4FastList;
  public boolean createFromNative = false;
  private boolean useTextSpan = false;

  private boolean setAutoMarqueOnFocus = false;
  private boolean updateDirty = false;
  private int currentEllipsize = -1;
  private boolean boldOnFocus = false;
  public TVTextView(Context context,HippyMap  iniProps,boolean createFromNative) {
    super(context);
    this.createFromNative = createFromNative;
    this.iniProps = iniProps;
//    this.isAutoWidth = isAutoWidth();
//    this.isAutoHeight = isAutoHeight();
    this.useTextSpan = iniProps != null && iniProps.containsKey("textSpan");
  }

  public void setSetAutoMarqueOnFocus(boolean setAutoMarqueOnFocus) {
//    Log.i("TVTextViewLog","setSetAutoMarqueOnFocus "+setAutoMarqueOnFocus+",this:"+this);
    this.setAutoMarqueOnFocus = setAutoMarqueOnFocus;
    updateDirty = true;
    invalidate();
    refreshDrawableState();
  }

  public void setBoldOnFocus(boolean boldOnFocus) {
    this.boldOnFocus = boldOnFocus;
    refreshDrawableState();
  }

  public boolean useTextSpan(){
    return useTextSpan;
  }

  private NativeGestureDispatcher mGestureDispatcher;

  private int mFocusColor = Color.TRANSPARENT;
  private int mNormalColor = Color.WHITE;
  private int mSelectColor = Color.TRANSPARENT;
  private int maxWidth = 0;
  private int maxHeight = 0;

  @Override
  public void setEllipsize(TextUtils.TruncateAt where) {
    super.setEllipsize(where);
//    currentEllipsize = where.ordinal();
  }

  private boolean needMeasureHeight = true;

  private boolean forceUpdate = false;

  public void setFocusColor(int mFocusColor) {
    this.mFocusColor = mFocusColor;
    invalidate();
  }

  @Override
  public int getMaxWidth() {
    return maxWidth;
  }


  public void setSelectColor(int selectColor) {
    this.mSelectColor = selectColor;
    invalidate();
  }

  private void requestMeasureHeight(){
      this.needMeasureHeight = true;
  }
  private void consumeMeasure(){
    this.needMeasureHeight = false;
  }

  public void bindNode(FastAdapter.ElementNode mDomNode4FastList) {
    this.mDomNode4FastList = mDomNode4FastList;
  }

  @Override
  public void setMaxWidth(int maxWidth) {
    this.maxWidth = maxWidth;
  }


  @Override
  protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    boolean result = super.onTouchEvent(event);
    if (mGestureDispatcher != null)
    {
      result |= mGestureDispatcher.handleTouchEvent(event);
    }
    return result;
  }


  @Override
  public boolean isSelected() {
    if(forceUpdate){
      forceUpdate = false;
      return !super.isSelected();
    }
    return super.isSelected();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
  }

  @Override
  public void setTextColor(int color) {
    this.mNormalColor = color;
    super.setTextColor(color);
  }

  @Override
  public void setSelected(boolean selected) {
    super.setSelected(selected);
  }

  private void forceSetSelect(boolean b){
    forceUpdate = true;
    super.setSelected(b);
  }




  @Override
  public NativeGestureDispatcher getGestureDispatcher()
  {
    return mGestureDispatcher;
  }

  @Override
  public void setGestureDispatcher(NativeGestureDispatcher dispatcher)
  {
    mGestureDispatcher = dispatcher;
  }

  private void postSetColor(final int color){
    super.setTextColor(color);
    postInvalidateDelayed(16);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if(mDomNode4FastList != null && mDomNode4FastList.isHeightWrapContent) {
      if (LogUtils.isDebug()) {
        Log.e("AutoMeasure", "FIX_ITEM_SIZE  onMeasure title: " + getText() + ",getMeasuredHeight:" + getMeasuredHeight()+",getMeasuredWidth:"+getMeasuredWidth()+",this:"+hashCode());
      }
      measureTextHeight(mDomNode4FastList,getMeasuredHeight());
    }
//    if(mDomNode4FastList != null && mDomNode4FastList.isWidthWrapContent){
//        //设置text时，由于不能及时测试量结果 ，所以这里需要重新layout一下。
//      measureTextWidth(mDomNode4FastList,getMeasuredWidth());
//    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if(mBackDrawable != null){
      mBackDrawable.setBounds(0,0,w,h);
    }
  }

  public void setGradientDrawable(HippyMap map){

    if(map == null){
      mBackDrawable = null;
    }else {
      this.mBackDrawable = HippyViewGroup.createGradientDrawable(this,map);

    }

    invalidate();
  }


  @Override
  public void draw(Canvas canvas) {
    if(mBackDrawable != null){
      if(mBackDrawable.getBounds().isEmpty()){
        mBackDrawable.setBounds(new Rect(0,0,getWidth(),getHeight()));
      }
      mBackDrawable.draw(canvas);
    }
    super.draw(canvas);
  }



  @Override
  protected void drawableStateChanged() {

    super.drawableStateChanged();
    int[] states = getDrawableState();

    ExtendUtil.handleShowOnState(this,states,showOnState);
    ExtendUtil.handleCustomShowOnState(this, mCustomStateList,showOnStateCustom);
//
    if(isFocusable() || isDuplicateParentStateEnabled() ) {
      final boolean focused = ExtendUtil.stateContainsAttribute(states, android.R.attr.state_focused);
      final boolean selected = ExtendUtil.stateContainsAttribute(states, android.R.attr.state_selected);
      if(setAutoMarqueOnFocus) {
        //Log.v("TVTextViewLog","drawableStateChanged focused:"+focused+",selected:"+selected+",currentEllipsize:"+currentEllipsize);
        if (focused) {
          //Log.i("TVTextViewLog","+++change MARQUEE on focused text:"+getText());
          setEllipsize(TextUtils.TruncateAt.MARQUEE);
          setSingleLine();
          setMarqueeRepeatLimit(-1);
          currentEllipsize = TextUtils.TruncateAt.MARQUEE.ordinal();
          if(typeStyle != 0) {
            hadSetType = false;
          }
          if(boldOnFocus){
            hadSetType = false;
            setTypeStyle("bold");
          }
          RenderUtil.reLayoutView(this);
          postInvalidateDelayed(16);
        }else{
            setEllipsize(TextUtils.TruncateAt.END);
            if(typeStyle != 0) {
              hadSetType = false;
            }
            if(boldOnFocus){
              hadSetType = false;
              setTypeStyle("normal");
            }
            RenderUtil.reLayoutView(this);
            postInvalidateDelayed(16);
            currentEllipsize = TextUtils.TruncateAt.END.ordinal();
//          }
        }
        updateDirty = false;
      }
      //fixme : 这里需要解决当ellipsize非4时不生效的问题
//      else {
//        if (boldOnFocus) {
//          setSingleLine();
//          hadSetType = false;
//          if (focused) {
//            setTypeStyle("bold");
//          } else {
//            setTypeStyle("normal");
//          }
//          RenderUtil.reLayoutView(this);
//          postInvalidateDelayed(16);
//        }
//      }
      if(focused && selected){
        if(mFocusColor != 0){
            forceSetSelect(true);
            postSetColor(mFocusColor);
            return;
        }
      }else{
        if(mFocusColor != 0){
          if(focused){
            forceSetSelect(true);
            postSetColor(mFocusColor);
            return;
          }
        }
        if(mSelectColor != 0){
          if(selected){
            postSetColor(mSelectColor);
            forceSetSelect(false);
            return;
          }
        }
      }

      postSetColor(mNormalColor);
      forceSetSelect(focused);
    }
  }

  private int showOnState[];

  @Override
  public void setShowOnState(int[] showOnState) {
    this.showOnState = showOnState;
  }

  /** custom showOnState
   * --------------------
   */
  private String[] showOnStateCustom;
  ArrayMap<String,Boolean> mCustomStateList = null;
  @Override
  public void setCustomState(String state, boolean on) {
    if (mCustomStateList == null) {
      mCustomStateList = new ArrayMap<>();
    }
    mCustomStateList.put(state,on);
    refreshDrawableState();
  }

  @Override
  public void setShowOnCustomState(String[] showOnState) {
    this.showOnStateCustom = showOnState;
  }

  /**custom showOnState
   * ----------------
   */

  public boolean isAutoWidth() {
    return iniProps != null && iniProps.containsKey("autoWidth");
  }
  public boolean isAutoHeight() {
    return iniProps != null && iniProps.containsKey("autoHeight");
  }

  private int typeStyle = Typeface.NORMAL;
  private String family = null;
  private boolean hadSetType = false;
  public void setTypeStyle(String type){
    switch (type){
      case "bold":
        typeStyle = Typeface.BOLD;
        break;
      case "italic":
        typeStyle = Typeface.ITALIC;
        break;
      case "bold_italic":
        typeStyle = Typeface.BOLD_ITALIC;
        break;
      default:
        typeStyle = Typeface.NORMAL;
        break;
    }
    doTypeFace();
  }
  public void setFamily(String family){
    if (TextUtils.isEmpty(family)) return;

    this.family = family;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (!hadSetType){
      hadSetType = true;
      doTypeFace();
    }
    super.onLayout(changed, left, top, right, bottom);
  }

  void doTypeFace(){
    Typeface typeface;
    if (!TextUtils.isEmpty(family)) {
      typeface = TypeFaceUtil.getTypeface(family, typeStyle,null);
    } else {
      typeface = Typeface.defaultFromStyle(typeStyle);
    }
    if (typeface != null) {
      setTypeface(typeface);
    }
  }

  public void setTextSpan(HippyMap spanMap){
    if (LogUtils.isDebug() && mDomNode4FastList != null) {
      Log.i("AutoMeasure", "FIX_ITEM_SIZE setTextSpan spanMap:"+spanMap+",bindNode:"+mDomNode4FastList.getTemplateNode());
    }
    if (spanMap != null){
      //bugfix:这里使用textSpan设置过之后将useTextSpan设置成true,以防止出现使用TextSpan后，又被setText将文本置空
      useTextSpan = true;
      String title = "";
      if (spanMap.containsKey("text")) {
        title = spanMap.getString("text");
      }
      if (TextUtils.isEmpty(title))return;
      SpannableString sStr = new SpannableString(title);
      if (spanMap.containsKey("spanAttr")) {
        HippyArray spanAttr = spanMap.getArray("spanAttr");
        if (spanAttr != null && spanAttr.size() > 0){
          for (int i = 0; i < spanAttr.size(); i++) {
            HippyMap span = spanAttr.getMap(i);
            String type = span.getString("type");
            HippyArray array = span.getArray("value");
            switch (type){
              case "size":
                sStr = parseSizeSpan(sStr,array);
                break;
              case "color":
                sStr = parseColorSpan(sStr,array);
                break;
            }
          }
        }
        setText(sStr);
        if (LogUtils.isDebug()) {
          Log.e("AutoMeasure", "FIX_ITEM_SIZE setTextSpan title " + title + ",sStr:" + sStr+",isMeasureDirty:"+isMeasureDirty);
        }
      }
    }
  }


  @Override
  public void setTextSize(int unit, float size) {
    super.setTextSize(unit, size);
    if (LogUtils.isDebug()) {
      Log.i("AutoMeasure", "FIX_ITEM_SIZE setTextSize size " + size +",this:"+hashCode());
    }

  }

  private SpannableString parseSizeSpan(SpannableString sStr, HippyArray sizeSpanArray){
    if (sStr != null && sizeSpanArray != null && sizeSpanArray.size() > 0){
      assert sizeSpanArray.size() > 2;
      int size = sizeSpanArray.getInt(0);
      int startPos = sizeSpanArray.getInt(1);
      int endPos = sizeSpanArray.getInt(2);
      sStr.setSpan(new AbsoluteSizeSpan((int) PixelUtil.dp2px(size)), startPos, endPos, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }
    return sStr;
  }

  private SpannableString parseColorSpan(SpannableString sStr,HippyArray sizeSpanArray){
    if (sStr != null && sizeSpanArray != null && sizeSpanArray.size() > 0){
      assert sizeSpanArray.size() > 2;
      String spanColor = sizeSpanArray.getString(0);
      int color = Color.parseColor(spanColor);
      int startPos = sizeSpanArray.getInt(1);
      int endPos = sizeSpanArray.getInt(2);
      sStr.setSpan(new ForegroundColorSpan(color), startPos, endPos, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }
    return sStr;
  }

  protected PostHandlerView mPostView;
  private boolean onBindNew = false;
  private boolean enablePostTask = false;
  public void setEnablePostTask(boolean enablePostTask) {
    this.enablePostTask = enablePostTask;
  }
  int loadDelay = 20;

  public void setDelayLoad(int delay) {
    this.loadDelay = delay;
  }

  @Override
  public void onItemBind() {
//    if(typeStyle == 1){
//      hadSetType = false;
//      setTypeStyle("bold");
//      RenderUtil.reLayoutView(this);
//    }
//    onResetBeforeCache();
  }
//
//  @Override
//  public void setRootPostHandlerView(PostHandlerView pv) {
//    this.mPostView = pv;
//  }

//  @Override
//  public void setText(CharSequence text, BufferType type) {
//    if(TextUtils.isEmpty(text)){
//      super.setText(text, type);
//    }else {
//      if(isPostTaskEnabled()){
//        mPostView.postTask(POST_TASK_CATEGORY_TEXT, hashCode(), () -> {
//          super.setText(text, type);
//        },loadDelay);
//      }else{
//        super.setText(text, type);
//      }
//    }
//  }




  @Override
  public void setText(CharSequence text, BufferType type) {
    super.setText(text, type);
    if (LogUtils.isDebug() && mDomNode4FastList != null) {
      Log.e("AutoMeasure", "FIX_ITEM_SIZE setText with BufferType text:"+text+",this:"+hashCode()+",bindNode:"+mDomNode4FastList.getTemplateNode());
    }
    isMeasureDirty = true;
    if(useTextSpan && mDomNode4FastList != null && mDomNode4FastList.isWidthWrapContent){
      measureTextWidth(mDomNode4FastList);
    }
    if (mDomNode4FastList != null && mDomNode4FastList.isHeightWrapContent) {
     //bugfix:存在因没requestLayout导致部分文本大小重新计算错误，特别在一行及俩行文本切换时
      requestLayout();
    }
//    Log.i("TVTextViewLog","setText text:"+text);
//    if(getEllipsize() == TextUtils.TruncateAt.MARQUEE || setAutoMarqueOnFocus) {
//      RenderUtil.reLayoutView(this);
//      postInvalidateDelayed(16);
//      refreshDrawableState();
//    }
  }




  @Override
  public void resetProps() {

  }

  @Override
  public void clear() {

  }

  protected boolean isPostTaskEnabled(){
    return enablePostTask && mPostView != null;
  }


  @Override
  public void onResetBeforeCache() {
    if(!TextUtils.isEmpty(getText())){
      if(typeStyle != 0) {
        hadSetType = false;
      }
      //zhaopeng 2022 7 13 TextView重用时，如果是marquee的状态，重新设置文本后，可能存在位置不正确的问题，需要手动layout一下
      if(getEllipsize() == TextUtils.TruncateAt.MARQUEE || setAutoMarqueOnFocus || boldOnFocus || typeStyle != 0) {
//        if (LogUtils.isDebug()) {
//          Log.i("TVTextViewLog", "onResetBeforeCache MARQUEE tx:" + getText());
//        }
        RenderUtil.reLayoutView(this);
//        refreshDrawableState();
//        postInvalidateDelayed(16);
      }
    }
  }



  @Override
  public void notifySaveState() {

  }

  @Override
  public void notifyRestoreState() {

  }


  public void measureTextHeight(FastAdapter.ElementNode node,int height){
    final TVTextView tx = this;
    if (LogUtils.isDebug()) {
      Log.i("AutoMeasure", "####FIX_ITEM_SIZE measureTextHeight this:"+hashCode());
    }
    final CharSequence text = tx.getText();
    if(!TextUtils.isEmpty(text)){
      if (LogUtils.isDebug()) {
        final float padding = node.getStylePadding(1) + node.getStylePadding(3);
        Log.d("AutoMeasure", "####FIX_ITEM_SIZE measureTextHeight IN TVTextView padding:" + padding + ",textHeight:" + (float) height + ",width:" + ((float) height + padding) + ",text:" + tx.getText()+",this:"+hashCode());
      }
      if(height < 1){
        //bugfix:当调试<1 时将尺寸标记为UNDEFINED
        if(LogUtils.isDebug()) {
          Log.e("AutoMeasure", "measureTextHeight  error height == 0 tx : " + tx.getText() + ",getMeasuredWidth:" + getMeasuredWidth());
        }
        node.setStyleHeight(FlexConstants.UNDEFINED);
      }else {
        node.setStyleHeight(height);
      }
      //tx.setPadding((int) node.getStylePadding(0), (int) node.getStylePadding(1), (int) node.getStylePadding(2), (int) node.getStylePadding(3));
    }else{
      if (LogUtils.isDebug()) {
        Log.e("AutoMeasure", "####FIX_ITEM_SIZE measureTextHeight  return tx.getPaint() != null || TextUtils.isEmpty(text),text:" + text);
      }
      node.setStyleHeight(0);
    }
  }

  public void measureTextWidth(FastAdapter.ElementNode node){
    final TVTextView tx = this;
    final CharSequence text = tx.getText();
    if( tx.getPaint() != null || !TextUtils.isEmpty(text)){
      //final int width = (int) Math.max(maxWidth, getPaint().measureText(text,0,text.length()));
      if(node.getStyleWidth() < 1 || isMeasureDirty) {
        final float padding = node.getStylePadding(0) + node.getStylePadding(2);
        final float textWidth = Layout.getDesiredWidth(text, tx.getPaint());
        if (LogUtils.isDebug()) {
          Log.d("AutoMeasure", "FIX_ITEM_SIZE measureTextWidth IN TVTextView padding:" + padding + ",textWidth:" + textWidth + ",width:" + (textWidth + padding) + ",Node:" + node + ",text:" + tx.getText()+",this:"+hashCode());
        }
        final int totalWidth = (int) Math.max(tx.getMaxWidth(), textWidth + padding);
        node.setStyleWidth(totalWidth);
        tx.setPadding((int) node.getStylePadding(0), (int) node.getStylePadding(1), (int) node.getStylePadding(2), (int) node.getStylePadding(3));
//        FastAdapter.updateLayoutF(this,node.getLayoutX(),node.getLayoutY(),totalWidth,node.getLayoutHeight());
//        measureParentIfNeed(node);
      }else{
        if (LogUtils.isDebug()) {
          Log.e("AutoMeasure", "FIX_ITEM_SIZE measureTextWidth return onStyleWidth > 1 Node:" + node + ",text:" + tx.getText()+",isMeasureDirty:"+isMeasureDirty);
        }
      }
    }else{
      if (LogUtils.isDebug()) {
        Log.e("AutoMeasure", "FIX_ITEM_SIZE measureTextWidth  return tx.getPaint() != null || TextUtils.isEmpty(text),text:" + text);
      }
      node.setStyleWidth(0);
//      FastAdapter.updateLayoutF(this,node.getLayoutX(),node.getLayoutY(),0,node.getLayoutHeight());
//      measureParentIfNeed(node);
    }
  }




}
