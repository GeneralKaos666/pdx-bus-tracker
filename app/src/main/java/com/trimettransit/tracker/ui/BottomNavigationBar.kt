package com.trimettransit.tracker.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.trimettransit.tracker.R
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.appearance.onColorFor
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class BottomNavItem(
    val pageIndex: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val iconSize: Dp = 24.dp
)

internal val bottomNavItems = listOf(
    BottomNavItem(0, R.string.nav_favorites, Icons.Filled.Favorite),
    BottomNavItem(1, R.string.nav_recent, Icons.Filled.History),
    BottomNavItem(2, R.string.nav_routes, Icons.Filled.Map),
    BottomNavItem(3, R.string.nav_trips, Icons.Filled.Directions)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainBottomBar(
    topPage: Int,
    pagePosition: Float,
    onNavigate: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onContextClick: () -> Unit = {},
    contextLabelRes: Int? = null,
    contextIcon: ImageVector? = null
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val itemHeight = 40.dp
    val shouldHideLabel = fontScale > 1.25f ||
            windowInfo.containerSize.width < with(density) { 360.dp.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showBack) {
                PillActionButton(
                    onClick = onBackClick,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    shape = appCardShape(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    size = itemHeight
                )
            }
            Surface(
                shape = appCardShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                AnimatedContent(
                    targetState = contextLabelRes == null,
                    transitionSpec = {
                        val enter = if (AppMotion.reduceMotion) {
                            fadeIn(m3EffectsDefault())
                        } else {
                            fadeIn(m3EffectsDefault()) + scaleIn(initialScale = 0.95f, animationSpec = m3SpatialDefault())
                        }
                        val exit = if (AppMotion.reduceMotion) {
                            fadeOut(m3EffectsFast())
                        } else {
                            fadeOut(m3EffectsFast()) + scaleOut(targetScale = 0.95f, animationSpec = m3SpatialFast())
                        }
                        enter togetherWith exit
                    },
                    label = "nav_collapse"
                ) { isTopLevel ->
                    if (isTopLevel) {
                        MainTabRow(
                            topPage = topPage,
                            items = bottomNavItems,
                            itemHeight = itemHeight,
                            shouldHideLabel = shouldHideLabel,
                            onNavigate = onNavigate,
                            pagePosition = pagePosition
                        )
                    } else {
                        CompactContextPill(
                            contextLabelRes = contextLabelRes ?: R.string.nav_arrivals,
                            contextIcon = contextIcon,
                            onClick = onContextClick,
                            itemHeight = itemHeight
                        )
                    }
                }
            }
            PillActionButton(
                onClick = onSettingsClick,
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                shape = appCardShape(),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                size = itemHeight
            )
        }
    }
}

/** Sharable pill shape so the bottom bar consistently tracks the user's card radius. */
@Composable
internal fun appCardShape(): Shape = appCardShape()

private data class PillBounds(val x: Int, val width: Int)

@Composable
private fun PillActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    size: Dp
) {
    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        val source = remember { MutableInteractionSource() }
        IconButton(
            onClick = onClick,
            interactionSource = source,
            modifier = Modifier
                .size(size)
                .pressScale(source, 0.92f)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = contentColor)
        }
    }
}

@Composable
private fun MainTabRow(
    topPage: Int,
    items: List<BottomNavItem>,
    itemHeight: Dp,
    shouldHideLabel: Boolean,
    onNavigate: (Int) -> Unit,
    pagePosition: Float
) {
    val bounds = remember { mutableStateMapOf<Int, PillBounds>() }
    var boxLeft by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Continuous page position (currentPage + drag/fling offset fraction) drives the
    // pill so it tracks finger swipes frame-by-frame instead of snapping on settle.
    val maxIndex = items.lastIndex
    val position = pagePosition.coerceIn(0f, maxIndex.toFloat())
    val fromPage = floor(position).toInt().coerceIn(0, maxIndex)
    val toPage = ceil(position).toInt().coerceIn(0, maxIndex)
    val fraction = position - fromPage
    val fromBounds = bounds[fromPage]
    val toBounds = bounds[toPage]
    val pillTarget = when {
        fromBounds == null -> toBounds
        toBounds == null -> fromBounds
        else -> PillBounds(
            x = fromBounds.x + ((toBounds.x - fromBounds.x) * fraction).roundToInt(),
            width = fromBounds.width + ((toBounds.width - fromBounds.width) * fraction).roundToInt()
        )
    }

    val indicatorOffset = with(density) { (pillTarget?.x ?: 0).toDp() }
    val indicatorWidth = with(density) { (pillTarget?.width ?: 0).toDp() }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .onGloballyPositioned { coords ->
                boxLeft = coords.positionInWindow().x.roundToInt()
            }
    ) {
        val appearance = LocalAppearanceStyle.current
        val hasCustomPillAccent = appearance.pillAccent != null
        val pillContainer = appearance.pillAccent ?: MaterialTheme.colorScheme.surfaceContainer
        val pillContent = if (hasCustomPillAccent) {
            onColorFor(appearance.pillAccent!!)
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        if (pillTarget != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
            ) {
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(itemHeight)
                        .clip(appCardShape())
                        .background(pillContainer)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                val isSelected = topPage == item.pageIndex
                val icon = item.icon
                val labelRes = item.labelRes
                val showLabel = isSelected && !shouldHideLabel

                val labelAlpha by animateFloatAsState(
                    targetValue = if (showLabel) 1f else 0f,
                    animationSpec = m3EffectsDefault(),
                    label = "label_alpha_$index"
                )

                val itemSource = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        if (item.pageIndex != topPage) onNavigate(item.pageIndex)
                    },
                    interactionSource = itemSource,
                    modifier = Modifier
                        .width(if (showLabel) 128.dp else 48.dp)
                        .height(itemHeight)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            bounds[index] = PillBounds(
                                x = pos.x.roundToInt() - boxLeft,
                                width = coords.size.width
                            )
                        }
                        .pressScale(itemSource, 0.96f)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isSelected) {
                            pillContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(labelRes),
                            tint = if (isSelected) {
                                pillContent
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(item.iconSize)
                        )
                        if (showLabel) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                color = pillContent,
                                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Compact/Medium companion to [MainBottomBar]: a left-edge rail for wide (Expanded) layouts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainNavigationRail(
    topPage: Int,
    onNavigate: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        bottomNavItems.forEach { item ->
            NavigationRailItem(
                selected = topPage == item.pageIndex,
                onClick = { onNavigate(item.pageIndex) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.labelRes)) },
                alwaysShowLabel = false
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings)) },
            alwaysShowLabel = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactContextPill(
    contextLabelRes: Int,
    contextIcon: ImageVector?,
    onClick: () -> Unit,
    itemHeight: Dp
) {
    Surface(
        shape = appCardShape(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        val source = remember { MutableInteractionSource() }
        IconButton(
            onClick = onClick,
            interactionSource = source,
            modifier = Modifier
                .size(itemHeight)
                .pressScale(source, 0.92f)
        ) {
            Icon(
                imageVector = contextIcon ?: Icons.Default.Settings,
                contentDescription = stringResource(contextLabelRes),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}