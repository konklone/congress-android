package com.sunlightlabs.android.congress.notifications;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public class PaginationAdapter implements OnScrollListener {
	
	public interface Paginates {
		public void loadNextPage();
	}
	
	private Paginates context;

	public PaginationAdapter(Paginates context) {
		this.context = context;
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
		int padding = 5; // padding from bottom
		if (visibleCount > 0 && (firstVisible + visibleCount + padding >= totalCount))
			context.loadNextPage();
    }
	
	@Override
    public void onScrollStateChanged(AbsListView v, int s) { }
}