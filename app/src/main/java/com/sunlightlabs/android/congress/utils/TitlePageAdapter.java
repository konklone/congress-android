package com.sunlightlabs.android.congress.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.support.v13.app.FragmentPagerAdapter;
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
	private Map<String,Integer> positionsByHandle = new HashMap<String,Integer>();
	
	private ViewGroup mainView;
	private Map<String,View> titleViews = new HashMap<String,View>();
	
	// track whether each page has been seen yet, to support the onPageSelectedOnce callback on an activity
	private Map<String,Boolean> selectedYet = new HashMap<String,Boolean>();
	
	String currentHandle;
	
	private ViewPager pager;
	private Activity activity;
	
	public TitlePageAdapter(Activity activity) {
        super(activity.getFragmentManager());
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
		notifyDataSetChanged();
		
		final int position = fragments.size() - 1;
		
		positionsByHandle.put(handle, position);
		selectedYet.put(handle, false);
		
		View titleView = LayoutInflater.from(activity).inflate(R.layout.pager_tab, null);
		((TextView) titleView.findViewById(R.id.tab_name)).setText(title);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		titleView.findViewById(R.id.inner_tab).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pager.setCurrentItem(position);
			}
		});
		
		mainView.addView(titleView, params);
		titleViews.put(handle, titleView);
		
		// mark default handle as on, or the first item
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
    
    public void pageSelected(int position) {
    	String newHandle = handles.get(position);
    	
//    	if (!selectedYet.get(newHandle)) {
//    		selectedYet.put(newHandle, true);
//    		Fragment fragment = fragments.get(position);
//    		if (fragment instanceof SelectedOnce) {
//    			((SelectedOnce) fragment).onSelectedOnce();
//    			Log.d(Utils.TAG, "Let fragment by handle " + newHandle + " know about first selection");
//    		}
//    	}
    	Log.d(Utils.TAG, "Selected page with handle " + newHandle);
    	
    	markOff(currentHandle);
    	markOn(newHandle);
    	
    	currentHandle = newHandle;
    }
    
    public void selectPage(int position) {
    	pager.setCurrentItem(position);
    }
    
    public void selectPage(String handle) {
    	selectPage(positionsByHandle.get(handle));
    }
    
    private void markOff(String handle) {
    	titleViews.get(handle).findViewById(R.id.tab_line).setVisibility(View.INVISIBLE);
    }
    
    private void markOn(String handle) {
    	titleViews.get(handle).findViewById(R.id.tab_line).setVisibility(View.VISIBLE);
    }
    
    public interface SelectedOnce {
    	void onSelectedOnce();
    }
    
    private static class TitlePageListener extends ViewPager.SimpleOnPageChangeListener {
    	TitlePageAdapter adapter;
    	
    	public TitlePageListener(TitlePageAdapter adapter) {
    		this.adapter = adapter;
    	}
    	
    	@Override
    	public void onPageSelected(int position) {
    		adapter.pageSelected(position);
    	}
    }
}