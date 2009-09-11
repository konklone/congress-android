package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorList extends Activity {
	private ApiCall api;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Put API Key here
        api = new ApiCall("");
        
        setupControls();
    }
    
    public void setupControls() {
        Button fetch = (Button) this.findViewById(R.id.fetch);
        final EditText zip = (EditText) this.findViewById(R.id.zip);
        
    	fetch.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			fetch(zip.getText().toString());
    		}
    	});
    }
    
    public void fetch(String zipCode) {
        Legislator[] legislators = Legislator.getLegislatorsForZipCode(api, zipCode);
        TextView debug = (TextView) this.findViewById(R.id.debug);
        
        if (legislators.length > 0)
        	debug.setText(legislators[0].getName());
        else
        	debug.setText("No legislators found");
    }
    
    public void launchProfile(String id) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorProfile");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		Bundle extras = new Bundle();
		extras.putString(LegislatorProfile.LEGISLATOR_ID, id); 
		i.putExtras(extras);
		
		startActivity(i);
    }
}