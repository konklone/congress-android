package com.sunlightlabs.android.congress.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public abstract class DateAdapterHelper<Content> {
	Fragment context;
	
	public Resources resources;
	public LayoutInflater inflater;
	
	public DateAdapterHelper(Fragment context) {
		this.context = context;
		this.inflater = LayoutInflater.from(context.getActivity());
		this.resources = context.getResources();
	}
	
	public DateAdapter adapterFor(List<Content> contents) {
		return new DateAdapter(this, context, processContents(contents));
	}
	
	abstract public Date dateFor(Content action);
	
	abstract public View contentView(ContentWrapper wrapper);
	
	// Optionally override this
	public View dateView(DateWrapper wrapper) {
		return Utils.dateView(context.getActivity(), wrapper.date, Utils.shortDateThisYear(wrapper.date));
	}
	
	private List<ItemWrapper> processContents(List<Content> contents) {
		List<ItemWrapper> items = new ArrayList<ItemWrapper>();
		
		int currentMonth = -1;
		int currentDay = -1;
		int currentYear = -1;
		
		for (int i=0; i<contents.size(); i++) {
			Content content = contents.get(i);
			
			Date contentDate = dateFor(content);
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(contentDate);
			int month = calendar.get(Calendar.MONTH);
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int year = calendar.get(Calendar.YEAR);
			
			if (currentMonth != month || currentDay != day || currentYear != year) {
				
				items.add(new DateWrapper(contentDate));
				
				currentMonth = month;
				currentDay = day;
				currentYear = year;
			}
			
			items.add(new ContentWrapper(content));
		}
		
		return items;
	}
	
	class DateAdapter extends ArrayAdapter<DateAdapterHelper<Content>.ItemWrapper> {
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
				return dateView((DateWrapper) item);
			else 
				return contentView((ContentWrapper) item);
		}
		
    }
	
	public class ItemWrapper {}
	
	public class DateWrapper extends ItemWrapper {
		public Date date;
		public DateWrapper(Date date) { this.date = date; }
	}
	
	public class ContentWrapper extends ItemWrapper {
		public Content content;
		public ContentWrapper(Content content) { this.content = content; }
	}
}