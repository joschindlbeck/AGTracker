package de.js.app.agtracker.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentTrackAreaRunningBinding

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
    private lateinit var mPolylineOptions: PolylineOptions
    private var mLine: Polyline? = null
    private var makePolygon = false
    private lateinit var mPolygonOptions: PolygonOptions
    private var mPolygon: Polygon? = null
    private var mStartLocation: Location? = null
    private var mLastLocation: Location? = null
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
        binding.cbDrawPolygon.setOnClickListener{ onClickDrawPolygon(it) }

        //Map
        val supportMapFragment: SupportMapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        return root
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_track_area_running, container, false)
    }

    private fun onClickDrawPolygon(view: View?) {
        makePolygon = (view as CheckBox).isChecked

    }

    private fun onStopTracking(view: View?) {

    }

    override fun onResume() {
        super.onResume()
        // register to Location updates
        (activity as MainActivityNav).registerForLocationUpdates(this)
    }

    override fun onPause() {
        super.onPause()
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

    override fun onLocationUpdate(location: Location) {
        // update LatLong
        _binding?.tvCurLat?.text = String.format("%.10f", location.latitude)
        _binding?.tvCurLong?.text = String.format("%.10f", location.longitude)
        _binding?.tvCurAccuracy?.text = String.format("%.3f", location.accuracy)

        //set new point
        setNewPointForArea(location)
    }

    private fun setNewPointForArea(location: Location) {
        if (mLastLocation == null) {
            // first time call, set last location
            mLastLocation = location
            mStartLocation = location
        }

        var results: FloatArray = FloatArray(3)
        Location.distanceBetween(
            mLastLocation!!.latitude,
            mLastLocation!!.longitude,
            location.latitude,
            location.longitude,
            results
        )
        if (results[0] > 0.5) {
            // set point if distance is exceeded TODO: make customizable
            mTrackedLocations.add(location)
            if(makePolygon){
                //Polygon
                if (mPolygon == null) {
                    //first time
                    mPolygonOptions = PolygonOptions()
                    mPolygonOptions.add(
                        LatLng(
                            mStartLocation!!.latitude,
                            mStartLocation!!.longitude
                        )
                    )
                    mPolygonOptions.strokeColor(Color.argb(255,255,0,0))
                    mPolygonOptions.strokeJointType(JointType.ROUND)
                    mPolygonOptions.fillColor(Color.argb(128,255,0,0))
                }

                mPolygonOptions.add(LatLng(location.latitude, location.longitude))
                mPolygon?.remove()
                mPolygon = mGoogleMap.addPolygon(mPolygonOptions)

                //focus on line
                val bounds = LatLngBounds.builder()
                for (point in mPolygon!!.points) {
                    bounds.include(point)
                }
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 50))
            }else {
                //Polyline
                if (mLine == null) {
                    //first time
                    mPolylineOptions = PolylineOptions()
                    mPolylineOptions.add(
                        LatLng(
                            mStartLocation!!.latitude,
                            mStartLocation!!.longitude
                        )
                    )
                    mPolylineOptions.color(Color.argb(255,255,0,0))
                }

                mPolylineOptions.add(LatLng(location.latitude, location.longitude))
                mLine?.remove()
                mLine = mGoogleMap.addPolyline(mPolylineOptions)

                //focus on line
                val bounds = LatLngBounds.builder()
                for (point in mLine!!.points) {
                    bounds.include(point)
                }
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 50))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) {
            Log.e(TrackAreaRunningFragment.javaClass.simpleName, "Error getting google maps")
            return
        }
        //googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(true)

        mGoogleMap = googleMap


    }
}