package com.sunlightlabs.android.congress.notifications.subscribers;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;
import com.sunlightlabs.congress.services.ProPublica;

import java.util.List;

public class BillsLegislatorSubscriber extends Subscriber {

	@Override
	public String decodeId(Object result) {
		return ((Bill) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupAPI(context);
		try {
			return BillService.recentlySponsored(subscription.id, 1);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest sponsored bills for " + subscription, e);
			return null;
		}
	}

	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == ProPublica.PER_PAGE)
			return results + " or more new bills sponsored.";
		else if (results > 1)
			return results + " new bills sponsored.";
		else
			return results + " new bill sponsored.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.legislatorIntent(subscription.id).putExtra("tab", "bills");
	}

	@Override
	public String subscriptionName(Subscription subscription) {
		return "Sponsored Bills: " + subscription.name;
	}
}