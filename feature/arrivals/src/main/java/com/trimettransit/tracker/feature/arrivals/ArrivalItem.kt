package com.trimettransit.tracker.feature.arrivals

import android.content.Context
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.domain.displayTimeMillis
import com.trimettransit.tracker.model.domain.isCanceled
import com.trimettransit.tracker.model.domain.isEstimated
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitTypeLabel
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import com.trimettransit.tracker.util.formatDateTime
import kotlin.math.roundToInt
import org.joda.time.DateTime

@Composable
internal fun ArrivalItem(
    arrival: Arrival,
    context: Context,
    modifier: Modifier = Modifier,
    refreshKey: Int = 0,
    lineDetours: List<Detour> = emptyList(),
    onShowAlerts: (List<Detour>) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val type = transitBadgeLetter(arrival.routeId)
    val scheme = MaterialTheme.colorScheme
    val overrides = LocalAppearanceStyle.current.transitTypeColors
    val color = transitColor(type, scheme, overrides)
    val displayTime = arrival.displayTimeMillis

    val formattedTime = if (displayTime > 0L) formatDateTime(DateTime(displayTime), context) else ""
    val minutesAway = if (displayTime > 0L) minutesUntil(displayTime) else 0L

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource),
        shape = appCardShape(),
        colors = CardDefaults.cardColors(
            containerColor = lerp(
                MaterialTheme.colorScheme.surfaceContainerLow,
                color,
                0.10f
            )
        ),
        border = appCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = appCardShape(),
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = transitIconResource(type)),
                        contentDescription = stringResource(transitTypeLabel(type)),
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = arrival.shortSign,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = lineDetours.isNotEmpty(),
                enter = fadeIn(m3EffectsDefault()) + scaleIn(initialScale = 0.6f, animationSpec = m3SpatialDefault()),
                exit = fadeOut(m3EffectsFast()) + scaleOut(targetScale = 0.6f, animationSpec = m3SpatialFast())
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    val alertInteractionSource = remember { MutableInteractionSource() }
                    Surface(
                        shape = appCardShape(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .requiredSizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .pressScale(alertInteractionSource)
                            .clickable(
                                interactionSource = alertInteractionSource,
                                indication = LocalIndication.current
                            ) { onShowAlerts(lineDetours) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alert_warning),
                                contentDescription = stringResource(R.string.show_alerts_for_route, arrival.routeId),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = appCardShape(),
                color = MaterialTheme.colorScheme.onSurface
            ) {
                if (arrival.isCanceled) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.arrival_cancelled),
                            color = MaterialTheme.colorScheme.surface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (arrival.reason.isNotEmpty()) {
                            Text(
                                text = arrival.reason,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else if (arrival.dropOffOnly) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.arrival_dropoff_only),
                            color = MaterialTheme.colorScheme.surface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (arrival.reason.isNotEmpty()) {
                            Text(
                                text = arrival.reason,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        CountdownLabel(
                            minutesAway = minutesAway,
                            isEstimated = arrival.isEstimated,
                            color = MaterialTheme.colorScheme.surface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val delayText = formatDelay(arrival, context)
                        if (delayText != null) {
                            Text(
                                text = delayText,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else if (!arrival.isEstimated) {
                            Text(
                                text = stringResource(R.string.scheduled),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDelay(arrival: Arrival, context: Context): String? {
    if (!arrival.isEstimated || arrival.estimatedMillis == 0L || arrival.scheduledMillis == 0L) return null
    val delayMin = (arrival.estimatedMillis - arrival.scheduledMillis) / 60000.0
    return when {
        delayMin >= 1.0 -> context.getString(R.string.arrival_delay_late, delayMin.roundToInt())
        delayMin <= -1.0 -> context.getString(R.string.arrival_delay_early, -delayMin.roundToInt())
        else -> context.getString(R.string.arrival_on_time)
    }
}