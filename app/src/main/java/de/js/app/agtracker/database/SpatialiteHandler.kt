package de.js.app.agtracker.database

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import de.js.app.agtracker.models.TrackedPlaceModel
import jsqlite.Constants
import jsqlite.Database
import java.io.File

/**
 *  Handler class for Spatialite DB
 */
class SpatialiteHandler {

    // Properties
    lateinit var mDB: Database

    // Constants
    companion object {
        private const val TAG = "DatabaseManager"
        private const val DB_DIRECTORY = "database"
        private const val DB_FILE = "agtracker.sqlite"
        private const val DB_VERSION = 1

    }

    /**
     *  Database initialization
     */
    @Throws(jsqlite.Exception::class)
    fun init(context: Context) {
        try {
            val dir: File? = context.getExternalFilesDir(DB_DIRECTORY)
            val spatialDbFile = File(dir, DB_FILE)

            mDB = Database()
            //DB already exists?
            if (!spatialDbFile.exists()) {
                // DB does not exist
                createDB(spatialDbFile)
            } else {
                //open it
                mDB.open(spatialDbFile.getAbsolutePath(), Constants.SQLITE_OPEN_READWRITE)
                Log.d(TAG, "Database Version: ${mDB.dbversion()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Database", e)
            throw e
        }

    }

    private fun createDB(spatialDbFile: File) {
        //TODO: Exception Handling
        mDB.open(
            spatialDbFile.getAbsolutePath(), Constants.SQLITE_OPEN_READWRITE
                    or Constants.SQLITE_OPEN_CREATE
        )

        //Create Tracked places
        val CREATE_TABLE = "CREATE TABLE ${TableDescriptions.TrackedPlace.TABLE_NAME} " +
                "(${TableDescriptions.TrackedPlace.FIELD_ID} INTEGER PRIMARY KEY, " +
                "${TableDescriptions.TrackedPlace.FIELD_NAME} text, " +
                "${TableDescriptions.TrackedPlace.FIELD_LATITUDE} text, " +
                "${TableDescriptions.TrackedPlace.FIELD_LONGITUDE} text, " +
                "${TableDescriptions.TrackedPlace.FIELD_DATE} text)"
        val stmt = mDB.prepare(CREATE_TABLE)
        if (stmt.step()) {
            Log.i(TAG, "Table ${TableDescriptions.TrackedPlace.TABLE_NAME} was created")
        }
        stmt.close()

    }

    fun addTrackedPlace(trackedPlace: TrackedPlaceModel): Boolean {
        val INSERT = "INSERT INTO ${TableDescriptions.TrackedPlace.TABLE_NAME} " +
                "(${TableDescriptions.TrackedPlace.FIELD_NAME}, " +
                "${TableDescriptions.TrackedPlace.FIELD_LATITUDE}, " +
                "${TableDescriptions.TrackedPlace.FIELD_LONGITUDE}, " +
                "${TableDescriptions.TrackedPlace.FIELD_DATE}) " +
                "VALUES('${trackedPlace.name}', " +
                "${trackedPlace.latitude}, " +
                "${trackedPlace.longitude}, " +
                "'${trackedPlace.date}');"
        Log.i(TAG, "Insert Statement: $INSERT")
        val stmt = mDB.prepare(INSERT)
        val success = stmt.step()
        stmt.close()
        return true
    }

    fun getPlaceList(): ArrayList<TrackedPlaceModel> {
        val list: ArrayList<TrackedPlaceModel> = ArrayList()
        val selectQuery = "SELECT * FROM ${TableDescriptions.TrackedPlace.TABLE_NAME} " +
                "ORDER BY ${TableDescriptions.TrackedPlace.FIELD_DATE} DESC;"
        Log.i(TAG, "Select Statement: " + selectQuery)
        try {
            val stmt = mDB.prepare(selectQuery)
            while (stmt.step()) {
                val place = TrackedPlaceModel(
                    stmt.column_int(0),
                    stmt.column_string(1),
                    stmt.column_double(2),
                    stmt.column_double(3),
                    stmt.column_string(4)
                )
                list.add(place)
            }
            stmt.reset()
            stmt.close()
        } catch (e: SQLiteException) {
            return ArrayList()
        }
        return list
    }

    fun deleteTrackedPlace(trackedPlaceModel: TrackedPlaceModel): Int {
        //TODO Implement
        return 1
    }
}


