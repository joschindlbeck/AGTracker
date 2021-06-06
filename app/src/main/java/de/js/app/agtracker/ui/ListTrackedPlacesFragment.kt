package de.js.app.agtracker.ui

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.activities.MainActivity
import de.js.app.agtracker.activities.MapActivity
import de.js.app.agtracker.adapter.PlacesAdapter
import de.js.app.agtracker.databinding.FragmentListTrackedPlacesBinding
import de.js.app.agtracker.databinding.FragmentTrackPointBinding
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.SwipeToDeleteCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ListTrackedPlacesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ListTrackedPlacesFragment : Fragment() {
    private var _binding: FragmentListTrackedPlacesBinding? = null
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

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListTrackedPlacesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //ged from db
        getPlacesFromLocalDB()

        return root
    }

    private fun setupPlacesRecyclerView(placeList: ArrayList<TrackedPlaceModel>) {
        binding.rvPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaces.setHasFixedSize(true)
        val placesAdapter = PlacesAdapter(requireContext(), placeList)
        binding.rvPlaces.adapter = placesAdapter

        placesAdapter.setOnClickListener(object :
            PlacesAdapter.OnClickListener {
            override fun onClick(position: Int, model: TrackedPlaceModel) {
                val intent = Intent(requireContext(), MapActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })

        val deleteSwipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvPlaces.adapter as PlacesAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                getPlacesFromLocalDB() // Holt aktualisierte Liste aus der Datenbank, nachdem Element gelöscht wurde.

            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(rvPlaces)
    }

    private fun getPlacesFromLocalDB() {
        val placeList = (activity as MainActivityNav).dbHandler!!.getPlaceList()
        if (placeList.size > 0) {
            binding.rvPlaces.visibility = View.VISIBLE
            binding.tvNoPlacesFound.visibility = View.GONE
            setupPlacesRecyclerView(placeList)
            for (i in placeList) {
                Log.e("Name", i.name)
            }
        } else {
            binding.rvPlaces.visibility = View.GONE
            binding.tvNoPlacesFound.visibility = View.VISIBLE
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ListTrackedPlacesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ListTrackedPlacesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}