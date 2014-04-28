package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.sunlightlabs.congress.models.CongressException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Various static methods that other classes can use to fetch legislator profile images,
 * and cause them to be downloaded and cached to disk.
 */

public class LegislatorImage {
    public static final String USER_AGENT = "Sunlight's Congress Android App (https://github.com/sunlightlabs/congress-android)";
	public static final String PIC_LARGE = "450x550";
    public static final String PIC_SMALL = "225x275";
	
	// 30 day expiration time on cached legislator avatars
	public static final long CACHE_IMAGES = (long) 1000 * 60 * 60 * 24 * 30;
	
	// should be called from within a background task, as this performs a network call
	public static BitmapDrawable getImage(String size, String bioguideId, Context context) {
		if (context == null) // if we've lost the activity, abandon it
			return null;
		
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
			return new BitmapDrawable(context.getResources(), picPath(size, bioguideId, context));
	}
	
	// assumes you've already checked to make sure the file exists
	public static boolean tooOld(File file) {
		return file.lastModified() < (System.currentTimeMillis() - CACHE_IMAGES);
	}
	
	public static String picUrl(String size, String bioguideId) {
		return "http://theunitedstates.io/images/congress/" + size + "/" + bioguideId + ".jpg";
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
        try {
            downloadFile(url, outFile);
        } catch (CongressException e) {
            // swallow!
        }
	}

    // adapted from HttpClient's EntityUtils
    public static byte[] toByteArray(HttpURLConnection connection) throws IOException {
        if (connection == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }

        InputStream instream = connection.getInputStream();
        if (instream == null) {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            byte[] tmp = new byte[4096];
            int l;
            while((l = instream.read(tmp)) != -1) {
                buffer.write(tmp, 0, l);
            }
        } finally {
            instream.close();
        }
        return buffer.toByteArray();
    }
	
	public static void downloadFile(String url, File outputFile) throws CongressException {
        Log.d(Utils.TAG, "Member photo: " + url);

        // play nice with OkHttp
        HttpManager.init();

        HttpURLConnection connection;
        URL theUrl;

        try {
            theUrl = new URL(url);
            connection = (HttpURLConnection) theUrl.openConnection();
        } catch(MalformedURLException e) {
            throw new CongressException(e, "Bad URL: " + url);
        } catch (IOException e) {
            throw new CongressException(e, "Problem opening connection to " + url);
        }

        try {
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                // read input stream first to fetch response headers
                byte[] buffer = toByteArray(connection);
                DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
                fos.write(buffer);
                fos.flush();
                fos.close();
            } else
                return;
        } catch (IOException e) {
            Log.e(Utils.TAG, "IO Exception on getting legislator photo", e);
            throw new CongressException(e, "IO Exception on getting legislator photo");
        } finally {
            connection.disconnect();
        }
	}
	
}
