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

public class BillsRecentSubscriber extends Subscriber {
	private static final int PER_PAGE = 40;

	@Override
	public String decodeId(Object result) {
		return ((Bill) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupRTC(context);
		
		try {
			return BillService.recentlyIntroduced(PER_PAGE, 1);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest bills for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new bills.";
		else
			return results + " new bill.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent(Intent.ACTION_MAIN)
			.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.BillList")
			.putExtra("type", BillList.BILLS_RECENT);
	}
}