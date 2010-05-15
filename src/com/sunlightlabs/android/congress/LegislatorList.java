package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.sunlightlabs.android.congress.utils.LegislatorAdapter;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.java.Committee;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;

public class LegislatorList extends ListActivity {
	private final static int SEARCH_ZIP = 0;
	private final static int SEARCH_LOCATION = 1;
	private final static int SEARCH_STATE = 2;
	private final static int SEARCH_LASTNAME = 3;
	private final static int SEARCH_COMMITTEE = 4;

	private ArrayList<Legislator> legislators = null;
	private LoadLegislatorsTask loadLegislatorsTask = null;
	private ShortcutImageTask shortcutImageTask = null;

	private boolean shortcut;

	private String zipCode, lastName, state, committeeId, committeeName;
	private double latitude = -1;
	private double longitude = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		
		Utils.setupSunlight(this);

		Bundle extras = getIntent().getExtras();

		zipCode = extras.getString("zip_code");
		latitude = extras.getDouble("latitude");
		longitude = extras.getDouble("longitude");
		lastName = extras.getString("last_name");
		state = extras.getString("state");
		committeeId = extras.getString("committeeId");
		committeeName = extras.getString("committeeName");

		shortcut = extras.getBoolean("shortcut", false);

		setupControls();

		LegislatorListHolder holder = (LegislatorListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			legislators = holder.legislators;
			loadLegislatorsTask = holder.loadLegislatorsTask;
			shortcutImageTask = holder.shortcutImageTask;
		}

		if (loadLegislatorsTask == null && shortcutImageTask == null)
			loadLegislators();
		else {
			if (loadLegislatorsTask != null)
				loadLegislatorsTask.onScreenLoad(this);

			if (shortcutImageTask != null)
				shortcutImageTask.onScreenLoad(this);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		LegislatorListHolder holder = new LegislatorListHolder();
		holder.legislators = this.legislators;
		holder.loadLegislatorsTask = this.loadLegislatorsTask;
		holder.shortcutImageTask = this.shortcutImageTask;
		return holder;
	}

	public void loadLegislators() {
		if (legislators == null)
			loadLegislatorsTask = (LoadLegislatorsTask) new LoadLegislatorsTask(this).execute();
		else
			displayLegislators();
	}

	public void displayLegislators() {
		if (legislators.size() > 0)
			setListAdapter(new LegislatorAdapter(this, legislators));
		else {
			switch (searchType()) {
			case SEARCH_ZIP:
				Utils.showBack(this, R.string.empty_zipcode);
				break;
			case SEARCH_LOCATION:
				Utils.showBack(this, R.string.empty_location);
				break;
			case SEARCH_LASTNAME:
				Utils.showBack(this, R.string.empty_last_name);
				break;
			default:
				Utils.showBack(this, R.string.empty_general);
			}
		}
	}

	public void setupControls() {
		((Button) findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		Utils.setLoading(this, R.string.legislators_loading);
		Utils.setTitleSize(this, 20);
		switch (searchType()) {
		case SEARCH_ZIP:
			Utils.setTitle(this, "Legislators For " + zipCode);
			break;
		case SEARCH_LOCATION:
			Utils.setTitle(this, "Legislators For Your Location");
			break;
		case SEARCH_LASTNAME:
			Utils.setTitle(this, "Legislators Named \"" + lastName + "\"");
			break;
		case SEARCH_COMMITTEE:
			Utils.setTitle(this, committeeName);
			break;
		case SEARCH_STATE:
			Utils.setTitle(this, "Legislators from " + Utils.stateCodeToName(this, state));
			break;
		default:
			Utils.setTitle(this, "Legislator Search");
		}
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		selectLegislator((Legislator) parent.getItemAtPosition(position));
	}

	public void selectLegislator(Legislator legislator) {
		if (shortcut)
			shortcutImageTask = (ShortcutImageTask) new ShortcutImageTask(this, legislator).execute();
		else
			startActivity(Utils.legislatorIntent(this, legislator));
	}

	private int searchType() {
		if (zipSearch())
			return SEARCH_ZIP;
		else if (locationSearch())
			return SEARCH_LOCATION;
		else if (lastNameSearch())
			return SEARCH_LASTNAME;
		else if (stateSearch())
			return SEARCH_STATE;
		else if (committeeSearch())
			return SEARCH_COMMITTEE;
		else
			return SEARCH_LOCATION;
	}

	private boolean zipSearch() {
		return zipCode != null;
	}

	private boolean locationSearch() {
		// sucks for people at the intersection of the equator and prime
		// meridian
		return (latitude != 0.0 && longitude != 0.0);
	}

	private boolean lastNameSearch() {
		return lastName != null;
	}

	private boolean stateSearch() {
		return state != null;
	}

	private boolean committeeSearch() {
		return committeeId != null;
	}

	public void returnShortcutIcon(Legislator legislator, Bitmap icon) {
		setResult(RESULT_OK, Utils.shortcutIntent(this, legislator, icon));
		finish();
	}

	private class ShortcutImageTask extends AsyncTask<Void, Void, Bitmap> {
		public LegislatorList context;
		public Legislator legislator;
		private ProgressDialog dialog;

		public ShortcutImageTask(LegislatorList context, Legislator legislator) {
			super();
			this.legislator = legislator;
			this.context = context;
			this.context.shortcutImageTask = this;
		}

		@Override
		protected void onPreExecute() {
			loadingDialog();
		}

		public void onScreenLoad(LegislatorList context) {
			this.context = context;
			loadingDialog();
		}

		@Override
		protected Bitmap doInBackground(Void... nothing) {
			return LegislatorImage.shortcutImage(legislator.bioguide_id, context);
		}

		@Override
		protected void onPostExecute(Bitmap shortcutIcon) {
			if (dialog != null && dialog.isShowing())
				dialog.dismiss();

			context.returnShortcutIcon(legislator, shortcutIcon);

			context.shortcutImageTask = null;
		}

		public void loadingDialog() {
			dialog = new ProgressDialog(context);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Creating shortcut...");

			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cancel(true);
					context.finish();
				}
			});

			dialog.show();
		}
	}

	private class LoadLegislatorsTask extends AsyncTask<Void, Void, ArrayList<Legislator>> {
		public LegislatorList context;
		
		public LoadLegislatorsTask(LegislatorList context) {
			super();
			this.context = context;
		}

		public void onScreenLoad(LegislatorList context) {
			this.context = context;
		}

		@Override
		protected ArrayList<Legislator> doInBackground(Void... nothing) {
			ArrayList<Legislator> legislators = new ArrayList<Legislator>();
			ArrayList<Legislator> lower = new ArrayList<Legislator>();
																		
			ArrayList<Legislator> temp;
			try {
				switch (searchType()) {
				case SEARCH_ZIP:
					temp = Legislator.allForZipCode(zipCode);
					break;
				case SEARCH_LOCATION:
					temp = Legislator.allForLatLong(latitude, longitude);
					break;
				case SEARCH_LASTNAME:
					temp = Legislator.allWhere("lastname__istartswith", lastName);
					break;
				case SEARCH_COMMITTEE:
					temp = Committee.find(committeeId).members;
					break;
				case SEARCH_STATE:
					temp = Legislator.allWhere("state", state);
					break;
				default:
					return legislators;
				}

				// sort legislators Senators-first
				for (int i = 0; i < temp.size(); i++) {
					if (temp.get(i).title.equals("Sen"))
						legislators.add(temp.get(i));
					else
						lower.add(temp.get(i));
				}
				Collections.sort(legislators);
				Collections.sort(lower);
				legislators.addAll(lower);

				return legislators;

			} catch (CongressException exception) {
				return legislators;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<Legislator> legislators) {
			context.legislators = legislators;

			// if there's only one result, don't even make them click it
			if (legislators.size() == 1) {
				context.selectLegislator(legislators.get(0));

				// if we're going on to the profile of a legislator, we want to cut the list out of the stack
				// but if we're generating a shortcut, the shortcut process will be spawning off
				// a separate background thread, that needs a live activity while it works,
				// and will call finish() on its own
				if (!shortcut)
					context.finish();
			} else
				context.displayLegislators();

			context.loadLegislatorsTask = null;
		}
	}

	static class LegislatorListHolder {
		ArrayList<Legislator> legislators;
		LoadLegislatorsTask loadLegislatorsTask;
		ShortcutImageTask shortcutImageTask;
	}
}