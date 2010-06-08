package com.sunlightlabs.services;

import org.json.JSONObject;

import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Roll;

public interface RollService {
	Roll fromDrumbone(JSONObject json) throws CongressException;

	Roll find(String id, String sections) throws CongressException;

	Roll rollFor(String url) throws CongressException;
}
