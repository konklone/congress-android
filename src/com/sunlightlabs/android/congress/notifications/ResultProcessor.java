package com.sunlightlabs.android.congress.notifications;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;

public abstract class ResultProcessor {
	protected Context context;
	protected NotificationEntity entity;

	public ResultProcessor(Context context, NotificationEntity entity) {
		this.entity = entity;
		this.context = context;
	}

	public abstract void callUpdate();
	
	protected abstract String decodeId(Object result);

	/**
	 * This method assumes that the results are ordered and the most recent one
	 * is the last in the list. If it's not the case, then it must be first
	 * sorted to match this criterion.
	 */
	protected void processResults(List<?> results) {
		final String id = entity.id;
		final String ntype = entity.notification_type;

		if (results == null || results.isEmpty()) {
			Log.d(Utils.TAG, getClass().getSimpleName() + ": No " + ntype + " to process for entity " + id);
			return;
		}

		final int size = results.size();
		Log.d(Utils.TAG,  getClass().getSimpleName() + ": Loaded " + size + " " + ntype + " for entity with id "
				+ id);
		
		
		String lastId = decodeId(results.get(size - 1));
		// search for the last seen id in the list of results, and calculate how
		// many new results are after that id
		if(entity.lastSeenId != null) {
			int foundPosition = -1;
			for (Object result : results) {
				if (entity.lastSeenId.equals(decodeId(result))) {
					foundPosition = results.indexOf(result);
					break;
				}
			}

			if (foundPosition > -1) {
				entity.results = size - foundPosition - 1;
				Log.d(Utils.TAG,  getClass().getSimpleName() + ": There are " + entity.results + " *NEW* " + ntype
						+ " for entity " + id);
			}
		}
		else // entity.lastSeenId is null, meaning it's the first time we check
			Log.d(Utils.TAG, getClass().getSimpleName() + ": First time check for entity " + id
					+ ", " + "set the last seen " + ntype + " id to " + lastId);
	
		// set the last seen id to the id of the most recent result
		entity.lastSeenId = lastId;
	}

	public NotificationEntity getEntity() {
		return entity;
	}
}
