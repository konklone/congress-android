package com.sunlightlabs.android.congress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class LegislatorProfile extends Activity {
	public static final String PIC_SMALL = "40x50";
	public static final String PIC_MEDIUM = "100x125";
	public static final String PIC_LARGE = "200x250";
	
	private String id, titledName, party, state, domain, phone, website, office;
	private Drawable avatar;
	private ImageView picture;
	
	private static final String avatarPath = "/sdcard/sunlight-android/avatars/";
	
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
    		picture.setImageDrawable(avatar);
    		bindAvatar();
        }
    };
	
	public void loadInformation() {
		picture = (ImageView) this.findViewById(R.id.profile_picture);
		BitmapDrawable file = quickGetImage(PIC_MEDIUM, id);
		if (file != null)
			picture.setImageDrawable(file);
		else
			picture.setImageResource(R.drawable.loading_photo);
		
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(titledName);
		
		TextView partyView = (TextView) this.findViewById(R.id.profile_party); 
		partyView.setText(partyName(party));
		
		TextView stateView = (TextView) this.findViewById(R.id.profile_state); 
		stateView.setText(stateName(state));
		
		TextView domainView = (TextView) this.findViewById(R.id.profile_domain); 
		domainView.setText(domainName(domain));

		TextView phoneView = (TextView) this.findViewById(R.id.profile_phone);
		phoneView.setText(phone);
		Linkify.addLinks(phoneView, Linkify.PHONE_NUMBERS);
		
		TextView websiteView = (TextView) this.findViewById(R.id.profile_website);
		websiteView.setText(websiteName(website));
		Linkify.addLinks(websiteView, Linkify.WEB_URLS);
	}
	
	// needs to only be called when avatars have been downloaded and cached
	private void bindAvatar() {
		picture.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent avatarIntent = new Intent(LegislatorProfile.this, Avatar.class);
				Bundle extras = new Bundle();
				extras.putString("id", id);
				avatarIntent.putExtras(extras);
				
				startActivity(avatarIntent);
			}
		});
	}
	
	public void loadImage() {
		Thread loadingThread = new Thread() {
			public void run() {
				Drawable drawable = getImage(PIC_MEDIUM, id);
				if (drawable != null)
					avatar = drawable;
				else
					avatar = getResources().getDrawable(R.drawable.no_photo);
				
				handler.post(updateThread);
			}
		};
		loadingThread.start();
	}
	
	/*
	 * Various static methods that other classes can use to fetch legislator profile images,
	 * and cause them to be downloaded and cached to disk.
	 */
	
	public static BitmapDrawable getImage(String size, String bioguideId) {
		initializeDirectories(bioguideId);
		
		File imageFile = new File(picPath(size, bioguideId));
		if (!imageFile.exists())
			cacheImages(bioguideId);
		
		return new BitmapDrawable(picPath(size, bioguideId));
	}
	
	// Quick checks the disk for an avatar - if it isn't loaded
	public static BitmapDrawable quickGetImage(String size, String bioguideId) {
		File imageFile = new File(picPath(size, bioguideId));
		if (imageFile.exists())
			return new BitmapDrawable(picPath(size, bioguideId));
		else
			return null;
	}
	
	private static void initializeDirectories(String bioguideId) {
		File avatarDir = new File(avatarPath);
		if (!avatarDir.exists())
			avatarDir.mkdirs();
		
		File legislatorDir = new File(picDir(bioguideId));
		if (!legislatorDir.exists())
			legislatorDir.mkdir();
	}
	
	private static String picUrl(String size, String bioguideId) {
		return "http://assets.sunlightfoundation.com/moc/" + size + "/" + bioguideId + ".jpg";
	}
	
	private static String picPath(String size, String bioguideId) {
		return picDir(bioguideId) + size + ".jpg"; 
	}
	
	private static String picDir(String bioguideId) {
		return avatarPath + bioguideId + "/";
	}
	
	private static void cacheImages(String bioguideId) {
		cacheImage(PIC_SMALL, bioguideId);
		cacheImage(PIC_MEDIUM, bioguideId);
		cacheImage(PIC_LARGE, bioguideId);
	}
	
	private static void cacheImage(String size, String bioguideId) {
		File outFile = new File(picPath(size, bioguideId));
		if (outFile.exists())
			return;
		
		String url = picUrl(size, bioguideId);
		downloadFile(url, outFile);
	}
	
	private static InputStream fetchStream(String address) {
		try {
			URL url = new URL(address);
			return ((InputStream) url.getContent());
		} catch (Exception e) {
			return null;
		}
	}
	
	private static void downloadFile(String url, File outputFile) {
		try {
			URL u = new URL(url);
			URLConnection conn = u.openConnection();
			int contentLength = conn.getContentLength();
			
			DataInputStream stream = new DataInputStream(u.openStream());
			
	        byte[] buffer = new byte[contentLength];
	        stream.readFully(buffer);
	        stream.close();
	        
	        DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
	        fos.write(buffer);
	        fos.flush();
	        fos.close();
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String websiteName(String url) {
		return url.replace("http://", "").replace("/", "");
	}
	
	private String partyName(String code) {
		if (code.equals("D"))
			return "Democrat";
		if (code.equals("R"))
			return "Republican";
		if (code.equals("I"))
			return "Independent";
		else
			return "";
	}
	
	private String domainName(String domain) {
		if (domain.equals("Upper Seat"))
			return "Senior Senator";
		if (domain.equals("Lower Seat"))
			return "Junior Senator";
		else
			return domain;
	}
	
	private String stateName(String code) {
		if (code.equals("AL"))
	        return "Alabama";
	    if (code.equals("AK"))
	        return "Alaska";
	    if (code.equals("AZ"))
	        return "Arizona";
	    if (code.equals("AR"))
	        return "Arkansas";
	    if (code.equals("CA"))
	        return "California";
	    if (code.equals("CO"))
	        return "Colorado";
	    if (code.equals("CT"))
	        return "Connecticut";
	    if (code.equals("DE"))
	        return "Delaware";
	    if (code.equals("DC"))
	        return "District of Columbia";
	    if (code.equals("FL"))
	        return "Florida";
	    if (code.equals("GA"))
	        return "Georgia";
	    if (code.equals("HI"))
	        return "Hawaii";
	    if (code.equals("ID"))
	        return "Idaho";
	    if (code.equals("IL"))
	        return "Illinois";
	    if (code.equals("IN"))
	        return "Indiana";
	    if (code.equals("IA"))
	        return "Iowa";
	    if (code.equals("KS"))
	        return "Kansas";
	    if (code.equals("KY"))
	        return "Kentucky";
	    if (code.equals("LA"))
	        return "Louisiana";
	    if (code.equals("ME"))
	        return "Maine";
	    if (code.equals("MD"))
	        return "Maryland";
	    if (code.equals("MA"))
	        return "Massachusetts";
	    if (code.equals("MI"))
	        return "Michigan";
	    if (code.equals("MN"))
	        return "Minnesota";
	    if (code.equals("MS"))
	        return "Mississippi";
	    if (code.equals("MO"))
	        return "Missouri";
	    if (code.equals("MT"))
	        return "Montana";
	    if (code.equals("NE"))
	        return "Nebraska";
	    if (code.equals("NV"))
	        return "Nevada";
	    if (code.equals("NH"))
	        return "New Hampshire";
	    if (code.equals("NJ"))
	        return "New Jersey";
	    if (code.equals("NM"))
	        return "New Mexico";
	    if (code.equals("NY"))
	        return "New York";
	    if (code.equals("NC"))
	        return "North Carolina";
	    if (code.equals("ND"))
	        return "North Dakota";
	    if (code.equals("OH"))
	        return "Ohio";
	    if (code.equals("OK"))
	        return "Oklahoma";
	    if (code.equals("OR"))
	        return "Oregon";
	    if (code.equals("PA"))
	        return "Pennsylvania";
	    if (code.equals("PR"))
	        return "Puerto Rico";
	    if (code.equals("RI"))
	        return "Rhode Island";
	    if (code.equals("SC"))
	        return "South Carolina";
	    if (code.equals("SD"))
	        return "South Dakota";
	    if (code.equals("TN"))
	        return "Tennessee";
	    if (code.equals("TX"))
	        return "Texas";
	    if (code.equals("UT"))
	        return "Utah";
	    if (code.equals("VT"))
	        return "Vermont";
	    if (code.equals("VA"))
	        return "Virginia";
	    if (code.equals("WA"))
	        return "Washington";
	    if (code.equals("WV"))
	        return "West Virginia";
	    if (code.equals("WI"))
	        return "Wisconsin";
	    if (code.equals("WY"))
	        return "Wyoming";
	    else
	        return null;
	}
	
}