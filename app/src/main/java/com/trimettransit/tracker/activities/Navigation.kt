package com.trimettransit.tracker.activities

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.trimettransit.tracker.R
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import kotlinx.serialization.Serializable

internal val AnimatedContentTransitionScope<*>.navEnter: EnterTransition
    get() = if (AppMotion.reduceMotion) {
        fadeIn(initialAlpha = 0.7f, animationSpec = m3EffectsDefault())
    } else {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = m3SpatialDefault()
        ) + fadeIn(
            initialAlpha = 0.7f,
            animationSpec = m3EffectsDefault()
        )
    }

internal val AnimatedContentTransitionScope<*>.navExit: ExitTransition
    get() = if (AppMotion.reduceMotion) {
        fadeOut(targetAlpha = 0.7f, animationSpec = m3EffectsFast())
    } else {
        slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = m3SpatialFast()
        ) + fadeOut(
            targetAlpha = 0.7f,
            animationSpec = m3EffectsFast()
        )
    }

internal val AnimatedContentTransitionScope<*>.navPopEnter: EnterTransition
    get() = if (AppMotion.reduceMotion) {
        fadeIn(initialAlpha = 0.7f, animationSpec = m3EffectsDefault())
    } else {
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = m3SpatialDefault()
        ) + fadeIn(
            initialAlpha = 0.7f,
            animationSpec = m3EffectsDefault()
        )
    }

internal val AnimatedContentTransitionScope<*>.navPopExit: ExitTransition
    get() = if (AppMotion.reduceMotion) {
        fadeOut(targetAlpha = 0.7f, animationSpec = m3EffectsFast())
    } else {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = m3SpatialFast()
        ) + fadeOut(
            targetAlpha = 0.7f,
            animationSpec = m3EffectsFast()
        )
    }

/**
 * Enter transition for the Arrivals destination: the fast spatial spring so pushing to
 * Arrivals from Home/Routes reads tighter/snappier than the default [navEnter].
 */
internal val AnimatedContentTransitionScope<*>.navEnterArrivals: EnterTransition
    get() = if (AppMotion.reduceMotion) {
        fadeIn(initialAlpha = 0.7f, animationSpec = m3EffectsFast())
    } else {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = m3SpatialFast()
        ) + fadeIn(
            initialAlpha = 0.7f,
            animationSpec = m3EffectsFast()
        )
    }

// Type-safe navigation destinations, shared by the NavHost registration and every navigate()/popBackStack().
@Serializable
object HomeDestination

@Serializable
object SettingsDestination

@Serializable
object NearbyStopsDestination

@Serializable
data class ArrivalsDestination(
    val stopId: Int,
    val stopName: String = "",
    val routeId: Int = -1,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * Bundle saver for the expanded-pane detail selection (primitives only). Typed on the
 * nullable destination because this BOM's rememberSaveable(stateSaver=…) wants
 * Saver<T, Any> with T = the state's full (nullable) type. null saves as null.
 */
internal val arrivalsDestinationSaver: Saver<ArrivalsDestination?, Any> = Saver(
    save = { dest ->
        dest?.let { listOf(it.stopId, it.stopName, it.routeId, it.lat, it.lng) }
    },
    restore = { value ->
        @Suppress("UNCHECKED_CAST")
        (value as? List<Any>)?.let { list ->
            ArrivalsDestination(
                stopId = list[0] as Int,
                stopName = list[1] as String,
                routeId = list[2] as Int,
                lat = list[3] as Double,
                lng = list[4] as Double
            )
        }
    }
)

/** Base path for the pdxbus:// deep link into Arrivals (query params optional via route defaults). */
internal const val ARRIVALS_DEEP_LINK_BASE = "pdxbus://arrivals"

/** Back arrow used by every non-top-level top app bar. */
@Composable
internal fun BackNavigationIcon(onClick: () -> Unit) {
    val backSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = backSource,
        modifier = Modifier.size(48.dp).pressScale(backSource)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}