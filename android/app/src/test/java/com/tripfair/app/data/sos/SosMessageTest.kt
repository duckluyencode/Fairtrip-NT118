package com.tripfair.app.data.sos

import com.tripfair.app.data.places.LocationDto
import com.tripfair.app.ui.screens.sos.coordinatesText
import org.junit.Assert.assertEquals
import org.junit.Test

class SosMessageTest {
    @Test
    fun `the maps link and the coordinates use dots whatever the phone's language`() {
        val myKhe = LocationDto(16.05441, 108.24691)

        assertEquals("https://maps.google.com/?q=16.05441,108.24691", mapsLink(myKhe))
        assertEquals("16.0544° N, 108.2469° E", coordinatesText(myKhe))
        assertEquals("33.8688° S, 122.0840° W", coordinatesText(LocationDto(-33.8688, -122.084)))
    }
}
