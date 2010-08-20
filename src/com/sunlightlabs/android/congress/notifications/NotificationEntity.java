package com.sunlightlabs.android.congress.notifications;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationEntity implements Serializable {
	private static final long serialVersionUID = -8734277713086848691L;
	public static final String sep = ":";

	public String id, name, notificationClass, lastSeenId, notificationData;
	public int results;

	public NotificationEntity() {}
	
	public NotificationEntity(String id, String name, String notificationClass,
			String... data) {
		this.id = id;
		this.name = name;
		this.notificationClass = notificationClass;
		if (data != null) this.notificationData = flatten(data);
	}

	private String flatten(String[] data) {
		StringBuilder result = new StringBuilder();
		
		boolean first = true;
		int length = data.length;
		
		for(int i = 0; i < length; i++) {
			if(first)
				first = false;
			else
				result.append(sep);
			result.append(data[i]);
		}

		return result.toString();
	}

	public String notificationName() {
		String name = notificationClass.substring(
				notificationClass.lastIndexOf('.') + 1).replace("Finder", "");
		Pattern p = Pattern.compile("\\p{Lu}");
		Matcher m = p.matcher(name);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, " " + m.group().toLowerCase());
		}
		m.appendTail(sb);
		return sb.toString().trim();
	}

	@Override
	public String toString() {
		return "{id:" + id + ",name:" + name + ",data:" + notificationData
				+ ",lastSeenId:" + lastSeenId + ",results:" + results + "}";
	}
}
