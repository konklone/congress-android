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
import android.graphics.drawable.BitmapDrawable;

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
		try {
			URL u = new URL(url);
			URLConnection conn = u.openConnection();
			int contentLength = conn.getContentLength();
			if (contentLength < 0)
				return; // bad connection, I guess - return null to throw up the
						// No Photo image

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
