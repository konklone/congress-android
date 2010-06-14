package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
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

import com.sunlightlabs.android.congress.utils.AddressUpdater;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.LocationUpdater;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.AddressUpdater.AddressUpdateable;
import com.sunlightlabs.android.congress.utils.LocationUpdater.LocationUpdateable;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.CommitteeService;
import com.sunlightlabs.congress.services.LegislatorService;

public class LegislatorList extends ListActivity implements LoadPhotoTask.LoadsPhoto,
		LocationUpdateable<LegislatorList>, LocationListener, AddressUpdateable<LegislatorList> {
	private final static int SEARCH_ZIP = 0;
	private final static int SEARCH_LOCATION = 1;
	private final static int SEARCH_STATE = 2;
	private final static int SEARCH_LASTNAME = 3;
	private final static int SEARCH_COMMITTEE = 4;

	private static final int MSG_TIMEOUT = 100;

	private final static String TAG = "CONGRESS";

	private ArrayList<Legislator> legislators = null;
	private LoadLegislatorsTask loadLegislatorsTask = null;

	private HashMap<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();

	private String zipCode, lastName, state, committeeId, committeeName;
	private double latitude = -1;
	private double longitude = -1;

	private String address;

	private LocationUpdater locationUpdater;
	private AddressUpdater addressUpdater;
	private boolean relocating = false;

	private HeaderViewWrapper headerWrapper;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == MSG_TIMEOUT) {
				Log.d(TAG, "handleMessage(): received message=" + msg);
				onLocationUpdateError((CongressException) msg.obj);
				locationUpdater.requestLocationUpdateHalt();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled_header);

		Utils.setupSunlight(this);

		Bundle extras = getIntent().getExtras();

		zipCode = extras.getString("zip_code");
		latitude = extras.getDouble("latitude");
		longitude = extras.getDouble("longitude");
		address = extras.getString("address");
		lastName = extras.getString("last_name");
		state = extras.getString("state");
		committeeId = extras.getString("committeeId");
		committeeName = extras.getString("committeeName");

		Log.d(TAG, "onCreate(): latitude=" + latitude + ",longitude=" + longitude + ",address=" + address);

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

			locationUpdater = holder.locationUpdater;
			addressUpdater = holder.addressUpdater;
			address = holder.address;
			relocating = holder.relocating;
		}

		if (loadLegislatorsTask == null)
			loadLegislators();
		else {
			if (loadLegislatorsTask != null)
				loadLegislatorsTask.onScreenLoad(this);
		}

		if(locationUpdater == null)
			locationUpdater = new LocationUpdater(this);
		else
			locationUpdater.onScreenLoad(this);

		if(addressUpdater != null)
			addressUpdater.onScreenLoad(this);

		setupControls();

		if (relocating) {
			Log.d(TAG, "onCreate(): relocating=" + relocating);
			toggleRelocating(true);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		LegislatorListHolder holder = new LegislatorListHolder();
		holder.legislators = this.legislators;
		holder.loadLegislatorsTask = this.loadLegislatorsTask;
		holder.loadPhotoTasks = this.loadPhotoTasks;

		holder.addressUpdater = addressUpdater;
		holder.locationUpdater = locationUpdater;
		holder.address = address;
		holder.relocating = relocating;
		return holder;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroying activity. Remove location updates");
		locationUpdater.requestLocationUpdateHalt();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "Pausing activity. Remove location updates");
		locationUpdater.requestLocationUpdateHalt();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming activity. Request location update");
		locationUpdater.requestLocationUpdate();
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

	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id))
			loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		loadPhotoTasks.remove((String) tag);
		
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
		switch (searchType()) {
		case SEARCH_ZIP:
			Utils.setTitle(this, "Legislators For " + zipCode);
			break;
		case SEARCH_LOCATION:
			showHeader(); // make the location update header visible
			displayAddress(address);
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


	private static class LegislatorAdapter extends ArrayAdapter<Legislator> {
		LayoutInflater inflater;
		LegislatorList context;

		public LegislatorAdapter(LegislatorList context, ArrayList<Legislator> items) {
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
			if (legislators.size() == 1 && searchType() != SEARCH_LOCATION) {
				context.selectLegislator(legislators.get(0));
				context.finish();
			} else
				context.displayLegislators();
			
			context.loadLegislatorsTask = null;
		}
	}

	static class LegislatorListHolder {
		ArrayList<Legislator> legislators;
		LoadLegislatorsTask loadLegislatorsTask;
		HashMap<String,LoadPhotoTask> loadPhotoTasks;

		AddressUpdater addressUpdater;
		LocationUpdater locationUpdater;
		String address;
		boolean relocating;
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
			return (txt == null ? txt = (TextView) base.findViewById(R.id.text_2) : txt);
		}

		public View getLoading() {
			return (loading == null ? loading = base
					.findViewById(R.id.updating_spinner) : loading);
		}
	}

	private void showHeader() {
		headerWrapper = new HeaderViewWrapper(findViewById(R.id.list_header));
		headerWrapper.getBase().setVisibility(View.VISIBLE);
		((TextView) headerWrapper.getBase().findViewById(R.id.text_1)).setText(R.string.location_not_accurate);
		TextView txt = headerWrapper.getTxt();
		txt.setText(R.string.update);
		txt.setClickable(true);
		txt.setFocusable(true);
		txt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				updateLocation();
			}
		});
	}

	private void toggleRelocating(boolean updating) {
		headerWrapper.getTxt().setText(updating ? R.string.updating : R.string.update);
		headerWrapper.getTxt().setEnabled(updating ? false : true);
		headerWrapper.getLoading().setVisibility(updating ? View.VISIBLE : View.GONE);
	}

	private void updateLocation() {
		relocating = true;
		locationUpdater.requestLocationUpdate();
		toggleRelocating(true);
	}

	private void displayAddress(String address) {
		Utils.setTitle(this, "Legislators For " + ((address == null || address.equals("")) ? "Your Location" : address));
	}

	private void reloadLegislators() {
		legislators = null;
		loadLegislators();
	}

	public void onLocationUpdate(Location location) {
		Log.d(TAG, "onLocationUpdate(): location=" + location);
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		addressUpdater = (AddressUpdater) new AddressUpdater(this).execute(location);
		locationUpdater.requestLocationUpdateHalt();
	}

	public void onLocationUpdateError(CongressException e) {
		Log.d(TAG, "onLocationUpdateError(): e=" + e + ", relocating=" + relocating);
		if(relocating) {
			toggleRelocating(false);
			relocating = false;
			Toast.makeText(this, R.string.location_update_fail, Toast.LENGTH_SHORT).show();	
		}
	}

	public void onAddressUpdate(String address) {
		Log.d(TAG, "onAddressUpdate(): address=" + address);
		this.address = address;
		displayAddress(address);

		addressUpdater = null;
		toggleRelocating(false);
		relocating = false;

		reloadLegislators();
	}

	public void onAddressUpdateError(CongressException e) {
		Log.d(TAG, "onAddressUpdateError(): e=" + e);
		this.address = "";
		displayAddress(address);

		addressUpdater = null;
		toggleRelocating(false);
		relocating = false;
	}

	public Handler getHandler() {
		return handler;
	}

	public void onLocationChanged(Location location) {
		locationUpdater.onLocationChanged(location);
	}

	public void onProviderDisabled(String provider) {
		locationUpdater.onProviderDisabled(provider);
	}

	public void onProviderEnabled(String provider) {
		locationUpdater.onProviderEnabled(provider);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		locationUpdater.onStatusChanged(provider, status, extras);
	}
}