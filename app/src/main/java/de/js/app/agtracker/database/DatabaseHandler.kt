package de.js.app.agtracker.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import de.js.app.agtracker.models.TrackedPlaceModel

class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

        companion object{
            private const val DATABASE_NAME = "AGTrackerDB"
            private const val DATABASE_VERSION = 1
        }

    override fun onCreate(db: SQLiteDatabase?) {
        //sql create statement
        val CREATE_TABLE = "CREATE TABLE place " +
                "(id INTEGER PRIMARY KEY, " +
                "name text, " +
                "latitude text, " +
                "longitude text, " +
                "date text)"
        db?.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
       db!!.execSQL("DROP TABLE IF EXISTS place")
        onCreate(db)

    }

    fun addPlace(trackedPlace: TrackedPlaceModel): Long{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("name", trackedPlace.name)
        contentValues.put("date", trackedPlace.date)
        contentValues.put("latitude", trackedPlace.latitude)
        contentValues.put("longitude",trackedPlace.longitude)

        val result = db.insert("place",null,contentValues)
        db.close()
        return result

    }

    fun getPlaceList(): ArrayList<TrackedPlaceModel>{
        val list: ArrayList<TrackedPlaceModel> = ArrayList()
        val selectQuery = "SELECT * FROM place ORDER BY date DESC"
        val db = this.readableDatabase
        try {
            val cursor: Cursor = db.rawQuery(selectQuery, null)
            if(cursor.moveToFirst()){
                do{
                    val place = TrackedPlaceModel(
                        cursor.getInt(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getDouble(cursor.getColumnIndex("latitude")),
                        cursor.getDouble(cursor.getColumnIndex("longitude")),
                        cursor.getString(cursor.getColumnIndex("date")),
                    )
                    list.add(place)
                }while (cursor.moveToNext())
            }
            cursor.close()
        }catch (e: SQLiteException){
            db.execSQL(selectQuery)
            return ArrayList()
        }
        return list
    }

    fun deleteTrackedPlace(trackedPlaceModel: TrackedPlaceModel): Int {
        val db = this.writableDatabase
        val success = db.delete("place", "id = " + trackedPlaceModel.id, null)

        db.close()
        return success
    }
}