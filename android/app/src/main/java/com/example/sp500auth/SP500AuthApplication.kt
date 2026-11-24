package com.example.sp500auth

import android.app.Application
import com.example.sp500auth.data.push.PushRegistrar
import com.example.sp500auth.data.push.createPushChannel
import com.example.sp500auth.data.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SP500AuthApplication : Application() {
    @Inject lateinit var push: PushRegistrar
    @Inject lateinit var auth: AuthRepository

    override fun onCreate() {
        super.onCreate()
        createPushChannel(this)
        push.start()
        // HU12_02: closing the session unlinks this phone while the account can still be identified.
        auth.beforeLogout = { push.detach() }
    }
}
