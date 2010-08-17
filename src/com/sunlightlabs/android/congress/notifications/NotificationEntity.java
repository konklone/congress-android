package com.sunlightlabs.android.congress.notifications;

import java.io.Serializable;

public class NotificationEntity implements Serializable {
	private static final long serialVersionUID = -8734277713086848691L;
	public static final String SEPARATOR = "[|]+";

	// entity types
	public static final String LEGISLATOR = "legislator";
	public static final String BILL = "bill";
	public static final String LAW = "law";

	// notification types
	public static final String TWEETS = "tweets";
	public static final String VIDEOS = "videos";
	public static final String NEWS = "news";
	public static final String VOTES = "votes";
	public static final String HISTORY = "history";

	public String id, name, type, lastSeenId, status, notification_data, notification_type;
	public int results;

	public NotificationEntity() {}
	
	public NotificationEntity(String id, String type, String name, String nType, String... nData) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.notification_type = nType;
		this.notification_data = flatten(nData);
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
		return "{id:" + id + ",type:" + type + ",name:" + name + ",nType:" + notification_type
				+ ", nData:" + notification_data + ",lastSeenId:" + lastSeenId + ",status:" + status
				+ ",results:" + results + "}";
	}
}
