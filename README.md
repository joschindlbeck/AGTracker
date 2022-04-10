# AGTracker - An Android APP and idea how to record precision areas for farming purposes.

**work in progress!**

* app is in early alpha stage
* first field tests started (two fields recorded)
* no internationalization (German texts only)
* not yet optimized / quality assurance tbd.

## Project goal

The intention of this project is to enhance the open source guidance system [AgOpenGPS](https://github.com/farmerbriantee/AgOpenGPS) with a option to do precision farming, especially applying herbicide to small spots of weed.

## Current Setup / Components

### AGTracker Android App

To determine and track the areas in the field where herbicide shall be apllied, we developed the Android based app AGTracker that acts as a tracking device for GPS coordinates.

In a nutshell, you click a button on the app and the app starts to record GPS points wherever you move. With this tracking, you can walk around a spot of weeds in the field and AGTracker is creating a GPS point cloud in the background. This data can then be exported to QGIS for further processing.

![Start recording](/docs/images/Screenshot_area_tracking_start1.png)
![Start recording](/docs/images/screenshot_area_tracking_1.png)

[Demo Video](https://youtu.be/wDYr2NimR5c)

To enable a high degree of accurracy, the app gets the GPS coordinates by an Ardusimple F9P GPS receiver with NTRIP data correction. The GPS handling is done via the [Lefebure NTRIP Client](https://play.google.com/store/apps/details?id=com.lefebure.ntripclient&hl=de&gl=US) and forwarded to the AG Tracker app as GPS mock signal.

### QGIS processing

After export of the GPS data from the app QGIS is used to visualize the data, to enable the option to manually edit the data if needed and process further to be usuable for AgOpenGPS.

![Weed spots](/docs/images/screenshot_qgis_unkraut.png)
![Weed spots processed](/docs/images/screenshot_qgs_processed.png)

Handover to AGOpenGPS is done by automated creation of a Sections-File to "mock" AGOpenGPS that every place in the specific field has been already applied besides the weed spots. Therefore in QGIS a processing routine creates an area of the field and substracts the weed spots (basically cuts holes in the area). The remaining area is then devided to square fields and those are processed to a Section file by a custom python script.

For a detailed explanaition see this [video](https://vimeo.com/645569057)

Details on the QGIS processing and the relevant scripts can be found [here](https://github.com/joschindlbeck/aog_qgis).

### AgOpenGPS processing

The created Sections file can than be imported in AgOpenGPS as usual and will result in a partly applied field where only the relevant spots are not yeat applied. With the AgOpenGPS section control enabled, only the weed spots will be applied.

![AgO](/docs/images/screenshot_ago.jpeg)
