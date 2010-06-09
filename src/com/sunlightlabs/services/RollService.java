package com.sunlightlabs.services;

import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Roll;

public interface RollService {
	Roll find(String id, String sections) throws CongressException;

	Roll rollFor(String url) throws CongressException;
}
