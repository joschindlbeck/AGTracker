package de.js.app.agtracker.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.google.maps.android.ktx.utils.sphericalPathLength
import de.js.app.agtracker.R
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.CompassUtil
import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mHeadingTargetNorth: Double = 0.0
    private var mHeadingToTarget: Float = 0f
    private var mLineToTarget: Polyline? = null
    private var mPlaceDetails: TrackedPlaceModel? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var mGoogleMap: GoogleMap
    private lateinit var mTarget: LatLng
    private var mCurrentAzimuth: Float = 0f
    private lateinit var mCompassUtil: CompassUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mPlaceDetails =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as TrackedPlaceModel
        }

        if (mPlaceDetails != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = mPlaceDetails!!.name
            //supportActionBar!!.setHomeButtonEnabled(true)
        }

        val supportMapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        //Location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Register for loaction updates
        //register for location callbacks
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    // Update UI with location data
                    tv_LatLong.text =
                        location?.latitude.toString() + " \n " + location.longitude.toString()

                    // draw line to target
                    mLineToTarget?.remove()

                    val position = LatLng(location.latitude, location.longitude)
                    var polyOptions = PolylineOptions().add(position).add(mTarget)
                    polyOptions.pattern(listOf(Dot())).color(R.color.red).width(20f)

                    mLineToTarget = mGoogleMap?.addPolyline(polyOptions)
                    var distance = (mLineToTarget as Polyline).sphericalPathLength
                    tv_Distance.text = distance.toString()
                    tv_distance_arrow.text = String.format("%.1f", distance)

                    //focus on line
                    val bounds = LatLngBounds.builder().include(position).include(mTarget).build()
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))

                    //directions
                    mHeadingTargetNorth = SphericalUtil.computeHeading(position, mTarget)
                    tv_headingTargetNorth.text = "${mHeadingTargetNorth}°"

                    rotateArrow()
                    //mGoogleMap?.addMarker(MarkerOptions().position(position).title("me"))
                    //mLatitude = location!!.latitude
                    //mLongitude = location!!.longitude
                    //tvCurLat.text = mLatitude.toString()
                    //tvCurLong.text = mLongitude.toString()
                }
            }
        }

        //Setup Compass
        setupCompass()
    }

    private fun setupCompass() {
        mCompassUtil = CompassUtil(this)
        val cl: CompassUtil.CompassListener = getCompassListener()
        mCompassUtil.setListener(cl)
    }

    private fun getCompassListener(): CompassUtil.CompassListener {
        val compassListener = object : CompassUtil.CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
                tv_headingHandyNorth.text = azimuth.toString()
                mCurrentAzimuth = azimuth
                rotateArrow()
            }
        }
        return compassListener
    }

    private fun rotateArrow() {
        //calculate heading to target
        var az: Float
        if (mCurrentAzimuth <= 180) {
            az = mCurrentAzimuth
        } else {
            az = (360 - mCurrentAzimuth) * (-1)
        }
        //val newHeadingToTargetRelative = mHeadingTargetNorth.toFloat() - az
        val newHeadingToTargetRelative = az - mHeadingTargetNorth.toFloat()
        var newHeadingToTarget = 0f
        if (newHeadingToTargetRelative < 0) {
            newHeadingToTarget = 360 + newHeadingToTargetRelative
        } else {
            newHeadingToTarget = newHeadingToTargetRelative
        }

        val animation = RotateAnimation(
            -mHeadingToTarget, -newHeadingToTarget, Animation.RELATIVE_TO_SELF,
            0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        mHeadingToTarget = newHeadingToTarget

        animation.duration = 500
        animation.repeatCount = 0
        animation.fillAfter = true
        iv_direction_arrow.startAnimation(animation)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed(); return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {

        googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE

        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true
        googleMap?.uiSettings?.setAllGesturesEnabled(true)


        // add postion
        val position = LatLng(mPlaceDetails!!.latitude, mPlaceDetails!!.longitude)
        googleMap!!.addMarker(MarkerOptions().position(position).title(mPlaceDetails!!.name))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
        googleMap.animateCamera(newLatLngZoom)

        mGoogleMap = googleMap
        mTarget = position

    }

    private fun startLocationUpdates() {
        try {
            val locationRequest: LocationRequest = LocationRequest()
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            locationRequest.setInterval(1000) // 1 seconds  // TODO: Remove hardcoded update time

            mFusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // No permission
            Toast.makeText(
                this,
                "Security Exception! No permissions for Location request",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        mCompassUtil.start()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        mCompassUtil.stop()
    }

}