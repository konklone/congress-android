package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

public class LegislatorProfile extends Activity {
	private String id, titledName, party, state, domain, phone, website, office;
	private Drawable avatar;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        
        Bundle extras = getIntent().getExtras(); 
        
        id = extras.getString("id");
        titledName = extras.getString("titledName");
        party = extras.getString("party");
        state = extras.getString("state");
        domain = extras.getString("domain");
        phone = extras.getString("phone");
        website = extras.getString("website");
        office = extras.getString("office");
        
        loadInformation();
        loadImage();
	}
	
	final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
    		ImageView picture = (ImageView) LegislatorProfile.this.findViewById(R.id.picture);
    		picture.setImageDrawable(avatar);
        }
    };
	
	public void loadInformation() {		
		// name
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(titledName);
		
		// party and state
		TextView party_state = (TextView) this.findViewById(R.id.profile_party_state);
		String party_line = "(" + party + "-" + state + ") " + domain; 
		party_state.setText(party_line);

		// phone
		TextView phoneView = (TextView) this.findViewById(R.id.profile_phone);
		phoneView.setText(phone);
		
		// office address
		TextView officeView = (TextView) this.findViewById(R.id.profile_office);
		officeView.setText(office);
		
		// website
		TextView websiteView = (TextView) this.findViewById(R.id.profile_website);
		websiteView.setText(website);
	}
	
	public void loadImage() {
		Thread loadingThread = new Thread() {
			public void run() {
				String url = picUrl("100x125", id);
				InputStream stream = (InputStream) fetchObject(url);
				if (stream != null)
					avatar = Drawable.createFromStream(stream, "src");
				else
					avatar = getResources().getDrawable(R.drawable.no_photo);
				
				handler.post(updateThread);
			}
		};
		loadingThread.start();
	}
	
	private Object fetchObject(String address) {
		try {
			URL url = new URL(address);
			return url.getContent();
		} catch (Exception e) {
			return null;
		}
	}
	
	private String picUrl(String size, String bioguideId) {
		return "http://assets.sunlightfoundation.com/moc/" + size + "/" + bioguideId + ".jpg";
	}
	
}