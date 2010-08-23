package com.sunlightlabs.android.congress;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.sunlightlabs.android.congress.notifications.NotificationEntity;
import com.sunlightlabs.android.congress.utils.Utils;

public class Footer extends RelativeLayout {
	public static final int ON = 1;
	public static final int OFF = 0;

	private int textViewId;
	private int imageViewId;
	private FooterText textView;
	private FooterImage imageView;

	private int state;

	private NotificationEntity entity;
	private Database database;
	private Context context;

	public Footer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Footer);
		textViewId = a.getResourceId(R.styleable.Footer_textView, 0);
		imageViewId = a.getResourceId(R.styleable.Footer_imageView, 0);
		a.recycle();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		textView = (FooterText) findViewById(textViewId);
		imageView = (FooterImage) findViewById(imageViewId);
		setupControls();
	}

	private void setupControls() {
		// default state
		state = OFF;
	}
	
	public void init(NotificationEntity entity) {
		this.entity = entity;
		init();
	}

	public void init() {
		database = new Database(context);
		database.open();

		setUIListener();
		doInitUI();
	}

	private void setUIListener() {
		setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (entity != null) // tab footer
					doFooterLogic();
				else  // MainMenu footer
					doUpdateUI();
			}
		});
	}
	
	public void doInitUI() {
		if (entity != null) { // tab footer
			textView.setTextOn(Utils.footerText(context.getString(R.string.footer_on), entity.notificationName()));
			textView.setTextOff(Utils.footerText(context.getString(R.string.footer_off), entity.notificationName()));
			
			if (Utils.getBooleanPreference(context,
					Preferences.KEY_NOTIFY_ENABLED,
					Preferences.DEFAULT_NOTIFY_ENABLED)
					&& database.hasNotification(entity.id, entity.notificationClass)) {
				setOn();
			}
			else
				setOff();
		} // MainMenu footer
		else {
			if(database.hasNotifications()) {
				setVisibility(View.VISIBLE);
				if (Utils.getBooleanPreference(context,
						Preferences.KEY_NOTIFY_ENABLED,
						Preferences.DEFAULT_NOTIFY_ENABLED))
					setOn();
				else
					setOff();
			}
			else {
				if (Utils.getBooleanPreference(context,
						Preferences.KEY_NOTIFY_ENABLED, 
						Preferences.DEFAULT_NOTIFY_ENABLED)) {
					Utils.setBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, false);
					Utils.stopNotificationsBroadcast(context);
				}
				setVisibility(View.GONE);
			}
		}
	}
	
	private void doUpdateUI() {
		// turn off all notifications at once
		if (state == Footer.OFF) {
			setOn();
			Utils.setBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, true);
			Utils.startNotificationsBroadcast(context);
		} else {
			setOff();
			Utils.setBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, false);
			Utils.stopNotificationsBroadcast(context);
		}
	}

	private void doFooterLogic() {
		String id = entity.id;
		String cls = entity.notificationClass;

		if (state == OFF) { // current state is OFF; must turn notifications ON
			if (database.addNotification(entity) != -1) {
				setOn();
				Log.d(Utils.TAG, "Added notification in the db for entity " + id);

				// the service is stopped but there are notifications in the database => start the service
				if (!Utils.getBooleanPreference(context,
						Preferences.KEY_NOTIFY_ENABLED,
						Preferences.DEFAULT_NOTIFY_ENABLED)) {
					Utils.setBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, true);
					Utils.startNotificationsBroadcast(context);
				}
			}
		}
		else { // current state is ON; must turn notifications OFF
			if (database.removeNotification(id, cls) != 0) {
				setOff();
				Log.d(Utils.TAG, "Removed notification from the db for entity " + id);
			}
		}
	}

	private void setOn() {
		state = ON;
		textView.setOn();
		imageView.setOn();
	}

	private void setOff() {
		state = OFF;
		textView.setOff();
		imageView.setOff();
	}

	public FooterText getTextView() {
		return textView;
	}

	public FooterImage getImageView() {
		return imageView;
	}

	// must be called to avoid database leaks
	public void onDestroy() {
		database.close();
	}
}
