package com.trimettransit.tracker.feature.stops

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.trimettransit.tracker.util.ConnectionUtils

/**
 * True when a fetch came back empty-handed on a device with no usable network, as opposed to a
 * server-side failure. Callers use it to say which of the two happened, since only one of them is
 * worth offering a retry for.
 */
internal fun <T> isOfflineFailure(result: T?, context: Context): Boolean =
    result == null && !ConnectionUtils.isOnline(context)

/**
 * Invokes [onRegained] when the device goes from having no usable network to having one.
 *
 * The lines browser hides its retry button while offline, because tapping it cannot work. This is
 * what keeps that from stranding anyone: a screen that has nothing to show can reload itself the
 * moment connectivity returns, without the user having to leave and come back.
 *
 * The callback is invoked by the platform immediately after registration for the network that is
 * already present; that is deliberately not treated as a transition.
 */
@Composable
internal fun OnNetworkRegained(onRegained: () -> Unit) {
    val context = LocalContext.current
    val current = rememberUpdatedState(onRegained)
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@DisposableEffect onDispose { }
        var wasOnline = ConnectionUtils.isOnline(context)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = record()

            // The platform reports a network as available before it has finished validating it, and
            // "online" here requires a validated network. So the transition this exists for arrives
            // as a capability change, not as onAvailable.
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = record()

            override fun onLost(network: Network) {
                wasOnline = ConnectionUtils.isOnline(context)
            }

            private fun record() {
                val online = ConnectionUtils.isOnline(context)
                val regained = online && !wasOnline
                wasOnline = online
                if (regained) current.value()
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
}
