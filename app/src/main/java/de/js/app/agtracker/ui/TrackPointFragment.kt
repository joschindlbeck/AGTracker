package de.js.app.agtracker.ui

import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.databinding.FragmentTrackPointBinding
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.Util

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TrackPointFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TrackPointFragment : Fragment(), MainActivityNav.LocationUpdateListener {
    private var mGoodQuality: Boolean = false
    private var mCurLocation: Location? = null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    //View binding
    private var _binding: FragmentTrackPointBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTrackPointBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // set text from preferences
        val mainActivity = activity as MainActivityNav
        val btnTexts: List<String> = mainActivity.getButtonTextsFromPreferences()
        binding.btnTrack1.text = btnTexts[0]
        binding.btnTrack2.text = btnTexts[1]
        binding.btnTrack3.text = btnTexts[2]
        binding.btnTrack4.text = btnTexts[3]
        binding.btnTrack5.text = btnTexts[4]
        binding.btnTrack6.text = btnTexts[5]
        binding.btnTrack7.text = btnTexts[6]
        binding.btnTrack8.text = btnTexts[7]

        // set click handler
        binding.btnTrack1.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack2.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack3.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack4.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack5.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack6.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack7.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack8.setOnClickListener { onTrackButtonClicked(it) }
        //binding.btnTrack9.setOnClickListener { onTrackButtonClicked(it) }

        return root

        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_track_point, container, false)
    }

    fun onTrackButtonClicked(view: View) {
        // GPS quality good enough?
        if (!mGoodQuality) {
            Toast.makeText(requireContext(), "Bad GPS quality, no point added!", Toast.LENGTH_LONG)
                .show()
            return
        }

        // Track point
        var text = ""
        if (view is Button) {
            text = view.text as String
        }

        // Calculate Area/Ring around tracked place
        val ewkt = mCurLocation?.let { createCircleAroundLocation(it) }

        // save to DB
        val mainActivity = activity as MainActivityNav
        val trackedPlaceModel = TrackedPlaceModel(
            0,
            text,
            mCurLocation?.latitude ?: 0.0,
            mCurLocation?.longitude ?: 0.0,
            Util.getNowISO8601(),
            1,
            "",
            Util.getDeviceID(requireContext()),
            ewkt ?: ""
        )


        if (mainActivity.dbHandler?.addTrackedPlace(trackedPlaceModel) ?: 0 > 0) {
            //vibrate if succesffuly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mainActivity.mVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        500,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                //deprecated in API 26
                mainActivity.mVibrator.vibrate(500)
            }
            // Toast for confirmation
            Toast.makeText(
                activity,
                "Created $text: ${mainActivity.mLatitude}/${mainActivity.mLongitude}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Error
            Toast.makeText(
                activity,
                "Error",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    private fun createCircleAroundLocation(location: Location): String {
        val origin: LatLng = LatLng(location.latitude, location.longitude)
        var heading: Double = 0.0
        var points: ArrayList<LatLng> = ArrayList(8)
        for (heading in 0..359 step 45) {
            //TODO: Check distance, amount of points
            points.add(SphericalUtil.computeOffset(origin, 0.5, heading.toDouble()))
        }

        //Transform to ewkt
        //'SRID=4326; MULTIPOINT(long lat, long lat, ...)'
        var stringBuffer = StringBuffer("SRID=4326; MULTIPOINT(")
        for ((i, p) in points.withIndex()) {
            stringBuffer.append(p.longitude).append(" ").append(p.latitude)
            if (i == points.size - 1) {
                //last one
                stringBuffer.append(")")
            } else {
                stringBuffer.append(", ")
            }
        }
        return stringBuffer.toString()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TrackPointFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TrackPointFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onLocationUpdate(location: Location, isGoodQuality: Boolean) {
        _binding?.tvCurLat?.text = String.format("%.10f", location.latitude)
        _binding?.tvCurLong?.text = String.format("%.10f", location.longitude)
        _binding?.tvCurAccuracy?.text = String.format("%.3f", location.accuracy)

        if (isGoodQuality) {

            mCurLocation = location
            mGoodQuality = true
            binding.ivGpsQualityIcon.setColorFilter(Color.GREEN)

        } else {
            // do not use the location
            binding.ivGpsQualityIcon.setColorFilter(Color.RED)
            mGoodQuality = false
        }
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
}