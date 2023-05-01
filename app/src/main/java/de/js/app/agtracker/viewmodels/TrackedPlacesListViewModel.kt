package de.js.app.agtracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import de.js.app.agtracker.data.TrackedPlace
import de.js.app.agtracker.domain.repository.TrackedPlaceRepository
import javax.inject.Inject

@HiltViewModel
class TrackedPlacesListViewModel @Inject internal constructor(
    private val trackedPlaceRepository: TrackedPlaceRepository
) : ViewModel(){
    val trackedPlaces: LiveData<List<TrackedPlace>> = trackedPlaceRepository.getTrackedPlaces().asLiveData()

    fun searchForTrackedPlaces(query: String): LiveData<List<TrackedPlace>> {
        return trackedPlaceRepository.searchTrackedPlaces(query).asLiveData()
    }
}