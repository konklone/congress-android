package com.sunlightlabs.android.congress.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.MenuLegislators;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

import java.util.HashMap;
import java.util.Map;

public class MenuLegislatorsFragment extends ListFragment implements LoadPhotoTask.LoadsPhoto {
	private Map<String, LoadPhotoTask> loadPhotoTasks = new HashMap<>();
	private Map<String, ImageView> photoViews = new HashMap<>();

	private Database database;
	private Cursor cursor;

	public static MenuLegislators.StatesFragment newInstance() {
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

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		startActivity(Utils.legislatorIntent(((Legislator) view.getTag()).bioguide_id));
	}

	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id))
			loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this,
					LegislatorImage.PIC_LARGE, bioguide_id).execute(bioguide_id));
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
				photoView.setImageResource(R.drawable.person);
		}
	}

	public class FavoriteLegislatorsAdapter extends CursorAdapter {
		MenuLegislatorsFragment fragment;
		LayoutInflater inflater;

		public FavoriteLegislatorsAdapter(MenuLegislatorsFragment fragment, Cursor cursor) {
			super(fragment.getActivity(), cursor);
			this.fragment = fragment;
			this.inflater = LayoutInflater.from(fragment.getActivity());
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Legislator legislator = Database.loadLegislator(cursor);

			TextView name = view.findViewById(R.id.name);
			name.setText(nameFor(legislator));
			TextView position = view.findViewById(R.id.position);
			position.setText(positionFor(legislator));

			ImageView photo = view.findViewById(R.id.photo);
			LegislatorImage.setImageView(legislator.bioguide_id, LegislatorImage.PIC_LARGE, context, photo);

			view.setTag(legislator);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = inflater.inflate(R.layout.legislator_item, null);
			bindView(view, context, cursor);
			return view;
		}

		public String nameFor(Legislator legislator) {
			return legislator.last_name + ", " + legislator.first_name;
		}

		public String positionFor(Legislator legislator) {
			String stateName = Utils.stateCodeToName(fragment.getActivity(), legislator.state);
			String district;
			if (legislator.title.equals("Sen"))
				district = "Senator";
			else
				district = "District " + legislator.district;
			return legislator.party + " - " + stateName + " - " + district; 
		}
	}
}