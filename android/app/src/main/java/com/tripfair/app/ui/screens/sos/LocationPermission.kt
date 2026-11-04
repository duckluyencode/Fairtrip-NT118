package com.tripfair.app.ui.screens.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Whether SOS may read the location, and [request] to open the system permission dialog. */
class LocationPermissionState(val granted: Boolean, val request: () -> Unit)

/** Approximate location is enough to count as granted; SOS still asks for precise location. */
@Composable
fun rememberLocationPermission(): LocationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        granted = results.values.any { it }
    }
    return LocationPermissionState(granted = granted, request = { launcher.launch(LocationPermissions) })
}

private fun Context.hasLocationPermission(): Boolean =
    LocationPermissions.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
