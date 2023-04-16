package de.js.app.agtracker.location

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.LocationSource
import de.js.app.agtracker.location.RtkServiceWorker.Companion.UNIQUE_WORK_ID

/**
 * This class is responsible for providing location updates.
 */
class LocationRepository() : LocationListener {

    private lateinit var listener: AgTrackerLocationUpdateListener
    private var lastLocation: Location = Location("RTKService").apply {
        latitude = 47.85439864833333; longitude = 11.591493566666665; altitude = 0.0; accuracy = 0f
    }
    private var useRtk = false

    /**
     * Register for location updates from the FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun registerForLocationUpdatesFromFusedLocationProvider(
        activity: Activity,
        listener: AgTrackerLocationUpdateListener
    ) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // build location request
                val locationRequest =
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
                        setWaitForAccurateLocation(true)
                        setGranularity(Granularity.GRANULARITY_FINE)
                        setMinUpdateDistanceMeters(0.5f)
                    }.build()
                // register myself to get location updates (-> method onLocationChanged)
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    this,
                    Looper.getMainLooper()
                )
                // set the listener
                this.listener = listener

                this.useRtk = false
            }
        }
    }

    fun registerForLocationUpdatesFromRtkServiceWorker(
        activity: AppCompatActivity,
        listener: AgTrackerLocationUpdateListener
    ) {
        this.useRtk = true
        // set the listener
        this.listener = listener
        // register for updates from the RtkServiceWorker
        WorkManager.getInstance(activity)
            .getWorkInfosForUniqueWorkLiveData(RtkServiceWorker.UNIQUE_WORK_ID)
            .observe(activity) { workInfoList ->
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
                        location.elapsedRealtimeNanos = System.nanoTime()
                        this.lastLocation = location

                        // report to listener
                        listener.onLocationUpdate(location, isQualityGood(location))
                    }

                }
            }
    }


    fun getLocationSource(
        activity: AppCompatActivity,
        lifecycleOwner: LifecycleOwner
    ): LocationSource? {
        if (useRtk) {
            val locationSource = RtkLocationSource(activity, lifecycleOwner)
            locationSource.lastLocation = this.lastLocation
            return locationSource
        } else {
            return null
        }
    }

    private fun isQualityGood(location: Location): Boolean {
        //TODO("Check quality of Location")
        return true
    }

    override fun onLocationChanged(loc: Location) {
        // notify listener
        this.listener.onLocationUpdate(loc, isQualityGood(loc))

    }

    interface AgTrackerLocationUpdateListener {
        fun onLocationUpdate(location: Location, isQualityGood: Boolean)
    }

    companion object {
        private const val TAG = "LocationRepository"
        // Singleton pattern
        private var instance: LocationRepository? = null
        fun getInstance(): LocationRepository {
            if (instance == null) {
                instance = LocationRepository()
            }
            return instance!!
        }

    }
}