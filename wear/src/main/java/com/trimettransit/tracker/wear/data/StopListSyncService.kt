package com.trimettransit.tracker.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.StopSyncContract
import com.trimettransit.tracker.data.local.parseStopSyncJson
import timber.log.Timber

/**
 * Receives favorites/recent-stop pushes from the phone app and caches them into
 * this device's local database, keeping the watch UI in sync while it is open.
 */
class StopListSyncService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val json = DataMapItem.fromDataItem(event.dataItem)
                .dataMap
                .getString(StopSyncContract.KEY_STOPS)
                ?.takeIf { it.isNotEmpty() }
                ?: return@forEach
            val stops = json.parseStopSyncJson()
            when (event.dataItem.uri.path) {
                StopSyncContract.PATH_FAVORITES ->
                    runCatching { DatabaseHelper(this).replaceFavorites(stops) }
                        .onFailure { Timber.w(it, "Failed to cache favorites from phone") }
                StopSyncContract.PATH_RECENT ->
                    runCatching { DatabaseHelper(this).replaceRecentStops(stops) }
                        .onFailure { Timber.w(it, "Failed to cache recent stops from phone") }
            }
        }
    }
}