package com.sunlightlabs.android.congress.notifications;

import com.sunlightlabs.congress.models.CongressException;

import java.io.Serializable;

public class Subscription implements Serializable {
	private static final long serialVersionUID = -8734277713086848691L;

	public String id, name, notificationClass, data;

	public Subscription(String id, String name, String notificationClass, String data) {
		this.id = id;
		this.name = name;
		this.notificationClass = notificationClass;
		this.data = data;
	}

	public Subscriber getSubscriber() throws CongressException {
		try {
			return (Subscriber) Class.forName("com.sunlightlabs.android.congress.notifications.subscribers." + notificationClass).newInstance();
		} catch (Exception e) {
			throw new CongressException(e, e.getMessage());
		}
	}

	@Override
	public String toString() {
		return "{id: " + id + ", name: " + name + ", notificationClass: " + notificationClass + ", data: " + data + "}";
	}
}