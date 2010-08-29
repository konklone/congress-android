package com.sunlightlabs.android.congress.notifications;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.sunlightlabs.android.congress.R;

public class FooterImage extends ImageView {
	public int srcOn, srcOff;

	public FooterImage(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FooterImage);
		srcOn = array.getResourceId(R.styleable.FooterImage_srcOn, 0);
		srcOff = array.getResourceId(R.styleable.FooterImage_srcOff, 0);
		array.recycle();
	}


	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		setOff();
	}

	public void setOn() {
		this.setImageResource(srcOn);
	}

	public void setOff() {
		this.setImageResource(srcOff);
	}
}