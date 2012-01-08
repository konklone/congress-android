package com.sunlightlabs.android.congress.utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

/**
 * Various static methods that other classes can use to fetch legislator profile images,
 * and cause them to be downloaded and cached to disk.
 */

public class LegislatorImage {
	public static final String PIC_SMALL = "40x50";
	public static final String PIC_MEDIUM = "100x125";
	public static final String PIC_LARGE = "200x250";
	
	// 30 day expiration time on cached legislator avatars
	public static final long CACHE_IMAGES = (long) 1000 * 60 * 60 * 24 * 30;
	
	// should be called from within a background task, as this performs a network call
	public static BitmapDrawable getImage(String size, String bioguideId, Context context) {
		BitmapDrawable drawable = quickGetImage(size, bioguideId, context);
		if (drawable == null) {
			cacheImage(size, bioguideId, context);
			
			if (context != null) // activity may have disappeared while the image was being downloaded
				drawable = quickGetImage(size, bioguideId, context);
		}
		return drawable;
	}
	
	// will not make a network call, if file exists on disk you get the drawable, otherwise null
	public static BitmapDrawable quickGetImage(String size, String bioguideId, Context context) {
		File imageFile = new File(picPath(size, bioguideId, context));
		if (!imageFile.exists() || tooOld(imageFile))
			return null;
		else
			return new BitmapDrawable(picPath(size, bioguideId, context));
	}
	
	// assumes you've already checked to make sure the file exists
	public static boolean tooOld(File file) {
		return file.lastModified() < (System.currentTimeMillis() - CACHE_IMAGES);
	}
	
	public static String picUrl(String size, String bioguideId) {
		return "http://assets.sunlightfoundation.com/moc/" + size + "/" + bioguideId + ".jpg";
	}
	
	public static String picPath(String size, String bioguideId, Context context) {
		return picDir(bioguideId, context) + size + ".jpg"; 
	}
	
	public static String picDir(String bioguideId, Context context) {
		File cacheDir = context.getCacheDir();
		if (cacheDir == null)
			cacheDir = context.getFilesDir();
		File picDir = new File(cacheDir.getPath() + "/" + bioguideId);
		picDir.mkdirs();
		return picDir.getPath();
	}
	
	public static void cacheImage(String size, String bioguideId, Context context) {
		File outFile = new File(picPath(size, bioguideId, context));
		if (outFile.exists())
			outFile.delete();
		
		String url = picUrl(size, bioguideId);
		downloadFile(url, outFile);
	}
	
	public static void downloadFile(String url, File outputFile) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	byte[] buffer = EntityUtils.toByteArray(response.getEntity());
	        	DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
                fos.write(buffer);
                fos.flush();
                fos.close();
	        } else if (statusCode == HttpStatus.SC_NOT_FOUND)
	        	return;
	        else
	        	return;
        } catch (ClientProtocolException e) {
        	return;
	    } catch (IOException e) {
	    	Log.e(Utils.TAG, "IO Exception on getting legislator photo", e);
	    	return;
	    }
	}
	
}
