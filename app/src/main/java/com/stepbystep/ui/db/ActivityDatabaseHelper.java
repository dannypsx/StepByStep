package com.stepbystep.ui.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.stepbystep.ui.model.ActivityRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite helper for persisting activity records permanently.
 */
public final class ActivityDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "stepbystep.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ACTIVITIES = "activities";
    public static final String COL_ID = "id";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_STEPS = "steps";
    public static final String COL_SHAKES = "shakes";
    public static final String COL_SECONDS = "seconds";

    private static ActivityDatabaseHelper instance;

    public static synchronized ActivityDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ActivityDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ActivityDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ACTIVITIES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_STEPS + " INTEGER NOT NULL, "
                + COL_SHAKES + " INTEGER NOT NULL, "
                + COL_SECONDS + " REAL NOT NULL"
                + ")");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITIES);
        onCreate(db);
    }

    /** Inserts a new record and returns the generated row id. */
    public long insertRecord(long createdAt, int steps, int shakes, double seconds) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CREATED_AT, createdAt);
        values.put(COL_STEPS, steps);
        values.put(COL_SHAKES, shakes);
        values.put(COL_SECONDS, seconds);
        return db.insert(TABLE_ACTIVITIES, null, values);
    }

    /** Inserts with a specific ID (useful for restore/undo). */
    public void insertWithId(ActivityRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ID, record.id);
        values.put(COL_CREATED_AT, record.createdAt);
        values.put(COL_STEPS, record.steps);
        values.put(COL_SHAKES, record.shakes);
        values.put(COL_SECONDS, record.seconds);
        db.insertWithOnConflict(TABLE_ACTIVITIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Deletes a record by id. */
    public void deleteRecord(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ACTIVITIES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /** Returns all records ordered from newest to oldest. */
    public List<ActivityRecord> getAllRecords() {
        List<ActivityRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_ACTIVITIES,
                new String[]{COL_ID, COL_CREATED_AT, COL_STEPS, COL_SHAKES, COL_SECONDS},
                null, null, null, null, COL_CREATED_AT + " DESC, " + COL_ID + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                    long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT));
                    int steps = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STEPS));
                    int shakes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SHAKES));
                    double seconds = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SECONDS));
                    list.add(new ActivityRecord(id, createdAt, steps, shakes, seconds));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }
}
