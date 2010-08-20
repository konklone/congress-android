package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;

import com.sunlightlabs.android.congress.LegislatorTabs;
import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.utils.Utils;

public class LegislatorNewsFinder extends NotificationFinder {

	@Override
	public List<?> callUpdate(NotificationEntity entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String decodeId(Object result) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Intent notificationIntent(NotificationEntity entity) {
		return Utils.legislatorIntent(entity.id).putExtra("tab", LegislatorTabs.Tabs.news.ordinal());
	}

}
