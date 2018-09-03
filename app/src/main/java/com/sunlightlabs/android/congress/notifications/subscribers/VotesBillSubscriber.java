package com.sunlightlabs.android.congress.notifications.subscribers;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

import java.util.List;

public class VotesBillSubscriber extends Subscriber {

	@Override
	public String decodeId(Object result) {
		return ((Bill.Vote) result).full_id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupAPI(context);
		String billId = subscription.data;

		try {
			return BillService.find(billId).votes;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + subscription, e);
			return null;
		}
	}

	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new votes have occurred.";
		else if (results == 1)
			return "A vote has occurred.";
		else
			return results + " new votes have occurred.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.billIntent(subscription.id).putExtra("tab", "votes");
	}

	@Override
	public String subscriptionName(Subscription subscription) {
		return "Votes: " + subscription.name;
	}
}