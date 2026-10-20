package com.tripfair.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.tripfair.app.ui.theme.ThemeSampleScreen
import com.tripfair.app.ui.theme.TripFairTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar icons follow the phone's light/dark setting, same as TripFairTheme.
        enableEdgeToEdge()
        setContent {
            TripFairTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ThemeSampleScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
