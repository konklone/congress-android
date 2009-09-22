package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainMenu extends Activity {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupControls();
    }
	
	public void setupControls() {
        Button fetch = (Button) this.findViewById(R.id.fetch);
        final EditText zip = (EditText) this.findViewById(R.id.zip);
        
    	fetch.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			String zipCode = zip.getText().toString();
    			searchByZip(zipCode);
    		}
    	});
    }
	
	public void searchByZip(String zipCode) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode); 
		i.putExtras(extras);
		
		startActivity(i);
    }
}