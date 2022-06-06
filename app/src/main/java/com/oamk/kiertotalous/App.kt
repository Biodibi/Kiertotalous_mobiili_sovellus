package com.oamk.kiertotalous

import android.app.Application
import android.content.Intent
import com.oamk.kiertotalous.di.*
import com.oamk.kiertotalous.ui.MainActivity
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.logger.Level

class App : Application(), KoinComponent {
    private val koinModules by lazy {
        listOf(appModule, viewModelModule, localDataModule, remoteDataModule, deviceDataModule)
    }

    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(koinModules)
        }
    }

    fun restart() {
        unloadKoinModules(koinModules)
        loadKoinModules(koinModules)

        val intent = Intent(this@App, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}