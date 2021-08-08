package de.js.app.agtracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.js.app.agtracker.R

// Settings keys
const val SETTINGS_GPS_FILTER_ON = "gps_filter_on"
const val SETTINGS_GPS_MIN_ACCURACY = "gps_min_accuracy"

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var mGpsMinAccuracyPref =
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
        return super.onCreateView(inflater, container, savedInstanceState)
    }

}