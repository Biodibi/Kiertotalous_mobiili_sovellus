package com.oamk.kiertotalous.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import android.os.Build
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentReference
import com.oamk.kiertotalous.Const
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.data.DeviceRepository
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.data.RemoteDataRepository
import com.oamk.kiertotalous.extensions.toUidString
import com.oamk.kiertotalous.model.*
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.destination
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

class DeliveryFormViewModel(private val app: AppController,
                            private val localDataRepository: LocalDataRepository,
                            private val remoteDataRepository: RemoteDataRepository,
                            private val deviceRepository: DeviceRepository
) : ViewModel() {
    val date = ObservableField<String>()
    val siteName = ObservableField<String>()
    val weightResult = ObservableField<String>()
    val imageItems = ObservableArrayList<ImageItemData>()
    val parcelItems = ObservableArrayList<ParcelItemData>()
    val isProgressVisible = ObservableBoolean()

    var previouslySubmittedWeight = 0F
    var previouslySubmittedWeightUnixTimestamp = 0L

    @OptIn(DelicateCoroutinesApi::class)
    private val deviceConnectionContext = newSingleThreadContext("deviceConnectionContext")
    private val deviceConnectionScope = CoroutineScope(deviceConnectionContext)
    private var deviceConnectionJob: Job? = null
    private var closeDeviceConnectionJob: Job? = null

    private val formSubmitResultMutableLiveData: MutableLiveData<FirebaseResult<DocumentReference>> = MutableLiveData()
    val formSubmitResultLiveData: LiveData<FirebaseResult<DocumentReference>> = formSubmitResultMutableLiveData

    private val deviceEventMutableLiveData: MutableLiveData<DeviceEvent<Any>> = MutableLiveData()
    val deviceEventLiveData: LiveData<DeviceEvent<Any>> = deviceEventMutableLiveData

    init {
        val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
        val time = "${app.context.getString(R.string.time)} ${ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(formatter)}"
        date.set(time)

        viewModelScope.launch {
            deviceRepository.aclEvents().cancellable().collect { event ->
                when (event.intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = event.intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

                        if (state == BluetoothAdapter.STATE_ON) {
                            Timber.d("BluetoothAdapter.STATE_ON")
                            startDeviceConnectionJob()
                        } else if (state == BluetoothAdapter.STATE_OFF) {
                            deviceConnectionJob?.cancel()

                            Timber.d("BluetoothAdapter.STATE_OFF")
                            val error = ErrorEvent(ErrorEventType.BLUETOOTH_DISABLED)
                            deviceEventMutableLiveData.postValue(DeviceEvent.Error(error))
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        Timber.d("Disconnected")
                        deviceConnectionJob?.cancel()

                        val error = ErrorEvent(ErrorEventType.DEVICE_DISCONNECTED)
                        deviceEventMutableLiveData.postValue(DeviceEvent.Error(error))
                    }
                }
            }
        }

        fetchSite()
    }

    override fun onCleared() {
        super.onCleared()

        deviceRepository.logout()
        deviceConnectionJob?.cancel()
        deviceConnectionJob = null
        closeDeviceConnectionJob?.cancel()
        closeDeviceConnectionJob = null
        deviceConnectionScope.cancel()
    }

    fun startDeviceConnectionJob(delay: Long = 100) {
        closeDeviceConnectionJob?.cancel()

        deviceConnectionScope.launch {
            if (!deviceRepository.isBluetoothEnabled) {
                val error = ErrorEvent(ErrorEventType.BLUETOOTH_DISABLED)
                deviceEventMutableLiveData.postValue(DeviceEvent.Error(error))
            } else if (deviceConnectionJob?.isActive != true) {
                deviceConnectionJob = launch {
                    delay(delay)
                    deviceRepository.connectDevice().cancellable().collect { event ->
                        when (event) {
                            is DeviceEvent.Connecting -> {
                                Timber.d("Connecting")
                            }
                            is DeviceEvent.Connected -> {
                                Timber.d("Connected")
                            }
                            is DeviceEvent.WeightResult -> {
                                weightResult.set(event.result.toString())
                            }
                            is DeviceEvent.Error -> {
                                Timber.e(event.errorEvent.error)
                                deviceConnectionJob?.cancel()
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        deviceEventMutableLiveData.postValue(event as DeviceEvent<Any>)
                    }
                }
            }
        }
    }

    fun scheduleCloseDeviceConnectionJob(delay: Long = 0) {
        deviceConnectionScope.launch {
            closeDeviceConnectionJob?.cancel()
            closeDeviceConnectionJob = launch {
                delay(delay)
                deviceConnectionJob?.cancel()
            }
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return deviceRepository.isBluetoothEnabled
    }

    private fun fetchSite() {
        viewModelScope.launch {
            localDataRepository.userAccount?.siteId?.let { siteId ->
                remoteDataRepository.fetchSite(siteId).cancellable().collect { result ->
                    when (result) {
                        is FirebaseResult.Success -> {
                            siteName.set(result.data.name)
                        }
                        is FirebaseResult.Error -> {
                            Timber.e(result.error)
                        }
                    }
                }
            }
        }
    }

    fun submitDeliveryForm(description: String) {
        var isValidWeight = true
        val parcels = mutableListOf<Parcel>()
        parcelItems.forEach { parcelItem ->
            parcelItem.parcel?.let { parcel ->
                // Subtract tare weight
                if (parcel.type == "pallet") {
                    parcel.weight -= localDataRepository.palletTareWeight
                } else if (parcel.type == "trolley") {
                    parcel.weight -= localDataRepository.trolleyTareWeight
                }
                // Validate weight
                if (parcel.weight > Const.MIN_WEIGHT) {
                    parcels.add(parcel)
                } else {
                    parcelItems.remove(parcelItem)
                    isValidWeight = false
                }
            }
        }

        if (isValidWeight) {
            isProgressVisible.set(true)
            viewModelScope.launch {
                var allFilesUploaded = true
                val fileReferences = mutableListOf<String>()
                imageItems.forEach { file ->
                    file.fileInfo?.let { fileInfo ->
                        remoteDataRepository.upload(fileInfo).cancellable().collect { result ->
                            when (result) {
                                is FirebaseResult.Success -> {
                                    Timber.d("Upload success")
                                    fileReferences.add(fileInfo.fileName)
                                    // Local file can be deleted after successful upload
                                    if (fileInfo.file.delete()) {
                                        Timber.d("Deleted file: ${fileInfo.file.absolutePath}")
                                    }
                                }
                                is FirebaseResult.Error -> {
                                    Timber.e(result.error)
                                    allFilesUploaded = false
                                }
                            }
                        }
                    }
                }

                if (allFilesUploaded) {
                    localDataRepository.userAccount?.let { userAccount ->
                        val uid = UUID.randomUUID().toUidString()
                        val userId = userAccount.userId
                        val siteId = userAccount.siteId
                        val status = DeliveryStatus.OPEN.status
                        val created = Instant.now().toString()
                        val modified = Instant.now().toString()
                        val delivery = Delivery(uid, userId, siteId, status, description, parcels, fileReferences, created, modified)

                        remoteDataRepository.submitDeliveryForm(delivery).cancellable().collect { result ->
                            formSubmitResultMutableLiveData.value = result

                            when (result) {
                                is FirebaseResult.Success -> {
                                    parcelItems.lastOrNull()?.parcel?.weight?.let { weight ->
                                        previouslySubmittedWeight = weight
                                        previouslySubmittedWeightUnixTimestamp = Instant.now().toEpochMilli()
                                    }
                                    weightResult.set("")
                                    parcelItems.clear()
                                    imageItems.clear()
                                    app.showAppNotification(AppNotification(message = R.string.send_success))
                                }
                                is FirebaseResult.Error -> {
                                    Timber.e(result.error)
                                    app.showAppNotification(AppNotification(message = R.string.error_request_failed, isError = true))
                                }
                            }
                        }
                    }
                    isProgressVisible.set(false)
                } else {
                    isProgressVisible.set(false)
                    app.showAppNotification(AppNotification(message = R.string.error_request_failed, isError = true))
                }
            }
        } else {
            app.showAppNotification(AppNotification(message = R.string.error_check_weight, isError = true))
        }
    }

    fun compressAndAddImage(imageItem: ImageItemData) {
        viewModelScope.launch {
            imageItem.fileInfo?.file?.let { file ->
                Compressor.compress(app.context, file) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        default(width = 1200, quality = 90, format = Bitmap.CompressFormat.WEBP_LOSSY)
                    } else {
                        @Suppress("DEPRECATION")
                        default(width = 1200, quality = 90, format = Bitmap.CompressFormat.WEBP)
                    }
                    destination(file)
                }
                imageItems.add(0, imageItem)
            }
        }
    }

    fun isSameWeightAsPreviouslySubmittedWeight(weightWithTareWeight: Float, parcelType: String): Boolean {
        var weight = weightWithTareWeight
        // Subtract tare weight
        if (parcelType == "pallet") {
            weight -= localDataRepository.palletTareWeight
        } else if (parcelType == "trolley") {
            weight -= localDataRepository.trolleyTareWeight
        }

        return abs(weight - previouslySubmittedWeight) <= 2 &&
                (Instant.now().toEpochMilli() - previouslySubmittedWeightUnixTimestamp) < 2 * 60000
    }

    fun navigate(navInfo: NavInfo) {
        app.navigate(navInfo)
    }
}