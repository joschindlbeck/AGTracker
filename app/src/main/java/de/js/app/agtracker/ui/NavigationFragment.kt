package de.js.app.agtracker.ui

import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.google.maps.android.ktx.utils.sphericalPathLength
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentListTrackedPlacesBinding
import de.js.app.agtracker.databinding.FragmentNavigationBinding
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.CompassUtil
import kotlinx.android.synthetic.main.activity_map.*
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NavigationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NavigationFragment : Fragment(), MainActivityNav.LocationUpdateListener {
    private var mLocation: Location? = null
    private var mCurrentLocation: LatLng? = null
    private var mPlaceDetails: TrackedPlaceModel? = null
    private lateinit var mTarget: LatLng
    private var mHeadingTargetNorth: Double = 0.0
    private var mHeadingToTarget: Float = 0f
    private var mCurrentAzimuth: Float = 0f
    private lateinit var mCompassUtil: CompassUtil

    private var _binding: FragmentNavigationBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        //Get last entry
        val placeList = (activity as MainActivityNav).dbHandler!!.getPlaceList()
        if (placeList.size > 0) {
            mPlaceDetails = placeList[0]
            mTarget = LatLng(mPlaceDetails!!.latitude, mPlaceDetails!!.longitude)
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
        fun newInstance(param1: String, param2: String) =
            NavigationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onLocationUpdate(location: Location) {

        mCurrentLocation = LatLng(location.latitude,location.longitude)
        mLocation = location

        // distance
        val dist = SphericalUtil.computeDistanceBetween(mCurrentLocation,mTarget)

        // UI
        binding.tvDistanceArrow.text = String.format("%.1f", dist) + "m"
        binding.tvAccuracy.text = String.format("%.3f",location.accuracy) +"m"

        //directions
        mHeadingTargetNorth = SphericalUtil.computeHeading(mCurrentLocation, mTarget)

        // rotate arrow
        rotateArrow()

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

        binding.tvHeadingTarget.text = String.format("%.0f",mHeadingToTarget) +"°"

        animation.duration = 500
        animation.repeatCount = 0
        animation.fillAfter = true
        binding.ivDirectionArrow.startAnimation(animation)
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
                Calendar.getInstance().time.toInstant().toEpochMilli()
                  )
            mCurrentAzimuth = azimuth + magneticField.declination
            rotateArrow()
        }
    }
    return compassListener
}


}