package de.js.app.agtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import co.anbora.labs.spatia.builder.SpatiaRoom
import co.anbora.labs.spatia.geometry.GeometryConverters

@Database(
    entities = [TrackedPlace::class],
    version = 1,
    exportSchema = false
)

@TypeConverters(GeometryConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackedPlaceDao(): TrackedPlaceDao

    companion object {
        const val DB_NAME = "agtracker_room_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = SpatiaRoom.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Initialize Spatiallite
                        db.query("SELECT InitSpatialMetaData();").moveToNext()
                        // Room already creates a BLOB column for the geometry, so we need to use
                        // RecoverGeometryColumn to correctly initialize Spatialite's metadata
                        db.query("SELECT RecoverGeometryColumn('geo_posts', 'location', 4326, 'POINT', 'XY');")
                            .moveToNext()
                        // create a spatial index (optional)
                        db.query("SELECT CreateSpatialIndex('geo_posts', 'location');")
                            .moveToNext()
                    }
                }).build()
                INSTANCE = instance
                return instance
            }

        }
    }

}