package com.something15525.trimetgo.trimet_go.ui

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.something15525.trimetgo.trimet_go.R
import com.something15525.trimetgo.trimet_go.data.model.Stop
import com.something15525.trimetgo.trimet_go.util.ArrivalUtils
import com.something15525.trimetgo.trimet_go.ui.screens.DrawerActions
import com.something15525.trimetgo.trimet_go.ui.screens.DrawerContent
import com.something15525.trimetgo.trimet_go.ui.screens.home.HomeScreen
import com.something15525.trimetgo.trimet_go.ui.screens.stops.StopsScreen
import com.something15525.trimetgo.trimet_go.ui.theme.TriMetGoTheme

object ComposeBridge {
    fun setupHomeFragment(composeView: ComposeView, fragment: Fragment) {
        composeView.setContent {
            TriMetGoTheme {
                HomeScreen(
                    onNavigateToArrivals = { stop ->
                        fragment.startActivity(ArrivalUtils.a(fragment.requireActivity(), stop, true, -1))
                    }
                )
            }
        }
    }

    fun setupStopsFragment(composeView: ComposeView, fragment: Fragment) {
        composeView.setContent {
            TriMetGoTheme {
                StopsScreen(
                    onNavigateToArrivals = { stop, routeId ->
                        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(fragment.requireContext())
                        val onlySelectedRoute = prefs.getBoolean("pref_key_only_show_route_selected", true)
                        fragment.startActivity(ArrivalUtils.a(fragment.requireActivity(), stop, true, if (onlySelectedRoute) routeId else -1))
                    }
                )
            }
        }
    }

    fun setupDrawerContent(composeView: ComposeView, onCloseDrawer: Runnable? = null) {
        composeView.setContent {
            val ctx = LocalContext.current
            val activity = ctx as? android.app.Activity
            val navController = activity?.let {
                try { Navigation.findNavController(it, R.id.nav_host_fragment) } catch (_: Exception) { null }
            }

            TriMetGoTheme {
                DrawerContent(
                    actions = DrawerActions(
                        onHomeClick = {
                            navController?.navigate(R.id.home_fragment)
                            onCloseDrawer?.run()
                        },
                        onRoutesClick = {
                            navController?.navigate(R.id.stops_fragment)
                            onCloseDrawer?.run()
                        },
                        onSettings = {
                            activity?.startActivity(android.content.Intent(activity, com.something15525.trimetgo.trimet_go.activities.SettingsActivity::class.java))
                            onCloseDrawer?.run()
                        }
                    )
                )
            }
        }
    }
}
