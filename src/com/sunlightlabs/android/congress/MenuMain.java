package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sunlightlabs.android.congress.notifications.NotificationService;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.Utils;

public class MenuMain extends Activity {
	private static final int ABOUT = 0;
	private static final int FIRST = 1;
	private static final int CHANGELOG = 2;
	private static final int DONATION = 3;

	private static final String BULLET = "<b>&#183;</b> "; 
	
	private GoogleAnalyticsTracker tracker;
	private boolean tracked = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu_main);
		
		Holder holder = (Holder) getLastNonConfigurationInstance();
		if (holder != null)
			tracked = holder.tracked;

		tracker = Analytics.start(this);
		if (!tracked) {
			Analytics.page(this, tracker, "/");
			tracked = true;
		}
		
		setupControls();

		if (firstTime()) {
			newVersion(); // don't need to see the changelog on first install
			showDialog(FIRST);
			setNotificationState(); // initially, all notifications are stopped
		} else if (newVersion()) {
			if (askedDonation())
				showDialog(CHANGELOG);
			else
				showDialog(DONATION);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Holder(tracked);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Analytics.stop(tracker);
	}

	public void setupControls() {		
		GridView grid = (GridView) findViewById(R.id.grid);
		grid.setAdapter(new MenuAdapter(this));
		setupDebugBar();
		findViewById(R.id.notifications).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(MenuMain.this, NotificationSettings.class));
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

	public boolean firstTime() {
		if (Utils.getBooleanPreference(this, "first_time", true)) {
			Utils.setBooleanPreference(this, "first_time", false);
			return true;
		}
		return false;
	}
	
	public boolean askedDonation() {
		if (Utils.getBooleanPreference(this, "askedDonation", false) == false) {
			Utils.setBooleanPreference(this, "askedDonation", true);
			return false;
		}
		return true;
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
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();

		switch(id) {
		case ABOUT:
			View aboutView = inflater.inflate(R.layout.about, null);

			Spanned about1 = Html.fromHtml(
					"Bill information provided by <a href=\"http://govtrack.us\">GovTrack</a>, through the Library of Congress.  Bill summaries written by the Congressional Research Service.<br/><br/>" +
					"Legislator search and information powered by the <a href=\"http://services.sunlightlabs.com/api/\">Sunlight Labs Congress API</a>.<br/><br/>" + 
					"News mentions provided by the <a href=\"http://code.google.com/apis/newssearch/v1/\">Google News Search API</a>, and Twitter search powered by <a href=\"http://www.winterwell.com/software/jtwitter.php\">JTwitter</a>."
			);
			TextView aboutView1 = (TextView) aboutView.findViewById(R.id.about_1);
			aboutView1.setText(about1);
			aboutView1.setMovementMethod(LinkMovementMethod.getInstance());

			Spanned about2 = Html.fromHtml(
					"This app is made by <a href=\"http://sunlightlabs.com\">Sunlight Labs</a>, " + 
					"a division of the <a href=\"http://sunlightfoundation.com\">Sunlight Foundation</a> " +
					"that is dedicated to increasing government transparency through the power of technology."
			);
			TextView aboutView2 = (TextView) aboutView.findViewById(R.id.about_2);
			aboutView2.setText(about2);
			aboutView2.setMovementMethod(LinkMovementMethod.getInstance());

			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.app_name);
			builder.setView(aboutView);
			builder.setPositiveButton(R.string.about_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case FIRST:
			View firstView = inflater.inflate(R.layout.first_time, null);

			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.app_name);
			builder.setView(firstView);
			builder.setPositiveButton(R.string.first_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case CHANGELOG:
			View changelogView = inflater.inflate(R.layout.changelog, null);

			Spanned changelog = getChangelogHtml(R.array.changelog);
			Spanned changelogLast = getChangelogHtml(R.array.changelogLast);

			((TextView) changelogView.findViewById(R.id.changelog)).setText(changelog);
			((TextView) changelogView.findViewById(R.id.changelog_last_title)).setText(R.string.app_version_older);
			((TextView) changelogView.findViewById(R.id.changelog_last)).setText(changelogLast);

			builder.setIcon(R.drawable.icon);
			builder.setTitle(getResources().getString(R.string.changelog_title_prefix) + " " + getResources().getString(R.string.app_version));
			builder.setView(changelogView);
			builder.setPositiveButton(R.string.changelog_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			return builder.create();
		case DONATION:
			View donateView = inflater.inflate(R.layout.donation, null);
			
			Spanned donate2 = Html.fromHtml(
				"You can press Donate below to give through the Android Market. " +
				"If you would prefer to give more, or through the web, you can also " +
				"<a href=\"http://sunlightfoundation.com/donate/\">donate online</a>."
			);
			
			TextView donateTextView = (TextView) donateView.findViewById(R.id.donate_2);
			donateTextView.setText(donate2);
			donateTextView.setMovementMethod(LinkMovementMethod.getInstance());
			
			builder.setIcon(R.drawable.icon);
			builder.setTitle(R.string.donation_title);
			builder.setView(donateView);
			builder.setPositiveButton(R.string.donation_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			builder.setNeutralButton(R.string.donation_donate, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					donationPage();
				}
			});
			return builder.create();
		default:
			return null;
		}
	}

	private Spanned getChangelogHtml(int stringArrayId) {
		String[] array = getResources().getStringArray(stringArrayId);
		List<String> items = new ArrayList<String>();
		for (String item : array) { 
			items.add(BULLET + item); 
		}
		return Html.fromHtml(TextUtils.join("<br/><br/>", items));
	}

	@Override 
	public boolean onCreateOptionsMenu(Menu menu) { 
		super.onCreateOptionsMenu(menu); 
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) { 
		case R.id.settings:
			startActivity(new Intent(this, Settings.class));
			break;
		case R.id.feedback:
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getResources().getString(R.string.contact_email), null));
			intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.contact_subject));
			startActivity(intent);
			break;
		case R.id.changelog:
			Analytics.page(this, tracker, "/changelog", false);
			showDialog(CHANGELOG);
			break;
		case R.id.about:
			Analytics.page(this, tracker, "/about", false);
			showDialog(ABOUT);
			break;
		case R.id.donate:
			Analytics.page(this, tracker, "/donate", false);
			donationPage();
			break;
		}
		return true;
	}
	
	private void donationPage() {
		String packageName = getResources().getString(R.string.app_donation_package_name);
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
	}
	
	private static class MenuAdapter extends BaseAdapter {
		Context context;
		LayoutInflater inflater;
		
		private static final int BILLS = 0;
		private static final int VOTES = 1;
		private static final int LEGISLATORS = 2;
		private static final int COMMITTEES = 3;
		
		public MenuAdapter(Context context) {
			this.context = context;
			this.inflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return 4;
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
				icon.setImageResource(R.drawable.bill_multiple);
				text.setText(R.string.menu_main_bills);
				view.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						context.startActivity(new Intent(context, MenuBills.class));
					}
				});
			} else if (position == VOTES) {
				icon.setImageResource(R.drawable.rolls_menu);
				text.setText(R.string.menu_main_votes);
				view.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						context.startActivity(new Intent(context, RollList.class).putExtra("type", RollList.ROLLS_LATEST));
					}
				});
			} else if (position == LEGISLATORS) {
				icon.setImageResource(R.drawable.search_all);
				text.setText(R.string.menu_main_legislators);
				view.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						context.startActivity(new Intent(context, MenuLegislators.class));
					}
				});
			} else if (position == COMMITTEES) {
				icon.setImageResource(R.drawable.committee);
				text.setText(R.string.menu_main_committees);
				view.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						context.startActivity(new Intent(context, CommitteeTabs.class));
					}
				});
			}
			
			return view;
		}
		
	}
	
	private static class Holder {
		boolean tracked;
		
		public Holder(boolean tracked) {
			this.tracked = tracked;
		}
	}
}