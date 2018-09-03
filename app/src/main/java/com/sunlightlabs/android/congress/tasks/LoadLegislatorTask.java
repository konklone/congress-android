package com.sunlightlabs.android.congress.tasks;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.LegislatorService;

public class LoadLegislatorTask extends AsyncTask<String, Void, Legislator> {
	private Context context;
	private Fragment fragment;

	private CongressException exception;

	public LoadLegislatorTask(Context context) {
		this.context = context;
		Utils.setupAPI(context);
	}

	public void onScreenLoad(Context context) {
		this.context = context;
	}

	public LoadLegislatorTask(Fragment fragment) {
		this.fragment = fragment;
		FragmentUtils.setupAPI(fragment);
	}

	@Override
	protected Legislator doInBackground(String... params) {
		try {
			Legislator legislator = LegislatorService.find(params[0]);
			if (legislator == null)
				this.exception = new CongressException("Can't find/load legislator with ID: " + params[0]);

			return legislator;
		} catch (CongressException exception) {
			Log.w(Utils.TAG, "Error loading the legislator with ID: " + params[0]);
			this.exception = exception;
			return null;
		}
	}

	@Override
	protected void onPostExecute(Legislator legislator) {
		LoadsLegislator loader = (LoadsLegislator) (context != null ? context : fragment);

		if (legislator == null) // guaranteed to be an exception stored
			loader.onLoadLegislator(this.exception);
		else
			loader.onLoadLegislator(legislator);
	}

	public interface LoadsLegislator {
		void onLoadLegislator(Legislator legislator);
		void onLoadLegislator(CongressException exception);
	}
}