package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.fragments.BillListFragment;
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
		Utils.setupAPI(context);
		String query = subscription.data;
		
		try {
			Map<String,String> params = new HashMap<String,String>();
			params.put("order", "introduced_on");
			return BillService.search(query, params, 1, BillListFragment.PER_PAGE);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest bill search results for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == BillListFragment.PER_PAGE) {
			return results + " or more new bills for search \"" + subscription.data + "\".";
		} else if (results > 1) {
			return results + " new bills for search \"" + subscription.data + "\".";
		} else {
			return results + " new bill for search \"" + subscription.data + "\".";
		}
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent().setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.BillSearch")
			.putExtra("query", subscription.data)
			.putExtra("tab", "bills_recent");
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return "Bills matching \"" + subscription.data + "\""; 
	}
}