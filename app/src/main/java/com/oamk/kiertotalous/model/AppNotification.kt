package com.oamk.kiertotalous.model

import androidx.annotation.StringRes

data class AppNotification(
    @StringRes val title: Int? = null,
    @StringRes val message: Int,
    val duration: Int = 5000,
    val isError: Boolean = false
)