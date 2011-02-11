package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.LocationUtils;
import com.sunlightlabs.android.congress.utils.LocationUtils.LocationListenerTimeout;
import com.sunlightlabs.android.congress.utils.LocationUtils.LocationTimer;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.BillService;
import com.sunlightlabs.congress.services.CommitteeService;
import com.sunlightlabs.congress.services.LegislatorService;

public class LegislatorList extends ListActivity implements LoadPhotoTask.LoadsPhoto,
		LocationListenerTimeout {
	public final static int SEARCH_ZIP = 0;
	public final static int SEARCH_LOCATION = 1;
	public final static int SEARCH_STATE = 2;
	public final static int SEARCH_LASTNAME = 3;
	public final static int SEARCH_COMMITTEE = 4;
	public static final int SEARCH_COSPONSORS = 5;

	public final static String TAG = "CONGRESS";

	private List<Legislator> legislators = null;
	private LoadLegislatorsTask loadLegislatorsTask = null;

	private Map<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();
	
	private int type = -1;
	
	private String bill_id;

	private String zipCode, lastName, state, committeeId, committeeName;
	private double latitude = -1;
	private double longitude = -1;

	private LocationTimer timer;
	private boolean relocating = false;

	private HeaderViewWrapper headerWrapper;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			onTimeout((String) msg.obj);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled_header);

		Utils.setupSunlight(this);

		Bundle extras = getIntent().getExtras();
		type = extras.getInt("type");

		zipCode = extras.getString("zip_code");
		latitude = extras.getDouble("latitude");
		longitude = extras.getDouble("longitude");
		lastName = extras.getString("last_name");
		state = extras.getString("state");
		committeeId = extras.getString("committeeId");
		committeeName = extras.getString("committeeName");
		bill_id = extras.getString("bill_id");

		LegislatorListHolder holder = (LegislatorListHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			legislators = holder.legislators;
			loadLegislatorsTask = holder.loadLegislatorsTask;
			loadPhotoTasks = holder.loadPhotoTasks;

			if (loadPhotoTasks != null) {
				Iterator<LoadPhotoTask> iterator = loadPhotoTasks.values().iterator();
				while (iterator.hasNext())
					iterator.next().onScreenLoad(this);
			}
			
			relocating = holder.relocating;
			tracked = holder.tracked;
		}
		
		setupControls();
		
		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, url());
			tracked = true;
		}

		if (loadLegislatorsTask == null) {
			if (legislators == null && type == SEARCH_LOCATION) {
				if (!relocating)
					updateLocation();
				// if currently relocating, do nothing and just wait for it to complete
			} else
				loadLegislators();
		} else
			loadLegislatorsTask.onScreenLoad(this);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new LegislatorListHolder(legislators, loadLegislatorsTask, loadPhotoTasks, relocating, tracked);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (type == SEARCH_LOCATION) {
			cancelTimer();
			relocating = false;
			toggleRelocating();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}

	public void loadLegislators() {
		if (legislators == null)
			loadLegislatorsTask = (LoadLegislatorsTask) new LoadLegislatorsTask(this).execute();
		else
			displayLegislators();
	}

	public void displayLegislators() {
		if (type == SEARCH_LOCATION)
			headerWrapper.getBase().setVisibility(View.VISIBLE);
		
		if (legislators.size() > 0)
			setListAdapter(new LegislatorAdapter(this, legislators));
		else {
			switch (type) {
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
	
	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id)) {
			try {
				loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
			} catch (RejectedExecutionException e) {
				Log.e(TAG, "[LegislatorList] RejectedExecutionException occurred while loading photo.", e);
				onLoadPhoto(null, bioguide_id); // if we can't run it, then just show the no photo image and move on
			}
		}
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		loadPhotoTasks.remove(tag);
		
		LegislatorAdapter.ViewHolder holder = new LegislatorAdapter.ViewHolder();
		holder.bioguide_id = (String) tag;

		View result = getListView().findViewWithTag(holder);
		if (result != null) {
			if (photo != null)
				((ImageView) result.findViewById(R.id.photo)).setImageDrawable(photo);
			else // don't know the gender from here, default to female (to balance out how the shortcut image defaults to male)
				((ImageView) result.findViewById(R.id.photo)).setImageResource(R.drawable.no_photo_female);
		}
	}

	public Context getContext() {
		return this;
	}

	public void setupControls() {
		((Button) findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		Utils.setLoading(this, R.string.legislators_loading);
		Utils.setTitleSize(this, 20);
		Utils.setTitleIcon(this, R.drawable.person);
		
		switch (type) {
		case SEARCH_ZIP:
			Utils.setTitle(this, "Legislators For " + zipCode);
			break;
		case SEARCH_LOCATION:
			showHeader(); // make the location update header visible
			Utils.setTitle(this, "Your Legislators");
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
		case SEARCH_COSPONSORS:
			Utils.setTitle(this, "Cosponsors for\n" + Bill.formatId(bill_id));
			Utils.setTitleSize(this, 18);
			Utils.setLoading(this, R.string.legislators_loading_cosponsors);
			break;
		default:
			Utils.setTitle(this, "Legislator Search");
		}
	}
	
	public String url() {
		if (type == SEARCH_ZIP)
			return "/legislators/zip";
		else if (type == SEARCH_LOCATION)
			return "/legislators/location";
		else if (type == SEARCH_LASTNAME)
			return "/legislators/lastname";
		else if (type == SEARCH_COMMITTEE)
			return "/committee/" + committeeId + "/legislators";
		else if (type == SEARCH_STATE)
			return "/legislators/state";
		else if (type == SEARCH_COSPONSORS)
			return "/bill/" + bill_id + "/cosponsors";
		else
			return "/legislators";
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		selectLegislator((Legislator) parent.getItemAtPosition(position));
	}

	public void selectLegislator(Legislator legislator) {
		if (type == SEARCH_COSPONSORS) // cosponsors from Drumbone don't have enough info to go direct
			startActivity(Utils.legislatorLoadIntent(legislator.id));
		else
			startActivity(Utils.legislatorIntent(this, legislator));
	}
	
	private static class LegislatorAdapter extends ArrayAdapter<Legislator> {
		LayoutInflater inflater;
		LegislatorList context;

		public LegislatorAdapter(LegislatorList context, List<Legislator> items) {
			super(context, 0, items);
			this.context = context;
			inflater = LayoutInflater.from(context);
		}
		
		@Override
        public boolean areAllItemsEnabled() {
        	return true;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 1;
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;
			if (convertView == null) {
				view = inflater.inflate(R.layout.legislator_item, null);
				
				holder = new ViewHolder();
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.position = (TextView) view.findViewById(R.id.position);
				holder.photo = (ImageView) view.findViewById(R.id.photo);
				
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			
			Legislator legislator = getItem(position);

			// used as the hook to get the legislator image in place when it's loaded
			holder.bioguide_id = legislator.bioguide_id;

			holder.name.setText(nameFor(legislator));
			holder.position.setText(positionFor(legislator));

			BitmapDrawable photo = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM, legislator.bioguide_id, context);
			if (photo != null)
				holder.photo.setImageDrawable(photo);
			else {
				holder.photo.setImageResource(R.drawable.loading_photo);
				context.loadPhoto(legislator.bioguide_id);
			}

			return view;
		}

		public String nameFor(Legislator legislator) {
			return legislator.last_name + ", " + legislator.firstName();
		}

		public String positionFor(Legislator legislator) {
			String district = legislator.district;
			String stateName = Utils.stateCodeToName(context, legislator.state);			
			String position = "";

			if (district.equals("Senior Seat"))
				position = "Senior Senator from " + stateName;
			else if (district.equals("Junior Seat"))
				position = "Junior Senator from " + stateName;
			else if (district.equals("0")) {
				if (legislator.title.equals("Rep"))
					position = "Representative for " + stateName + " At-Large";
				else
					position = legislator.fullTitle() + " for " + stateName;
			} else
				position = "Representative for " + stateName + "-" + district;
			
			return "(" + legislator.party + ") " + position; 
		}
		
		static class ViewHolder {
			TextView name, position;
			ImageView photo;
			String bioguide_id;
			
			@Override
			public boolean equals(Object holder) {
				ViewHolder other = (ViewHolder) holder;
				return other != null && other instanceof ViewHolder && this.bioguide_id.equals(other.bioguide_id);
			}
		}

	}

	
	private class LoadLegislatorsTask extends AsyncTask<Void, Void, List<Legislator>> {
		public LegislatorList context;

		public LoadLegislatorsTask(LegislatorList context) {
			super();
			this.context = context;
		}

		public void onScreenLoad(LegislatorList context) {
			this.context = context;
		}

		@Override
		protected List<Legislator> doInBackground(Void... nothing) {
			List<Legislator> legislators = new ArrayList<Legislator>();
			List<Legislator> lower = new ArrayList<Legislator>();

			List<Legislator> temp;
			try {
				switch (context.type) {
				case SEARCH_ZIP:
					temp = LegislatorService.allForZipCode(zipCode);
					break;
				case SEARCH_LOCATION:
					temp = LegislatorService.allForLatLong(latitude, longitude);
					break;
				case SEARCH_LASTNAME:
					temp = LegislatorService.allWhere("lastname__istartswith", lastName);
					break;
				case SEARCH_COMMITTEE:
					temp = CommitteeService.find(committeeId).members;
					break;
				case SEARCH_STATE:
					temp = LegislatorService.allWhere("state", state);
					break;
				case SEARCH_COSPONSORS:
					temp = BillService.find(bill_id, new String[] {"cosponsors"}).cosponsors;
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
		protected void onPostExecute(List<Legislator> legislators) {
			context.legislators = legislators;
			
			// if there's only one result, don't even make them click it
			if (legislators.size() == 1 && (context.type != SEARCH_LOCATION && context.type != SEARCH_COSPONSORS)) {
				context.selectLegislator(legislators.get(0));
				context.finish();
			} else
				context.displayLegislators();
			
			context.loadLegislatorsTask = null;
		}
	}

	static class LegislatorListHolder {
		List<Legislator> legislators;
		LoadLegislatorsTask loadLegislatorsTask;
		Map<String,LoadPhotoTask> loadPhotoTasks;

		boolean relocating;
		boolean tracked;
		
		LegislatorListHolder(List<Legislator> legislators, LoadLegislatorsTask loadLegislatorsTask, Map<String,LoadPhotoTask> loadPhotoTasks,
				boolean relocating, boolean tracked) {
			this.legislators = legislators;
			this.loadLegislatorsTask = loadLegislatorsTask;
			this.loadPhotoTasks = loadPhotoTasks;
			this.relocating = relocating;
			this.tracked = tracked;
		}
	}

	private class HeaderViewWrapper {
		private TextView txt;
		private View base;
		private View loading;

		public HeaderViewWrapper(View base) {
			this.base = base;
		}
		
		public View getBase() {
			return base;
		}
		
		public TextView getTxt() {
			return (txt == null ? txt = (TextView) base.findViewById(R.id.text_1) : txt);
		}

		public View getLoading() {
			return (loading == null ? loading = base.findViewById(R.id.updating_spinner) : loading);
		}
	}

	private void showHeader() {
		headerWrapper = new HeaderViewWrapper(findViewById(R.id.list_header));
		headerWrapper.getTxt().setText(R.string.location_update);
		headerWrapper.getBase().setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!relocating)
					updateLocation();
			}
		});
	}

	private void toggleRelocating() {
		Log.d(TAG, "LegislatorList - toggleRelocating(): relocating is " + relocating);
		headerWrapper.getBase().setEnabled(relocating ? false : true);
		headerWrapper.getLoading().setVisibility(relocating ? View.VISIBLE : View.GONE);
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			Log.d(TAG, "LegislatorList - cancelTimer(): end updating timer");
		}
	}

	private void updateLocation() {
		relocating = true;
		toggleRelocating();
		timer = LocationUtils.requestLocationUpdate(this, handler, LocationManager.GPS_PROVIDER);
	}

	private void reloadLegislators() {
		legislators = null;
		loadLegislators();
	}
	
	public void onLocationUpdateError() {
		if (relocating) {
			Log.d(TAG, "LegislatorList - onLocationUpdateError(): cannot update location");
			relocating = false;
			toggleRelocating();

			Toast.makeText(this, R.string.location_update_fail, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void onLocationChanged(Location location) {
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		cancelTimer();
		
		relocating = false;
		toggleRelocating();
		
		reloadLegislators();
	}

	public void onProviderDisabled(String provider) {}

	public void onProviderEnabled(String provider) {}

	public void onStatusChanged(String provider, int status, Bundle extras) {}

	public void onTimeout(String provider) {
		Log.d(TAG, "LegislatorList - onTimeout(): timeout for provider " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			timer = LocationUtils.requestLocationUpdate(this, handler,
					LocationManager.NETWORK_PROVIDER);
			Log.d(TAG, "LegislatorList - onTimeout(): requesting update from network");
		} else
			onLocationUpdateError();
	}
}