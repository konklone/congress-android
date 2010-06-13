package com.sunlightlabs.youtube;

public class YouTubeException extends Exception {
	private static final long serialVersionUID = -2623309231327497087L;

    public YouTubeException(Exception cause) {
        super(cause);
    }

    public YouTubeException(String msg) {
        super(msg);
    }
}