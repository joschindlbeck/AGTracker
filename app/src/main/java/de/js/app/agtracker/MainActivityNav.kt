package de.js.app.agtracker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import de.js.app.agtracker.database.SpatialiteHandler
import de.js.app.agtracker.databinding.ActivityMainNavBinding
import de.js.app.agtracker.ui.SETTINGS_GPS_FILTER_ON
import de.js.app.agtracker.ui.SETTINGS_GPS_MIN_ACCURACY
import de.js.app.agtracker.util.KalmanLatLong
import de.js.app.agtracker.util.UncaughtExceptionHandler


class MainActivityNav : AppCompatActivity() {
    private var mPreferences: SharedPreferences? = null
    private var currentSpeed: Float = 0.0f
    private var runStartTimeInMillis: Long = 0
    private var kalmanFilter: KalmanLatLong = KalmanLatLong(0.5f)
    public lateinit var mVibrator: Vibrator
    public var dbHandler: SpatialiteHandler? = null
    public lateinit var mLocation: Location
    private val requestingLocationUpdates: Boolean = true
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainNavBinding
    private lateinit var locationCallback: LocationCallback
    public var mLatitude: Double = 0.0
    public var mLongitude: Double = 0.0

    var mLocationUpdateListeners: ArrayList<LocationUpdateListener> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set default exception handler
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtExceptionHandler(
                getExternalFilesDir(null) ?: filesDir
            )
        )

        // set display always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // get vibrations
        mVibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // set up Location
        setup_loaction_provider()

        // set up DB
        setup_db()

        //View binding & Navigation
        binding = ActivityMainNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMainActivityNav.toolbar)

        //binding.appBarMainActivityNav.fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show()
        //}
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main_activity_nav)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_track_point,
                R.id.nav_track_area,
                R.id.nav_navigation,
                R.id.nav_list_tracked_places,
                R.id.nav_export,
                R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun setup_db() {
        dbHandler = SpatialiteHandler()
        dbHandler?.init(this)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun setup_loaction_provider() {
        // get last known location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // check permissiona
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Deine Ortung ist ausgeschaltet. Bitte stelle diese an.",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    @SuppressLint("MissingPermission")
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        if (p0!!.areAllPermissionsGranted()) {
                            mFusedLocationClient.lastLocation
                                .addOnSuccessListener { location: Location? ->
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        startLocationUpdates()
                                    } else {
                                        //rare situation
                                        Log.e(
                                            this.javaClass.simpleName,
                                            "Error getting last location, null!"
                                        )
                                    }

                                }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRationaleDialogForPermissions()
                    }

                }
                ).onSameThread().check()
        }

        //register for location callbacks
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {

                    // save location data
                    mLatitude = location.latitude
                    mLongitude = location.longitude
                    mLocation = location

                    // check quality of Location
                    val isGoodQuality = checkIfValidLocation(location)

                    //inform listeners
                    for (l in mLocationUpdateListeners) {
                        l.onLocationUpdate(location, isGoodQuality)
                    }

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest: LocationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(1000) // 1 seconds

        mFusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        runStartTimeInMillis = SystemClock.elapsedRealtimeNanos() / 1000000
    }

    private fun checkIfValidLocation(location: Location): Boolean {
        if ((mPreferences?.getBoolean(SETTINGS_GPS_FILTER_ON, false) ?: false) != true) {
            return true
        }

        val age: Long = getLocationAge(location)

        if (age > 10 * 1000) { //more than 10 seconds
            Log.d(this.javaClass.simpleName, "Location is old")
            //oldLocationList.add(location)
            return false
        }

        if (location.accuracy <= 0) {
            Log.d(this.javaClass.simpleName, "Latitidue and longitude values are invalid.")
            //noAccuracyLocationList.add(location)
            return false
        }


        var minAccuracy = 1.0f
        try {
            minAccuracy =
                mPreferences?.getString(SETTINGS_GPS_MIN_ACCURACY, "1.0")?.toFloat() ?: 1.0f
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "Error reading Preference gps_min_accuracy", e)
        }
        val horizontalAccuracy = location.accuracy
        if (horizontalAccuracy > minAccuracy) { //TODO: What accuracy shall be taken? make it customizable!
            Log.d(this.javaClass.simpleName, "Accuracy is too low.")
            //inaccurateLocationList.add(location)
            return false
        }


        /* Kalman Filter */


        /* Kalman Filter */
        val Qvalue: Float

        val elapsedTimeInMillis: Long =
            (location.elapsedRealtimeNanos / 1000000) - runStartTimeInMillis


        if (currentSpeed === 0.0f) {
            Qvalue = 0.5f //3.0f //3 meters per second
        } else {
            Qvalue = currentSpeed // meters per second
        }

        kalmanFilter.Process(
            location.latitude,
            location.longitude,
            location.accuracy,
            elapsedTimeInMillis,
            Qvalue
        )
        val predictedLat: Double = kalmanFilter.get_lat()
        val predictedLng: Double = kalmanFilter.get_lng()

        val predictedLocation = Location("") //provider name is unecessary

        predictedLocation.latitude = predictedLat //your coords of course

        predictedLocation.longitude = predictedLng
        val predictedDeltaInMeters = predictedLocation.distanceTo(location)

        if (predictedDeltaInMeters > 60) {
            Log.d(
                this.javaClass.simpleName,
                "Kalman Filter detects mal GPS, we should probably remove this from track"
            )
            kalmanFilter.consecutiveRejectCount += 1
            if (kalmanFilter.consecutiveRejectCount > 3) {
                kalmanFilter =
                    KalmanLatLong(3f) //reset Kalman Filter if it rejects more than 3 times in raw.
            }
            //kalmanNGLocationList.add(location)
            return false
        } else {
            kalmanFilter.consecutiveRejectCount = 0
        }

        Log.d(this.javaClass.simpleName, "Location quality is good enough.")
        currentSpeed = location.speed

        return true

    }

    private fun getLocationAge(newLocation: Location): Long {
        val locationAge: Long
        if (Build.VERSION.SDK_INT >= 17) {
            locationAge =
                (SystemClock.elapsedRealtimeNanos() / 1000000) - (newLocation.getElapsedRealtimeNanos() / 1000000)
        } else {
            locationAge = System.currentTimeMillis() - newLocation.getTime()
        }
        return locationAge
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_activity_nav, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main_activity_nav)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this).setMessage(
            "Es liegen keine Berechtigungen vor. Diese können in den " +
                    "App-Einstellungen geändert werden."
        )
            .setPositiveButton("Zu den Einstellungen") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Abbrechen") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onResume() {
        super.onResume()
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    fun registerForLocationUpdates(listener: LocationUpdateListener) {
        mLocationUpdateListeners.add(listener)
    }

    fun unregisterForLocationUpdates(listener: LocationUpdateListener) {
        mLocationUpdateListeners.remove(listener)
    }

    public interface LocationUpdateListener {
        fun onLocationUpdate(location: Location, isQualityGood: Boolean)
    }

    override fun onBackPressed() {
        super.onBackPressed()

    }
}