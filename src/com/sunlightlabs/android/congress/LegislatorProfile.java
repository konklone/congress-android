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
		
		String url = "http://govpix.appspot.com/" + Uri.encode(legislator.picName());
		InputStream stream;
		Drawable drawable;
		try {
			stream = (InputStream) fetch(url);
			drawable = Drawable.createFromStream(stream, "src");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
				
		picture.setImageDrawable(drawable);
		
		
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(legislator.getName());
	}
	
	public Object fetch(String address) throws MalformedURLException, IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}

	
}