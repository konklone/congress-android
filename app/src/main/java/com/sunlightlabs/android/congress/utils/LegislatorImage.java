package com.sunlightlabs.android.congress.utils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.sunlightlabs.android.congress.R;

import java.io.IOException;

/**
 * Various static methods that other classes can use to fetch legislator profile images,
 * and cause them to be downloaded and cached to disk.
 */


/**
 * LegislatoryImage adaptor to the Picasso image caching library.
 */
public class LegislatorImage {
	public static final String PIC_LARGE = "450x550";
    public static final String PIC_SMALL = "225x275";

	/**
	 * Return the URL for a given bioguideID and size pair
	 */
	private static String getImageURL(String bioguideId, String size) {
		return "https://theunitedstates.io/images/congress/" + size + "/" + bioguideId + ".jpg";
	}

	/**
	 * Get an image synchronously, used for Tasks that require the Drawable.
	 *
	 * This is mostly used as a shim / adaptor to allow code to use Picasso without a major
	 * overhaul. This should likely be refactored out at some point in favor of the Picasso
	 * async loader.
	 */
	public static Drawable getImage(String bioguideId, String imageSize, Context context) {
        if (context == null) return null;

        String url = LegislatorImage.getImageURL(bioguideId, imageSize);
		RequestCreator rc = Picasso.with(context)
				.load(url)
				.placeholder(R.drawable.loading_photo);

		try {
			return new BitmapDrawable(context.getResources(), rc.get());
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Adaptor to load in a Picasso image by BioguideID
	 */
	public static void setImageView(
			String bioguideId,
			String imageSize,
			Context context,
			ImageView imageView
	) {
		String url = LegislatorImage.getImageURL(bioguideId, imageSize);
		Picasso.with(context)
				.load(url)
				.placeholder(R.drawable.loading_photo)
				.into(imageView);
	}

}
