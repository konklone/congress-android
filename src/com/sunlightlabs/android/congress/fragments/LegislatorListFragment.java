package com.sunlightlabs.android.congress.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.LegislatorService;

public class LegislatorListFragment extends ListFragment implements LoadPhotoTask.LoadsPhoto { //, LocationListenerTimeout {
	public static final int SEARCH_ZIP = 0;
//	public static final int SEARCH_LOCATION = 1;
	public static final int SEARCH_STATE = 2;
	public static final int SEARCH_LASTNAME = 3;
//	public static final int SEARCH_COMMITTEE = 4;
//	public static final int SEARCH_COSPONSORS = 5;
	public static final int SEARCH_CHAMBER = 6; 
	
	List<Legislator> legislators;
	Map<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();
	int type;
	String chamber;
	//String bill_id;
	String zipCode, lastName, state; // committeeId, committeeName;
	
	//double latitude = -1;
	//double longitude = -1;

//	private LocationTimer timer;
//	private boolean relocating = false;
//
//	private HeaderViewWrapper headerWrapper;
	
//	private Handler handler = new Handler() {
//		@Override
//		public void handleMessage(Message msg) {
//			onTimeout((String) msg.obj);
//		}
//	};
	
	public static LegislatorListFragment forChamber(String chamber) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_CHAMBER);
		args.putString("chamber", chamber);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static LegislatorListFragment forState(String state) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_STATE);
		args.putString("state", state);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static LegislatorListFragment forZip(String zip) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_ZIP);
		args.putString("zip", zip);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static LegislatorListFragment forLastName(String lastName) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_LASTNAME);
		args.putString("last_name", lastName);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public LegislatorListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getInt("type");
		chamber = args.getString("chamber");
		zipCode = args.getString("zip");
		lastName = args.getString("last_name");
		state = args.getString("state");
		
		
		loadLegislators();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.list, container, false);
		if (type == SEARCH_CHAMBER) {
			ListView list = (ListView) view.findViewById(android.R.id.list);
			list.setFastScrollEnabled(true);
		}
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (legislators != null)
			displayLegislators();
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		FragmentUtils.setLoading(this, R.string.legislators_loading);
		
//		case SEARCH_LOCATION:
//			showHeader(); // make the location update header visible
//			ActionBarUtils.setTitle(this, "Your Legislators");
//			break;
//		case SEARCH_COMMITTEE:
//			ActionBarUtils.setTitle(this, committeeName);
//			break;
//		case SEARCH_COSPONSORS:
//			ActionBarUtils.setTitle(this, "Cosponsors for " + Bill.formatId(bill_id));
//			ActionBarUtils.setTitleSize(this, 16);
//			Utils.setLoading(this, R.string.legislators_loading_cosponsors);
//			break;
	}


//			if (legislators == null && type == SEARCH_LOCATION) {
//				if (!relocating)
//					updateLocation();
				// if currently relocating, do nothing and just wait for it to complete
	

	@Override
	public void onStop() {
		super.onStop();
//		if (type == SEARCH_LOCATION) {
//			cancelTimer();
//			relocating = false;
//			toggleRelocating();
//		}
	}
	
	
	public void loadLegislators() {
		new LoadLegislatorsTask(this).execute();
	}
	
	public void onLoadLegislators(List<Legislator> legislators) {
		// if there's only one result, don't even make them click it
//		if (legislators.size() == 1 && (context.type != SEARCH_LOCATION && context.type != SEARCH_COSPONSORS)) {
//			context.selectLegislator(legislators.get(0));
//			context.finish();
//		} 
		
		if (isAdded())
			displayLegislators();
	}
	
	public void onLoadLegislators(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.legislators_error);
	}

	public void displayLegislators() {
//		if (type == SEARCH_LOCATION)
//			headerWrapper.getBase().setVisibility(View.VISIBLE);
		
		if (legislators.size() > 0)
			setListAdapter(new LegislatorAdapter(this, legislators));
		else {
			switch (type) {
			case SEARCH_ZIP:
				FragmentUtils.showEmpty(this, R.string.empty_zipcode);
				break;
//			case SEARCH_LOCATION:
//				FragmentUtils.showEmpty(this, R.string.empty_location);
//				break;
			case SEARCH_LASTNAME:
				FragmentUtils.showEmpty(this, R.string.empty_last_name);
				break;
			default:
				FragmentUtils.showEmpty(this, R.string.legislators_error);
			}
		}
	}
	
	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id)) {
			try {
				loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
			} catch (RejectedExecutionException e) {
				Log.e(Utils.TAG, "[LegislatorListFragment] RejectedExecutionException occurred while loading photo.", e);
				onLoadPhoto(null, bioguide_id); // if we can't run it, then just show the no photo image and move on
			}
		}
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		if (!isAdded())
			return;
		
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
		return getActivity();
	}
	
	private void refresh() {
		legislators = null;
		FragmentUtils.setLoading(this, R.string.legislators_loading);
		FragmentUtils.showLoading(this);
		loadLegislators();
	}

//	public String url() {
//		
//		else if (type == SEARCH_LOCATION)
//			return "/legislators/location";
//		else if (type == SEARCH_COMMITTEE)
//			return "/committee/" + committeeId + "/legislators";
//		else if (type == SEARCH_COSPONSORS)
//			return "/bill/" + bill_id + "/cosponsors";
//	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		selectLegislator((Legislator) parent.getItemAtPosition(position));
	}

	public void selectLegislator(Legislator legislator) {
//		if (type == SEARCH_COSPONSORS) // cosponsors from RTC don't have enough info to go direct
//			startActivity(Utils.legislatorLoadIntent(legislator.id));
//		else
			startActivity(Utils.legislatorIntent(getActivity(), legislator));
	}
	
	private static class LegislatorAdapter extends ArrayAdapter<Legislator> {
		LayoutInflater inflater;
		LegislatorListFragment context;

		public LegislatorAdapter(LegislatorListFragment context, List<Legislator> items) {
			super(context.getActivity(), 0, items);
			this.context = context;
			inflater = LayoutInflater.from(context.getActivity());
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

			BitmapDrawable photo = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM, legislator.bioguide_id, context.getActivity());
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
			String stateName = Utils.stateCodeToName(context.getActivity(), legislator.state);
			String district;
			if (legislator.chamber.equals("senate"))
				district = legislator.district;
			else
				district = "District " + legislator.district;
			return legislator.party + " - " + stateName + " - " + district; 
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

	
	private static class LoadLegislatorsTask extends AsyncTask<Void, Void, List<Legislator>> {
		LegislatorListFragment context;
		CongressException exception;

		public LoadLegislatorsTask(LegislatorListFragment context) {
			super();
			this.context = context;
			FragmentUtils.setupSunlight(context);
		}

		@Override
		protected List<Legislator> doInBackground(Void... nothing) {
			List<Legislator> legislators = new ArrayList<Legislator>();
			List<Legislator> lower = new ArrayList<Legislator>();

			List<Legislator> temp;
			try {
				switch (context.type) {
				case SEARCH_ZIP:
					temp = LegislatorService.allForZipCode(context.zipCode);
					break;
//				case SEARCH_LOCATION:
//					temp = LegislatorService.allForLatLong(context.latitude, context.longitude);
//					break;
				case SEARCH_LASTNAME:
					temp = LegislatorService.allWhere("lastname__istartswith", context.lastName);
					break;
//				case SEARCH_COMMITTEE:
//					temp = CommitteeService.find(context.committeeId).members;
//					break;
				case SEARCH_STATE:
					temp = LegislatorService.allWhere("state", context.state);
					break;
//				case SEARCH_COSPONSORS:
//					temp = BillService.find(context.bill_id, new String[] {"cosponsors"}).cosponsors;
//					break;
				case SEARCH_CHAMBER:
					return LegislatorService.allForChamber(context.chamber);
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
				this.exception = exception;
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<Legislator> legislators) {
			context.legislators = legislators;
			
			if (exception == null)
				context.onLoadLegislators(legislators);
			else
				context.onLoadLegislators(exception);
		}
	}

//	private class HeaderViewWrapper {
//		private TextView txt;
//		private View base;
//		private View loading;
//
//		public HeaderViewWrapper(View base) {
//			this.base = base;
//		}
//		
//		public View getBase() {
//			return base;
//		}
//		
//		public TextView getTxt() {
//			return (txt == null ? txt = (TextView) base.findViewById(R.id.text_1) : txt);
//		}
//
//		public View getLoading() {
//			return (loading == null ? loading = base.findViewById(R.id.updating_spinner) : loading);
//		}
//	}
//
//	private void showHeader() {
//		headerWrapper = new HeaderViewWrapper(findViewById(R.id.list_header));
//		headerWrapper.getTxt().setText(R.string.location_update);
//		headerWrapper.getBase().setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				if (!relocating)
//					updateLocation();
//			}
//		});
//	}
//
//	private void toggleRelocating() {
//		Log.d(Utils.TAG, "LegislatorListFragment - toggleRelocating(): relocating is " + relocating);
//		headerWrapper.getBase().setEnabled(relocating ? false : true);
//		headerWrapper.getTxt().setText(relocating ? R.string.menu_location_updating : R.string.location_update);
//		headerWrapper.getLoading().setVisibility(relocating ? View.VISIBLE : View.GONE);
//	}
//
//	private void cancelTimer() {
//		if (timer != null) {
//			timer.cancel();
//			Log.d(Utils.TAG, "LegislatorListFragment - cancelTimer(): end updating timer");
//		}
//	}
//
//	private void updateLocation() {
//		relocating = true;
//		toggleRelocating();
//		timer = LocationUtils.requestLocationUpdate(this, handler, LocationManager.GPS_PROVIDER);
//	}
//
//	private void reloadLegislators() {
//		legislators = null;
//		loadLegislators();
//	}
//	
//	public void onLocationUpdateError() {
//		if (relocating) {
//			Log.d(Utils.TAG, "LegislatorListFragment - onLocationUpdateError(): cannot update location");
//			relocating = false;
//			toggleRelocating();
//
//			Toast.makeText(this, R.string.location_update_fail, Toast.LENGTH_SHORT).show();
//		}
//	}
//	
//	public void onLocationChanged(Location location) {
//		latitude = location.getLatitude();
//		longitude = location.getLongitude();
//		cancelTimer();
//		
//		relocating = false;
//		toggleRelocating();
//		
//		reloadLegislators();
//	}
//
//	public void onProviderDisabled(String provider) {}
//
//	public void onProviderEnabled(String provider) {}
//
//	public void onStatusChanged(String provider, int status, Bundle extras) {}
//
//	public void onTimeout(String provider) {
//		Log.d(Utils.TAG, "LegislatorListFragment - onTimeout(): timeout for provider " + provider);
//		if (provider.equals(LocationManager.GPS_PROVIDER)) {
//			timer = LocationUtils.requestLocationUpdate(this, handler,
//					LocationManager.NETWORK_PROVIDER);
//			Log.d(Utils.TAG, "LegislatorListFragment - onTimeout(): requesting update from network");
//		} else
//			onLocationUpdateError();
//	}
}