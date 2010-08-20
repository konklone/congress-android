package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.NotificationEntity;
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
	public List<?> callUpdate(NotificationEntity entity) {
		Utils.setupDrumbone(context);
		try {
			return BillService.find(entity.notificationData, "votes").votes;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + entity, e);
			return null;
		}
	}

	@Override
	public Intent notificationIntent(NotificationEntity entity) {
		return Utils.billIntent(entity.id, new Intent(Intent.ACTION_MAIN)
				.setClassName("com.sunlightlabs.android.congress",
				"com.sunlightlabs.android.congress.BillVotes")
				.putExtra("entity", entity));
	}
}
