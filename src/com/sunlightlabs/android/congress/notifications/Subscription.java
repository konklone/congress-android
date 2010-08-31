package com.sunlightlabs.android.congress.notifications;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Subscription implements Serializable {
	private static final long serialVersionUID = -8734277713086848691L;
	public static final String sep = ":";

	public String id, name, notificationClass, data, lastSeenId;
	
	public Subscription(String id, String name, String notificationClass, String data) {
		this.id = id;
		this.name = name;
		this.notificationClass = notificationClass;
		this.data = data;
	}
	
	public Subscription(String id, String name, String notificationClass, String data, String lastSeenId) {
		this(id, name, notificationClass, data);
		this.lastSeenId = lastSeenId;
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
		return "{id:" + id + ", name:" + name + ", data:" + data + ", lastSeenId:" + lastSeenId + "}";
	}
}