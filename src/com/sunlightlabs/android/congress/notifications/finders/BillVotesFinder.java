package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.notifications.NotificationFinder;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillVotesFinder extends NotificationFinder {

	@Override
	public String decodeId(Object result) {
		if(!(result instanceof Bill.Vote))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Bill.Vote");
		return String.valueOf(((Bill.Vote) result).voted_at.getTime());
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupDrumbone(context);
		try {
			String billId = subscription.data;
			return BillService.find(billId, "votes").votes;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + subscription, e);
			return null;
		}
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.billLoadIntent(subscription.id, new Intent(Intent.ACTION_MAIN)
				.setClassName("com.sunlightlabs.android.congress",
				"com.sunlightlabs.android.congress.BillVotes")
				.putExtra("subscription", subscription));
	}
}
