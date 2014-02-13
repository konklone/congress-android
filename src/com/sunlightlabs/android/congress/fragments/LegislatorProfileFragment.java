package com.sunlightlabs.android.congress.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.CommitteeMember;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.tasks.LoadDistrictTask;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.DistrictMapView;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.District;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorProfileFragment extends Fragment implements LoadPhotoTask.LoadsPhoto, LoadDistrictTask.LoadsDistrict {
	private Legislator legislator;
	private District district;
	
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
    	startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel://" + legislator.phone)));
    }
    
    public void seeCommittees() {
    	startActivity(new Intent(getActivity(), CommitteeMember.class)
    		.putExtra("legislator", legislator));
    }
    
    public void visit(String url, String social) {
    	Analytics.legislatorWebsite(getActivity(), legislator.bioguide_id, social);
    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
    
    public void setupControls() {
		View mainView = getView();
		
		if (!legislator.in_office)
			mainView.findViewById(R.id.out_of_office_text).setVisibility(View.VISIBLE);
		
		String party = partyName(legislator.party);
		String state = Utils.stateCodeToName(getActivity(), legislator.state);
		((TextView) mainView.findViewById(R.id.profile_state_party)).setText(party + " from " + state);
		((TextView) mainView.findViewById(R.id.profile_domain)).setText(legislator.getDomain());
		
		socialButton(R.id.twitter, legislator.twitterUrl(), Analytics.LEGISLATOR_TWITTER);
		socialButton(R.id.youtube, legislator.youtubeUrl(), Analytics.LEGISLATOR_YOUTUBE);
		socialButton(R.id.facebook, legislator.facebookUrl(), Analytics.LEGISLATOR_FACEBOOK);
		
		TextView officeView = (TextView) mainView.findViewById(R.id.profile_office);
		if (legislator.office != null && !legislator.office.equals(""))
			officeView.setText(officeName(legislator.office));
		else
			officeView.setVisibility(View.GONE);
		
		// allow for devices without phones
		boolean hasPhone = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
		if (hasPhone && legislator.phone != null && !legislator.phone.equals("")) {
			mainView.findViewById(R.id.call_office).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					callOffice();
				}
			});
		} else
			mainView.findViewById(R.id.call_office_container).setVisibility(View.GONE);
		
		if (legislator.website != null && !legislator.website.equals("")) {
			mainView.findViewById(R.id.visit_website).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					visit(legislator.website, Analytics.LEGISLATOR_WEBSITE);
				}
			});
		} else
			mainView.findViewById(R.id.visit_website_container).setVisibility(View.GONE);

		// we should always be able to link to their committees
		mainView.findViewById(R.id.committees).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				seeCommittees();
			}
		});
				
		// we support froyo for now, but maps use a jackson version not supported in the version of Java Froyo uses.
		// so, for now, let's just not give the map.
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO)
			setupMap();
		else
			((TextView) mainView.findViewById(R.id.map_text)).setText(R.string.map_unsupported);
	}
	
	public void setupMap() {
		if (this.district != null)
			displayDistrict();
		else
			loadDistrict();
	}
	
	// can assume this.district is set
	public void displayDistrict() {
		Log.i(Utils.TAG, "Got district map fetched, loading Mapbox map...");
		
		Context context = this.getActivity();
		DistrictMapView mapView = new DistrictMapView(context, context.getResources().getString(R.string.mapbox_id));
		
		District.drawDistrict(district, mapView);
		
		ViewGroup container = (ViewGroup) getView().findViewById(R.id.map_container);
		container.addView(mapView);
		Log.i(Utils.TAG, "Drew a map.");
	}
	
	public void loadDistrict() {
		Log.i(Utils.TAG, "Kicking off district map fetching...");
		new LoadDistrictTask(this).execute(legislator);
	}
	
	@Override
	public void onLoadDistrict(District district) {
		this.district = district;
		if (isAdded())
			displayDistrict();
	}
	
	@Override
	public void onLoadDistrict(CongressException exception) {
		Log.e(Utils.TAG, "Error fetching map :(", exception);
		Utils.alert(this.getContext(), "There was an error loading the district map.");
	}
	
	private void socialButton(int id, final String url, final String network) {
		View view = getView().findViewById(id);
		if (url != null && !url.equals("")) {
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					visit(url, network);
				}
			});
		} else
			view.setVisibility(View.GONE);
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