package com.oamk.kiertotalous.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.oamk.kiertotalous.BuildConfig
import com.oamk.kiertotalous.data.DeviceRepository
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.data.RemoteDataRepository
import com.oamk.kiertotalous.ui.*
import org.koin.dsl.module

val appModule = module {
    single { AppController(get(), get(), get(), get(), get(), get()) }
}

val viewModelModule = module {
    single { MainViewModel(get(), get()) }
    single { LoginViewModel(get(), get(), get()) }
    single { DeliveryFormViewModel(get(), get(), get(), get()) }
    single { DeliveriesViewModel(get(), get(), get()) }
    single { SummaryViewModel(get(), get()) }
    single { SettingsViewModel(get(), get()) }
}

val localDataModule = module {
    single { LocalDataRepository(get()) }
}

val deviceDataModule = module {
    single { DeviceRepository(get()) }
}

val remoteDataModule = module {
    single { FirebaseAuth.getInstance() }
    single {
        FirebaseCrashlytics.getInstance().apply {
            // Enable crashes collection in release builds
            if (BuildConfig.DEBUG) {
                setCrashlyticsCollectionEnabled(false)
            }
        }
    }
    single { FirebaseStorage.getInstance() }
    single { FirebaseMessaging.getInstance() }
    single {
        // Enable for Firestore debugging
        /* if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true)
        } */
        FirebaseFirestore.getInstance()?.apply {
            // Disable caching
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        }
    }
    single { FirebaseInstallations.getInstance() }
    single { RemoteDataRepository(get(), get(), get(), get(), get()) }
    single { ImageGlideModule() }
}