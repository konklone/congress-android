package com.sunlightlabs.android.congress.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;

public class TitlePageAdapter extends FragmentPagerAdapter {
	private List<String> handles = new ArrayList<String>();
	private List<Fragment> fragments = new ArrayList<Fragment>();
	
	private ViewGroup mainView;
	private Map<String,View> titleViews = new HashMap<String,View>();
	String currentHandle;
	
	private ViewPager pager;
	private FragmentActivity activity;
	
	public TitlePageAdapter(FragmentActivity activity) {
        super(activity.getSupportFragmentManager());
        this.activity = activity;
        this.pager = (ViewPager) activity.findViewById(R.id.pager);
        
        pager.setAdapter(this);
        pager.setOnPageChangeListener(new TitlePageListener(this));
        
        mainView = (ViewGroup) activity.findViewById(R.id.pager_titles);
    }
	
	/**
	 * Adds a title with associated fragment to the adapter, and a handle to refer to it by.
	 * @return Position of the added fragment.
	 */
	
	public int add(String handle, int title, Fragment fragment) {
		return add(handle, activity.getResources().getString(title), fragment);
	}
	
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