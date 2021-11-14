package de.js.app.agtracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.R
import de.js.app.agtracker.models.TrackedPlaceModel
import kotlinx.android.synthetic.main.item_place.view.*

open class PlacesAdapter(
    private val context: Context,
    private var list: ArrayList<TrackedPlaceModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onClickListener: OnClickListener? = null

    /**
     * Setzt das Item in MyViewHolder auf das Item, welches in XML von uns erstellt wurde
     *
     * Erstellung eines neuen
     * {@link ViewHolder} und Initialisierung einiger privater Felder, welche für das RecyclerView
     * benötigt werden.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_place,
                parent,
                false
            )
        )
    }

    /**
     * Verknüpft ein Item aus der View mit einem Element aus der ArrayList
     *
     * Wird aufgerufen, wenn das RecyclerView einen neuen {@link ViewHolder} des gegebenen
     * Typen benötigt.
     *
     * ViewHolder sollte mit einer neuen View erstellt werden, welches die Items des gegebenen
     * Typs repräsentiert. Ein View kann manuell erstellt werden oder durch eine XML-Datei
     * beeinflusst werden.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            //holder.itemView.iv_place_image.setImageURI(Uri.parse(model.image))

            holder.itemView.tvField.text = model.field_name
            holder.itemView.tvName.text = model.name
            holder.itemView.tvDate.text = model.date
            //holder.itemView.tvDate.text = DateTimeFormatter.ISO_INSTANT.format(DateTime(model.date))
            holder.itemView.tvLat.text = String.format("%.6f", model.latitude)
            holder.itemView.tvLong.text = String.format("%.6f", model.longitude)

            //set onClickListener for button
            holder.itemView.btnViewOnMap.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun removeAt(position: Int) {

        val isDeleted =
            (context as MainActivityNav).dbHandler?.deleteTrackedPlace(list[position]) ?: 0

        if (isDeleted > 0) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * ViewHolder beschreibt eine Item-Ansicht und enthält Metadaten über die Platzierung im
     * RecyclerView.
     */
    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
    interface OnClickListener {
        fun onClick(position: Int, model: TrackedPlaceModel)
    }
}