package de.js.app.agtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(trackedPlaces: List<TrackedPlace>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trackedPlace: TrackedPlace): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAll(trackedPlaces: List<TrackedPlace>)

    @Delete
    fun delete(trackedPlace: TrackedPlace)

    @Query("SELECT * FROM tracked_places ORDER BY date DESC")
    fun getTrackedPlaces(): Flow<List<TrackedPlace>>

    @Query("SELECT * FROM tracked_places WHERE date BETWEEN :dateFrom AND :dateTo" +
           " and name like '%' || :name || '%' order by date desc")
    fun getTrackedPlacesFiltered(dateFrom: String, dateTo: String, name: String): Flow<List<TrackedPlace>>

    @Query("SELECT * FROM tracked_places WHERE id = :id")
    fun getTrackedPlace(id: Long): TrackedPlace

    @Query("SELECT * FROM tracked_places WHERE name LIKE '%' || :query || '%'" +
           "OR date LIKE '%' || :query || '%' ORDER BY date DESC" )
    fun getSearchResults(query: String): Flow<List<TrackedPlace>>
}