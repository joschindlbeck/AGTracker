package de.js.app.agtracker.util

import android.content.Context
import androidx.preference.PreferenceManager

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
}