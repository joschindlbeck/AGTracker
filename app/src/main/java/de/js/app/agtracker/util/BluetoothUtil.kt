package de.js.app.agtracker.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Looper
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import de.js.app.agtracker.location.RtkServiceWorker
import java.util.*

class BluetoothUtil {
    companion object {
        /**
         * Get the name of the paired bluetooth device
         */
        @SuppressLint("MissingPermission")
        fun getPairedDeviceName(): String {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                for (device in pairedDevices) {
                    return device.name
                }
            }
            return ""
        }


        /**
         * Init BluetoothService with library
         */
        fun initBluetoothWithLibrary(context: Context) {

            // only if not yet been done
            try {
                BluetoothService.getDefaultInstance()
                return
            } catch (e: Exception) {
                // init
                if (Looper.myLooper() == null) {
                    Looper.prepare()
                }
                //Bluetooth config
                val config = BluetoothConfiguration()
                config.context = context
                config.bluetoothServiceClass = BluetoothClassicService::class.java
                config.bufferSize = 1024
                config.characterDelimiter = '\n'
                config.deviceName = RtkServiceWorker.BLUETOOTH_CONFIG_DEVICE_NAME
                config.callListenersInMainThread = false//true
                config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // Required
                BluetoothService.init(config)
                return
            }


        }
    }
}