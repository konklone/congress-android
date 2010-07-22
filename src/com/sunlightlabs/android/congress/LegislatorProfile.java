package com.sunlightlabs.android.congress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.ShortcutImageTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.models.Committee;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.CommitteeService;

public class LegislatorProfile extends ListActivity implements LoadPhotoTask.LoadsPhoto, ShortcutImageTask.CreatesShortcutImage {
	private Legislator legislator;

	private Drawable avatar;
	private ImageView picture;
	private ArrayList<Committee> committees;
	
	// need to keep this here between setupControls() and displayCommittees(), not sure why
	private View committeeHeader;
	
	private LoadPhotoTask loadPhotoTask;
	private LoadCommitteesTask loadCommitteesTask;
	private ShortcutImageTask shortcutImageTask;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Utils.setupSunlight(this);
        
		legislator = (Legislator) getIntent().getExtras().getSerializable("legislator");
		
        setupControls();
        
        LegislatorProfileHolder holder = (LegislatorProfileHolder) getLastNonConfigurationInstance();
        if (holder != null)
        	holder.loadInto(this);
        
        loadPhoto();
        
        if (legislator.in_office)
        	loadCommittees();
        
        if (shortcutImageTask != null)
        	shortcutImageTask.onScreenLoad(this);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new LegislatorProfileHolder(loadPhotoTask, loadCommitteesTask, shortcutImageTask, committees);
	}
	
	// committee callbacks and display function not being used at this time
	public void loadCommittees() {
		if (loadCommitteesTask != null)
			loadCommitteesTask.onScreenLoad(this);
		else {
			if (committees != null)
				displayCommittees();
			else
				loadCommitteesTask = (LoadCommitteesTask) new LoadCommitteesTask(this)
						.execute(legislator.getId());
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
	
	
	public void loadPhoto() {
		if (loadPhotoTask != null)
        	loadPhotoTask.onScreenLoad(this);
        else {
        	if (avatar != null)
        		displayAvatar();
        	else
				loadPhotoTask = (LoadPhotoTask) new LoadPhotoTask(this, LegislatorImage.PIC_LARGE)
						.execute(legislator.getId());
        }
	}
	
	public void onLoadPhoto(Drawable avatar, Object tag) {
		loadPhotoTask = null;
		this.avatar = avatar;
		displayAvatar();
	}
	
	public void onCreateShortcutIcon(Bitmap icon) {
		Utils.installShortcutIcon(this, legislator, icon);
	}
	
	public Context getContext() {
		return this;
	}
	
    public void displayAvatar() {
    	if (avatar != null)
    		picture.setImageDrawable(avatar);
    	else {
    		if (legislator.gender.equals("M"))
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
	    	else if (type.equals("voting"))
	    		votingRecord();
	    	else if (type.equals("sponsored"))
	    		sponsoredBills();
    	}
    }
    
    public void callOffice() {
    	startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel://" + legislator.phone)));
    }
    
    public void visitWebsite() {
    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(legislator.website)));
    }
    
    public void votingRecord() {
    	Intent intent = new Intent(this, RollList.class)
    		.putExtra("type", RollList.ROLLS_VOTER)
    		.putExtra("voter", legislator);
    	startActivity(intent);
    }
    
    public void sponsoredBills() {
    	Intent intent = new Intent(this, BillList.class)
    		.putExtra("type", BillList.BILLS_SPONSOR)
    		.putExtra("sponsor_id", legislator.getId())
    		.putExtra("sponsor_name", legislator.titledName());
    	startActivity(intent);
    }
    
    public void launchCommittee(Committee committee) {
    	Intent intent = new Intent()
    		.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList")
			.putExtra("committeeId", committee.id)
			.putExtra("committeeName", committee.name);
		startActivity(intent);
    }

	public void districtMap() {
		String url = Utils.districtMapUrl(legislator.title, legislator.state, legislator.district);
		Uri uri = Uri.parse("geo:0,0?q=" + url);
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri); 
		mapIntent.setData(uri); 
		startActivity(Intent.createChooser(mapIntent, getString(R.string.view_legislator_district)));
	}
	
	public void setupControls() {
		MergeAdapter adapter = new MergeAdapter();
		LayoutInflater inflater = LayoutInflater.from(this);
		
		View mainView = inflater.inflate(R.layout.profile, null);
		mainView.setEnabled(false);
		
		if (!legislator.in_office) {
			mainView.findViewById(R.id.out_of_office_text).setVisibility(View.VISIBLE);
			mainView.findViewById(R.id.website).setVisibility(View.GONE);
		}
		
		mainView.findViewById(R.id.website).setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				visitWebsite();
			}
		});
		mainView.findViewById(R.id.district).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				districtMap();
			}
		});
		
		picture = (ImageView) mainView.findViewById(R.id.profile_picture);
		
		((TextView) mainView.findViewById(R.id.profile_party)).setText(partyName(legislator.party));
		((TextView) mainView.findViewById(R.id.profile_state)).setText(Utils.stateCodeToName(this, legislator.state));
		((TextView) mainView.findViewById(R.id.profile_domain)).setText(domainName(legislator.getDomain()));
		
		adapter.addView(mainView);
		
		ArrayList<View> contactViews = new ArrayList<View>(3);
		
		String phone = legislator.phone;
		if (legislator.in_office && phone != null && !phone.equals("")) {
			View phoneView = inflater.inflate(R.layout.icon_list_item_2, null);
			((TextView) phoneView.findViewById(R.id.text_1)).setText("Call " + pronoun(legislator.gender) + " office");
			((TextView) phoneView.findViewById(R.id.text_2)).setText(phone);
			((ImageView) phoneView.findViewById(R.id.icon)).setImageResource(R.drawable.phone);
			phoneView.setTag("phone");
			contactViews.add(phoneView);
		}
		
		View votingRecordView = inflater.inflate(R.layout.icon_list_item_1, null);
		((TextView) votingRecordView.findViewById(R.id.text)).setText(R.string.voting_record);
		((ImageView) votingRecordView.findViewById(R.id.icon)).setImageResource(R.drawable.rolls);
		votingRecordView.setTag("voting");
		contactViews.add(votingRecordView);
		
		View sponsoredView = inflater.inflate(R.layout.icon_list_item_1, null);
		((TextView) sponsoredView.findViewById(R.id.text)).setText(R.string.sponsored_bills);
		((ImageView) sponsoredView.findViewById(R.id.icon)).setImageResource(R.drawable.bill_multiple);
		sponsoredView.setTag("sponsored");
		contactViews.add(sponsoredView);
		
		adapter.addAdapter(new ViewArrayAdapter(this, contactViews));
		
		if (legislator.in_office) {
			committeeHeader = inflater.inflate(R.layout.header_loading, null);
			((TextView) committeeHeader.findViewById(R.id.header_text)).setText(R.string.committees);
			((TextView) committeeHeader.findViewById(R.id.loading_message)).setText(R.string.loading_committees);
			adapter.addView(committeeHeader);
		}
		
		setListAdapter(adapter);
	}
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.legislator, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.main:
    		startActivity(new Intent(this, MainMenu.class));
    		break;
    	case R.id.shortcut:
    		if (shortcutImageTask == null)
    			shortcutImageTask = (ShortcutImageTask) new ShortcutImageTask(this).execute(legislator.getId());
    		break;
    	case R.id.govtrack:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.govTrackUrl(legislator.govtrack_id))));
    		break;
    	case R.id.opencongress:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.openCongressUrl(legislator.govtrack_id))));
    		break;
    	case R.id.bioguide:
    		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Legislator.bioguideUrl(legislator.bioguide_id))));
    		break;
    	}
    	return true;
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

        // ignoring convertView as a recycling possibility, too small a list to be worth it
		public View getView(int position, View convertView, ViewGroup parent) {
			Committee committee = getItem(position);
			
			View view = inflater.inflate(R.layout.profile_committee, null);
			((TextView) view.findViewById(R.id.name)).setText(committee.name);
			view.setTag(committee);
			
			return view;
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
			ArrayList<Committee> temp;
			
			try {
				temp = CommitteeService.forLegislator(bioguideId[0]);
			} catch (CongressException e) {
				this.exception = new CongressException(e, "Error loading committees.");
				return null;
			}
			for (int i=0; i<temp.size(); i++) {
				if (temp.get(i).chamber.equals("Joint"))
					joint.add(temp.get(i));
				else
					committees.add(temp.get(i));
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
		LoadPhotoTask loadPhotoTask;
		LoadCommitteesTask loadCommitteesTask;
		ShortcutImageTask shortcutImageTask;
		ArrayList<Committee> committees;
		
		LegislatorProfileHolder(LoadPhotoTask loadPhotoTask, LoadCommitteesTask loadCommitteesTask, ShortcutImageTask shortcutImageTask, ArrayList<Committee> committees) {
			this.loadPhotoTask = loadPhotoTask;
			this.loadCommitteesTask = loadCommitteesTask;
			this.shortcutImageTask = shortcutImageTask;
			this.committees = committees;
		}
		
		public void loadInto(LegislatorProfile context) {
			context.loadPhotoTask = loadPhotoTask;
			context.loadCommitteesTask = loadCommitteesTask;
			context.shortcutImageTask = shortcutImageTask;
			context.committees = committees;
		}
	}
}