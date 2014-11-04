package beproject.cbir.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TagsDatabase {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_SNO = "sno_no";
	public static final String KEY_PATH = "file_path";
	public static final String KEY_TAG = "tag";

	public static final String DATABASE_NAME = "Tagsdb";
	public static final String DATABASE_TABLE = "TAGsTable";
	public static final int DATABASE_VERSION = 1;

	public DbHelper ourHelper;
	public final Context ourContext;
	public SQLiteDatabase ourDatabase;
	public int i;
	public static String[] column = new String[] { KEY_ROWID, KEY_SNO,
			KEY_PATH, KEY_TAG };

	public static class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ROWID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_SNO
					+ " TEXT NOT NULL, " + KEY_PATH + " TEXT NOT NULL, "
					+ KEY_TAG + " TEXT NOT NULL);");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}

	}

	public TagsDatabase(Context c) {
		ourContext = c;
	}

	public TagsDatabase open() throws SQLException {
		ourHelper = new DbHelper(ourContext);
		ourDatabase = ourHelper.getWritableDatabase();

		return this;
	}

	public void close() {
		ourHelper.close();
	}

	public boolean CheckIsDataAlreadyInDBorNot(String fieldValue) {

		String Query = "Select * from " + DATABASE_TABLE + " where " + KEY_PATH
				+ "='" + fieldValue + "'";
		Cursor cursor = ourDatabase.rawQuery(Query, null);
		if (cursor.getCount() <= 0) {
			return false;
		}
		return true;
	}

	public ArrayList<String> GetTags(String fieldValue) {

		String Query = "Select * from " + DATABASE_TABLE + " where " + KEY_PATH
				+ "='" + fieldValue + "'";
		Cursor cursor = ourDatabase.rawQuery(Query, null);
		if (cursor.getCount() <= 0) {
			return null;
		}
		String[] tags;
		cursor.moveToFirst();
		String tag = cursor.getString(cursor.getColumnIndex(KEY_TAG));

		tags = tag.split(" , ");

		ArrayList<String> Tags = new ArrayList<String>();

		for (String i : tags) {
			Tags.add(i);
		}

		return Tags;
	}

	public ArrayList<String> CheckIfTagExists(String tag) {

		String Query = "Select * from " + DATABASE_TABLE + " where " + KEY_TAG
				+ " like '% " + tag + " %'";
		Cursor cursor = ourDatabase.rawQuery(Query, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			ArrayList<String> paths = new ArrayList<String>();
			for (int i = 0; i < cursor.getCount(); i++, cursor.moveToNext()) {
				paths.add(cursor.getString(cursor.getColumnIndex(KEY_PATH)));
			}
			return paths;
		}
		return null;

	}

	public long CreateOrUpdateEntry(String path, String tags) {

		if (CheckIsDataAlreadyInDBorNot(path)) {
			String Query = "Select * from " + DATABASE_TABLE + " where "
					+ KEY_PATH + "='" + path + "'";
			Cursor cursor = ourDatabase.rawQuery(Query, null);
			cursor.moveToFirst();
			updateEntry(cursor.getInt(cursor.getColumnIndex(KEY_ROWID)), path,
					tags);
			return 1;

		}
		String[] columns = new String[] { KEY_ROWID, KEY_SNO, KEY_PATH, KEY_TAG };
		Cursor c = ourDatabase.query(DATABASE_TABLE, columns, null, null, null,
				null, null);
		c.moveToLast();
		long l;
		String s;
		if (c.isBeforeFirst()) {
			l = 0;
		} else {
			s = c.getString(c.getColumnIndex(KEY_SNO));
			l = Long.parseLong(s);
		}
		c.close();
		ContentValues cv = new ContentValues();

		cv.put(KEY_PATH, path);
		cv.put(KEY_TAG, tags);
		cv.put(KEY_SNO, l + 1);
		return ourDatabase.insert(DATABASE_TABLE, null, cv);

	}

	public void updateEntry(long lRow, String mPath, String mTags)
			throws SQLException {
		ContentValues cvUpdated = new ContentValues();
		cvUpdated.put(KEY_PATH, mPath);
		cvUpdated.put(KEY_TAG, mTags);
		ourDatabase.update(DATABASE_TABLE, cvUpdated, KEY_ROWID + "=" + lRow,
				null);
	}

	public boolean deleteEntry(long lRow1) throws SQLException {
		ourDatabase.delete(DATABASE_TABLE, KEY_ROWID + "=" + lRow1, null);
		// correctingRowID(lRow1);
		return true;

	}

}
