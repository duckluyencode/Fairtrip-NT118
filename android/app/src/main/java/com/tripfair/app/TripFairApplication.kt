package com.tripfair.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.tripfair.app.data.AppContainer

/** Holds the [AppContainer] so it lives as long as the app process. */
class TripFairApplication : Application(), SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader

    override fun onCreate() {
        super.onCreate()
        container.start()
    }
}
