package com.tripfair.app.data.contacts

import kotlinx.serialization.Serializable

/** Someone who can receive the SOS text, as the backend returns it. */
@Serializable
data class EmergencyContact(
    val id: Int,
    val name: String,
    val relation: String? = null,
    /** E.164, e.g. "+84905128846". */
    val phone: String,
    val receivesSos: Boolean = true,
) {
    /** Up to two initials, e.g. "MĐ" for "Minh Đức"; one-word names give one letter. */
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.let { words ->
            if (words.size >= 3) words.last().take(1) else words.joinToString("") { it.take(1) }
        }.uppercase()

    /** Vietnamese numbers the local way ("090 512 8846"); foreign ones as stored. */
    val displayPhone: String
        get() = displayPhone(phone)
}

/** What the contact form sends; the backend accepts "0905 128 846" and stores E.164. */
@Serializable
data class ContactInput(
    val name: String,
    val relation: String?,
    val phone: String,
    val receivesSos: Boolean,
)

fun displayPhone(e164: String): String {
    if (!e164.startsWith("+84")) return e164
    val local = "0" + e164.removePrefix("+84")
    return if (local.length == 10) "${local.take(3)} ${local.substring(3, 6)} ${local.substring(6)}" else local
}
