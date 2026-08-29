package com.trimettransit.tracker.wear.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.StopSyncContract
import com.trimettransit.tracker.data.local.parseStopSyncJson
import timber.log.Timber

/**
 * Pulls the latest favorites/recent-stop snapshots the phone has published and
 * applies them to this device's local database. Called on first screen load so
 * the watch shows fresh data even when the phone's last push predates this session.
 */
object WearDataPull {

    suspend fun pullInto(context: Context) {
        runCatching {
            val dataClient = Wearable.getDataClient(context)
            val items = dataClient
                .getDataItems(Uri.parse("wear://*/${StopSyncContract.PATH_ROOT}"))
                .await()

            var favorites: List<com.trimettransit.tracker.model.Stop>? = null
            var recent: List<com.trimettransit.tracker.model.Stop>? = null
            items.forEach { item ->
                val json = DataMapItem.fromDataItem(item)
                    .dataMap
                    .getString(StopSyncContract.KEY_STOPS)
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@forEach
                when (item.uri.path) {
                    StopSyncContract.PATH_FAVORITES -> favorites = json.parseStopSyncJson()
                    StopSyncContract.PATH_RECENT -> recent = json.parseStopSyncJson()
                }
            }

            val db = DatabaseHelper(context)
            favorites?.let { db.replaceFavorites(it) }
            recent?.let { db.replaceRecentStops(it) }
        }.onFailure {
            Timber.w(it, "Failed to pull stop lists from phone")
        }
    }
}