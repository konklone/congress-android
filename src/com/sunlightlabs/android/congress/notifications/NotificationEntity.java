package com.sunlightlabs.android.congress.notifications;

import java.io.Serializable;

public class NotificationEntity implements Serializable {
	private static final long serialVersionUID = -8734277713086848691L;
	public static final String SEPARATOR = ":";

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
				result.append(SEPARATOR);
			result.append(data[i]);
		}

		return result.toString();
	}

	@Override
	public String toString() {
		return "{id:" + id + ",name:" + name + ",data:" + notificationData
				+ ",lastSeenId:" + lastSeenId + ",results:" + results + "}";
	}
}
