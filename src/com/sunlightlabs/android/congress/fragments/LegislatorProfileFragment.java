package com.sunlightlabs.android.congress.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sunlightlabs.android.congress.BillSponsor;
import com.sunlightlabs.android.congress.CommitteeMember;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.VoteVoter;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorProfileFragment extends Fragment implements LoadPhotoTask.LoadsPhoto {
	private Legislator legislator;
	
	private Drawable avatar;
	
	public static LegislatorProfileFragment create(Legislator legislator) {
		LegislatorProfileFragment frag = new LegislatorProfileFragment();
		Bundle args = new Bundle();
		args.putSerializable("legislator", legislator);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public LegislatorProfileFragment() {}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FragmentUtils.setupAPI(this);
        
        legislator = (Legislator) getArguments().getSerializable("legislator");
        loadPhoto();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.legislator_profile, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (avatar != null)
			displayAvatar();
	}

	public void loadPhoto() {
		new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(legislator.bioguide_id);
	}

	public void onLoadPhoto(Drawable avatar, Object tag) {
		if (avatar == null) {
			Resources resources = null;
			if (getActivity() != null)
				resources = getActivity().getResources();
			
			if (resources != null)
				avatar = resources.getDrawable(R.drawable.person);
		}
		this.avatar = avatar;
		
		if (isAdded())
			displayAvatar();
	}
	
	public Context getContext() {
		return getActivity();
	}
	
    public void displayAvatar() {
    	((ImageView) getView().findViewById(R.id.profile_picture)).setImageDrawable(avatar);
    }
    
    public void callOffice() {
    	Analytics.legislatorCall(getActivity(), legislator.bioguide_id);
    	startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel://" + legislator.phone)));
    }
    
    public void visitWebsite() {
    	Analytics.legislatorWebsite(getActivity(), legislator.bioguide_id);
    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(legislator.website)));
    }
    
    public void votingRecord() {
    	startActivity(new Intent(getActivity(), VoteVoter.class)
			.putExtra("legislator", legislator));
    }
    
    public void sponsoredBills() {
    	startActivity(new Intent(getActivity(), BillSponsor.class)
			.putExtra("legislator", legislator));
    }
    
    public void viewCommittees() {
    	startActivity(new Intent(getActivity(), CommitteeMember.class)
			.putExtra("legislator", legislator));
    }
    
    public void districtMap() {
		String url = Utils.districtMapUrl(legislator.title, legislator.state, legislator.district);
		Uri uri = Uri.parse("geo:0,0?q=" + url);
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri); 
		mapIntent.setData(uri);
		
		Analytics.legislatorDistrict(getActivity(), legislator.bioguide_id);
		
		startActivity(Intent.createChooser(mapIntent, getString(R.string.view_legislator_district)));
	}


	public void setupControls() {
		View mainView = getView();
		
		if (!legislator.in_office)
			mainView.findViewById(R.id.out_of_office_text).setVisibility(View.VISIBLE);
		
		((TextView) mainView.findViewById(R.id.profile_party)).setText(partyName(legislator.party));
		((TextView) mainView.findViewById(R.id.profile_state)).setText(Utils.stateCodeToName(getActivity(), legislator.state));
		((TextView) mainView.findViewById(R.id.profile_domain)).setText(domainName(legislator.getDomain()));
		((TextView) mainView.findViewById(R.id.profile_office)).setText(officeName(legislator.office));
	
		setupMap();
		
//		profileItem(R.id.profile_phone, "Call " + pronoun(legislator.gender) + " office", new View.OnClickListener() {
//			public void onClick(View v) {callOffice();}
//		});
//		
//		profileItem(R.id.profile_website, "Website", new View.OnClickListener() {
//			public void onClick(View v) {visitWebsite();}
//		});
//		
//		profileItem(R.id.profile_voting, R.string.voting_record, new View.OnClickListener() {
//			public void onClick(View v) {votingRecord();}
//		});
//		
//		profileItem(R.id.profile_bills, R.string.sponsored_bills, new View.OnClickListener() {
//			public void onClick(View v) {sponsoredBills();}
//		});
//		
//		profileItem(R.id.profile_committees, R.string.committees, new View.OnClickListener() {
//			public void onClick(View v) {viewCommittees();}
//		});
//		
//		if (legislator.website == null || legislator.website.equals(""))
//			mainView.findViewById(R.id.profile_website).setVisibility(View.GONE);
	}
	
	public void setupMap() {
		FragmentManager manager = getChildFragmentManager();
		
		SupportMapFragment fragment = (SupportMapFragment) manager.findFragmentById(R.id.map_container);
		if (fragment == null) {
			fragment= SupportMapFragment.newInstance();
			manager.beginTransaction().add(R.id.map_container, fragment).commit();
		}
		
        GoogleMap map = fragment.getMap();

        if (map != null) {
        	Log.i(Utils.TAG, "Loading map");
        	LatLng sydney = new LatLng(-33.867, 151.206);

	        map.setMyLocationEnabled(true);
	        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));
	
	        map.addMarker(new MarkerOptions()
		        .title("Sydney")
		        .snippet("The most populous city in Australia.")
		        .position(sydney));
        } else
        	Log.i(Utils.TAG, "No map.");
        
	}
	
	private View profileItem(int id, int text, View.OnClickListener listener) {
		return profileItem(id, getActivity().getResources().getString(text), listener);
	}
	
	private View profileItem(int id, String text, View.OnClickListener listener) {
		ViewGroup item = (ViewGroup) getView().findViewById(id);
		TextView textView = (TextView) item.findViewById(R.id.text);
		textView.setText(text);
		
		item.setOnClickListener(listener);
		
		return item;
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
	
	public static String officeName(String office) {
		return office.replaceAll("(?:House|Senate)? ?(?:Office)? ?Building", "").trim();
	}
}