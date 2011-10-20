package com.sunlightlabs.android.congress.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class ImageUtils {
	public static final String YOUTUBE_THUMB = "120x90";

	// should be called from within a background task, as this performs a
	// network call
	public static BitmapDrawable getImage(String size, String url, Context context) {
		int hash = url.hashCode();
		BitmapDrawable drawable = quickGetImage(size, hash, context);
		if (drawable == null) {
			cacheImage(size, hash, url, context);
			drawable = quickGetImage(size, hash, context);
		}
		return drawable;
	}

	// will not make a network call, if file exists on disk you get the
	// drawable, otherwise null
	public static BitmapDrawable quickGetImage(String size, int hash, Context context) {
		File imageFile = new File(picPath(size, hash, context));
		if (!imageFile.exists())
			return null;
		else
			return new BitmapDrawable(picPath(size, hash, context));
	}

	public static String picPath(String size, int hash, Context context) {
		return picDir(hash, context) + size + ".jpg";
	}

	public static String picDir(int hash, Context context) {
		File cacheDir = context.getCacheDir();
		if (cacheDir == null)
			cacheDir = context.getFilesDir();
		File picDir = new File(context.getCacheDir().getPath() + "/" + hash);
		picDir.mkdirs();
		return picDir.getPath();
	}

	public static void cacheImage(String size, int hash, String url, Context context) {
		File outFile = new File(picPath(size, hash, context));
		if (outFile.exists())
			outFile.delete();

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
