package com.trimettransit.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.R
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import kotlinx.coroutines.launch

@Composable
fun StopListItem(
    stop: Stop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zoomOnTap: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val zoom = remember { Animatable(1f) }
    Card(
        onClick = {
            if (zoomOnTap && !zoom.isRunning) {
                scope.launch {
                    zoom.animateTo(1.08f, m3SpatialDefault())
                    onClick()
                    zoom.animateTo(1f, m3SpatialFast())
                }
            } else {
                onClick()
            }
        },
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(interactionSource)
            .graphicsLayer {
                if (zoomOnTap) {
                    scaleX = zoom.value
                    scaleY = zoom.value
                }
            },
        shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = appCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorScheme = MaterialTheme.colorScheme
            val transitTypeColor = remember(stop.transitType, colorScheme) {
                transitColor(stop.transitType, colorScheme)
            }
            val transitGlyphColor = remember(stop.transitType, colorScheme) {
                transitOnColor(stop.transitType, colorScheme)
            }
            // Transit type indicator
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
                color = transitTypeColor
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when {
                        stop.routeNum > 0 -> Text(
                            text = stop.routeNum.toString(),
                            color = transitGlyphColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        !stop.transitType.isNullOrBlank() -> Icon(
                            painter = painterResource(id = transitIconResource(stop.transitType)),
                            contentDescription = stringResource(transitTypeLabel(stop.transitType)),
                            tint = transitGlyphColor,
                            modifier = Modifier.size(24.dp)
                        )
                        else -> Text(
                            text = stop.locId.toString(),
                            color = transitGlyphColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.desc,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (stop.dirDesc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stop.dirDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.common_stop_number, stop.locId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
