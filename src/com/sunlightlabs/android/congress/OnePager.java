package com.sunlightlabs.android.congress;

import java.util.regex.Pattern;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import com.sunlightlabs.android.congress.utils.ActionBarUtils;
import com.sunlightlabs.android.congress.utils.Analytics;

public class OnePager extends FragmentActivity {

	public String learnUrl = "http://sunlightfoundation.com/blog/2012/05/30/appropriators-may-undercut-legislative-transparency/";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.one_pager);
		
		setupControls();
	}
	
	
	public void setupControls() {
		ActionBarUtils.setTitle(this, R.string.one_pager_title);
		
		populateText(R.id.one_pager_1, R.string.one_pager_1);
		TextView text = populateText(R.id.one_pager_2, R.string.one_pager_2);
		Linkify.addLinks(text, 
				Pattern.compile("(S\\.|H\\.)(\\s?J\\.|\\s?R\\.|\\s?Con\\.| ?)(\\s?Res\\.)*\\s?\\d+", Pattern.CASE_INSENSITIVE), 
				"congress://com.sunlightlabs.android.congress/bill/112/");
		populateText(R.id.one_pager_3, R.string.one_pager_3);
		populateText(R.id.one_pager_4, R.string.one_pager_4);
		
		// set autolink for bill code
		
		findViewById(R.id.one_pager_button_1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callOffice();
			}
		});
		
		findViewById(R.id.one_pager_button_2).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				learnMore();
			}
		});
	}
	
	public TextView populateText(int viewId, int stringId) {
		Resources res = getResources();
		TextView text = (TextView) findViewById(viewId);
		text.setText(Html.fromHtml(res.getString(stringId)));
		text.setMovementMethod(LinkMovementMethod.getInstance());
		return text;
	}
	
	public void callOffice() {
    	Analytics.callOnThomas(this);
    	startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel://202-224-3121")));
    }
	
	public void learnMore() {
		Analytics.learnMoreThomas(this);
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(learnUrl)));
	}

}