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

public class VotesBillSubscriber extends Subscriber {

	@Override
	public String decodeId(Object result) {
		return String.valueOf(((Bill.Vote) result).voted_at.getTime());
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupAPI(context);
		String billId = subscription.data;
		
		try {
			return BillService.find(billId, new String[] {"passage_votes"}).votes;
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
		return Utils.billLoadIntent(subscription.id, Utils.billPagerIntent()
				.putExtra("tab", "votes"));
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return "Votes: " + subscription.name;
	}
}