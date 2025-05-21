package com.quicktvui.hippyext.views.fastlist;


import android.view.View;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;


public class EventDeliverer {
  final HippyEngineContext context;

  OnEventListener onEventListener;

  public EventDeliverer(HippyEngineContext context) {
    this.context = context;
  }

  public void sendEvent(String eventName, int id, HippyMap params){
    sendEvent(eventName,id,context,params,onEventListener);
  }

  public void sendEvent(String eventName, View view, HippyMap params){
    sendEvent(eventName,view.getId(),context,params,onEventListener);
  }

  public void sendEvent(HippyViewEvent event, int id, HippyMap params){
    sendEvent(event,id,context,params,onEventListener);
  }

  public void sendEvent(HippyViewEvent event, View view, HippyMap params){
    sendEvent(event,view.getId(),context,params,onEventListener);
  }
  public static void sendEvent(String eventName,int id,HippyEngineContext context,HippyMap params,OnEventListener onEventListener){
    sendEvent(new HippyViewEvent(eventName),id,context,params,onEventListener);
  }

  public static void sendEvent(HippyViewEvent event,int id,HippyEngineContext context,HippyMap params,OnEventListener onEventListener){
    if(context != null && id > -1) {
      if(onEventListener != null){
        onEventListener.onBeforeSend(event,id,context,params);
      }
      event.send(id,context,params);
    }
  }

  public void setOnEventListener(OnEventListener onEventListener) {
    this.onEventListener = onEventListener;
  }

  public interface OnEventListener {
    void onBeforeSend(HippyViewEvent event, int id,HippyEngineContext context, HippyMap params);
  }
}
