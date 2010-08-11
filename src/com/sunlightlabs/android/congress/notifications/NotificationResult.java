package com.sunlightlabs.android.congress.notifications;

public class NotificationResult {
	public String id, type, name, notificationType, notificationData, lastSeenId, status;
	public int results;

	@Override
	public String toString() {
		return "{id:" + id + ",type:" + type + ",name:" + name + ",notificationType:"
				+ notificationType + ", notificationData:" + notificationData + ",lastSeenId:"
				+ lastSeenId + ",status:" + status + ",results:" + results + "}";
	}
}
