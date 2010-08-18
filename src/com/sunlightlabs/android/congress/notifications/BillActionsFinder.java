package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillActionsFinder extends NotificationFinder {

	public BillActionsFinder(Context context) {
		super(context);
	}

	@Override
	protected String decodeId(Object result) {
		if(!(result instanceof Bill.Action))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Bill.Action");
		return String.valueOf(((Bill.Action) result).acted_at.getTime());
	}

	@Override
	public List<?> callUpdate(String data) {
		Utils.setupDrumbone(context);
		try {
			return BillService.find(data, "actions").actions;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest actions for " + data, e);
			return null;
		}
	}

}
