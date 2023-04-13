package de.js.app.agtracker.location

import android.content.Context
import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.maps.LocationSource
import androidx.work.WorkManager

private const val TAG = "RtkLocationSource"

/**
 * A [LocationSource] that uses the [RtkServiceWorker] to get the current location.
 */
class RtkLocationSource(context: Context, lifecycleOwner: LifecycleOwner) : LocationSource,
    DefaultLifecycleObserver {

    private val listeners = mutableListOf<LocationSource.OnLocationChangedListener>()
    private var paused = false
    var lastLocation: Location

    init {
        lastLocation = Location("RTKService").apply {
            latitude = 47.85439864833333
            longitude = 11.591493566666665
            altitude = 0.0
            accuracy = 0f
        }
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(RtkServiceWorker.UNIQUE_WORK_ID)
            .observe(lifecycleOwner) { workInfoList ->
                if (workInfoList != null && workInfoList.size > 0) {
                    val workInfo = workInfoList[0]
                    if (workInfo != null && !workInfo.state.isFinished) {
                        val lat = workInfo.progress.getDouble("latitude", lastLocation.latitude)
                        val lon = workInfo.progress.getDouble("longitude", lastLocation.longitude)
                        val alt = workInfo.progress.getDouble("altitude", lastLocation.altitude)
                        val accuracy = workInfo.progress.getFloat("accuracy", lastLocation.accuracy)
                        val location = Location("RTKService")
                        location.latitude = lat
                        location.longitude = lon
                        location.altitude = alt
                        location.accuracy = accuracy
                        this.lastLocation = location
                        if (!paused) {
                            listeners.forEach { listener -> listener.onLocationChanged(location) }
                        }

                    }
                }
            }
    }

    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        listeners.add(listener)
    }

    override fun deactivate() {
        listeners.clear()
    }


    override fun onResume(owner: LifecycleOwner) {
        paused = false
    }

    override fun onPause(owner: LifecycleOwner) {
        paused = true
    }
}