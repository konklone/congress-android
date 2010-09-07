package com.sunlightlabs.android.congress.notifications;

import android.content.Context;
import android.content.res.TypedArray;
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

public class Footer extends RelativeLayout {
	public static final int ON = 1;
	public static final int OFF = 0;

	private int textViewId;
	private int imageViewId;
	public FooterText textView;
	public FooterImage imageView;

	private int state;

	private Subscription subscription;
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
		
		state = OFF;
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
				doFooterLogic();
			}
		});
		
		textView.textOn = Utils.capitalize(String.format(context.getString(R.string.footer_on), "these items"));
		textView.textOff = Utils.capitalize(String.format(context.getString(R.string.footer_off), "these items"));
		
		if (Utils.getBooleanPreference(context, NotificationSettings.KEY_NOTIFY_ENABLED, NotificationSettings.DEFAULT_NOTIFY_ENABLED)
				&& database.hasSubscription(subscription.id, subscription.notificationClass))
			setOn();
		else
			setOff();
		
		setVisibility(View.VISIBLE);
	}
	

	private void doFooterLogic() {
		String id = subscription.id;
		String cls = subscription.notificationClass;

		if (state == OFF) { // current state is OFF; must turn notifications ON
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
		else { // current state is ON; must turn notifications OFF
			if (database.removeSubscription(id, cls) != 0) {
				setOff();
				Log.d(Utils.TAG, "Footer: Removed notification from the db for subscription " + id);
				Log.i(Utils.TAG, "Footer: [" + subscription.notificationClass + "][" + subscription.id + "] " + 
						"Removed notification from the db");
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

	// must be called to avoid database leaks
	public void onDestroy() {
		database.close();
	}

	public static class FooterImage extends ImageView {
		public int srcOn, srcOff;

		public FooterImage(Context context, AttributeSet attrs) {
			super(context, attrs);

			TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FooterImage);
			srcOn = array.getResourceId(R.styleable.FooterImage_srcOn, 0);
			srcOff = array.getResourceId(R.styleable.FooterImage_srcOff, 0);
			array.recycle();
		}

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();

			setOff();
		}

		public void setOn() {
			this.setImageResource(srcOn);
		}

		public void setOff() {
			this.setImageResource(srcOff);
		}
	}

	public static class FooterText extends TextView {
		public String textOn, textOff;

		public FooterText(Context context, AttributeSet attrs) {
			super(context, attrs);

			TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FooterText);
			textOn = array.getString(R.styleable.FooterText_textOn);
			textOff = array.getString(R.styleable.FooterText_textOff);
			array.recycle();
		}

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();

			setOff();
		}

		public void setOn() {
			setText(textOn);
		}

		public void setOff() {
			setText(textOff);
		}
	}
}