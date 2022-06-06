package com.oamk.kiertotalous.ui

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.oamk.kiertotalous.model.CustomActivityResult
import com.oamk.kiertotalous.model.FileInfo

class TakePictureActivityResultLauncher(private val activityResultRegistry: ActivityResultRegistry) : DefaultLifecycleObserver {
    private lateinit var launcher: ActivityResultLauncher<Uri>
    private lateinit var fileInfo: FileInfo
    val activityResult by lazy { MutableLiveData<CustomActivityResult<FileInfo>>() }

    override fun onCreate(lifecycleOwner: LifecycleOwner) {
        try {
            launcher = activityResultRegistry.register(LAUNCHER_KEY, lifecycleOwner, ActivityResultContracts.TakePicture()) { result ->
                if (result) {
                    activityResult.value = CustomActivityResult.OK(fileInfo)
                } else {
                    activityResult.value = CustomActivityResult.Cancelled("ActivityResultContracts.TakePicture() cancelled")
                }
            }
        } catch (exception: Throwable) {
            activityResult.value = CustomActivityResult.Error(exception)
        }
    }

    fun launch(fileInfo: FileInfo) {
        this.fileInfo = fileInfo
        try {
            launcher.launch(fileInfo.contentUri)
        } catch (exception: Throwable) {
            activityResult.value = CustomActivityResult.Error(exception)
        }
    }

    companion object {
        const val LAUNCHER_KEY = "TAKE_PICTURE_LAUNCHER"
    }
}