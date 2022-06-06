package com.oamk.kiertotalous.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.oamk.kiertotalous.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class DeviceRepository(private val context: Context) {
    private var deviceConnection: DeviceConnection? = null

    private val bluetoothAdapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private var aclEventsReceiver: BroadcastReceiver? = null

    fun aclEvents(): Flow<AclEvent> = callbackFlow<AclEvent> {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }

        aclEventsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, _intent: Intent?) {
                _intent?.let { intent ->
                    val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                    val event = AclEvent(intent, device)
                    trySend(event)
                }
            }
        }

        context.registerReceiver(aclEventsReceiver, filter)

        awaitClose {
            aclEventsReceiver?.let { receiver ->
                context.unregisterReceiver(receiver)
                aclEventsReceiver = null
            }
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun connectDevice(): Flow<DeviceEvent<Measurement>> = channelFlow<DeviceEvent<Measurement>> {
        bluetoothAdapter?.bondedDevices?.firstOrNull { it.name.startsWith(BLUETOOTH_DEVICE_NAME_IDENTIFIER) }?.let { bluetoothDevice ->
            trySend(DeviceEvent.Connecting())

            deviceConnection?.close()
            deviceConnection = DeviceConnection(bluetoothDevice, bluetoothAdapter!!)
            deviceConnection?.bluetoothSocket?.let {
                trySend(DeviceEvent.Connected())

                deviceConnection?.readMeasurements()?.cancellable()?.collect { measurement ->
                    if (measurement.length == 42) {
                        // Validate result
                        measurement.substring(10, 15).toFloatOrNull()?.let { result ->
                            if (result >= 0F) {
                                trySend(DeviceEvent.WeightResult(result))
                            }
                        }
                    } else if (measurement == "TEST") {
                        // Send random result if we are running diagnostics
                        trySend(DeviceEvent.WeightResult((120..300).random().toFloat()))
                    }
                } ?: run {
                    deviceConnection?.close()
                    deviceConnection = null

                    val error = ErrorEvent(ErrorEventType.SOCKET_ERROR)
                    trySend(DeviceEvent.Error(error))
                }
            } ?: run {
                deviceConnection?.close()
                deviceConnection = null

                val error = ErrorEvent(ErrorEventType.CONNECT_ERROR)
                trySend(DeviceEvent.Error(error))
            }
        } ?: run {
            deviceConnection?.close()
            deviceConnection = null

            val error = ErrorEvent(ErrorEventType.DEVICE_NOT_PAIRED)
            trySend(DeviceEvent.Error(error))
        }

        awaitClose {
            deviceConnection?.close()
            deviceConnection = null
        }
    }.catch { error ->
        deviceConnection?.close()
        deviceConnection = null

        val socketError = ErrorEvent(ErrorEventType.EXCEPTION, error)
        emit(DeviceEvent.Error(socketError))
    }.flowOn(Dispatchers.IO)

    fun logout() {
        deviceConnection?.close()
        deviceConnection = null

        aclEventsReceiver?.let { receiver ->
            context.unregisterReceiver(receiver)
            aclEventsReceiver = null
        }
    }

    private fun IntRange.random() = (Math.random() * ((endInclusive + 1) - start) + start).toInt()

    companion object {
        private const val BLUETOOTH_DEVICE_NAME_IDENTIFIER = "BTDA"
    }
}