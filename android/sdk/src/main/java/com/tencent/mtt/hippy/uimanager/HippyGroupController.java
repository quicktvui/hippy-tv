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

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.dom.node.NodeProps;


/**
 * Created by leonardgong on 2017/11/29 0029.
 */

public abstract class HippyGroupController<T extends ViewGroup & HippyViewBase> extends HippyViewController<T>
{
	/** touch/click intercept **/
	@HippyControllerProps(name = NodeProps.ON_INTERCEPT_TOUCH_EVENT, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setInterceptTouch(T viewGroup, boolean flag)
	{
		if (!handleGestureBySelf())
		{
			setGestureType(viewGroup, NodeProps.ON_INTERCEPT_TOUCH_EVENT, flag);
		}
	}





	/** touch/click intercept **/
	@HippyControllerProps(name = NodeProps.ON_INTERCEPT_PULL_UP_EVENT, defaultType = HippyControllerProps.BOOLEAN, defaultBoolean = false)
	public void setInterceptPullUp(T viewGroup, boolean flag)
	{
		if (!handleGestureBySelf())
		{
			setGestureType(viewGroup, NodeProps.ON_INTERCEPT_PULL_UP_EVENT, flag);
		}
	}

  @Override
  public void dispatchFunction(T view, String functionName, HippyArray dataArray)
  {
    super.dispatchFunction(view, functionName, dataArray);
    switch (functionName)
    {
      case "requestChildFocus":
        int p = dataArray.getInt(0);
        if(view instanceof ViewGroup){
          if(p > -1 && view.getChildCount() > p){
            final View v = view.getChildAt(p);
            if(v != null){
              v.requestFocus();
              Log.d("hippy","requestChildFocus index:"+p);
            }else{
              Log.e("hippy","requestChildFocus error invalid index:"+p);
            }
          }else{
            Log.e("hippy","requestChildFocus error invalid index:"+p);
          }
        }
        break;

    }
  }
}
