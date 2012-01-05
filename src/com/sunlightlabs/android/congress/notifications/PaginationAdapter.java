package com.sunlightlabs.android.congress.notifications;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

public class PaginationAdapter implements OnScrollListener {
	
	public interface Paginates {
		public void loadNextPage(int page);
	}
	
	private Paginates context;
	private int page;

	public PaginationAdapter(Paginates context) {
		this.context = context;
		this.page = 1;
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
		int padding = 5; // padding from bottom
		if (visibleCount > 0 && (firstVisible + visibleCount + padding >= totalCount)) {
			page += 1;
			context.loadNextPage(page);
		}
    }
	
	@Override
    public void onScrollStateChanged(AbsListView v, int s) { }
}