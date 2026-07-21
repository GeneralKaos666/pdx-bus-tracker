package com.something15525.trimetgo.trimet_go.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import com.something15525.trimetgo.trimet_go.R;
import com.something15525.trimetgo.trimet_go.data.model.Stop;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "TriMet_Go.db", (SQLiteDatabase.CursorFactory) null, 5);
    }

    public List<Stop> getFavorites() {
        ArrayList arrayList = new ArrayList();
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT * FROM favorites", null);
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
        cursorRawQuery.close();
        readableDatabase.close();
        return arrayList;
    }

    public List<Stop> getRecentStops() {
        ArrayList arrayList = new ArrayList();
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT * FROM recent_stops", null);
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
        cursorRawQuery.close();
        readableDatabase.close();
        return arrayList;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE favorites(id INTEGER PRIMARY KEY,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)");
        sQLiteDatabase.execSQL("CREATE TABLE recent_stops(id INTEGER PRIMARY KEY,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i2 == 1) {
            sQLiteDatabase.execSQL("CREATE TABLE favorites(id INTEGER PRIMARY KEY,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)");
        }
        if (i < 2) {
            sQLiteDatabase.execSQL("CREATE TABLE recent_stops(id INTEGER PRIMARY KEY,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)");
            i = 2;
        }
        if (i < 3 && i2 != 5) {
            sQLiteDatabase.execSQL("CREATE TABLE search_data(id INTEGER PRIMARY KEY,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)");
        }
        if (i < 4) {
            sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN longitude REAL");
            sQLiteDatabase.execSQL("ALTER TABLE recent_stops ADD COLUMN longitude REAL");
            if (i2 != 5) {
                sQLiteDatabase.execSQL("ALTER TABLE search_data ADD COLUMN longitude REAL");
            }
            sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN latitude REAL");
            sQLiteDatabase.execSQL("ALTER TABLE recent_stops ADD COLUMN latitude REAL");
            if (i2 != 5) {
                sQLiteDatabase.execSQL("ALTER TABLE search_data ADD COLUMN latitude REAL");
            }
        }
        if (i < 5) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS search_data");
        }
    }

    public void addFavorite(Stop stop, View view) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT loc_id FROM favorites", null);
        boolean z = false;
        if (cursorRawQuery.moveToFirst()) {
            do {
                if (cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id")) == stop.getLocId()) {
                    z = true;
                }
            } while (cursorRawQuery.moveToNext());
        }
        cursorRawQuery.close();
        if (!z) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("desc", stop.getDesc());
            contentValues.put("dir_desc", stop.getDirDesc());
            contentValues.put("loc_id", Integer.valueOf(stop.getLocId()));
            contentValues.put("transit_type", stop.getTransitType());
            contentValues.put("longitude", Double.valueOf(stop.getLongitude()));
            contentValues.put("latitude", Double.valueOf(stop.getLatitude()));
            writableDatabase.insert("favorites", null, contentValues);
            Snackbar.make(view, R.string.favorite_added_text, -1).show();
        } else {
            Snackbar.make(view, R.string.favorite_exists_text, -1).show();
        }
        writableDatabase.close();
    }

    public boolean isFavorite(int i) {
        Cursor cursorRawQuery = getWritableDatabase().rawQuery("SELECT loc_id FROM favorites", null);
        boolean z = false;
        if (cursorRawQuery.moveToFirst()) {
            do {
                if (cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id")) == i) {
                    z = true;
                }
            } while (cursorRawQuery.moveToNext());
        }
        cursorRawQuery.close();
        return z;
    }

    public void removeFavorite(int i, View view) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase.delete("favorites", "loc_id = " + i, null) > 0) {
            Snackbar.make(view, R.string.favorite_deleted_text, -1).show();
        } else {
            Snackbar.make(view, R.string.favorite_does_not_exist_text, -1).show();
        }
        writableDatabase.close();
    }

    public void addRecentStop(Stop stop) {
        int position;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT loc_id FROM recent_stops", null);
        boolean z = false;
        if (cursorRawQuery.moveToFirst()) {
            position = 0;
            do {
                if (cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id")) == stop.getLocId()) {
                    position = cursorRawQuery.getPosition();
                    z = true;
                }
            } while (cursorRawQuery.moveToNext());
        } else {
            position = 0;
        }
        if (!z) {
            if (cursorRawQuery.getCount() >= 20) {
                cursorRawQuery.moveToFirst();
                writableDatabase.delete("recent_stops", "loc_id = " + Integer.toString(cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id"))), null);
            }
        } else {
            cursorRawQuery.moveToPosition(position);
            writableDatabase.delete("recent_stops", "loc_id = " + Integer.toString(cursorRawQuery.getInt(cursorRawQuery.getColumnIndexOrThrow("loc_id"))), null);
        }
        cursorRawQuery.close();
        ContentValues contentValues = new ContentValues();
        contentValues.put("desc", stop.getDesc());
        contentValues.put("dir_desc", stop.getDirDesc());
        contentValues.put("loc_id", Integer.valueOf(stop.getLocId()));
        contentValues.put("transit_type", stop.getTransitType());
        contentValues.put("longitude", Double.valueOf(stop.getLongitude()));
        contentValues.put("latitude", Double.valueOf(stop.getLatitude()));
        writableDatabase.insert("recent_stops", null, contentValues);
        writableDatabase.close();
    }
}
