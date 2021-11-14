package de.js.app.agtracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentNavigationInputBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NavigationInputFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NavigationInputFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentNavigationInputBinding? = null

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
        _binding = FragmentNavigationInputBinding.inflate(inflater, container, false)

        binding.btnNavInputStart.setOnClickListener { onNavigationStart(it as Button) }

        return binding.root
    }

    private fun onNavigationStart(btn: Button) {

        //get data from fields & check if Double/convertable
        try {
            val latInput = binding.navInputLat.text.toString().toDouble()
            val lonInput = binding.navInputLon.text.toString().toDouble()
            val latLongArray = doubleArrayOf(latInput, lonInput)
            // if ok, navigate
            findNavController().navigate(
                R.id.action_navigationInputFragment_to_nav_navigation,
                bundleOf(Pair(ARG_PLACE_LATLONG, latLongArray))
            )

        } catch (t: Throwable) {
            Toast.makeText(requireContext(), "Error: ${t.localizedMessage}", Toast.LENGTH_LONG)
                .show()
        }


    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NavigationInputFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NavigationInputFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}