package com.esom.bank

import android.app.Application
import com.google.firebase.FirebaseApp
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MMKV.initialize(this)
    }
}