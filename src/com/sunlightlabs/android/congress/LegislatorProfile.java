package com.sunlightlabs.android.congress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.CongressException;
import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Committee;

public class LegislatorProfile extends Activity {
	public static final String PIC_SMALL = "40x50";
	public static final String PIC_MEDIUM = "100x125";
	public static final String PIC_LARGE = "200x250";
	
	// 30 day expiration time on cached legislator avatars
	public static final long CACHE_IMAGES = (long) 1000 * 60 * 60 * 24 * 30;
	
	private String id, titledName, party, gender, state, domain, phone, website;
	private String apiKey;
	private Drawable avatar;
	private ImageView picture;
	private ArrayList<Committee> committees;
	
	private boolean landscape;
	
	private LoadPhotosTask loadPhotosTask = null;
	private LoadCommitteesTask loadCommitteesTask = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        landscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        setContentView(landscape ? R.layout.profile_landscape : R.layout.profile);
        
        apiKey = getResources().getString(R.string.sunlight_api_key);
        
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
        
        LegislatorProfileHolder holder = (LegislatorProfileHolder) getLastNonConfigurationInstance();
        if (holder != null)
        	holder.loadInto(this);
        
        loadPhotos();
        loadCommittees();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new LegislatorProfileHolder(loadPhotosTask, loadCommitteesTask, committees);
	}
	
	public void loadCommittees() {
		if (loadCommitteesTask != null)
			loadCommitteesTask.onScreenLoad(this);
		else {
			if (committees != null)
				displayCommittees();
			else
				loadCommitteesTask = (LoadCommitteesTask) new LoadCommitteesTask(this).execute(id);
		}
	}
    
	public void onLoadCommittees(CongressException exception) {
		Utils.alert(this, exception);
		//change committee list message to one of failure
	}
	
	public void onLoadCommittees(ArrayList<Committee> committees) {
		this.committees = committees;
		Utils.alert(this, "Loaded committees");
		displayCommittees();
	}
	
	public void displayCommittees() {
		
	}
	
	public void loadPhotos() {
		if (loadPhotosTask != null)
        	loadPhotosTask.onScreenLoad(this);
        else {
        	if (avatar != null)
        		displayAvatar();
        	else
        		loadPhotosTask = (LoadPhotosTask) new LoadPhotosTask(this).execute(id);
        }
	}
	
    public void displayAvatar() {
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
	
	public void loadInformation() {
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/AlteHaasGroteskRegular.ttf");
		
		picture = (ImageView) this.findViewById(R.id.profile_picture);
		
		TextView name = (TextView) this.findViewById(R.id.profile_name);
		name.setText(titledName);
		name.setTypeface(font);
		
		TextView partyView = (TextView) this.findViewById(R.id.profile_party); 
		partyView.setText(partyName(party));
		partyView.setTypeface(font);
		
		TextView stateView = (TextView) this.findViewById(R.id.profile_state);
		String stateName = stateName(state);
		stateView.setText(stateName);
		if (!landscape && stateName.equals("District of Columbia"))
			stateView.setTextSize(16);
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
		int density = context.getResources().getDisplayMetrics().densityDpi;
		Bitmap profile, scaled;
		switch (density) {
		case DisplayMetrics.DENSITY_LOW:
			profile = getImage(LegislatorProfile.PIC_SMALL, bioguideId, context).getBitmap();
			scaled = Bitmap.createScaledBitmap(profile, 32, 40, true);
			return Bitmap.createBitmap(scaled, 0, 2, scaled.getWidth(), scaled.getHeight() - 4);
		case DisplayMetrics.DENSITY_MEDIUM:
			// this will be a 40x50 image, that I want to turn into a 40x48 image
			profile = getImage(LegislatorProfile.PIC_SMALL, bioguideId, context).getBitmap();
			return Bitmap.createBitmap(profile, 0, 1, profile.getWidth(), profile.getHeight()-2);
		case DisplayMetrics.DENSITY_HIGH:
		default:
			// will be a 100x125 image, I want to scale it down to 60x75, and then chop 3 lines off of it
			profile = getImage(LegislatorProfile.PIC_MEDIUM, bioguideId, context).getBitmap();
			scaled = Bitmap.createScaledBitmap(profile, 60, 75, true);
			return Bitmap.createBitmap(scaled, 0, 1, scaled.getWidth(), scaled.getHeight() - 3);
		}
		
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
	
	public static String stateName(String code) {
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
	
	public static String stateCode(String name) {
		name = name.toLowerCase();
	    if (name.equals("alabama"))
	        return "AL";
	    if (name.equals("alaska"))
	        return "AK";
	    if (name.equals("arizona"))
	        return "AZ";
	    if (name.equals("arkansas"))
	        return "AR";
	    if (name.equals("california"))
	        return "CA";
	    if (name.equals("colorado"))
	        return "CO";
	    if (name.equals("connecticut"))
	        return "CT";
	    if (name.equals("delaware"))
	        return "DE";
	    if (name.equals("district of columbia"))
	        return "DC";
	    if (name.equals("florida"))
	        return "FL";
	    if (name.equals("georgia"))
	        return "GA";
	    if (name.equals("hawaii"))
	        return "HI";
	    if (name.equals("idaho"))
	        return "ID";
	    if (name.equals("illinois"))
	        return "IL";
	    if (name.equals("indiana"))
	        return "IN";
	    if (name.equals("iowa"))
	        return "IA";
	    if (name.equals("kansas"))
	        return "KS";
	    if (name.equals("kentucky"))
	        return "KY";
	    if (name.equals("louisiana"))
	        return "LA";
	    if (name.equals("maine"))
	        return "ME";
	    if (name.equals("maryland"))
	        return "MD";
	    if (name.equals("massachusetts"))
	        return "MA";
	    if (name.equals("michigan"))
	        return "MI";
	    if (name.equals("minnesota"))
	        return "MN";
	    if (name.equals("mississippi"))
	        return "MS";
	    if (name.equals("missouri"))
	        return "MO";
	    if (name.equals("montana"))
	        return "MT";
	    if (name.equals("nebraska"))
	        return "NE";
	    if (name.equals("nevada"))
	        return "NV";
	    if (name.equals("new hampshire"))
	        return "NH";
	    if (name.equals("new jersey"))
	        return "NJ";
	    if (name.equals("new mexico"))
	        return "NM";
	    if (name.equals("new york"))
	        return "NY";
	    if (name.equals("north carolina"))
	        return "NC";
	    if (name.equals("north dakota"))
	        return "ND";
	    if (name.equals("ohio"))
	        return "OH";
	    if (name.equals("oklahoma"))
	        return "OK";
	    if (name.equals("oregon"))
	        return "OR";
	    if (name.equals("pennsylvania"))
	        return "PA";
	    if (name.equals("puerto rico"))
	        return "PR";
	    if (name.equals("rhode island"))
	        return "RI";
	    if (name.equals("south carolina"))
	        return "SC";
	    if (name.equals("south dakota"))
	        return "SD";
	    if (name.equals("tennessee"))
	        return "TN";
	    if (name.equals("texas"))
	        return "TX";
	    if (name.equals("utah"))
	        return "UT";
	    if (name.equals("vermont"))
	        return "VT";
	    if (name.equals("virginia"))
	        return "VA";
	    if (name.equals("washington"))
	        return "WA";
	    if (name.equals("west virginia"))
	        return "WV";
	    if (name.equals("wisconsin"))
	        return "WI";
	    if (name.equals("wyoming"))
	        return "WY";
	    else
	        return null;
	}
	
	private class LoadPhotosTask extends AsyncTask<String,Void,Drawable> {
		public LegislatorProfile context;
		
		public LoadPhotosTask(LegislatorProfile context) {
			super();
			this.context = context;
		}
		
		public void onScreenLoad(LegislatorProfile context) {
			this.context = context;
		}
		
		@Override
		public Drawable doInBackground(String... bioguideId) {
			return LegislatorProfile.getImage(PIC_MEDIUM, bioguideId[0], context);
		}
		
		@Override
		public void onPostExecute(Drawable avatar) {
			context.avatar = avatar;
			context.displayAvatar();
			context.loadPhotosTask = null;
		}
	}
	
	private class LoadCommitteesTask extends AsyncTask<String,Void,ArrayList<Committee>> {
		private LegislatorProfile context;
		private CongressException exception;
		
		public LoadCommitteesTask(LegislatorProfile context) {
			this.context = context;
		}
		
		public void onScreenLoad(LegislatorProfile context) {
			this.context = context;
		}
		
		@Override
		public ArrayList<Committee> doInBackground(String... bioguideId) {
			ArrayList<Committee> committees = new ArrayList<Committee>();
			Committee[] temp;
			try {
				temp = Committee.getCommitteesForLegislator(new ApiCall(context.apiKey), bioguideId[0]);
			} catch (IOException e) {
				this.exception = new CongressException(e, "Error loading committees.");
				return null;
			}
			for (int i=0; i<temp.length; i++)
				committees.add(temp[i]);
			return committees;
		}
		
		@Override
		public void onPostExecute(ArrayList<Committee> committees) {
			context.loadCommitteesTask = null;
			
			if (exception != null && committees == null)
				context.onLoadCommittees(exception);
			else
				context.onLoadCommittees(committees);
		}
	}
	
	static class LegislatorProfileHolder {
		LoadPhotosTask loadPhotosTask;
		LoadCommitteesTask loadCommitteesTask;
		ArrayList<Committee> committees;
		
		LegislatorProfileHolder(LoadPhotosTask loadPhotosTask, LoadCommitteesTask loadCommitteesTask, ArrayList<Committee> committees) {
			this.loadPhotosTask = loadPhotosTask;
			this.loadCommitteesTask = loadCommitteesTask;
			this.committees = committees;
		}
		
		public void loadInto(LegislatorProfile context) {
			context.loadPhotosTask = loadPhotosTask;
			context.loadCommitteesTask = loadCommitteesTask;
			context.committees = committees;
		}
	}

}