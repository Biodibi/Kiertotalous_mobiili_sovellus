package com.oamk.kiertotalous.model

sealed class CustomActivityResult<out T : Any> {
    data class OK<out T : Any>(val result: T, val requestCode: Int? = null) : CustomActivityResult<T>()
    data class Cancelled(val result: Any?, val requestCode: Int? = null) : CustomActivityResult<Nothing>()
    data class Error(val error: Throwable, val requestCode: Int? = null) : CustomActivityResult<Nothing>()
}