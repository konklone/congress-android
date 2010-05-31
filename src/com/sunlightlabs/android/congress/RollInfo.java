package com.sunlightlabs.android.congress;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Roll;

public class RollInfo extends ListActivity {
	private String id;
	private Roll roll;
	
	private LoadRollTask loadRollTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		
		setupControls();
		
		RollInfoHolder holder = (RollInfoHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.roll = holder.roll;
			this.loadRollTask = holder.loadRollTask;
		}
		
		loadRoll();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new RollInfoHolder(loadRollTask, roll);
	}
	
	public void setupControls() {
		Roll tempRoll = Roll.splitRollId(id);
		String title = Utils.capitalize(tempRoll.chamber) + " Roll No. " + tempRoll.number + ", " + tempRoll.year;
		((TextView) findViewById(R.id.title_text)).setText(title);
	}
	
	public void onLoadRoll(Roll roll) {
		this.loadRollTask = null;
		this.roll = roll;
		
		Utils.alert(this, "Loaded roll:\n" + roll.question);
		displayRoll();
	}
	
	public void onLoadRoll(CongressException exception) {
		this.loadRollTask = null;
		Utils.alert(this, R.string.error_connection);
		finish();
	}
	
	public void displayRoll() {
		Utils.alert(this, "displayRoll()");
	}
	
	public void loadRoll() {
		if (loadRollTask != null)
			loadRollTask.onScreenLoad(this);
		else {
			if (roll != null)
				displayRoll();
			else
				loadRollTask = (LoadRollTask) new LoadRollTask(this, id).execute("basic,bill");
		}
	}
	
	
	private class LoadRollTask extends AsyncTask<String,Void,Roll> {
		private RollInfo context;
		private CongressException exception;
		private String rollId;
		
		public LoadRollTask(RollInfo context, String rollId) {
			this.context = context;
			this.rollId = rollId;
			Utils.setupDrumbone(context);
		}
		
		public void onScreenLoad(RollInfo context) {
			this.context = context;
		}
		
		@Override
		public Roll doInBackground(String... sections) {
			try {
				return Roll.find(rollId, sections[0]);
			} catch (CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(Roll roll) {
			if (exception != null && roll == null)
				context.onLoadRoll(exception);
			else
				context.onLoadRoll(roll);
		}
	}
	
	static class RollInfoHolder {
		private LoadRollTask loadRollTask;
		private Roll roll;
		
		public RollInfoHolder(LoadRollTask loadRollTask, Roll roll) {
			this.loadRollTask = loadRollTask;
			this.roll = roll;
		}
	}
}