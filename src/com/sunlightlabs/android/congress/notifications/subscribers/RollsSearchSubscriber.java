package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class RollsSearchSubscriber extends Subscriber {
	
	@Override
	public String decodeId(Object result) {
		return ((Roll) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupAPI(context);
		String query = subscription.data;
		
		try {
			Map<String,String> params = new HashMap<String,String>();
			params.put("order", "voted_at");
			params.put("how", "roll");
			return RollService.search(query, params, 1, RollListFragment.PER_PAGE);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == RollListFragment.PER_PAGE)
			return results + " or more new votes matching \"" + subscription.data + "\".";
		else if (results > 1)
			return results + " new votes matching \"" + subscription.data + "\".";
		else
			return results + " new vote matching \"" + subscription.data + "\".";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent().setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.VoteSearch")
				.putExtra("query", subscription.data)
				.putExtra("tab", "votes_recent");
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return "Votes matching \"" + subscription.data + "\"";
	}
}