package de.js.app.agtracker.util

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Data
import de.js.app.agtracker.location.MyNtripClient.Companion.NTRIP_MOUNTPOINT
import de.js.app.agtracker.location.MyNtripClient.Companion.NTRIP_PASSWORD
import de.js.app.agtracker.location.MyNtripClient.Companion.NTRIP_PORT
import de.js.app.agtracker.location.MyNtripClient.Companion.NTRIP_SERVER
import de.js.app.agtracker.location.MyNtripClient.Companion.NTRIP_USERNAME
import de.js.app.agtracker.location.RtkServiceWorker.Companion.INPUT_DATA_DEVICE_NAME
import de.js.app.agtracker.location.RtkServiceWorker.Companion.INPUT_DATA_NTRIP_DELAY

object PreferencesUtil {
    /**
     * Get the texts for the weed buttons from the preferences
     */
    fun getButtonTextsFromPreferences(context: Context): List<String> {
        // get Texts for buttons from Settings, ids are btn1, btn2, ..., btn8
        val textList: MutableList<String> = mutableListOf()
        for (btnNo in 1..8 step 1) {
            var settingsId: String = "btn$btnNo"
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            pref.getString(settingsId, "")?.let { textList.add(it) }
        }
        return textList
    }

    fun getGpsSourceFromPreferences(context: Context): String {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getString("gps_source", "GPS")!!
    }

    fun getWorkerInputDataFromPreferences(context: Context): Data {
        val params = mutableMapOf<String,Any?>()
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        params[INPUT_DATA_DEVICE_NAME] = pref.getString("bluetooth_device", "RTK_GNSS_734")
        params[INPUT_DATA_NTRIP_DELAY] = pref.getString("ntrip_delay", "5000")?.toInt() ?: 5000
        params[NTRIP_USERNAME] = pref.getString("ntrip_username", "username")
        params[NTRIP_PASSWORD] = pref.getString("ntrip_password", "password")
        params[NTRIP_MOUNTPOINT] = pref.getString("ntrip_mountpoint", "mountpoint")
        params[NTRIP_SERVER] = pref.getString("ntrip_server", "195.200.70.200")
        params[NTRIP_PORT] = pref.getString("ntrip_port", "2101")?.toInt() ?: 2101

        return Data.Builder().putAll(params)
            .putString("device_name", "RTK_GNSS_734")
            .build()
    }
}