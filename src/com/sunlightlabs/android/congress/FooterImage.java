package com.sunlightlabs.android.congress;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FooterImage extends ImageView {
	private int srcOnId;
	private int srcOffId;

	public FooterImage(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FooterImage);
		RuntimeException e = null;
		srcOnId = a.getResourceId(R.styleable.FooterImage_srcOn, 0);
		srcOffId = a.getResourceId(R.styleable.FooterImage_srcOff, 0);

		if (srcOnId == 0)
			e = new IllegalArgumentException(a.getPositionDescription()
					+ ": The scrOn attribute is required and must refer to a valid drawable.");
		if (srcOffId == 0)
			e = new IllegalArgumentException(a.getPositionDescription()
					+ ": The scrOff attribute is required and must refer to a valid drawable.");

		a.recycle();

		if (e != null)
			throw e;
	}


	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// default is off
		setOff();
	}

	public void setOn() {
		this.setImageResource(srcOnId);
	}

	public void setOff() {
		this.setImageResource(srcOffId);
	}

	public int getSrcOnId() {
		return srcOnId;
	}

	public void setSrcOnId(int srcOnId) {
		this.srcOnId = srcOnId;
	}

	public int getSrcOffId() {
		return srcOffId;
	}

	public void setSrcOffId(int srcOffId) {
		this.srcOffId = srcOffId;
	}
}
