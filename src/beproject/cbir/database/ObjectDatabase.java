package beproject.cbir.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ObjectDatabase {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_SNO = "sno_no";
	public static final String KEY_PATH = "file_path";
	public static final String KEY_ACCEPT = "tag_accept";
	public static final String KEY_TOTAL = "tag_total";

	public static final String DATABASE_NAME = "TagsValsdb";
	public static final String DATABASE_TABLE = "TAGsValsTable";
	public static final int DATABASE_VERSION = 1;

	public DbHelper ourHelper;
	public final Context ourContext;
	public SQLiteDatabase ourDatabase;
	public int i;
	public static String[] column = new String[] { KEY_ROWID, KEY_SNO,
			KEY_PATH, KEY_ACCEPT, KEY_TOTAL };

	public static class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ROWID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_SNO
					+ " TEXT NOT NULL, " + KEY_PATH + " TEXT NOT NULL, "
					+ KEY_ACCEPT + " INTEGER NOT NULL, " + KEY_TOTAL
					+ " INTEGER NOT NULL);");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}

	}

	public ObjectDatabase(Context c) {
		ourContext = c;
	}

	public ArrayList<String> GetPaths() {

		String Query = "Select * from " + DATABASE_TABLE;
		Cursor cursor = ourDatabase.rawQuery(Query, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			ArrayList<String> paths = new ArrayList<String>();
			for (int i = 0; i < cursor.getCount(); i++) {
				paths.add(cursor.getString(cursor.getColumnIndex(KEY_PATH)));
				cursor.moveToNext();
			}
			return paths;
		}
		return null;

	}

	public ObjectDatabase open() throws SQLException {
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

	public void deleteUnworthyTags() {
		String Query = "Select * from " + DATABASE_TABLE + " where "
				+ KEY_TOTAL + ">=" + 10 + "";
		Cursor cursor = ourDatabase.rawQuery(Query, null);
		if (cursor.getCount() <= 0) {
			return;
		}

		for (cursor.moveToFirst(); cursor.isAfterLast(); cursor.moveToNext()) {
			int accept = cursor.getInt(cursor.getColumnIndex(KEY_ACCEPT));
			int total = cursor.getInt(cursor.getColumnIndex(KEY_TOTAL));

			if ((float) (accept / total) < 0.5f) {
				deleteEntry(cursor.getString(cursor.getColumnIndex(KEY_PATH)));
			}
		}

	}

	public long CreateOrUpdateEntry(String path, int accepted, int total) {

		if (CheckIsDataAlreadyInDBorNot(path)) {

			if (total == 1)
				return 1;

			String Query = "Select * from " + DATABASE_TABLE + " where "
					+ KEY_PATH + "='" + path + "'";
			Cursor cursor = ourDatabase.rawQuery(Query, null);
			cursor.moveToFirst();
			updateEntry(cursor.getInt(cursor.getColumnIndex(KEY_ROWID)), path,
					accepted, total);
			return 1;

		}
		String[] columns = new String[] { KEY_ROWID, KEY_SNO, KEY_PATH,
				KEY_ACCEPT, KEY_TOTAL };
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
		cv.put(KEY_TOTAL, total);
		cv.put(KEY_ACCEPT, accepted);
		cv.put(KEY_SNO, l + 1);
		return ourDatabase.insert(DATABASE_TABLE, null, cv);

	}

	public void updateEntry(long lRow, String mPath, int mAccept, int mTotal)
			throws SQLException {
		ContentValues cvUpdated = new ContentValues();
		cvUpdated.put(KEY_PATH, mPath);
		cvUpdated.put(KEY_TOTAL, mTotal);
		cvUpdated.put(KEY_ACCEPT, mAccept);
		ourDatabase.update(DATABASE_TABLE, cvUpdated, KEY_ROWID + "=" + lRow,
				null);
	}

	public boolean deleteEntry(long lRow1) throws SQLException {
		ourDatabase.delete(DATABASE_TABLE, KEY_ROWID + "=" + lRow1, null);
		return true;

	}

	public void updateEntry(String path, int accept, int total)
			throws SQLException {
		ContentValues cvUpdated = new ContentValues();
		// cvUpdated.put(KEY_PATH, mPath);
		String Query = "Select * from " + DATABASE_TABLE + " where " + KEY_PATH
				+ "='" + path + "'";
		Cursor cursor = ourDatabase.rawQuery(Query, null);
		if (cursor.getCount() <= 0)
			return;
		cursor.moveToFirst();
		int lRow = cursor.getInt(cursor.getColumnIndex(KEY_ROWID));
		int mTotal = cursor.getInt(cursor.getColumnIndex(KEY_TOTAL)) + total;
		int mAccept = cursor.getInt(cursor.getColumnIndex(KEY_ACCEPT)) + accept;
		cvUpdated.put(KEY_TOTAL, mTotal);
		cvUpdated.put(KEY_ACCEPT, mAccept);
		ourDatabase.update(DATABASE_TABLE, cvUpdated, KEY_ROWID + "=" + lRow,
				null);

	}

	public void deleteEntry(String path) {
		try {
			ourDatabase.delete(DATABASE_TABLE, KEY_PATH + "='" + path + "'",
					null);
		} catch (SQLException ex) {
			Log.d("Del OBD", ex.toString());
		}

	}

}
