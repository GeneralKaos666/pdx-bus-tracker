package com.trimettransit.tracker.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.trimettransit.tracker.model.Stop

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    val favorites: List<Stop>
        get() {
            val stops = mutableListOf<Stop>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM favorites", null).use { cursor ->
                while (cursor.moveToNext()) {
                    stops.add(
                        Stop(
                            desc = cursor.getString(cursor.getColumnIndexOrThrow("desc")),
                            dirDesc = cursor.getString(cursor.getColumnIndexOrThrow("dir_desc")),
                            transitType = cursor.getString(cursor.getColumnIndexOrThrow("transit_type")),
                            locId = cursor.getInt(cursor.getColumnIndexOrThrow("loc_id")),
                            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                            routeNum = cursor.getInt(cursor.getColumnIndexOrThrow("route_num"))
                        )
                    )
                }
            }
            db.close()
            return stops
        }

    val recentStops: List<Stop>
        get() {
            val stops = mutableListOf<Stop>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM recent_stops", null).use { cursor ->
                while (cursor.moveToNext()) {
                    stops.add(
                        0,
                        Stop(
                            desc = cursor.getString(cursor.getColumnIndexOrThrow("desc")),
                            dirDesc = cursor.getString(cursor.getColumnIndexOrThrow("dir_desc")),
                            transitType = cursor.getString(cursor.getColumnIndexOrThrow("transit_type")),
                            locId = cursor.getInt(cursor.getColumnIndexOrThrow("loc_id")),
                            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                            routeNum = cursor.getInt(cursor.getColumnIndexOrThrow("route_num"))
                        )
                    )
                }
            }
            db.close()
            return stops
        }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS favorites(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER UNIQUE,longitude REAL,latitude REAL,route_num INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS recent_stops(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER UNIQUE,longitude REAL,latitude REAL,route_num INTEGER)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_favorites_loc_id ON favorites(loc_id)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_recent_stops_loc_id ON recent_stops(loc_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS recent_stops(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER,longitude REAL,latitude REAL)")
        }
        if (oldVersion < 4) {
            runCatching { db.execSQL("ALTER TABLE favorites ADD COLUMN longitude REAL") }
            runCatching { db.execSQL("ALTER TABLE recent_stops ADD COLUMN longitude REAL") }
            runCatching { db.execSQL("ALTER TABLE favorites ADD COLUMN latitude REAL") }
            runCatching { db.execSQL("ALTER TABLE recent_stops ADD COLUMN latitude REAL") }
        }
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS search_data")
        }
        if (oldVersion < 6) {
            db.execSQL("DELETE FROM favorites WHERE rowid NOT IN (SELECT MIN(rowid) FROM favorites GROUP BY loc_id)")
            db.execSQL("DELETE FROM recent_stops WHERE rowid NOT IN (SELECT MIN(rowid) FROM recent_stops GROUP BY loc_id)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_favorites_loc_id ON favorites(loc_id)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_recent_stops_loc_id ON recent_stops(loc_id)")
        }
        if (oldVersion < 7) {
            runCatching { db.execSQL("ALTER TABLE favorites ADD COLUMN route_num INTEGER DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE recent_stops ADD COLUMN route_num INTEGER DEFAULT 0") }
        }
    }

    fun addFavorite(stop: Stop, view: View?) {
        val db = writableDatabase
        db.query("favorites", arrayOf("loc_id"), "loc_id = ?", arrayOf(stop.locId.toString()), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) {
                ContentValues().apply {
                    put("desc", stop.desc)
                    put("dir_desc", stop.dirDesc)
                    put("loc_id", stop.locId)
                    put("transit_type", stop.transitType)
                    put("longitude", stop.longitude)
                    put("latitude", stop.latitude)
                    put("route_num", stop.routeNum)
                    db.insertWithOnConflict("favorites", null, this, SQLiteDatabase.CONFLICT_IGNORE)
                }
                if (view != null) {
                    Snackbar.make(view, R.string.favorite_added_text, Snackbar.LENGTH_INDEFINITE).show()
                }
            } else {
                if (view != null) {
                    Snackbar.make(view, R.string.favorite_exists_text, Snackbar.LENGTH_INDEFINITE).show()
                }
            }
        }
    }

    fun isFavorite(locId: Int): Boolean {
        val db = readableDatabase
        return db.query("favorites", arrayOf("loc_id"), "loc_id = ?", arrayOf(locId.toString()), null, null, null).use { cursor ->
            cursor.moveToFirst()
        }
    }

    fun removeFavorite(locId: Int, view: View?) {
        val db = writableDatabase
        if (db.delete("favorites", "loc_id = ?", arrayOf(locId.toString())) > 0) {
            Snackbar.make(view!!, R.string.favorite_deleted_text, Snackbar.LENGTH_INDEFINITE).show()
        } else {
            Snackbar.make(view!!, R.string.favorite_does_not_exist_text, Snackbar.LENGTH_INDEFINITE).show()
        }
        db.close()
    }

    fun addRecentStop(stop: Stop) {
        val db = writableDatabase
        db.delete("recent_stops", "loc_id = ?", arrayOf(stop.locId.toString()))
        ContentValues().apply {
            put("desc", stop.desc)
            put("dir_desc", stop.dirDesc)
            put("loc_id", stop.locId)
            put("transit_type", stop.transitType)
            put("longitude", stop.longitude)
            put("latitude", stop.latitude)
            put("route_num", stop.routeNum)
            db.insertWithOnConflict("recent_stops", null, this, SQLiteDatabase.CONFLICT_REPLACE)
        }
        db.rawQuery("SELECT COUNT(*) FROM recent_stops", null).use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) > 20) {
                db.execSQL("DELETE FROM recent_stops WHERE id NOT IN (SELECT id FROM recent_stops ORDER BY id DESC LIMIT 20)")
            }
        }
    }

    companion object {
        private const val DB_NAME = "TriMet_Go.db"
        private const val DB_VERSION = 7
    }
}
