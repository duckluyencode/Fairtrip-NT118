package com.tripfair.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tripfair.app.navigation.TripFairNavDisplay
import com.tripfair.app.ui.theme.TripFairTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar icons follow the phone's light/dark setting, same as TripFairTheme.
        enableEdgeToEdge()
        setContent {
            TripFairTheme {
                TripFairNavDisplay()
            }
        }
    }
}
