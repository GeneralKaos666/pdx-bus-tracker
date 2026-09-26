package com.trimettransit.tracker.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.ADD_PINNED_LINE_COLUMN_SQL
import com.trimettransit.tracker.model.domain.PINNED_LINE_COLUMN
import com.trimettransit.tracker.model.domain.mapPinnedLine

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    val favorites: List<Stop>
        get() {
            val stops = mutableListOf<Stop>()
            val db = readableDatabase
            db.rawQuery("SELECT desc, dir_desc, transit_type, loc_id, longitude, latitude, route_num FROM favorites ORDER BY CASE WHEN sort_order > 0 THEN sort_order ELSE id END ASC, id ASC", null).use { cursor ->
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
            return stops
        }

    /**
     * Favourited stop ids only. [favorites] selects every column and builds a [Stop] per row; a
     * favourite toggle needs nothing but the id, so this avoids materialising the rest.
     */
    val favoriteIds: Set<Int>
        get() {
            val ids = mutableSetOf<Int>()
            val db = readableDatabase
            db.rawQuery("SELECT loc_id FROM favorites", null).use { cursor ->
                while (cursor.moveToNext()) {
                    ids.add(cursor.getInt(cursor.getColumnIndexOrThrow("loc_id")))
                }
            }
            return ids
        }

    val recentStops: List<Stop>
        get() {
            val stops = mutableListOf<Stop>()
            val db = readableDatabase
            db.rawQuery("SELECT desc, dir_desc, transit_type, loc_id, longitude, latitude, route_num FROM recent_stops ORDER BY id DESC", null).use { cursor ->
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
            return stops
        }

    override fun onCreate(db: SQLiteDatabase) {
        // `label` is reserved for the deferred custom favorite-name feature: nothing
        // reads or writes it yet, but the v8 upgrade path already adds the column
        // on existing installs, so fresh installs keep it for schema parity. Do not
        // drop it without a new versioned migration.
        // `pinned_line` (v9) holds the per-stop line-pin default; NULL = no default.
        db.execSQL("CREATE TABLE IF NOT EXISTS favorites(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER UNIQUE,longitude REAL,latitude REAL,route_num INTEGER,sort_order INTEGER DEFAULT 0,label TEXT DEFAULT '',pinned_line INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS recent_stops(id INTEGER PRIMARY KEY AUTOINCREMENT,desc TEXT,dir_desc TEXT,transit_type TEXT,loc_id INTEGER UNIQUE,longitude REAL,latitude REAL,route_num INTEGER)")
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
        if (oldVersion < 8) {
            runCatching { db.execSQL("ALTER TABLE favorites ADD COLUMN sort_order INTEGER DEFAULT 0") }
            // Reserved for the deferred custom favorite-name feature (see onCreate):
            // kept deliberately so v7 databases match the v8 schema.
            runCatching { db.execSQL("ALTER TABLE favorites ADD COLUMN label TEXT DEFAULT ''") }
            runCatching { db.execSQL("UPDATE favorites SET sort_order = id WHERE sort_order = 0 OR sort_order IS NULL") }
        }
        if (oldVersion < 9) {
            // Per-stop line-pin default. Plain nullable INTEGER: every existing
            // row reads back NULL, i.e. today's behavior, with no backfill.
            runCatching { db.execSQL(ADD_PINNED_LINE_COLUMN_SQL) }
        }
    }

    fun addFavorite(stop: Stop): Boolean {
        val db = writableDatabase
        // The MAX(sort_order) read and the insert must be atomic or two
        // concurrent adds can compute the same sort_order.
        return db.transaction {
            val values = stopValues(stop)
            rawQuery("SELECT COALESCE(MAX(sort_order), 0) FROM favorites", null).use { cursor ->
                val next = if (cursor.moveToFirst()) cursor.getInt(0) + 1 else 1
                values.put("sort_order", next)
            }
            val rowId = insertWithOnConflict(
                "favorites",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            rowId != -1L
        }
    }

    fun isFavorite(locId: Int): Boolean {
        val db = readableDatabase
        return db.query("favorites", arrayOf("loc_id"), "loc_id = ?", arrayOf(locId.toString()), null, null, null).use { cursor ->
            cursor.moveToFirst()
        }
    }

    fun removeFavorite(locId: Int): Boolean {
        val db = writableDatabase
        val removed = db.delete("favorites", "loc_id = ?", arrayOf(locId.toString())) > 0
        return removed
    }

    fun setFavoriteOrder(idsInOrder: List<Int>) {
        val db = writableDatabase
        db.transaction {
            idsInOrder.forEachIndexed { index, locId ->
                ContentValues().apply { put("sort_order", index + 1) }.let { values ->
                    update("favorites", values, "loc_id = ?", arrayOf(locId.toString()))
                }
            }
        }
    }

    /**
     * Stored per-stop line-pin default, or null when the stop is not favorited
     * or no pin was ever set (NULL column = today's behavior).
     */
    fun getPinnedLine(locId: Int): Int? {
        val db = readableDatabase
        return db.query(
            "favorites",
            arrayOf(PINNED_LINE_COLUMN),
            "loc_id = ?",
            arrayOf(locId.toString()),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null
            else mapPinnedLine(cursor.isNull(0), cursor.getInt(0))
        }
    }

    /**
     * Stores ([routeId]) or clears (null) the per-stop line-pin default. A
     * no-op for stops that are not favorited. Transactional like the other
     * favorites writes.
     */
    fun setPinnedLine(locId: Int, routeId: Int?) {
        val db = writableDatabase
        db.transaction {
            val values = ContentValues().apply {
                if (routeId == null) putNull(PINNED_LINE_COLUMN)
                else put(PINNED_LINE_COLUMN, routeId)
            }
            update("favorites", values, "loc_id = ?", arrayOf(locId.toString()))
        }
    }

    fun removeRecentStop(locId: Int): Boolean {
        val db = writableDatabase
        return db.delete("recent_stops", "loc_id = ?", arrayOf(locId.toString())) > 0
    }

    fun clearRecentStops() {
        val db = writableDatabase
        db.delete("recent_stops", null, null)
    }

    fun addRecentStop(stop: Stop) {
        val db = writableDatabase
        // Delete+insert (and the trim below) must be atomic or a crash mid-way
        // can leave the stop duplicated under its UNIQUE index replacement.
        db.transaction {
            delete("recent_stops", "loc_id = ?", arrayOf(stop.locId.toString()))
            insertWithOnConflict("recent_stops", null, stopValues(stop), SQLiteDatabase.CONFLICT_REPLACE)
            rawQuery("SELECT COUNT(*) FROM recent_stops", null).use { cursor ->
                if (cursor.moveToFirst() && cursor.getInt(0) > 20) {
                    execSQL("DELETE FROM recent_stops WHERE id NOT IN (SELECT id FROM recent_stops ORDER BY id DESC LIMIT 20)")
                }
            }
        }
    }

    private fun stopValues(stop: Stop) = ContentValues().apply {
        put("desc", stop.desc)
        put("dir_desc", stop.dirDesc)
        put("loc_id", stop.locId)
        put("transit_type", stop.transitType)
        put("longitude", stop.longitude)
        put("latitude", stop.latitude)
        put("route_num", stop.routeNum)
    }

    companion object {
        private const val DB_NAME = "TriMet_Go.db"
        private const val DB_VERSION = 9
    }
}
