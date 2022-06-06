package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oamk.kiertotalous.Const
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.data.RemoteDataRepository
import com.oamk.kiertotalous.model.AppNotification
import com.oamk.kiertotalous.model.FirebaseResult
import com.oamk.kiertotalous.model.UserRole
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(private val app: AppController,
                     private val remoteDataRepository: RemoteDataRepository,
                     private val localDataRepository: LocalDataRepository
) : ViewModel() {
    var isProgressVisible = ObservableBoolean(false)

    fun loginAndSubscribeToPushNotifications(email: String, password: String) {
        viewModelScope.launch {
            isProgressVisible.set(true)
            remoteDataRepository.loginWithEmailAndPassword(email, password).cancellable().collect { loginResult ->
                when (loginResult) {
                    is FirebaseResult.Success -> {
                        when (loginResult.data.userRole()) {
                            UserRole.COURIER -> {
                                remoteDataRepository.subscribeToTopic(Const.TOPIC_DELIVERIES).cancellable().collect { subscriptionResult ->
                                    when (subscriptionResult) {
                                        is FirebaseResult.Success -> {
                                            localDataRepository.userAccount = loginResult.data
                                        }
                                        is FirebaseResult.Error -> {
                                            Timber.e(subscriptionResult.error)
                                            app.showAppNotification(AppNotification(message = R.string.error_request_failed, isError = true))
                                        }
                                    }
                                }
                            }
                            else -> localDataRepository.userAccount = loginResult.data
                        }
                    }
                    is FirebaseResult.Error -> {
                        Timber.e(loginResult.error)
                        app.showAppNotification(AppNotification(message = R.string.error_request_failed, isError = true))
                    }
                }
                isProgressVisible.set(false)
            }
        }
    }
}