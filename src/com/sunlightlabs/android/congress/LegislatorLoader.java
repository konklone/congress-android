package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask;
import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask.LoadsLegislator;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorLoader extends Activity implements LoadsLegislator {
	private LoadLegislatorTask loadLegislatorTask = null;
	private String id;
	private int tab;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_fullscreen);
		
		id = getIntent().getStringExtra("legislator_id");
		tab = getIntent().getIntExtra("tab", 0);
        
        loadLegislatorTask = (LoadLegislatorTask) getLastNonConfigurationInstance();
        if (loadLegislatorTask != null)
        	loadLegislatorTask.onScreenLoad(this);
        else
			loadLegislatorTask = (LoadLegislatorTask) new LoadLegislatorTask(this, tab).execute(id);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadLegislatorTask;
	}
	
	
	public void onLoadLegislator(Legislator legislator, int... tab) {
		if (legislator != null) {
			Intent i = null;
			if (tab != null && tab.length > 0)
				i = Utils.legislatorIntent(this, legislator, tab[0]);
			else
				i = Utils.legislatorIntent(this, legislator);
			startActivity(i);
		}
		else
			Utils.alert(this, R.string.error_connection);
		
		loadLegislatorTask = null;
		finish();
	}
}
