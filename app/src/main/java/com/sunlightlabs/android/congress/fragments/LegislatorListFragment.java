package com.sunlightlabs.android.congress.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.Committee.Membership;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.BillService;
import com.sunlightlabs.congress.services.CommitteeService;
import com.sunlightlabs.congress.services.LegislatorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

public class LegislatorListFragment extends ListFragment implements LoadPhotoTask.LoadsPhoto {
	public static final int SEARCH_STATE = 2;
	public static final int SEARCH_LASTNAME = 3;
	public static final int SEARCH_COMMITTEE = 4;
	public static final int SEARCH_COSPONSORS = 5;
	public static final int SEARCH_CHAMBER = 6; 
	
	List<Legislator> legislators;
	Map<String,LoadPhotoTask> loadPhotoTasks = new HashMap<String,LoadPhotoTask>();
	
	int type;
	String chamber;
	String billId;
	String lastName, state;
	Committee committee;
	
	public static LegislatorListFragment forChamber(String chamber) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_CHAMBER);
		args.putString("chamber", chamber);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static LegislatorListFragment forState(String state) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_STATE);
		args.putString("state", state);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static LegislatorListFragment forCommittee(Committee committee) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_COMMITTEE);
		args.putSerializable("committee", committee);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public static LegislatorListFragment forBill(String billId) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_COSPONSORS);
		args.putString("billId", billId);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public static LegislatorListFragment forLastName(String lastName) {
		LegislatorListFragment frag = new LegislatorListFragment();
		Bundle args = new Bundle();
		args.putInt("type", SEARCH_LASTNAME);
		args.putString("last_name", lastName);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}

	public LegislatorListFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getInt("type");
		chamber = args.getString("chamber");
		lastName = args.getString("last_name");
		state = args.getString("state");
		committee = (Committee) args.getSerializable("committee");
		billId = args.getString("billId");

		loadLegislators();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.list, container, false);
		
		if (type == SEARCH_CHAMBER) {
			ListView list = (ListView) view.findViewById(android.R.id.list);
			list.setFastScrollEnabled(true);
		}
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (legislators != null)
			displayLegislators();
	}
	
	public void setupControls() {
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				refresh();
			}
		});

		if (type == SEARCH_COSPONSORS)
			FragmentUtils.setLoading(this, R.string.legislators_loading_cosponsors);
		else
			FragmentUtils.setLoading(this, R.string.legislators_loading);
	}
	
	public void loadLegislators() {
		new LoadLegislatorsTask(this).execute();
	}
	
	public void onLoadLegislators(List<Legislator> legislators) {
		if (!isAdded())
			return;
		
		// if there's only one result, don't even make them click it
		if ((legislators.size() == 1) && (type != SEARCH_COSPONSORS)) {
			selectLegislator(legislators.get(0));
			getActivity().finish();
		} else 
			displayLegislators();
	}
	
	public void onLoadLegislators(CongressException exception) {
		if (isAdded())
			FragmentUtils.showRefresh(this, R.string.legislators_error);
	}

	public void displayLegislators() {
		if (legislators.size() > 0)
			setListAdapter(new LegislatorAdapter(this, legislators));
		else {
			switch (type) {
			case SEARCH_LASTNAME:
				FragmentUtils.showEmpty(this, R.string.empty_last_name);
				break;
			default:
				FragmentUtils.showEmpty(this, R.string.legislators_error);
			}
		}
	}
	
	public void loadPhoto(String bioguide_id) {
		if (!loadPhotoTasks.containsKey(bioguide_id)) {
			try {
				loadPhotoTasks.put(bioguide_id, (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_SMALL, bioguide_id).execute(bioguide_id));
			} catch (RejectedExecutionException e) {
				Log.e(Utils.TAG, "[LegislatorListFragment] RejectedExecutionException occurred while loading photo.", e);
				onLoadPhoto(null, bioguide_id); // if we can't run it, then just show the no photo image and move on
			}
		}
	}

	public void onLoadPhoto(Drawable photo, Object tag) {
		if (!isAdded())
			return;
		
		loadPhotoTasks.remove(tag);
		
		LegislatorAdapter.ViewHolder holder = new LegislatorAdapter.ViewHolder();
		holder.bioguide_id = (String) tag;
		
		View result = getListView().findViewWithTag(holder);
		if (result != null) {
			if (photo != null)
				((ImageView) result.findViewById(R.id.photo)).setImageDrawable(photo);
			else 
				((ImageView) result.findViewById(R.id.photo)).setImageResource(R.drawable.person);
		}
	}

	public Context getContext() {
		return getActivity();
	}
	
	private void refresh() {
		legislators = null;
		FragmentUtils.setLoading(this, R.string.legislators_loading);
		FragmentUtils.showLoading(this);
		loadLegislators();
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		selectLegislator((Legislator) parent.getItemAtPosition(position));
	}

	public void selectLegislator(Legislator legislator) {
		startActivity(Utils.legislatorIntent(legislator.bioguide_id));
	}
	
	private static class LegislatorAdapter extends ArrayAdapter<Legislator> {
		LayoutInflater inflater;
		LegislatorListFragment context;

		public LegislatorAdapter(LegislatorListFragment context, List<Legislator> items) {
			super(context.getActivity(), 0, items);
			this.context = context;
			inflater = LayoutInflater.from(context.getActivity());
		}
		
		@Override
        public boolean areAllItemsEnabled() {
        	return true;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 1;
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;
			if (convertView == null) {
				view = inflater.inflate(R.layout.legislator_item, null);
				
				holder = new ViewHolder();
				holder.title = (TextView) view.findViewById(R.id.committee_title);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.position = (TextView) view.findViewById(R.id.position);
				holder.photo = (ImageView) view.findViewById(R.id.photo);
				
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			
			Legislator legislator = getItem(position);

			// used as the hook to get the legislator image in place when it's loaded
			holder.bioguide_id = legislator.bioguide_id;

			holder.name.setText(nameFor(legislator));
			holder.position.setText(positionFor(legislator));

			ImageView photo = (ImageView) view.findViewById(R.id.photo);
			LegislatorImage.setImageView(legislator.bioguide_id, LegislatorImage.PIC_SMALL,
					context.getContext(), holder.photo);

			return view;
		}

		public String nameFor(Legislator legislator) {
			if (context.type == SEARCH_COMMITTEE) {
				return legislator.title + ". " + legislator.firstName() + " " + legislator.last_name;
			} else
				return legislator.last_name + ", " + legislator.firstName();
		}

		public String positionFor(Legislator legislator) {
			String stateName = Utils.stateCodeToName(context.getActivity(), legislator.state);
			
			if (context.type == SEARCH_COMMITTEE) {
				String position = legislator.party + " - " + stateName;
				if (legislator.membership != null && legislator.membership.title != null)
					return legislator.membership.title + " - " + position;
				else
					return position;
			} else {
				String district;
				if (legislator.chamber.equals("senate"))
					district = "Senator";
				else
					district = "District " + legislator.district;
				
				return legislator.party + " - " + stateName + " - " + district;
			}
		}
		
		static class ViewHolder {
			TextView title, name, position;
			ImageView photo;
			String bioguide_id;
			
			@Override
			public boolean equals(Object holder) {
				ViewHolder other = (ViewHolder) holder;
				return other != null && other instanceof ViewHolder && this.bioguide_id.equals(other.bioguide_id);
			}
		}

	}

	
	private static class LoadLegislatorsTask extends AsyncTask<Void, Void, List<Legislator>> {
		LegislatorListFragment context;
		CongressException exception;

		public LoadLegislatorsTask(LegislatorListFragment context) {
			super();
			this.context = context;
			FragmentUtils.setupAPI(context);
		}

		@Override
		protected List<Legislator> doInBackground(Void... nothing) {
			List<Legislator> legislators = new ArrayList<Legislator>();
			
			List<Legislator> temp;
			try {
				switch (context.type) {
				case SEARCH_LASTNAME:
					temp = LegislatorService.allByLastName(context.lastName);
					break;
				case SEARCH_COMMITTEE:
					temp = CommitteeService.find(context.committee.id).members;
					break;
				case SEARCH_STATE:
					temp = LegislatorService.allForState(context.state);
					break;
				case SEARCH_COSPONSORS:
					temp = BillService.find(context.billId, new String[] {"cosponsors"}).cosponsors;
					break;
				case SEARCH_CHAMBER:
					return LegislatorService.allByChamber(context.chamber);
				default:
					return legislators;
				}
				
				if (context.type == SEARCH_COMMITTEE) {
					// put Chair and Ranking Member first, then
					// put majority in order of rank, then minority in order of rank
					List<Legislator> leaders = new ArrayList<Legislator>();
					List<Legislator> rankAndFile = new ArrayList<Legislator>();
					
					for (int i=0; i< temp.size(); i++) {
						Legislator legislator = temp.get(i);
						Membership membership = legislator.membership;
						if (membership.title != null && (membership.title.contains("Chair") || membership.title.contains("Ranking")))
							leaders.add(legislator);
						else
							rankAndFile.add(legislator);
					}
					
					Collections.sort(leaders, new Comparator<Legislator>() {
						@Override
						public int compare(Legislator a, Legislator b) {
							return a.membership.title.compareTo(b.membership.title);
						}
					});
					
					Collections.sort(rankAndFile, new Comparator<Legislator>() {
						@Override
						public int compare(Legislator a, Legislator b) {
							if (a.membership.side.equals(b.membership.side))
								return a.membership.rank - b.membership.rank;
							else
								return a.membership.side.compareTo(b.membership.side);
						}
					});
					
					legislators.addAll(leaders);
					legislators.addAll(rankAndFile);
					
				} else {
					// sort legislators Senators-first
					List<Legislator> upper = new ArrayList<Legislator>();
					List<Legislator> lower = new ArrayList<Legislator>();
					
					for (int i = 0; i < temp.size(); i++) {
						if (temp.get(i).chamber.equals("senate"))
							upper.add(temp.get(i));
						else
							lower.add(temp.get(i));
					}
					Collections.sort(upper);
					Collections.sort(lower);
					legislators.addAll(upper);
					legislators.addAll(lower);
				}

				return legislators;

			} catch (CongressException exception) {
				this.exception = exception;
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<Legislator> legislators) {
			context.legislators = legislators;
			
			if (exception == null)
				context.onLoadLegislators(legislators);
			else
				context.onLoadLegislators(exception);
		}
	}

}