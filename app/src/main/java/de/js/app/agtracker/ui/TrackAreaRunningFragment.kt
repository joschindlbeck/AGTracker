package de.js.app.agtracker.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentTrackAreaRunningBinding
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.Util

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TrackAreaRunningFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TrackAreaRunningFragment : Fragment(), MainActivityNav.LocationUpdateListener,
    OnMapReadyCallback {
    private var place_id: Long = 0
    private lateinit var mPolylineOptions: PolylineOptions
    private var mLine: Polyline? = null
    private var mStartLocation: Location? = null
    private var mLastTrackedLocation: Location? = null
    private var mCurrentLocation: Location? = null
    private var mTrackedLocations: ArrayList<Location> = ArrayList()

    // TODO: Rename and change types of parameters
    private var tracking_id: String? = null
    private var param2: String? = null

    private lateinit var mGoogleMap: GoogleMap

    //View binding
    private var _binding: FragmentTrackAreaRunningBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tracking_id = it.getString(TrackAreaRunningFragment.ARG_TRACKING_ID)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrackAreaRunningBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.tvTrackAreaRunningForId.text = tracking_id
        binding.btnTrackingStop.setOnClickListener { onStopTracking(it) }

        //Map
        val supportMapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        return root
    }


    private fun onStopTracking(view: View?) {
        // just a convenience button, logic is in onPause()
        findNavController().navigateUp() //navigate back
    }

    override fun onResume() {
        super.onResume()
        //Log
        Log.d(this.javaClass.simpleName, "onResume")
        // register to Location updates
        (activity as MainActivityNav).registerForLocationUpdates(this)
    }

    override fun onPause() {
        super.onPause()
        //Log
        Log.d(this.javaClass.simpleName, "onPause")

        //if we are recording an area/multiple point, set lat/long to the centroid


        // deregister from Location updates
        (activity as MainActivityNav).unregisterForLocationUpdates(this)
    }

    companion object {
        const val ARG_TRACKING_ID = "tracking_id"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TrackAreaRunningFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TrackAreaRunningFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRACKING_ID, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onLocationUpdate(location: Location, isGoodQuality: Boolean) {

        // update LatLong in Display
        _binding?.tvCurLat?.text = String.format("%.10f", location.latitude)
        _binding?.tvCurLong?.text = String.format("%.10f", location.longitude)
        _binding?.tvCurAccuracy?.text = String.format("%.3f", location.accuracy)
        _binding?.tvCurLat2?.text = String.format("%.10f", location.latitude)
        _binding?.tvCurLong2?.text = String.format("%.10f", location.longitude)

        if (isGoodQuality) {

            mCurrentLocation = location
            //set new point
            setNewPointForArea(location)
            binding.ivGpsQualityIcon.setColorFilter(Color.GREEN)

        } else {
            // do not use the location
            binding.ivGpsQualityIcon.setColorFilter(Color.RED)
        }
    }

    private fun setNewPointForArea(location: Location) {
        if (mLastTrackedLocation == null) {
            // first time call, set last location
            mLastTrackedLocation = location
            mStartLocation = location
        }

        var results: FloatArray = FloatArray(3)
        Location.distanceBetween(
            mLastTrackedLocation!!.latitude,
            mLastTrackedLocation!!.longitude,
            location.latitude,
            location.longitude,
            results
        )

        Log.d(this.javaClass.simpleName, "Distance between:" + results[0].toString())

        if (results[0] > 0.25) {
            // set point if distance is exceeded TODO: make customizable
            mTrackedLocations.add(location)
            if (mLine == null) {
                //first time
                mPolylineOptions = PolylineOptions()
                mPolylineOptions.add(
                    LatLng(
                        mStartLocation!!.latitude,
                        mStartLocation!!.longitude
                    )
                )
                mPolylineOptions.color(Color.argb(255, 255, 0, 0))

                createTrackedPlace(location)

            }

            mPolylineOptions.add(LatLng(location.latitude, location.longitude))
            mLine?.remove()
            mLine = mGoogleMap.addPolyline(mPolylineOptions)


            if (mLine!!.points.size > 1) {
                // add additional points also on db
                addPointsToTrackedPlace(mLine!!.points)
            }

            //focus on line
            val bounds = LatLngBounds.builder()
            for (point in mLine!!.points) {
                bounds.include(point)
            }
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 50))

            mLastTrackedLocation = location //update last location where we set a point
        }
    }

    private fun addPointsToTrackedPlace(points: List<LatLng>) {
        val mainActivity = activity as MainActivityNav
        mainActivity.dbHandler?.addPointsToTrackedPlace(place_id, points)
    }

    private fun createTrackedPlace(location: Location) {
        // save to DB, create record
        val mainActivity = activity as MainActivityNav
        val trackedPlaceModel = TrackedPlaceModel(
            0,
            tracking_id ?: "",
            location.latitude,
            location.longitude,
            Util.getNowISO8601(),
            1,
            "",
            Util.getDeviceID(requireContext()),
            ""
        )
        place_id = mainActivity.dbHandler?.addTrackedPlace(trackedPlaceModel) ?: 0
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        if (googleMap == null) {
            Log.e(TrackAreaRunningFragment.javaClass.simpleName, "Error getting google maps")
            return
        }
        //googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(true)

        val position = LatLng(mCurrentLocation?.latitude ?: 0.0, mCurrentLocation?.longitude ?: 0.0)
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
        googleMap.animateCamera(newLatLngZoom)

        mGoogleMap = googleMap


    }

}