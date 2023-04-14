package de.js.app.agtracker.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import com.google.android.gms.location.*

/**
 * This class is responsible for providing location updates.
 */
class LocationRepository() : LocationListener {

    private lateinit var listener: AgTrackerLocationUpdateListener
    private var lastLocation: Location = Location("RTKService").apply {
        latitude = 47.85439864833333; longitude = 11.591493566666665; altitude = 0.0; accuracy = 0f
    }

    /**
     * Register for location updates from the FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun registerForLocationUpdatesFromFusedLocationProvider(
        activity: AppCompatActivity,
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
            }
        }
    }

    fun registerForLocationUpdatesFromRtkServiceWorker(
        activity: AppCompatActivity,
        listener: AgTrackerLocationUpdateListener
    ) {
        // set the listener
        this.listener = listener
        // register for updates from the RtkServiceWorker
        WorkManager.getInstance(activity.applicationContext)
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
                        this.lastLocation = location

                        // report to listener
                        listener.onLocationUpdate(location, isQualityGood(location))
                    }

                }
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

}