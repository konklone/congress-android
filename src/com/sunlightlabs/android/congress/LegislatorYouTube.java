package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.twitter.Status;
import com.sunlightlabs.android.yahoo.news.NewsItem;
import com.sunlightlabs.android.youtube.Video;
import com.sunlightlabs.android.youtube.YouTube;
import com.sunlightlabs.android.youtube.YouTubeException;

public class LegislatorYouTube extends ListActivity {
	private static final int LOADING = 0;
	private static final int MENU_WATCH = 0;
	
	private String username;
	private Video[] videos;
	
	private Button refresh;
	
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.youtube_list);
    	
    	username = getIntent().getStringExtra("username");
    	
    	setupControls();
    	
    	loadVideos();
	}
	
    final Handler handler = new Handler();
    final Runnable updateThread = new Runnable() {
        public void run() {        	
        	setListAdapter(new VideoAdapter(LegislatorYouTube.this, videos));
        	
        	if (videos.length <= 0) {
        		TextView empty = (TextView) LegislatorYouTube.this.findViewById(R.id.youtube_empty);
        		empty.setText(R.string.youtube_empty);
        		refresh.setVisibility(View.VISIBLE);
        	}
        	
        	dismissDialog(LOADING);
        }
    };
	
	protected void loadVideos() {
		Thread loadingThread = new Thread() {
	        public void run() { 
	        	try {
	        		YouTube youtube = new YouTube();
	        		videos = youtube.getVideos(username);
	        	} catch(YouTubeException e) {
	        		Toast.makeText(LegislatorYouTube.this, "Couldn't load videos.", Toast.LENGTH_SHORT).show();
	        	}
	        	handler.post(updateThread);
	        }
	    };
	    loadingThread.start();
	    
		showDialog(LOADING);
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
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_WATCH:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			Video video = (Video) getListView().getItemAtPosition(info.position);
			launchVideo(video);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void launchVideo(Video video) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(video.url)));
	}
	
	private void setupControls() {
		refresh = (Button) this.findViewById(R.id.youtube_refresh);
    	refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadVideos();
			}
		});
    	registerForContextMenu(getListView());
	}
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Plucking videos from the air...");
            return dialog;
        default:
            return null;
        }
    }
    
    protected class VideoAdapter extends BaseAdapter {
    	private Video[] videos;
    	LayoutInflater inflater;

        public VideoAdapter(Activity context, Video[] videos) {
            this.videos = videos;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

		public int getCount() {
			return videos.length;
		}

		public Object getItem(int position) {
			return videos[position];
		}

		public long getItemId(int position) {
			return ((long) position);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			if (convertView == null) {
				view = (LinearLayout) inflater.inflate(R.layout.youtube, null);
			} else {
				view = (LinearLayout) convertView;
			}
			
			Video video = (Video) getItem(position);
			
			TextView text = (TextView) view.findViewById(R.id.video_title);
			text.setText(video.title);
			
			TextView description = (TextView) view.findViewById(R.id.video_description);
			description.setText(video.description);
			
			TextView when = (TextView) view.findViewById(R.id.video_when);
			when.setText(video.timestamp.format("%b %d"));
			
			return view;
		}
    }
}