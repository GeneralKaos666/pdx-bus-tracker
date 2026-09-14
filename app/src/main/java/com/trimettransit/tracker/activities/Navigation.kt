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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

/** Back arrow used by every non-top-level top app bar. */
@Composable
internal fun BackNavigationIcon(onClick: () -> Unit) {
    val backSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = backSource,
        modifier = Modifier.pressScale(backSource)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}