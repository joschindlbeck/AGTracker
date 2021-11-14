package de.js.app.agtracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentTrackAreaBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TrackAreaFragement.newInstance] factory method to
 * create an instance of this fragment.
 */
class TrackAreaFragement : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    //View binding
    private var _binding: FragmentTrackAreaBinding? = null
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrackAreaBinding.inflate(inflater, container, false)
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


        // binding of click handler
        binding.btnTrack1.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack2.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack3.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack4.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack5.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack6.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack7.setOnClickListener { onTrackButtonClicked(it) }
        binding.btnTrack8.setOnClickListener { onTrackButtonClicked(it) }
        //binding.btnTrack9.setOnClickListener {onTrackButtonClicked(it)}

        return root

    }

    private fun onTrackButtonClicked(view: View?) {
        var text = ""
        if (view is Button) {
            text = view.text as String
        }
    findNavController().navigate(R.id.action_nav_track_area_to_trackAreaRunningFragment,
                                bundleOf(Pair(TrackAreaRunningFragment.ARG_TRACKING_ID,text)))
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TrackArea.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TrackAreaFragement().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}