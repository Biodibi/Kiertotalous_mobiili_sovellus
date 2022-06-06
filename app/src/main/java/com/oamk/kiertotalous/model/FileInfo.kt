package com.oamk.kiertotalous.model

import android.net.Uri
import com.google.firebase.storage.StorageMetadata
import java.io.File

data class FileInfo(
    var fileName: String,
    var uriString: String,
    var contentUriString: String,
    var contentType: String,
    var extension: String,
) {
    val uri: Uri
        get() = Uri.parse(uriString)

    val contentUri: Uri
        get() = Uri.parse(contentUriString)

    val file: File
        get() = File(uriString)

    val metadata: StorageMetadata
        get() = StorageMetadata.Builder().apply { contentType = contentType }.build()
}
