package com.fieldcrm.android

import android.app.Application
import com.fieldcrm.android.core.notification.NotificationHelper
import com.fieldcrm.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }
        NotificationHelper.createChannel(this)
    }
}
