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

	public FavLegislatorsAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		((FavLegislatorWrapper) view.getTag()).populateFrom(cursor, context);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View row = LayoutInflater.from(context).inflate(R.layout.favorite_legislator, null);
		FavLegislatorWrapper wrapper = new FavLegislatorWrapper(row);
		
		wrapper.populateFrom(cursor, context);
		row.setTag(wrapper);
		
		return row;
	}

	public class FavLegislatorWrapper {
		private View row;

		private TextView name, position;
		private ImageView photo;
		
		public Legislator legislator;

		public FavLegislatorWrapper(View row) {
			this.row = row;
		}

		void populateFrom(Cursor c, Context context) {
			legislator = Legislator.fromCursor(c);
			
			getName().setText(legislator.titledName());
			String position = Legislator.partyName(legislator.party) + " from " 
				+ Utils.stateCodeToName(context, legislator.state);
			getPosition().setText(position);
			
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