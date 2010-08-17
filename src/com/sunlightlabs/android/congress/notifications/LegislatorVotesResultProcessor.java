package com.sunlightlabs.android.congress.notifications;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class LegislatorVotesResultProcessor extends ResultProcessor {

	public LegislatorVotesResultProcessor(Context context, NotificationEntity entity) {
		super(context, entity);
	}

	@Override
	public String decodeId(Object result) {
		if(!(result instanceof Roll))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Roll");
		return ((Roll) result).id;
	}

	@Override
	public void callUpdate() {
		String[] data = entity.inflateData();
		String id = data[0];
		String chamber = data[1];
		int page = Integer.parseInt(data[2]);
		int per_page = Integer.parseInt(data[3]);

		Utils.setupDrumbone(context);
		try {
			processResults(RollService.latestVotes(id, chamber, per_page, page));
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for legislator " + id + " using "
					+ entity.notification_data, e);
		}
	}

}
