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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.google.android.gms.maps.model.LatLng
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.util.BluetoothUtil
import java.util.*

private const val TAG = "RtkServiceWorker"

class RtkServiceWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context, workerParams
), MyNtripClient.OnDataReceivedListener, BluetoothService.OnBluetoothEventCallback {
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
        @Throws(IndexOutOfBoundsException::class)
        fun convertGgaLocationToLatLng(
            ggaLat: String,
            ggaNS: String,
            ggaLon: String,
            ggaEW: String
        ): LatLng {

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
        }

        fun getFixTypeFromGGA(gga: String): GpsFixTypeEnum {
            val ggaSplit = gga.split(",")
            return when (ggaSplit[6].toInt()) {
                0 -> GpsFixTypeEnum.NO_FIX
                1 -> GpsFixTypeEnum.GPS
                2 -> GpsFixTypeEnum.DGPS
                3 -> GpsFixTypeEnum.PPS
                4 -> GpsFixTypeEnum.RTK
                5 -> GpsFixTypeEnum.RTK_FLOAT
                6 -> GpsFixTypeEnum.EST
                7 -> GpsFixTypeEnum.MANUAL
                8 -> GpsFixTypeEnum.SIMULATION
                9 -> GpsFixTypeEnum.WAAS
                else -> GpsFixTypeEnum.NO_FIX
            }
        }

        fun getImageForFixType(fixType: GpsFixTypeEnum): Int {
            return when (fixType) {
                GpsFixTypeEnum.NO_FIX -> R.drawable.ic_fix_none_24
                GpsFixTypeEnum.GPS -> R.drawable.ic_fix_gps_24
                GpsFixTypeEnum.DGPS -> R.drawable.ic_fix_gps_24
                GpsFixTypeEnum.PPS -> R.drawable.ic_fix_unknown_24
                GpsFixTypeEnum.RTK -> R.drawable.ic_fix_rtk_24
                GpsFixTypeEnum.RTK_FLOAT -> R.drawable.ic_fix_rtk_float_24
                GpsFixTypeEnum.EST -> R.drawable.ic_fix_unknown_24
                GpsFixTypeEnum.MANUAL -> R.drawable.ic_fix_unknown_24
                GpsFixTypeEnum.SIMULATION -> R.drawable.ic_fix_unknown_24
                GpsFixTypeEnum.WAAS -> R.drawable.ic_fix_unknown_24
            }
        }
    }


    private var lastGGA: String = ""
    private var lastGGAFixType: GpsFixTypeEnum = GpsFixTypeEnum.NO_FIX

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

            //update notification
            setForegroundAsync(foregroundInfo)
            // sleep
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
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(applicationContext.getString(R.string.rtk_notification_title))
                .setContentText(applicationContext.getString(R.string.rtk_notification_text))
                .setSmallIcon(getImageForFixType(lastGGAFixType))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
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
        //Log.d(TAG, "Data received from BluetoothDevice: buffer = $buffer, length = $length")
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
            //Log.d(TAG, "Store GGA for NTRIP to progress: $lastGGA")
            try {
                val latLng = convertGgaLocationToLatLng(
                    messageParts[2],
                    messageParts[3],
                    messageParts[4],
                    messageParts[5]
                )
                Log.i(TAG, "latLng: ${latLng.latitude}, ${latLng.longitude}")
                setProgressAsync(
                    workDataOf(
                        "gga" to lastGGA,
                        "latitude" to latLng.latitude,
                        "longitude" to latLng.longitude,
                        "numSatellites" to messageParts[7].toInt(),
                        "sentence" to sentence
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error converting GGA to LatLng: ${e.message}")
            }

            // extract FixType
            Log.d(TAG, "FixType: $fixType")
            lastGGAFixType = getFixTypeFromGGA(message)


        }
        // GST - we use it for accuracy
        if (sentence.length >= 6 && sentence.substring(3, 6) == "GST") {
            Log.d(TAG, "New GST from Bluetooth Device = $message")
            val accuracy = messageParts[2].toFloat()
            Log.d(TAG, "accuracy: $accuracy")
            setProgressAsync(
                workDataOf(
                    "accuracy" to accuracy,
                    "sentence" to sentence
                )
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

enum class GpsFixTypeEnum(val value: Int) {
    NO_FIX(0),  // Invalid, no position available
    GPS(1), // Autonomous GPS fix, no correction data used.
    DGPS(2), //DGPS fix, using a local DGPS base station or correction service such as WAAS or EGNOS.
    PPS(3), // PPS fix, I’ve never seen this used.
    RTK(4), // RTK fix, high accuracy Real Time Kinematic.
    RTK_FLOAT(5), // RTK Float, better than DGPS, but not quite RTK.
    EST(6), //Estimated fix (dead reckoning).
    MANUAL(7), // Manual input mode.
    SIMULATION(8), // Simulation mode.
    WAAS(9) //WAAS fix (not NMEA standard, but NovAtel receivers report this instead of a 2).
}