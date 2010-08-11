package com.sunlightlabs.android.congress.notifications;

public class NotificationResult {
	public String id;
	public String type;
	public String name;
	public String notificationType;
	public String notificationData;
	public String lastSeenId;
	public String status;
	public int results;

	@Override
	public String toString() {
		return "{id:" + id + ",type:" + type + ",name:" + name + ",notificationType:"
				+ notificationType + ", notificationData:" + notificationData + ",lastSeenId:"
				+ lastSeenId + ",status:" + status + ",results:" + results + "}";
	}
}
