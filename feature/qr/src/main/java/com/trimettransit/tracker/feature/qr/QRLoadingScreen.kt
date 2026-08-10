package com.trimettransit.tracker.feature.qr

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.FadeInOnce
import com.trimettransit.tracker.ui.components.pressScale
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.util.ConnectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

private const val TAG = "QRLoadingScreen"

enum class QRLoadingState {
    Loading, Error, Offline
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRLoadingScreen(
    qrUri: String,
    onNavigateBack: () -> Unit,
    onStopResolved: (stopId: Int) -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(QRLoadingState.Loading) }
    var errorMessage by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(qrUri) {
        state = QRLoadingState.Loading
        try {
            if (!ConnectionUtils.isOnline(context)) {
                state = QRLoadingState.Offline
                return@LaunchedEffect
            }

            val uri = URI.create(qrUri)
            if (!SecurityUtils.isValidHttpsUri(uri) || !SecurityUtils.isAllowedQrHost(uri.host)) {
                state = QRLoadingState.Error
                errorMessage = "Invalid QR code link."
                return@LaunchedEffect
            }

            val stopId = withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(uri.toURL()).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code ${response.code}")
                    }
                    val finalUrl = response.request.url
                    if (!"https".equals(finalUrl.scheme, ignoreCase = true) ||
                        !SecurityUtils.isAllowedQrHost(finalUrl.host)
                    ) {
                        throw SecurityException("Invalid redirect host")
                    }
                    SecurityUtils.extractStopIdFromPath(finalUrl.encodedPath)
                        ?: throw IllegalStateException("Could not parse stop ID")
                }
            }

            // Validate immediately after extraction so non-numeric/oversized IDs fail early,
            // before any further network calls.
            val stopIdInt = stopId.toIntOrNull()
            if (stopIdInt == null) {
                state = QRLoadingState.Error
                errorMessage = "Invalid stop ID in QR code."
                return@LaunchedEffect
            }

            if (!SecurityUtils.hasConfiguredTrimetApiKey()) {
                state = QRLoadingState.Error
                errorMessage = "API key not configured.\nPlease check app settings."
                return@LaunchedEffect
            }

            onStopResolved(stopIdInt)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Malformed QR URI", e)
            state = QRLoadingState.Error
            errorMessage = "Invalid QR code."
        } catch (e: SecurityException) {
            Log.e(TAG, "Security check failed", e)
            state = QRLoadingState.Error
            errorMessage = "Invalid QR code."
        } catch (e: Exception) {
            Log.e(TAG, "QR loading failed", e)
            state = QRLoadingState.Error
            errorMessage = "Could not load stop.\nPlease try again."
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Loading Stop") },
                navigationIcon = {
                    val backSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onNavigateBack,
                        interactionSource = backSource,
                        modifier = Modifier.pressScale(backSource)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            FadeInOnce {
                Crossfade(
                    targetState = state,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    label = "qrState"
                ) { currentState ->
            when (currentState) {
                QRLoadingState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Finding stop...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                QRLoadingState.Error -> {
                    QrErrorColumn(
                        message = errorMessage,
                        onBack = onNavigateBack
                    )
                }

                QRLoadingState.Offline -> {
                    QrErrorColumn(
                        message = "No internet connection.\nPlease check your connection.",
                        onBack = onNavigateBack
                    )
                }
            }
            }
            }
        }
    }
}

@Composable
private fun QrErrorColumn(message: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        val goBackSource = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = onBack,
            interactionSource = goBackSource,
            modifier = Modifier.pressScale(goBackSource)
        ) {
            Text("Go Back")
        }
    }
}
