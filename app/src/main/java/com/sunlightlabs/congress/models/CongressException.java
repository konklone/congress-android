package com.sunlightlabs.congress.models;

public class CongressException extends Exception {
	
	private static final long serialVersionUID = -2623309261327198187L;
    private String msg;
    
    public CongressException(String msg) {
    	super(msg);
    	this.msg = msg;
    }
    
    public CongressException(Exception e, String msg) {
    	super(e);
    	this.msg = msg;
    }
    
    public String getMessage() {
    	return this.msg;
    }
    
    public static class NotFound extends CongressException {
    	public NotFound(String msg) {
    		super(msg);
    	}
    	private static final long serialVersionUID = -2623309261327198188L;
    }
    
    public static class BehindFirewall extends CongressException {
    	public BehindFirewall(String msg) {
    		super(msg);
    	}
    	
    	private static final long serialVersionUID = -2623309261327198189L;
    }
}
