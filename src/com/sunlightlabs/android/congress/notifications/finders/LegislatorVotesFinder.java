package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.RollList;
import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class LegislatorVotesFinder extends NotificationFinder {
	private static final int PER_PAGE = 40;

	@Override
	public String decodeId(Object result) {
		if(!(result instanceof Roll))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Roll");
		return ((Roll) result).id;
	}

	@Override
	public List<?> callUpdate(NotificationEntity entity) {
		Utils.setupDrumbone(context);
		try {
			return RollService.latestVotes(entity.id, entity.notificationData, PER_PAGE, 1);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + entity, e);
			return null;
		}
	}

	@Override
	public Intent notificationIntent(NotificationEntity entity) {
		return Utils.legislatorIntent(entity.id, new Intent(Intent.ACTION_MAIN)
				.setClassName("com.sunlightlabs.android.congress",
				"com.sunlightlabs.android.congress.RollList")
				.putExtra("type", RollList.ROLLS_VOTER));
	}
}
