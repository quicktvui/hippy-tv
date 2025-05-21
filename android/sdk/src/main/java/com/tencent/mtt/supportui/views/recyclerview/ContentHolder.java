package com.tencent.mtt.supportui.views.recyclerview;

import android.view.View;

/**
 * Created by leonardgong on 2018/4/9 0008.
 */

public class ContentHolder
{
	public Object	mParentViewHolder;
	public View		mContentView;
	public int		mContentLeftPadding	= 0;
	public int		mItemPaddingLeft	= 0;
	public int		mItemPaddingRight	= 0;
	//zhaopeng 2021-1-6 这里将mFocusable由true改成false,因为RecyclerViewItem不需要焦点
//	public boolean	mFocusable			= true;
	public boolean	mFocusable			= false;
	public boolean	mForceBind			= false;

	public void inTraversals(int traversalPurpose, int position, RecyclerViewBase recyclerView)
	{

	}

	public void setEnable(boolean enabled)
	{
		if (mContentView != null && mContentView.getParent() != null)
		{
			((View) mContentView.getParent()).setEnabled(enabled);
		}
	}

}
