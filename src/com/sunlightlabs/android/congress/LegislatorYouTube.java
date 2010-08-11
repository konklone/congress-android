package com.sunlightlabs.android.congress;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.sunlightlabs.android.congress.Footer.OnFooterClickListener;
import com.sunlightlabs.android.congress.Footer.State;
import com.sunlightlabs.android.congress.LegislatorYouTube.VideoAdapter.VideoHolder;
import com.sunlightlabs.android.congress.notifications.Notifications;
import com.sunlightlabs.android.congress.utils.ImageUtils;
import com.sunlightlabs.android.congress.utils.LoadYoutubeThumbTask;
import com.sunlightlabs.android.congress.utils.LoadYoutubeVideosTask;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.LoadYoutubeThumbTask.LoadsThumb;
import com.sunlightlabs.android.congress.utils.LoadYoutubeVideosTask.LoadsYoutubeVideos;
import com.sunlightlabs.youtube.Video;

public class LegislatorYouTube extends ListActivity implements LoadsThumb, LoadsYoutubeVideos,
		OnFooterClickListener {
	private static final String NOTIFICATION_TYPE = "youtube";

	private static final int MENU_WATCH = 0;
	private static final int MENU_COPY = 1;
	
	private Video[] videos;
	private LoadYoutubeVideosTask loadVideosTask = null;
	private HashMap<Integer, LoadYoutubeThumbTask> loadThumbTasks = new HashMap<Integer, LoadYoutubeThumbTask>();
	
	private Database database;
	private String entityId, entityType, entityName, youtubeUsername;

	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.list_footer);
    	
		database = new Database(this);
		database.open();

		Intent i = getIntent();
		entityId = i.getStringExtra("entityId");
		entityName = i.getStringExtra("entityName");
		entityType = i.getStringExtra("entityType");
		youtubeUsername = i.getStringExtra("youtubeUsername");
    	
    	LegislatorYouTubeHolder holder = (LegislatorYouTubeHolder) getLastNonConfigurationInstance();
    	if (holder != null) {
    		videos = holder.videos;
    		loadVideosTask = holder.loadVideosTask;
    		if (loadVideosTask != null)
    			loadVideosTask.onScreenLoad(this);

			loadThumbTasks = holder.loadThumbTasks;
			if (loadThumbTasks != null) {
				Iterator<LoadYoutubeThumbTask> iterator = loadThumbTasks.values().iterator();
				while (iterator.hasNext())
					iterator.next().onScreenLoad(this);
			}
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
		holder.loadThumbTasks = this.loadThumbTasks;
    	return holder;
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
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

		setupFooter();
	}

	private void setupFooter() {
		Footer footer = (Footer) findViewById(R.id.footer);
		footer.setListener(this);
		footer.setHasEntity(true);
		footer.setEntityId(entityId);
		footer.setEntityName(entityName);
		footer.setEntityType(entityType);
		footer.setNotificationType(NOTIFICATION_TYPE);
		footer.setNotificationData(youtubeUsername);
		footer.setDatabase(database);

		// if the service is started, check the database
		if (Utils.getBooleanPreference(this, Preferences.KEY_NOTIFY_ENABLED, Preferences.DEFAULT_NOTIFY_ENABLED)
				&& Database.NOTIFICATIONS_ON.equals(database.getNotificationStatus(entityId, NOTIFICATION_TYPE)))
			footer.setOn();
		else
			footer.setOff();
	}

	public void onFooterClick(Footer footer, State state) {
		if (state == State.ON) {
			
			// if notifications are not yet enabled, send broadcast to start them
			if (!Utils.getBooleanPreference(this, Preferences.KEY_NOTIFY_ENABLED,
					Preferences.DEFAULT_NOTIFY_ENABLED)) {

				Utils.setBooleanPreference(this, Preferences.KEY_NOTIFY_ENABLED, true);
				Notifications.startNotificationsBroadcast(this);
			}
		}
	}
    
	protected void loadVideos() {
	    if (videos == null)
			loadVideosTask = (LoadYoutubeVideosTask) new LoadYoutubeVideosTask(this).execute(youtubeUsername);
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
		LegislatorYouTube context;

        public VideoAdapter(LegislatorYouTube context, Video[] videos) {
            super(context, 0, videos);
			this.context = context;
            inflater = LayoutInflater.from(context);
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
				view = inflater.inflate(R.layout.youtube, null);
			
			Video video = getItem(position);

			VideoHolder holder = new VideoHolder();
			holder.url = video.thumbnailUrl;
			holder.hash = holder.url == null ? null : holder.url.hashCode();
			
			TextView title = (TextView) view.findViewById(R.id.video_title);
			title.setText(video.title);
			holder.title = title;
			
			// make the date stand out in the description using bold text
			StringBuilder full = new StringBuilder("<b>").append(video.timestamp.format("%b %d")).append("</b>");
			String description = video.description != null ? video.description.trim() : "";
			
			if (!description.equals("")) // check to see if the video has a non-empty description first
				full.append(" - ").append(description);
			
			TextView desc = (TextView) view.findViewById(R.id.video_description);
			desc.setText(Html.fromHtml(Utils.truncate(full.toString(), 150)));
			holder.description = desc;
			
			ImageView thumb = (ImageView) view.findViewById(R.id.thumbnail);
			holder.thumb = thumb;

			if (holder.hash != null) {
				BitmapDrawable pic = ImageUtils.quickGetImage(ImageUtils.YOUTUBE_THUMB, holder.hash, context);
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
    
    
    static class LegislatorYouTubeHolder {
		Video[] videos;
		LoadYoutubeVideosTask loadVideosTask;
		HashMap<Integer, LoadYoutubeThumbTask> loadThumbTasks;
	}
	

	public void loadThumb(VideoAdapter.VideoHolder holder) {
		int hash = holder.url.hashCode();
		if (!loadThumbTasks.containsKey(hash))
			loadThumbTasks.put(hash, (LoadYoutubeThumbTask) new LoadYoutubeThumbTask(this,
					ImageUtils.YOUTUBE_THUMB, holder).execute(holder.url));
	}

	public void onLoadThumb(Drawable thumb, Object tag) {
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

	public Context getContext() {
		return this;
	}

	public void onLoadYoutubeVideos(Video[] videos, String... id) {
		this.videos = videos;
		displayVideos();
		loadVideosTask = null;
	}
}