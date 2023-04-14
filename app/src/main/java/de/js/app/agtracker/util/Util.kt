package de.js.app.agtracker.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object Util {

    /**
     * Return current date/time in ISO8601 as expected by SQLite
     * YYYY-MM-DD HH:MM:SS
     */
    fun getNowISO8601(): String {
        val myFormat = "yyyy-MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        return sdf.format(Calendar.getInstance().time).toString()
    }

    /**
     * Get Timestamp for used in file paths
     */
    fun getTimestampPath(): String {
        val myFormat = "yyyy-MM-dd_HH_mm_ss"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        return sdf.format(Calendar.getInstance().time).toString()
    }

    /**
     * Return Android Device ID
     */
    fun getDeviceID(context: Context): String {
        return Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ANDROID_ID
        )
    }

    /**
     * Zip all files in a directory
     */
    fun zipDirectory(directory: String, zipFile: String) {
        val dir = File(directory)
        val zipOutput = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))
        val data = ByteArray(2048)

        if (!dir.isDirectory) return

        for (f in dir.listFiles()!!) {
            if (f.isFile) {
                FileInputStream(f).use { fi ->
                    BufferedInputStream(fi).use { origin ->
                        val entry = ZipEntry(f.name)
                        entry.size = f.length()
                        zipOutput.putNextEntry(entry)
                        while (true) {
                            val readBytesLen = origin.read(data)
                            if (readBytesLen == -1) {
                                break
                            }
                            zipOutput.write(data, 0, readBytesLen)
                        }
                    }
                }
            }
        }
        zipOutput.closeEntry()
        zipOutput.close()

    }

    /**
     * Check if all permissions are granted, if not request them
     */
    fun checkAndRequestPermissions(activity: Activity): Boolean {
        val context = activity.applicationContext
        val bluetooth = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
        val bluetooth2 = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        val storage =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val loc =
            ContextCompat.checkSelfPermission(context , Manifest.permission.ACCESS_COARSE_LOCATION)
        val loc2 = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (bluetooth != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH)
        }
        if (bluetooth2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (loc2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (loc != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                listPermissionsNeeded.toTypedArray(),
                1
            )
            return false
        }
        return true
    }
}