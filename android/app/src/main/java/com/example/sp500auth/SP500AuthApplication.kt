package com.example.sp500auth

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SP500AuthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
