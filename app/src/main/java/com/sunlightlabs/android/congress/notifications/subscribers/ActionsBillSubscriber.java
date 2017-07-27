package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class ActionsBillSubscriber extends Subscriber {

	@Override
	public String decodeId(Object result) {
		return ((Bill.Action) result).full_id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupAPI(context);
		String billId = subscription.data;
		
		try {
			return BillService.find(billId).actions;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest actions for " + subscription, e);
			return null;
		}
	}

	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new action items.";
		else
			return results + " new action item.";
	}
	
	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.billIntent(subscription.id).putExtra("tab", "history");
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return "Activity: " + subscription.name;
	}
}