package com.sunlightlabs.android.congress.notifications;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sunlightlabs.android.congress.Database;
import com.sunlightlabs.android.congress.NotificationSettings;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;

public class Footer extends RelativeLayout {
	public static final int DISABLED = -1;
	public static final int OFF = 0;
	public static final int ON = 1;

	public TextView text;

	private int state;

	private Subscription subscription;
	private Database database;
	private Context context;

	public Footer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		text = (TextView) findViewById(R.id.text);
	}

	public void init(Subscription subscription) {
		this.subscription = subscription;
		database = new Database(context);
		database.open();

		setupControls();
	}
	
	public void setupControls() {
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
		} else
			setDisabled();
		
		setVisibility(View.VISIBLE);
	}
	

	private void onTap() {
		if (state == OFF) { 
			if (database.addSubscription(subscription) != -1) {
				setOn();
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
					"Added notification in the db for subscription with lastSeenId: " + ((subscription.lastSeenId == null) ? "null" : subscription.lastSeenId));

				// the service is stopped but there are notifications in the database => start the service
				if (!Utils.getBooleanPreference(context,
						NotificationSettings.KEY_NOTIFY_ENABLED,
						NotificationSettings.DEFAULT_NOTIFY_ENABLED)) {
					Utils.setBooleanPreference(context, NotificationSettings.KEY_NOTIFY_ENABLED, true);
					Utils.startNotificationsBroadcast(context);
				}
			}
		}
		
		else if (state == ON) { 
			if (database.removeSubscription(subscription.id, subscription.notificationClass) != 0) {
				setOff();
				Log.d(Utils.TAG, "Footer: Removed notification from the db for subscription " + subscription.id);
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
						"Removed notification from the db");
			}
		}
		
		else if (state == DISABLED)
			context.startActivity(new Intent(context, NotificationSettings.class));
	}

	private void setOn() {
		state = ON;
		text.setText(R.string.footer_on);
	}

	private void setOff() {
		state = OFF;
		text.setText(R.string.footer_off);
	}
	
	private void setDisabled() {
		state = DISABLED;
		text.setText(R.string.footer_disabled);
	}

	public void onDestroy() {
		database.close();
	}
}