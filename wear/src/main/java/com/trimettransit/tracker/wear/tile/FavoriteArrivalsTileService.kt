package com.trimettransit.tracker.wear.tile

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.tile
import androidx.wear.tiles.timeline
import androidx.wear.tiles.timelineEntry
import com.trimettransit.tracker.R
import com.trimettransit.tracker.wear.WearMainActivity
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import timber.log.Timber

/**
 * Watch Tile showing the soonest departure from the watch's own first favorite
 * stop as a live per-minute countdown. Entries are pre-rendered for the next few
 * minutes so the system updates the face without waking the app; when the window
 * runs out the system re-requests the tile and the [TileRefreshWorker] keeps the
 * cache fresh in the background.
 */
class FavoriteArrivalsTileService : Material3TileService() {

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        return try {
            val snapshot = TileCache.snapshot(context)
            val next = snapshot?.arrivals?.sortedBy { it.arrivalTimeMillis }?.firstOrNull()
            if (snapshot == null || next == null) {
                noDeparturesTile(System.currentTimeMillis())
            } else {
                countdownTile(snapshot, next, System.currentTimeMillis())
            }
        } catch (t: Throwable) {
            Timber.e(t, "Tile build failed, showing fallback")
            fallbackTile()
        }
    }

    /** Last-resort layout so the tile never renders black when an error slips through. */
    private fun MaterialScope.fallbackTile(): Tile = tile(
        timeline = timeline(
            timelineEntry(
                layout = primaryLayout(
                    mainSlot = {
                        text(
                            text = LayoutString(context.getString(R.string.app_name)),
                            typography = Typography.TITLE_MEDIUM,
                            maxLines = 1
                        )
                    }
                )
            )
        )
    )

    private fun MaterialScope.noDeparturesTile(now: Long): Tile = tile(
        timeline = timeline(
            timelineEntry(
                layout = primaryLayout(
                    mainSlot = {
                        text(
                            text = LayoutString(context.getString(R.string.tile_fallback_title)),
                            typography = Typography.TITLE_MEDIUM,
                            maxLines = 2
                        )
                    }
                ),
                validity = validity(now, now + 15 * 60_000)
            )
        ),
        // Re-request well before the 15-minute window expires.
        freshness = 10.minutes
    )

    private fun MaterialScope.countdownTile(
        snapshot: TileCache.Snapshot,
        next: TileCache.TileArrival,
        now: Long
    ): Tile {
        val windowEnd = now + 12 * 60_000
        val entryCount = ((windowEnd - now + 59_999) / 60_000).toInt().coerceIn(1, 15)
        val stopName = snapshot.stop.desc.ifBlank { context.getString(R.string.stop_format, snapshot.stop.locId) }
        val subline = "${next.sign} · ${hourMinute(next.arrivalTimeMillis)}"

        val entries = (0 until entryCount).map { i ->
            val start = now + i * 60_000L
            timelineEntry(
                layout = primaryLayout(
                    titleSlot = {
                        text(
                            text = LayoutString(stopName),
                            typography = Typography.TITLE_SMALL,
                            maxLines = 1
                        )
                    },
                    mainSlot = {
                        val countdown = next.minutesFrom(start)
                        text(
                            text = LayoutString(if (countdown <= 0) context.getString(R.string.tile_due) else context.getString(R.string.tile_countdown_min, countdown)),
                            typography = Typography.DISPLAY_LARGE,
                            maxLines = 1
                        )
                    },
                    bottomSlot = {
                        text(
                            text = LayoutString(subline),
                            typography = Typography.LABEL_MEDIUM,
                            maxLines = 1
                        )
                    },
                    onClick = openStopClickable(snapshot)
                ),
                validity = validity(start, start + 60_000)
            )
        }

        return tile(
            timeline = timeline(*entries.toTypedArray()),
            // Shorter than the 12-minute window so the system re-requests before
            // the last entry expires, never leaving the face black.
            freshness = 10.minutes
        )
    }

    private fun MaterialScope.openStopClickable(snapshot: TileCache.Snapshot): ModifiersBuilders.Clickable {
        val extras = mapOf(
            TileExtras.KEY_LOC_ID to ActionBuilders.intExtra(snapshot.stop.locId),
            TileExtras.KEY_NAME to ActionBuilders.stringExtra(snapshot.stop.desc),
            TileExtras.KEY_ROUTE to ActionBuilders.intExtra(snapshot.stop.routeNum)
        )
        return ModifiersBuilders.Clickable.Builder()
            .setId("open_stop")
            .setOnClick(
                ActionBuilders.launchAction(
                    ComponentName(context, WearMainActivity::class.java),
                    extras
                )
            )
            .build()
    }

    /** Validity window for a timeline entry, as absolute epoch milliseconds. */
    private fun validity(start: Long, end: Long): TimelineBuilders.TimeInterval =
        TimelineBuilders.TimeInterval.Builder()
            .setStartMillis(start)
            .setEndMillis(end)
            .build()

    private fun hourMinute(epochMillis: Long): String {
        val t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        val h = (t.hour % 12).let { if (it == 0) 12 else it }
        val m = t.minute
        return if (m < 10) "$h:0$m" else "$h:$m"
    }
}