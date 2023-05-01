package de.js.app.agtracker.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import de.js.app.agtracker.R
import de.js.app.agtracker.adapter.TrackedPlacesListAdapter
import de.js.app.agtracker.databinding.FragmentListTrackedPlaces2Binding
import de.js.app.agtracker.viewmodels.TrackedPlacesListViewModel

private const val LOG_TAG = "ListTrackedPlaces2"

@AndroidEntryPoint
class ListTrackedPlaces2Fragment : Fragment() {

    private  lateinit var binding: FragmentListTrackedPlaces2Binding
    //private val viewModel: TrackedPlacesListViewModel by viewModels()
    private val viewModel: TrackedPlacesListViewModel by activityViewModels()
    private lateinit var searchView: SearchView
    private lateinit var adapter: TrackedPlacesListAdapter
    private lateinit var listFilterViewModel: ListFilterViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // binding
        binding = FragmentListTrackedPlaces2Binding.inflate(inflater, container, false)

        // Tracked Places Model & adapter
        adapter = TrackedPlacesListAdapter()
        binding.rvTrackedPlacesList.adapter = adapter
        subscribeUi(adapter, binding)

        // Filter Model
        listFilterViewModel = ViewModelProvider(requireActivity())[ListFilterViewModel::class.java]
        listFilterViewModel.dateFrom.observe(viewLifecycleOwner) { result ->
            Log.i(LOG_TAG, "dateFrom: $result")
        }
        listFilterViewModel.dateTo.observe(viewLifecycleOwner) { result ->
            Log.i(LOG_TAG, "dateTo: $result")
        }
        listFilterViewModel.name.observe(viewLifecycleOwner) { result ->
            Log.i(LOG_TAG, "name: $result")
        }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_tracked_places_menu, menu)

        //add search?
        val search = menu.findItem(R.id.action_search)
        searchView = search.actionView as SearchView
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                //Log.i(LOG_TAG, "onQueryTextSubmit: $query")
                if(query!=null){
                    getItemsFromDb(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //Log.i(LOG_TAG, "onQueryTextChange: $newText")
                if(newText!=null){
                    getItemsFromDb(newText)
                }
                return true
            }
        })


        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun getItemsFromDb(query: String) {
        viewModel.searchForTrackedPlaces(query).observe(viewLifecycleOwner) { result ->
            //Log.i(LOG_TAG, "searchTrackedPlaces: $result")
            adapter.submitList(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_filter -> {
                //viewModel.toggleFilter()
                Log.i(LOG_TAG, "Filter clicked")
                ListFilterFragment().show(parentFragmentManager, "ListFilterFragment")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
        return true

    }
}