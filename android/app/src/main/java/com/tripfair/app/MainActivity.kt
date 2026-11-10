package com.tripfair.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tripfair.app.navigation.TripFairNavDisplay
import com.tripfair.app.ui.theme.TripFairTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    /**
     * The app is in Vietnamese whatever the phone's language is, so system-provided texts
     * (the date picker's months and weekdays, number formats) must be Vietnamese too.
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { setLocale(Locale.forLanguageTag("vi-VN")) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar icons follow the phone's light/dark setting, same as TripFairTheme.
        enableEdgeToEdge()
        setContent {
            TripFairTheme {
                TripFairNavDisplay(container = (application as TripFairApplication).container)
            }
        }
    }
}
