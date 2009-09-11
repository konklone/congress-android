package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorProfile extends Activity {
	public static String LEGISLATOR_ID = "com.sunlightlabs.android.congress.legislator_id";
	private String id;
	private Legislator legislator;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        
        id = getIntent().getStringExtra(LEGISLATOR_ID);
        
        loadLegislator(id);
        loadInformation();
	}
	
	
	public void loadLegislator(String id) {
		ApiCall api = new ApiCall("");
		legislator = Legislator.getLegislatorById(api, id);
	}
	
	
	public void loadInformation() {
		ImageView picture = (ImageView) this.findViewById(R.id.picture);
		picture.setImageResource(R.drawable.no_photo);
		
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(legislator.getName());
	}
	
}