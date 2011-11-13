package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.CommitteeService;

public class CommitteeList extends FragmentActivity {
	public static final int CHAMBERS = 1;
	public static final int LEGISLATOR = 2;
	
	private int type;
	private Legislator legislator;
	
	private ViewPager pager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Bundle extras = getIntent().getExtras();
		
		type = extras.getInt("type");
		legislator = (Legislator) extras.getSerializable("legislator");
		
		setupControls();
		if (type == LEGISLATOR)
			Analytics.track(this, "/legislator/committees?bioguide_id=" + legislator.id);
		else if (type == CHAMBERS)
			Analytics.track(this, "/committees");
	}
	
	private void setupControls() {
		if (type == LEGISLATOR) {
			setContentView(R.layout.frame_titled);
			Utils.setTitle(this, "Committees for " + legislator.titledName(), R.drawable.committees);
			Utils.setTitleSize(this, 16);
			getSupportFragmentManager().beginTransaction().add(R.id.frame, CommitteeListFragment.forLegislator(legislator)).commit();
		} else {
			setContentView(R.layout.pager_titled);
			setupPager();
		}
	}
	
	private void setupPager() {
		Resources resources = getResources();
		
		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		TitlePageAdapter adapter = new TitlePageAdapter(this, pager);
		adapter.add("house", resources.getString(R.string.tab_house), CommitteeListFragment.forChamber("house"));
		adapter.add("senate", resources.getString(R.string.tab_senate), CommitteeListFragment.forChamber("senate"));
		adapter.add("joint", resources.getString(R.string.tab_joint), CommitteeListFragment.forChamber("joint"));
	}
	
	public static class TitlePageAdapter extends FragmentPagerAdapter {
		private List<String> handles = new ArrayList<String>();
		private List<Fragment> fragments = new ArrayList<Fragment>();
		
		private ViewGroup mainView;
		private Map<String,View> titleViews = new HashMap<String,View>();
		String currentHandle;
		
		private ViewPager pager;
		private FragmentActivity activity;
		
		public TitlePageAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            this.activity = activity;
            this.pager = pager;
            
            pager.setAdapter(this);
            pager.setOnPageChangeListener(new TitlePageListener(this));
            
            mainView = (ViewGroup) activity.findViewById(R.id.pager_titles);
        }
		
		/**
		 * Adds a title with associated fragment to the adapter, and a handle to refer to it by.
		 * @return Position of the added fragment.
		 */
		
		public int add(String handle, String title, Fragment fragment) {
			handles.add(handle);
			fragments.add(fragment);
			
			final int position = fragments.size() - 1;
			
			View titleView = LayoutInflater.from(activity).inflate(R.layout.pager_tab, null);
			((TextView) titleView.findViewById(R.id.tab_name)).setText(title);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
			titleView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					pager.setCurrentItem(position);
				}
			});
			
			mainView.addView(titleView, params);
			titleViews.put(handle, titleView);
			
			// mark first item on by default
			if (position == 0) {
				currentHandle = handle;
				markOn(handle);
			}
			
			return position;
		}

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }
        
        public void selectPage(int position) {
        	String newHandle = handles.get(position);
        	Log.d(Utils.TAG, "Selected page with handle " + newHandle);
        	
        	markOff(currentHandle);
        	markOn(newHandle);
        	
        	currentHandle = newHandle;
        }
        
        private void markOff(String handle) {
        	titleViews.get(handle).findViewById(R.id.tab_line).setVisibility(View.INVISIBLE);
        }
        
        private void markOn(String handle) {
        	titleViews.get(handle).findViewById(R.id.tab_line).setVisibility(View.VISIBLE);
        }
        
        private static class TitlePageListener extends ViewPager.SimpleOnPageChangeListener {
        	TitlePageAdapter adapter;
        	
        	public TitlePageListener(TitlePageAdapter adapter) {
        		this.adapter = adapter;
        	}
        	
        	@Override
        	public void onPageSelected(int position) {
        		adapter.selectPage(position);
        	}
        }
	}
	
	public static class CommitteeListFragment extends ListFragment {
		public static final int CHAMBER = 1;
		public static final int LEGISLATOR = 2;
		
		private List<Committee> committees;
		private int type;
		private String chamber;
		private Legislator legislator;
		
		public static CommitteeListFragment forChamber(String chamber) {
			CommitteeListFragment frag = new CommitteeListFragment();
			Bundle args = new Bundle();
			args.putInt("type", CHAMBER);
			args.putString("chamber", chamber);
			frag.setArguments(args);
			frag.setRetainInstance(true);
			return frag;
		}
		
		public static CommitteeListFragment forLegislator(Legislator legislator) {
			CommitteeListFragment frag = new CommitteeListFragment();
			Bundle args = new Bundle();
			args.putInt("type", LEGISLATOR);
			args.putSerializable("legislator", legislator);
			frag.setArguments(args);
			frag.setRetainInstance(true);
			return frag;
		}
		
		public CommitteeListFragment() {}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			FragmentUtils.setupSunlight(this);
			
			Bundle args = getArguments();
			type = args.getInt("type");
			chamber = args.getString("chamber");
			legislator = (Legislator) args.getSerializable("legislator");
			
			if (type == CHAMBER)
				new LoadCommitteesTask(this).execute("chamber", chamber);
			else if (type == LEGISLATOR)
				new LoadCommitteesTask(this).execute("bioguideId", legislator.bioguide_id);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.list, container, false);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			
			FragmentUtils.setLoading(this, R.string.committees_loading);
			
			if (committees != null)
				displayCommittees();
		}
		
		@Override
		public void onListItemClick(ListView parent, View v, int position, long id) {
			selectCommittee((Committee) parent.getItemAtPosition(position));
		}

		private void selectCommittee(Committee committee) {
			startActivity(new Intent(getActivity(), LegislatorList.class)
				.putExtra("type", LegislatorList.SEARCH_COMMITTEE)
				.putExtra("committeeId", committee.id)
				.putExtra("committeeName", committee.name));
		}
		
		public void onLoadCommittees(List<Committee> committees) {
			Collections.sort(committees);
			this.committees = committees;
			
			if (isAdded()) 
				displayCommittees();
		}
		
		public void onLoadCommittees(CongressException exception) {
			if (isAdded())
				FragmentUtils.showEmpty(this, exception.getMessage());
		}

		public void displayCommittees() {
			if (committees.size() > 0)
				setListAdapter(new CommitteeAdapter(this, committees));
			else
				FragmentUtils.showEmpty(this, (type == CHAMBER ? R.string.committees_empty : R.string.legislator_no_committees));
		}

		private static class CommitteeAdapter extends ArrayAdapter<Committee> {
			LayoutInflater inflater;

			public CommitteeAdapter(Fragment context, List<Committee> items) {
				super(context.getActivity(), 0, items);
				this.inflater = LayoutInflater.from(context.getActivity());
			}

			@Override
			public int getViewTypeCount() {
				return 1;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = null;
				ViewHolder holder;
				if (convertView == null) {
					view = inflater.inflate(R.layout.committee_item, null);
					holder = new ViewHolder();
					holder.name = (TextView) view.findViewById(R.id.name);
					view.setTag(holder);
				}
				else {
					view = convertView;
					holder = (ViewHolder) view.getTag();
				}

				Committee committee = getItem(position);
				holder.committeeId = committee.id;
				holder.name.setText(committee.name);

				return view;
			}
			
			static class ViewHolder {
				TextView name;
				String committeeId;

				@Override
				public boolean equals(Object o) {
					ViewHolder oh = (ViewHolder) o;
					return oh != null && oh.committeeId.equals(this.committeeId);
				}
			}
		}
		
		private static class LoadCommitteesTask extends AsyncTask<String, Void, List<Committee>> {
			private CommitteeListFragment context;
			private CongressException exception;

			public LoadCommitteesTask(CommitteeListFragment context) {
				this.context = context;
			}

			@Override
			protected List<Committee> doInBackground(String... params) {
				if (params[0].equals("chamber"))
					return forChamber(params[1]);
				else if (params[0].equals("bioguideId"))
					return forLegislator(params[1]);
				else
					return null;
			}
			
			private List<Committee> forLegislator(String bioguideId) {
				List<Committee> committees = new ArrayList<Committee>();
				List<Committee> joint = new ArrayList<Committee>();
				List<Committee> temp;
				
				try {
					temp = CommitteeService.forLegislator(bioguideId);
				} catch (CongressException e) {
					this.exception = new CongressException(e, "Error loading committees.");
					return null;
				}
				for (int i=0; i<temp.size(); i++) {
					if (temp.get(i).chamber.equals("Joint"))
						joint.add(temp.get(i));
					else
						committees.add(temp.get(i));
				}
				Collections.sort(committees);
				Collections.sort(joint);
				committees.addAll(joint);
				return committees;
			}
			
			private List<Committee> forChamber(String chamber) {
				List<Committee> result = new ArrayList<Committee>();
				try {
					result = CommitteeService.getAll(chamber);
				} catch (CongressException e) {
					Log.e(Utils.TAG, "There has been an exception while getting the committees for chamber "
							+ chamber + ": " + e.getMessage());
				}
				return result;
			}

			@Override
			protected void onPostExecute(List<Committee> result) {
				if (result != null && exception == null)
					context.onLoadCommittees(result);
				else
					context.onLoadCommittees(exception);
			}

		}
	}
}