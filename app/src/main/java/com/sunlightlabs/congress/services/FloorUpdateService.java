package com.sunlightlabs.congress.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.FloorUpdate;

import java.io.IOException;
import java.util.List;

public class FloorUpdateService {

	// /{congress}/{chamber}/floor_updates.json
	public static List<FloorUpdate> latest(String chamber, int page) throws CongressException {
		String congress = String.valueOf(Bill.currentCongress());
		String[] endpoint = { congress, chamber, "floor_updates" };

		return updatesFor(ProPublica.url(endpoint, page));
	}
	
	private static List<FloorUpdate> updatesFor(String url) throws CongressException {
		List<FloorUpdate> updates;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode floorUpdateNode = objectMapper.readTree(ProPublica.fetchJSON(url));

			JsonNode resultsNode = floorUpdateNode.get("results");
			JsonNode floorActionsNode = resultsNode.get(0).get("floor_actions");
			ObjectReader reader = objectMapper.readerFor(new TypeReference<List<FloorUpdate>>() {});
			updates = reader.readValue(floorActionsNode);

		} catch (IOException e) {
			throw new CongressException(e, "Problem parsing the JSON from " + url);
		}
		
		return updates;
	}
}