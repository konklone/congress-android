package com.sunlightlabs.android.congress.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.District;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.DistrictService;

public class LoadDistrictTask extends AsyncTask<Legislator, Void, District> {
	private LoadsDistrict fragment;
	
	private CongressException exception;

	public LoadDistrictTask(LoadsDistrict fragment) {
		this.fragment = fragment;
		// doesn't need to setup a Sunlight API key
	}

	@Override
	protected District doInBackground(Legislator... params) {
		Legislator legislator = params[0];
		try {
			District district = DistrictService.find(legislator);
			if (district == null)
				this.exception = new CongressException("Can't load district.");
			
			return district;
		} catch (CongressException exception) {
			Log.w(Utils.TAG, "Could not load the district for legislator " + legislator.bioguide_id);
			this.exception = exception;
			return null;
		}
	}

	@Override
	protected void onPostExecute(District district) {
		if (district == null) // guaranteed to be an exception stored
			fragment.onLoadDistrict(this.exception);
		else
			fragment.onLoadDistrict(district);
	}

	public interface LoadsDistrict {
		void onLoadDistrict(District district);
		void onLoadDistrict(CongressException exception);
	}
}
