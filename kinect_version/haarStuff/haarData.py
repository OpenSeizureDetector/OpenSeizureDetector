#!/usr/bin/python
#
#############################################################################
#
# Copyright Graham Jones, June 2014 
#
#############################################################################
#
#   This file is part of OpenSeizureDetector.
#
#    OpenSeizureDetector is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Foobar is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with OpenSeizureDetector.  If not, see <http://www.gnu.org/licenses/>.
##############################################################################
#
import os

class HaarTarget:
    POS_FOLDER_NAME = "posImg"
    POS_FILE_NAME = "positive.txt"
    NEG_FOLDER_NAME = "negImg"
    NEG_FILE_NAME = "negative.txt"
    negImages = []  # simple list of positive images
    posImages = []  # list of lists (fname (targetlist))
                    # where targetlist is a list of tuples (x,y,width,height)
    def __init__(self,haarDataDir):
        print "HaarTarget.__init__(%s)" % haarDataDir
        self.haarDataDir = haarDataDir

        posFolder = os.path.join(self.haarDataDir,self.POS_FOLDER_NAME)
        self.initPosImages(posFolder)
        negFolder = os.path.join(self.haarDataDir,self.NEG_FOLDER_NAME)
        self.initNegImages(negFolder)
        


    def initPosImages(self,posFolder):
        print "HaarTarget.initPosImages(%s)" % posFolder
        self.makeDirectoryIfRequired(posFolder)
        posFname = os.path.join(posFolder,self.POS_FILE_NAME)
        print posFname

    def initNegImages(self,negFolder):
        print "HaarTarget.initNegImages(%s)" % negFolder
        self.makeDirectoryIfRequired(negFolder)
        negFname = os.path.join(negFolder,self.NEG_FILE_NAME)
        print negFname

        if (os.path.isfile(negFname)):
            negFile = open(negFname,"r")
            if (negFile):
                lines = negFile.read().splitlines()
                for lineStr in lines:
                    print lineStr
                    self.negImages.append(lineStr)
            else:
                print "failed to open %s for reading" % negFname
        else:
            print "%s does not exist" % negFname

        print self.negImages

    def makeDirectoryIfRequired(self,dirPath):
        if not os.path.isdir(dirPath):
            print "Creating folder %s" % dirPath
            os.makedirs(dirPath)
        else:
            print "Folder %s already exists" % dirPath


        

class HaarData:
    '''High level class to describe a set of haar cascade training data.
    it contains data for any number of target objects, each of which is
    described by an object of the HaarTarget class
    '''
    def __init__(self,dataDir="."):
        print dataDir
        self.targets = {}
        self.initFromDir(dataDir)


    def initFromDir(self,dataDir):
        '''(re)Initialise the haar data store from the data in file directory
        dataDir.
        It expects to find folder for each target object.
        for each object there should be two folders 'posImg' and 'negImg' that
        contains the positive and negative image files for the object, and the
        text files expected by opencv_createsamples
        '''
        self.dataDir = dataDir
        fileList = os.listdir(self.dataDir)
        for fname in fileList:
            if os.path.isdir(fname):
                print "found directory %s" % fname
                self.targets[fname] = HaarTarget(fname)



if __name__ == "__main__":
    haarData = HaarData(".")
    #haarData.printSummmary()

