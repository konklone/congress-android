package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.NotificationFinder;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillVotesFinder extends NotificationFinder {

	public BillVotesFinder(Context context) {
		super(context);
	}

	@Override
	public String decodeId(Object result) {
		if(!(result instanceof Bill.Vote))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Bill.Vote");
		return String.valueOf(((Bill.Vote) result).voted_at.getTime());
	}

	@Override
	public List<?> callUpdate(String data) {
		Utils.setupDrumbone(context);
		try {
			return BillService.find(data, "votes").votes;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + data, e);
			return null;
		}
	}

}
