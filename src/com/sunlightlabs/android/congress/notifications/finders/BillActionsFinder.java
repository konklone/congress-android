package com.sunlightlabs.android.congress.notifications.finders;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillActionsFinder extends NotificationFinder {

	@Override
	public String decodeId(Object result) {
		if(!(result instanceof Bill.Action))
			throw new IllegalArgumentException("The result must be of type com.sunlightlabs.congress.models.Bill.Action");
		return String.valueOf(((Bill.Action) result).acted_at.getTime());
	}

	@Override
	public List<?> fetchUpdates(NotificationEntity entity) {
		Utils.setupDrumbone(context);
		try {
			return BillService.find(entity.notificationData, "actions").actions;
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest actions for " + entity, e);
			return null;
		}
	}

	@Override
	public Intent notificationIntent(NotificationEntity entity) {
		return Utils.billLoadIntent(entity.id, new Intent(Intent.ACTION_MAIN).setClassName(
				"com.sunlightlabs.android.congress",
				"com.sunlightlabs.android.congress.BillHistory")
				.putExtra("entity", entity));
	}
}
