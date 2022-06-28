package de.js.app.agtracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentNavigationBinding
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.CompassUtil
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.fragment_navigation.view.*
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
const val ARG_PLACE_ID = "PLACE_ID"
const val ARG_PLACE_LATLONG = "PLACE_LATLONG"

/**
 * A simple [Fragment] subclass.
 * Use the [NavigationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NavigationFragment : Fragment(), MainActivityNav.LocationUpdateListener, OnMapReadyCallback {
    private var mLocation: Location? = null
    private var mCurrentLocation: LatLng? = null
    private var mPlaceDetails: TrackedPlaceModel? = null
    private lateinit var mTarget: LatLng
    private var mHeadingTargetNorth: Double = 0.0
    private var mHeadingToTarget: Float = 0f
    private var mCurrentAzimuth: Float = 0f
    private lateinit var mCompassUtil: CompassUtil
    private lateinit var mGoogleMap: GoogleMap
    private var mLineToTarget: Polyline? = null
    private var _binding: FragmentNavigationBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // TODO: Rename and change types of parameters
    private var mPlaceID: Int = 0
    private var mPlaceLatLong: DoubleArray? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mPlaceID = it.getInt(ARG_PLACE_ID)
            mPlaceLatLong = it.getDoubleArray(ARG_PLACE_LATLONG)
        }

        if (mPlaceID != 0) {
            // place ID is given, read Target from DB
            mPlaceDetails = (activity as MainActivityNav).dbHandler!!.getPlace(mPlaceID)
            mTarget = LatLng(mPlaceDetails!!.latitude, mPlaceDetails!!.longitude)
        } else if (mPlaceLatLong != null) {
            // Lat/Long is given, navigate directly to the coordinates, no Place in DB available
            mTarget = LatLng(mPlaceLatLong!![0], mPlaceLatLong!![1])
        }

        //Setup Compass
        setupCompass()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Map
        val supportMapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        return root
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_navigation, container, false)
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivityNav).registerForLocationUpdates(this)
        mCompassUtil.start()
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivityNav).unregisterForLocationUpdates(this)
        mCompassUtil.stop()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NavigationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(place_id: Int?, place_latlong: DoubleArray?) =
            NavigationFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PLACE_ID, place_id ?: 0)
                    putDoubleArray(ARG_PLACE_LATLONG, place_latlong)
                }
            }
    }

    override fun onLocationUpdate(location: Location, isGoodQuality: Boolean) {

        mCurrentLocation = LatLng(location.latitude, location.longitude)
        mLocation = location

        // distance
        val dist = SphericalUtil.computeDistanceBetween(mCurrentLocation, mTarget)

        // UI
        binding.tvDistanceArrow.text = String.format("%.1f", dist) + "m"
        binding.tvAccuracy.text = String.format("%.3f", location.accuracy) + "m"

        //directions
        mHeadingTargetNorth = SphericalUtil.computeHeading(mCurrentLocation, mTarget)

        // rotate arrow
        rotateArrow()

        // update map
        updateMap(location)

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

        binding.tvHeadingTarget.text = String.format("%.0f", mHeadingToTarget) + "°"

        animation.duration = 500
        animation.repeatCount = 0
        animation.fillAfter = true
        binding.ivDirectionArrow.startAnimation(animation)
    }

    private fun updateMap(currentLocation: Location) {

        // only if map is already loaded
        if (!this::mGoogleMap.isInitialized) return

        // Draw line to target
        mLineToTarget?.remove()
        val position = LatLng(currentLocation.latitude, currentLocation.longitude)
        var polyOptions = PolylineOptions().add(position).add(mTarget)
        polyOptions.pattern(listOf(Dot())).color(Color.RED).width(5f)
        mLineToTarget = mGoogleMap?.addPolyline(polyOptions)

        //focus on line
        val bounds = LatLngBounds.builder().include(position).include(mTarget).build()
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))

    }

    private fun setupCompass() {
        mCompassUtil = CompassUtil(requireContext())
        val cl: CompassUtil.CompassListener = getCompassListener()
        mCompassUtil.setListener(cl)
    }

    private fun getCompassListener(): CompassUtil.CompassListener {
        val compassListener = object : CompassUtil.CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
            if(mLocation==null) return
            val magneticField = GeomagneticField(
               mLocation!!.latitude.toFloat(),
                mLocation!!.longitude.toFloat(),
                mLocation!!.altitude.toFloat(),
                Calendar.getInstance().time.time
            )
                mCurrentAzimuth = azimuth + magneticField.declination
                rotateArrow()
            }
        }
        return compassListener
    }

    override fun onMapReady(googleMap: GoogleMap) {

        googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(
                this.javaClass.simpleName,
                "Unexpected Error! Permission for Location Access is missing"
            )
            throw Throwable("Unexpected Error! Permission for Location Access is missing")
        }
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(true)


        // add target postion
        googleMap.addMarker(MarkerOptions().position(mTarget))

        // Zoom in; zoom level goes from 2 to 21 - we zoom in to the most detail
        //googleMap.setMinZoomPreference(18f)
        //googleMap.setMaxZoomPreference(20f)
        val defaultZoom = CameraUpdateFactory.zoomTo(20f)
        googleMap.animateCamera(defaultZoom)

        // Draw target location boundary (if we navigate to a Place)
        if (mPlaceDetails != null) {
            val polyOptions = getPolygonBoundaryForTarget(mPlaceDetails!!.id)
            if (polyOptions.points.isNotEmpty()) {
                var poly = googleMap.addPolygon(polyOptions)
            }
        }

        mGoogleMap = googleMap
    }

    private fun getPolygonBoundaryForTarget(id: Int): PolygonOptions {
        val dbHandler = (activity as MainActivityNav).dbHandler
        val wkt = dbHandler!!.getConvexHullAsWKT(id)

        val fillColor = Color.argb(128, 255, 0, 0)
        var polygon = PolygonOptions().strokeColor(Color.RED).fillColor(fillColor).strokeWidth(5f)

        val pointList = wkt.replace("POLYGON", "").replace("(", "").replace(")", "")
        val coords = pointList.split(",")
        for (coord in coords) {
            try {
                val lat = coord.split(" ")[1].toDouble()
                val long = coord.split(" ")[0].toDouble()
                polygon.add(LatLng(lat, long))
            } catch (e: IndexOutOfBoundsException) {
                Log.e(
                    NavigationFragment.javaClass.simpleName,
                    "Error getting Coords from convex hull",
                    e
                )
            }

        }
        return polygon
    }
}