package de.js.app.agtracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.anbora.labs.spatia.geometry.MultiPoint
import de.js.app.agtracker.util.Util

@Entity(tableName = "tracked_places")
data class TrackedPlace(
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "date")
    var date: String = Util.getNowISO8601(),
    @ColumnInfo(name = "device_id")
    var deviceId: String,
    @ColumnInfo(name = "field_id")
    var fieldId: Long = 0,
    @ColumnInfo(name = "geom_multi_point")
    var geomMultiPoint: MultiPoint
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var trackedPlaceId: Long = 0
}