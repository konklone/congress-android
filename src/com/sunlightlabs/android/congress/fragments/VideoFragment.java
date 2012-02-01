package com.sunlightlabs.android.congress.fragments;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Video;
import com.sunlightlabs.congress.services.VideoService;

public class VideoFragment extends Fragment implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
	
	private String chamber;
	
	private Video latestVideo;
	private VideoView videoView;
	private MediaController controller;
	
	public static VideoFragment forChamber(String chamber) {
		VideoFragment frag = new VideoFragment();
		Bundle args = new Bundle();
		args.putString("chamber", chamber);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public VideoFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		chamber = getArguments().getString("chamber");
		
		loadVideo();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.video_floor, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (latestVideo != null)
			displayVideo();
	}
	
	public void setupControls() {
		videoView = (VideoView) getView().findViewById(R.id.the_video);
		videoView.setOnErrorListener(this);
		videoView.setOnPreparedListener(this);
		videoView.setOnCompletionListener(this);
		
		controller = new MediaController(getActivity());
		controller.setAnchorView(videoView);
		
		videoView.setMediaController(controller);
	}
	
	public void loadVideo() {
		new LatestVideoTask(this, chamber).execute();
	}
	
	public void onLoadVideo(Video video) {
		if (!isAdded())
			return;
		
		this.latestVideo = video;
		displayVideo();
	}
	
	public void onLoadVideo(CongressException exception) {
		if (isAdded()) {
			Utils.alert(getActivity(), exception);
		}
	}
	
	public void displayVideo() {
		String url = latestVideo.clipUrls.get("mp4");
		Uri uri = Uri.parse(url);
		videoView.setVisibility(View.VISIBLE);
		videoView.setVideoURI(uri);
	}
	
	public class LatestVideoTask extends AsyncTask<Void,Void,Video> {
		private VideoFragment fragment;
		private CongressException exception;
		private String chamber;
		
		public LatestVideoTask(VideoFragment fragment, String chamber) {
			this.fragment = fragment;
			this.chamber = chamber;
			Utils.setupRTC(fragment.getActivity());
		}
		
		@Override
		public Video doInBackground(Void... nothing) {
			try {
				return VideoService.forDay(chamber, "2011-12-14");
			} catch (CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(Video video) {
			if (exception != null && video == null)
				fragment.onLoadVideo(exception);
			else
				fragment.onLoadVideo(video);
		}
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		Utils.alert(getActivity(), "Video ready");
		controller.show(0);
	}

	@Override
	public boolean onError(MediaPlayer arg0, int what, int extra) {
		Utils.alert(getActivity(), "Error: " + what + ", " + extra);
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		
	}
	
}