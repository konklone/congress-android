package com.sunlightlabs.android.congress;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sunlightlabs.congress.models.Legislator;

public class Database {
	private static final String TAG = "CongressDatabase";

	private static final String DATABASE_NAME = "congress.db";
	private static final int DATABASE_VERSION = 1;

	private static final String LEGISLATORS_TABLE = "legislators";
	private static final String BILLS_TABLE = "bills";

	private static final String[] LEGISLATOR_COLUMNS = new String[] { "id", "bioguide_id",
			"govtrack_id", "first_name", "last_name", "nickname", "name_suffix", "title", "party",
			"state", "district", "gender", "congress_office", "website", "phone", "twitter_id",
			"youtube_url" };
	private static final String[] BILL_COLUMNS = new String[] {};


	private DatabaseHelper helper;
	private SQLiteDatabase database;
	private Context context;

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
		} catch (Exception e) {
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

			for (int i = 0; i < LEGISLATOR_COLUMNS.length; i++) {
				sql.append(", " + LEGISLATOR_COLUMNS[i] + " TEXT");
			}
			sql.append(");");
			db.execSQL(sql.toString());
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading " + DATABASE_NAME + " from version " + oldVersion + " to "
					+ newVersion + ", wiping old data");
			db.execSQL("DROP TABLE IF EXISTS " + LEGISLATORS_TABLE);
			onCreate(db);
		}
	}
}
