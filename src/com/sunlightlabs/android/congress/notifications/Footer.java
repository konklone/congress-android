package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;

public class Footer {
	public static final int DISABLED = -1;
	public static final int OFF = 0;
	public static final int ON = 1;

	public TextView text;
	public ImageView image;

	private int state;

	private Context context;
	private Resources resources;
	private ViewGroup footerView;
	private Database database;
	
	private Subscription subscription;
	private List<String> latestIds;
	
	
	public Footer(Context context, ViewGroup footerView) {
		onScreenLoad(context, footerView);
	}
	
	public void onScreenLoad(Context context, ViewGroup footerView) {
		this.context = context;
		this.resources = context.getResources();
		
		this.footerView = footerView;
		this.text = (TextView) footerView.findViewById(R.id.text);
		this.image = (ImageView) footerView.findViewById(R.id.image);
		// spinner
		
		database = new Database(context);
	}
	
	public static Footer from(Activity activity) {
		return new Footer(activity, (ViewGroup) activity.findViewById(R.id.footer));
	}
	
	public void init(Subscription subscription, List<?> objects) {
		this.subscription = subscription;
		
		setupControls();
		
		Subscriber subscriber;
		try {
			subscriber = subscription.getSubscriber();
			List<String> ids = new ArrayList<String>();
			if (objects != null) {
				int size = objects.size();
				for (int i=0; i<size; i++) {
					// can get rid of this null check when we switch to a pagination approach that doesn't use a null entry 
					Object obj = objects.get(i);
					if (obj != null)
						ids.add(subscriber.decodeId(obj));
				}
			}
			
			this.latestIds = ids;
		} catch(CongressException e) {
			Log.e(Utils.TAG, "Could not instantiate a Subscriber of class " + subscription.notificationClass, e);
		}
	}
	
	public void setupControls() {
		database.open();
		
		footerView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onTap();
			}
		});
		
		if (Utils.getBooleanPreference(context, NotificationSettings.KEY_NOTIFY_ENABLED, NotificationSettings.DEFAULT_NOTIFY_ENABLED)) {
			if (database.hasSubscription(subscription.id, subscription.notificationClass))
				setOn();
			else
				setOff();
		} else {
			if (firstTime())
				setFirstTime();
			else
				setDisabled();
		}
		
		footerView.setVisibility(View.VISIBLE);
		
		database.close();
	}
	

	private void onTap() {
		database.open();
		
		if (state == OFF) {
			long rows = database.addSubscription(subscription, latestIds);
			
			if (rows != -1) {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
					"Added notification in the db for subscription with " + rows + " new inserted IDs");
			} else {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
					"Error saving notifications, -1 returned from one or more insert calls");
			}
			
			setOn();
		}
		
		else if (state == ON) {
			long rows = database.removeSubscription(subscription.id, subscription.notificationClass); 
			setOff();

			Log.d(Utils.TAG, "Footer: Removed notification from the db for subscription " + subscription.id);
			Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
					"Removed notification from the db, " + rows + " deleted");
		}
		
		else if (state == DISABLED)
			context.startActivity(new Intent(context, NotificationSettings.class));
		
		database.close();
	}

	private void setOn() { 
		state = ON;
		
		text.setText(R.string.footer_on);
		text.setTextColor(resources.getColor(R.color.footer_on_text));
		image.setVisibility(View.VISIBLE);
		image.setImageResource(R.drawable.notifications_on);
		
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_on));
	}

	private void setOff() {
		state = OFF;
		
		text.setText(R.string.footer_off);
		text.setTextColor(resources.getColor(R.color.footer_off_text));
		image.setVisibility(View.VISIBLE);
		
		image.setImageResource(R.drawable.notifications_off);
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_off));
	}
	
	private void setDisabled() {
		state = DISABLED;
		
		text.setText(R.string.footer_disabled);
		text.setTextColor(resources.getColor(R.color.footer_disabled_text));
		
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_disabled));
	}
	
	private void setFirstTime() {
		state = DISABLED; // leave it at disabled for purposes of tapping
		
		text.setText(R.string.footer_first_time);
		text.setTextColor(resources.getColor(R.color.footer_first_time_text));
		
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_first_time));
	}
	
	// will turn false once the user has visited the notification settings (and seen the explanation dialog) for the first time
	private boolean firstTime() {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(NotificationSettings.KEY_FIRST_TIME_SETTINGS, NotificationSettings.DEFAULT_FIRST_TIME_SETTINGS);
	}
	
	private static class SubscribeTask extends AsyncTask<Void,Void,Integer> {
		private Footer footer;
		private Database database;
		
		private Subscription subscription;
		private List<String> latestIds;

		public SubscribeTask(Footer footer) {
			this.footer = footer;
			this.database = footer.database;
			this.subscription = footer.subscription;
			this.latestIds = footer.latestIds;
		}

		@Override
		public Integer doInBackground(Void... nothing) {
			database.open();
			int results = (int) database.addSubscription(subscription, latestIds);
			database.close();
			return results;
		}

		@Override
		public void onPostExecute(Integer rows) {
			if (rows != -1) {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
					"Added notification in the db for subscription with " + rows + " new inserted IDs");
			} else {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
					"Error saving notifications, -1 returned from one or more insert calls");
			}
		}
	}
}