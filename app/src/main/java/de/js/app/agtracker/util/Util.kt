package de.js.app.agtracker.util

import android.content.Context
import android.provider.Settings
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
}