package com.sunlightlabs.services;

import java.util.ArrayList;

import com.sunlightlabs.congress.java.Committee;
import com.sunlightlabs.congress.java.CongressException;

public interface CommitteeService {
	ArrayList<Committee> forLegislator(String bioguide_id) throws CongressException;

	Committee find(String id) throws CongressException;

	Committee committeeFor(String url) throws CongressException;

	ArrayList<Committee> committeesFor(String url) throws CongressException;
}
