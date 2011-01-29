package com.sunlightlabs.android.congress.notifications;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;

public class Footer extends RelativeLayout {
	public static final int DISABLED = -1;
	public static final int OFF = 0;
	public static final int ON = 1;

	public TextView text;
	public ImageView image;

	private int state;

	private Database database;
	private Context context;
	private Resources resources;
	
	private Subscription subscription;
	private List<String> latestIds;

	public Footer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		this.resources = context.getResources();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		text = (TextView) findViewById(R.id.text);
		image = (ImageView) findViewById(R.id.image);
	}
	
	public void init(Subscription subscription, List<?> objects) {
		this.subscription = subscription;
		database = new Database(context);

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
		
		setOnClickListener(new View.OnClickListener() {
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
		
		setVisibility(View.VISIBLE);
		database.close();
	}
	

	private void onTap() {
		database.open();
		
		if (state == OFF) {
			long rows = database.addSubscription(subscription, latestIds); 
			if (rows != -1) {
				setOn();
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
					"Added notification in the db for subscription with " + rows + " new inserted IDs");
			} else {
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " +
					"Error saving notifications, -1 returned from one or more insert calls");
			}
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
		this.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_on));
	}

	private void setOff() {
		state = OFF;
		text.setText(R.string.footer_off);
		text.setTextColor(resources.getColor(R.color.footer_off_text));
		image.setVisibility(View.VISIBLE);
		image.setImageResource(R.drawable.notifications_off);
		this.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_off));
	}
	
	private void setDisabled() {
		state = DISABLED;
		text.setText(R.string.footer_disabled);
		text.setTextColor(resources.getColor(R.color.footer_disabled_text));
		this.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_disabled));
	}
	
	private void setFirstTime() {
		state = DISABLED; // leave it at disabled for purposes of tapping
		text.setText(R.string.footer_first_time);
		text.setTextColor(resources.getColor(R.color.footer_first_time_text));
		this.setBackgroundDrawable(resources.getDrawable(R.drawable.footer_first_time));
	}
	
	// will turn false once the user has visited the notification settings (and seen the explanation dialog) for the first time
	private boolean firstTime() {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(NotificationSettings.KEY_FIRST_TIME_SETTINGS, NotificationSettings.DEFAULT_FIRST_TIME_SETTINGS);
	}
}