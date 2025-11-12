package com.nervesparks.iris.app

import android.app.Application
import com.nervesparks.iris.common.logging.IrisLogger
import com.nervesparks.iris.common.logging.LogLevel
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class with Hilt integration
 */
@HiltAndroidApp
class IrisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
    }

    private fun initializeLogging() {
        // Set log level based on debug build
        IrisLogger.setLogLevel(LogLevel.INFO)
        IrisLogger.info("Iris application initialized")
    }
}
