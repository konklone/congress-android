package com.sunlightlabs.android.congress;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenu extends Activity {
	public static final int RESULT_ZIP = 1;
	private static final int RESULT_SHORTCUT = 2;
	private Location location;

	// whether the user has come to this activity looking to create a shortcut
	private boolean shortcut = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadLocation();
        setupControls();

        String action = getIntent().getAction();
        if (action != null && action.equals(Intent.ACTION_CREATE_SHORTCUT))
        	shortcut = true;
    }
	
	public void loadLocation() {
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		location = null;
		List<String> enabled = lm.getProviders(true);
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
	public void setupControls() {
        Button fetchZip = (Button) this.findViewById(R.id.fetch_zip);
        Button fetchLocation = (Button) this.findViewById(R.id.fetch_location);
        
    	fetchZip.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			Intent intent = new Intent();
    			intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetZip");
    			startActivityForResult(intent, RESULT_ZIP);
    		}
    	});
    	
    	if (location != null) {
	    	fetchLocation.setOnClickListener(new View.OnClickListener() {
	    		public void onClick(View v) {
	    			if (location != null)
	    				searchByLatLong(location.getLatitude(), location.getLongitude());
	    		}
	    	});
    	} else
    		fetchLocation.setEnabled(false);
    }
	
	public void searchByZip(String zipCode) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode);
		extras.putBoolean("shortcut", shortcut);
		i.putExtras(extras);
		
		if (shortcut)
			startActivityForResult(i, RESULT_SHORTCUT);
		else
			startActivity(i);
    }
	
	public void searchByLatLong(double latitude, double longitude) {
		Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putDouble("latitude", latitude);
		extras.putDouble("longitude", longitude);
		extras.putBoolean("shortcut", shortcut);
		i.putExtras(extras);
		
		if (shortcut)
			startActivityForResult(i, RESULT_SHORTCUT);
		else
			startActivity(i);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_ZIP:
			if (resultCode == RESULT_OK) {
				String zipCode = data.getExtras().getString("zip_code");
				searchByZip(zipCode);
			}
			break;
		case RESULT_SHORTCUT:
			if (resultCode == RESULT_OK) {
				String legislatorName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
				setResult(RESULT_OK, data);
	    		finish();
			}
			break;
		}
	}
}