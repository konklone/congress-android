package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.congress.java.CongressException;

public class Utils {
	public static void alert(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
	
	public static void alert(Context context, int msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
	
	public static void alert(Context context, CongressException exception) {
		String message = exception == null ? "Unhandled error." : exception.getMessage();
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	public static String stateCodeToName(Context context, String code) {
		String[] codes = context.getResources().getStringArray(R.array.state_codes);
		String[] names = context.getResources().getStringArray(R.array.state_names);
		
		for (int i=0; i<codes.length; i++) {
			if (codes[i].equals(code))
				return names[i];
		}
		return null;
	}
	
	public static String stateNameToCode(Context context, String name) {
		String[] codes = context.getResources().getStringArray(R.array.state_codes);
		String[] names = context.getResources().getStringArray(R.array.state_names);
		
		for (int i=0; i<names.length; i++) {
			if (names[i].equals(name))
				return codes[i];
		}
		return null;
	}
	
	public static LinearLayout makeIconListItem(LayoutInflater inflater, Object tag, int icon, String text) {
		LinearLayout item = (LinearLayout) inflater.inflate(R.layout.icon_list_item_1, null);
        ((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) item.findViewById(R.id.text)).setText(text);
        item.setTag(tag);
        return item;
	}
	
	public static String truncate(String text, int length) {
		if (text.length() > length)
			return text.substring(0, length - 3) + "...";
		else
			return text;
	}
}