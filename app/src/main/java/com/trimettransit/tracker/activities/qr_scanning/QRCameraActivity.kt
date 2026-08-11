package com.trimettransit.tracker.activities.qr_scanning

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.activities.MainActivity
import com.trimettransit.tracker.feature.qr.QRScannerCameraScreen
import com.trimettransit.tracker.ui.theme.TriMetGoTheme

class QRCameraActivity : ComponentActivity() {

    private var cameraGranted by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (!granted) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> {
                cameraGranted = true
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        setContent {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val dynamicColor = remember { prefs.getBoolean("pref_key_dynamic_color", true) }
            TriMetGoTheme(dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QRScannerCameraScreen(
                        cameraEnabled = cameraGranted,
                        onQrDetected = { qrUri ->
                            val intent = Intent(this@QRCameraActivity, MainActivity::class.java).apply {
                                putExtra("qr_uri", qrUri)
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                            finish()
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
