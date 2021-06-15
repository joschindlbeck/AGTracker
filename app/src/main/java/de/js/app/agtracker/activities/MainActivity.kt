package de.js.app.agtracker.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import de.js.app.agtracker.R
import de.js.app.agtracker.adapter.PlacesAdapter
import de.js.app.agtracker.database.SpatialiteHandler
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.KMLUtil
import de.js.app.agtracker.util.SwipeToDeleteCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_place.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var dbHandler: SpatialiteHandler? = null
    private val requestingLocationUpdates: Boolean = true
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    private var cal = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // display always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                                    //tvCurLat.text = location!!.latitude.toString()
                                    //tvCurLong.text = location!!.longitude.toString()
                                    startLocationUpdates()
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
                    // Update UI with location data
                    mLatitude = location!!.latitude
                    mLongitude = location!!.longitude
                    tvCurLat.text = mLatitude.toString()
                    tvCurLong.text = mLongitude.toString()
                }
            }
        }
        // DB
        dbHandler = SpatialiteHandler()
        dbHandler?.init(this)

        //Load data from DB
        getPlacesFromLocalDB()
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
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
        //open db?
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        //close db?
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback)
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

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getPlacesFromLocalDB() {
        //val dbHandler = DatabaseHandler(this)
        //val dbHandler = SpatialiteHandler()
        //dbHandler.init(this)
        val placeList = dbHandler!!.getPlaceList()
        if (placeList.size > 0) {
            rvPlaces.visibility = View.VISIBLE
            tv_no_places_found.visibility = View.GONE
            setupPlacesRecyclerView(placeList)
            for (i in placeList) {
                Log.e("Name", i.name)
            }
        } else {
            rvPlaces.visibility = View.GONE
            tv_no_places_found.visibility = View.VISIBLE
        }
    }

    private fun setupPlacesRecyclerView(placeList: ArrayList<TrackedPlaceModel>) {
        rvPlaces.layoutManager = LinearLayoutManager(this)
        rvPlaces.setHasFixedSize(true)
        val placesAdapter = PlacesAdapter(this, placeList)
        rvPlaces.adapter = placesAdapter

        placesAdapter.setOnClickListener(object :
            PlacesAdapter.OnClickListener {
            override fun onClick(position: Int, model: TrackedPlaceModel) {
                val intent = Intent(this@MainActivity, MapActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // TODO (Schritt 6: Adapter Funktion aufrufen, wenn der Nutzer ein Item in die richtige Richtung wischt)
                // START
                val adapter = rvPlaces.adapter as PlacesAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                getPlacesFromLocalDB() // Holt aktualisierte Liste aus der Datenbank, nachdem Element gelöscht wurde.
                // ENDE
            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(rvPlaces)
    }


    fun onTrackButtonClicked(view: View) {
        var text = ""
        if (view is Button) {
            text = view.text as String
        }

        Toast.makeText(this, text, Toast.LENGTH_LONG).show()

        val myFormat = "dd.MM.yyyy HH:mm:ss"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())

        // save to DB
        val trackedPlaceModel = TrackedPlaceModel(
            0,
            text,
            mLatitude,
            mLongitude,
            sdf.format(Calendar.getInstance().time).toString(),
            1,
            "Test",
            ""
        )


        /*
        val dbHandler = DatabaseHandler(this)
        val addedPlace = dbHandler.addPlace(trackedPlaceModel)
        if (addedPlace > 0) {
            //Toast.makeText(this, "Erfolgreich gespeichert", Toast.LENGTH_LONG).show()
            vibrate()
            // reload from db
            getPlacesFromLocalDB()
        }*/
        //val dbHandler = SpatialiteHandler()
        //dbHandler.init(this)
        if (dbHandler!!.addTrackedPlace(trackedPlaceModel) > 0) {
            //Toast.makeText(this, "Erfolgreich gespeichert", Toast.LENGTH_LONG).show()
            vibrate()
            // reload from db
            getPlacesFromLocalDB()
        }


    }

    private fun vibrate() {
        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(500)
        }
    }

    fun onMapButtonClicked(view: View) {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)

    }

    companion object {
        internal const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }

    fun onSaveButtonClicked(view: View) {
        var kmlUtil: KMLUtil = KMLUtil()
        var myExternalFile: File = File(getExternalFilesDir("AGTracker"), "TrackedPlace.kml")
        /**
        var kml = kmlUtil.createKML(this.dbHandler!!)
        var ok = kmlUtil.writeKMLToFile(myExternalFile, kml)
        if (ok) {
        Toast.makeText(
        this,
        "Speichern erfolgreich",
        Toast.LENGTH_LONG
        ).show()
        } else {
        Toast.makeText(
        this,
                "Fehler beim speichern",
                Toast.LENGTH_LONG
            ).show()
        }
         */
    }
}