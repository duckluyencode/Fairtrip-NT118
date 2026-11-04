package com.tripfair.app

import android.app.Application
import com.tripfair.app.data.AppContainer

/** Holds the [AppContainer] so it lives as long as the app process. */
class TripFairApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
