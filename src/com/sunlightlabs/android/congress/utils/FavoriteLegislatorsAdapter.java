package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.MainMenu;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.LegislatorService;

public class FavoriteLegislatorsAdapter extends CursorAdapter {
	private MainMenu context;

	public FavoriteLegislatorsAdapter(MainMenu context, Cursor c) {
		super(context, c);
		this.context = context;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		FavoriteLegislatorWrapper wrapper = (FavoriteLegislatorWrapper) view.getTag();
		wrapper.populateFrom(cursor);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View row = inflater.inflate(R.layout.legislator_item, null);
		FavoriteLegislatorWrapper wrapper = new FavoriteLegislatorWrapper(row);
		row.setTag(wrapper);
		wrapper.populateFrom(cursor);
		return row;
	}

	public class FavoriteLegislatorWrapper {
		private View row;
		private ImageView photo;
		private TextView name;
		private TextView position;
		private Legislator legislator;

		public FavoriteLegislatorWrapper(View row) {
			this.row = row;
		}

		public ImageView getPhoto() {
			return photo == null ? photo = (ImageView) row.findViewById(R.id.photo) : photo;
		}

		public TextView getName() {
			return name == null ? name = (TextView) row.findViewById(R.id.name) : name;
		}

		public TextView getPosition() {
			return position == null ? position = (TextView) row.findViewById(R.id.position) : position;
		}

		public Legislator getLegislator() {
			return legislator;
		}

		void populateFrom(Cursor c) {
			legislator = LegislatorService.fromCursor(c);

			getName().setText(legislator.getOfficialName());
			getPosition().setText(
					legislator.getPosition(Utils.stateCodeToName(context, legislator.state)));

			BitmapDrawable picture = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM,
					legislator.bioguide_id, context);
			if (picture != null)
				getPhoto().setImageDrawable(picture);
			else {
				getPhoto().setImageResource(R.drawable.loading_photo);
				context.loadPhoto(legislator.bioguide_id, this);
			}
		}

		public void onLoadPhoto(Drawable photo, String bioguideId) {
			if (photo != null)
				getPhoto().setImageDrawable(photo);
			else
				getPhoto().setImageResource(R.drawable.no_photo_female);
		}
	}

}