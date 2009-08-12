package com.sunlightlabs.api;

import java.util.*;
import java.io.*;
import java.net.*;

import org.json.*;

import com.sunlightlabs.entities.*;

/**
 * API call er object - holds the API Key
 * com.sunlightlabs.api.ApiCall steve Jul 22, 2009
 */
public class ApiCall {
	public static Class<ApiCall> THIS_CLASS = ApiCall.class;
	public static ApiCall[] EMPTY_ARRAY = {};

	
	public static final String URL_CALL_TEMPLATE = "http://services.sunlightlabs.com/api/%API_CALL%.json?apikey=%API_KEY%";
	public static final String name = "http://services.sunlightlabs.com/api/wordlist";
	public static final String DEFAULT_ENCODING = "UTF-8";


	private final String m_ApiKey;

	public ApiCall(String apiKey) {
		super();
		m_ApiKey = apiKey;
	}

	/**
	 * return the API Key
	 * @return probably non-null key
	 */
	public String getApiKey() {
		return m_ApiKey;
	}

	/**
	 * 
	 * @param apiCall
	 * @param params
	 * @return
	 */
	public String callAPI(String apiCall, Map<String, String> params) {
		String url = URL_CALL_TEMPLATE.replace("%API_CALL%", apiCall);
		url = url.replace("%API_KEY%", getApiKey());
		url += buildPropertyString(params);
		return getUrlLines(url);
	}

	/**
	 * 
	 * @param apiCall
	 * @param params
	 * @return
	 */
	public JSONObject getJSONResponse(String apiCall, Map<String, String> params) {
		String s = callAPI(apiCall, params);
	//	if(s.contains("lobbyist"))
	//	  System.out.println(s);
		JSONTokener tokenizer = new JSONTokener(s);
		try {
			JSONObject obj = new JSONObject(tokenizer);
			JSONObject rsp = null;
			rsp = obj.getJSONObject("response");
			return rsp;
		} catch (JSONException ex) {
			throw new RuntimeException(ex);
		}

	}

	/**
	 * read a URL text
	 * @param url non-null url
	 * @return non-null String
	 */
	public String getUrlLines(String url) {
		StringBuilder sb = new StringBuilder();

		try {
			URL u = new URL(url);
			URLConnection yc = u.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(yc
					.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
				sb.append("\n");
			}
			in.close();
			String ret = sb.toString();
			return ret;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * escape a string for addition in a url
	 * @param data non-null string
	 * @return escaped string
	 */
	public String htmlEscape(String data) {
		try {
			return URLEncoder.encode(data, DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// @SuppressWarnings()
			return URLEncoder.encode(data);
		}
	}

	/**
	 * make a string with &key=value...
	 * 
	 * @param params
	 *            key-value pairs
	 * @return as above
	 */
	protected String buildPropertyString(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		for (String key : params.keySet()) {
			sb.append("&");
			sb.append(htmlEscape(key));
			sb.append("=");
			sb.append(htmlEscape(params.get(key)));
		}
		String ret = sb.toString();
		return ret;
	}

	
	
}
