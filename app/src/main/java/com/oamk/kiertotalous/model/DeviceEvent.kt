package com.oamk.kiertotalous.model

import android.bluetooth.BluetoothDevice
import android.content.Intent

enum class ErrorEventType {
    BLUETOOTH_DISABLED,
    DEVICE_NOT_PAIRED,
    DEVICE_DISCONNECTED,
    CONNECT_ERROR,
    SOCKET_ERROR,
    EXCEPTION
}

data class ErrorEvent(var errorEventType: ErrorEventType, var error: Throwable? = null)

data class AclEvent(val intent: Intent, val bluetoothDevice: BluetoothDevice?)

sealed class DeviceEvent<T> {
    class Connecting<T> : DeviceEvent<T>()
    class Connected<T> : DeviceEvent<T>()
    data class WeightResult<T>(val result: Float) : DeviceEvent<T>()
    data class Error<T>(val errorEvent: ErrorEvent) : DeviceEvent<T>()
}