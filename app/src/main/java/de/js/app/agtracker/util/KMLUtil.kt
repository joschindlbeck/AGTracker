package de.js.app.agtracker.util

import android.util.Log
import android.util.Xml
import de.js.app.agtracker.database.SpatialiteHandler
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.StringWriter

class KMLUtil {

    private val EXTENDED_DATA_DATE: String = "Date"
    private val EXTENDED_DATA_DEVICE: String = "DeviceId"
    private val EXTENDED_DATA_ID: String = "Id"

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

    /**
    fun createKML(context: Context): String {
    val ns = "http://www.opengis.net/kml/2.2"

    val xmlSerializer = Xml.newSerializer()
    val writer = StringWriter()
    xmlSerializer.setOutput(writer)
    xmlSerializer.startDocument("UTF-8", false)
    xmlSerializer.startTag(ns, "kml")
    xmlSerializer.startTag("", "Document")
    xmlSerializer.startTag("", "Name")
    xmlSerializer.text("Places from AGTracker")
    xmlSerializer.endTag("", "Name")

    // Loop places in DB
    /* TODO: Change To Spatial DB Hanlder
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
    */
    return writer.toString()
    }

    private fun makeTag(xmlSerializer: XmlSerializer, tag: String, text: String) {
    xmlSerializer.startTag("", tag)
    xmlSerializer.text(text)
    xmlSerializer.endTag("", tag)
    }
     */

    fun createKML(dbHandler: SpatialiteHandler): String {
        val xml = Xml.newSerializer()
        val writer = StringWriter()
        val exportList = dbHandler.getExportKml()

        // Build the XML
        xml.document(xmlStringWriter = writer) {
            element("http://www.opengis.net/kml/2.2", "kml") {
                element("", "Document") {
                    element("", "Folder") {
                        element("", "name", "Punkte")
                        //alle Punkte
                        for (item in exportList) {
                            elementCdsect("", "Placemark", item.kml_points) {
                                element("", "name", item.name)
                                element("", "description", item.name)
                                element("", "ExtendedData") {
                                    element("", "Data") {
                                        attribute("", "name", EXTENDED_DATA_DATE)
                                        element("", "value", item.date)
                                    }
                                    element("", "Data") {
                                        attribute("", "name", EXTENDED_DATA_DEVICE)
                                        element("", "value", item.device_id)
                                    }
                                    element("", "Data") {
                                        attribute("", "name", EXTENDED_DATA_ID)
                                        element("", "value", item.id)
                                    }

                                }
                            }
                        }
                    }
                    element("", "Folder") {
                        element("", "name", "Flächen")
                        //alle Punkte
                        for (item in exportList) {
                            elementCdsect("", "Placemark", item.kml_area) {
                                element("", "name", item.name)
                                element("", "description", item.name)
                                element("", "ExtendedData") {
                                    element("", "Data") {
                                        attribute("", "name", EXTENDED_DATA_DATE)
                                        element("", "value", item.date)
                                    }
                                    element("", "Data") {
                                        attribute("", "name", EXTENDED_DATA_DEVICE)
                                        element("", "value", item.device_id)
                                    }
                                    element("", "Data") {
                                        attribute("", "name", EXTENDED_DATA_ID)
                                        element("", "value", item.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //workaround: reomve cdsect
        var xmlString = writer.toString()
        return xmlString.replace("<![CDATA[", "").replace("]]>", "")

    }
}