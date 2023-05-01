package de.js.app.agtracker.data

import android.util.Log
import co.anbora.labs.spatia.geometry.MultiPoint
import co.anbora.labs.spatia.geometry.Point
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks.await
import de.js.app.agtracker.domain.repository.TrackedPlaceRepository
import de.js.app.agtracker.util.Util
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackedPlaceRepositoryImpl @Inject constructor(
    private val trackedPlaceDao: TrackedPlaceDao
) : TrackedPlaceRepository {

    override fun getTrackedPlaces() = trackedPlaceDao.getTrackedPlaces()
    override fun getTrackedPlace(id: Long) = trackedPlaceDao.getTrackedPlace(id)
    override fun insertAll(trackedPlaces: List<TrackedPlace>) =
        trackedPlaceDao.insertAll(trackedPlaces)

    override fun insert(trackedPlace: TrackedPlace): Long {
        return trackedPlaceDao.insert(trackedPlace)
    }

    override fun updateAll(trackedPlaces: List<TrackedPlace>) =
        trackedPlaceDao.updateAll(trackedPlaces)

    override fun delete(trackedPlace: TrackedPlace) = trackedPlaceDao.delete(trackedPlace)

    override fun createTrackedPlace(name: String, deviceId: String, fieldId: Long): Long {
        // empty MultiPoint
        val geomMultiPoint = MultiPoint(listOf(Point(0.0, 0.0)))
        val trackedPlace =
            TrackedPlace(name, Util.getNowISO8601(), deviceId, fieldId, geomMultiPoint)
        val id = insert(trackedPlace)
        return id
    }

    override fun replacePointsOfTrackedPlace(trackedPlaceId: Long, points: List<LatLng>) {
        val pointList = mutableListOf<Point>()
        points.forEach {
            val p = Point(it.longitude, it.latitude)
            pointList.add(p)
        }

        val newGeomMultiPoint = MultiPoint(pointList)
        getTrackedPlace(trackedPlaceId).let {
            it.geomMultiPoint = newGeomMultiPoint
            updateAll(listOf(it))
        }
    }
}