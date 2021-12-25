# -*- coding: utf-8 -*-

"""
***************************************************************************
*                                                                         *
*   This program is free software; you can redistribute it and/or modify  *
*   it under the terms of the GNU General Public License as published by  *
*   the Free Software Foundation; either version 2 of the License, or     *
*   (at your option) any later version.                                   *
*                                                                         *
***************************************************************************
"""

from qgis.PyQt.QtCore import QCoreApplication
from qgis.core import (QgsProcessing,
                       QgsFeatureSink,
                       QgsProcessingException,
                       QgsProcessingAlgorithm,
                       QgsProcessingParameterFeatureSource,
                       QgsProcessingParameterFeatureSink,
                       QgsProcessingParameterFileDestination,
                       QgsProcessingParameterFile)
from qgis import processing
from math import cos

class AgSectionFileCreator(QgsProcessingAlgorithm):
    """
    This is an algorithm that takes a vector layer with quadrats and
    creates an AGOpenGPS Section file in a way that all quadrats are already
    applied in AGO.
    """

    # Constants used to refer to parameters and outputs. They will be
    # used when calling the algorithm from another algorithm, or when
    # calling from the QGIS console.

    INPUT = 'INPUT'
    OUTPUT = 'OUTPUT'
    OUTPUT_FILE = 'OUTPUT_FILE'
    INPUT_FIELDS_FILE = 'INPUT_FIELDS_FILE'
    
    mPerDegreeLat = 0.0
    mPerDegreeLon = 0.0
    latStart = 48.9636327590282
    lonStart = 12.1934211840036
    count = 0
    
    def tr(self, string):
        """
        Returns a translatable string with the self.tr() function.
        """
        return QCoreApplication.translate('Processing', string)

    def createInstance(self):
        return AgSectionFileCreator()

    def name(self):
        """
        Returns the algorithm name, used for identifying the algorithm. This
        string should be fixed for the algorithm, and must not be localised.
        The name should be unique within each provider. Names should contain
        lowercase alphanumeric characters only and no spaces or other
        formatting characters.
        """
        return 'agsectionfilecreator'

    def displayName(self):
        """
        Returns the translated algorithm name, which should be used for any
        user-visible display of the algorithm name.
        """
        return self.tr('Section file creator for AGOpenGPS')

    def group(self):
        """
        Returns the name of the group this algorithm belongs to. This string
        should be localised.
        """
        return self.tr('AGOpenGPS')

    def groupId(self):
        """
        Returns the unique ID of the group this algorithm belongs to. This
        string should be fixed for the algorithm, and must not be localised.
        The group id should be unique within each provider. Group id should
        contain lowercase alphanumeric characters only and no spaces or other
        formatting characters.
        """
        return 'agopengps'

    def shortHelpString(self):
        """
        Returns a localised short helper string for the algorithm. This string
        should provide a basic description about what the algorithm does and the
        parameters and outputs associated with it..
        """
        return self.tr("Create Section file for AGOpenGPS")

    def initAlgorithm(self, config=None):
        """
        Here we define the inputs and output of the algorithm, along
        with some other properties.
        """

        # Input Layer with quadrats
        self.addParameter(
            QgsProcessingParameterFeatureSource(
                self.INPUT,
                self.tr('Input layer'),
                [QgsProcessing.TypeVectorAnyGeometry]
            )
        )
        # Input File Fields.txt from AGOpenGPS
        self.addParameter(QgsProcessingParameterFile(self.INPUT_FIELDS_FILE, self.tr('AGO Fields file')))
        # File destination for Section.txt
        self.addParameter(QgsProcessingParameterFileDestination(self.OUTPUT_FILE, "Section file for AGO"))
        
        

    def processAlgorithm(self, parameters, context, feedback):
        """
        Here is where the processing itself takes place.
        """

        # Retrieve the feature source = the vector layer
        source = self.parameterAsSource(
            parameters,
            self.INPUT,
            context
        )

        # If source was not found, throw an exception to indicate that the algorithm
        # encountered a fatal error. The exception text can be any string, but in this
        # case we use the pre-built invalidSourceError method to return a standard
        # helper text for when a source cannot be evaluated
        if source is None:
            raise QgsProcessingException(self.invalidSourceError(parameters, self.INPUT))

        # Retrieve AGO fields file
        fields = self.parameterAsFile(parameters, self.INPUT_FIELDS_FILE, context)
        if fields is None:
            raise QgsProcessingException(self.invalidSourceError(parameters, self.INPUT_FIELDS_FILE)) 

        file = self.parameterAsFileOutput(parameters, self.OUTPUT_FILE, context)

        # If sink was not created, throw an exception to indicate that the algorithm
        # encountered a fatal error. The exception text can be any string, but in this
        # case we use the pre-built invalidSinkError method to return a standard
        # helper text for when a sink cannot be evaluated
        if file is None:
            raise QgsProcessingException(self.invalidSinkError(parameters, self.OUTPUT_FILE))

        # Send some information to the user
        feedback.pushInfo('CRS is {}'.format(source.sourceCrs().authid()))

        # Compute the number of steps to display within the progress bar and
        # get features from source
        total = 100.0 / source.featureCount() if source.featureCount() else 0
        features = source.getFeatures()
        
        # Init AGO Logic
        self.setLatLonStart(fields,feedback)
        self.setLocalMetersPerDegree(self.latStart)
        
        feedback.pushInfo("Reading Geometries...")
        vertexList = []
        for current, feature in enumerate(features):
            # Stop the algorithm if cancel button has been clicked
            if feedback.isCanceled():
                #break
                exit()

            # Get feature geometry
            if feature.hasGeometry():
                # TODO: Fehler wenn kein Qudarat/Rechteck!
                vertices = list(feature.geometry().vertices())
                vertexList.append(self.convertWGS84ToLocal(vertices[0].y(), vertices[0].x()))
                vertexList.append(self.convertWGS84ToLocal(vertices[3].y(), vertices[3].x()))
                vertexList.append(self.convertWGS84ToLocal(vertices[1].y(), vertices[1].x()))
                vertexList.append(self.convertWGS84ToLocal(vertices[2].y(), vertices[2].x()))
            
        # Remove duplicate vertices and split to patches
        # -> when printing two adjacent quadrats, the 2nd vertex of the first is identical to the 1st vertex of the second
        # -> and the 4th vertex of the first is identical to the 3rd of the second
        # 1--2 1--2 1--2
        # |  | |  | |  |
        # 3--4 3--4 3--4
        # => we want to remove the duplicates
        cleanVertexList = []
        patchList = []
        for i, vertex in enumerate(vertexList):
            if i < 4: # we take the first 4
                cleanVertexList.append(vertex)
            else: # checks start with the 5th vertex
                if i % 2: # we check only uneven indexes (5th, 7th, etc.)
                    if not vertex == vertexList[i-3]:
                        # not identical: store this as new patch
                        feedback.pushInfo(str(cleanVertexList))
                        feedback.pushInfo("----------------------")
                        patchList.append(cleanVertexList)
                        # Clear list
                        cleanVertexList.clear()
                        cleanVertexList.append(vertex)
                else: # even index, we use these
                    cleanVertexList.append(vertex)
        

        feedback.pushInfo("Writing Sections file...")
        with open(file, "w") as output_file:
            for patch in patchList:
                output_file.write(str(len(patch))+"\n")
                output_file.write('27,151,160' +"\n")
                for p in patch:
                    output_file.write(p +"\n")

        
        
        
        
        """
        feedback.pushInfo("Writing Sections file...")
        with open(file, "w") as output_file:
            for current, feature in enumerate(features):
                # Stop the algorithm if cancel button has been clicked
                if feedback.isCanceled():
                    #break
                    exit()

                # Get feature geometry
                if feature.hasGeometry():
                    
                    my_list = list(feature.geometry().vertices())
                    output_file.write('5' +"\n")
                    output_file.write('27,151,160' +"\n")    

                    s = self.convertWGS84ToLocal(my_list[0].y(), my_list[0].x())
                    output_file.write(s +"\n")
                    self.count = self.count + 1
                    
                    s = self.convertWGS84ToLocal(my_list[3].y(), my_list[3].x())
                    output_file.write(s +"\n")
                    self.count = self.count + 1
                    
                    s = self.convertWGS84ToLocal(my_list[1].y(), my_list[1].x())
                    output_file.write(s +"\n")
                    self.count = self.count + 1
                    
                    s = self.convertWGS84ToLocal(my_list[2].y(), my_list[2].x())
                    output_file.write(s +"\n")
                    self.count = self.count + 1

                # Update the progress bar
                feedback.setProgress(int(current * total))
                
            #output_file.write(str(self.count))
        """

        # To run another Processing algorithm as part of this algorithm, you can use
        # processing.run(...). Make sure you pass the current context and feedback
        # to processing.run to ensure that all temporary layer outputs are available
        # to the executed algorithm, and that the executed algorithm can send feedback
        # reports to the user (and correctly handle cancellation and progress reports!)
        if False:
            buffered_layer = processing.run("native:buffer", {
                'INPUT': dest_id,
                'DISTANCE': 1.5,
                'SEGMENTS': 5,
                'END_CAP_STYLE': 0,
                'JOIN_STYLE': 0,
                'MITER_LIMIT': 2,
                'DISSOLVE': False,
                'OUTPUT': 'memory:'
            }, context=context, feedback=feedback)['OUTPUT']

        # Return the results of the algorithm. In this case our only result is
        # the feature sink which contains the processed features, but some
        # algorithms may return multiple feature sinks, calculated numeric
        # statistics, etc. These should all be included in the returned
        # dictionary, with keys matching the feature corresponding parameter
        # or output names.
        return {self.OUTPUT_FILE: file}

    def setLatLonStart(self,pathToFieldsFile, feedback):
        # read file
        with open(pathToFieldsFile, "r") as field:
            lines = field.readlines()
        # Search StartFix line
        i = lines.index("StartFix\n")
        # get value
        startfix = lines[i+1]
        feedback.pushInfo(f"StartFix from Fields:{startfix}")
        # set latStart and lonStart
        latlon = startfix.split(",")
        self.latStart = float(latlon[0])
        self.lonStart = float(latlon[1])
        feedback.pushInfo(f"LatStart is {self.latStart} and LonStart is {self.lonStart}")

    def setLocalMetersPerDegree(self, latStart):
        self.mPerDegreeLat = 111132.92 - 559.82 * cos(2.0 * latStart * 0.01745329251994329576923690766743) + 1.175 * cos(
            4.0 * latStart * 0.01745329251994329576923690766743) - 0.0023 * cos(6.0 * latStart * 0.01745329251994329576923690766743)
    
        self.mPerDegreeLon = 111412.84 * cos(latStart * 0.01745329251994329576923690766743) - 93.5 * cos(
            3.0 * latStart * 0.01745329251994329576923690766743) + 0.118 * cos(5.0 * latStart * 0.01745329251994329576923690766743)

        
    def convertWGS84ToLocal(self,Lat, Lon) -> str:

        self.mPerDegreeLon = 111412.84 * cos(Lat * 0.01745329251994329576923690766743) - 93.5 * cos(3.0 * Lat * 0.01745329251994329576923690766743)
        + 0.118 * cos(5.0 * Lat * 0.01745329251994329576923690766743)

        Northing = (Lat - self.latStart) * self.mPerDegreeLat
        Easting = (Lon - self.lonStart) * self.mPerDegreeLon
    
        return(str(round(Easting,3)) + "," + str(round(Northing,3)) + ",0")