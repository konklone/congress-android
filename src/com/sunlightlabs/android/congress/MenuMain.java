package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.sunlightlabs.android.congress.notifications.NotificationService;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.UpcomingBill;
import com.sunlightlabs.congress.services.UpcomingBillService;

public class MenuMain extends FragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu_main);

		Analytics.track(this, "/");
		
		setupControls();
		setupFragments();
		
		if (firstTime()) {
			newVersion(); // don't need to see the changelog on first install
			FragmentUtils.alertDialog(this, AlertFragment.FIRST);
			setNotificationState(); // initially, all notifications are stopped
		} else if (newVersion())
			showChangelog();
	}

	public void setupControls() {
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
	
	private void setupFragments() {
		FragmentManager manager = getSupportFragmentManager();
		if (manager.findFragmentById(R.id.main_navigation) == null)
			manager.beginTransaction().add(R.id.main_navigation, MainMenuFragment.newInstance()).commit();
		if (manager.findFragmentById(R.id.upcoming_list) == null)
			manager.beginTransaction().add(R.id.upcoming_list, UpcomingFragment.newInstance()).commit();
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
	
	public static class MainMenuFragment extends Fragment {
		
		public static MainMenuFragment newInstance() {
			MainMenuFragment frag = new MainMenuFragment();
			frag.setRetainInstance(true);
			return frag;
		}
		
		public MainMenuFragment() {}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.main_navigation_frame, container, false);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setupControls();
		}
		
		private void setupControls() {
			menuItem(R.id.menu_legislators, R.drawable.people, R.string.menu_main_legislators,
					new Intent(getActivity(), MenuLegislators.class));
			
			menuItem(R.id.menu_bills, R.drawable.bills, R.string.menu_main_bills,
					new Intent(getActivity(), MenuBills.class));
			
			menuItem(R.id.menu_votes, R.drawable.votes, R.string.menu_main_votes,
					new Intent(getActivity(), RollList.class)
						.putExtra("type", RollList.ROLLS_LATEST));
			
			menuItem(R.id.menu_floor, R.drawable.floor, R.string.menu_main_floor_updates,
					new Intent(getActivity(), FloorUpdatePager.class));
			
			menuItem(R.id.menu_hearings, R.drawable.hearings, R.string.menu_main_hearings,
					new Intent(getActivity(), HearingList.class)
						.putExtra("chamber", "senate"));
			
			menuItem(R.id.menu_committees, R.drawable.committees, R.string.menu_main_committees,
					new Intent(getActivity(), CommitteeList.class)
						.putExtra("type", CommitteeList.CHAMBERS));
		}
		
		private View menuItem(int id, int icon, int text, final Intent intent) {
			ViewGroup item = (ViewGroup) getView().findViewById(id);
			((ImageView) item.findViewById(R.id.icon)).setImageResource(icon);
			((TextView) item.findViewById(R.id.text)).setText(text);
			
			item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(intent);
				}
			});
			
			return item;
		}
		
	}
	
	public static class UpcomingFragment extends ListFragment {
		
		List<UpcomingBill> upcomingBills;
		
		public static UpcomingFragment newInstance() {
			UpcomingFragment fragment = new UpcomingFragment();
			fragment.setRetainInstance(true);
			return fragment;
		}
		
		public UpcomingFragment() {}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			new UpcomingBillsTask(this).execute();
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.list_no_divider, container, false);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setupControls();
			
			if (upcomingBills != null)
				displayUpcomingBills();
		}
		
		private void setupControls() {
			FragmentUtils.setLoading(this, R.string.upcoming_loading);
			
			// TODO: setup refresh button behavior
		}
		
		@Override
		public void onListItemClick(ListView parent, View v, int position, long id) {
			if (isAdded()) {
				Bill bill = ((UpcomingAdapter.Bill) parent.getItemAtPosition(position)).bill;
				startActivity(Utils.billLoadIntent(bill.id, bill.code));
			}
		}
		
		private void onLoadUpcomingBills(List<UpcomingBill> upcomingBills) {
			this.upcomingBills = upcomingBills;
			
			if (isAdded())
				displayUpcomingBills();
		}
		
		private void onLoadUpcomingBills(CongressException exception) {
			if (isAdded())
				FragmentUtils.showRefresh(this, R.string.upcoming_error);
		}
		
		private void displayUpcomingBills() {
			if (upcomingBills.size() > 0)
				setListAdapter(new UpcomingAdapter(this, UpcomingAdapter.wrapUpcoming(upcomingBills)));
			else
				FragmentUtils.showEmpty(this, R.string.upcoming_empty);
		}
		
		static class UpcomingBillsTask extends AsyncTask<String, Void, List<UpcomingBill>> {
			private UpcomingFragment context;
			private CongressException exception;

			public UpcomingBillsTask(UpcomingFragment context) {
				FragmentUtils.setupRTC(context);
				this.context = context;
			}

			@Override
			protected List<UpcomingBill> doInBackground(String... params) {
				try {
					Date today = new GregorianCalendar().getTime();
					return UpcomingBillService.comingUp(today);
				} catch (CongressException e) {
					this.exception = new CongressException(e, "Error loading upcoming activity.");
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(List<UpcomingBill> result) {
				if (result != null && exception == null)
					context.onLoadUpcomingBills(result);
				else
					context.onLoadUpcomingBills(exception);
			}
		}
		
		static class UpcomingAdapter extends ArrayAdapter<UpcomingAdapter.Item> {
			LayoutInflater inflater;
			
			private static final int TYPE_DATE = 0;
			private static final int TYPE_BILL = 1;

			public UpcomingAdapter(Fragment context, List<UpcomingAdapter.Item> items) {
				super(context.getActivity(), 0, items);
				this.inflater = LayoutInflater.from(context.getActivity());
			}
			
			@Override
	        public boolean isEnabled(int position) {
	        	return getItemViewType(position) == UpcomingAdapter.TYPE_BILL;
	        }
	        
	        @Override
	        public boolean areAllItemsEnabled() {
	        	return false;
	        }
	        
	        @Override
	        public int getItemViewType(int position) {
	        	Item item = getItem(position);
	        	if (item instanceof UpcomingAdapter.Date)
	        		return UpcomingAdapter.TYPE_DATE;
	        	else
	        		return UpcomingAdapter.TYPE_BILL;
	        }
	        
	        @Override
	        public int getViewTypeCount() {
	        	return 2;
	        }

			@Override
			public View getView(int position, View view, ViewGroup parent) {
				Item item = getItem(position);
				if (item instanceof Date) {
					if (view == null)
						view = inflater.inflate(R.layout.upcoming_date, null);
					
					Date date = (Date) item;
					
					((TextView) view.findViewById(R.id.date_name)).setText(date.dateName);
					((TextView) view.findViewById(R.id.date_full)).setText(date.dateFull);
				} else { // instanceof Action
					if (view == null)
						view = inflater.inflate(R.layout.upcoming_bill, null);
					
					Bill bill = (Bill) item;
					
					((TextView) view.findViewById(R.id.code)).setText(bill.code);
					((TextView) view.findViewById(R.id.title)).setText(bill.title);
				}

				return view;
			}
			
			static class Item {}
			static class Date extends Item {
				String dateName, dateFull;
			}
			static class Bill extends Item {
				String code, title;
				com.sunlightlabs.congress.models.Bill bill;
				String context;
			}
			
			static List<UpcomingAdapter.Item> wrapUpcoming(List<UpcomingBill> upcomingBills) {
				List<Item> items = new ArrayList<Item>();
				
				SimpleDateFormat dateNameFormat = new SimpleDateFormat("EEEE");
				SimpleDateFormat dateFullFormat = new SimpleDateFormat("MMM d");
				SimpleDateFormat testFormat = new SimpleDateFormat("yyyy-MM-dd");
				
				String today = testFormat.format(Calendar.getInstance().getTime());
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, 1);
				String tomorrow = testFormat.format(cal.getTime());
				
				
				String currentDay = "";
				
				for (int i=0; i<upcomingBills.size(); i++) {
					UpcomingBill upcomingBill = upcomingBills.get(i);
					
					GregorianCalendar calendar = new GregorianCalendar();
					calendar.setTime(upcomingBill.legislativeDay);
					
					String testDay = testFormat.format(upcomingBill.legislativeDay);
					
					if (!currentDay.equals(testDay)) {
						Date date = new Date();
						if (today.equals(testDay))
							date.dateName = "TODAY";
						else if (tomorrow.equals(testDay))
							date.dateName = "TOMORROW";
						else
							date.dateName = dateNameFormat.format(upcomingBill.legislativeDay).toUpperCase();
						
						date.dateFull = dateFullFormat.format(upcomingBill.legislativeDay).toUpperCase();
						items.add(date);
						
						currentDay = testDay;
					}
					
					com.sunlightlabs.congress.models.Bill rootBill = upcomingBill.bill;
					Bill bill = new Bill();
					
					bill.code = com.sunlightlabs.congress.models.Bill.formatCodeShort(rootBill.code);
					
					String title;
					if (rootBill.short_title != null && !rootBill.short_title.equals(""))
						title = rootBill.short_title;
					else
						title = Utils.truncate(rootBill.official_title, 60);
					
					bill.title = title;
					bill.bill = rootBill;
					
					items.add(bill);
				}
				
				return items;
			}
		}
	}
}