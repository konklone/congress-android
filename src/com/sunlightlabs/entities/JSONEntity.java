package com.sunlightlabs.entities;

import java.io.*;
import java.util.*;

import org.json.*;

import com.sunlightlabs.api.*;
/**
 * com.sunlightlabs.entities.JSONEntity
 * steve Jul 22, 2009
 */
public abstract class JSONEntity {
	public static Class<JSONEntity> THIS_CLASS = JSONEntity.class;
	public static JSONEntity[] EMPTY_ARRAY = {};
	
	/**
	 * extract the properties from a JSOM object
	 * @param obj non-null json object
	 * @return non-null map of properteis
	 */
	public static Map<String,Object> getProperties(JSONObject obj) {
		try {
			Map<String,Object> ret = new HashMap<String,Object>();
			for(Iterator<String> itr = obj.keys(); itr.hasNext(); ) {
				String key = itr.next();
				Object value = obj.get(key);
				ret.put(key, value);
			}
			
			return ret;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * turn a JSONArray of JSONObjects into the Objects 
	 * @param items non-null JSONArray
	 * @return  non-null array of JSONObject
	 */
	public static JSONObject[] getArrayItems(JSONArray items)
	{
		try {
			List<JSONObject> holder = new ArrayList<JSONObject>();
			for (int i = 0; i < items.length(); i++) {
				JSONObject js = (JSONObject)items.get(i);
				 
				js = getContainedJSONObject(js);
				holder.add(js);
			}

			JSONObject[] ret = new JSONObject[holder.size()];
			holder.toArray(ret);
			return ret;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			    throw new RuntimeException(e);
		}
	}
	
	/**
	 * turn a JSONArray of JSONObjects into the Objects 
	 * @param items non-null JSONArray
	 * @return  non-null array of JSONObject
	 */
	public static String[] getArrayStringItems(JSONArray items)
	{
		try {
			List<String> holder = new ArrayList<String>();
			for (int i = 0; i < items.length(); i++) {
				String js = (String)items.get(i);
				 
				holder.add(js);
			}

			String[] ret = new String[holder.size()];
			holder.toArray(ret);
			return ret;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			    throw new RuntimeException(e);
		}
	}

	
	/**
	 * JSONArray wrap their elements - this unwraps
	 * @param container wrapped element
	 * @return contained element
	 */
	public static JSONObject getContainedJSONObject(JSONObject container)
	{
		try {
			Iterator<String> itr = container.keys();
			if(!itr.hasNext())
				return null;
			String key = itr.next();
			return container.getJSONObject(key);
		} catch (JSONException e) {
			    throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param call
	 * @param params
	 * @param apiCall
	 * @param arrayName
	 * @return
	 */
	public static String[] getJSONStrings(ApiCall call,
			Map<String, String> params, String apiCall,String arrayName) {
		try {
			JSONObject obj = call.getJSONResponse(apiCall, params);
			JSONArray items = obj.getJSONArray(arrayName);
			return getArrayStringItems(items);
	} catch (JSONException e) {
		    throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * call the web site and 
	 * @param call
	 * @param params
	 * @param apiCall
	 * @param arrayName
	 * @return
	 */
	public static JSONObject[] getJSONObjects(ApiCall call,
			Map<String, String> params, String apiCall,String arrayName) {
		try {
			JSONObject obj = call.getJSONResponse(apiCall, params);
			JSONArray items = obj.getJSONArray(arrayName);
			List<JSONObject> holder = new ArrayList<JSONObject>();
			for (int i = 0; i < items.length(); i++) {
				JSONObject js = (JSONObject)items.get(i);
				js = getContainedJSONObject(js);
				holder.add(js);
			}

			JSONObject[] ret = holder.toArray(new JSONObject[0]);
			return ret;
		} catch (JSONException e) {
		    throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param call
	 * @param params
	 * @param apiCall
	 * @param arrayName
	 * @return
	 */
	public static JSONObject  getJSONObject(ApiCall call,
			Map<String, String> params, String apiCall,String arrayName) {
		try {
			JSONObject obj = call.getJSONResponse(apiCall, params);
			JSONObject ret = obj.getJSONObject(arrayName);
			return ret;
		} catch (JSONException e) {
		    throw new RuntimeException(e);
		}
	}

	private final Map<String,String> m_Properties = 
		new HashMap<String,String>();
	
	public JSONEntity(JSONObject data) {
		this(getProperties(data));
	}
	
	public JSONEntity(Map<String,Object> data) {
		for(String key : data.keySet()) {
			Object value = data.get(key);
			if(value instanceof String) {
				m_Properties.put(key,(String)value);
				continue;
			}
			if(value instanceof JSONArray) {
				handleJSONArray((JSONArray)value);
				continue;
			}
			value = null; // break here
		}
	}
	
	/**
	 * return a property
	 * @param key non-null key
	 * @return possibly null property
	 */
	public String getProperty(String key) {
		return m_Properties.get(key);
	}
	
	/**
	 * return an array of all property names
	 * @return non-null array
	 */
	public String[] getAllKeys() {
		String[] ret = m_Properties.keySet().toArray(new String[0]);
		Arrays.sort(ret);
		return ret;
	}
	
	protected void handleJSONArray( JSONArray value)
	{
		throw new UnsupportedOperationException("Fix this");
	}
	
	/**
	 * extract properties as NameValuePairs
	 * @return non-null array if NameValuePairs
	 */
	public NameValuePair[] getNameValuePairs() {
		List<NameValuePair> holder = new ArrayList<NameValuePair>();
		for(String key : m_Properties.keySet() ) {
			String value = m_Properties.get(key);
			holder.add(new NameValuePair(key, value));
		}
		
		NameValuePair[] ret = new NameValuePair[holder.size()];
		holder.toArray(ret);
		return ret;
	}
	
	/**
	 * show all properties on System.out
	 */
	public  void showProperties() {
		showProperties(System.out);
	}
	
	/**
	 * write all name value pairs to the appender
	 * @param out non-null appender
	 */
	public  void showProperties(Appendable out) {
		NameValuePair[] values = getNameValuePairs();
		for (int j = 0; j < values.length; j++) {
			try {
				out.append(values[j].toString());
				out.append("\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				    throw new RuntimeException(e);
			}
		}
	}

	/**
	 * show object on system.out
	 */
	public  void show() {
		show(System.out);
	}
	
	/**
	 * show object on out
	 * @param out non-null appender
	 */
	public  void show(Appendable out) {
		try {
			out.append(getClass().getSimpleName());
			out.append("\n");
		} catch (IOException e) {
			    throw new RuntimeException(e);
		}
		showProperties(out);
	}

	/**
	 * JSON name of the item
	 * @return non-null String
	 */
	public abstract String getEntityName();
	
	/**
	 * unofficial list of properties
	 */
	public abstract String[] getKnownProperties();

	/**
	 * true if l2 is of the same class and has he same properties
	 * useful in testing 
	 * @param l2 non-null test object
	 * @return true if they are the same
	 */
	public boolean equivalent(JSONEntity l2)
	{
		if(l2.getClass() != getClass())
			return false;
		if(l2.m_Properties.size() != m_Properties.size())
			return false;
		for(String key : m_Properties.keySet()) {
			String p1 = getProperty(key);
			String p2 = l2.getProperty(key);
					
			if(!p1.equals(p2))
				return false;
		}
		return true;
	}
}
