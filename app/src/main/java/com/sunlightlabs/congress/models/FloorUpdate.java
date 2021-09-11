package com.sunlightlabs.congress.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class FloorUpdate implements Serializable {
	private static final long serialVersionUID = 1L;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss Z")
	public Date timestamp;
	@JsonProperty("date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	public Date legislativeDay;
	@JsonProperty("description")
	public String update;
	public String chamber;
	public int congress;
	@JsonProperty("action_id")
	public String actionId;
	@JsonProperty("bill_ids")
	public List<String> billIds;

}