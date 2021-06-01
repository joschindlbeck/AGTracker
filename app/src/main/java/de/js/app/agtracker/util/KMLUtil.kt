package de.js.app.agtracker.util

import android.content.Context
import android.util.Log
import android.util.Xml
import android.widget.Toast
import de.js.app.agtracker.database.DatabaseHandler
import de.js.app.agtracker.models.TrackedPlaceModel
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.StringWriter

class KMLUtil {

    fun writeKMLToFile(myExternalFile: File, text: String): Boolean {
        //var myExternalFile: File = File(getExternalFilesDir(filepath),fileName)
        try {
            val fileOutPutStream = FileOutputStream(myExternalFile)
            fileOutPutStream.write(text.toByteArray())
            fileOutPutStream.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("FileOutput", e.localizedMessage, e)
            return false
        }
    }

    fun createKML(context: Context): String{
        val ns = "http://www.opengis.net/kml/2.2"

        val xmlSerializer = Xml.newSerializer()
        val writer = StringWriter()
        xmlSerializer.setOutput(writer)
        xmlSerializer.startDocument("UTF-8", false)
        xmlSerializer.startTag(ns,"kml")
        xmlSerializer.startTag("","Document")
        xmlSerializer.startTag("","Name")
        xmlSerializer.text("Places from AGTracker")
        xmlSerializer.endTag("","Name")

        // Loop places in DB
        val db = DatabaseHandler(context)
        var list: ArrayList<TrackedPlaceModel> = db.getPlaceList()
        for(item in list){
            xmlSerializer.startTag("","Placemark")
            makeTag(xmlSerializer,"name",item.name)
            xmlSerializer.startTag("","TimeStamp")
            makeTag(xmlSerializer,"when",item.date)
            xmlSerializer.endTag("","TimeStamp")
            xmlSerializer.startTag("","Point")
            makeTag(xmlSerializer,"coordinates",item.longitude.toString()+","+item.latitude.toString()+",0")
            xmlSerializer.endTag("","Point")
            xmlSerializer.endTag("","Placemark")
        }

        xmlSerializer.endTag("","Document")
        xmlSerializer.endTag(ns, "kml")
        xmlSerializer.endDocument()

        return writer.toString()
    }

    private fun makeTag(xmlSerializer: XmlSerializer,tag: String, text: String){
        xmlSerializer.startTag("",tag)
        xmlSerializer.text(text)
        xmlSerializer.endTag("",tag)
    }
}