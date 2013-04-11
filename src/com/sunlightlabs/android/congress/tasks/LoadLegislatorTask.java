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
				this.exception = new CongressException("Can't load legislator with this ID from Sunlight.");
			
			return legislator;
		} catch (CongressException exception) {
			Log.w(Utils.TAG, "Could not load the legislator with id " + params[0] + " from Sunlight");
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
