package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tripfair.app.R

/** Someone who can receive the SOS text. */
data class EmergencyContact(
    val id: String,
    val name: String,
    val relation: String,
    /** Display format, e.g. "090 512 8846". */
    val phone: String,
    val receivesSos: Boolean,
) {
    /** Up to two initials, e.g. "MĐ" for "Minh Đức"; one-word names give one letter. */
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.let { words ->
            if (words.size >= 3) words.last().take(1) else words.joinToString("") { it.take(1) }
        }.uppercase()
}

/** Public emergency number on the SOS screen. */
data class Hotline(val number: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)

object SampleSos {
    const val GPS_ACCURACY = "± 8 m"
    const val ADDRESS = "Bãi biển Mỹ Khê, Đà Nẵng"
    const val COORDINATES = "16.0544° N, 108.2469° E"
    const val TRIP_NOTE = "Duy Đức đang đi cùng nhóm 4 người trong chuyến Đà Nẵng 3 ngày."

    val relations = listOf("Gia đình", "Bạn bè", "Đồng nghiệp", "Khác")

    val contacts = listOf(
        EmergencyContact("mom", "Mẹ", "Gia đình", "090 512 8846", receivesSos = true),
        EmergencyContact("minh-duc", "Minh Đức", "Bạn thân", "091 347 2280", receivesSos = true),
        EmergencyContact("dung", "Ngô Văn Dũng", "Anh họ", "098 622 1035", receivesSos = false),
    )

    val hotlines = listOf(
        Hotline("113", R.string.sos_police, R.drawable.ic_shield),
        Hotline("115", R.string.sos_ambulance, R.drawable.ic_ambulance),
        Hotline("114", R.string.sos_fire, R.drawable.ic_flame),
    )
}
