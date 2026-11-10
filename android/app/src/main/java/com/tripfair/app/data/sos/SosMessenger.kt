package com.tripfair.app.data.sos

import android.content.Intent
import androidx.core.net.toUri
import com.tripfair.app.data.places.LocationDto
import java.util.Locale

/** What happened to an SOS message. */
enum class SosOutcome {
    /** The SMS app was opened with the recipients and the text ready; the user presses send. */
    OpenedSmsApp,

    /** This device has no SMS app. */
    Failed,
}

/** Link that opens the position in any maps app or browser. */
fun mapsLink(location: LocationDto): String =
    String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", location.lat, location.lng)

/**
 * Opens the phone's SMS app on a message to [numbers]. SMS goes over the phone network, so
 * it works without internet. The app does not send by itself: Android 17 refuses SmsManager
 * to apps that are not the SMS app, even with the SEND_SMS permission, and going through the
 * SMS app needs no permission at all.
 */
fun smsAppIntent(numbers: List<String>, message: String): Intent =
    Intent(Intent.ACTION_SENDTO, "smsto:${numbers.joinToString(";")}".toUri()).putExtra("sms_body", message)
