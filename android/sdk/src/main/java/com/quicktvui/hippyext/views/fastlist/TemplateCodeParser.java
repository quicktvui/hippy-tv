package com.quicktvui.hippyext.views.fastlist;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.utils.LogUtils;

import java.util.Arrays;

public class TemplateCodeParser {
  static final String TAG = "TemplateCodeParser";

  public static final String PENDING_PROP_TRANSLATION = "translation";
  public static final String PENDING_PROP_SIZE = "size";
  public static final String PENDING_PROP_LAYOUT = "layout";
  public static final String PENDING_PROP_FLEX_STYLE = "flexStyle";
  public static final String PLACEHOLDER_STYLE = "placeholderStyle";
  public static final String ITEM_SID = "itemSID";
  @Deprecated
  public static final String PENDING_PROP_EVENT_CLICK = "eventClick";
  @Deprecated
  public static final String PENDING_PROP_EVENT_FOCUS = "eventFocus";
  public static final String PENDING_PROP_SHOW_IF = "showIf";
  public static final String PENDING_PROP_CREATE_IF = "createIf";

  public static final String[] PENDING_PROPS_EVENT = new String[]{
//    FastAdapter.PENDING_PROP_TRANSLATION,
//    FastAdapter.PENDING_PROP_SIZE,
//    FastAdapter.PENDING_PROP_LAYOUT,
//    FastAdapter.PENDING_PROP_FLEX_STYLE,
    PENDING_PROP_EVENT_CLICK,
    PENDING_PROP_EVENT_FOCUS,
  };

  static String parsePendingProp(Object object){
    if(object instanceof String){
      final String value =  ((String) object).trim();
//      if(value.contains("=")){
//        //等式不是pendingProp
//        return null;
//      }
      if(value.length() > 3 && value.startsWith("${") ){
        return value.substring(2,value.length() - 1);
      }
    }
    return null;
  }

//  static boolean isEquationProp(String prop){
//    return PENDING_PROP_SHOW_IF.equals(prop);
//  }

  static boolean isEquationProp(Object object){
    return object instanceof String && ((String) object).startsWith("${") && ((String) object).contains("=");
  }

  /* 判断等式是否成立
<!--  showIf="${detailStyle=2}"-->
<!--  showIf="${isMark=false}"-->
<!--  showIf="${name=MyName}"-->
   */
  public static boolean parseBooleanFromPendingProp(String prop,HippyMap dataMap,Object code){
    if(code instanceof String){
      final String value =  ((String) code).trim();
      if(value.length() > 3 && value.startsWith("${")){
        final String[] eq_array = value.substring(2,value.length() - 1).split("==");
        final String valueAfter = eq_array[1];
        final String key = eq_array[0];
        Object dataFromMap = getValueFromCode(dataMap, key);
        if(dataFromMap == null){
          if(LogUtils.isDebug()) {
            Log.i(TAG, "parseBooleanTemplate return false on  key :" + key + ",valueAfter:" + valueAfter + ",dataFromMap:" + null+",dataMap:"+dataMap);
          }
          return false;
        }
        if(LogUtils.isDebug()) {
          Log.i(TAG, "parseBooleanTemplate key :" + key + ",valueAfter:" + valueAfter + ",dataFromMap:" + dataFromMap);
        }
        try {
          if (isNumeric(valueAfter)) {
            //数字
            if(LogUtils.isDebug()) {
              int number = (int) dataFromMap;
              if(LogUtils.isDebug()) {
                Log.i(TAG, "parseBooleanTemplate 1 key :" + key + ",valueAfter:" + valueAfter + ",number:" + number);
              }
            }
            return (int)dataFromMap == Integer.parseInt(valueAfter);
          }
        }catch (Exception ignored){
          if(LogUtils.isDebug()) {
            Log.e(TAG, "parseBooleanTemplate return error on  key :" + key + ",valueAfter:" + valueAfter + ",dataFromMap:" + dataFromMap);
          }
        }
        if("true".equals(valueAfter) || "false".equals(valueAfter)){
          if(LogUtils.isDebug()) {
            Log.i(TAG, "parseBooleanTemplate 444 key :" + key + ",valueAfter:" + valueAfter + ",dataFromMap:" + dataFromMap);
          }
          if(dataFromMap instanceof Boolean){
            if(LogUtils.isDebug()) {
              Log.i(TAG, "parseBooleanTemplate return instance boolean on  key :" + key + ",valueAfter:" + valueAfter + ",dataFromMap:" + dataFromMap);
            }
            return Boolean.parseBoolean(valueAfter) == (Boolean) dataFromMap;
          }
          if(dataFromMap instanceof String) {
            if(LogUtils.isDebug()) {
              Log.i(TAG, "parseBooleanTemplate return instance String on  key :" + key + ",valueAfter:" + valueAfter + ",dataFromMap:" + dataFromMap);
            }
            return valueAfter.equals(dataFromMap);
          }
        }
        if(LogUtils.isDebug()) {
          final Object stringStr = dataFromMap;
          Log.i(TAG, "parseBooleanTemplate 3 key :" + key + ",valueAfter:" + valueAfter+",stringStr:"+stringStr);
        }
        return valueAfter.equals(dataFromMap);
      }
    }
    Log.i(TAG,"parseBooleanTemplate final return false on prop:"+prop);
    return false;
  }

  public static boolean isNumeric(final CharSequence cs) {
    // 判断是否为空，如果为空则返回false
    if (cs == null || cs.length() < 1) {
      return false;
    }
    // 通过 length() 方法计算cs传入进来的字符串的长度，并将字符串长度存放到sz中
    final int sz = cs.length();
    // 通过字符串长度循环
    for (int i = 0; i < sz; i++) {
      // 判断每一个字符是否为数字，如果其中有一个字符不满足，则返回false
      if (!Character.isDigit(cs.charAt(i))) {
        return false;
      }
    }
    // 验证全部通过则返回true
    return true;
  }

  public static boolean isPendingPro(Object object) {
    if(object instanceof String){
      final String value =  ((String) object).trim();
      if(value.contains("=")){
        //等式不是pendingProp
        return false;
      }
      if(value.length() > 3 && value.startsWith("${") ){
        return true;
      }
    }
    return false;
  }

  public static boolean isPendingProForce(Object object) {
    if(object instanceof String){
      final String value =  ((String) object).trim();
      return value.startsWith("${");
    }
    return false;
  }
  public static Object getValueFromCode(final HippyMap data,@Nullable final String code){
    if(code != null && code.contains(".")){
//      if(LogUtils.isDebug()) {
//        Log.d(TAG, "getValueFromCode code: " + code+",data:"+data);
//      }
      //xx.yy.zz
      String[] keys = code.split("\\.");
      HippyMap target = data;
      for(int i = 0; i < keys.length; i ++){
        final String key = keys[i];
        final Object object = target.containsKey(key)?  target.get(key) : null;
        if(i == keys.length -1){
          //最后一个
//          if(LogUtils.isDebug()) {
//            Log.d(TAG, "getValueFromCode find return object:" + object+",key:"+key);
//          }
          return object;
        }else{
          if(object instanceof  HippyMap){
            target = (HippyMap) object;
          }else if(object != null){
            throw new IllegalArgumentException("item Data 暂只支持map key:"+key+",object:"+object+",target:"+target+",code:"+code);
          }
        }
      }
      return null;
    }else {
//      if(LogUtils.isDebug()) {
//        Log.d(TAG, "getValueFromCode directly key:" + code);
//      }
      return data.get(code);
    }
  }

  public static void setValueFromCode(final HippyMap data, final String code,final Object newData){
    if(code.contains(".")){
      if(LogUtils.isDebug()) {
        Log.d("Utils", "setValueFromCode code: " + code);
      }
      //xx.yy.zz
      String[] keys = code.split("\\.");
      HippyMap target = data;
      for(int i = 0; i < keys.length; i ++){
        final String key = keys[i];
        final Object object = target.containsKey(key)?  target.get(key) : null;
        if(i == keys.length -1){
          //最后一个
          if(LogUtils.isDebug()) {
            Log.i("Utils", "setValueFromCode find return target:" + target+",key:"+key+",newData:"+newData);
          }
          target.pushObject(key,newData);
        }else{
          if(object instanceof  HippyMap){
            target = (HippyMap) object;
          }else{
            throw new IllegalArgumentException("item Data 暂只支持map key:"+key+",object:"+object+",target:"+target+",code:"+code);
          }
        }
      }
    }else {
//      Log.d(TAG,"getValueFromCode directly key:"+code);
      data.pushObject(code,newData);
      if(LogUtils.isDebug()) {
        Log.i("Utils", "setValueFromCode setData:" + data+",key:"+code+",newData:"+newData);
      }
    }
  }

  static String parsePlaceholderProp(String prop,HippyMap map){
    Object object = map.get(prop);
    final String valueKey =  TemplateCodeParser.parsePendingProp(object);
    if(!TextUtils.isEmpty(valueKey)) {
      return valueKey;
    }else{
      final int index = Arrays.binarySearch(TemplateCodeParser.PENDING_PROPS_EVENT, prop);
      if (index > -1) {
        return TemplateCodeParser.PENDING_PROPS_EVENT[index];
      }else{
        return null;
      }
    }
  }
}
