package com.sunlightlabs.android.congress.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.LegislatorService;

public class LoadLegislatorTask extends AsyncTask<String, Void, Legislator> {
	private LoadsLegislator context;

	public LoadLegislatorTask(LoadsLegislator context) {
		this.context = context;
		Utils.setupSunlight((Context) context);
	}

	public void onScreenLoad(LoadsLegislator context) {
		this.context = context;
	}

	@Override
	protected Legislator doInBackground(String... params) {
		try {
			return LegislatorService.find(params[0]);
		} catch (CongressException exception) {
			Log.w(Utils.TAG, "Could not load the legislator with id " + params[0] + " from Sunlight");
			return null;
		}
	}

	@Override
	protected void onPostExecute(Legislator legislator) {
		if (isCancelled())
			return;
		context.onLoadLegislator(legislator);
	}

	public interface LoadsLegislator {
		void onLoadLegislator(Legislator legislator);
	}
}
