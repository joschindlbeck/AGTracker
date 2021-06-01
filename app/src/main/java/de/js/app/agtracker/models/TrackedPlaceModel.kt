package de.js.app.agtracker.models

import java.io.Serializable

data class TrackedPlaceModel (
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val date: String,
        ): Serializable
