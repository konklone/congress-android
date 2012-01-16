package com.sunlightlabs.android.congress.fragments;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class MenuLegislatorsFragment extends ListFragment implements LoadPhotoTask.LoadsPhoto {
	private Map<String, LoadPhotoTask> loadPhotoTasks = new HashMap<String, LoadPhotoTask>();
	private Map<String, ImageView> photoViews = new HashMap<String, ImageView>();
	
	private Database database;
	private Cursor cursor;
	
	public static MenuLegislatorsFragment newInstance() {
		MenuLegislatorsFragment frag = new MenuLegislatorsFragment();
		frag.setRetainInstance(true);
		return frag;
	}
	
	public MenuLegislatorsFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setupDatabase();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.menu_legislators, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
	}
	
	public void setupDatabase() {
		database = new Database(getActivity());
		database.open();
		
		cursor = database.getLegislators();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (cursor != null)
			cursor.requery();
		setupControls();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	private void setupControls() {
		if (cursor != null && cursor.getCount() > 0) {
			setListAdapter(new FavoriteLegislatorsAdapter(this, cursor));
			getView().findViewById(R.id.menu_legislators_no_favorites).setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
		} else {
			getView().findViewById(R.id.menu_legislators_no_favorites).setVisibility(View.VISIBLE);
			getListView().setVisibility(View.GONE);
		}
	}
	
//	private void searchByZip(String zipCode) {
//		startActivity(new Intent(this, LegislatorListFragment.class)
//			.putExtra("type", LegislatorListFragment.SEARCH_ZIP)
//			.putExtra("zip_code", zipCode));
//	}
//
//	private void searchByLocation() {
//		startActivity(new Intent(this, LegislatorListFragment.class)
//			.putExtra("type", LegislatorListFragment.SEARCH_LOCATION));
//	}
//
//	private void searchByLastName(String lastName) {
//		startActivity(new Intent(this, LegislatorListFragment.class)
//			.putExtra("type", LegislatorListFragment.SEARCH_LASTNAME)
//			.putExtra("last_name", lastName));
//	}
//
//	private void searchByState(String state) {
//		startActivity(new Intent(this, LegislatorListFragment.class)
//			.putExtra("type", LegislatorListFragment.SEARCH_STATE)
//			.putExtra("state", state));
//	}
	
	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		startActivity(Utils.legislatorLoadIntent(((Legislator) view.getTag()).id));
	}
	
	
//				String zipCode = data.getExtras().getString("response").trim();
//				if (!zipCode.equals("")) {
//					Utils.setStringPreference(this, "search_zip", zipCode);
//					searchByZip(zipCode);
//				}
			
//				String lastName = data.getExtras().getString("response").trim();
//				if (!lastName.equals("")) {
//					Utils.setStringPreference(this, "search_lastname", lastName);
//					searchByLastName(lastName);
//				}
//			
//				String state = data.getExtras().getString("response").trim();
//				if (!state.equals("")) {
//					String code = Utils.stateNameToCode(this, state);
//					if (code != null) {
//						Utils.setStringPreference(this, "search_state", state); // store the name, not the code
//						searchByState(code);
//					}
//				}
			
	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id))
			loadPhotoTasks.put(bioguide_id, 
					(LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_MEDIUM, bioguide_id).execute(bioguide_id));
	}

	@Override
	public Context getContext() {
		return getActivity();
	}

	@Override
	public void onLoadPhoto(Drawable photo, Object tag) {
		if (!isAdded())
			return;
		
		String bioguide_id = (String) tag;
		loadPhotoTasks.remove(bioguide_id);
		ImageView photoView = photoViews.get(bioguide_id);
		if (photoView != null) {
			if (photo != null)
				photoView.setImageDrawable(photo);
			else
				photoView.setImageResource(R.drawable.no_photo_female);
		}
	}
	
	public class FavoriteLegislatorsAdapter extends CursorAdapter {
		MenuLegislatorsFragment fragment;
		
		public FavoriteLegislatorsAdapter(MenuLegislatorsFragment fragment, Cursor cursor) {
			super(fragment.getActivity(), cursor);
			this.fragment = fragment;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Legislator legislator = Database.loadLegislator(cursor);
			
			TextView name = (TextView) view.findViewById(R.id.name);
			name.setText(legislator.titledName());
			
			BitmapDrawable picture = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM, legislator.bioguide_id, context);
			
			ImageView photo = (ImageView) view.findViewById(R.id.photo);
			
			if (picture != null)
				photo.setImageDrawable(picture);
			else {
				photo.setImageResource(R.drawable.loading_photo);
				photoViews.put(legislator.bioguide_id, photo);
				fragment.loadPhoto(legislator.bioguide_id);
			}
			
			view.setTag(legislator);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(R.layout.favorite_legislator, null);
			bindView(view, context, cursor);
			return view;
		}
	}
}