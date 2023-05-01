package de.js.app.agtracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import de.js.app.agtracker.R
import de.js.app.agtracker.adapter.TrackedPlacesListAdapter
import de.js.app.agtracker.databinding.FragmentListTrackedPlaces2Binding
import de.js.app.agtracker.viewmodels.TrackedPlacesListViewModel

@AndroidEntryPoint
class ListTrackedPlaces2Fragment : Fragment() {

    private  lateinit var binding: FragmentListTrackedPlaces2Binding
    private val viewModel: TrackedPlacesListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListTrackedPlaces2Binding.inflate(inflater, container, false)

        val adapter = TrackedPlacesListAdapter()
        binding.rvTrackedPlacesList.adapter = adapter
        subscribeUi(adapter, binding)
        return binding.root
    }

    private fun subscribeUi(adapter: TrackedPlacesListAdapter, binding: FragmentListTrackedPlaces2Binding) {
        viewModel.trackedPlaces.observe(viewLifecycleOwner) { result ->
            adapter.submitList(result) {
                // At this point, the content should be drawn
                activity?.reportFullyDrawn()
            }
        }
    }

}