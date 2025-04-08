package com.crms.crmsAndroid

import android.app.Application
import android.content.Context

class CAMS : Application() {
    companion object {
        private lateinit var instance: CAMS

        fun getAppContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}