package com.sunlightlabs.android.congress.notifications;

public class NotificationEntity {
	public String id, type, name, notificationData, lastSeenId, status;
	public NotificationType notificationType;
	public int results;

	@Override
	public String toString() {
		return "{id:" + id + ",type:" + type + ",name:" + name + ",notificationType:"
				+ notificationType.name() + ", notificationData:" + notificationData + ",lastSeenId:"
				+ lastSeenId + ",status:" + status + ",results:" + results + "}";
	}
}
