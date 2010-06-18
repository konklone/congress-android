package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.congress.models.Legislator;

public class FavLegislatorsAdapter extends CursorAdapter {
	private Context context;

	public FavLegislatorsAdapter(Context context, Cursor c) {
		super(context, c);
		this.context = context;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		FavLegislatorWrapper wrapper = (FavLegislatorWrapper) view.getTag();
		wrapper.populateFrom(cursor);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View row = inflater.inflate(R.layout.legislator_item, null);
		FavLegislatorWrapper wrapper = new FavLegislatorWrapper(row);
		row.setTag(wrapper);
		wrapper.populateFrom(cursor);
		return row;
	}

	public class FavLegislatorWrapper {
		private View row;
		private ImageView photo;
		private TextView name;
		private TextView position;
		private Legislator legislator;

		public FavLegislatorWrapper(View row) {
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
			legislator = Legislator.fromCursor(c);

			getName().setText(legislator.getOfficialName());
			getPosition().setText(legislator.getPosition(Utils.stateCodeToName(context, legislator.state)));

			BitmapDrawable picture = LegislatorImage.quickGetImage(LegislatorImage.PIC_MEDIUM,
					legislator.bioguide_id, context);
			if (picture != null)
				getPhoto().setImageDrawable(picture);
			else {
				getPhoto().setImageResource(R.drawable.loading_photo);

				Class<?> paramTypes[] = new Class<?>[] { String.class, FavLegislatorWrapper.class };
				Object[] args = new Object[] { legislator.bioguide_id, this };
				try {
					context.getClass().getMethod("loadPhoto", paramTypes).invoke(context, args);
				} catch (Exception e) {
					Log.e(this.getClass().getName(),
							"The Context must implement LoadPhotoTask.LoadsPhoto interface!");
				}
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