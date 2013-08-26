package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.NotificationTabs;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;

public class Footer {
	public static final int ERROR = -2;
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
	
	// initialize the footer to a fragment, obtain a tracker from its activity's fragment pool
	// set it up with a subscription and a list of seen items
	public static void setup(Fragment fragment, Subscription subscription, List<?> objects) {
		new Footer(fragment).init(subscription, objects);
	}
	
	public Footer(Fragment fragment) {
		FragmentActivity activity = fragment.getActivity();
		this.context = activity;
		this.resources = activity.getResources();
		
		this.footerView = (ViewGroup) fragment.getView().findViewById(R.id.footer);
		this.text = (TextView) footerView.findViewById(R.id.text);
		this.image = (ImageView) footerView.findViewById(R.id.image);
		this.working = (ProgressBar) footerView.findViewById(R.id.working);
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
					// TODO: can get rid of this null check when we switch to a pagination approach that doesn't use a null entry 
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
	
	public void hide() {
		this.footerView.setVisibility(View.GONE);
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
				try {
					database.open();
					boolean on = database.hasSubscription(subscription.id, subscription.notificationClass);
					database.close();
				
					if (on)
						setOn();
					else
						setOff();
				} catch(SQLiteException e) {
					Log.e(Utils.TAG, "Error on initializing footer, giving up and letting the user know.", e);
					setError();
				}
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
			Analytics.subscribeNotification(context, subscription.notificationClass);
			new SubscribeTask(this).execute();
		}
		
		else if (state == ON) {
			setWorking();
			Analytics.unsubscribeNotification(context, subscription.notificationClass);
			new UnsubscribeTask(this).execute();
		}
		
		else if (state == DISABLED)
			context.startActivity(new Intent(context, NotificationTabs.class));
	}

	private void setOn() { 
		state = ON;
		
		text.setText(R.string.footer_on);
		text.setTextColor(resources.getColor(R.color.text));
		image.setVisibility(View.VISIBLE);
		image.setImageResource(R.drawable.circle_on);
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundColor(resources.getColor(R.color.background_dark));
	}

	private void setOff() {
		state = OFF;
		
		text.setText(R.string.footer_off);
		text.setTextColor(resources.getColor(R.color.text_grey));
		image.setVisibility(View.VISIBLE);
		image.setImageResource(R.drawable.circle_off);
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundColor(resources.getColor(R.color.background_dark));
	}
	
	private void setWorking() {
		state = WORKING;
		
		text.setText(R.string.footer_working);
		image.setVisibility(View.GONE);
		working.setVisibility(View.VISIBLE);
		
		text.setTextColor(resources.getColor(R.color.text_grey));
		footerView.setBackgroundColor(resources.getColor(R.color.background_dark));
	}
	
	private void setDisabled() {
		state = DISABLED;
		
		text.setText(R.string.footer_disabled);
		text.setTextColor(resources.getColor(R.color.text_grey));
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundColor(resources.getColor(R.color.background_grey));
	}
	
	private void setFirstTime() {
		state = DISABLED; // leave it at disabled for purposes of tapping
		
		text.setText(R.string.footer_first_time);
		text.setTextColor(resources.getColor(R.color.text));
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundColor(resources.getColor(R.color.background_grey));
	}
	
	// used when there's a database error on initialization and there's not much else to do
	private void setError() {
		state = ERROR;
		
		text.setText(R.string.footer_error);
		text.setTextColor(resources.getColor(R.color.text_grey));
		image.setVisibility(View.GONE);
		working.setVisibility(View.GONE);
		
		footerView.setBackgroundColor(resources.getColor(R.color.background_dark));
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
				Log.e(Utils.TAG, "Database exception on subscribe tap.", e);
				database.close();
				return -1;
			}
		}

		@Override
		public void onPostExecute(Integer rows) {
			if (rows == -1) {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
				"Error saving notifications, -1 returned from one or more insert calls");
				Utils.alert(footer.context, R.string.footer_busy);
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
				Log.e(Utils.TAG, "Database exception on unsubscribe tap.", e);
				database.close();
				return -1;
			}
		}

		@Override
		public void onPostExecute(Integer rows) {
			if (rows == -1) {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
				"Error saving notifications, -1 returned from a delete call");
				Utils.alert(footer.context, R.string.footer_busy);
				setOn();
			} else {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
						"Removed notification from the db, " + rows + " rows deleted");
				
				setOff();
			}
		}
	}
}