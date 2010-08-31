package com.sunlightlabs.android.congress.notifications;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;

public class FooterText extends TextView {
	public String textOn, textOff;

	public FooterText(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FooterText);
		textOn = array.getString(R.styleable.FooterText_textOn);
		textOff = array.getString(R.styleable.FooterText_textOff);
		array.recycle();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		setOff();
	}

	public void setOn() {
		setText(textOn);
	}

	public void setOff() {
		setText(textOff);
	}
}