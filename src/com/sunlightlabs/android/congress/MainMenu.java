package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainMenu extends Activity {
	public static final int RESULT_ZIP = 1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupControls();
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
    	
    	fetchLocation.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
    			Location location = null;
    			if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
    				location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    			else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    				location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    			else
    				Toast.makeText(MainMenu.this, "Cannot detect your location. Visit your location settings to turn on WiFi or GPS location detection.", Toast.LENGTH_LONG).show();
    			
    			if (location != null)
    				searchByLatLong(location.getLatitude(), location.getLongitude());
    		}
    	});
    }
	
	public void searchByZip(String zipCode) {
    	Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode); 
		i.putExtras(extras);
		
		startActivity(i);
    }
	
	public void searchByLatLong(double latitude, double longitude) {
		Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		
		Bundle extras = new Bundle();
		extras.putDouble("latitude", latitude);
		extras.putDouble("longitude", longitude);
		i.putExtras(extras);
		
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
		}
	}
}