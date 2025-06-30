package com.abhyanshchannelac.smartcamera

import android.app.Application
import timber.log.Timber

class SmartCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
