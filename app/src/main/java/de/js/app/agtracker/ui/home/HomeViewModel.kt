package de.js.app.agtracker.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Hier können bald Felder ausgewählt werden. \n" +
                "Im Moment wird alles auf Feld ID = 1 gespeichert."
    }
    val text: LiveData<String> = _text
}