package com.sunlightlabs.android.twitter;

public class TwitterException extends Exception {
    private static final long serialVersionUID = -2623309261327497087L;

    public TwitterException(Exception cause) {
        super(cause);
    }

    public TwitterException(String msg) {
        super(msg);
    }
}