package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class LegislatorVotesFinder extends NotificationFinder {
	private static final int PER_PAGE = 40;

	public LegislatorVotesFinder(Context context) {
		super(context);
	}

	@Override
	public String decodeId(Object result) {
		if(!(result instanceof Roll))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Roll");
		return ((Roll) result).id;
	}

	@Override
	public List<?> callUpdate(String data) {
		String[] split = data.split(NotificationEntity.SEPARATOR);
		if (split == null || split.length < 2) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for legislator. "
					+ "You should provide the 'id' and 'chamber' params in notification data.");
			return null;
		}
		String id = split[0];
		String chamber = split[1];

		Utils.setupDrumbone(context);
		try {
			return RollService.latestVotes(id, chamber, PER_PAGE, 1);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for id " + id + " and chamber " + chamber, e);
			return null;
		}
	}

}
