package com.sunlightlabs.google.news;

public class NewsException extends Exception {
    public int statusCode = -1;
    private static final long serialVersionUID = -2623309261327498087L;

    public NewsException(Exception cause) {
        super(cause);
    }

    public NewsException(String msg, int statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return this.statusCode;
    }
	
}