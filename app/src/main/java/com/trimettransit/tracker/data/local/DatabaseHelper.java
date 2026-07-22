package com.trimettransit.tracker.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import com.trimettransit.tracker.R;
import com.trimettransit.tracker.data.model.Stop;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "TriMet_Go.db";
    private static final int DB_VERSION = 6;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, DB_VERSION);
    }

    public List<Stop> getFavorites() {
        List<Stop> arrayList = new ArrayList<>();
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT * FROM favorites", null);
        try {
            if (cursorRawQuery.moveToFirst()) {
                do {
                    Stop stop = new Stop();
                    stop.setDesc(cursorRawQuery.getString(cursorRawQuery.getColumnIndexOrThrow("desc")));
                    stop.setDirDesc(cursorRawQuery.getString(cursorRawQuery.getColumnIndexOrThrow("dir_desc")));
                    stop.setTransitType(cursorRawQuery.getString(cursorRawQuery.getColumnIndexOrThrow("transit_type")));
                    stop.setLocId(cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id")));
                    stop.setLongitude(cursorRawQuery.getDouble(cursorRawQuery.getColumnIndexOrThrow("longitude")));
                    stop.setLatitude(cursorRawQuery.getDouble(cursorRawQuery.getColumnIndexOrThrow("latitude")));
                    arrayList.add(stop);
                } while (cursorRawQuery.moveToNext());
            }
        } finally {
            cursorRawQuery.close();
            readableDatabase.close();
        }
        return arrayList;
    }

    public List<Stop> getRecentStops() {
        List<Stop> arrayList = new ArrayList<>();
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT * FROM recent_stops", null);
        try {
            if (cursorRawQuery.moveToFirst()) {
                do {
                    Stop stop = new Stop();
                    stop.setDesc(cursorRawQuery.getString(cursorRawQuery.getColumnIndexOrThrow("desc")));
                    stop.setDirDesc(cursorRawQuery.getString(cursorRawQuery.getColumnIndexOrThrow("dir_desc")));
                    stop.setTransitType(cursorRawQuery.getString(cursorRawQuery.getColumnIndexOrThrow("transit_type")));
                    stop.setLocId(cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id")));
                    stop.setLongitude(cursorRawQuery.getDouble(cursorRawQuery.getColumnIndexOrThrow("longitude")));
                    stop.setLatitude(cursorRawQuery.getDouble(cursorRawQuery.getColumnIndexOrThrow("latitude")));
                    arrayList.add(0, stop);
                } while (cursorRawQuery.moveToNext());
            }
        } finally {
            cursorRawQuery.close();
            readableDatabase.close();
        }
        return arrayList;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS favorites(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER UNIQUE,longitude REAL,latitude REAL)");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS recent_stops(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER UNIQUE,longitude REAL,latitude REAL)");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_favorites_loc_id ON favorites(loc_id)");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_recent_stops_loc_id ON recent_stops(loc_id)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 2) {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS recent_stops(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)");
        }
        if (i < 4) {
            try {
                sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN longitude REAL");
            } catch (Exception ignored) {
            }
            try {
                sQLiteDatabase.execSQL("ALTER TABLE recent_stops ADD COLUMN longitude REAL");
            } catch (Exception ignored2) {
            }
            try {
                sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN latitude REAL");
            } catch (Exception ignored3) {
            }
            try {
                sQLiteDatabase.execSQL("ALTER TABLE recent_stops ADD COLUMN latitude REAL");
            } catch (Exception ignored4) {
            }
        }
        if (i < 5) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS search_data");
        }
        if (i < 6) {
            sQLiteDatabase.execSQL("DELETE FROM favorites WHERE rowid NOT IN (SELECT MIN(rowid) FROM favorites GROUP BY loc_id)");
            sQLiteDatabase.execSQL("DELETE FROM recent_stops WHERE rowid NOT IN (SELECT MIN(rowid) FROM recent_stops GROUP BY loc_id)");
            sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_favorites_loc_id ON favorites(loc_id)");
            sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_recent_stops_loc_id ON recent_stops(loc_id)");
        }
    }

    public void addFavorite(Stop stop, View view) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        Cursor cursorRawQuery = writableDatabase.query("favorites", new String[]{"loc_id"}, "loc_id = ?", new String[]{String.valueOf(stop.getLocId())}, null, null, null);
        boolean z = cursorRawQuery.moveToFirst();
        cursorRawQuery.close();
        if (!z) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("desc", stop.getDesc());
            contentValues.put("dir_desc", stop.getDirDesc());
            contentValues.put("loc_id", Integer.valueOf(stop.getLocId()));
            contentValues.put("transit_type", stop.getTransitType());
            contentValues.put("longitude", Double.valueOf(stop.getLongitude()));
            contentValues.put("latitude", Double.valueOf(stop.getLatitude()));
            writableDatabase.insertWithOnConflict("favorites", null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
            if (view != null) {
                Snackbar.make(view, R.string.favorite_added_text, -1).show();
            }
        } else {
            if (view != null) {
                Snackbar.make(view, R.string.favorite_exists_text, -1).show();
            }
        }
        writableDatabase.close();
    }

    public boolean isFavorite(int i) {
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursorRawQuery = readableDatabase.query("favorites", new String[]{"loc_id"}, "loc_id = ?", new String[]{String.valueOf(i)}, null, null, null);
        boolean z = cursorRawQuery.moveToFirst();
        cursorRawQuery.close();
        return z;
    }

    public void removeFavorite(int i, View view) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase.delete("favorites", "loc_id = ?", new String[]{String.valueOf(i)}) > 0) {
            Snackbar.make(view, R.string.favorite_deleted_text, -1).show();
        } else {
            Snackbar.make(view, R.string.favorite_does_not_exist_text, -1).show();
        }
        writableDatabase.close();
    }

    public void addRecentStop(Stop stop) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.delete("recent_stops", "loc_id = ?", new String[]{String.valueOf(stop.getLocId())});
        ContentValues contentValues = new ContentValues();
        contentValues.put("desc", stop.getDesc());
        contentValues.put("dir_desc", stop.getDirDesc());
        contentValues.put("loc_id", Integer.valueOf(stop.getLocId()));
        contentValues.put("transit_type", stop.getTransitType());
        contentValues.put("longitude", Double.valueOf(stop.getLongitude()));
        contentValues.put("latitude", Double.valueOf(stop.getLatitude()));
        writableDatabase.insertWithOnConflict("recent_stops", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        Cursor countCursor = writableDatabase.rawQuery("SELECT COUNT(*) FROM recent_stops", null);
        if (countCursor.moveToFirst() && countCursor.getInt(0) > 20) {
            writableDatabase.execSQL("DELETE FROM recent_stops WHERE id NOT IN (SELECT id FROM recent_stops ORDER BY id DESC LIMIT 20)");
        }
        countCursor.close();
        writableDatabase.close();
    }
}
