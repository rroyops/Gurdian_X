package com.example

import android.app.Application
import com.example.core.di.ServiceLocator

class GuardianXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
        ServiceLocator.logger.i("GuardianXApp", "GuardianX platform initialized successfully")
    }
}
