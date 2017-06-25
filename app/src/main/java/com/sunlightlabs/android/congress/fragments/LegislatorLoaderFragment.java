package com.sunlightlabs.android.congress.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.sunlightlabs.android.congress.LegislatorPager;
import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorLoaderFragment extends Fragment implements LoadLegislatorTask.LoadsLegislator {
	private static String FRAGMENT_TAG = "LegislatorLoaderFragment";
	
	public LegislatorPager context;
	public Legislator legislator;
	public CongressException exception;
	
	public static void start(LegislatorPager context) {
		start(context, false);
	}
	
	public static void start(LegislatorPager context, boolean restart) {
		FragmentManager manager = context.getSupportFragmentManager();
		LegislatorLoaderFragment fragment = (LegislatorLoaderFragment) manager.findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			fragment = new LegislatorLoaderFragment();
			fragment.setRetainInstance(true);
			fragment.context = context;
			
			manager.beginTransaction().add(fragment, FRAGMENT_TAG).commit();
		} else if (restart) {
			fragment.context = context;
			fragment.run();
		} else
			fragment.context = context; // still assign context
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		run();
	}
	
	public void run() {
		// If this activity was killed and is being resumed, it's possible for this to get run at the start
		// of the *activity's* onCreate method (in super.onCreate()), before any context has been assigned to this fragment.
		// If this happens, context will be null, and it's okay to simply pass on this, because the run()
		// call will get called again at the end of the activity's onCreate() method, at the call to start(). 
		if (context != null)
			new LoadLegislatorTask(this).execute(context.bioguide_id);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (this.legislator != null)
			context.onLoadLegislator(legislator);
		else if (this.exception != null)
			context.onLoadLegislator(this.exception);
	}
	
	public LegislatorLoaderFragment() {}
	
	// pass through
	public void onLoadLegislator(Legislator legislator) {
		this.legislator = legislator;
		context.onLoadLegislator(legislator);
	}
	
	public void onLoadLegislator(CongressException exception) {
		this.exception = exception;
		context.onLoadLegislator(exception);
	}
}