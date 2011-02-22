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
import com.sunlightlabs.congress.services.RealTimeCongress;

public class Database {
	private static final int DATABASE_VERSION = 5; // updated last for version 3.0

	public boolean closed = true;

	private static final String DATABASE_NAME = "congress.db";

	private static final String[] LEGISLATOR_COLUMNS = new String[] { "id", "bioguide_id",
			"govtrack_id", "first_name", "last_name", "nickname", "name_suffix", "title", "party",
			"state", "district", "gender", "congress_office", "website", "phone", "twitter_id",
			"youtube_url" };
	private static final String[] BILL_COLUMNS = new String[] { "id", "code", "short_title", "official_title" };

	private static final String[] SUBSCRIPTION_COLUMNS = new String[] {"id", "name", "data", "seen_id", "notification_class" };

	private DatabaseHelper helper;
	private SQLiteDatabase database;
	private Context context;
	
	// uses RTC date format
	private static SimpleDateFormat format = new SimpleDateFormat(RealTimeCongress.dateFormat);

	public Database(Context context) {
		this.context = context;
	}

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
		return database.delete("legislators", "id=?", new String[] { id });
	}

	public Cursor getLegislator(String id) {
		Cursor cursor = database.query("legislators", LEGISLATOR_COLUMNS, "id=?",
				new String[] { id }, null, null, null);

		cursor.moveToFirst();
		return cursor;
	}

	public Cursor getLegislators() {
		return database.rawQuery("SELECT * FROM legislators", null);
	}

	public static String formatDate(Date date) {
		return date == null ? null : format.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		return date == null ? null : format.parse(date);
	}

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

	public static Bill loadBill(Cursor c) {
		Bill bill = new Bill();

		bill.id = c.getString(c.getColumnIndex("id"));
		bill.code = c.getString(c.getColumnIndex("code"));
		bill.short_title = c.getString(c.getColumnIndex("short_title"));
		bill.official_title = c.getString(c.getColumnIndex("official_title"));
		
		return bill;
	}

	public static Legislator loadLegislator(Cursor c) {
		Legislator legislator = new Legislator();

		legislator.id = c.getString(c.getColumnIndex("id"));
		legislator.bioguide_id = c.getString(c.getColumnIndex("bioguide_id"));
		legislator.govtrack_id = c.getString(c.getColumnIndex("govtrack_id"));
		legislator.first_name = c.getString(c.getColumnIndex("first_name"));
		legislator.last_name = c.getString(c.getColumnIndex("last_name"));
		legislator.nickname = c.getString(c.getColumnIndex("nickname"));
		legislator.name_suffix = c.getString(c.getColumnIndex("name_suffix"));
		legislator.title = c.getString(c.getColumnIndex("title"));
		legislator.party = c.getString(c.getColumnIndex("party"));
		legislator.state = c.getString(c.getColumnIndex("state"));
		legislator.district = c.getString(c.getColumnIndex("district"));
		legislator.gender = c.getString(c.getColumnIndex("gender"));
		legislator.congress_office = c.getString(c.getColumnIndex("congress_office"));
		legislator.website = c.getString(c.getColumnIndex("website"));
		legislator.phone = c.getString(c.getColumnIndex("phone"));
		legislator.twitter_id = c.getString(c.getColumnIndex("twitter_id"));
		legislator.youtube_url = c.getString(c.getColumnIndex("youtube_url"));
		
		return legislator;
	}

	public Cursor getSubscriptions() {
		return database.rawQuery("SELECT DISTINCT id, name, data, notification_class FROM subscriptions", null);
	}
	
	public Cursor allSubscriptions() {
		return database.rawQuery("SELECT * from subscriptions WHERE seen_id IS NULL", null);
	}

	public Cursor getSubscription(String id, String notificationClass) {
		StringBuilder query = new StringBuilder("id=? AND notification_class=?");

		return database.query("subscriptions", SUBSCRIPTION_COLUMNS, query.toString(),
				new String[] { id, notificationClass }, null, null, null);
	}
	
	public boolean hasSubscription(String id, String notificationClass) {
		Cursor c = getSubscription(id, notificationClass);
		boolean hasSubscription = c.moveToFirst();
		c.close();
		
		return hasSubscription;
	}
	
	public boolean hasSubscriptionItem(String id, String notificationClass, String itemId) {
		StringBuilder query = new StringBuilder("id=? AND notification_class=? AND seen_id=?");

		Cursor c = database.query("subscriptions", SUBSCRIPTION_COLUMNS, query.toString(),
				new String[] { id, notificationClass, itemId }, null, null, null);
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
		
		// insert placeholder item with null seen_id, so that a subscription is registered even for empty lists
		return database.insert("subscriptions", null, cv);
	}
	
	public long addSeenIds(Subscription subscription, List<String> latestIds) {
		ContentValues cv = new ContentValues(SUBSCRIPTION_COLUMNS.length);
		cv.put("id", subscription.id);
		cv.put("name", subscription.name);
		cv.put("notification_class", subscription.notificationClass);
		cv.put("data", subscription.data);
		
		int rows = 0;
		boolean failed = false;
		
		int size = latestIds.size();
		for (int i=0; i<size; i++) {
			cv.put("seen_id", latestIds.get(i));
			if (database.insert("subscriptions", null, cv) >= 0)
				rows += 1;
			else
				failed = true;
		}
		
		return (failed ? -1 : rows);
	}
	
	public long removeSubscription(String id, String notificationClass) {
		return database.delete("subscriptions", "id=? AND notification_class=?", 
				new String[] { id , notificationClass });
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
		
//		private void renameColumn(SQLiteDatabase db, String table, String oldColumn, String newColumn) {
//			addColumn(db, table, newColumn);
//			db.execSQL("UPDATE " + table + " SET " + newColumn + "=" + oldColumn + ";");
//			// abandon old column, no way to remove columns in SQLite
//		}
		
		private void addColumn(SQLiteDatabase db, String table, String newColumn) {
			db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + newColumn + " TEXT;");
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createTable(db, "bills", BILL_COLUMNS);
			createTable(db, "legislators", LEGISLATOR_COLUMNS);
			createTable(db, "subscriptions", SUBSCRIPTION_COLUMNS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(Utils.TAG, "Upgrading " + DATABASE_NAME + " from version " + oldVersion + " to " + newVersion);

			// Version 2 - Favorites (bills and legislators table), 
			//   first release, in version 2.6
			
			// Version 3 - Notifications (subscriptions table)
			// 	 released in version 2.9
			if (oldVersion < 3)
				createTable(db, "subscriptions", new String[] {"id", "name", "data", "last_seen_id", "notification_class" });
			
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
			
		}
	}
}