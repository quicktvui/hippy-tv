package com.quicktvui.hippyext.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import com.quicktvui.hippyext.views.fastlist.Utils;
import com.quicktvui.base.ui.tag.FontTag;
import com.quicktvui.base.ui.tag.HtmlTagHandler;
import com.quicktvui.base.ui.tag.SpanTag;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.dom.node.NodeProps;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.utils.PixelUtil;

@HippyController(name = TextViewController.CLASS_NAME)
public class TextViewController extends HippyViewController<TVTextView> {
  public static final String CLASS_NAME = "TextView";
  @Override
  protected View createViewImpl(Context context) {
    return null;
  }

  @Override
  protected View createViewImpl(Context context, HippyMap iniProps) {
    return createViewImpl(context,iniProps,false);
  }

  public View createViewImpl(Context context, HippyMap iniProps,boolean createFromNative) {
    final TVTextView text = new TVTextView(context,iniProps,createFromNative);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if(text.getContext().getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.P) {
        if(iniProps.getInt(NodeProps.TEXT_LINES) > 1) {
          text.setIncludeFontPadding(false);
          //在targetSDK 28以上会带FallbackLineSpacing,从而导致不台平台文本展示不一致，这里去掉
          text.setFallbackLineSpacing(false);
        }
      }
    }
    //Fixme 这里为了天猫4k机器bug，临时写死
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (PixelUtil.getScreenWidth() > 3000) {
        text.setFallbackLineSpacing(true);
        text.setLineHeight(4);
      }
    }
    if(iniProps.containsKey("enablePostTask")){
      text.setEnablePostTask(true);
    }
    if(!createFromNative && iniProps.containsKey("text")){//从FastList中创建时，负责过滤掉${}类型的文本
      setText(text,iniProps.getString("text"));
    }
    return text;
  }

  @HippyControllerProps(name = "lineSpacing", defaultType = HippyControllerProps.NUMBER)
  public void setLineSpacing(TVTextView tvTextView,int space){
    tvTextView.setLineSpacing(space,1);
  }

  @Override
  protected void onCreateViewByCache(View view, String type, HippyMap iniProps) {
    super.onCreateViewByCache(view, type, iniProps);
    if(view instanceof TVTextView && !((TVTextView) view).createFromNative){
      if(iniProps.containsKey("text")){
        setText((TVTextView) view,iniProps.getString("text"));
      }
    }
  }

  @Override
  public void dispatchFunction(TVTextView view, String functionName, HippyArray var) {
    super.dispatchFunction(view, functionName, var);
    switch (functionName){
      case "setText":
        this.setText(view,var.getString(0));
        break;
      case "setTextSize":
        this.setTextSize(view, var.getInt(0));
        break;
      case "setTextColor":
        this.setTextColor(view, var.getString(0));
        break;
      case "textSpan":
        this.setItemJson(view, var.getMap(0));
        break;
    }
  }

  @Override
  protected void updateExtra(View view, Object object) {
    super.updateExtra(view, object);
  }

  @HippyControllerProps(name = NodeProps.TEXT, defaultType = HippyControllerProps.STRING, defaultString = "")
  public void setText(TVTextView tvTextView,String text){

//    tvTextView.setText(Html.fromHtml(text));
      if(!tvTextView.useTextSpan()){
        if(text != null && text.startsWith("<")){//兼容html样式的文本
          setTextHtml(tvTextView,text);
        }else{
          tvTextView.setText(text);
        }
      }else{
        if(LogUtils.isDebug()){
          Log.w("TextViewController","setText return on use TextSpan");
        }
      }
  }

  @HippyControllerProps(name = "textHtml", defaultType = HippyControllerProps.STRING, defaultString = "")
  public void setTextHtml(TVTextView tvTextView,String text){
//    tvTextView.setText(Html.fromHtml(text));
    HtmlTagHandler htmlTagHandler = new HtmlTagHandler();
    htmlTagHandler.registerTag("span",new SpanTag(tvTextView.getContext()));
    htmlTagHandler.registerTag("font",new FontTag(tvTextView.getContext()));
    try {
      Spanned spanned = Html.fromHtml(text, htmlTagHandler, htmlTagHandler);
      tvTextView.setText(spanned);
    } catch (Throwable ignored) {
    }
  }

  @HippyControllerProps(name = "textSpan", defaultType = HippyControllerProps.MAP)
  public void setItemJson(TVTextView view, HippyMap json) {
    if (view != null) {
      view.setTextSpan(json);
    }
  }

  @HippyControllerProps(name = "typeface", defaultType = HippyControllerProps.STRING)
  public void setTypeface(TVTextView view, String typeFace) {
    if (view != null) {
      view.setTypeStyle(typeFace);
    }
  }


  @HippyControllerProps(name = NodeProps.FONT_FAMILY, defaultType = HippyControllerProps.STRING)
  public void setFontFamily(TVTextView view, String family){
    if (view != null)
      view.setFamily(family);
  }

  @HippyControllerProps(name = NodeProps.TEXT_GRAVITY_NUMBER, defaultType = HippyControllerProps.NUMBER, defaultNumber = Gravity.START)
  public void setGravity(TVTextView tvTextView,int gravity){
    tvTextView.setGravity(gravity);
  }

  @HippyControllerProps(name = NodeProps.TEXT_GRAVITY, defaultType = HippyControllerProps.STRING)
  public void setGravityByString(TVTextView tvTextView,String gravity){
    if(gravity.contains("|")){
      final String[] gs = gravity.split("\\|");
      final int gravity1 = getGravity(gs[0]);
      final int gravity2 = getGravity(gs[1]);
      tvTextView.setGravity(gravity1 | gravity2);
    }else {
      tvTextView.setGravity(getGravity(gravity));
    }
  }


  @HippyControllerProps(name = NodeProps.FONT_SIZE, defaultType = HippyControllerProps.NUMBER)
  public void setFontSize(TVTextView tvTextView,int size){
    setTextSize(tvTextView,size);
//    Log.i("ZHAOPENG","createTextViewImpl size:"+size);

//    setLineHeight(tvTextView,size + 2);
//    RenderUtil.reLayoutView(tvTextView);

  }

  @HippyControllerProps(name = "paddingRect", defaultType = HippyControllerProps.ARRAY)
  public void setPadding(TVTextView view, HippyArray array){
    if(LogUtils.isDebug()){
      Log.d("TV_TEXT_VIEW","setPaddingRect array : "+array+",View id:"+view.getId()+",text:"+view.getText());
    }
      if(array == null){
        view.setPadding(0,0,0,0);
      }else {
        view.setPadding(
          Utils.toPX(array.getInt(0)),
          Utils.toPX(array.getInt(1)),
          Utils.toPX(array.getInt(2)),
          Utils.toPX(array.getInt(3))
          );
      }
  }

  private int getGravity(String gravity){
    switch (gravity) {
      case "center":
        return Gravity.CENTER;
      case "top":
        return Gravity.TOP;
      case "bottom":
        return Gravity.BOTTOM;
      case "end":
        return Gravity.END;
      case "centerHorizontal":
        return Gravity.CENTER_HORIZONTAL;
      case "centerVertical":
        return Gravity.CENTER_VERTICAL;
      case "start":
      default:
        return Gravity.START;
    }
  }

  @HippyControllerProps(name = NodeProps.TEXT_ALIGNMENT, defaultType = HippyControllerProps.NUMBER)
  public void setTextAlignment(TVTextView tvTextView,int align){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      tvTextView.setTextAlignment(align);
    }else{
      Log.e("hippy","setTextAlignment error , need JELLY_BEAN_MR1");
    }
  }

  @HippyControllerProps(name = NodeProps.ELLIPSIZE_MODE, defaultType = HippyControllerProps.NUMBER, defaultNumber = 2)
  public void setEllipsize(TVTextView tvTextView,int where){
    switch (where){
      case 0 :
        tvTextView.setSetAutoMarqueOnFocus(false);
        tvTextView.setEllipsize(TextUtils.TruncateAt.START);
        break;
      case 1 :
        tvTextView.setSetAutoMarqueOnFocus(false);
        tvTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        break;
      case 2 : {
        tvTextView.setSetAutoMarqueOnFocus(false);
        tvTextView.setEllipsize(TextUtils.TruncateAt.END);
        break;
      }
      case 3 :{
        tvTextView.setSetAutoMarqueOnFocus(false);
        tvTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvTextView.setSingleLine();
        tvTextView.setMarqueeRepeatLimit(-1);
        break;
      }
      case 4:{
       // Log.i("TVTextViewLog","setEllipsize 4");
        tvTextView.setSetAutoMarqueOnFocus(true);
        tvTextView.setEllipsize(TextUtils.TruncateAt.END);
        break;
      }
    }

  }


  @HippyControllerProps(name = NodeProps.TEXT_LINES, defaultType = HippyControllerProps.NUMBER)
  public void setLines(TVTextView tvTextView,int lines){
    tvTextView.setLines(lines);

  }

  @HippyControllerProps(name = NodeProps.TEXT_MAX_LINES, defaultType = HippyControllerProps.NUMBER)
  public void setMaxLines(TVTextView tvTextView,int lines){
    tvTextView.setMaxLines(lines);
  }

  @HippyControllerProps(name = NodeProps.TEXT_COLOR, defaultType = HippyControllerProps.STRING)
  public void setTextColor(TVTextView tvTextView,String color){
    final int colorInt = Color.parseColor(color);
    tvTextView.setTextColor(ColorStateList.valueOf(colorInt));
  }

  @HippyControllerProps(name = "maxWidth", defaultType = HippyControllerProps.NUMBER)
  public void setMaxWidth(TVTextView tvTextView,int maxWidth) {
    tvTextView.setMaxWidth((int) PixelUtil.dp2px(maxWidth));
  }


  @HippyControllerProps(name = NodeProps.TEXT_SIZE, defaultType = HippyControllerProps.NUMBER)
  public void setTextSize(TVTextView tvTextView,int size){
//    tvTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    tvTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) Math.ceil(PixelUtil.dp2px(size)));
  }

  @HippyControllerProps(name = NodeProps.LINE_HEIGHT, defaultType = HippyControllerProps.NUMBER)
  public void setLineHeight(TVTextView tvTextView,int height){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      tvTextView.setLineHeight(height);
    }
  }

  @HippyControllerProps(name = NodeProps.TEXT_SELECT, defaultType = HippyControllerProps.BOOLEAN)
  public void setSelect(TVTextView tvTextView,boolean enable){
    tvTextView.setSelected(enable);
  }

  @HippyControllerProps(name = "fallbackLineSpacing", defaultType = HippyControllerProps.BOOLEAN)
  public void setFallbackLineSpacing(TVTextView tvTextView,boolean enable){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      tvTextView.setFallbackLineSpacing(enable);
    }
  }

  @HippyControllerProps(name = "includeFontPadding", defaultType = HippyControllerProps.BOOLEAN)
  public void setIncludeFontPadding(TVTextView tvTextView,boolean enable){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      tvTextView.setIncludeFontPadding(enable);
    }
  }

  @HippyControllerProps(name = NodeProps.TEXT_SELECT)
  public void setSelect(TVTextView tvTextView){
    tvTextView.setSelected(true);
  }

  @HippyControllerProps(name = "boldOnFocus")
  public void setBoldOnFocus(TVTextView tvTextView,boolean enable){
    tvTextView.setBoldOnFocus(enable);
  }

  //此处vue是怎么传值的
  @HippyControllerProps(name = "gradientBackground", defaultType = HippyControllerProps.MAP)
  public void setGradientBG(TVTextView view, HippyMap hippyMap){
    if(LogUtils.isDebug()) {
      Log.d("hippy", "ViewGroupController setGradientDrawableBackground view:" + view + ",map:" + hippyMap);
    }
    view.setGradientDrawable(hippyMap);
  }


  @Override
  public void onFocusChange(View v, boolean hasFocus) {
    super.onFocusChange(v, hasFocus);
    if(v instanceof TVTextView ){
      v.setSelected(hasFocus);
    }
  }

  @Override
  public void dispatchFunction(TVTextView view, String functionName, HippyArray params, Promise promise) {
    super.dispatchFunction(view, functionName, params, promise);
    switch (functionName){
      case "getText":
        if(promise != null){
          promise.resolve(view.getText().toString());
        }
        break;
    }
  }
}
