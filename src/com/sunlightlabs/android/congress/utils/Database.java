package com.sunlightlabs.android.congress.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.Congress;

public class Database {
	private static final int DATABASE_VERSION = 8; // updated last for version 4.1

	public boolean closed = true;

	private static final String DATABASE_NAME = "congress.db";

	private static final String[] LEGISLATOR_COLUMNS = new String[] { "bioguide_id",
			"first_name", "last_name", "nickname", "name_suffix", "title", "party",
			"state", "district", "gender" };
	private static final String[] BILL_COLUMNS = new String[] { "id", "short_title", "official_title" };

	private static final String[] SUBSCRIPTION_COLUMNS = new String[] { "id", "name", "data", "notification_class", "unseen_count" };
	
	private static final String[] SEEN_COLUMNS = new String[] { "subscription_id", "subscription_class", "seen_id" };

	private DatabaseHelper helper;
	private SQLiteDatabase database;
	private Context context;
	
	// standard date format across the API
	private static SimpleDateFormat format = new SimpleDateFormat(Congress.dateFormat);

	public Database(Context context) {
		this.context = context;
	}
	
	public Database open() {
		helper = new DatabaseHelper(context);
		closed = false;
		database = helper.getWritableDatabase();
		return this;
	}

	public boolean isOpen() {
		return database.isOpen();
	}

	public void close() {
		closed = true;
		helper.close();
	}
	
	/** Legislators */

	private ContentValues fromLegislator(Legislator legislator, int size) {
		try {
			Class<?> cls = Class.forName("com.sunlightlabs.congress.models.Legislator");
			ContentValues cv = new ContentValues(size);
			for (int i = 0; i < LEGISLATOR_COLUMNS.length; i++) {
				String column = LEGISLATOR_COLUMNS[i];
				cv.put(column, (String) cls.getDeclaredField(column).get(legislator));
			}
			return cv;
		} catch (Exception e) {
			return null;
		}
	}

	private ContentValues fromLegislator(Legislator legislator) {
		return fromLegislator(legislator, LEGISLATOR_COLUMNS.length);
	}

	public long addLegislator(Legislator legislator) {
		ContentValues cv = fromLegislator(legislator);
		if (cv != null)
			return database.insert("legislators", null, cv);
		return -1;
	}

	public int removeLegislator(String id) {
		return database.delete("legislators", "bioguide_id=?", new String[] { id });
	}

	public Cursor getLegislator(String id) {
		Cursor cursor = database.query("legislators", LEGISLATOR_COLUMNS, "bioguide_id=?",
				new String[] { id }, null, null, null);

		cursor.moveToFirst();
		return cursor;
	}

	public Cursor getLegislators() {
		return database.rawQuery("SELECT * FROM legislators", null);
	}
	
	public static Legislator loadLegislator(Cursor c) {
		Legislator legislator = new Legislator();

		legislator.bioguide_id = c.getString(c.getColumnIndex("bioguide_id"));
		legislator.first_name = c.getString(c.getColumnIndex("first_name"));
		legislator.last_name = c.getString(c.getColumnIndex("last_name"));
		legislator.nickname = c.getString(c.getColumnIndex("nickname"));
		legislator.name_suffix = c.getString(c.getColumnIndex("name_suffix"));
		legislator.title = c.getString(c.getColumnIndex("title"));
		legislator.party = c.getString(c.getColumnIndex("party"));
		legislator.state = c.getString(c.getColumnIndex("state"));
		legislator.district = c.getString(c.getColumnIndex("district"));
		legislator.gender = c.getString(c.getColumnIndex("gender"));
		
		return legislator;
	}

	public static String formatDate(Date date) {
		return date == null ? null : format.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		return date == null ? null : format.parse(date);
	}
	
	/** Bills */

	// error condition is -1
	public long addBill(Bill bill) {
		try {
			Class<?> cls = Class.forName("com.sunlightlabs.congress.models.Bill");
			ContentValues cv = new ContentValues(BILL_COLUMNS.length);
			for (int i = 0; i < BILL_COLUMNS.length; i++) {
				String column = BILL_COLUMNS[i];
				cv.put(column, (String) cls.getDeclaredField(column).get(bill));
			}
			return database.insert("bills", null, cv);
		} catch (Exception e) {
			return -1;
		}
	}

	// error condition is 0
	public int removeBill(String id) {
		try { 
			return database.delete("bills", "id=?", new String[] { id });
		} catch (SQLiteException e) {
			Log.w(Utils.TAG, "Exception while unstarring bill: " + e.getMessage());
			return 0;
		}
	}
	
	public Cursor getBill(String id) {
		Cursor cursor = database.query("bills", BILL_COLUMNS, "id=?", new String[] { id }, null, null, null);
		cursor.moveToFirst();
		return cursor;
	}

	public Cursor getBills() {
		return database.rawQuery("SELECT * FROM bills", null);
	}

	public static Bill loadBill(Cursor c) {
		Bill bill = new Bill();

		bill.id = c.getString(c.getColumnIndex("id"));
		bill.short_title = c.getString(c.getColumnIndex("short_title"));
		bill.official_title = c.getString(c.getColumnIndex("official_title"));
		
		return bill;
	}

	
	/** Subscriptions */

	public Cursor getSubscriptions() {
		return database.rawQuery("SELECT * FROM subscriptions", null);
	}

	public Cursor getSubscription(String id, String notificationClass) {
		return database.query("subscriptions", SUBSCRIPTION_COLUMNS, "id=? AND notification_class=?",
				new String[] { id, notificationClass }, null, null, null);
	}
	
	public boolean hasSubscription(String id, String notificationClass) {
		Cursor c = getSubscription(id, notificationClass);
		boolean hasSubscription = c.moveToFirst();
		c.close();
		
		return hasSubscription;
	}
	
	public boolean hasSubscriptionItem(String subscriptionId, String subscriptionClass, String itemId) {
		Cursor c = database.query("seen_items", SEEN_COLUMNS, "subscription_id=? AND subscription_class=? AND seen_id=?",
				new String[] { subscriptionId, subscriptionClass, itemId }, null, null, null);
		boolean hasItem = c.moveToFirst();
		c.close();
		
		return hasItem;
	}

	public long addSubscription(Subscription subscription) {
		ContentValues cv = new ContentValues(SUBSCRIPTION_COLUMNS.length);
		cv.put("id", subscription.id);
		cv.put("name", subscription.name);
		cv.put("notification_class", subscription.notificationClass);
		cv.put("data", subscription.data);
		return database.insert("subscriptions", null, cv);
	}
	
	public long addSeenIds(Subscription subscription, List<String> latestIds) {
		int rows = 0;
		boolean failed = false;
		
		int size = latestIds.size();
		for (int i=0; i<size; i++) {
			ContentValues cv = new ContentValues(SUBSCRIPTION_COLUMNS.length);
			cv.put("subscription_id", subscription.id);
			cv.put("subscription_class", subscription.notificationClass);
			cv.put("seen_id", latestIds.get(i));
			if (database.insert("seen_items", null, cv) >= 0)
				rows += 1;
			else
				failed = true;
		}
		
		return (failed ? -1 : rows);
	}
	
	public long removeSubscription(String id, String notificationClass) {
		long first = database.delete("subscriptions", "id=? AND notification_class=?", 
				new String[] { id , notificationClass });
		long second = database.delete("seen_items", "subscription_id=? AND subscription_class=?", 
				new String[] { id , notificationClass });
		return first + second;
	}
	
	public static Subscription loadSubscription(Cursor c) {
		String id = c.getString(c.getColumnIndex("id"));
		String name = c.getString(c.getColumnIndex("name"));
		String data = c.getString(c.getColumnIndex("data"));
		String notificationClass = c.getString(c.getColumnIndex("notification_class"));
		
		return new Subscription(id, name, notificationClass, data);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		private void createTable(SQLiteDatabase db, String table, String[] columns) {
			StringBuilder sql = new StringBuilder("CREATE TABLE " + table);
			sql.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT");

			for (int i = 0; i < columns.length; i++)
				sql.append(", " + columns[i] + " TEXT");

			sql.append(");");
			db.execSQL(sql.toString());
		}
		
		private void addColumn(SQLiteDatabase db, String table, String newColumn) {
			db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + newColumn + " TEXT;");
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			createTable(db, "bills", BILL_COLUMNS);
			createTable(db, "legislators", LEGISLATOR_COLUMNS);
			createTable(db, "subscriptions", SUBSCRIPTION_COLUMNS);
			createTable(db, "seen_items", SEEN_COLUMNS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(Utils.TAG, "Upgrading " + DATABASE_NAME + " from version " + oldVersion + " to " + newVersion);

			// Version 2 - Favorites (bills and legislators table), 
			//   first release, in version 2.6
			
			// Version 3 - Notifications (subscriptions table)
			// 	 released in version 2.9
			if (oldVersion < 3) {
				// add the table as it was then, not as it may be now, so that future migrations run correctly
				createTable(db, "subscriptions", new String[] {"id", "name", "data", "last_seen_id", "notification_class" });
			}
			
			// Version 4 - Remove a bunch of timeline columns, update subscription structure
			//   released in version 2.9.8
			if (oldVersion < 4) {
				// no SQL commands needed for timeline, columns are left abandoned
				
				// abandon lastSeenId column
				try {
					addColumn(db, "subscriptions", "seen_id");
				} catch(SQLiteException e) {
					// need this to catch a bug I created by having my old 2->3 migration not use the original column names,
					// which caused there to be a dupe seen_id column for users upgrading directly from 2->4. 
					// I fixed the 2->3 createTable line, but for those stuck in that state, I need to swallow their dupe
					// column exception and let them move on with their lives.
					
					// swallow!
				}
			}
			
			// Version 5 - Fix bug in subscription rows
			//   released in version 3.0
			if (oldVersion < 5) {
				// Problem: notification checker was accidentally adding duplicate rows with null seen_id's on each run, 
				// meaning that when loading a list of all subscriptions, there would be many many duplicate rows.
				// This migration finds all distinct subscriptions, removes all rows where the seen_id is null, and then
				// recreates one row with that information, essentially cleaning out duplicates.
				
				// get all unique subscriptions (with columns as they existed at this state)
				Cursor cursor = db.rawQuery("SELECT DISTINCT id, name, data, notification_class FROM subscriptions", null);
				if (cursor.getCount() > 0 && cursor.moveToFirst()) {
					Log.i(Utils.TAG, "Beginning migration, " + cursor.getCount() + " subscriptions to de-dupe");
					int i = 0;
					do {
						// load subscription data
						String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
						String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
						String data = cursor.getString(cursor.getColumnIndexOrThrow("data"));
						String notificationClass = cursor.getString(cursor.getColumnIndexOrThrow("notification_class"));
						
						// delete all rows for this subscription where seen_id is null
						long rows = db.delete("subscriptions", "id=? AND notification_class=? AND seen_id IS NULL", 
								new String[] { id , notificationClass });
						Log.i(Utils.TAG, "Removed " + rows + " rows for subscription with {id: " + id + ", notificationClass: " + notificationClass + ", name: " + name + ", data: " + data + "}"); 
						
						// insert placeholder item with null seen_id, so that a subscription is registered even for empty lists
						ContentValues cv = new ContentValues(SUBSCRIPTION_COLUMNS.length);
						cv.put("id", id);
						cv.put("name", name);
						cv.put("notification_class", notificationClass);
						cv.put("data", data);
						long results = db.insert("subscriptions", null, cv);
						Log.i(Utils.TAG, "Inserted row with ID " + results + " in their place");
						
						i += 1;
					} while (cursor.moveToNext());
					
					cursor.close();
					
					Log.i(Utils.TAG, "Migration to level 5 complete, de-duped " + i + " subscriptions");
				}
				
			}
			
			// Version 6 - Remove nominations subscriber, split subscriptions tables in two 
			//   released in version 3.3
			if (oldVersion < 6) {
				// remove nominations subscriber 
				Log.i(Utils.TAG, "Expunging RollsNominationSubscriber rows from database...");
				long rows = db.delete("subscriptions", "notification_class=?", new String[] {"RollsNominationsSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " RollsNominationsSubscriber entries from database");
				
				// Restructure subscriptions tables to split them out into two.
				// This is much cleaner, and sets the foundation for proper accumulated unseen counts.
				
				// remove subscriptions->seen_id (no SQL necessary, column abandoned)
				Log.i(Utils.TAG, "Creating seen_items table...");
				createTable(db, "seen_items", new String[] { "subscription_id", "subscription_class", "seen_id" });
				
				// move existing seen items into the new table
				Cursor cursor = db.rawQuery("SELECT id, notification_class, seen_id FROM subscriptions WHERE seen_id IS NOT NULL", null);
				if (cursor.getCount() > 0 && cursor.moveToFirst()) {
					Log.i(Utils.TAG, "Beginning migration of seen items, " + cursor.getCount() + " seen items to transfer");
					int i = 0;
					do {
						String subscriptionId = cursor.getString(cursor.getColumnIndexOrThrow("id"));
						String subscriptionClass = cursor.getString(cursor.getColumnIndexOrThrow("notification_class"));
						String seenId = cursor.getString(cursor.getColumnIndexOrThrow("seen_id"));
						
						ContentValues cv = new ContentValues(3);
						cv.put("subscription_id", subscriptionId);
						cv.put("subscription_class", subscriptionClass);
						cv.put("seen_id", seenId);
						
						long results = db.insert("seen_items", null, cv);
						Log.i(Utils.TAG, "Transferred seen_item into seen_items with ID " + results);
						
						i += 1;
					} while (cursor.moveToNext());
					
					Log.i(Utils.TAG, "Finished transfer, counted " + i + " transferrals");
				}
				
				// delete all those seen items from the original table
				Log.i(Utils.TAG, "Clearing out subscriptions with a seen_id...");
				long seenRows = db.delete("subscriptions", "seen_id IS NOT NULL", null);
				Log.i(Utils.TAG, "Removed " + seenRows + " subscription rows with a seen_id");
				
				
				// add accumulated unseen_items field on subscriptions
				Log.i(Utils.TAG, "Adding unseen_count to subscriptions table...");
				addColumn(db, "subscriptions", "unseen_count");
				
				Log.i(Utils.TAG, "Migration to level 6 complete");
			}
			
			// Version 7 - 
			//   * Remove RollsSearchSubscriber subscriptions
			//   * Remove TwitterSubscriber subscriptions
			//   * Abandon unnecessary fields from starred legislators
			//   * Rename bill IDs from hcres/scres -> hconres/sconres in:
			//     - bills table
			//     - subscriptions table
			//     - seen_items table
			// released in version 4.0

			if (oldVersion < 7) {
				// Not actually removing columns, but am documenting which ones remain on the table,
				// but are not supported.
				
				// Abandoning fields on `legislators`:
				//	"id", "govtrack_id", "congress_office", "website", "phone", "twitter_id", "youtube_url"
				
				// Abandoning fields on `bills`: "code"
				
				Log.i(Utils.TAG, "Renaming bill IDs from hcres/scres to hconres/sconres...");
				
				long rows = 0;
				Cursor cursor;
				try {
					Log.i(Utils.TAG, "- In starred bills table (bills)...");
					cursor = db.rawQuery("SELECT * FROM bills WHERE id LIKE \"%cres%\"", null);
					if (cursor.moveToFirst()) {
						do {
							String billId = cursor.getString(cursor.getColumnIndexOrThrow("id"));
							if (billId.contains("cres")) {
								String newId = billId.replace("cres", "conres");
								
								Log.i(Utils.TAG, "    [" + billId + "] -> [" + newId + "]");
								db.execSQL("UPDATE bills SET id=? WHERE id=?", new String[] {newId, billId});
								rows += 1;
							}
						} while (cursor.moveToNext());
					}
					Log.i(Utils.TAG, "Updated " + rows + " bills in bills table.");
					
					
					String[] subscriptions = new String[] {"ActionsBillSubscriber", "VotesBillSubscriber", "NewsBillSubscriber"};
					for (int i=0; i<subscriptions.length; i++) {
						String subscription = subscriptions[i];
						rows = 0;
						
						Log.i(Utils.TAG, "- In subscriptions table (" + subscription + ")...");
						cursor = db.rawQuery("SELECT * FROM subscriptions WHERE notification_class = ? AND id LIKE \"%cres%\"", new String[] { subscription });
						if (cursor.moveToFirst()) {
							do {
								String billId = cursor.getString(cursor.getColumnIndexOrThrow("id"));
								if (billId.contains("cres")) {
									
									String newId = billId.replace("cres", "conres");
									String newData = newId;
									
									// bill news subscriptions use the formatted code as the data
									if (subscription.equals("NewsBillSubscriber")) {
										String billCode = cursor.getString(cursor.getColumnIndexOrThrow("data"));
										newData = billCode.replace("C. Res.", "Con. Res.");
									}
									
									Log.i(Utils.TAG, "    [" + billId + "] -> [" + newId + "]");
									db.execSQL("UPDATE subscriptions SET id=?, data=? WHERE id=? AND notification_class=?", new String[] {newId, newData, billId, subscription});
									rows += 1;
								}
							} while (cursor.moveToNext());
						}
						
						Log.i(Utils.TAG, "Updated " + rows + " subscriptions rows with new ids (" + subscription + ")");
					}
					
					String[] seenTypes = new String[] { "BillsLawsSubscriber", "BillsLegislatorSubscriber", "BillsRecentSubscriber", "BillsSearchSubscriber" };
					for (int i=0; i<seenTypes.length; i++) {
						String seenType = seenTypes[i];
						rows = 0;
						
						Log.i(Utils.TAG, "- In seen_items table (" + seenType + ")...");
						cursor = db.rawQuery("SELECT * FROM seen_items WHERE subscription_class = ? AND seen_id LIKE \"%cres%\"", new String[] { seenType});
						if (cursor.moveToFirst()) {
							do {
								String billId = cursor.getString(cursor.getColumnIndexOrThrow("seen_id"));
								if (billId.contains("cres")) {
									String newId = billId.replace("cres", "conres");
									
									Log.i(Utils.TAG, "    [" + billId + "] -> [" + newId + "]");
									db.execSQL("UPDATE seen_items SET seen_id=? WHERE seen_id=? and subscription_class=?", new String[] {newId, billId, seenType});
									rows += 1;
								}
							} while (cursor.moveToNext());
						}
						
						Log.i(Utils.TAG, "Updated " + rows + " seen_items rows with new ids (" + seenType + ")");
					}
					
					cursor.close();
					
				} catch (SQLiteException e) {
					Log.e(Utils.TAG, "Error while renaming bill IDs:", e);
				}
				
				Log.i(Utils.TAG, "Removing RollsSearchSubscriber subscriptions and seen items...");
				rows = db.delete("subscriptions", "notification_class=?", new String[] {"RollsSearchSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " RollsSearchSubscriber entries from subscriptions");
				rows = db.delete("seen_items", "subscription_class=?", new String[] {"RollsSearchSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " RollsSearchSubscriber entries from seen_items");
				
				Log.i(Utils.TAG, "Removing TwitterSubscriber subscriptions and seen items...");
				rows = db.delete("subscriptions", "notification_class=?", new String[] {"TwitterSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " TwitterSubscriber entries from subscriptions");
				rows = db.delete("seen_items", "subscription_class=?", new String[] {"TwitterSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " TwitterSubscriber entries from seen_items");
			}
			
			// Version 8 - 
			//   * Remove YouTubeSubscriber subscriptions (we'll now link to YouTube profiles)
			//	 * Remove BillsLaws subscriptions (was not heavily used)
			// released in version 4.1
			
			Log.i(Utils.TAG, "oldVersion: " + oldVersion);
			if (oldVersion < 8) {
				long rows = 0;
				
				Log.i(Utils.TAG, "Removing YoutubeSubscriber subscriptions and seen items...");
				rows = db.delete("subscriptions", "notification_class=?", new String[] {"YoutubeSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " YoutubeSubscriber entries from subscriptions");
				rows = db.delete("seen_items", "subscription_class=?", new String[] {"YoutubeSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " YoutubeSubscriber entries from seen_items");
				
				Log.i(Utils.TAG, "Removing BillsLawsSubscriber subscriptions and seen items...");
				rows = db.delete("subscriptions", "notification_class=?", new String[] {"BillsLawsSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " BillsLawsSubscriber entries from subscriptions");
				rows = db.delete("seen_items", "subscription_class=?", new String[] {"BillsLawsSubscriber"});
				Log.i(Utils.TAG, "Removed " + rows + " BillsLawsSubscriber entries from seen_items");
			}
		}
	}
}