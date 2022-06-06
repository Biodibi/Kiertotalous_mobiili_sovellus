package com.oamk.kiertotalous.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

@SuppressLint("MissingPermission")
class DeviceConnection(bluetoothDevice: BluetoothDevice, val bluetoothAdapter: BluetoothAdapter) {
    private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bufferedReader: BufferedReader? = null

    var bluetoothSocket: BluetoothSocket? = null

    init {
        try {
            bluetoothDevice.createRfcommSocketToServiceRecord(BLUETOOTH_SPP)?.let { socket ->
                socket.connect()
                bluetoothSocket = socket
            }
        } catch (exception: Throwable) {
            Timber.e(exception)
        }
    }

    fun readMeasurements(): Flow<String> = channelFlow {
        bufferedReader = BufferedReader(InputStreamReader(bluetoothSocket?.inputStream))
        bufferedReader?.use { reader ->
            var previousLine: String? = null
            // Each measurement is terminated with '0D0A' which can be readLine function
            var line = reader.readLine()
            while (line != null) {
                if (line != previousLine || line == "TEST") {
                    trySend(line)
                }
                previousLine = line
                line = reader.readLine()
            }
        }
    }

    /**
     * Close the streams and socket connection.
     */
    fun close() {
        try {
            bluetoothSocket?.close()
            bufferedReader?.close()
            bufferedReader = null
        } catch (exception: Throwable) {
            Timber.e(exception)
        }
    }
}