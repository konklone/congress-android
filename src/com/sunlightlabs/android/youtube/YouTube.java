package com.sunlightlabs.android.youtube;

public class YouTube {
		
	public Video[] getVideos(String username) throws YouTubeException {
		if (false)
			throw new YouTubeException("e");
		Video[] videos = {new Video("a"), new Video("b")};
		return videos;
	}
	
	private String feedUrl(String username) {
		return "http://gdata.youtube.com/feeds/api/users/" + username + "/uploads?orderby=updated"; 
	}
}