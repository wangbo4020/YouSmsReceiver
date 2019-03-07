package com.example.smssend

import android.app.Application
import com.example.smssend.content.AppPreferences

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        AppPreferences.initialize(this)
    }
}