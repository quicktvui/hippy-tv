package com.tencent.mtt.hippy.views.list;

import android.graphics.Rect;

import android.support.annotation.NonNull;

import com.tencent.mtt.supportui.views.recyclerview.RecyclerViewBase;

public class ItemDecorations {

    public static class SimpleBetweenItem extends RecyclerViewBase.ItemDecoration {

        final int itemSpace;
        final boolean horizontal;

        public SimpleBetweenItem(int itemSpace,boolean horizontal) {
            this.itemSpace = itemSpace;
            this.horizontal = horizontal;
        }

        public SimpleBetweenItem(int itemSpace) {
            this.itemSpace = itemSpace;
            this.horizontal = true;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, int itemPosition, @NonNull RecyclerViewBase parent) {
            super.getItemOffsets(outRect, itemPosition,parent);
            if(horizontal) {
                outRect.right = itemSpace;
            }else{
                outRect.bottom = itemSpace;
            }
        }
    }


    public static class ListEndBlank extends RecyclerViewBase.ItemDecoration{
        final int blank;
        final int orientation;


        public ListEndBlank(int blank, int orientation) {
            this.blank = blank;
            this.orientation = orientation;
        }

        public ListEndBlank(int orientation) {
            this(100,orientation);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,int itemPosition, @NonNull RecyclerViewBase parent) {
            super.getItemOffsets(outRect, itemPosition,parent);
          if(itemPosition > 0) {
                if (orientation == RecyclerViewBase.HORIZONTAL) {
                    if(itemPosition >= parent.getAdapter().getItemCount()/ - 1){
                        outRect.right = blank;
                    }
                } else {
                    if(itemPosition >= parent.getAdapter().getItemCount() - 1){
                        outRect.bottom = blank;
                    }
                }
            }
        }
    }


}
