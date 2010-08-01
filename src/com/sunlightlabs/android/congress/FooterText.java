package com.sunlightlabs.android.congress;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

public class FooterText extends TextView {
	private String textOn;
	private String textOff;

	public FooterText(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FooterText);
		textOn = a.getString(R.styleable.FooterText_textOn);
		textOff = a.getString(R.styleable.FooterText_textOff);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// default is off
		setOff();
	}

	public void setOn() {
		setText(textOn);
	}

	public void setOff() {
		setText(textOff);
	}

	public String getTextOn() {
		return textOn;
	}

	public void setTextOn(String textOn) {
		this.textOn = textOn;
	}

	public String getTextOff() {
		return textOff;
	}

	public void setTextOff(String textOff) {
		this.textOff = textOff;
	}
}
