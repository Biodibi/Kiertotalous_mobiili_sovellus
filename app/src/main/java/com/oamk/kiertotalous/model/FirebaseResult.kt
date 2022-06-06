package com.oamk.kiertotalous.model

sealed class FirebaseResult<T> {
    data class Success<T>(val data: T) : FirebaseResult<T>()
    data class Error<T>(val error: Throwable) : FirebaseResult<T>()
}