package com.tencent.mtt.hippy.views.image;

import com.tencent.mtt.supportui.adapters.image.IImageRequestListener;

public interface IImageStateListener<T> extends IImageRequestListener<T> {
    void onImageLayout(T drawableTarget, int left, int top, int right, int bottom);
}
