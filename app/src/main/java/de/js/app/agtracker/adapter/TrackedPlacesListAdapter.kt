package de.js.app.agtracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.js.app.agtracker.data.TrackedPlace
import de.js.app.agtracker.databinding.ItemTrackedPlaceBinding

class TrackedPlacesListAdapter: ListAdapter<TrackedPlace,TrackedPlacesListAdapter.ViewHolder>(TrackedPlaceDiffCallback()) {
    class ViewHolder(private val binding: ItemTrackedPlaceBinding) : RecyclerView.ViewHolder(binding.root) {
        init {


            /** binding.btnViewOnMap.setOnClickListener { view ->
            binding.viewModel?.placeId?.let { placeId ->
            navigateToPlace(placeId, view)
            }
            }**/
        }

        fun bind(trackedPlace: TrackedPlace) {
            with(binding) {
                tvName.text = trackedPlace.name
                tvDate.text = trackedPlace.date
                //TODO: weitere Felder
                executePendingBindings()
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTrackedPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class TrackedPlaceDiffCallback : DiffUtil.ItemCallback<TrackedPlace>() {

    override fun areItemsTheSame(
        oldItem: TrackedPlace,
        newItem: TrackedPlace
    ): Boolean {
        return oldItem.trackedPlaceId == newItem.trackedPlaceId
        //return oldItem.plant.plantId == newItem.plant.plantId
    }

    override fun areContentsTheSame(
        oldItem: TrackedPlace,
        newItem: TrackedPlace
    ): Boolean {
        return oldItem == newItem
    }
}