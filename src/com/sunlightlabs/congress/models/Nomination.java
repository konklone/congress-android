package com.sunlightlabs.congress.models;

import java.util.List;

public class Nomination {

	public List<Nominee> nominees;
	public String nomination_id;
	
	public class Nominee {
		public String name, position;
	}
}