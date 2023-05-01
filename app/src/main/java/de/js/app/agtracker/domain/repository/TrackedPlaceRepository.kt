package de.js.app.agtracker.domain.repository

import com.google.android.gms.maps.model.LatLng
import de.js.app.agtracker.data.TrackedPlace

interface TrackedPlaceRepository {
    fun getTrackedPlaces(): List<TrackedPlace>
    fun getTrackedPlace(id: Long): TrackedPlace

    fun insertAll(trackedPlaces: List<TrackedPlace>)
    fun insert(trackedPlace: TrackedPlace): Long

    fun updateAll(trackedPlaces: List<TrackedPlace>)

    fun delete(trackedPlace: TrackedPlace)
    fun createTrackedPlace(name: String, deviceId: String, fieldId: Long): Long

    fun replacePointsOfTrackedPlace(trackedPlaceId: Long, points: List<LatLng>)
}