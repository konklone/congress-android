package com.sunlightlabs.android.congress;

import android.content.Context;
import android.widget.Toast;

public class Utils {
	public static void alert(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
	
	public static void alert(Context context, int msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
}
