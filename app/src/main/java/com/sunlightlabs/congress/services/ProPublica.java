package com.sunlightlabs.congress.services;


import android.util.Log;

import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProPublica {

    public static final TimeZone CONGRESS_TIMEZONE = TimeZone.getTimeZone("America/New_York");
    public static final String dateOnlyFormat = "yyyy-MM-dd";


    // Pro Publica Per Page
    public static final int PER_PAGE = 20;

    // filled in by the client in keys.xml
    public static String baseUrl = null;
    public static String userAgent = null;
    public static String apiKey = null;

    public static String url(String[] components) throws CongressException {
        return url(components, null, -1);
    }

    public static String url(String[] components, int page) throws CongressException {
        return url(components, null, page);
    }

    public static String url(String[] components, Map<String,String> params, int page) throws CongressException {

        if (components == null || components.length == 0)
            throw new CongressException("No path components given.");

        // cobble together the path components
        StringBuilder path = new StringBuilder();

        try {
            for (int i=0; i < components.length; i++) {
                path.append(URLEncoder.encode(components[i], "UTF-8" ));
                if ((i + 1) < components.length)
                    path.append("/");
            }
        } catch(UnsupportedEncodingException e) {
            throw new CongressException(e, "Unicode not supported on this device somehow.");
        }
        path.append(".json");

        // cobble together any needed query string params
        if (params == null) params = new HashMap<>();

        // Only use for query string is an "offset" for pagination as needed
        if (page > 0) {
            int offset = (page - 1) * PER_PAGE;
            params.put("offset", String.valueOf(offset));
        }

        StringBuilder query = new StringBuilder();

        if (!params.keySet().isEmpty()) {
            query.append("?");
        }

        Iterator<String> iterator = params.keySet().iterator();

        try {
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = params.get(key);

                query.append(URLEncoder.encode(key, "UTF-8"));
                query.append("=");
                query.append(URLEncoder.encode(value, "UTF-8"));
                if (iterator.hasNext())
                    query.append("&");
            }
        } catch (UnsupportedEncodingException e) {
            throw new CongressException(e, "Unicode not supported on this device somehow.");
        }

        return baseUrl + path.toString() + query.toString();
    }

    public static String fetchJSON(String url) throws CongressException {
        Log.d(Utils.TAG, "Pro Publica API: " + url);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .build();

        Headers headers = new Headers.Builder()
                .add("User-Agent", userAgent)
                .add("X-API-Key", apiKey)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new CongressException.NotFound("404 Not Found from " + url);
            }
            if(response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            throw new CongressException(e, "Problem fetching JSON from " + url);
        }
        return "";
    }

    public static JSONObject firstResult(String url) throws CongressException {
        JSONArray results = resultsFor(url);
        if (results.length() > 0) {
            try {
                return (JSONObject) results.get(0);
            } catch(JSONException e) {
                throw new CongressException(e, "Error getting first result from " + url);
            }
        } else
            return null;
    }

    public static JSONArray resultsFor(String url) throws CongressException {
        String rawJSON = fetchJSON(url);
        JSONArray results = null;
        try {
            JSONObject response = new JSONObject(rawJSON);

            // First check that the Pro Publica API said 'OK'
            String status = response.getString("status");
            if (!status.equals("OK"))
                throw new CongressException("Got a non-OK status from " + url + "\n\n" + rawJSON);

            results = response.getJSONArray("results");

        } catch(JSONException e) {
            throw new CongressException(e, "Problem parsing the JSON from " + url);
        }

        return results;
    }


    // assumes date stamps are in "YYYY-MM-DD" format, which they will be.
    // Date objects automatically assign a time of midnight, but these dates are meant to represent whole days.
    // If we read these in as UTC, or even EST (Congress' time), then when formatted for display in the user's local timezone,
    // they could be printed as the day before the one they represent.
    // To work around Java/Android not having a class that represents a time-less day, we force the hour to be noon UTC,
    // which means that no matter which timezone it is formatted as, it will be the same day.
    public static Date parseDateOnly(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(dateOnlyFormat, Locale.US);
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        format.setTimeZone(gmt);
        Calendar calendar = new GregorianCalendar(gmt);
        calendar.setTime(format.parse(date));
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        return calendar.getTime();
    }

    // BYO timestamp format
    public static Date parseTimestamp(String timestamp, String format) throws ParseException {
        SimpleDateFormat simpleFormat = new SimpleDateFormat(format, Locale.US);
        simpleFormat.setTimeZone(CONGRESS_TIMEZONE);
        return simpleFormat.parse(timestamp);
    }
}
