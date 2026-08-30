package com.trimettransit.tracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.wear.tile.TileExtras
import com.trimettransit.tracker.wear.tile.TileScheduler

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the stand-alone "Next departure" tile's background feed scheduled.
        TileScheduler.schedulePeriodic(this)
        setContent {
            WearApp(startStop = readTileStop())
        }
    }

    /** The Tile opens the app with a stop in the intent extras (deep link). */
    private fun readTileStop(): Stop? {
        val locId = intent?.getIntExtra(TileExtras.KEY_LOC_ID, 0) ?: 0
        if (locId <= 0) return null
        return Stop(
            desc = intent.getStringExtra(TileExtras.KEY_NAME).orEmpty(),
            locId = locId,
            routeNum = intent.getIntExtra(TileExtras.KEY_ROUTE, 0)
        )
    }
}