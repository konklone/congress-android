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

	// in case we need more action when the footer is turned on/off
	public static interface OnFooterClickListener {
		public void onFooterClick(Footer footer, State state);
	}

	public enum State {
		ON, OFF;
	}

	private int textViewId;
	private int imageViewId;
	private FooterText textView;
	private FooterImage imageView;

	private OnFooterClickListener listener;
	private State state;

	private NotificationEntity entity;
	private Database database;
	private Context context;

	public Footer(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Footer);
		RuntimeException e = null;

		textViewId = a.getResourceId(R.styleable.Footer_textView, 0);
		if (textViewId == 0)
			e = new IllegalArgumentException(a.getPositionDescription()
					+ ": The textView attribute is required and must refer to a valid child.");

		imageViewId = a.getResourceId(R.styleable.Footer_imageView, 0);
		if (imageViewId == 0)
			e = new IllegalArgumentException(a.getPositionDescription()
					+ ": The imageView attribute is required and must refer to a valid child.");

		a.recycle();

		if (e != null)
			throw e;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		textView = (FooterText) findViewById(textViewId);
		if (textView == null) {
			String name = getResources().getResourceEntryName(textViewId);
			throw new RuntimeException("Your Footer must have a child View "
					+ "whose id attribute is 'R.id." + name + "'");
		}
		imageView = (FooterImage) findViewById(imageViewId);
		if (imageView == null) {
			String name = getResources().getResourceEntryName(imageViewId);
			throw new RuntimeException("Your Footer must have a child View "
					+ "whose id attribute is 'R.id." + name + "'");
		}

		setupControls();
	}

	private void setupControls() {
		// default state
		state = State.OFF;
	}

	private void setUIListener() {
		setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (entity != null && database != null)
					doFooterLogic();
				else
					// this footer has no entity attached; just update the UI
					doUpdateUI();
				
				// if there is a listener, call its callback method
				if (listener != null)
					listener.onFooterClick(Footer.this, state);
			}
		});
	}
	
	private void doUpdateUI() {
		if (state == State.OFF)
			setOn();
		else
			setOff();
	}

	private void doFooterLogic() {
		String id = entity.id;
		String nType = entity.notificationType;
		boolean ok = true;

		String status = database.getNotificationStatus(id, nType);

		// the current state is OFF; must turn notifications ON
		if (state == State.OFF) {

			// Case 1: there is no entry in the notifications table for this entity:
			// add a notification
			if (status == null) {
				ok = database.addNotification(entity) != -1;
				Log.d(Utils.TAG, "Footer: Added " + nType + " notifications for entity " + id + "->" + ok);
			}

			// Case 2: there is an entry in the notifications table for this entity:
			// update notification status
			else {
				ok = database.setNotificationStatus(id, nType, Database.NOTIFICATIONS_ON) != -1;
				Log.d(Utils.TAG, "Footer: Set " + nType + " notifications ON for entity " + id + "->" + ok);
			}
		}

		// the current state is ON; must turn notifications OFF
		// this means we set the notification status in the database to OFF for the current entity
		// it doesn't mean we stop the service; it can only be stopped from MainMenu footer
		else {
			// it means there is an entry in the notifications table
			ok = database.setNotificationStatus(id, nType, Database.NOTIFICATIONS_OFF) != -1;
			Log.d(Utils.TAG, "Footer: Set " + nType + " notifications OFF for entity " + id + "->" + ok);
		}
		
		// all database operations went smoothly; update footer UI
		if (ok)
			doUpdateUI();
	}

	private void setOn() {
		state = State.ON;
		textView.setOn();
		imageView.setOn();

		// start the notification service, if it's currently stopped
		if (!Utils.getBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED,
				Preferences.DEFAULT_NOTIFY_ENABLED)) {
			Utils.setBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, true);
			Utils.startNotificationsBroadcast(context);
		}
	}

	private void setOff() {
		state = State.OFF;
		textView.setOff();
		imageView.setOff();
	}

	public void init(NotificationEntity entity, Database database) {
		if(entity == null || database == null) {
			Log.d(Utils.TAG, "You must set the entity and the database before calling init() on the footer!");
			return;
		}
		this.entity = entity;
		this.database = database;
		setUIListener();

		// if the service is started, check the database to set the initial state of the UI
		if (Utils.getBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED,
				Preferences.DEFAULT_NOTIFY_ENABLED)
				&& Database.NOTIFICATIONS_ON.equals(database.getNotificationStatus(entity.id,
						entity.notificationType)))
			setOn();
		else
			setOff();
	}

	public void init() {
		setUIListener();

		if (Utils.getBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED,
				Preferences.DEFAULT_NOTIFY_ENABLED))
			setOn();
		else
			setOff();
	}

	public void setListener(OnFooterClickListener listener) {
		this.listener = listener;
	}
}
