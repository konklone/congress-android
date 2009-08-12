package com.sunlightlabs.android.congress;

import java.util.HashMap;
import java.util.Map;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LegislatorList extends Activity {
	private ApiCall api;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Put API Key here
        api = new ApiCall("");
        
        // Put zip code here
        Legislator[] legislators = Legislator.getLegislatorsForZipCode(api, "");
        
        TextView debug = (TextView) this.findViewById(R.id.debug);
        if (legislators.length > 0)
        	debug.setText(legislators[0].getName());
        else
        	debug.setText("No legislators found");
    }
}