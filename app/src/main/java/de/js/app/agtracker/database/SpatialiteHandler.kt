package de.js.app.agtracker.database

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteException
import android.system.Os
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import de.js.app.agtracker.models.KmlExportModel
import de.js.app.agtracker.models.TrackedPlaceModel
import de.js.app.agtracker.util.CompressionUtilities
import de.js.app.agtracker.util.FileUtilities
import de.js.app.agtracker.util.Util
import jsqlite.Constants
import jsqlite.Database
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


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
        private const val EXPORT_DIRECTORY = "export"
        private const val DB_FILE = "agtracker.sqlite"
        private const val DB_VERSION = 1
        public const val EXPORT_TYPE_SHP = "SHP"

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
                createDB2(spatialDbFile, context, dir!!)
            }
            //open it
            mDB.open(spatialDbFile.getAbsolutePath(), Constants.SQLITE_OPEN_READWRITE)
            Log.d(TAG, "Database Version: ${mDB.dbversion()}")

            //Set environment variables
            // for Project DB
            Os.setenv("PROJ_LIB", dir?.absolutePath + "/proj/proj", true)
            // for relaxed Security
            Os.setenv("SPATIALITE_SECURITY", "relaxed", true)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Database", e)
            throw e
        }

    }

    private fun createDB2(spatialDbFile: File, context: Context, dbDir: File) {
        //Prepare PROJ db
        copyProj(context, dbDir)

        //Copy Template Database
        copyDB(context, spatialDbFile)

    }

    private fun copyProj(context: Context, dbDir: File) {
        val projFolder: File = File(dbDir, "proj")
        if (!projFolder.exists()) {
            projFolder.mkdir()
        }

        val projDbFile = File(projFolder, "proj.db")
        if (!projDbFile.exists()) {
            val zipFile: File = File(dbDir, "proj.zip")
            val assetManager: AssetManager = context.assets
            val inputStream: InputStream = assetManager.open("proj.zip")
            FileUtilities.copyFile(inputStream, FileOutputStream(zipFile))
            try {
                CompressionUtilities.unzipFolder(
                    zipFile.absolutePath,
                    projFolder.getAbsolutePath(),
                    false
                )
            } finally {
                zipFile.delete()
            }
        }
        Os.setenv("PROJ_LIB", projFolder.absolutePath + "/proj", true)

    }

    private fun copyDB(context: Context, spatialDbFile: File) {
        var templateIS = context.assets.open("agtracker_template.sqlite")
        var out = FileOutputStream(spatialDbFile.absolutePath, false)
        var buffer: ByteArray = ByteArray(1024)
        while (templateIS.read(buffer) > 0) {
            out.write(buffer)
        }
        templateIS.close()
        out.close()
        Log.i(TAG, "Database copy from template successflu!")
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

    fun addTrackedPlace(trackedPlace: TrackedPlaceModel): Long {
        //TODO: Error handling
        val insertSql = "INSERT INTO tPlace\n" +
                "(name, date, field_id, geom_multi, latitude, longitude, device_id)\n" +
                "values ('${trackedPlace.name}', '${trackedPlace.date}', '${trackedPlace.field_id}', " +
                "GeomFromEWKT('${trackedPlace.geom_multi}')," +
                "'${trackedPlace.latitude}', " +
                "'${trackedPlace.longitude}'," +
                "'${trackedPlace.device_id}');"
        Log.d(TAG, "Insert Statement: $insertSql")
        val stmt = mDB.prepare(insertSql)
        val success = stmt.step()
        stmt.close()
        // get id
        return mDB.last_insert_rowid()

    }

    fun getPlaceList(): ArrayList<TrackedPlaceModel> {
        val list: ArrayList<TrackedPlaceModel> = ArrayList()
        val selectQuery =
            "Select tPlace.id, tPlace.name, latitude, longitude, date, field_id, tFields.name as field_name, " +
                    "device_id, asewkt(geom_multi) from tPlace inner join tFields where tFields.id = field_id " +
                    "order by date desc;"
        Log.d(TAG, "Select Statement: " + selectQuery)
        try {
            val stmt = mDB.prepare(selectQuery)
            while (stmt.step()) {
                val place = TrackedPlaceModel(
                    stmt.column_int(0),
                    stmt.column_string(1),
                    stmt.column_double(2),
                    stmt.column_double(3),
                    stmt.column_string(4),
                    stmt.column_int(5),
                    stmt.column_string(6),
                    stmt.column_string(7),
                    stmt.column_string(8)
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

    fun getPlace(id: Int): TrackedPlaceModel {
        val list: ArrayList<TrackedPlaceModel> = ArrayList()
        val selectQuery =
            "Select tPlace.id, tPlace.name, latitude, longitude, date, field_id, tFields.name as field_name, " +
                    "device_id, asewkt(geom_multi) from tPlace inner join tFields on tFields.id = field_id " +
                    "where tPlace.id = $id;"
        Log.d(TAG, "Select Statement: " + selectQuery)
        try {
            val stmt = mDB.prepare(selectQuery)
            while (stmt.step()) {
                val place = TrackedPlaceModel(
                    stmt.column_int(0),
                    stmt.column_string(1),
                    stmt.column_double(2),
                    stmt.column_double(3),
                    stmt.column_string(4),
                    stmt.column_int(5),
                    stmt.column_string(6),
                    stmt.column_string(7),
                    stmt.column_string(8)
                )
                list.add(place)
            }
            stmt.reset()
            stmt.close()
        } catch (e: SQLiteException) {
            return TrackedPlaceModel(0, "Place not found", 0.0, 0.0, "", 0, "", "", "")
        }
        return list.first()
    }

    fun getExportKml(): ArrayList<KmlExportModel> {
        val list: ArrayList<KmlExportModel> = ArrayList()
        val selectQuery = "Select * from vExportKml;"
        Log.d(TAG, "Select Statement: " + selectQuery)
        try {
            val stmt = mDB.prepare(selectQuery)
            while (stmt.step()) {
                val kmlExportModel = KmlExportModel(
                    stmt.column_string(0), //id
                    stmt.column_string(1), //name
                    stmt.column_string(2), //date
                    stmt.column_string(3), //device_id
                    stmt.column_string(4), //field_name
                    stmt.column_string(5), //kml_points
                    stmt.column_string(6) //kml_area
                )
                list.add(kmlExportModel)
            }
            stmt.reset()
            stmt.close()
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error during DB execution", e)
            return ArrayList()
        }
        return list
    }

    fun deleteTrackedPlace(trackedPlaceModel: TrackedPlaceModel): Int {
        var deleteQuery = "delete from tPlace where id = ${trackedPlaceModel.id}"

        Log.d(TAG, deleteQuery)

        try {
            val stmt = mDB.prepare(deleteQuery)
            val success = stmt.step()
            stmt.close()
            return trackedPlaceModel.id

        } catch (e: Exception) {
            Log.e(TAG, "Error at Deletion", e)
            return 0
        }
    }

    fun addPointsToTrackedPlace(placeId: Long, points: List<LatLng>) {

        //Build geom string for spatial db
        val sb = StringBuffer("GeomFromEWKT('SRID=4326; MULTIPOINT(")

        for ((i, point) in points.withIndex()) {
            if (i == points.size - 1) {
                // last one
                sb.append(point.longitude).append(" ").append(point.latitude).append((")')"))
            } else {
                sb.append(point.longitude).append(" ").append(point.latitude).append((", "))
            }
        }

        var UPDATE = "UPDATE tPlace \n" +
                "set geom_multi = \n" + sb.toString() +
                " where id = $placeId;"

        Log.d(TAG, UPDATE)

        //TODO: Error Handling
        val stmt = mDB.prepare(UPDATE)
        val success = stmt.step()
        stmt.close()

    }

    /**
     * Return the Lat/Long values for the Center Point of a Multipoint
     * geometry stored in table tPlaces
     * @param id: DB ID to be retrieved
     */
    fun getLatLongCenterForMultipoint(id: Int): LatLng {
        var center: LatLng = LatLng(0.0, 0.0)
        val selectQuery = "select ST_X(center) as lat, ST_Y(center) as long from(\n" +
                "select Centroid(hull) as center from(\n" +
                "select ConvexHull(mp) as hull from (\n" +
                "select geom_multi as mp from tPlace where id = $id)));\n"

        Log.d(TAG, "Select Statement: " + selectQuery)
        try {
            val stmt = mDB.prepare(selectQuery)
            while (stmt.step()) {
                center = LatLng(stmt.column_double(0), stmt.column_double(1))

            }
            stmt.reset()
            stmt.close()
            return center
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting Center", e)
            return center
        }
    }

    fun export(context: Context, exportType: String, zipFileName: String) {
        val dir: File? =
            context.getExternalFilesDir(EXPORT_DIRECTORY + File.pathSeparator + exportType + Util.getTimestampPath())
        Log.d(TAG, "Export path: " + dir?.absolutePath)
        if (dir != null) {
            if (!dir.exists()) {
                dir.createNewFile()
            }
            // export via SQL / Spatialite
            when (exportType) {
                EXPORT_TYPE_SHP -> exportSHP(dir)
                else -> exportSHP(dir)
            }

            // Zip the directory
            Util.zipDirectory(dir.absolutePath, zipFileName)
        }

    }

    private fun exportSHP(dir: File) {
        val selectQuery =
            "select ExportSHP('tPlace', 'geom_multi', '${dir.absolutePath + File.pathSeparator}AGTracker', 'UTF-8')"
        Log.d(TAG, selectQuery)
        val stmt = mDB.prepare(selectQuery)
        val success = stmt.step()
        stmt.close()
    }

    /**
     * Get the convex hull of a place as WellKnownText Representation
     * @param id ID of the place (db key)
     */
    fun getConvexHullAsWKT(id: Int): String {
        var wkt = ""
        val selectQuery = "Select aswkt(convexhull(geom_multi))from tPlace where id = $id;"
        Log.d(TAG, "Select Statement: " + selectQuery)
        try {
            val stmt = mDB.prepare(selectQuery)
            while (stmt.step()) {
                wkt = stmt.column_string(0)
            }
            stmt.reset()
            stmt.close()
            return wkt
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error getting ConvexHull as GeoJSON", e)
            return wkt
        }
    }

}


