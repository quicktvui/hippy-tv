package com.quicktvui.hippyext.views.fastlist;

public interface PostHandlerView {
  void postTask(int category,int type,Runnable  r,int delay);
  void clearTask(int cate,int type);
}
