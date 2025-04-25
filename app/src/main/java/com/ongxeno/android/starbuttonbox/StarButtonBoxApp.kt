package com.ongxeno.android.starbuttonbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class required by Hilt.
 * Annotate with @HiltAndroidApp to enable dependency injection application-wide.
 */
@HiltAndroidApp
class StarButtonBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
