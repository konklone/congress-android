package com.sunlightlabs.android.congress.fragments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.android.congress.fragments.YouTubeFragment.VideoAdapter.VideoHolder;
import com.sunlightlabs.android.congress.notifications.Footer;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.tasks.LoadYoutubeThumbTask;
import com.sunlightlabs.android.congress.tasks.LoadYoutubeThumbTask.LoadsThumb;
import com.sunlightlabs.android.congress.tasks.LoadYoutubeVideosTask;
import com.sunlightlabs.android.congress.tasks.LoadYoutubeVideosTask.LoadsYoutubeVideos;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.ImageUtils;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Legislator;

public class YouTubeFragment extends ListFragment implements LoadsThumb, LoadsYoutubeVideos {
	private List<Video> videos;
	private Map<Integer, LoadYoutubeThumbTask> loadThumbTasks = new HashMap<Integer, LoadYoutubeThumbTask>();
	
	private Legislator legislator;

	// it is assumed this fragment will not be instantiated if the legislator does not have a youtube ID
	
	public static YouTubeFragment create(Legislator legislator) {
		YouTubeFragment frag = new YouTubeFragment();
		Bundle args = new Bundle();
 
		args.putSerializable("legislator", legislator);
		
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public YouTubeFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		legislator = (Legislator) args.getSerializable("legislator");
		
		loadVideos();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_footer, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (videos != null)
			displayVideos();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (videos != null)
			setupSubscription();
	}
	
	private void setupControls() {
		FragmentUtils.setLoading(this, R.string.youtube_loading);
		((Button) getView().findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				videos = null;
				FragmentUtils.showLoading(YouTubeFragment.this);
				loadVideos();
			}
		});
	}

	private void setupSubscription() {
		Footer.setup(this, new Subscription(legislator.bioguide_id, Subscriber.notificationName(legislator), "YoutubeSubscriber", legislator.youtube_id), videos);
	}
    
	protected void loadVideos() {
		new LoadYoutubeVideosTask(this).execute(legislator.youtube_id);
	}
	
	public void onLoadVideos(List<Video> videos) {
		this.videos = videos;
		if (isAdded())
			displayVideos();
	}
	
	protected void displayVideos() {
    	if (videos != null && videos.size() > 0)
	    	setListAdapter(new VideoAdapter(YouTubeFragment.this, videos));
    	else
	    	FragmentUtils.showRefresh(this, R.string.youtube_empty);
    	
    	setupSubscription();
    }
	
	@Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		launchVideo((Video) parent.getItemAtPosition(position));
	}
	
	private void launchVideo(Video video) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(video.url)));
	}
	
	protected class VideoAdapter extends ArrayAdapter<Video> {
		LayoutInflater inflater;
		YouTubeFragment context;

        public VideoAdapter(YouTubeFragment context, List<Video> videos) {
            super(context.getActivity(), 0, videos);
			this.context = context;
            inflater = LayoutInflater.from(context.getActivity());
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return true;
        }
        
        @Override
        public int getViewTypeCount() {
        	return 1;
        }

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = inflater.inflate(R.layout.video, null);
			
			Video video = getItem(position);

			VideoHolder holder = new VideoHolder();
			holder.url = video.thumbnailUrl;
			holder.hash = holder.url == null ? null : holder.url.hashCode();
			
			TextView title = (TextView) view.findViewById(R.id.video_title);
			title.setText(video.title);
			holder.title = title;
			
			// make the date stand out in the description using bold text
			StringBuilder full = new StringBuilder("<b>").append(video.timestamp.format("%b %d")).append("</b>");
			
			full.append(", " + video.formatDuration());
			
			String description = video.description != null ? video.description.trim() : "";
			if (!description.equals("")) // check to see if the video has a non-empty description first
				full.append(" - ").append(description);
			
			TextView desc = (TextView) view.findViewById(R.id.video_description);
			desc.setText(Html.fromHtml(Utils.truncate(full.toString(), 150)));
			holder.description = desc;
			
			ImageView thumb = (ImageView) view.findViewById(R.id.thumbnail);
			holder.thumb = thumb;

			if (holder.hash != null) {
				BitmapDrawable pic = ImageUtils.quickGetImage(ImageUtils.YOUTUBE_THUMB, holder.hash, context.getActivity());
				if (pic != null) {
					holder.thumb.setImageDrawable(pic);
				} else {
					holder.thumb.setImageResource(R.drawable.loading_photo);
					context.loadThumb(holder);
				}
			}
			else 
				holder.thumb.setImageResource(R.drawable.youtube_thumb);
			
			view.setTag(holder);
			return view;
		}
		
		class VideoHolder {
			Integer hash;
			String url;
			ImageView thumb;
			TextView title;
			TextView description;

			@Override
			public boolean equals(Object o) {
				// 'this' cannot be null
				if (o == null || !(o instanceof VideoHolder))
					return false;
				VideoHolder ov = (VideoHolder) o;
				return ov.hash.equals(this.hash);
			}
		}
    }
    
    
 	public void loadThumb(VideoAdapter.VideoHolder holder) {
		int hash = holder.url.hashCode();
		if (!loadThumbTasks.containsKey(hash))
			loadThumbTasks.put(hash, (LoadYoutubeThumbTask) new LoadYoutubeThumbTask(this,
					ImageUtils.YOUTUBE_THUMB, holder).execute(holder.url));
	}

	public void onLoadThumb(Drawable thumb, Object tag) {
		if (!isAdded())
			return;
		
		VideoAdapter.VideoHolder holder = (VideoHolder) tag;

		loadThumbTasks.remove(holder.hash);

		View result = getListView().findViewWithTag(holder);
		if (result != null) {
			if (thumb != null)
				holder.thumb.setImageDrawable(thumb);
			else
				holder.thumb.setImageResource(R.drawable.youtube_thumb);
		}
	}


	
}
