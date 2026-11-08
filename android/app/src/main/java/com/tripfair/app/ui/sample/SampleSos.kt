package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tripfair.app.R
import com.tripfair.app.data.contacts.EmergencyContact

/** Public emergency number on the SOS screen. */
data class Hotline(val number: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)

object SampleSos {
    const val GPS_ACCURACY = "± 8 m"
    const val ADDRESS = "Bãi biển Mỹ Khê, Đà Nẵng"
    const val COORDINATES = "16.0544° N, 108.2469° E"
    const val TRIP_NOTE = "Duy Đức đang đi cùng nhóm 4 người trong chuyến Đà Nẵng 3 ngày."

    val relations = listOf("Gia đình", "Bạn bè", "Đồng nghiệp", "Khác")

    val contacts = listOf(
        EmergencyContact(1, "Mẹ", "Gia đình", "+84905128846", receivesSos = true),
        EmergencyContact(2, "Minh Đức", "Bạn thân", "+84913472280", receivesSos = true),
        EmergencyContact(3, "Ngô Văn Dũng", "Anh họ", "+84986221035", receivesSos = false),
    )

    val hotlines = listOf(
        Hotline("113", R.string.sos_police, R.drawable.ic_shield),
        Hotline("115", R.string.sos_ambulance, R.drawable.ic_ambulance),
        Hotline("114", R.string.sos_fire, R.drawable.ic_flame),
    )
}
