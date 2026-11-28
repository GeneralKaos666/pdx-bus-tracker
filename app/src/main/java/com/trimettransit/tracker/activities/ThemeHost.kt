package com.trimettransit.tracker.activities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowInsetsControllerCompat
import com.trimettransit.tracker.ui.appearance.AppearanceStyle
import com.trimettransit.tracker.ui.theme.TriMetGoTheme

/**
 * Applies the in-app appearance to the Compose tree and the system bars.
 *
 * WindowCompat.enableEdgeToEdge deliberately leaves bar-icon appearance to the caller, so
 * every screen entry must mirror the in-app light/dark choice here. Kept out of
 * MainActivity so the activity composes screens instead of owning theme plumbing.
 */
@Composable
fun ThemeHost(
    isDark: Boolean,
    appearance: AppearanceStyle,
    cardCornerRadius: Dp,
    cardCutCorners: Boolean,
    cardOutlinesEnabled: Boolean,
    cardOutlineColor: Color?,
    content: @Composable () -> Unit
) {
    TriMetGoTheme(
        darkTheme = isDark,
        appearance = appearance,
        cardCornerRadius = cardCornerRadius,
        cardCutCorners = cardCutCorners,
        cardOutlinesEnabled = cardOutlinesEnabled,
        cardOutlineColor = cardOutlineColor
    ) {
        val activity = LocalActivity.current
        SideEffect {
            if (activity != null) {
                WindowInsetsControllerCompat(
                    activity.window, activity.window.decorView
                ).isAppearanceLightStatusBars = !isDark
                WindowInsetsControllerCompat(
                    activity.window, activity.window.decorView
                ).isAppearanceLightNavigationBars = !isDark
            }
        }
        content()
    }
}
