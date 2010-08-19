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
		public void onFooterClick(Footer footer, int state);
	}

	public static final int ON = 1;
	public static final int OFF = 0;

	private int textViewId;
	private int imageViewId;
	private FooterText textView;
	private FooterImage imageView;

	private OnFooterClickListener listener;
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
		database = new Database(context);
		database.open();

		setUIListener();

		// if the service is started, check the database to set the initial state of the UI
		if (Utils.getBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED,
				Preferences.DEFAULT_NOTIFY_ENABLED)
				&& database.hasNotification(entity.id, entity.notificationClass))
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

	private void setUIListener() {
		setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (entity != null)
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
		if (state == OFF)
			setOn();
		else
			setOff();
	}

	private void doFooterLogic() {
		String id = entity.id;
		String cls = entity.notificationClass;
		boolean dbOk = true;

		if (state == OFF) // current state is OFF; must turn notifications ON
			dbOk = database.addNotification(entity) != -1;
		
		else // current state is ON; must turn notifications OFF
			dbOk = database.removeNotification(id, cls) != -1;

		// all database operations went smoothly; update footer UI
		if (dbOk)
			doUpdateUI();
		else
			Log.w(Utils.TAG, "doFooterLogic(): database operation not successful!");
	}

	private void setOn() {
		state = ON;
		textView.setOn();
		imageView.setOn();

		// start the notification service, if it's currently stopped
		if (!Utils.getBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, Preferences.DEFAULT_NOTIFY_ENABLED)) {
			Utils.setBooleanPreference(context, Preferences.KEY_NOTIFY_ENABLED, true);
			Utils.startNotificationsBroadcast(context);
		}
	}

	private void setOff() {
		state = OFF;
		textView.setOff();
		imageView.setOff();
	}

	public void setListener(OnFooterClickListener listener) {
		this.listener = listener;
	}

	// must be called to avoid database leaks
	public void onDestroy() {
		database.close();
	}
}
