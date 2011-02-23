package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.NotificationTabs;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;

public class Footer {
	public static final int DISABLED = -1;
	public static final int OFF = 0;
	public static final int ON = 1;
	public static final int WORKING = 2;

	public TextView text;
	public ImageView image;
	public ProgressBar working;

	private int state;

	private Activity context;
	private Resources resources;
	private ViewGroup footerView;
		
	private Subscription subscription;
	private List<String> latestIds;
	
	GoogleAnalyticsTracker tracker;
	
	public Footer(Activity context, GoogleAnalyticsTracker tracker) {
		onScreenLoad(context, tracker);
	}
	
	public void onScreenLoad(Activity context, GoogleAnalyticsTracker tracker) {
		this.context = context;
		this.resources = context.getResources();
		
		this.footerView = (ViewGroup) context.findViewById(R.id.footer);
		this.text = (TextView) footerView.findViewById(R.id.text);
		this.image = (ImageView) footerView.findViewById(R.id.image);
		this.working = (ProgressBar) footerView.findViewById(R.id.working);
		
		this.tracker = tracker;
	}
	
	public static Footer from(Activity activity, GoogleAnalyticsTracker tracker) {
		return new Footer(activity, tracker);
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
		
		footerView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onTap();
			}
		});
		
		// our logic in the footer works out so we should never have concurrent database access attempts, but
		// if that were not the case, we would need to surround calls to the database with a synchronized(this) {} block
		
		if (Utils.getBooleanPreference(context, NotificationSettings.KEY_NOTIFY_ENABLED, NotificationSettings.DEFAULT_NOTIFY_ENABLED)) {
			if (state == WORKING)
				setWorking();
			else {
				Database database = new Database(context);
				database.open();
				boolean on = database.hasSubscription(subscription.id, subscription.notificationClass);
				database.close();
				
				if (on)
					setOn();
				else
					setOff();
			}
		} else {
			if (firstTime())
				setFirstTime();
			else
				setDisabled();
		}
		
		footerView.setVisibility(View.VISIBLE);
	}
	

	private void onTap() {
		if (state == OFF) {
			setWorking();
			Analytics.subscribeNotification(context, tracker, subscription.notificationClass);
			new SubscribeTask(this).execute();
		}
		
		else if (state == ON) {
			setWorking();
			Analytics.unsubscribeNotification(context, tracker, subscription.notificationClass);
			new UnsubscribeTask(this).execute();
		}
		
		else if (state == DISABLED)
			context.startActivity(new Intent(context, NotificationTabs.class));
	}

	private void setOn() { 
		state = ON;
		
		text.setText(R.string.footer_on);
		text.setTextColor(resources.getColor(R.color.footer_on_text));
		image.setVisibility(View.VISIBLE);
		image.setImageResource(R.drawable.notifications_on);
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_on));
	}

	private void setOff() {
		state = OFF;
		
		text.setText(R.string.footer_off);
		text.setTextColor(resources.getColor(R.color.footer_off_text));
		image.setVisibility(View.VISIBLE);
		image.setImageResource(R.drawable.notifications_off);
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_off));
	}
	
	private void setWorking() {
		state = WORKING;
		
		text.setText(R.string.footer_working);
		image.setVisibility(View.GONE);
		working.setVisibility(View.VISIBLE);
		
		text.setTextColor(resources.getColor(R.color.footer_off_text));
		footerView.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_off));
	}
	
	private void setDisabled() {
		state = DISABLED;
		
		text.setText(R.string.footer_disabled);
		text.setTextColor(resources.getColor(R.color.footer_disabled_text));
		working.setVisibility(View.GONE);
		
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
	
	private class SubscribeTask extends AsyncTask<Void,Void,Integer> {
		private Footer footer;
		
		private Subscription subscription;
		private List<String> latestIds;

		public SubscribeTask(Footer footer) {
			this.footer = footer;
			this.subscription = footer.subscription;
			this.latestIds = footer.latestIds;
		}

		@Override
		public Integer doInBackground(Void... nothing) {
			Database database = new Database(footer.context);
			database.open();
			
			try {
				database.addSubscription(subscription);
				int results = (int) database.addSeenIds(subscription, latestIds);
				
				database.close();
				return results;
			} 
			// most likely a locked database
			catch (SQLiteException e) {
				database.close();
				return -1;
			}
		}

		@Override
		public void onPostExecute(Integer rows) {
			if (rows == -1) {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
				"Error saving notifications, -1 returned from one or more insert calls");
				Utils.alert(footer.context, R.string.footer_error);
				setOff();
			} else {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
					"Added notification in the db for subscription with " + rows + " new inserted IDs");
				
				setOn();
			}
		}
	}
	
	private class UnsubscribeTask extends AsyncTask<Void,Void,Integer> {
		private Footer footer;

		private Subscription subscription;

		public UnsubscribeTask(Footer footer) {
			this.footer = footer;
			this.subscription = footer.subscription;
		}

		@Override
		public Integer doInBackground(Void... nothing) {
			Database database = new Database(footer.context);
			database.open();
			try {
				int results = (int) database.removeSubscription(subscription.id, subscription.notificationClass);
				database.close();
				return results;
			}
			// most likely a locked database
			catch (SQLiteException e) {
				database.close();
				return -1;
			}
		}

		@Override
		public void onPostExecute(Integer rows) {
			if (rows == -1) {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
				"Error saving notifications, -1 returned from a delete call");
				Utils.alert(footer.context, R.string.footer_error);
				setOn();
			} else {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
						"Removed notification from the db, " + rows + " deleted");
				
				setOff();
			}
		}
	}
}