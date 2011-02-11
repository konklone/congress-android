package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.MenuLegislators.FavoriteLegislatorsAdapter.FavoriteLegislatorWrapper;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.android.congress.utils.ViewWrapper;
import com.sunlightlabs.congress.models.Legislator;

public class MenuLegislators extends ListActivity implements LoadPhotoTask.LoadsPhoto {
	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	public static final int RESULT_STATE = 3;
	
	public static final int SEARCH_LOCATION = 3;
	public static final int SEARCH_ZIP = 4;
	public static final int SEARCH_STATE = 5;
	public static final int SEARCH_NAME = 6;
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked;
	
	private Map<String, LoadPhotoTask> loadPhotoTasks = new HashMap<String, LoadPhotoTask>();
	private Map<String, FavoriteLegislatorWrapper> favoritePeopleWrappers = new HashMap<String, FavoriteLegislatorWrapper>();
	
	private Database database;
	private Cursor cursor;
	
	private MergeAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_titled);
		
		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null) {
			loadPhotoTasks = holder.loadPhotoTasks;

			if (loadPhotoTasks != null) {
				Iterator<LoadPhotoTask> iterator = loadPhotoTasks.values().iterator();
				while (iterator.hasNext())
					iterator.next().onScreenLoad(this);
			}
			favoritePeopleWrappers = holder.favoritePeopleWrappers;
			tracked = holder.tracked;
		}

		setupDatabase();
		setupControls();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Holder(loadPhotoTasks, favoritePeopleWrappers, tracked);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
		Analytics.stop(tracker);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (cursor != null)
			cursor.requery();
		adapter.notifyDataSetChanged();
	}
	
	private void setupControls() {
		Utils.setTitle(this, R.string.menu_main_legislators);
		Utils.setTitleIcon(this, R.drawable.person);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		adapter = new MergeAdapter();

		List<View> searchViews = new ArrayList<View>(5);
		searchViews.add(inflateItem(inflater, R.drawable.search_location, R.string.menu_legislators_location, SEARCH_LOCATION));
		searchViews.add(inflateItem(inflater, R.drawable.search_all, R.string.menu_legislators_state, SEARCH_STATE));
		searchViews.add(inflateItem(inflater, R.drawable.search_lastname, R.string.menu_legislators_lastname, SEARCH_NAME));            
		searchViews.add(inflateItem(inflater, R.drawable.search_zip, R.string.menu_legislators_zip, SEARCH_ZIP));
		adapter.addAdapter(new ViewArrayAdapter(this, searchViews));
		
		adapter.addView(Utils.inflateHeader(inflater, R.string.menu_legislators_favorite));
		
		if (cursor != null && cursor.getCount() > 0) {
			adapter.addAdapter(new FavoriteLegislatorsAdapter(this, cursor));
		} else {
			TextView noFavorites = (TextView) inflater.inflate(R.layout.menu_no_favorites, null);
			noFavorites.setText(R.string.menu_legislators_no_favorites);
			adapter.addView(noFavorites);
		}
		
		setListAdapter(adapter);
	}
	
	public void setupDatabase() {
		database = new Database(this);
		database.open();
		
		cursor = database.getLegislators();
		startManagingCursor(cursor);
	}
	
	private View inflateItem(LayoutInflater inflater, int icon, int text, Object tag) {
		View item = inflater.inflate(R.layout.menu_item, null);
		((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
		((TextView) item.findViewById(R.id.text)).setText(text);
		item.setTag(new ViewWrapper(item, tag));
		return item;
	}

	private void searchByZip(String zipCode) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_ZIP)
			.putExtra("zip_code", zipCode));
	}

	private void searchByLocation() {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_LOCATION));
	}

	private void searchByLastName(String lastName) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_LASTNAME)
			.putExtra("last_name", lastName));
	}

	private void searchByState(String state) {
		startActivity(new Intent(this, LegislatorList.class)
			.putExtra("type", LegislatorList.SEARCH_STATE)
			.putExtra("state", state));
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Object tag = v.getTag();
		if (tag instanceof ViewWrapper) {
			ViewWrapper wrapper = (ViewWrapper) v.getTag();
			int type = ((Integer) wrapper.getTag()).intValue();
			switch (type) {
			case SEARCH_LOCATION:
				searchByLocation();
				break;
			case SEARCH_ZIP:
				getResponse(RESULT_ZIP);
				break;
			case SEARCH_NAME:
				getResponse(RESULT_LASTNAME);
				break;
			case SEARCH_STATE:
				getResponse(RESULT_STATE);
				break;
			default:
				break;
			}
		}
		else if (tag instanceof FavoriteLegislatorWrapper)
			startActivity(Utils.legislatorLoadIntent(((FavoriteLegislatorWrapper) tag).legislator.id));
	}
	
	private void getResponse(int requestCode) {
		Intent intent = new Intent();

		switch (requestCode) {
		case RESULT_ZIP:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a zip code:")
			.putExtra("hint", "e.g. 11216")
			.putExtra("startValue", Utils.getStringPreference(this, "search_zip"))
			.putExtra("inputType", InputType.TYPE_CLASS_PHONE);
			break;
		case RESULT_LASTNAME:
			intent.setClass(this, GetText.class)
			.putExtra("ask", "Enter a last name:")
			.putExtra("hint", "e.g. Schumer")
			.putExtra("startValue", Utils.getStringPreference(this, "search_lastname"))
			.putExtra("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			break;
		case RESULT_STATE:
			intent.setClass(this, GetState.class)
			.putExtra("startValue", Utils.getStringPreference(this, "search_state"));
			break;
		default:
			break;
		}

		startActivityForResult(intent, requestCode);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_ZIP:
			if (resultCode == RESULT_OK) {
				String zipCode = data.getExtras().getString("response").trim();
				if (!zipCode.equals("")) {
					Utils.setStringPreference(this, "search_zip", zipCode);
					searchByZip(zipCode);
				}
			}
			break;
		case RESULT_LASTNAME:
			if (resultCode == RESULT_OK) {
				String lastName = data.getExtras().getString("response").trim();
				if (!lastName.equals("")) {
					Utils.setStringPreference(this, "search_lastname", lastName);
					searchByLastName(lastName);
				}
			}
			break;
		case RESULT_STATE:
			if (resultCode == RESULT_OK) {
				String state = data.getExtras().getString("response").trim();
				if (!state.equals("")) {
					String code = Utils.stateNameToCode(this, state);
					if (code != null) {
						Utils.setStringPreference(this, "search_state", state); // store the name, not the code
						searchByState(code);
					}
				}
			}
			break;
//		case RESULT_BILL_CODE:
//			if (resultCode == RESULT_OK) {
//				String code = data.getExtras().getString("response").trim();
//				if (!code.equals("")) {
//					String billId = Bill.codeToBillId(code);
//					if (billId != null) {
//						Utils.setStringPreference(this, "search_bill_code", code); // store the code, not the bill_id
//						searchByBillId(billId, code);
//					}
//				}
//			}
//			break;
		}
	}
	
	public void loadPhoto(String bioguide_id, FavoriteLegislatorWrapper wrapper) {
		if (!loadPhotoTasks.containsKey(bioguide_id)) {
			loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this,
					LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
			favoritePeopleWrappers.put(bioguide_id, wrapper);
		}
	}

	public Context getContext() {
		return this;
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		String bioguide_id = (String) tag;
		loadPhotoTasks.remove(bioguide_id);
		favoritePeopleWrappers.get(bioguide_id).onLoadPhoto(photo, bioguide_id);
		favoritePeopleWrappers.remove(bioguide_id);
	}
	
	// Favorite legislators adapter for the menu
	public class FavoriteLegislatorsAdapter extends CursorAdapter {

		public FavoriteLegislatorsAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((FavoriteLegislatorWrapper) view.getTag()).populateFrom(cursor, context);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = LayoutInflater.from(context).inflate(R.layout.favorite_legislator, null);
			FavoriteLegislatorWrapper wrapper = new FavoriteLegislatorWrapper(row);
			
			wrapper.populateFrom(cursor, context);
			row.setTag(wrapper);
			
			return row;
		}

		public class FavoriteLegislatorWrapper {
			private View row;

			private TextView name, position;
			private ImageView photo;
			
			public Legislator legislator;

			public FavoriteLegislatorWrapper(View row) {
				this.row = row;
			}

			void populateFrom(Cursor c, Context context) {
				legislator = Database.loadLegislator(c);
				
				getName().setText(legislator.titledName());
				String position = Legislator.partyName(legislator.party) + " from " 
					+ Utils.stateCodeToName(context, legislator.state);
				getPosition().setText(position);
				
				BitmapDrawable picture = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM, legislator.bioguide_id, context);
				
				if (picture != null)
					getPhoto().setImageDrawable(picture);
				else {
					getPhoto().setImageResource(R.drawable.loading_photo);

					Class<?> paramTypes[] = new Class<?>[] { String.class, FavoriteLegislatorWrapper.class };
					Object[] args = new Object[] { legislator.bioguide_id, this };
					try {
						context.getClass().getMethod("loadPhoto", paramTypes).invoke(context, args);
					} catch (Exception e) {
						Log.e(Utils.TAG, "The Context must implement LoadPhotoTask.LoadsPhoto interface!");
					}
				}
			}

			public void onLoadPhoto(Drawable photo, String bioguideId) {
				if (photo != null)
					getPhoto().setImageDrawable(photo);
				else
					getPhoto().setImageResource(R.drawable.no_photo_female);
			}
			
			private TextView getName() {
				return name == null ? name = (TextView) row.findViewById(R.id.name) : name;
			}
			
			private TextView getPosition() {
				return position == null ? position = (TextView) row.findViewById(R.id.position) : position;
			}
			
			private ImageView getPhoto() {
				return photo == null ? photo = (ImageView) row.findViewById(R.id.photo) : photo;
			}
		}
	}

	
	private class Holder {
		Map<String,LoadPhotoTask> loadPhotoTasks;
		Map<String, FavoriteLegislatorWrapper> favoritePeopleWrappers;
		boolean tracked;
		
		Holder(Map<String, LoadPhotoTask> loadPhotoTasks, Map<String, FavoriteLegislatorWrapper> favoritePeopleWrappers, boolean tracked) {
			this.loadPhotoTasks = loadPhotoTasks;
			this.favoritePeopleWrappers = favoritePeopleWrappers;
			this.tracked = tracked;
		}
	}
}