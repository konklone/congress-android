package com.sunlightlabs.android.congress.fragments;

import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.PaginationListener;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Hearing;
import com.sunlightlabs.congress.services.HearingService;
import com.sunlightlabs.congress.services.ProPublica;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HearingListFragment extends ListFragment implements PaginationListener.Paginates {
	private List<Hearing> hearings;

    PaginationListener pager;
    View loadingView;

	public static Fragment upcoming() {
		HearingListFragment frag = new HearingListFragment();
		frag.setRetainInstance(true);
		return frag;
	}

	public HearingListFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadHearings();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setupControls();

		if (hearings != null)
			displayHearings();
	}

	private void setupControls() {
		getView().findViewById(R.id.refresh).setOnClickListener(v -> refresh());

        loadingView = LayoutInflater.from(getActivity()).inflate(R.layout.loading_page, null);
        loadingView.setVisibility(View.GONE);

        pager = new PaginationListener(this);
        getListView().setOnScrollListener(pager);

		FragmentUtils.setLoading(this, R.string.hearings_loading);
	}

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        selectHearing((Hearing) parent.getItemAtPosition(position));
    }

	private void refresh() {
		hearings = null;
		FragmentUtils.setLoading(this, R.string.hearings_loading);
		FragmentUtils.showLoading(this);
		loadHearings();
	}

    private void loadHearings() {
        new LoadHearingsTask(this, 1).execute();
    }

    @Override
    public void loadNextPage(int page) {
        getListView().setOnScrollListener(null);
        loadingView.setVisibility(View.VISIBLE);
        new LoadHearingsTask(this, page).execute();
    }

	public void selectHearing(Hearing hearing) {
		Date date = hearing.occursAt;
		long startTime = date.getTime();
		long endTime = startTime + (3 * 60 * 60 * 1000);

		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");

		intent.putExtra("title", "Hearing: " + hearing.committee.name);
		intent.putExtra("description", hearing.description);
		intent.putExtra("eventLocation", hearing.room);

		intent.putExtra("beginTime", startTime);
		intent.putExtra("endTime", endTime);
		intent.putExtra("allDay", false);

		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Utils.alert(getActivity(), R.string.hearings_no_calendar);
		}
	}

	public void onLoadHearings(List<Hearing> hearings, int page) {
        if (!isAdded())
            return;

        if (page == 1) {
            this.hearings = hearings;
            displayHearings();
        } else {
            this.hearings.addAll(hearings);
            loadingView.setVisibility(View.GONE);
            ((HearingAdapter) getListAdapter()).notifyDataSetChanged();
        }

        // only re-enable the pagination if we got a full page back
        if (hearings.size() >= ProPublica.PER_PAGE)
            getListView().setOnScrollListener(pager);
	}

	public void onLoadHearings(CongressException exception) {
		FragmentUtils.showRefresh(this, R.string.hearings_error);
	}

	public void displayHearings() {
		if (hearings.size() > 0) {
			TextView header = getView().findViewById(R.id.header_simple_text);
			header.setText(R.string.hearings_header);
			header.setVisibility(View.VISIBLE);

			setListAdapter(new HearingAdapter(this, hearings));
		} else
			FragmentUtils.showEmpty(this, R.string.hearings_empty);
	}

    private static class LoadHearingsTask extends AsyncTask<Void, Void, List<Hearing>> {
        private HearingListFragment context;
        private CongressException exception;
        private int page;

        public LoadHearingsTask(HearingListFragment context, int page) {
            FragmentUtils.setupAPI(context);
            this.context = context;
            this.page = page;
        }

        @Override
        protected List<Hearing> doInBackground(Void... params) {
            List<Hearing> hearings;

            try {
                hearings = HearingService.upcoming(page);
            } catch (CongressException e) {
                Log.e(Utils.TAG, "Error while loading committee hearings: " + e.getMessage());
                this.exception = e;
                return null;
            }

            return hearings;
        }

        @Override
        protected void onPostExecute(List<Hearing> hearings) {
            if (exception != null)
                context.onLoadHearings(exception);
            else
                context.onLoadHearings(hearings, page);
        }
    }

	static class HearingAdapter extends ArrayAdapter<Hearing> {
    	LayoutInflater inflater;
    	Resources resources;

        public HearingAdapter(HearingListFragment context, List<Hearing> items) {
            super(context.getActivity(), 0, items);
            inflater = LayoutInflater.from(context.getActivity());
            resources = context.getResources();
        }

		@Override
        public boolean areAllItemsEnabled() {
        	return true;
        }

		@Override
        public int getViewTypeCount() {
        	return 1;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			Hearing hearing = getItem(position);

			if (view == null)
				view = inflater.inflate(R.layout.hearing, null);

			Date date = hearing.occursAt;
            SimpleDateFormat dateDisplay = new SimpleDateFormat("MMM d");
            SimpleDateFormat timeDisplay = new SimpleDateFormat("h:mm aa");
            dateDisplay.setTimeZone(ProPublica.CONGRESS_TIMEZONE);
            timeDisplay.setTimeZone(ProPublica.CONGRESS_TIMEZONE);

			String month = dateDisplay.format(date).toUpperCase();
			String time = timeDisplay.format(date) + " ET";

            String chamberCap = hearing.chamber.substring(0, 1).toUpperCase() + hearing.chamber.substring(1);
			String name = chamberCap + " " + hearing.committee.name;

			String room = hearing.room;
			if (room.equals("TBA"))
				room = "Room TBA";

			((TextView) view.findViewById(R.id.month)).setText(month);
			((TextView) view.findViewById(R.id.time)).setText(time);
			((TextView) view.findViewById(R.id.name)).setText(name);
			((TextView) view.findViewById(R.id.room)).setText(room);
			((TextView) view.findViewById(R.id.description)).setText(Utils.truncate(hearing.description, 200));

			return view;
		}
    }
}