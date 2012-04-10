package com.sunlightlabs.android.congress.fragments;

import java.util.List;

import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.TwitterException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadTweetsTask;
import com.sunlightlabs.android.congress.tasks.LoadTweetsTask.LoadsTweets;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.congress.models.Legislator;

public class TweetsFragment extends ListFragment implements LoadsTweets {
	private List<Status> tweets;
	private Legislator legislator;
	
	public static TweetsFragment create(Legislator legislator) {
		TweetsFragment frag = new TweetsFragment();
		Bundle args = new Bundle();
		
		args.putSerializable("legislator", legislator);
		
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public TweetsFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		legislator = (Legislator) args.getSerializable("legislator");
		
		loadTweets();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (tweets != null)
			displayTweets();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (tweets != null)
			setupSubscription();
	}
	
	private void setupControls() {
		FragmentUtils.setLoading(this, R.string.twitter_loading);
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tweets = null;
				FragmentUtils.showLoading(TweetsFragment.this);
				loadTweets();
			}
		});
	}

	private void setupSubscription() {
		Footer.setup(this, new Subscription(legislator.id, Subscriber.notificationName(legislator), "TwitterSubscriber", legislator.twitter_id), tweets);
	}

	protected void loadTweets() {	    
		new LoadTweetsTask(this).execute(legislator.twitter_id);
	}
	
	public void onLoadTweets(List<Status> tweets) {
		this.tweets = tweets;
		if (isAdded())
			displayTweets();
	}
	
	public void onLoadTweets(TwitterException e) {
		if (isAdded()) {
			String error = e.getMessage();
			if (error.equals("401 Unauthorized http://twitter.com/account/rate_limit_status.json"))
				FragmentUtils.showRefresh(this, R.string.twitter_rate_limit);
			else
				FragmentUtils.showRefresh(this, R.string.twitter_error);
		}
	}
	
	public void displayTweets() {
    	if (tweets != null && tweets.size() > 0)
	    	setListAdapter(new TweetAdapter(this, tweets));
    	else
	    	FragmentUtils.showRefresh(this, R.string.twitter_empty);
    	
    	setupSubscription();
    }
	
	protected class TweetAdapter extends ArrayAdapter<Status> {
    	LayoutInflater inflater;

        public TweetAdapter(Fragment context, List<Status> tweets) {
        	super(context.getActivity(), 0, tweets);
            inflater = LayoutInflater.from(context.getActivity());
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return false;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 1;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = inflater.inflate(R.layout.tweet, null); 
			
			Status tweet = getItem(position);
			((TextView) view.findViewById(R.id.tweet_text)).setText(tweet.text);
			TextView byline = (TextView) view.findViewById(R.id.tweet_byline);
			byline.setText(Html.fromHtml("posted " + timeAgoInWords(tweet.createdAt.getTime()) + " by <a href=\"http://twitter.com/" + tweet.user.screenName + "\">@" + tweet.user.screenName + "</a>"));
			byline.setMovementMethod(LinkMovementMethod.getInstance());
			
			view.setEnabled(false);
			
			return view;
		}
		
		private String timeAgoInWords(long olderTime) {
			long now = System.currentTimeMillis();
			long diff = now - olderTime; 
			if (diff < 2000) // 2 seconds
				return "just now";
			else if (diff < 50000) // 50 seconds
				return (diff / 1000) + " seconds ago";
			else if (diff < 65000) // 1 minute, 5 seconds
				return "a minute ago";
			else if (diff < 3300000) // 55 minutes
				return (diff / 60000) + " minutes ago";
			else if (diff < 3900000) // 65 minutes
				return "an hour ago";
			else if (diff < 82800000) // 23 hours
				return (diff / 3600000) + " hours ago";
			else if (diff < 90000000) // 25 hours
				return "a day ago";
			else if (diff < 1123200000) // 13 days
				return (diff / 86400000) + " days ago";
			else {
				Time old = new Time();
				old.set(olderTime);
				return old.format("%b %d");
			}
		}

    }
}