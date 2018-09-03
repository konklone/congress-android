/***
	Copyright (c) 2008-2009 CommonsWare, LLC
	Portions (c) 2009 Google, Inc.
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may obtain
	a copy of the License at
		https://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/		

package com.commonsware.cwac.merge;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.commonsware.cwac.sacklist.SackOfViewsAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that merges multiple child adapters and views
 * into a single contiguous whole.
 *
 * Adapters used as pieces within MergeAdapter must
 * have view type IDs monotonically increasing from 0. Ideally,
 * adapters also have distinct ranges for their row ids, as
 * returned by getItemId().
 *
 */
public class MergeAdapter extends BaseAdapter {
	private ArrayList<ListAdapter> pieces = new ArrayList<>();

	/**
		* Stock constructor, simply chaining to the superclass.
    */
	public MergeAdapter() {
		super();
	}

	/**
		* Adds a new adapter to the roster of things to appear
		* in the aggregate list.
		* @param adapter Source for row views for this section
    */
	public void addAdapter(ListAdapter adapter) {
		pieces.add(adapter);
	}

	/**
		* Adds a new View to the roster of things to appear
		* in the aggregate list.
		* @param view Single view to add
    */
	public void addView(View view) {
		ArrayList<View> list = new ArrayList<>(1);
		
		list.add(view);
		
		addViews(list);
	}

	/**
		* Adds a list of views to the roster of things to appear
		* in the aggregate list.
		* @param views List of views to add
    */
	public void addViews(List<View> views) {
		pieces.add(new SackOfViewsAdapter(views));
	}

	/**
		* Get the data item associated with the specified
		* position in the data set.
		* @param position Position of the item whose data we want
    */
	
	public Object getItem(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				return(piece.getItem(position));
			}

			position-=size;
		}
		
		return(null);
	}

	/**
		* How many items are in the data set represented by this
		* Adapter.
    */
	
	public int getCount() {
		int total=0;
		
		for (ListAdapter piece : pieces) {
			total+=piece.getCount();
		}
		
		return(total);
	}

	/**
		* Returns the number of types of Views that will be
		* created by getView().
    */
	@Override
	public int getViewTypeCount() {
		int total=0;
		
		for (ListAdapter piece : pieces) {
			total+=piece.getViewTypeCount();
		}

		return (Math.max(total, 1)); // needed for setListAdapter() before content add'
	}

	/**
		* Get the type of View that will be created by getView()
		* for the specified item.
		* @param position Position of the item whose data we want
    */
	@Override
	public int getItemViewType(int position) {
		int typeOffset=0;
		int result=-1;
		
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				result=typeOffset+piece.getItemViewType(position);
				break;
			}

			position-=size;
			typeOffset+=piece.getViewTypeCount();
		}

		return(result);
	}

	/**
		* Are all items in this ListAdapter enabled? If yes it
		* means all items are selectable and clickable.
    */
	@Override
	public boolean areAllItemsEnabled() {
		return(false);
	}

	/**
		* Returns true if the item at the specified position is
		* not a separator.
		* @param position Position of the item whose data we want
    */
	@Override
	public boolean isEnabled(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				return(piece.isEnabled(position));
			}
			position-=size;
		}
		return(false);
	}

	/**
		* Get a View that displays the data at the specified
		* position in the data set.
		* @param position Position of the item whose data we want
		* @param convertView View to recycle, if not null
		* @param parent ViewGroup containing the returned View
    */
	
	public View getView(int position, View convertView, ViewGroup parent) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();

			if (position<size) {
				
				return(piece.getView(position, convertView, parent));
			}

			position-=size;
		}
		
		return(null);
	}

	/**
		* Get the row id associated with the specified position
		* in the list.
		* @param position Position of the item whose data we want
    */
	
	public long getItemId(int position) {
		for (ListAdapter piece : pieces) {
			int size=piece.getCount();
			
			if (position<size) {
				return(piece.getItemId(position));
			}

			position-=size;
		}
		return(-1);
	}
}