package com.example

import android.app.Application
import android.util.Log
import com.example.blackbox.BlackBoxCore

class AppClonerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("AppClonerApplication", "Application onCreate: initializing BlackBox engine")
        BlackBoxCore.get().init(this)
    }
}
