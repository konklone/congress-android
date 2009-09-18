package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorProfile extends Activity {
	private String picName, titledName, party, state, domain, phone, website, office, youtube_url, twitter_id; 
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator_profile);
        
        Bundle extras = getIntent().getExtras(); 
        
        picName = extras.getString("picName");
        titledName = extras.getString("titledName");
        party = extras.getString("party");
        state = extras.getString("state");
        domain = extras.getString("domain");
        phone = extras.getString("phone");
        website = extras.getString("website");
        office = extras.getString("office");
        youtube_url = extras.getString("youtube_url");
        twitter_id = extras.getString("twitter_id");
        
        loadInformation();
	}
	
	public void loadInformation() {
		ImageView picture = (ImageView) this.findViewById(R.id.picture);
		picture.setImageDrawable(fetchImage());
		
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
	
		
//		// twitter handle
//		String twitter_id = legislator.getProperty("twitter_id");
//		if (!twitter_id.equals("")) {
//			TextView twitter = (TextView) this.findViewById(R.id.profile_twitter);
//			twitter.setText("Twitter: @" + twitter_id);
//		}
//		
//		// YouTube account
//		String youtube_url = legislator.getProperty("youtube_url");
//		if (!youtube_url.equals("")) {
//			TextView youtube = (TextView) this.findViewById(R.id.profile_youtube);
//			youtube.setText("YouTube: " + youtube_url.replaceFirst("http://(?:www\\.)?youtube\\.com/", ""));
//		}
	}
	
	public Drawable fetchImage() {
		String url = "http://govpix.appspot.com/" + Uri.encode(picName);
		InputStream stream;
		Drawable drawable = null;
		try {
			stream = (InputStream) fetchObject(url);
			drawable = Drawable.createFromStream(stream, "src");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return drawable;
	}
	
	private Object fetchObject(String address) throws MalformedURLException, IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}

	
}