package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.CongressException;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.entities.Committee;

public class LegislatorProfile extends ListActivity {
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
        
        setupControls();
        
        LegislatorProfileHolder holder = (LegislatorProfileHolder) getLastNonConfigurationInstance();
        if (holder != null)
        	holder.loadInto(this);
        
        loadPhotos();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new LegislatorProfileHolder(loadPhotosTask, loadCommitteesTask, committees);
	}
	
	// committee callbacks and display function not being used at this time
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
	}
	
	public void onLoadCommittees(ArrayList<Committee> committees) {
		this.committees = committees;
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
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	String tag = (String) v.getTag();
    	if (tag.equals("phone"))
    		callOffice();
    	else if (tag.equals("web"))
    		visitWebsite();
    }
    
    public void callOffice() {
    	startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel://" + phone)));
    }
    
    public void visitWebsite() {
    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(website)));
    }
	
	public void setupControls() {
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/AlteHaasGroteskRegular.ttf");
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout mainView = (LinearLayout) inflater.inflate(R.layout.profile, null);
		mainView.setEnabled(false);
		
		picture = (ImageView) mainView.findViewById(R.id.profile_picture);
		
		TextView name = (TextView) mainView.findViewById(R.id.profile_name);
		name.setText(titledName);
		name.setTypeface(font);
		
		TextView partyView = (TextView) mainView.findViewById(R.id.profile_party); 
		partyView.setText(partyName(party));
		partyView.setTypeface(font);
		
		TextView stateView = (TextView) mainView.findViewById(R.id.profile_state);
		String stateName = Utils.stateCodeToName(this, state);
		stateView.setText(stateName);
		if (!landscape && stateName.equals("District of Columbia"))
			stateView.setTextSize(16);
		stateView.setTypeface(font);
		
		TextView domainView = (TextView) mainView.findViewById(R.id.profile_domain); 
		domainView.setText(domainName(domain));
		domainView.setTypeface(font);
		
		LinearLayout phoneView = (LinearLayout) inflater.inflate(R.layout.profile_contact, null);
		TextView phoneText = (TextView) phoneView.findViewById(R.id.text);
		phoneText.setText("Call " + pronoun(gender) + " office");
		phoneText.setTypeface(font);
		((ImageView) phoneView.findViewById(R.id.icon)).setImageResource(R.drawable.phone);
		phoneView.setTag("phone");
		
		LinearLayout websiteView = (LinearLayout) inflater.inflate(R.layout.profile_contact, null);
		TextView websiteText = (TextView) websiteView.findViewById(R.id.text);
		websiteText.setText("Visit " + pronoun(gender) + " website");
		websiteText.setTypeface(font);
		((ImageView) websiteView.findViewById(R.id.icon)).setImageResource(R.drawable.web);
		websiteView.setTag("web");
		
		ArrayList<View> contactViews = new ArrayList<View>();
		contactViews.add(phoneView);
		contactViews.add(websiteView);
		
		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(mainView);
		adapter.addAdapter(new ViewArrayAdapter(this, contactViews));
		setListAdapter(adapter);
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
	
	// For URLs that use subdomains (i.e. yarmuth.house.gov) return just that.
	// For URLs that use paths (i.e. house.gov/wu) return just that.
	// In both cases, remove the http://, the www., and any unneeded trailing stuff.
	public static String websiteName(String url) {
		String noPrefix = url.replaceAll("^http://(?:www\\.)?", "");
		
		String noSubdomain = "^((?:senate|house)\\.gov/.*?)/";
		Pattern pattern = Pattern.compile(noSubdomain);
		Matcher matcher = pattern.matcher(noPrefix);
		if (matcher.find())
			return matcher.group(1);
		else
			return noPrefix.replaceAll("/.*$", "");
	}
	
	public static String websiteLink(String url) {
		return "<a href=\"" + url + "\">" + websiteName(url) + "</a>";
	}
	
	public static String partyName(String code) {
		if (code.equals("D"))
			return "Democrat";
		if (code.equals("R"))
			return "Republican";
		if (code.equals("I"))
			return "Independent";
		else
			return "";
	}
	
	public static String domainName(String domain) {
		if (domain.equals("Upper Seat"))
			return "Senior Senator";
		if (domain.equals("Lower Seat"))
			return "Junior Senator";
		else
			return domain;
	}
	
	public static String pronoun(String gender) {
		if (gender.equals("M"))
			return "his";
		else // "F"
			return "her";
	}
	
	// dirt simple class, to give views the selectable appearance on click and long click
	// that one expects from listviews, but made to be easily dumped into a MergeAdapter
	// for use in the middle of a large scrollable pane
	private class ViewArrayAdapter extends ArrayAdapter<View> {

        public ViewArrayAdapter(Activity context, ArrayList<View> items) {
            super(context, 0, items);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			return getItem(position);
		}
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
			return LegislatorImage.getImage(LegislatorImage.PIC_MEDIUM, bioguideId[0], context);
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