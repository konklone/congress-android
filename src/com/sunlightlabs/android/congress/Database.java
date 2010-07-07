package com.sunlightlabs.android.congress;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.Drumbone;

public class Database {
	private static final String TAG = "CongressDatabase";

	private static final String DATABASE_NAME = "congress.db";
	private static final int DATABASE_VERSION = 2;

	private static final String LEGISLATORS_TABLE = "legislators";
	private static final String BILLS_TABLE = "bills";

	private static final String[] LEGISLATOR_COLUMNS = new String[] { "id", "bioguide_id",
			"govtrack_id", "first_name", "last_name", "nickname", "name_suffix", "title", "party",
			"state", "district", "gender", "congress_office", "website", "phone", "twitter_id",
			"youtube_url" };
	private static final String[] BILL_COLUMNS = new String[] { "id", "type", "number", "session",
			"code", "short_title", "official_title", "house_result", "senate_result", "passed",
			"vetoed", "override_house_result", "override_senate_result", "awaiting_signature",
			"enacted", "last_vote_at", "last_action_at", "introduced_at", "house_result_at",
			"senate_result_at", "passed_at", "vetoed_at", "override_house_result_at",
			"override_senate_result_at", "awaiting_signature_since", "enacted_at", "sponsor_id",
			"sponsor_party", "sponsor_state", "sponsor_title", "sponsor_first_name",
			"sponsor_nickname", "sponsor_last_name" };


	private DatabaseHelper helper;
	private SQLiteDatabase database;
	private Context context;
	// uses Drumbone date format
	private static SimpleDateFormat df = new SimpleDateFormat(Drumbone.dateFormat[0]);

	public Database(Context context) {
		this.context = context;
	}

	public long addLegislator(Legislator legislator) {
		try {
			Class<?> cls = Class.forName("com.sunlightlabs.congress.models.Legislator");
			ContentValues cv = new ContentValues(LEGISLATOR_COLUMNS.length);
			for (int i = 0; i < LEGISLATOR_COLUMNS.length; i++) {
				String column = LEGISLATOR_COLUMNS[i];
				cv.put(column, (String) cls.getDeclaredField(column).get(legislator));
			}
			return database.insert(LEGISLATORS_TABLE, null, cv);
		} catch (ClassNotFoundException e) {
			return -1;
		} catch (IllegalAccessException e) {
			return -1;
		} catch (NoSuchFieldException e) {
			return -1;
		}
	}

	public int removeLegislator(String id) {
		return database.delete(LEGISLATORS_TABLE, "id=?", new String[] { id });
	}

	public Cursor getLegislator(String id) {
		Cursor cursor = database.query(LEGISLATORS_TABLE, LEGISLATOR_COLUMNS, "id=?",
				new String[] { id }, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		return cursor;
	}

	public Cursor getLegislators() {
		return database.rawQuery("SELECT * FROM " + LEGISLATORS_TABLE, null);
	}

	public static String formatDate(Date date) {
		return date == null ? null : df.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		return date == null ? null : df.parse(date);
	}

	public long addBill(Bill bill) {
		try {
			Class<?> cls = Class.forName("com.sunlightlabs.congress.models.Bill");
			ContentValues cv = new ContentValues(BILL_COLUMNS.length);
			for (int i = 0; i < BILL_COLUMNS.length - 7; i++) {
				String column = BILL_COLUMNS[i];
				Object field = cls.getDeclaredField(column).get(bill);
				if (field instanceof Date)
					cv.put(column, formatDate((Date) field));
				else
					cv.put(column, field == null ? null : field.toString());
			}
			Legislator sponsor = bill.sponsor;
			cv.put("sponsor_id", sponsor.getId());
			cv.put("sponsor_party", sponsor.party);
			cv.put("sponsor_state", sponsor.state);
			cv.put("sponsor_title", sponsor.title);
			cv.put("sponsor_first_name", sponsor.firstName());
			cv.put("sponsor_nickname", sponsor.nickname);
			cv.put("sponsor_last_name", sponsor.last_name);
			return database.insert(BILLS_TABLE, null, cv);
		} catch (Exception e) {
			return -1;
		}
	}

	public int removeBill(String id) {
		return database.delete(BILLS_TABLE, "id=?", new String[] { id });
	}

	public Cursor getBill(String id) {
		Cursor cursor = database.query(BILLS_TABLE, BILL_COLUMNS, "id=?", new String[] { id },
				null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		return cursor;
	}

	public Cursor getBills() {
		return database.rawQuery("SELECT * FROM " + BILLS_TABLE, null);
	}

	public Database open() {
		helper = new DatabaseHelper(context);
		database = helper.getWritableDatabase();
		return this;
	}

	public void close() {
		helper.close();
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			// create legislators table
			StringBuilder sql = new StringBuilder("CREATE TABLE " + LEGISLATORS_TABLE);
			sql.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT");

			for (int i = 0; i < LEGISLATOR_COLUMNS.length; i++)
				sql.append(", " + LEGISLATOR_COLUMNS[i] + " TEXT");
			
			sql.append(");");
			db.execSQL(sql.toString());

			// create bills table
			sql.setLength(0); // clear
			sql.append("CREATE TABLE " + BILLS_TABLE).append(
					" (_id INTEGER PRIMARY KEY AUTOINCREMENT");
			for (int i = 0; i < BILL_COLUMNS.length; i++)
				sql.append(", " + BILL_COLUMNS[i] + " TEXT");
			
			sql.append(");");
			db.execSQL(sql.toString());
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading " + DATABASE_NAME + " from version " + oldVersion + " to "
					+ newVersion + ", wiping old data");
			db.execSQL("DROP TABLE IF EXISTS " + LEGISLATORS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + BILLS_TABLE);
			onCreate(db);
		}
	}
}
