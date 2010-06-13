package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.youtube.Video;
import com.sunlightlabs.youtube.YouTube;
import com.sunlightlabs.youtube.YouTubeException;

public class LegislatorYouTube extends ListActivity {
	private static final int MENU_WATCH = 0;
	private static final int MENU_COPY = 1;
	
	private String username;
	private Video[] videos;
	
	private LoadVideosTask loadVideosTask = null;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.list);
    	
    	username = getIntent().getStringExtra("username");
    	
    	LegislatorYouTubeHolder holder = (LegislatorYouTubeHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		videos = holder.videos;
    		loadVideosTask = holder.loadVideosTask;
    		if (loadVideosTask != null)
    			loadVideosTask.onScreenLoad(this);
    	}
    	
    	setupControls();
    	if (loadVideosTask == null)
    		loadVideos();
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
    	LegislatorYouTubeHolder holder = new LegislatorYouTubeHolder();
    	holder.videos = this.videos;
    	holder.loadVideosTask = this.loadVideosTask;
    	return holder;
    }
	
	private void setupControls() {
		Utils.setLoading(this, R.string.youtube_loading);
		((Button) findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				videos = null;
				Utils.showLoading(LegislatorYouTube.this);
				loadVideos();
			}
		});
    	registerForContextMenu(getListView());
	}
    
	protected void loadVideos() {
	    if (videos == null)
    		loadVideosTask = (LoadVideosTask) new LoadVideosTask(this).execute(username);
    	else
    		displayVideos();
	}
	
	protected void displayVideos() {
    	if (videos != null && videos.length > 0)
	    	setListAdapter(new VideoAdapter(LegislatorYouTube.this, videos));
    	else
	    	Utils.showRefresh(this, R.string.youtube_empty);
    }
	
	@Override
	public void onListItemClick(ListView parent, View view, int position, long id) {
		Video video = (Video) parent.getItemAtPosition(position);
		launchVideo(video);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, MENU_WATCH, 0, "Watch");
		menu.add(0, MENU_COPY, 1, "Copy link");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Video video = (Video) getListView().getItemAtPosition(info.position);
		
		switch (item.getItemId()) {
		case MENU_WATCH:
			launchVideo(video);
			return true;
		case MENU_COPY:
			ClipboardManager cm = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
			cm.setText(video.url);
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void launchVideo(Video video) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(video.url)));
	}
	
	protected class VideoAdapter extends ArrayAdapter<Video> {
    	LayoutInflater inflater;

        public VideoAdapter(Activity context, Video[] videos) {
            super(context, 0, videos);
            inflater = LayoutInflater.from(context);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			if (convertView == null)
				view = (LinearLayout) inflater.inflate(R.layout.youtube, null);
			else
				view = (LinearLayout) convertView;
			
			Video video = getItem(position);
			((TextView) view.findViewById(R.id.video_title)).setText(video.title);
			
			// make the date stand out in the description using bold text
			StringBuilder full_desc = new StringBuilder("<b>").append(video.timestamp.format("%b %d")).append("</b>");
			String video_desc = video.description != null ? video.description.trim() : "";
			
			if(!video_desc.equals("")) { // check to see if the video has a non-empty description first
				full_desc.append(" - ").append(video_desc);
			}
			((TextView) view.findViewById(R.id.video_description)).setText(Html.fromHtml(Utils.truncate(full_desc.toString(), 150)));
			
			return view;
		}
    }
    
    private class LoadVideosTask extends AsyncTask<String,Void,Video[]> {
    	public LegislatorYouTube context;
    	
    	public LoadVideosTask(LegislatorYouTube context) {
    		super();
    		this.context = context;
    	}
    	
    	public void onScreenLoad(LegislatorYouTube context) {
    		this.context = context;
    	}
    	
    	@Override
    	protected Video[] doInBackground(String... usernames) {
    		try {
        		return new YouTube().getVideos(username);
        	} catch(YouTubeException e) {
        		return null;
        	}
    	}
    	
    	@Override
    	protected void onPostExecute(Video[] videos) {
    		context.videos = videos;
    		context.displayVideos();
    		context.loadVideosTask = null;
    	}
    }
    
    static class LegislatorYouTubeHolder {
		Video[] videos;
		LoadVideosTask loadVideosTask;
	}
}