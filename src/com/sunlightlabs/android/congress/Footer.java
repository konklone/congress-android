package com.sunlightlabs.android.congress;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class Footer extends RelativeLayout {
	private final static String TAG = "CONGRESS";

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

	private boolean hasEntity = false;
	private String entityId, entityType, entityName, notificationType, notificationData;
	private Database database;

	public Footer(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Footer);
		RuntimeException e = null;

		textViewId = a.getResourceId(R.styleable.Footer_textView, 0);
		if (textViewId == 0)
			e = new IllegalArgumentException(a.getPositionDescription()
					+ ": The textView attribute is required and must refer to a valid child.");
		
		imageViewId = a.getResourceId(R.styleable.Footer_imageView, 0);
		if(imageViewId == 0)
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
		state = State.OFF;

		setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				boolean ok = true;
				
				if (hasEntity) {
					String status = database.getNotificationStatus(entityId, notificationType);
					Log.d(TAG, notificationType + " notifications for entity " + entityId + " are " + status);
					
					// must turn notifications ON
					if(state == State.OFF) {
								
						// there is no entry in the notifications table for this entity
						if (status == null) {
							ok = database.addNotification(entityId, entityType, entityName,
									notificationType, notificationData) != -1;
							Log.d(TAG, "Adding " + notificationType + " notifications for entity "
									+ entityId + " result is: " + ok);
						}
						
						// there is an entry in the notifications table for this entity
						else {
							ok = database.setNotificationStatus(entityId, notificationType, Database.NOTIFICATIONS_ON) != -1;
							Log.d(TAG, "Setting " + notificationType
									+ " notifications ON for entity " + entityId + " result is: "
									+ ok);
						}
					}

					// must turn notifications OFF
					else {
						ok = database.setNotificationStatus(entityId, notificationType, Database.NOTIFICATIONS_OFF) != -1;
						Log.d(TAG, "Setting " + notificationType + " notifications OFF for entity "
								+ entityId + " result is: " + ok);
					}
				}

				if(ok) 
					if (state == State.OFF)
						setOn();
					else
						setOff();

				if (listener != null)
					listener.onFooterClick(Footer.this, state);
			}
		});
	}

	public void setOn() {
		state = State.ON;
		textView.setOn();
		imageView.setOn();
	}

	public void setOff() {
		state = State.OFF;
		textView.setOff();
		imageView.setOff();
	}

	public void setListener(OnFooterClickListener listener) {
		this.listener = listener;
	}

	public State getState() {
		return state;
	}

	public FooterText getTextView() {
		return textView;
	}

	public FooterImage getImageView() {
		return imageView;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public String getNotificationData() {
		return notificationData;
	}

	public void setNotificationData(String notificationData) {
		this.notificationData = notificationData;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public void setHasEntity(boolean hasEntity) {
		this.hasEntity = hasEntity;
	}
}
