package com.sunlightlabs.android.congress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class LegislatorProfile extends Activity {
	public static final String PIC_SMALL = "40x50";
	public static final String PIC_MEDIUM = "100x125";
	public static final String PIC_LARGE = "200x250";
	
	// 30 day expiration time on cached legislator avatars
	public static final long CACHE_IMAGES = (long) 1000 * 60 * 60 * 24 * 30;
	
	private String id, titledName, party, gender, state, domain, phone, website;
	private Drawable avatar;
	private ImageView picture;
	
	private boolean landscape;
	
	private Typeface font;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        landscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        setContentView(landscape ? R.layout.profile_landscape : R.layout.profile);
        
        font = Typeface.createFromAsset(getAssets(), "fonts/AlteHaasGroteskRegular.ttf");
        
        Bundle extras = getIntent().getExtras(); 
        
        id = extras.getString("id");
        titledName = extras.getString("titledName");
        party = extras.getString("party");
        state = extras.getString("state");
        gender = extras.getString("gender");
        domain = extras.getString("domain");
        phone = extras.getString("phone");
        website = extras.getString("website");
        
        loadInformation();
        loadImage();
	}
	
	final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {
        	if (avatar != null) {
	    		picture.setImageDrawable(avatar);
	    		bindAvatar();
        	} else {
        		if (gender.equals("M"))
					avatar = getResources().getDrawable(R.drawable.no_photo_male);
				else // "F"
					avatar = getResources().getDrawable(R.drawable.no_photo_female);
        		picture.setImageDrawable(avatar);
        		// do not bind a click event to the "no photo" avatar
        	}
        }
    };
	
	public void loadInformation() {
		picture = (ImageView) this.findViewById(R.id.profile_picture);
		
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(titledName);
		name.setTypeface(font);
		
		TextView partyView = (TextView) this.findViewById(R.id.profile_party); 
		partyView.setText(partyName(party));
		partyView.setTypeface(font);
		
		TextView stateView = (TextView) this.findViewById(R.id.profile_state);
		String stateName = stateName(state);
		stateView.setText(stateName(state));
		if (!landscape && stateName.equals("District of Columbia"))
			stateView.setTextSize(18);
		stateView.setTypeface(font);
		
		TextView domainView = (TextView) this.findViewById(R.id.profile_domain); 
		domainView.setText(domainName(domain));
		domainView.setTypeface(font);
		
		TextView phoneView = (TextView) this.findViewById(R.id.profile_phone);
		phoneView.setText(phone);
		phoneView.setTypeface(font);
		Linkify.addLinks(phoneView, Linkify.PHONE_NUMBERS);
		
		TextView websiteView = (TextView) this.findViewById(R.id.profile_website);
		websiteView.setText(Html.fromHtml(websiteLink(website)));
		websiteView.setMovementMethod(LinkMovementMethod.getInstance());
		websiteView.setTypeface(font);
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
				avatar = getImage(PIC_MEDIUM, id, LegislatorProfile.this);
				handler.post(updateThread);
			}
		};
		loadingThread.start();
	}
	
	/*
	 * Various static methods that other classes can use to fetch legislator profile images,
	 * and cause them to be downloaded and cached to disk.
	 */
	
	public static BitmapDrawable getImage(String size, String bioguideId, Context context) {
		File imageFile = new File(picPath(size, bioguideId, context));
		
		if (!imageFile.exists())
			cacheImages(bioguideId, context);
		else if (tooOld(imageFile))
			cacheImages(bioguideId, context);
		
		
		if (!imageFile.exists()) // download failed for some reason
			return null;
		
		return new BitmapDrawable(picPath(size, bioguideId, context));
	}
	
	public static Bitmap shortcutImage(String bioguideId, Context context) {
		Bitmap small = getImage(LegislatorProfile.PIC_SMALL, bioguideId, context).getBitmap();
		// this will be a 40x50 image, that I want to turn into a 40x48 image
		return Bitmap.createBitmap(small, 0, 1, small.getWidth(), small.getHeight()-2);
	}
	
	// assumes you've already checked to make sure the file exists
	public static boolean tooOld(File file) {
		return file.lastModified() < (System.currentTimeMillis() - CACHE_IMAGES);
	}
	
	private static String picUrl(String size, String bioguideId) {
		return "http://assets.sunlightfoundation.com/moc/" + size + "/" + bioguideId + ".jpg";
	}
	
	private static String picPath(String size, String bioguideId, Context context) {
		return picDir(bioguideId, context) + size + ".jpg"; 
	}
	
	private static String picDir(String bioguideId, Context context) {
		return context.getDir(bioguideId, Context.MODE_PRIVATE).getPath();
	}
	
	private static void cacheImages(String bioguideId, Context context) {
		cacheImage(PIC_SMALL, bioguideId, context);
		cacheImage(PIC_MEDIUM, bioguideId, context);
		cacheImage(PIC_LARGE, bioguideId, context);
	}
	
	private static void cacheImage(String size, String bioguideId, Context context) {
		File outFile = new File(picPath(size, bioguideId, context));
		if (outFile.exists())
			outFile.delete();
		
		String url = picUrl(size, bioguideId);
		downloadFile(url, outFile);
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
			return; // swallow a 404, we'll throw up a No Photo image
		} catch (IOException e) {
			return; // swallow a 404, we'll throw up a No Photo image
		}
	}
	
	// For URLs that use subdomains (i.e. yarmuth.house.gov) return just that.
	// For URLs that use paths (i.e. house.gov/wu) return just that.
	// In both cases, remove the http://, the www., and any unneeded trailing stuff.
	private String websiteName(String url) {
		String noPrefix = url.replaceAll("^http://(?:www\\.)?", "");
		
		String noSubdomain = "^((?:senate|house)\\.gov/.*?)/";
		Pattern pattern = Pattern.compile(noSubdomain);
		Matcher matcher = pattern.matcher(noPrefix);
		if (matcher.find())
			return matcher.group(1);
		else
			return noPrefix.replaceAll("/.*$", "");
	}
	
	private String websiteLink(String url) {
		return "<a href=\"" + url + "\">" + websiteName(url) + "</a>";
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