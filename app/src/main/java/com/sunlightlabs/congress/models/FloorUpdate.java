package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class FloorUpdate implements Serializable {
	private static final long serialVersionUID = 1L;

    public String id;
	public Date timestamp;
	public Date legislativeDay;
	public String update;
	public String chamber;
	public int congress;
}