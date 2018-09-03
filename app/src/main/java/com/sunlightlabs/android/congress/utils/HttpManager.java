package com.sunlightlabs.android.congress.utils;

import android.util.Log;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import okhttp3.OkHttpClient;

/**
 * Singleton class to manage HTTP interaction between OkHttp and HttpUrlConnection.
 */
public class HttpManager {

    public static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        OkHttpClient okHttpClient = new OkHttpClient();

        // adapted from https://github.com/mapbox/mapbox-android-sdk/pull/244
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(); // The system has no TLS. Just give up.
        }
		//okHttpClient.sslSocketFactory(SSLContext.getSocketFactory());

        Log.w(Utils.TAG, "Initializing an OkHttpClient instance as the URL stream handler factory forevermore.");
		URL.setURLStreamHandlerFactory((URLStreamHandlerFactory) okHttpClient);
    }
}