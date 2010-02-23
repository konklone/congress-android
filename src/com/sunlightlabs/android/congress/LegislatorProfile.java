package com.sunlightlabs.android.congress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
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
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.api.ApiCall;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.entities.Committee;

public class LegislatorProfile extends ListActivity {
	private String id, titledName, party, gender, state, domain, phone, website;
	private String apiKey;
	private Drawable avatar;
	private ImageView picture;
	private ArrayList<Committee> committees;
	
	// need to keep this here between setupControls() and displayCommittees(), not sure why
	private LinearLayout committeeHeader;
	
	private LoadPhotosTask loadPhotosTask = null;
	private LoadCommitteesTask loadCommitteesTask = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
        loadCommittees();
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
		displayCommittees();
	}
	
	public void onLoadCommittees(ArrayList<Committee> committees) {
		this.committees = committees;
		displayCommittees();
	}
	
	public void displayCommittees() {
		if (committees != null) {
			if (committees.size() > 0) {
				committeeHeader.findViewById(R.id.loading).setVisibility(View.GONE);
				MergeAdapter adapter = (MergeAdapter) getListAdapter();
				adapter.addAdapter(new CommitteeAdapter(this, committees));
				setListAdapter(adapter);
			} else {
				committeeHeader.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
				((TextView) committeeHeader.findViewById(R.id.loading_message)).setText("Belongs to no committees.");
			}
		} else {
			committeeHeader.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
			((TextView) committeeHeader.findViewById(R.id.loading_message)).setText("Error loading committees.");
		}
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
    	if (avatar != null)
    		picture.setImageDrawable(avatar);
    	else {
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
    	Object tag = v.getTag();
    	if (tag.getClass().getSimpleName().equals("Committee")) {
    		launchCommittee((Committee) tag);
    	} else {
    		String type = (String) tag;
	    	if (type.equals("phone"))
	    		callOffice();
	    	else if (type.equals("web"))
	    		visitWebsite();
    	}
    }
    
    public void callOffice() {
    	startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel://" + phone)));
    }
    
    public void visitWebsite() {
    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(website)));
    }
    
    public void launchCommittee(Committee committee) {
    	Intent intent = new Intent()
    		.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList")
			.putExtra("committeeId", committee.getProperty("id"))
			.putExtra("committeeName", committee.getProperty("name"));
		startActivity(intent);
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
		stateView.setTypeface(font);
		
		TextView domainView = (TextView) mainView.findViewById(R.id.profile_domain); 
		domainView.setText(domainName(domain));
		domainView.setTypeface(font);
		
		LinearLayout phoneView = (LinearLayout) inflater.inflate(R.layout.icon_list_item_2, null);
		TextView phoneText = (TextView) phoneView.findViewById(R.id.text_1);
		phoneText.setText("Call " + pronoun(gender) + " office");
		phoneText.setTypeface(font);
		TextView phoneNumber = (TextView) phoneView.findViewById(R.id.text_2);
		phoneNumber.setText(phone);
		phoneNumber.setTypeface(font);
		((ImageView) phoneView.findViewById(R.id.icon)).setImageResource(R.drawable.phone);
		phoneView.setTag("phone");
		
		LinearLayout websiteView = (LinearLayout) inflater.inflate(R.layout.icon_list_item_2, null);
		TextView websiteText = (TextView) websiteView.findViewById(R.id.text_1);
		websiteText.setText("Visit " + pronoun(gender) + " website");
		websiteText.setTypeface(font);
		TextView websiteUrl = (TextView) websiteView.findViewById(R.id.text_2);
		websiteUrl.setText(websiteName(website));
		websiteUrl.setTypeface(font);
		((ImageView) websiteView.findViewById(R.id.icon)).setImageResource(R.drawable.web);
		websiteView.setTag("web");
		
		ArrayList<View> contactViews = new ArrayList<View>(2);
		contactViews.add(phoneView);
		contactViews.add(websiteView);
		
		committeeHeader = (LinearLayout) inflater.inflate(R.layout.header_loading, null);
		((TextView) committeeHeader.findViewById(R.id.header_text)).setText("Committees");
		((TextView) committeeHeader.findViewById(R.id.loading_message)).setText("Loading committees...");
		
		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(mainView);
		adapter.addAdapter(new ViewArrayAdapter(this, contactViews));
		adapter.addView(committeeHeader);
		
		setListAdapter(adapter);
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
	
	protected class CommitteeAdapter extends ArrayAdapter<Committee> {
		LayoutInflater inflater;

        public CommitteeAdapter(Activity context, ArrayList<Committee> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			Committee committee = getItem(position);
			
			// ignoring convertView as a recycling possibility -
			// the list is too small to make the extra logic worth it
			LinearLayout view = (LinearLayout) inflater.inflate(R.layout.profile_committee, null);
			((TextView) view.findViewById(R.id.name)).setText(committee.getProperty("name"));
			view.setTag(committee);
			
			return view;
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
			return LegislatorImage.getImage(LegislatorImage.PIC_LARGE, bioguideId[0], context);
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
			ArrayList<Committee> joint = new ArrayList<Committee>();
			Committee[] temp;
			
			try {
				temp = Committee.getCommitteesForLegislator(new ApiCall(context.apiKey), bioguideId[0]);
			} catch (IOException e) {
				this.exception = new CongressException(e, "Error loading committees.");
				return null;
			}
			for (int i=0; i<temp.length; i++) {
				if (temp[i].getProperty("chamber").equals("Joint"))
					joint.add(temp[i]);
				else
					committees.add(temp[i]);
			}
			Collections.sort(committees);
			Collections.sort(joint);
			committees.addAll(joint);
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