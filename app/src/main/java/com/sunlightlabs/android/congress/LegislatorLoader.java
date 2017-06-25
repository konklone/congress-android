package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask;
import com.sunlightlabs.android.congress.tasks.LoadLegislatorTask.LoadsLegislator;
import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
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
		
		// if coming from a shortcut intent, there appears to be a bug with packaging sub-intents
		// and the intent will be null
		if (intent == null)
			intent = Utils.legislatorIntent(id);

        loadLegislatorTask = (LoadLegislatorTask) getLastNonConfigurationInstance();
        if (loadLegislatorTask != null)
        	loadLegislatorTask.onScreenLoad(this);
        else
			loadLegislatorTask = (LoadLegislatorTask) new LoadLegislatorTask(this).execute(id);
        
        setupControls();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadLegislatorTask;
	}
	
	public void setupControls() {
		Utils.setLoading(this, R.string.legislator_loading);
		ActionBarUtils.setTitle(this, R.string.app_name, new Intent(this, MenuLegislators.class));
	}
	
	public void onLoadLegislator(Legislator legislator) {
		if (legislator != null) {
			intent.putExtra("legislator", legislator);
			// pass entry info along, this loader class is an implementation detail
			startActivity(Analytics.passEntry(this, intent));
		} else
			Utils.alert(this, R.string.error_connection);
		
		loadLegislatorTask = null;
		finish();
	}
	
	public void onLoadLegislator(CongressException exception) {
		onLoadLegislator((Legislator) null);
	}
}
