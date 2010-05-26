package com.sunlightlabs.android.congress.utils;

import android.content.Context;

import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;

public interface LoadsBill {
	public void onLoadBill(Bill bill);
	public void onLoadBill(CongressException exception);
	public Context getContext();
}