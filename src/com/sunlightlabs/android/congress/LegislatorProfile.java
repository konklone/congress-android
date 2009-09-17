package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.TabActivity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Legislator;

public class LegislatorProfile extends TabActivity {
	public static String LEGISLATOR_ID = "com.sunlightlabs.android.congress.legislator_id";
	private TabHost tabHost;
	private String id;
	private Legislator legislator;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legislator);
        
        id = getIntent().getStringExtra(LEGISLATOR_ID);
        
        setupTabs();
        
        loadLegislator(id);
        loadInformation();
	}
	
	
	public void setupTabs() {
		tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("profile_tab").setIndicator("Profile").setContent(R.id.profile_tab));
		tabHost.addTab(tabHost.newTabSpec("news_tab").setIndicator("News").setContent(R.id.news_tab));
		tabHost.setCurrentTab(0);
	}
	
	public void loadLegislator(String id) {
		String api_key = getResources().getString(R.string.sunlight_api_key);
		ApiCall api = new ApiCall(api_key);
		legislator = Legislator.getLegislatorById(api, id);
	}
	
	
	public void loadInformation() {
		ImageView picture = (ImageView) this.findViewById(R.id.picture);
		picture.setImageDrawable(fetchImage());
		
		// name
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(legislator.titledName());
		
		// party and state
		TextView party_state = (TextView) this.findViewById(R.id.profile_party_state);
		String party_line = 
				"(" + legislator.getProperty("party") + "-" + legislator.getProperty("state") + ") " 
				+ legislator.getDomain(); 
		party_state.setText(party_line);
	
		
		// website
		TextView website = (TextView) this.findViewById(R.id.profile_website);
		website.setText(legislator.getProperty("website"));
		
		// phone
		TextView phone = (TextView) this.findViewById(R.id.profile_phone);
		phone.setText(legislator.getProperty("phone"));
		
		// office address
		TextView office = (TextView) this.findViewById(R.id.profile_office);
		office.setText(legislator.getProperty("congress_office"));
		
		// twitter handle
		String twitter_id = legislator.getProperty("twitter_id");
		if (!twitter_id.equals("")) {
			TextView twitter = (TextView) this.findViewById(R.id.profile_twitter);
			twitter.setText("@" + twitter_id);
		}
		
		// YouTube account
		String youtube_url = legislator.getProperty("youtube_url");
		if (!youtube_url.equals("")) {
			TextView youtube = (TextView) this.findViewById(R.id.profile_youtube);
			youtube.setText(youtube_url);
		}
	}
	
	public Drawable fetchImage() {
		String url = "http://govpix.appspot.com/" + Uri.encode(legislator.picName());
		InputStream stream;
		Drawable drawable = null;
		try {
			stream = (InputStream) fetch(url);
			drawable = Drawable.createFromStream(stream, "src");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return drawable;
	}
	
	public Object fetch(String address) throws MalformedURLException, IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}

	
}