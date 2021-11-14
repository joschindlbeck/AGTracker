package de.js.app.agtracker.models

import java.io.Serializable

/**
 * Data model for KML export, matches view vExportKml in DB
 */
data class KmlExportModel(
    val id: String,
    val name: String,
    val date: String,
    val device_id: String,
    val field_name: String,
    val kml_points: String,
    val kml_area: String
) : Serializable