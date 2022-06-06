package com.oamk.kiertotalous.extensions

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.oamk.kiertotalous.BuildConfig
import com.oamk.kiertotalous.model.FileInfo
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun Context.createTempFile(prefix: String, suffix: String?): FileInfo? {
    var fileInfo: FileInfo? = null
    try {
        val file = File.createTempFile(prefix, suffix)
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
        fileInfo = FileInfo(
            fileName = file.name,
            uriString = Uri.fromFile(file).path!!,
            contentUriString = FileProvider.getUriForFile(this, BuildConfig.FILE_PROVIDER, file).toString(),
            contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "",
            extension = extension
        )
        file.deleteOnExit()
    } catch (exception: Throwable) {
        Timber.e(exception, "Error occurred while creating temp file")
    }
    return fileInfo
}

fun Context.createTempImageFile(): FileInfo? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    return createTempFile(timeStamp, ".webp")
}

fun Activity.hideKeyboard() {
    try {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = currentFocus
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    } catch (exception: Throwable) {
        Timber.e(exception)
    }
}
