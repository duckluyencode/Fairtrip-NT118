package com.tripfair.app.data.network

import okhttp3.Interceptor

/** Release builds log nothing about requests. */
internal fun debugInterceptors(): List<Interceptor> = emptyList()
