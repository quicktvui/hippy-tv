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
package com.tencent.mtt.hippy.uimanager;

import android.view.View;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.modules.HippyModuleManager;
import com.tencent.mtt.hippy.modules.javascriptmodules.EventDispatcher;

public class HippyViewEvent {

  private final String mEventName;

  public HippyViewEvent(String eventName) {
    this.mEventName = eventName;
  }

  public void send(View view, Object param) {
    if (view != null && view.getContext() instanceof HippyInstanceContext) {
      HippyEngineContext context = ((HippyInstanceContext) view.getContext()).getEngineContext();
      send(view.getId(), context, param);
    }
  }

  public void send(int id, HippyEngineContext context, Object param) {
    if (context == null) {
      return;
    }

    HippyModuleManager hmm = context.getModuleManager();
    if (hmm != null) {
      hmm.getJavaScriptModule(EventDispatcher.class).receiveUIComponentEvent(id, mEventName, param);
//      Log.w("hippy","receiveUIComponentEvent id:"+id+",mEventName:"+mEventName+",param:"+param);
    }
  }
}
