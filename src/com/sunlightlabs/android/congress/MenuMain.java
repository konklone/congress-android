package com.sunlightlabs.android.congress;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.notifications.NotificationService;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;

public class MenuMain extends FragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu_main);

		Analytics.track(this, "/");
		
		setupControls();
		
		if (firstTime()) {
			newVersion(); // don't need to see the changelog on first install
			FragmentUtils.alertDialog(this, AlertFragment.FIRST);
			setNotificationState(); // initially, all notifications are stopped
		} else if (newVersion())
			showChangelog();
	}

	public void setupControls() {
		Utils.setTitleSize(this, 24);
		
		GridView grid = (GridView) findViewById(R.id.grid);
		grid.setAdapter(new MenuAdapter(this));
		grid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String tag = (String) view.getTag();
				if (tag.equals("committees"))
					startActivity(new Intent(MenuMain.this, CommitteeTabs.class));
				else if (tag.equals("bills"))
					startActivity(new Intent(MenuMain.this, MenuBills.class));
				else if (tag.equals("votes"))
					startActivity(new Intent(MenuMain.this, RollList.class).putExtra("type", RollList.ROLLS_LATEST));
				else if (tag.equals("legislators"))
					startActivity(new Intent(MenuMain.this, MenuLegislators.class));
				else if (tag.equals("floor_updates"))
					startActivity(new Intent(MenuMain.this, FloorUpdateTabs.class));
				else if (tag.equals("hearings"))
					startActivity(new Intent(MenuMain.this, HearingList.class).putExtra("chamber", "senate"));
			}
		});
		
		setupDebugBar();
		
		findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { showAbout(); }
		});
		
		findViewById(R.id.donate).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { goDonate(); }
		});
		
		findViewById(R.id.feedback).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { doFeedback(); }
		});
		
		
		Utils.setActionButton(this, R.id.action_1, R.drawable.notifications, new View.OnClickListener() {
			public void onClick(View v) { 
				startActivity(new Intent(MenuMain.this, NotificationTabs.class)); 
			}
		});
	}
	
	private void setupDebugBar() {
		if (getResources().getString(R.string.debug_show_buttons).equals("true")) {
			findViewById(R.id.debug_bar).setVisibility(View.VISIBLE);
			findViewById(R.id.check).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					WakefulIntentService.sendWakefulWork(MenuMain.this, NotificationService.class);
				}
			});
		}
	}

	// destructive function that triggers the first time flag and lets you know if it did so
	public boolean firstTime() {
		if (Utils.getBooleanPreference(this, "first_time", true)) {
			Utils.setBooleanPreference(this, "first_time", false);
			return true;
		}
		return false;
	}
	
	public boolean newVersion() {
		String lastVersionSeen = getVersionSeen();
		String currentVersion = getResources().getString(R.string.app_version);
		if (lastVersionSeen != null && lastVersionSeen.equals(currentVersion))
			return false;
		else {
			setVersionSeen(currentVersion);
			return true;
		}
	}

	public void setNotificationState() {
		Utils.setBooleanPreference(this, NotificationSettings.KEY_NOTIFY_ENABLED,
				NotificationSettings.DEFAULT_NOTIFY_ENABLED);
	}


	public void setVersionSeen(String version) {
		Utils.setStringPreference(this, "last_version_seen", version);
	}

	public String getVersionSeen() {
		return Utils.getStringPreference(this, "last_version_seen");
	}


	@Override 
	public boolean onCreateOptionsMenu(Menu menu) { 
		super.onCreateOptionsMenu(menu); 
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void goDonate() {
		Analytics.page(this, "/donate", false);
		String packageName = getResources().getString(R.string.app_donation_package_name);
		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
		} catch(ActivityNotFoundException e) {
			Utils.alert(this, R.string.no_market_installed);
		}
	}
	
	public void showAbout() {
		Analytics.page(this, "/about", false);
		FragmentUtils.alertDialog(this, AlertFragment.ABOUT);
	}
	
	public void showChangelog() {
		Analytics.page(this, "/changelog", false);
		FragmentUtils.alertDialog(this, AlertFragment.CHANGELOG);
	}
	
	public void doFeedback() {
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getResources().getString(R.string.contact_email), null));
		intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.contact_subject));
		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) { 
		case R.id.settings:
			startActivity(new Intent(this, Settings.class));
			break;
		case R.id.changelog:
			showChangelog();
			break;
		}
		return true;
	}
	
	
	private static class MenuAdapter extends BaseAdapter {
		private static final int BILLS = 0;
		private static final int VOTES = 1;
		private static final int LEGISLATORS = 2;
		private static final int COMMITTEES = 3;
		private static final int FLOOR = 4;
		private static final int HEARINGS = 5;
		
		LayoutInflater inflater;
		
		public MenuAdapter(Context context) {
			this.inflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return 6;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = inflater.inflate(R.layout.menu_main_item, null);
			
			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			TextView text = (TextView) view.findViewById(R.id.text);
			
			if (position == BILLS) {
				icon.setImageResource(R.drawable.menu_selector_bills);
				text.setText(R.string.menu_main_bills);
				view.setTag("bills");
			} else if (position == VOTES) {
				icon.setImageResource(R.drawable.menu_selector_votes);
				text.setText(R.string.menu_main_votes);
				view.setTag("votes");
			} else if (position == LEGISLATORS) {
				icon.setImageResource(R.drawable.menu_selector_people);
				text.setText(R.string.menu_main_legislators);
				view.setTag("legislators");
			} else if (position == COMMITTEES) {
				icon.setImageResource(R.drawable.menu_selector_committees);
				text.setText(R.string.menu_main_committees);
				view.setTag("committees");
			} else if (position == FLOOR) {
				icon.setImageResource(R.drawable.menu_selector_floor);
				text.setText(R.string.menu_main_floor_updates);
				view.setTag("floor_updates");
			} else if (position == HEARINGS) {
				icon.setImageResource(R.drawable.menu_selector_hearings);
				text.setText(R.string.menu_main_hearings);
				view.setTag("hearings");
			}
			
			return view;
		}
		
	}
}