package com.sunlightlabs.android.congress.notifications.finders;

import android.content.Intent;

import com.sunlightlabs.android.congress.BillTabs;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;

public class BillNewsFinder extends LegislatorNewsFinder {

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.billLoadIntent(subscription.id, Utils.billTabsIntent()
				.putExtra("tab", BillTabs.Tabs.news));
	}
}
