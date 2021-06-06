package de.js.app.agtracker.ui

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentHomeBinding
import de.js.app.agtracker.databinding.FragmentTrackPointBinding
import de.js.app.agtracker.models.TrackedPlaceModel
import kotlinx.android.synthetic.main.fragment_track_point.*
import java.text.SimpleDateFormat
import java.util.*

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


        binding.btnTrack1.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack2.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack3.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack4.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack5.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack6.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack7.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack8.setOnClickListener {onTrackButtonClicked(it)}
        binding.btnTrack9.setOnClickListener {onTrackButtonClicked(it)}

        return root

        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_track_point, container, false)
    }

    fun onTrackButtonClicked(view: View) {
        var text = ""
        if (view is Button) {
            text = view.text as String
        }

        val myFormat = "dd.MM.yyyy HH:mm:ss"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())

        // save to DB
        val mainActivity = activity as MainActivityNav
        val trackedPlaceModel = TrackedPlaceModel(
            0,
            text,
            mainActivity.mLatitude,
            mainActivity.mLongitude,
            sdf.format(Calendar.getInstance().time).toString()
        )


        if (mainActivity.dbHandler?.addTrackedPlace(trackedPlaceModel) ?: false) {
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

    override fun onLocationUpdate(location: Location) {
        _binding?.tvCurLat?.text = String.format("%.10f", location.latitude)
        _binding?.tvCurLong?.text = String.format("%.10f", location.longitude)
        _binding?.tvCurAccuracy?.text = String.format("%.3f", location.accuracy)
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