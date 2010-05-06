package com.sunlightlabs.android.congress.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.congress.java.Legislator;

public class LegislatorAdapter extends ArrayAdapter<Legislator> {
	LayoutInflater inflater;
	Activity context;

    public LegislatorAdapter(Activity context, ArrayList<Legislator> items) {
        super(context, 0, items);
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout view;
		
		if (convertView == null)
			view = (LinearLayout) inflater.inflate(R.layout.legislator_item, null);
		else
			view = (LinearLayout) convertView;
			
		Legislator legislator = getItem(position);
		((TextView) view.findViewById(R.id.name)).setText(nameFor(legislator));
		((TextView) view.findViewById(R.id.position)).setText(positionFor(legislator));
		
		return view;
	}
	
	public String nameFor(Legislator legislator) {
		return legislator.last_name + ", " + legislator.firstName();
	}
	
	public String positionFor(Legislator legislator) {
		String district = legislator.district;
		String stateName = Utils.stateCodeToName(context, legislator.state);
		
		if (district.equals("Senior Seat"))
			return "Senior Senator from " + stateName;
		else if (district.equals("Junior Seat"))
			return "Junior Senator from " + stateName;
		else if (district.equals("0")) {
			if (legislator.title.equals("Rep"))
				return "Representative for " + stateName + " At-Large";
			else
				return legislator.fullTitle() + " for " + stateName;
		} else
			return "Representative for " + stateName + "-" + district;
	}
	
}