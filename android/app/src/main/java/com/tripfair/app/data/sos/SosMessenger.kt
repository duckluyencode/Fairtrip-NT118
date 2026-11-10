package com.tripfair.app.data.sos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.tripfair.app.data.places.LocationDto
import java.util.Locale

/** What happened to an SOS message. */
enum class SosOutcome {
    /** Handed to the phone's SMS service for every recipient. */
    Sent,

    /** The SMS app was opened with the message ready; the user still has to press send. */
    OpenedSmsApp,

    /** No way to send from this device (no SMS permission and no SMS app). */
    Failed,
}

/** Link that opens the position in any maps app or browser. */
fun mapsLink(location: LocationDto): String =
    String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", location.lat, location.lng)

/**
 * Sends the SOS text. SMS goes over the phone network, so it works without internet.
 * With the SEND_SMS permission the message is sent directly; without it the SMS app is
 * opened with the recipients and the text filled in.
 */
class SosMessenger(context: Context) {
    private val appContext = context.applicationContext

    fun canSendDirectly(): Boolean =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    /** True when every message was handed to the SMS service. Long texts are split by the system. */
    fun sendDirectly(numbers: List<String>, message: String): Boolean {
        if (numbers.isEmpty() || !canSendDirectly()) return false
        return try {
            val sms = smsManager()
            val parts = sms.divideMessage(message)
            numbers.forEach { number -> sms.sendMultipartTextMessage(number, null, parts, null, null) }
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /** Opens the SMS app on a message to [numbers]. */
    fun smsAppIntent(numbers: List<String>, message: String): Intent =
        Intent(Intent.ACTION_SENDTO, "smsto:${numbers.joinToString(";")}".toUri())
            .putExtra("sms_body", message)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @Suppress("DEPRECATION") // SmsManager.getDefault() is the only way before Android 12.
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
}
