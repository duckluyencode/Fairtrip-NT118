package com.tripfair.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.tripfair.app.data.places.LocationDto
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Where the phone is, for searching nearby and for SOS. */
interface LocationProvider {
    /** True once the user allowed approximate or precise location. */
    fun hasPermission(): Boolean

    /**
     * The current position, or null without permission or when the phone cannot tell
     * (location turned off, no fix in time). Never asks for permission itself.
     */
    suspend fun current(): LocationDto?
}

/** Uses Google Play services: the last known fix when there is one, else a fresh one. */
class FusedLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    override fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }

    // Typing in the search box asks again for every suggestion: the answer (a position, or
    // none) is reused for a short while instead of waiting for a fix each time.
    private var answer: LocationDto? = null
    private var answeredAt = 0L

    override suspend fun current(): LocationDto? {
        if (!hasPermission()) return null
        val now = SystemClock.elapsedRealtime()
        if (answeredAt != 0L && now - answeredAt < (if (answer != null) REUSE_FIX_MS else RETRY_AFTER_MS)) return answer
        answer = locate()
        answeredAt = SystemClock.elapsedRealtime()
        return answer
    }

    @SuppressLint("MissingPermission") // checked by hasPermission()
    private suspend fun locate(): LocationDto? {
        client.lastLocation.awaitOrNull()?.let { return LocationDto(it.latitude, it.longitude) }
        val cancel = CancellationTokenSource()
        val fresh = withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).awaitOrNull()
        }
        if (fresh == null) cancel.cancel()
        return fresh?.let { LocationDto(it.latitude, it.longitude) }
    }

    private companion object {
        const val FRESH_FIX_TIMEOUT_MS = 4_000L
        const val REUSE_FIX_MS = 60_000L
        const val RETRY_AFTER_MS = 15_000L
    }
}

/** The task's result, or null when it fails (e.g. location is turned off). */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resume(null) }
    addOnCanceledListener { continuation.resume(null) }
}
