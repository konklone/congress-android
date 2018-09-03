package com.sunlightlabs.android.congress.utils;

import android.app.Fragment;
import android.app.ListFragment;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class DateAdapterHelper<Content> {
	protected ListFragment context;

	public Resources resources;
	public LayoutInflater inflater;

	public DateAdapter adapter;
	public List<Content> contents;
	public List<ItemWrapper> items;

	// manage sticky header
	private ViewGroup stickyHeader;
	private TextView stickyHeaderLeft;
	private TextView stickyHeaderRight;
	private OnScrollListener auxScrollListener;

	public interface StickyHeader {}

	public DateAdapterHelper(ListFragment context) {
		this.context = context;
		this.inflater = LayoutInflater.from(context.getActivity());
		this.resources = context.getResources();

		if (context instanceof StickyHeader) {
			stickyHeader = context.getView().findViewById(R.id.header_container);
			stickyHeader.addView(stickyDateView(inflater), 0);
		}
	}

	public DateAdapter adapterFor(List<Content> contents) {
		this.contents = contents;
		this.items = processContents(contents);
		this.adapter = new DateAdapter(this, context, items);

		if (context instanceof StickyHeader) {
			context.getListView().setOnScrollListener(adapter);
			// trigger it once
			if (contents.size() > 0) { 
				// updateStickyHeader(dateFor(contents.get(0)));
				stickyHeader.setVisibility(View.VISIBLE);
			}
		} else
			context.getListView().setOnScrollListener(auxScrollListener);
		return adapter;
	}

	public void notifyDataSetChanged() {
		// this.contents should have been updated in the context
		// this.items is cleared and added to so that the memory reference stays the same,
		// and the adapter's notifyDataSetChanged method can be used (will keep the list in-place when it's updated)
		this.items.clear(); 
		this.items.addAll(processContents(contents));
		this.adapter.notifyDataSetChanged();
	}

	public void setOnScrollListener(OnScrollListener listener) {
		auxScrollListener = listener;
	}

	abstract public Date dateFor(Content content);

	// must override
	public View contentView(Content content) { return null; }

	// override if you want to make use of this aspect of the adapter
	public View contentView(Content content, boolean showTime) {
		return contentView(content); // ignored by default
	}

	// Optionally override this
	public View dateView(Date date) {
		return Utils.dateView(context.getActivity(), date, Utils.fullDateThisYear(date));
	}

	// override this to control how the date view gets populated
	public void updateStickyHeader(Date date, View view, TextView left, TextView right) {}

	// called from the internal adapter
	private void updateStickyHeader(Date date) {
		updateStickyHeader(date, this.stickyHeader.getChildAt(0), stickyHeaderLeft, stickyHeaderRight);
	}

	public View stickyDateView(LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.list_item_date, null);
		stickyHeaderLeft = view.findViewById(R.id.date_left);
		stickyHeaderRight = view.findViewById(R.id.date_right);
		return view;
	}

	private List<ItemWrapper> processContents(List<Content> contents) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
    	SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aa");

		List<ItemWrapper> items = new ArrayList<>();

		String currentDate = "";
		String currentTime = "";

		for (int i=0; i<contents.size(); i++) {
			Content content = contents.get(i);
			ContentWrapper wrapper = new ContentWrapper(content);

			// figure out whether we need to insert a date header first,
			// and whether the showTime flag should be marked

			Date contentDate = dateFor(content);

			String date = dateFormat.format(contentDate);
			String time = timeFormat.format(contentDate);

			if (!currentDate.equals(date))
				items.add(new DateWrapper(contentDate));

			if (!currentTime.equals(time))
				wrapper.showTime = true;

			currentDate = date;
			currentTime = time;

			items.add(wrapper);
		}
		return items;
	}

	public class DateAdapter extends ArrayAdapter<DateAdapterHelper<Content>.ItemWrapper> implements OnScrollListener {
    	DateAdapterHelper<Content> helper;

		private static final int TYPE_DATE = 0;
    	private static final int TYPE_CONTENT = 1; 

    	public DateAdapter(DateAdapterHelper<Content> helper, Fragment context, List<DateAdapterHelper<Content>.ItemWrapper> items) {
            super(context.getActivity(), 0, items);
            this.helper = helper;
        }

		@Override
        public boolean isEnabled(int position) {
        	return false;
        }

		@Override
        public boolean areAllItemsEnabled() {
        	return false;
        }

		@Override
        public int getItemViewType(int position) {
        	DateAdapterHelper<Content>.ItemWrapper item = getItem(position);
        	if (item instanceof DateAdapterHelper.DateWrapper)
        		return TYPE_DATE;
        	else
        		return TYPE_CONTENT;
        }

		@Override
        public int getViewTypeCount() {
        	return 2;
        }

		@SuppressWarnings("unchecked")
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			DateAdapterHelper<Content>.ItemWrapper item = getItem(position);
			if (item instanceof DateAdapterHelper.DateWrapper)
				return dateView(((DateWrapper) item).date);
			else { 
				ContentWrapper wrapper = (ContentWrapper) item;
				return contentView(wrapper.content, wrapper.showTime);
			}
		}

		// this will only run (the OnScrollListener will only be set) if the helper is attached to a fragment implementing StickyHeader
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			int position = firstVisibleItem;

			// guaranteed to be at least one item after a date wrapper, and it will be a content item, 
			// because of how the adapter builds itself
			if (getItemViewType(position) == TYPE_DATE)
				position += 1;

			Content content = ((ContentWrapper) getItem(position)).content;

			helper.updateStickyHeader(dateFor(content));

			if (helper.auxScrollListener != null)
				helper.auxScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// pass on the scroll event if an auxiliary scroll listener is registered
			if (helper.auxScrollListener != null)
				helper.auxScrollListener.onScrollStateChanged(view, scrollState);
		}
    }

	public class ItemWrapper {}

	public class DateWrapper extends ItemWrapper {
		public Date date;
		public DateWrapper(Date date) { this.date = date; }
	}

	public class ContentWrapper extends ItemWrapper {
		public Content content;
		public boolean showTime;
		public ContentWrapper(Content content) { 
			this.content = content;
			this.showTime = false;
		}
	}
}