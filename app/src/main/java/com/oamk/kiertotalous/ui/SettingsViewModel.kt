package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.model.UserRole

class SettingsViewModel(private val app: AppController,
                        private val localDataRepository: LocalDataRepository) : ViewModel() {
    val userData = ObservableField<String>()
    val palletTareWeight = ObservableField<String>()
    val trolleyTareWeight = ObservableField<String>()
    val isTareSettingEnabled = ObservableField<Boolean>()

    fun loadSettings() {
        localDataRepository.userAccount?.toJson()?.let { userAccount ->
            userData.set(userAccount)
        }

        palletTareWeight.set(localDataRepository.palletTareWeight.toString())
        trolleyTareWeight.set(localDataRepository.trolleyTareWeight.toString())
        isTareSettingEnabled.set(localDataRepository.userAccount?.userRole() == UserRole.STORE)
    }

    fun saveTareWeights(palletTareWeight: Float, trolleyTareWeight: Float) {
        localDataRepository.palletTareWeight = palletTareWeight
        localDataRepository.trolleyTareWeight = trolleyTareWeight
    }

    fun logout() {
        app.logout()
    }
}