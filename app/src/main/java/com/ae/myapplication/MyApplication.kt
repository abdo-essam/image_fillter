package com.ae.myapplication


import android.app.Application
import com.ae.movies.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import com.ae.network.di.networkModule

class MyApplication() : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MyApplication)
            modules(
                networkModule,
                viewModelModule
            )
        }
    }
}