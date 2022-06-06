package com.oamk.kiertotalous.data

import android.content.Context
import android.content.SharedPreferences
import com.oamk.kiertotalous.model.Site
import com.oamk.kiertotalous.model.Summary
import com.oamk.kiertotalous.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalDataRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

    private val userAccountMutableStateFlow = MutableStateFlow<UserAccount?>(null)
    val userAccountStateFlow = userAccountMutableStateFlow.asStateFlow()

    private val jsonBuilder = Json {
        ignoreUnknownKeys = true
    }

    var userAccount: UserAccount?
        get() = prefs.getString(KEY_USER_ACCOUNT, null)?.let {
            try {
                jsonBuilder.decodeFromString<UserAccount>(it)
            } catch (exception: Throwable) {
                return null
            }
        }
        set(value) {
            prefs.edit().putString(KEY_USER_ACCOUNT, jsonBuilder.encodeToString(value)).apply()

            if (userAccountMutableStateFlow.value != userAccount) {
                userAccountMutableStateFlow.value = userAccount
            }
        }

    var previousSummary: Summary?
        get() = prefs.getString(KEY_PREVIOUS_SUMMARY, null)?.let {
            try {
                jsonBuilder.decodeFromString<Summary>(it)
            } catch (exception: Throwable) {
                return null
            }
        }
        set(value) {
            prefs.edit().putString(KEY_PREVIOUS_SUMMARY, jsonBuilder.encodeToString(value)).apply()
        }
    
    var sites: List<Site>?
        get() = prefs.getString(KEY_SITES, null)?.let {
            try {
                jsonBuilder.decodeFromString<List<Site>>(it)
            } catch (exception: Throwable) {
                return null
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SITES, jsonBuilder.encodeToString(value)).apply()
        }

    var orderedSiteIds: List<String>?
        get() = prefs.getString(KEY_ORDERED_SITES, null)?.let {
            try {
                jsonBuilder.decodeFromString<List<String>>(it)
            } catch (exception: Throwable) {
                return null
            }
        }
        set(value) {
            prefs.edit().putString(KEY_ORDERED_SITES, jsonBuilder.encodeToString(value)).apply()
        }

    var palletTareWeight: Float
        get() = prefs.getFloat(KEY_PALLET_TARE_WEIGHT, 0F)

        set(value) {
            prefs.edit().putFloat(KEY_PALLET_TARE_WEIGHT, value).apply()
        }

    var trolleyTareWeight: Float
        get() = prefs.getFloat(KEY_TROLLEY_TARE_WEIGHT, 0F)

        set(value) {
            prefs.edit().putFloat(KEY_TROLLEY_TARE_WEIGHT, value).apply()
        }

    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val APP_PREFERENCES = "app_preferences"

        private const val KEY_USER_ACCOUNT = "KEY_USER_ACCOUNT"
        private const val KEY_SITES = "KEY_SITES"
        private const val KEY_ORDERED_SITES = "KEY_ORDERED_SITES"
        private const val KEY_PREVIOUS_SUMMARY = "KEY_PREVIOUS_SUMMARY"
        private const val KEY_PALLET_TARE_WEIGHT = "KEY_PALLET_TARE_WEIGHT"
        private const val KEY_TROLLEY_TARE_WEIGHT = "KEY_TROLLEY_TARE_WEIGHT"
    }
}