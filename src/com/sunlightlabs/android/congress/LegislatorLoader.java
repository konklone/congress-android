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
	private Intent intent;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_fullscreen);
		
		Intent i = getIntent();
		id = i.getStringExtra("id");
		intent = (Intent) i.getParcelableExtra("intent");

        loadLegislatorTask = (LoadLegislatorTask) getLastNonConfigurationInstance();
        if (loadLegislatorTask != null)
        	loadLegislatorTask.onScreenLoad(this);
        else
			loadLegislatorTask = (LoadLegislatorTask) new LoadLegislatorTask(
					this).execute(id);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadLegislatorTask;
	}
	
	
	public void onLoadLegislator(Legislator legislator) {
		if (legislator != null) {
			intent.putExtra("legislator", legislator);
			startActivity(intent);
		}
		else
			Utils.alert(this, R.string.error_connection);
		
		loadLegislatorTask = null;
		finish();
	}
}
