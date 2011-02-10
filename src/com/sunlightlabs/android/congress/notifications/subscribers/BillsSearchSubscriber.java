package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.BillList;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillsSearchSubscriber extends Subscriber {

	@Override
	public String decodeId(Object result) {
		return ((Bill) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupRTC(context);
		
		try {
			return BillService.search(subscription.data, 1, BillList.PER_PAGE);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest bill search results for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new bills for search \"" + subscription.data + "\".";
		else
			return results + " new bill for search \"" + subscription.data + "\".";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent().setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.BillList")
			.putExtra("type", BillList.BILLS_SEARCH)
			.putExtra("query", subscription.data);
	}
}