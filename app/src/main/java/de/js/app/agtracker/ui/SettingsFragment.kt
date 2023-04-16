package de.js.app.agtracker.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.js.app.agtracker.R

// Settings keys
const val SETTINGS_GPS_FILTER_ON = "gps_filter_on"
const val SETTINGS_GPS_MIN_ACCURACY = "gps_min_accuracy"
const val SETTINGS_BLUETOOTH_DEVICE = "bluetooth_device"


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }


    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mGpsMinAccuracyPref =
            preferenceManager.findPreference<Preference>(SETTINGS_GPS_MIN_ACCURACY) as EditTextPreference
        mGpsMinAccuracyPref.onPreferenceChangeListener =
            object : Preference.OnPreferenceChangeListener {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    try {
                        // check if float
                        (newValue as String).toFloat()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            requireContext(),
                            "Error, entry must be a decimal number, use . as separator, e.g. 1.0",
                            Toast.LENGTH_LONG
                        ).show()
                        return false
                    }
                    return true
                }
            }

        // add bluetooth devices to list
        val bluetoothDevicePref = preferenceManager.findPreference<Preference>(
            SETTINGS_BLUETOOTH_DEVICE
        ) as ListPreference
        val mapDevices = mutableMapOf<String, String>()
        BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.forEach {
            mapDevices[it.name] = it.name
        }
        bluetoothDevicePref.entries = mapDevices.values.toTypedArray()
        bluetoothDevicePref.entryValues = mapDevices.keys.toTypedArray()


        return super.onCreateView(inflater, container, savedInstanceState)
    }


}