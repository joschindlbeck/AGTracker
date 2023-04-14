package de.js.app.agtracker.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.google.android.gms.maps.model.LatLng
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.util.BluetoothUtil
import java.util.*

private const val TAG = "RtkServiceWorker"

class RtkServiceWorker (context: Context, workerParams: WorkerParameters) : Worker(
    context, workerParams
), MyNtripClient.OnDataReceivedListener,BluetoothService.OnBluetoothEventCallback {
    companion object {
        const val UNIQUE_WORK_ID = "RtkServiceWorker"
        const val NOTIFICATION_CHANNEL_ID = "RtkServiceWorker"
        const val NOTIFICATION_CHANNEL_NAME = "RTK Service"
        const val NOTIFICATION_ID = 1
        const val INPUT_DATA_NTRIP_DELAY = "ntripDelay"
        const val INPUT_DATA_DEVICE_NAME = "deviceName"
        const val BLUETOOTH_CONFIG_DEVICE_NAME = "RtkDevice"

        /**
         * Convert GGA Location to LatLng
         */
        fun convertGgaLocationToLatLng(ggaLat: String, ggaNS: String,ggaLon: String, ggaEW: String): LatLng {

            try {
                //Latitude
                var latString = ggaLat.substring(0, 2) + ":" + ggaLat.substring(2) //[+-]DD:MM.MMMMM
                if (ggaNS == "S") {
                    latString = "-$latString"
                }
                val lat = Location.convert(latString)

                //Longitude
                var lonString = ggaLon.substring(0, 3) + ":" + ggaLon.substring(3) //[+-]DDD:MM.MMMMM
                if (ggaEW == "W") {
                    lonString = "-$lonString"
                }
                val lon = Location.convert(lonString)
                return LatLng(lat, lon)
            }catch (IndexOutOfBoundsException: Exception){
                Log.e(TAG,  "Error converting location to decimals")
                return LatLng(0.0, 0.0)
            }
        }

    }

    private var lastGGA: String = ""

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        Log.d(TAG, "doWork() called")

        // connect to bluetooth device
        if (!connectBluetoothDevice()) {
            return Result.failure()
        }

        // get Input Data
        val ntripDelay = inputData.getLong(INPUT_DATA_NTRIP_DELAY, 5000)

        // set work as foreground
        setForegroundAsync(foregroundInfo)

        // Show notification
        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, getNotification())
        }

        // start ntrip client
        val params = inputData.keyValueMap
        val ntripClient = MyNtripClient(params)
        ntripClient.setOnDataReceivedListener(this)

        //loop until stopped
        while (!isStopped) {
            // send last GGA to ntrip client
            if (lastGGA.isNotEmpty()) {
                ntripClient.setGGA(lastGGA)
                ntripClient.call()
            }
            Thread.sleep(ntripDelay)
        }

        // we result in success
        return Result.success()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getNotification(): Notification {
        val intent = Intent(applicationContext, MainActivityNav::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, 0)


        val notification =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).setContentTitle(applicationContext.getString(R.string.rtk_notification_title))
                .setContentText(applicationContext.getString(R.string.rtk_notification_text))
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent) //TODO: change icon
                .setPriority(NotificationCompat.PRIORITY_LOW).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        return notification
    }

    override fun getForegroundInfo(): ForegroundInfo {

        return ForegroundInfo(NOTIFICATION_ID, getNotification())
    }

    override fun onStopped() {
        super.onStopped()
        Log.d(TAG, "onStopped() called")
        val service = BluetoothService.getDefaultInstance()
        service.disconnect()
    }

    private fun initBluetoothWithLibrary() {
        BluetoothUtil.initBluetoothWithLibrary(applicationContext)
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetoothDevice(): Boolean {
        // get device name from inputData
        val deviceName = inputData.getString(INPUT_DATA_DEVICE_NAME)
        val bAdapter = BluetoothAdapter.getDefaultAdapter()

        val bDevice = bAdapter.bondedDevices.find { it.name == deviceName }
        if (bDevice == null) {
            Log.e(TAG, "Device $deviceName not found")
            return false
        }
        // init bluetooth
        initBluetoothWithLibrary()
        // connect to device
        val service = BluetoothService.getDefaultInstance()
        // register for callbacks
        service.setOnEventCallback(this)
        // connect
        service.connect(bDevice)

        return true
    }
    override fun onDataRead(buffer: ByteArray?, length: Int) {
        Log.d(TAG, "Data received from BluetoothDevice: buffer = $buffer, length = $length")
        val message = String(buffer!!, 0, length)
        val messageParts = message.split(",")
        // get sentence
        val sentence = messageParts[0]
        //Log.d(TAG, "onDataRead() called with: sentence = $sentence")
        workDataOf("sentence" to sentence)
        //setProgressAsync(lastSentence)
        // check GGA
        if (sentence.length >= 6 && sentence.substring(3, 6) == "GGA") {
            Log.d(TAG, "New GGA from Bluetooth Device = $message")
            lastGGA = message
            val fixType = messageParts[6].toInt()
            setProgressAsync(workDataOf("fixType" to fixType))
            Log.d(TAG, "Store GGA for NTRIP to progress: $lastGGA")
            val latLng = convertGgaLocationToLatLng(messageParts[2], messageParts[3], messageParts[4], messageParts[5])
            Log.i(TAG, "latLng: ${latLng.latitude}, ${latLng.longitude}")
            setProgressAsync(
                workDataOf("gga" to lastGGA,
                    "latitude" to latLng.latitude,
                    "longitude" to latLng.longitude,
                    "numSatellites" to messageParts[7].toInt(),
                    "sentence" to sentence)
            )


        }
        // GST - we use it for accuracy
        if (sentence.length >= 6 && sentence.substring(3, 6) == "GST") {
            Log.d(TAG, "New GST from Bluetooth Device = $message")
            val accuracy = messageParts[2].toFloat()
            Log.d(TAG, "accuracy: $accuracy")
            setProgressAsync(
                workDataOf("accuracy" to accuracy,
                    "sentence" to sentence)
            )
        }
    }

    override fun onStatusChange(state: BluetoothStatus) {
        Log.d(TAG, "onStatusChange() called with: state = $state")
    }

    override fun onDeviceName(deviceName: String?) {
        Log.d(TAG, "onDeviceName() called with: deviceName = $deviceName")
    }

    override fun onToast(message: String?) {
        Log.d(TAG, "onToast() called with: message = $message")
    }

    override fun onDataWrite(buffer: ByteArray?) {
        Log.d(TAG, "onDataWrite() called with: buffer = $buffer")
    }


    override fun onDataReceived(data: ByteArray) {
        Log.d(TAG, "onDataReceived() called with: data = $data")
        val btService = BluetoothService.getDefaultInstance()
        btService.write(data)
    }

}