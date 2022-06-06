package com.oamk.kiertotalous.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.model.AppNotification
import com.oamk.kiertotalous.model.NavInfo

class MainViewModel(private val app: AppController,
                    private val localDataRepository: LocalDataRepository
) : ViewModel() {
    val navInfoLiveData: LiveData<NavInfo?> = app.navInfoStateFlow.asLiveData()
    val appNotificationLiveData: LiveData<AppNotification?> = app.appNotificationStateFlow.asLiveData()
    val userAccount
        get() = localDataRepository.userAccount
}