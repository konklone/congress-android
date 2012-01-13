package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.fragments.RollListFragment;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class RollsRecentSubscriber extends Subscriber {
	
	@Override
	public String decodeId(Object result) {
		return ((Roll) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupRTC(context);
		
		try {
			return RollService.latestVotes(1, RollListFragment.PER_PAGE);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == RollListFragment.PER_PAGE)
			return results + " or more new votes.";
		else if (results > 1)
			return results + " new votes.";
		else
			return results + " new vote.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent()
			.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.MenuVotes");
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return context.getResources().getString(R.string.menu_votes_recent);
	}
	
}