package com.trimettransit.tracker.feature.arrivals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.domain.displayTimeMillis
import com.trimettransit.tracker.model.domain.isCanceled
import com.trimettransit.tracker.model.domain.isEstimated
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardShape

internal const val TOP_ARRIVAL_ROWS = 5

@Composable
internal fun PipCountdownContent(
    arrivals: List<Arrival>,
    stopName: String,
    modifier: Modifier = Modifier,
    tick: Int = 0
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stopName.ifBlank { stringResource(R.string.stop) },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        if (arrivals.isEmpty()) {
            Text(
                text = stringResource(R.string.no_upcoming_arrivals),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        } else {
            arrivals.take(TOP_ARRIVAL_ROWS).forEach { arrival ->
                val type = transitBadgeLetter(arrival.routeId)
                val color = transitColor(
                    type,
                    scheme,
                    LocalAppearanceStyle.current.transitTypeColors
                )
                val displayTime = arrival.displayTimeMillis
                val minutesAway = if (displayTime > 0L) minutesUntil(displayTime) else 0L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = appCardShape(),
                        color = color
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = transitIconResource(type)),
                                contentDescription = null,
                                tint = scheme.surface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = arrival.shortSign,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (arrival.isCanceled) {
                        Text(
                            text = stringResource(R.string.canceled),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.error
                        )
                    } else if (arrival.dropOffOnly) {
                        Text(
                            text = stringResource(R.string.dropoff_only),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        CountdownLabel(
                            minutesAway = minutesAway,
                            isEstimated = arrival.isEstimated,
                            color = color,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}