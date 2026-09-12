package com.trimettransit.tracker.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Alert
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.rememberDenseGridEnabled
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.staggeredFadeIn
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.util.SingleJobRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Load state for the home screen's service-alerts feed. */
class ServiceAlertsState(
    val alerts: List<Alert>,
    val isLoading: Boolean,
    val isError: Boolean
)

/**
 * Active service alerts (system-wide and route-scoped) from the TriMet Alerts V2 feed.
 * Auto-refreshes on app re-entry via RememberOnResume, job-deduped like the stop lists.
 * System-wide alerts sort first so general service messages aren't buried under route ones.
 */
@Composable
fun ServiceAlertsList(
    transitRepository: TransitRepository,
    modifier: Modifier = Modifier
) {
    val alerts = rememberServiceAlertsLoader { transitRepository.getAlerts() }
    val sorted = remember(alerts.alerts) {
        alerts.alerts.sortedWith(
            compareByDescending<Alert> { it.systemWide }
                .thenBy { it.displayTitle.lowercase() }
        )
    }
    Crossfade(
        targetState = when {
            alerts.isLoading && sorted.isEmpty() -> 0
            alerts.isError && sorted.isEmpty() -> 1
            sorted.isEmpty() -> 2
            else -> 3
        },
        animationSpec = m3EffectsDefault(),
        label = "serviceAlertsList"
    ) { state ->
        when (state) {
            0 -> ListLoadingSkeleton()
            1 -> ErrorState(message = stringResource(R.string.unable_to_load))
            2 -> EmptyState(message = stringResource(R.string.no_service_alerts))
            else -> AlertsList(alerts = sorted, modifier = modifier)
        }
    }
}

@Composable
private fun AlertsList(
    alerts: List<Alert>,
    modifier: Modifier = Modifier
) {
    val dense = rememberDenseGridEnabled()
    val listState = rememberLazyGridState()
    val smoothFling = rememberSmoothFlingBehavior()
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (dense) 2 else 1),
        state = listState,
        modifier = modifier.fillMaxSize(),
        flingBehavior = smoothFling,
        contentPadding = PaddingValues(
            top = 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = navPillBottomPadding() + 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(alerts, key = { _, alert -> alert.id }, contentType = { _, _ -> "alert" }) { index, alert ->
            ServiceAlertItem(
                alert = alert,
                modifier = Modifier.animateItem().staggeredFadeIn(index)
            )
        }
    }
}

@Composable
private fun ServiceAlertItem(
    alert: Alert,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = appCardShape(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (alert.systemWide) {
                Surface(
                    shape = appCardShape(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.system_wide_alert),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            val title = alert.displayTitle
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (alert.desc.isNotBlank() && alert.desc != title) {
                Text(
                    text = alert.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (alert.routeIds.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    alert.routeIds.forEach { routeId ->
                        Surface(
                            shape = appCardShape(),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "#$routeId",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberServiceAlertsLoader(
    read: suspend () -> List<Alert>?
): ServiceAlertsState {
    val coroutineScope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }
    val runner = remember { SingleJobRunner(coroutineScope) }

    fun load() {
        runner.launchWithJob { job ->
            try {
                alerts = withContext(Dispatchers.IO) { read().orEmpty() }
                isError = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to load service alerts")
                isError = true
            } finally {
                if (runner.isCurrent(job)) isLoading = false
            }
        }
    }

    RememberOnResume {
        if (hasLoaded) {
            isLoading = true
        } else {
            hasLoaded = true
        }
        load()
    }

    return ServiceAlertsState(alerts, isLoading, isError)
}