package com.sunlightlabs.android.congress.notifications;

import java.io.Serializable;

public class Subscription implements Serializable {
	private static final long serialVersionUID = -8734277713086848691L;

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

	@Override
	public String toString() {
		return "{id:" + id + ", name:" + name + ", data:" + data + ", lastSeenId:" + lastSeenId + "}";
	}
}