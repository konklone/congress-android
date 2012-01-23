package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Video implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String clipId, videoId;
	public String chamber;
	public List<String> billIds;
	public List<String> rollIds;
	public List<String> bioguideIds;
	public int duration;
	public Date pubdate;
	public int session;
	public Date legislativeDay;
	public Map<String,String> clipUrls;
}