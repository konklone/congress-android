package com.sunlightlabs.android.congress.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.LegislatorService;

public class LoadLegislatorTask extends AsyncTask<String, Void, Legislator> {
	private Context context;
	private Fragment fragment;

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
			return LegislatorService.find(params[0]);
		} catch (CongressException exception) {
			Log.w(Utils.TAG, "Could not load the legislator with id " + params[0] + " from Sunlight");
			return null;
		}
	}

	@Override
	protected void onPostExecute(Legislator legislator) {
		LoadsLegislator loader = (LoadsLegislator) (context != null ? context : fragment);
		loader.onLoadLegislator(legislator);
	}

	public interface LoadsLegislator {
		void onLoadLegislator(Legislator legislator);
	}
}
