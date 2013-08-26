package com.sunlightlabs.android.congress;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Database;
import com.sunlightlabs.android.congress.utils.Utils;

public class NotificationSubscriptions extends ListActivity {

	private Database database;
	private Cursor cursor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		
		setupDatabase();
		setupControls();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	public void setupDatabase() {
		database = new Database(this);
		database.open();
		
		cursor = database.getSubscriptions();
		startManagingCursor(cursor);
	}
	
	public void setupControls() {
		if (cursor.getCount() == 0)
			Utils.showEmpty(this, R.string.notifications_empty);
		setListAdapter(new SubscriptionAdapter(this, cursor));
	}
	
	@Override
	protected void onListItemClick(ListView l, View view, int position, long id) {
		Object tag = view.getTag();
		if (tag != null)
			startActivity((Intent) tag);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (cursor != null)
			cursor.requery();
		setupControls();
	}
	
	class SubscriptionAdapter extends CursorAdapter {
		
		public SubscriptionAdapter(Context context, Cursor cursor) {
			super(context, cursor);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView text = (TextView) view.findViewById(R.id.text);
			
			Subscription subscription = Database.loadSubscription(cursor);
			Subscriber subscriber;
			try {
				subscriber = subscription.getSubscriber();
				subscriber.context = context;
				
				text.setText(subscriber.subscriptionName(subscription));
				view.setTag(subscriber.notificationIntent(subscription));
				view.setEnabled(true);
			} catch (Exception e) {
				Log.e(Utils.TAG, "Could not instantiate a Subscriber of class " + subscription.notificationClass, e);
				
				text.setText(R.string.notification_not_found);
				view.setTag(null);
				view.setEnabled(false);
			}
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(R.layout.subscription_item, null);
			bindView(view, context, cursor);
			return view;
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Analytics.start(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.stop(this);
	}
}