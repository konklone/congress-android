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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

public class LegislatorProfile extends Activity {
	public static final String PIC_SMALL = "40x50";
	public static final String PIC_MEDIUM = "100x125";
	public static final String PIC_LARGE = "200x250";
	
	private String id, titledName, party, state, domain, phone, website, office;
	private Drawable avatar;
	
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
    		ImageView picture = (ImageView) LegislatorProfile.this.findViewById(R.id.picture);
    		picture.setImageDrawable(avatar);
        }
    };
	
	public void loadInformation() {		
		// avatar (loading photo)
		ImageView photo = (ImageView) this.findViewById(R.id.picture);
		photo.setImageResource(R.drawable.loading_photo);
		
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
	
}