package org.ifaco.mybluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*

class Connect(val bluetoothAdapter: BluetoothAdapter?, val device: BluetoothDevice? = null) :
    Thread() {
    companion object {
        const val NAME = "My Bluetooth"
    }

    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, UUID.randomUUID())
    }
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device?.createRfcommSocketToServiceRecord(UUID.randomUUID())
    }

    override fun run() {
        if (device == null) {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Main.connectHandler?.obtainMessage(0, "Socket's accept() method failed")
                        ?.sendToTarget()
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        } else {
            bluetoothAdapter?.cancelDiscovery()
            mmSocket?.use { socket ->
                try {
                    socket.connect()
                } catch (e: IOException) {// DOESN'T INDICATE THAT THE DEVICE ISN'T IN RANGE!
                    Main.connectHandler?.obtainMessage(
                        0,
                        if (e.message != null) e.message else "IOException"
                    )?.sendToTarget()
                    cancel()
                }
                manageMyConnectedSocket(socket)
            }
        }
    }

    fun cancel() {
        if (device == null) {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Main.connectHandler?.obtainMessage(0, "Could not close the connect socket")
                    ?.sendToTarget()
            }
        } else {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Main.connectHandler?.obtainMessage(0, "Could not close the client socket")
                    ?.sendToTarget()
            }
        }
    }

    fun manageMyConnectedSocket(socket: BluetoothSocket) {
    }
}