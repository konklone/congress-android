package winterwell.jtwitter;




/**
 * A runtime exception for when Twitter requests don't work. All
 * {@link Twitter} methods can throw this.
 * <p>
 * This contains several subclasses which should be thrown to mark
 * different problems. Error handling is particulalry important as
 * Twitter tends to be a bit flaky.
 * <p>
 * I believe unchecked exceptions are preferable to checked ones,
 * because they avoid the problems caused by swallowing exceptions.
 * But if you don't like runtime exceptions, just edit this class.
 * 
 * @author Daniel Winterstein
 */
public class TwitterException extends RuntimeException {
	/**
	 * A timeout exception - probably caused by Twitter being overloaded.
	 */
	public static class Timeout extends TwitterException {
		public Timeout(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
	}
	/**
	 * A code 50X error (e.g. 502) - indicating something went wrong at 
	 * Twitter's end. The API equivalent of the Fail Whale. 
	 * Usually retrying in a minute will fix this.
	 */
	public static class E50X extends TwitterException {
		public E50X(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
	}
	/**
	 * A Forbidden exception
	 * @author daniel
	 *
	 */
	public static class E403 extends TwitterException {
		public E403(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
	}

	private static final long serialVersionUID = 1L;
	
	private String additionalInfo = "";
	
	/**
	 * Wrap an exception as a TwitterException.
	 */
	TwitterException(Exception e) {
		super(e);
		// avoid gratuitous nesting of exceptions
		assert !(e instanceof TwitterException) : e;
	}

	/**
	 * @param string
	 */
	public TwitterException(String string) {
		super(string);
	}
	
	public TwitterException(String string, String additionalInfo) {
		this(string);
		this.setAdditionalInfo(additionalInfo);
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	/**
	 * Indicates a 404: resource does not exist error from Twitter.
	 * Note: Can be throw in relation to suspended users.
	 */
	public static class E404 extends TwitterException {
		public E404(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;		
	}
	/**
	 * Indicates a rate limit error (i.e. you've over-used Twitter)
	 */
	public static class RateLimit extends TwitterException {
		public RateLimit(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;		
	}
}
