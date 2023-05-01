package de.js.app.agtracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import de.js.app.agtracker.data.TrackedPlace
import de.js.app.agtracker.domain.repository.TrackedPlaceRepository
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class TrackedPlacesListViewModel @Inject internal constructor(
    private val trackedPlaceRepository: TrackedPlaceRepository
) : ViewModel(){
    var trackedPlaces: LiveData<List<TrackedPlace>> = trackedPlaceRepository.getTrackedPlaces().asLiveData()

    fun searchForTrackedPlaces(query: String): LiveData<List<TrackedPlace>> {
        return trackedPlaceRepository.searchTrackedPlaces(query).asLiveData()
    }

    fun getTrackedPlacesFiltered(dateFrom: String, dateTo: String, name: String): LiveData<List<TrackedPlace>> {
        trackedPlaces = trackedPlaceRepository.getTrackedPlacesFiltered(dateFrom, dateTo, name).asLiveData()
        return trackedPlaces
    }
}