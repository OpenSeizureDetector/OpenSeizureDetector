#!/usr/bin/python
#
#############################################################################
#
# Copyright Graham Jones, December 2013 
#
#  Original version by Joseph Howse in his book, "OpenCV Computer Vision with 
#      Python" (Packt Publishing, 2013).
#      http://nummist.com/opencv/
#      http://www.packtpub.com/opencv-computer-vision-with-python/book
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
# This uses the bottle framework to make a simple web server
#
import time
import bottle
#from bottle import route
from threading import Thread

# This trick is taken from http://stackoverflow.com/questions/8725605/
#               bottle-framework-and-oop-using-method-instead-of-function

class benWebServer():
    def __init__(self,bf):
        self.bf = bf  # benFinder class instance that we will control.
        server = Thread(target = bottle.run, kwargs={'host':'0.0.0.0','port':8080})
        server.setDaemon(True)
        server.start()

    def index(self):
        return "ok"


    def getFPS(self):
        print "getFPS"
        return "%3.1f" % self.bf.fps

    def getnPeaks(self):
        return "%d" % self.bf.nPeaks

    def getTs_time(self):
        return "%3.1f" % self.bf.ts_time

    def getRate(self):
        return "%3.1f " % self.bf.rate

    def getRawImg(self):
        fname = self.bf.rawImgFname
        return self.serveStatic(fname)

    def getMaskedImg(self):
        fname = self.bf.maskedImgFname
        return self.serveStatic(fname)

    def getChartImg(self):
        fname = self.bf.chartImgFname
        return self.serveStatic(fname)

    def staticFiles(self,filepath):
        """ Used to serve the static files from the /static path"""
        print filepath
        return self.serveStatic(filepath,True)

    def serveStatic(self,fname, static=False):
        if static:
            rootPath = './www'
        else:
            rootPath = '.'
        return bottle.static_file(fname,root=rootPath)


def setRoutes(app):
    """Initialise the web server routing - must be called with app set to be
    an instance of benWebServer.
    """
    bottle.route("/")(app.index)
    bottle.route("/static/<filepath:path>")(app.staticFiles)
    bottle.route("/fps")(app.getFPS)
    bottle.route("/ts_time")(app.getTs_time)
    bottle.route("/nPeaks")(app.getnPeaks)
    bottle.route("/rate")(app.getRate)    
    bottle.route("/rawImg")(app.getRawImg)    
    bottle.route("/maskedImg")(app.getMaskedImg)
    bottle.route("/chartImg")(app.getChartImg)
