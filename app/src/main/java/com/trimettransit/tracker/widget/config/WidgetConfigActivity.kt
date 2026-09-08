package com.trimettransit.tracker.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.ui.theme.TriMetGoTheme
import com.trimettransit.tracker.widget.NextArrivalsWidget
import com.trimettransit.tracker.widget.WidgetConfig
import com.trimettransit.tracker.widget.toPersistentMap
import kotlinx.coroutines.launch

/**
 * Hosts [WidgetConfigScreen] for both first-time widget placement and long-press
 * "Edit" reconfiguration. Persists the per-widget config and re-renders the widget.
 */
class WidgetConfigActivity : ComponentActivity() {

    private val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(DatabaseHelper(applicationContext))
    }

    /** ID of the widget being configured; absent only on a deep-launch outside the launcher. */
    private val appWidgetId: Int? by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ).takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
    }

    private val glanceId: GlanceId? by lazy {
        appWidgetId?.let {
            runCatching { GlanceAppWidgetManager(this).getGlanceIdBy(it) }.getOrNull()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TriMetGoTheme {
                // New placement starts from defaults; reconfig loads the live config first.
                var initialConfig by remember {
                    mutableStateOf(if (glanceId == null) WidgetConfig() else null)
                }
                LaunchedEffect(glanceId) {
                    initialConfig = glanceId?.let { id ->
                        runCatching { loadConfig(id) }.getOrNull() ?: WidgetConfig()
                    }
                }
                val initial = initialConfig ?: return@TriMetGoTheme
                WidgetConfigScreen(
                    initial = initial,
                    favoritesRepository = favoritesRepository,
                    onDone = ::saveAndFinish,
                    onCancel = ::cancelAndFinish
                )
            }
        }
    }

    private suspend fun loadConfig(id: GlanceId): WidgetConfig =
        WidgetConfig.fromPersistentMap(
            getAppWidgetState<Preferences>(this, PreferencesGlanceStateDefinition, id)
                .toConfigMap()
        )

    private fun saveAndFinish(config: WidgetConfig) {
        val id = glanceId
        if (id == null) {
            appWidgetId?.let { setResult(RESULT_OK, resultIntent(it)) }
            finish()
            return
        }
        lifecycleScope.launch {
            // Writes exactly the Task 1 persistent-map keys; the title key is omitted
            // when the title is null.
            updateAppWidgetState(this@WidgetConfigActivity, id) { mutable ->
                config.toPersistentMap().forEach { (key, value) ->
                    mutable[stringPreferencesKey(key)] = value
                }
            }
            NextArrivalsWidget().update(this@WidgetConfigActivity, id)
            appWidgetId?.let { setResult(RESULT_OK, resultIntent(it)) }
            finish()
        }
    }

    private fun cancelAndFinish() {
        appWidgetId?.let { setResult(RESULT_CANCELED, resultIntent(it)) }
        finish()
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

private fun Preferences.toConfigMap(): Map<String, String> = buildMap {
    asMap().forEach { (key, value) -> put(key.name, value.toString()) }
}