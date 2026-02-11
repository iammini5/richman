package com.legendsoftware.richmangoogleplaybillinglibrarytest

import android.app.Application
import com.legendsoftware.richmangoogleplaybillinglibrarytest.prefs.SecureCoinManager

class RichmanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureCoinManager.init(this)
    }
}