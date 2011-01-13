package com.sunlightlabs.android.congress.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

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
	
	public static Bitmap shortcutImage(String bioguideId, Context context) {
		Bitmap profile, scaled;
		
		/* This needs to stay commented until we phase out Android 1.5. 
		 * Then, we can start making more optimized versions of the shortcut icon
		 * for each screen resolution.
		 * 
		 * Specifically, we can't refer explicitly to the densityDpi value from DisplayMetrics
		 * without causing INSANE, ARCANE, STUPID errors on Android 1.5.
		 * 
		 */
//		int density = context.getResources().getDisplayMetrics().densityDpi;
//		switch (density) {
//		case DisplayMetrics.DENSITY_LOW:
//			profile = getImage(PIC_SMALL, bioguideId, context).getBitmap();
//			scaled = Bitmap.createScaledBitmap(profile, 32, 40, true);
//			return Bitmap.createBitmap(scaled, 0, 2, scaled.getWidth(), scaled.getHeight() - 4);
//		case DisplayMetrics.DENSITY_MEDIUM:
//			// this will be a 40x50 image, that I want to turn into a 40x48 image
//			profile = getImage(PIC_SMALL, bioguideId, context).getBitmap();
//			return Bitmap.createBitmap(profile, 0, 1, profile.getWidth(), profile.getHeight()-2);
//		case DisplayMetrics.DENSITY_HIGH:
//		default:
			// will be a 100x125 image, I want to scale it down to 60x75, and then chop 3 lines off of it
			BitmapDrawable drawable = getImage(PIC_MEDIUM, bioguideId, context);
			if (drawable == null)
				return null;
			
			profile = drawable.getBitmap();
			scaled = Bitmap.createScaledBitmap(profile, 60, 75, true);
			return Bitmap.createBitmap(scaled, 0, 1, scaled.getWidth(), scaled.getHeight() - 3);
//		}
		
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
		try {
			URL u = new URL(url);
			URLConnection conn = u.openConnection();
			int contentLength = conn.getContentLength();
			if (contentLength < 0)
				return; // bad connection, I guess - return null to throw up the No Photo image
			
			DataInputStream stream = new DataInputStream(u.openStream());
			
	        byte[] buffer = new byte[contentLength];
	        stream.readFully(buffer);
	        stream.close();
	        
	        DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
	        fos.write(buffer);
	        fos.flush();
	        fos.close();
		} catch (FileNotFoundException e) {
			return; // swallow a 404, we'll throw up a No Photo image
		} catch (IOException e) {
			return; // swallow a 404, we'll throw up a No Photo image
		}
	}
	
}
