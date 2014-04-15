package com.sunlightlabs.android.congress.utils;


/**
 * Singleton class to manage HTTP interaction between OkHttp and HttpUrlConnection.
 */
public class HttpManager {

    public static HttpManager instance;

    public static void init() {
        if (instance == null)
            instance = new HttpManager();
    }

    public HttpManager() {
//        OkHttpClient okHttpClient = new OkHttpClient();

        // adapted from https://github.com/mapbox/mapbox-android-sdk/pull/244
//        SSLContext sslContext;
//        try {
//            sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, null, null);
//        } catch (GeneralSecurityException e) {
//            throw new AssertionError(); // The system has no TLS. Just give up.
//        }
//        okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());

//        Log.w(Utils.TAG, "Initializing an OkHttpClient instance as the URL stream handler factory forevermore.");
//        URL.setURLStreamHandlerFactory(okHttpClient);
    }
}
