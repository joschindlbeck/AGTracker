package de.js.app.agtracker.models

import java.io.Serializable

data class TrackedPlaceModel (
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val date: String,
    val field_id: Int,
    val device_id: String?,
    val geom_multi: String
): Serializable
