package winterwell.jtwitter;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.TwitterException.E403;

/**
 * Java wrapper for the Twitter API version 1.2.2
 * <p>
 * Example usage:
 * <code><pre>
	// Make a Twitter object
	Twitter twitter = new Twitter("my-name","my-password");
	// Print Winterstein's status
	System.out.println(twitter.getStatus("winterstein"));
	// Set my status
	twitter.updateStatus("Messing about in Java");
</pre></code>
 * <p>
 * See {@link http://www.winterwell.com/software/jtwitter.php} for more
 * information about this wrapper. See
 * {@link http://apiwiki.twitter.com/Twitter-API-Documentation} for more
 * information about the Twitter API.
 * <p>
 * Notes:
 * <ul>
 * <li>This wrapper takes care of all url-encoding/decoding.
 * <li>This wrapper will throw a runtime exception (TwitterException) if a
 * methods fails, e.g. it cannot connect to Twitter.com or you make a bad
 * request. I would like to improve error-handling, and welcome suggestions on
 * cases where more informative exceptions would be helpful.
 * </ul>
 * 
 * <h4>Copyright and License</h4>
 * This code is copyright (c) Winterwell Associates 2008/2009 and (c) ThinkTank
 * Mathematics Ltd, 2007 except where otherwise stated. It is released as
 * open-source under the LGPL license. See <a
 * href="http://www.gnu.org/licenses/lgpl.html"
 * >http://www.gnu.org/licenses/lgpl.html</a> for license details. This code
 * comes with no warranty or support.
 * 
 * <h4>Change List</h4>
 * The change list is kept online at: {@link http://www.winterwell.com/software/changelist.txt}
 * 
 * @author Daniel Winterstein
 */
public class Twitter {

	/**
	 * Interface for an http client - e.g. allows for OAuth to be used instead.
	 * The default version is {@link URLConnectionHttpClient}.
	 * <p>
	 * If creating your own version, please provide support for throwing
	 * the right subclass of TwitterException - see {@link URLConnectionHttpClient#processError(java.net.HttpURLConnection)}
	 * for example code.
	 * 
	 * @author Daniel Winterstein
	 */
	public static interface IHttpClient {
		/** Whether this client can authenticate to the server. */
		boolean canAuthenticate();

		/**
		 * Send an HTTP GET request and return the response body. Note that this
		 * will change all line breaks into system line breaks!
		 * 
		 * @throws TwitterException for a variety of reasons
		 * @throws TwitterException.E404 for resource-does-not-exist errors
		 */
		String getPage(String uri, Map<String, String> vars,
				boolean authenticate) throws TwitterException;

		/**
		 * Send an HTTP POST request and return the response body.
		 * 
		 * @param uri
		 *            The uri to post to.
		 * @param vars
		 *            The form variables to send. These are URL encoded before
		 *            sending.
		 * @param authenticate
		 *            If true, send user authentication
		 * @return The response from the server.
		 * 
		 * @throws TwitterException for a variety of reasons
		 * @throws TwitterException.E404 for resource-does-not-exist errors
		 */
		String post(String uri, Map<String, String> vars, boolean authenticate)
		throws TwitterException;

	}

	/**
	 * This gives common access to features that are common to both
	 * {@link Message}s and {@link Status}es.
	 * 
	 * @author daniel
	 * 
	 */
	public static interface ITweet {

		Date getCreatedAt();

		/**
		 * @return The Twitter id for this post. This is used by some API
		 *         methods.
		 */
		long getId();

		/** The actual status text. This is also returned by {@link #toString()} */
		String getText();

		/** The User who made the tweet */
		User getUser();

	}

	/**
	 * A Twitter direct message. Fields are null if unset.
	 * <p>
	 * Historical note: this class used to cover \@you mentions as well,
	 * but these are better described by Statuses.
	 */
	public static final class Message implements ITweet {

		/**
		 * 
		 * @param json
		 * @return
		 * @throws TwitterException
		 */
		static List<Message> getMessages(String json)
		throws TwitterException {
			if (json.trim().equals(""))
				return Collections.emptyList();
			try {
				List<Message> msgs = new ArrayList<Message>();
				JSONArray arr = new JSONArray(json);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					Message u = new Message(obj);
					msgs.add(u);
				}
				return msgs;
			} catch (JSONException e) {
				throw new TwitterException(e);
			}
		}

		private final Date createdAt;
		private final long id;
		private final User recipient;
		private final User sender;
		private final String text;

		/**
		 * @param obj
		 * @throws JSONException
		 * @throws TwitterException
		 */
		Message(JSONObject obj) throws JSONException,
		TwitterException {
			id = obj.getLong("id");
			text = obj.getString("text");
			String c = jsonGet("created_at", obj);
			createdAt = new Date(c);
			sender = new User(obj.getJSONObject("sender"), null);
			// recipient - for messages you sent
			if (obj.has("recipient")) {
				recipient = new User(obj.getJSONObject("recipient"), null);
			} else {
				recipient = null;
			}
		}

		public Date getCreatedAt() {
			return createdAt;
		}

		public long getId() {
			return id;
		}

		/**
		 * @return the recipient (for messages sent by the authenticating user)
		 */
		public User getRecipient() {
			return recipient;
		}

		public User getSender() {
			return sender;
		}

		public String getText() {
			return text;
		}

		/**
		 * This is equivalent to {@link #getSender()}
		 */
		public User getUser() {
			return getSender();
		}


		@Override
		public String toString() {
			return text;
		}

	}

	/**
	 * A Twitter status post. .toString() returns the status text.
	 * <p>
	 * Notes: This is a finalised data object. It has no methods but exposes its
	 * fields. If you want to change your status, use
	 * {@link Twitter#updateStatus(String)} and
	 * {@link Twitter#destroyStatus(Status)}.
	 */
	public static final class Status implements ITweet {
		/**
		 * Convert from a json array of objects into a list of tweets.
		 * @param json can be empty, must not be null
		 * @throws TwitterException
		 */
		static List<Status> getStatuses(String json) throws TwitterException {
			if (json.trim().equals(""))
				return Collections.emptyList();
			try {
				List<Status> tweets = new ArrayList<Status>();
				JSONArray arr = new JSONArray(json);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					Status tweet = new Status(obj, null);
					tweets.add(tweet);
				}
				return tweets;
			} catch (JSONException e) {
				throw new TwitterException(e);
			}
		}

		/**
		 * Search results use a slightly different protocol! In particular
		 * w.r.t. user ids and info.
		 * 
		 * @param searchResults
		 * @return search results as Status objects - but with dummy users!
		 * The dummy users have a screenname and a profile image url, but
		 * no other information. This reflects the current behaviour of the Twitter API.
		 */
		static List<Status> getStatusesFromSearch(Twitter tw,
				JSONObject searchResults) {
			try {
				List<Status> users = new ArrayList<Status>();
				JSONArray arr = searchResults.getJSONArray("results");
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					String userScreenName = obj.getString("from_user");
					String profileImgUrl = obj.getString("profile_image_url");
					User user = new User(userScreenName);
					user.profileImageUrl = URI(profileImgUrl);
					Status s = new Status(obj, user);
					users.add(s);
				}
				return users;
			} catch (JSONException e) {
				throw new TwitterException(e);
			}
		}

		public final Date createdAt;
		public final long id;
		/** The actual status text. */
		public final String text;

		public final User user;

		/**
		 * E.g. "web" vs. "im"
		 */
		public final String source;

		/**
		 * regex for @you mentions
		 */
		static final Pattern AT_YOU_SIR = Pattern.compile("@(\\w+)");

		/**
		 * @param object
		 * @param user
		 *            Set when parsing the json returned for a User
		 * @throws TwitterException
		 */
		Status(JSONObject object, User user) throws TwitterException {
			try {
				id = object.getLong("id");
				text = jsonGet("text", object);
				String c = jsonGet("created_at", object);
				createdAt = new Date(c);
				source = jsonGet("source", object);
				if (user != null) {
					this.user = user;
				} else {
					JSONObject jsonUser = object.optJSONObject("user");
					this.user = new User(jsonUser, this);
				}
			} catch (JSONException e) {
				throw new TwitterException(e);
			}
		}

		public Date getCreatedAt() {
			return createdAt;
		}

		/**
		 * @return The Twitter id for this post. This is used by some API
		 *         methods.
		 */
		public long getId() {
			return id;
		}

		/**
		 * @return list of \@mentioned people  (there is no guarantee that
		 * these mentions are for correct Twitter screen-names). May be empty,
		 * never null. Screen-names are always lowercased.
		 */
		public List<String> getMentions() {
			Matcher m = AT_YOU_SIR.matcher(text);
			List<String> list = new ArrayList<String>(2);
			while(m.find()) {
				// skip email addresses (and other poorly formatted things)
				if (m.start()!=0
						&& text.charAt(m.start()-1) != ' ') continue;
				String mention = m.group(1);
				// enforce lower case
				list.add(mention.toLowerCase());
			}
			return list;
		}

		/** The actual status text. This is also returned by {@link #toString()} */
		public String getText() {
			return text;
		}

		public User getUser() {
			return user;
		}

		/**
		 * @return The text of this status. E.g. "Kicking fommil's arse at
		 *         Civilisation."
		 */

		@Override
		public String toString() {
			return text;
		}
	}
	/**
	 * A Twitter user. Fields are null if unset.
	 * 
	 * @author daniel
	 */
	public static final class User {
		static List<User> getUsers(String json) throws TwitterException {
			if (json.trim().equals(""))
				return Collections.emptyList();
			try {
				List<User> users = new ArrayList<User>();
				JSONArray arr = new JSONArray(json);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					User u = new User(obj, null);
					users.add(u);
				}
				return users;
			} catch (JSONException e) {
				throw new TwitterException(e);
			}
		}

		public final String description;
		public final long id;
		public final String location;
		/** The display name, e.g. "Daniel Winterstein" */
		public final String name;
		/** we allow this to be edited as a convenience for the User
		 * objects generated by search */
		public URI profileImageUrl;
		public final boolean protectedUser;
		/**
		 * The login name, e.g. "winterstein" This is the only thing used by
		 * equals() and hashcode(). This is always lower-case, as Twitter
		 * screen-names are case insensitive.
		 */
		public final String screenName;
		public final Status status;
		public final URI website;
		/**
		 * Number of seconds between a user's registered time zone and Greenwich
		 * Mean Time (GMT) - aka Coordinated Universal Time or UTC. Can be
		 * positive or negative.
		 */
		public final int timezoneOffSet;
		public final String timezone;
		public long followersCount;
		public final String profileBackgroundColor;
		public final String profileLinkColor;
		public final String profileTextColor;
		public final String profileSidebarFillColor;
		public final String profileSidebarBorderColor;

		public final long friendsCount;

		public final String createdAt;

		public final long favoritesCount;

		public final URI profileBackgroundImageUrl;

		public final boolean profileBackgroundTile;

		public final long statusesCount;

		public final boolean notifications;

		public final boolean verified;
		public final boolean following;
		User(JSONObject obj, Status status) throws TwitterException {
			try {
				id = obj.getLong("id");
				name = jsonGet("name", obj);
				screenName = jsonGet("screen_name", obj).toLowerCase();
				location = jsonGet("location", obj);
				description = jsonGet("description", obj);
				String img = jsonGet("profile_image_url", obj);
				profileImageUrl = img == null ? null : URI(img);
				String url = jsonGet("url", obj);
				website = url == null ? null : URI(url);
				protectedUser = obj.getBoolean("protected");
				followersCount = obj.getLong("followers_count");
				profileBackgroundColor = obj.getString("profile_background_color");
				profileLinkColor = obj.getString("profile_link_color");
				profileTextColor = obj.getString("profile_text_color");
				profileSidebarFillColor = obj.getString("profile_sidebar_fill_color");
				profileSidebarBorderColor = obj.getString("profile_sidebar_border_color");
				friendsCount = obj.getLong("friends_count");
				createdAt = obj.getString("created_at");
				favoritesCount = obj.getLong("favourites_count");
				String utcOffSet = jsonGet("utc_offset", obj);
				timezoneOffSet = utcOffSet == null ? 0 : Integer.parseInt(utcOffSet);
				timezone = jsonGet("time_zone", obj);
				img = jsonGet("profile_background_image_url", obj);
				profileBackgroundImageUrl = img == null ? null : URI(img);
				profileBackgroundTile = obj.getBoolean("profile_background_tile");
				statusesCount = obj.getLong("statuses_count");
				notifications = obj.optBoolean("notifications");
				verified = obj.getBoolean("verified");
				following = obj.optBoolean("following");
				// status
				if (status == null) {
					JSONObject s = obj.optJSONObject("status");
					this.status = s == null ? null : new Status(s, this);
				} else {
					this.status = status;
				}
			}
			catch (JSONException e) {
				throw new TwitterException(e);
			}
		}
		/**
		 * Create a dummy User object. All fields are set to null. This will be
		 * equals() to an actual User object, so it can be used to query
		 * collections. E.g. <code><pre>
		 * // Test whether jtwit is a friend
		 * twitter.getFriends().contains(new User("jtwit"));
		 * </pre></code>
		 * 
		 * @param screenName This will be converted to lower-case as
		 * Twitter screen-names are case insensitive
		 */
		public User(String screenName) {
			id = -1;
			name = null;
			this.screenName = screenName.toLowerCase();
			status = null;
			location = null;
			description = null;
			profileImageUrl = null;
			website = null;
			protectedUser = false;
			followersCount = 0;
			profileBackgroundColor = null;
			profileLinkColor = null;
			profileTextColor = null;
			profileSidebarFillColor = null;
			profileSidebarBorderColor = null;
			friendsCount = 0;
			createdAt = null;
			favoritesCount = 0;
			timezoneOffSet = -1;
			timezone = null;
			profileBackgroundImageUrl = null;
			profileBackgroundTile = false;
			statusesCount = 0;
			notifications = false;
			verified = false;
			following = false;
		}
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof User))
				return false;
			User ou = (User) other;
			if (screenName.equals(ou.screenName))
				return true;
			return false;
		}
		public String getCreatedAt()
		{
			return createdAt;
		}
		public String getDescription() {
			return description;
		}
		/** number of statuses a user has marked as favorite */
		public long getFavoritesCount()
		{
			return favoritesCount;
		}
		public long getFollowersCount()
		{
			return followersCount;
		}

		public long getFriendsCount()
		{
			return friendsCount;
		}

		public long getId() {
			return id;
		}

		public String getLocation() {
			return location;
		}

		/** The display name, e.g. "Daniel Winterstein" */
		public String getName() {
			return name;
		}

		public String getProfileBackgroundColor()
		{
			return profileBackgroundColor;
		}

		public URI getProfileBackgroundImageUrl() {
			return profileBackgroundImageUrl;
		}

		public URI getProfileImageUrl() {
			return profileImageUrl;
		}

		public String getProfileLinkColor()
		{
			return profileLinkColor;
		}

		public String getProfileSidebarBorderColor()
		{
			return profileSidebarBorderColor;
		}

		public String getProfileSidebarFillColor()
		{
			return profileSidebarFillColor;
		}

		public String getProfileTextColor()
		{
			return profileTextColor;
		}

		public boolean getProtectedUser()
		{
			return protectedUser;
		}

		/** The login name, e.g. "winterstein" */
		public String getScreenName() {
			return screenName;
		}

		public Status getStatus() {
			return status;
		}

		public long getStatusesCount() {
			return statusesCount;
		}

		/**
		 * String version of the timezone
		 */
		public String getTimezone() {
			return timezone;
		}




		/**
		 * Number of seconds between a user's registered time zone and Greenwich
		 * Mean Time (GMT) - aka Coordinated Universal Time or UTC. Can be
		 * positive or negative.
		 */
		public int getTimezoneOffSet() {
			return timezoneOffSet;
		}

		public URI getWebsite() {
			return website;
		}

		public int hashCode() {
			return screenName.hashCode();
		}

		/**
		 * @return true if this is a dummy User object, in which case almost
		 * all of it's fields will be null - with the exception of screenName.
		 * Dummy User objects are equals() to full User objects.
		 */
		public boolean isDummyObject() {
			return name==null;
		}

		public boolean isFollowing() {
			return following;
		}

		public boolean isNotifications() {
			return notifications;
		}

		public boolean isProfileBackgroundTile() {
			return profileBackgroundTile;
		}

		public boolean isProtectedUser() {
			return protectedUser;
		}

		/**
		 * @return true if the account has been verified by Twitter to
		 * really be who it claims to be.
		 */
		public boolean isVerified() {
			return verified;
		}

		/**
		 * Returns the User's screenName (i.e. their Twitter login)
		 */
		@Override
		public String toString() {
			return screenName;
		}
	}

	public final static String version = "1.3.0";
	/**
	 * Create a map from a list of key, value pairs. An easy way to make small
	 * maps, basically the equivalent of {@link Arrays#asList(Object...)}.
	 */
	@SuppressWarnings("unchecked")
	private static <K, V> Map<K, V> asMap(Object... keyValuePairs) {
		assert keyValuePairs.length % 2 == 0;
		Map m = new HashMap(keyValuePairs.length / 2);
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			m.put(keyValuePairs[i], keyValuePairs[i + 1]);
		}
		return m;
	}

	/**
	 * Convenience method for making Dates. Because Date is a tricksy bugger of
	 * a class.
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return date object
	 */
	public static Date getDate(int year, String month, int day) {
		try {
			Field field = GregorianCalendar.class.getField(month.toUpperCase());
			int m = field.getInt(null);
			Calendar date = new GregorianCalendar(year, m, day);
			return date.getTime();
		} catch (Exception x) {
			throw new IllegalArgumentException(x.getMessage());
		}
	}

	/**
	 * Convenience method: Finds a user with the given screen-name from the
	 * list.
	 * 
	 * @param screenName
	 *            aka login name
	 * @param users
	 * @return User with the given name, or null.
	 */
	public static User getUser(String screenName, List<User> users) {
		assert screenName != null && users != null;
		for (User user : users) {
			if (screenName.equals(user.screenName))
				return user;
		}
		return null;
	}

	/** Helper method to deal with JSON-in-Java weirdness */
	protected static String jsonGet(String key, JSONObject jsonObj) {
		Object val = jsonObj.opt(key);
		if (val == null)
			return null;
		if (JSONObject.NULL.equals(val))
			return null;
		return val.toString();
	}

	/**
	 * 
	 * @param args Can be used as a command-line tweet tool. To do so,
	 * enter 3 arguments: name, password, tweet
	 * 
	 * If empty, prints version info.
	 */
	public static void main(String[] args) {
		// Post a tweet if we are handed a name, password and tweet
		if (args.length==3) {
			Twitter tw = new Twitter(args[0], args[1]);
			Status s = tw.setStatus(args[2]);
			System.out.println(s);
			return;
		}
		System.out.println("Java interface for Twitter");
		System.out.println("--------------------------");
		System.out.println("Version "+version);
		System.out.println("Released under LGPL by Winterwell Associates Ltd.");
		System.out
		.println("See source code or JavaDoc for details on how to use.");
	}

	/**
	 * Convert to a URI, or return null if this is badly formatted
	 */
	private static URI URI(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			return null; // Bad syntax
		}
	}

	private String sourceApp = "jtwitterlib";

	private Integer pageNumber;

	private Long sinceId;

	private Date sinceDate;

	/**
	 * Provides support for fetching many pages
	 */
	private int maxResults;

	private final IHttpClient http;

	/**
	 * Can be null even if we have authentication when using OAuth
	 */
	private final String name;

	/**
	 * Create a Twitter client without specifying a user.
	 */
	public Twitter() {
		this(null, new URLConnectionHttpClient());
	}

	/**
	 * Java wrapper for the Twitter API.
	 * 
	 * @param name
	 *            the authenticating user's name, if known. Can be null.
	 * @param client
	 */
	public Twitter(String name, IHttpClient client) {
		this.name = name;
		http = client;
	}

	/**
	 * Java wrapper for the Twitter API.
	 * 
	 * @param screenName
	 *            The name of the Twitter user. Only used by some methods. Can
	 *            be null if you avoid methods requiring authentication.
	 * @param password
	 *            The password of the Twitter user. Can be null if you avoid
	 *            methods requiring authentication.
	 */
	public Twitter(String screenName, String password) {
		this(screenName, new URLConnectionHttpClient(screenName, password));
	}

	// private Format format = Format.xml;

	/**
	 * Add in since and page, if set. This is called by methods that return
	 * lists of statuses or messages.
	 * 
	 * @param vars
	 * @return vars
	 */
	private Map<String, String> addStandardishParameters(
			Map<String, String> vars) {
		if (sinceId != null)
			vars.put("since_id", sinceId.toString());
		if (pageNumber != null) {
			vars.put("page", pageNumber.toString());
			// this is used once only
			pageNumber = null;
		}
		return vars;
	}

	/**
	 * Create a map from a list of key/value pairs.
	 * @param keyValuePairs
	 * @return
	 */
	private Map<String, String> aMap(String... keyValuePairs) {
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < keyValuePairs.length; i+=2) {
			map.put(keyValuePairs[i],keyValuePairs[i+1]);
		}
		return map;
	}

	/**
	 * Equivalent to {@link #follow(String)}. C.f.
	 * http://apiwiki.twitter.com/Migrating-to-followers-terminology
	 * 
	 * @param username
	 *            Required. The ID or screen name of the user to befriend.
	 * @return The befriended user.
	 * @deprecated Use {@link #follow(String)} instead, which is equivalent.
	 */
	@Deprecated
	public User befriend(String username) throws TwitterException {
		return follow(username);
	}

	/**
	 * Equivalent to {@link #stopFollowing(String)}.
	 * 
	 * @deprecated Please use {@link #stopFollowing(String)} instead.
	 */
	@Deprecated
	public User breakFriendship(String username) {
		return stopFollowing(username);
	}

	/**
	 * If sinceDate is set, filter keeping only those messages that came after
	 * since date
	 * 
	 * @param list
	 * @return filtered list
	 */
	private <T extends ITweet> List<T> dateFilter(List<T> list) {
		if (sinceDate == null)
			return list;
		ArrayList<T> filtered = new ArrayList<T>(list.size());
		for (T message : list) {
			if (message.getCreatedAt() == null)
				filtered.add(message);
			else if (sinceDate.before(message.getCreatedAt()))
				filtered.add(message);
		}
		return filtered;
	}

	/**
	 * Destroys the status specified by the required ID parameter. The
	 * authenticating user must be the author of the specified status.
	 * 
	 */
	public void destroyStatus(long id) throws TwitterException {
		String page = post("http://twitter.com/statuses/destroy/" + id
				+ ".json", null, true);
		// Note: Sends two HTTP requests to Twitter rather than one: Twitter appears
		// not to make deletions visible until the user's status page is requested.
		flush();
		assert page != null;
	}

	/**
	 * Destroys the given status. Equivalent to {@link #destroyStatus(int)}. The
	 * authenticating user must be the author of the status post.
	 */
	public void destroyStatus(Status status) throws TwitterException {
		destroyStatus(status.getId());
	}

	void flush() {
		// This seems to prompt twitter to update in some cases!
		http.getPage("http://twitter.com/" + name, null, true);
	}

	/**
	 * Start following a user.
	 * 
	 * @param username
	 *            Required. The ID or screen name of the user to befriend.
	 * @return The befriended user, or null if they were already being followed.
	   @throws TwitterException if the user does not exist or has been suspended.
	 */
	public User follow(String username) throws TwitterException {
		if (username == null)
			throw new NullPointerException();
		String page;
		try {
			page = post("http://twitter.com/friendships/create/" + username
					+ ".json", null, true);
			// is this needed? doesn't seem to fix things
			//			http.getPage("http://twitter.com/friends", null, true);
		} catch(E403 e) {
			// check if we've tried to follow someone we're already
			// following
			if (isFollowing(username)) {
				return null;
			}
			throw e;
		}
		try {
			return new User(new JSONObject(page), null);
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Convenience for {@link #follow(String)}
	 * @param user
	 */
	public void follow(User user) {
		follow(user.screenName);
	}

	/**
	 * Returns a list of the direct messages sent to the authenticating user.
	 * <p>
	 * Note: the Twitter API makes this available in rss if that's of interest.
	 */
	public List<Message> getDirectMessages() {
		return getMessages("http://twitter.com/direct_messages.json",
				standardishParameters());
	}

	/**
	 * Returns a list of the direct messages sent *by* the authenticating user.
	 */
	public List<Message> getDirectMessagesSent() {
		return getMessages("http://twitter.com/direct_messages/sent.json",
				standardishParameters());
	}

	/**
	 * Returns a list of the users currently featured on the site with their
	 * current statuses inline.
	 * <p>
	 * Note: This is no longer part of the Twitter API. Support is provided via
	 * other methods.
	 */
	public List<User> getFeatured() throws TwitterException {
		List<User> users = new ArrayList<User>();
		List<Status> featured = getPublicTimeline();
		for (Status status : featured) {
			User user = status.getUser();
			users.add(user);
		}
		return users;
	}

	/**
	 * Returns the IDs of the authenticating user's followers.
	 * 
	 * @throws TwitterException
	 */
	public List<Long> getFollowerIDs() throws TwitterException {
		return getUserIDs("http://twitter.com/followers/ids.json");
	}

	/**
	 * Returns the IDs of the specified user's followers.
	 * 
	 * @param The
	 *            screen name of the user whose followers are to be fetched.
	 * @throws TwitterException
	 */
	public List<Long> getFollowerIDs(String screenName) throws TwitterException {
		return getUserIDs("http://twitter.com/followers/ids/" + screenName
				+ ".json");
	}

	/**
	 * Returns the authenticating user's (latest) followers, each with current
	 * status inline.
	 */
	public List<User> getFollowers() throws TwitterException {
		return getUsers("http://twitter.com/statuses/followers.json");
	}

	/**
	 * Returns the IDs of the authenticating user's friends. (people who
	 * the user follows).
	 * 
	 * @throws TwitterException
	 */
	public List<Long> getFriendIDs() throws TwitterException {
		return getUserIDs("http://twitter.com/friends/ids.json");
	}

	/**
	 * Returns the IDs of the specified user's friends.
	 * 
	 * @param The
	 *            screen name of the user whose friends are to be fetched.
	 * @throws TwitterException
	 */
	public List<Long> getFriendIDs(String screenName) throws TwitterException {
		return getUserIDs("http://twitter.com/friends/ids/" + screenName
				+ ".json");
	}

	/**
	 * Returns the authenticating user's (latest 100) friends, each with current
	 * status inline. NB - friends are people who *you* follow.
	 * <p>
	 * Note that there seems to be a small delay from Twitter in updates to this list.
	 * @throws TwitterException
	 * @see #getFriendIDs()
	 * @see #isFollowing(String)
	 */
	public List<User> getFriends() throws TwitterException {
		return getUsers("http://twitter.com/statuses/friends.json");
	}

	/**
	 * 
	 * Returns the (latest 100) given user's friends, each with current status
	 * inline.
	 * 
	 * @param username
	 *            The ID or screen name of the user for whom to request a list
	 *            of friends.
	 * @throws TwitterException
	 */
	public List<User> getFriends(String username) throws TwitterException {
		return getUsers("http://twitter.com/statuses/friends/" + username
				+ ".json");
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * authenticating user and that user's friends.
	 */
	public List<Status> getFriendsTimeline() throws TwitterException {
		return getStatuses("http://twitter.com/statuses/friends_timeline.json",
				standardishParameters());
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * user (given by id) and that user's friends.
	 * 
	 * @param id
	 *            Specifies the ID or screen name of the user for whom to return
	 *            the friends_timeline.
	 * 
	 */
	public List<Status> getFriendsTimeline(String id) throws TwitterException {
		Map<String, String> map = asMap("id", id);
		addStandardishParameters(map);
		return getStatuses("http://twitter.com/statuses/friends_timeline.json",
				map);
	}

	/**
	 * 
	 * @param url
	 * @param var
	 * @param isPublic
	 *            Value to set for Message.isPublic
	 * @return
	 */
	private List<Message> getMessages(String url, Map<String, String> var) {
		// Default: 1 page
		if (maxResults < 1) {
			List<Message> msgs = Message.getMessages(http.getPage(url, var, true));
			msgs = dateFilter(msgs);
			return msgs;
		}
		// Fetch all pages until we run out
		// -- or Twitter complains in which case you'll get an exception
		pageNumber = 1;
		List<Message> msgs = new ArrayList<Message>();
		while (msgs.size() <= maxResults) {
			String p = http.getPage(url, var, true);
			List<Message> nextpage = Message.getMessages(p);
			nextpage = dateFilter(nextpage);
			msgs.addAll(nextpage);
			if (nextpage.size() < 20)
				break;
			pageNumber++;
			var.put("page", Integer.toString(pageNumber));
		}
		return msgs;
	}

	/**
	 * @return Name of the authenticating user, or null if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the 20 most recent statuses from non-protected users who have set
	 * a custom user icon. Does not require authentication.
	 * <p>
	 * Note: Twitter cache-and-refresh this every 60 seconds, so there is little
	 * point calling it more frequently than that.
	 */
	public List<Status> getPublicTimeline() throws TwitterException {
		return getStatuses("http://twitter.com/statuses/public_timeline.json",
				standardishParameters());
	}

	/**
	 * @return the remaining number of API requests available to the
	 *         authenticating user before the API limit is reached for the
	 *         current hour. <i>If this is negative you should stop using
	 *         Twitter with this login for a bit.</i> Note: Calls to
	 *         rate_limit_status do not count against the rate limit.
	 */
	public int getRateLimitStatus() {
		String json = http
		.getPage("http://twitter.com/account/rate_limit_status.json",
				null, true);
		try {
			JSONObject obj = new JSONObject(json);
			int hits = obj.getInt("remaining_hits");
			return hits;
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Returns the 20 most recent replies/mentions (status updates with
	 * 
	 * @username) to the authenticating user. Replies are only available to the
	 *            authenticating user; you can not request a list of replies to
	 *            another user whether public or protected.
	 *            <p>
	 *            The Twitter API now refers to replies as <i>mentions</i>. We
	 *            have kept the old terminology here.
	 */
	public List<Status> getReplies() throws TwitterException {
		return getStatuses("http://twitter.com/statuses/replies.json",
				standardishParameters());
	}

	/**
	 * @return The current status of the user. null if unset (ie if they have
	 *         never tweeted)
	 */
	public Status getStatus() throws TwitterException {
		Map<String, String> vars = asMap("count", 1);
		String json = http.getPage(
				"http://twitter.com/statuses/user_timeline.json", vars, true);
		List<Status> statuses = Status.getStatuses(json);
		if (statuses.size() == 0)
			return null;
		return statuses.get(0);
	}

	/**
	 * Returns a single status, specified by the id parameter below. The
	 * status's author will be returned inline.
	 * 
	 * @param id
	 *            The numerical ID of the status you're trying to retrieve.
	 */
	public Status getStatus(long id) throws TwitterException {
		String json = http.getPage("http://twitter.com/statuses/show/" + id
				+ ".json", null, true);
		try {
			return new Status(new JSONObject(json), null);
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * @return The current status of the given user, as a normal String.
	 */
	public Status getStatus(String username) throws TwitterException {
		assert username != null;
		Map<String, String> vars = asMap("id", username, "count", 1);
		String json = http.getPage(
				"http://twitter.com/statuses/user_timeline.json", vars, false);
		List<Status> statuses = Status.getStatuses(json);
		if (statuses.size() == 0)
			return null;
		return statuses.get(0);

	}

	private List<Status> getStatuses(String url, Map<String, String> var) {
		// Default: 1 page
		if (maxResults < 1) {
			List<Status> msgs = Status
			.getStatuses(http.getPage(url, var, true));
			msgs = dateFilter(msgs);
			return msgs;
		}
		// Fetch all pages until we run out
		// -- or Twitter complains in which case you'll get an exception
		pageNumber = 1;
		List<Status> msgs = new ArrayList<Status>();
		while (msgs.size() <= maxResults) {
			List<Status> nextpage = Status.getStatuses(http.getPage(url, var,
					true));
			nextpage = dateFilter(nextpage);
			msgs.addAll(nextpage);
			if (nextpage.size() < 20)
				break;
			pageNumber++;
			var.put("page", Integer.toString(pageNumber));
		}
		return msgs;
	}

	private List<Long> getUserIDs(String url) {
		String json = http.getPage(url, null, true);
		List<Long> ids = new ArrayList<Long>();
		try {
			JSONArray jarr = new JSONArray(json);
			for (int i = 0; i < jarr.length(); i++) {
				ids.add(jarr.getLong(i));
			}
		} catch (JSONException e) {
			throw new TwitterException("Could not parse id list" + e);
		}
		return ids;
	}

	private List<User> getUsers(String url) {
		return User.getUsers(http.getPage(url, null, true));
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * authenticating user.
	 */
	public List<Status> getUserTimeline() throws TwitterException {
		return getStatuses("http://twitter.com/statuses/user_timeline.json",
				standardishParameters());
	}

	/**
	 * Returns the most recent statuses posted in the last 24 hours from the
	 * given user.
	 * <p>
	 * This method will authenticate if it can (i.e. if the Twitter object has a
	 * username and password). Authentication is needed to see the posts of a
	 * private user.
	 * 
	 * @param id
	 *            Can be null. Specifies the ID or screen name of the user for
	 *            whom to return the user_timeline.
	 * @param since
	 *            Can be null. Narrows the returned results to just those
	 *            statuses created after the specified date.
	 */
	public List<Status> getUserTimeline(String id) throws TwitterException {
		Map<String, String> vars = asMap("id", id);
		addStandardishParameters(vars);
		// Should we authenticate?
		boolean authenticate = http.canAuthenticate();
		String json = http.getPage(
				"http://twitter.com/statuses/user_timeline.json", vars,
				authenticate);
		return Status.getStatuses(json);
	}

	/**
	 * Is the authenticating user <i>followed by</i> userB?
	 * 
	 * @param userB
	 *            The screen name of a Twitter user.
	 * @return Whether or not the user is followed by userB.
	 */
	public boolean isFollower(String userB) {
		return isFollower(userB, name);
	}

	/**
	 * @return true if followerScreenName <i>is</i> following followedScreenName
	 * 
	 * @throws TwitterException.E403 if one of the users has protected their
	 * updates and you don't have access. This can be counter-intuitive
	 * (and annoying) at times!
	 */
	public boolean isFollower(String followerScreenName,
			String followedScreenName) {
		assert followerScreenName != null && followedScreenName != null;
		String page = http.getPage(
				"http://twitter.com/friendships/exists.json",
				aMap(
						"user_a", followerScreenName,
						"user_b", followedScreenName), true);
		return Boolean.valueOf(page);
	}

	/**
	 * Does the authenticating user <i>follow</i> userB?
	 * 
	 * @param userB
	 *            The screen name of a Twitter user.
	 * @return Whether or not the user follows userB.
	 */
	public boolean isFollowing(String userB) {
		if (isFollower(name, userB)) return true;
		// hopefully temporary workarounds for hopefully temporary issues with Twitter's follower API
		// Note: These do not appear to help! Left in 'cos what harm can they do?
		List<User> friends = getFriends();
		User b = new User(userB);
		if (friends.contains(b)) return true;
		long bid = show(userB).getId();
		List<Long> fids = getFriendIDs();
		if (fids.contains(bid)) return true;
		return false;
	}

	/**
	 * Convenience for {@link #isFollowing(String)}
	 * @param user
	 */
	public boolean isFollowing(User user) {
		return isFollowing(user.screenName);
	}

	/**
	 * Switches off notifications for updates from the specified user <i>who
	 * must already be a friend</i>.
	 * 
	 * @param username
	 *            Stop getting notifications from this user, who must already be
	 *            one of your friends.
	 * @return the specified user
	 */
	public User leaveNotifications(String username) {
		String page = http.getPage("http://twitter.com/notifications/leave/"
				+ username + ".json", null, true);
		try {
			return new User(new JSONObject(page), null);
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Enables notifications for updates from the specified user <i>who must
	 * already be a friend</i>.
	 * 
	 * @param username
	 *            Get notifications from this user, who must already be one of
	 *            your friends.
	 * @return the specified user
	 */
	public User notify(String username) {
		String page = http.getPage("http://twitter.com/notifications/follow/"
				+ username + ".json", null, true);
		try {
			return new User(new JSONObject(page), null);
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Wrapper for {@link IHttpClient#post(String, Map, boolean)}.
	 */
	private String post(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException {
		String page = http.post(uri, vars, authenticate);
		return page;
	}

	/**
	 * Perform a search of Twitter.
	 * <p>
	 * Warning: the User objects returned by a search (as part of the Status objects)
	 * are dummy-users. The only information that is set is the user's screen-name
	 * and a profile image url. This reflects the current behaviour of the Twitter API.
	 * If you need more info, call {@link #show(String)} with the screen name.
	 * 
	 * TODO support for {@link #maxResults}
	 * 
	 * @param searchTerm
	 * @return search results (upto 100 per page)
	 */
	public List<Status> search(String searchTerm) {
		// number of tweets per page, max 100
		int rpp = 100;
		Map<String, String> vars = aMap("rpp",
				"" + rpp, "q", searchTerm);
		addStandardishParameters(vars);
		String json = http.getPage("http://search.twitter.com/search.json",
				vars, true);
		try {
			JSONObject jo = new JSONObject(json);
			List<Status> stati = Status.getStatusesFromSearch(this, jo);
			return dateFilter(stati);
		} catch (Exception e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Sends a new direct message to the specified user from the authenticating
	 * user.
	 * 
	 * @param recipient
	 *            Required. The ID or screen name of the recipient user.
	 * @param text
	 *            Required. The text of your direct message. Keep it under 140
	 *            characters!
	 * @return the sent message
	 * @throws TwitterException.E403 if the recipient is not following you.
	 * (you can \@mention anyone but you can only dm people who follow you).
	 */
	public Message sendMessage(String recipient, String text)
	throws TwitterException {
		assert recipient != null;
		if (text.length() > 140)
			throw new IllegalArgumentException("Message is too long.");
		Map<String, String> vars = asMap("user", recipient, "text", text);
		String result = post("http://twitter.com/direct_messages/new.json",
				vars, true);
		try {
			return new Message(new JSONObject(result));
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * @param maxResults
	 *            if greater than zero, requests will attempt to fetch as many
	 *            pages as are needed! -1 by default, in which case most methods
	 *            return the first 20 statuses/messages.
	 *            <p>
	 *            If setting a high figure, you should usually also set a
	 *            sinceId or sinceDate to limit your Twitter usage. Otherwise
	 *            you can easily exceed your rate limit.
	 */
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * @param pageNumber
	 *            null (the default) returns the first page. Pages are indexed
	 *            from 1. This is used once only
	 */
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	/**
	 * Date based filter on statuses and messages. This is done client-side as
	 * Twitter have - for their own inscrutable reasons - pulled support for
	 * this feature.
	 * <p>
	 * If using this, you probably also want to increase
	 * {@link #setMaxResults(int)} - otherwise you get at most 20, and possibly
	 * less (since the filtering is done client side).
	 * 
	 * @param sinceDate
	 */
	public void setSinceDate(Date sinceDate) {
		this.sinceDate = sinceDate;
	}

	/**
	 * Narrows the returned results to just those statuses created after the
	 * specified status id. This will be used until it is set to null. Default
	 * is null.
	 * <p>
	 * If using this, you probably also want to increase
	 * {@link #setMaxResults(int)} (otherwise you just get the most recent 20).
	 * 
	 * @param statusId
	 */
	public void setSinceId(Long statusId) {
		sinceId = statusId;
	}

	/**
	 * Set the source application. This will be mentioned on Twitter alongside
	 * status updates (with a small label saying source: myapp).
	 * 
	 * <i>In order for this to work, you must first register your app with
	 * Twitter and get a source name from them! Otherwise the source will appear
	 * as "web".</i>
	 * 
	 * @param sourceApp
	 *            jtwitterlib by default. Set to null for no source.
	 */
	public void setSource(String sourceApp) {
		this.sourceApp = sourceApp;
	}

	/**
	 * Sets the authenticating user's status.
	 * <p>
	 * Identical to {@link #updateStatus(String)}, but with a Java-style name
	 * (updateStatus is the Twitter API name for this method).
	 * 
	 * @param statusText
	 *            The text of your status update. Must not be more than 160
	 *            characters and should not be more than 140 characters to
	 *            ensure optimal display.
	 * @return The posted status when successful.
	 */
	public Status setStatus(String statusText) throws TwitterException {
		return updateStatus(statusText);
	}

	/**
	 * Returns information of a given user, specified by ID or screen name.
	 * 
	 * @param id
	 *            The ID or screen name of a user.
	 * @throws exception if the user does not exist - or has been terminated
	 * (as happens to spam bots).
	 */
	public User show(String id) throws TwitterException {
		String json = http.getPage("http://twitter.com/users/show/" + id
				+ ".json", null, http.canAuthenticate());
		User user;
		try {
			user = new User(new JSONObject(json), null);
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
		return user;
	}

	/**
	 * Split a long message up into shorter chunks suitable for use with
	 * {@link #setStatus(String)} or {@link #sendMessage(String, String)}.
	 * 
	 * @param longStatus
	 * @return longStatus broken into a list of max 140 char strings
	 */
	public List<String> splitMessage(String longStatus) {
		// Is it really long?
		if (longStatus.length() <= 140)
			return Collections.singletonList(longStatus);
		// Multiple tweets for a longer post
		List<String> sections = new ArrayList<String>(4);
		StringBuilder tweet = new StringBuilder(140);
		String[] words = longStatus.split("\\s+");
		for (String w : words) {
			// messages have a max length of 140
			// plus the last bit of a long tweet tends to be hidden on
			// twitter.com, so best to chop 'em short too
			if (tweet.length() + w.length() + 1 > 140) {
				// Emit
				tweet.append("...");
				sections.add(tweet.toString());
				tweet = new StringBuilder(140);
				tweet.append(w);
			} else {
				if (tweet.length() != 0)
					tweet.append(" ");
				tweet.append(w);
			}
		}
		// Final bit
		if (tweet.length() != 0)
			sections.add(tweet.toString());
		return sections;
	}

	private Map<String, String> standardishParameters() {
		return addStandardishParameters(new HashMap<String, String>());
	}

	/**
	 * Destroy: Discontinues friendship with the user specified in the ID
	 * parameter as the authenticating user.
	 * 
	 * @param username
	 *            The ID or screen name of the user with whom to discontinue
	 *            friendship.
	 * @return the un-friended user (if they were a friend), or null if the
	 *         method fails because the specified user was not a friend.
	 */
	public User stopFollowing(String username) {
		assert getName() != null;
		try {
			String page = post("http://twitter.com/friendships/destroy/"
					+ username + ".json", null, true);
			// ?? is this needed to make Twitter update its cache? doesn't seem to fix things
			//			http.getPage("http://twitter.com/friends", null, true);
			User user;
			try {
				user = new User(new JSONObject(page), null);
			} catch (JSONException e) {
				throw new TwitterException(e);
			}
			return user;
		} catch (TwitterException e) {
			// were they a friend anyway?
			if (!isFollower(getName(), username)) {
				return null;
			}
			// Something else went wrong
			throw e;
		}
	}

	/**
	 * Convenience for {@link #stopFollowing(String)}
	 * @param user
	 */
	public void stopFollowing(User user) {
		stopFollowing(user.screenName);
	}

	/**
	 * Updates the authenticating user's status.
	 * 
	 * @param statusText
	 *            The text of your status update. Must not be more than 160
	 *            characters and should not be more than 140 characters to
	 *            ensure optimal display.
	 * @return The posted status when successful.
	 */
	public Status updateStatus(String statusText) throws TwitterException {
		if (statusText.length() > 160)
			throw new IllegalArgumentException(
					"Status text must be 160 characters or less: "
					+ statusText.length());
		Map<String, String> vars = asMap("status", statusText);
		if (sourceApp != null)
			vars.put("source", sourceApp);
		String result = post("http://twitter.com/statuses/update.json", vars,
				true);
		try {
			return new Status(new JSONObject(result), null);
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Does a user with the specified name or id exist?
	 * @param id The screen name or user id of the suspected user.
	 * @return False if the user doesn't exist or has been suspended, true otherwise.
	 */
	public boolean userExists(String id) {
		try {
			String json = http.getPage("http://twitter.com/users/show/" + id
					+ ".json", null, true);
		} catch (TwitterException.E404 e) {
			return false;
		}
		return true;
	}


}
