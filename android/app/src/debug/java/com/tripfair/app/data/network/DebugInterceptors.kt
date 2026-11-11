package com.tripfair.app.data.network

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/** Debug builds log the method, URL, status and timing of each request (no headers, no bodies). */
internal fun debugInterceptors(): List<Interceptor> =
    listOf(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
