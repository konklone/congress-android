package com.sunlightlabs.android.congress.utils;


import com.squareup.okhttp.OkHttpClient;

import java.net.URL;

/**
 * Singleton class to manage HTTP interaction between OkHttp and HttpUrlConnection.
 */
public class HttpManager {

    public static HttpManager instance;

    public static HttpManager init() {
        if (instance == null)
            instance = new HttpManager();
        return instance;
    }

    public HttpManager() {
        OkHttpClient okHttpClient = new OkHttpClient();
        URL.setURLStreamHandlerFactory(okHttpClient);
    }
}
