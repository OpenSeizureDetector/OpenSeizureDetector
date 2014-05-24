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
import json
import bottle
#from bottle import route
from threading import Thread
import httplib2                     # Needed to communicate with camera


# This trick is taken from http://stackoverflow.com/questions/8725605/
#               bottle-framework-and-oop-using-method-instead-of-function

class benWebServer():
    def __init__(self,bf):
        self._bf = bf  # benFinder class instance that we will control.
        self._chartImgFname = None
        self._backgroundImgFname = None
        self._rawImgFname = None
        self._maskedImgFname = None
        self._dataFname = None
        self._analysResults = None

        server = Thread(target = bottle.run, kwargs={'server':'cherrypy','host':'0.0.0.0','port':8080})
        server.setDaemon(True)
        server.start()

    def setChartImg(self,fname):
        self._chartImgFname = fname

    def setBgImg(self,fname):
        self._backgroundImgFname = fname

    def saveBgImg(self):
        self._bf.saveBgImg()
        bottle.redirect("/")

    def setRawImg(self,fname):
        self._rawImgFname = fname

    def setMaskedImg(self,fname):
        self._maskedImgFname = fname

    def setDataFname(self,fname):
        self._dataFname = fname

    def setAnalysisResults(self,resultsDict):
        self._analysisResults = resultsDict

    def index(self):
        bottle.redirect("/static/index.html")
        #return "ok"

    def getJSONData(self):
        return json.dumps(self._analysisResults)

    def getFPS(self):
        print "getFPS"
        return "%3.1f" % self.analysisResults['fps']

    def getnPeaks(self):
        return "df" % self.analysisResults['nPeaks']

    def getTs_time(self):
        return "%3.1f" % self.analysisResults['ts_time']

    def getRate(self):
        return "%3.1f" % self.analysisResults['rate']

    def getBri(self):
        return "%3.1f" % self.analysisResults['bri']

    def getRawImg(self):
        fname = self._rawImgFname
        return bottle.static_file(fname,root='/')

    def getMaskedImg(self):
        fname = self._maskedImgFname
        return bottle.static_file(fname,root='/')

    def getChartImg(self):
        fname = self._chartImgFname
        return bottle.static_file(fname,root='/')

    def getBgImg(self):
        fname = self._backgroundImgFname
        return bottle.static_file(fname,root='/')

    def moveCamera(self):
        retStr="moveCamera:"
        pos=int(bottle.request.query.pos)
        h = httplib2.Http(".cache")
        retStr = "%s, uname=%s, passwd=%s" % (retStr,
                                              self._bf.cfg.getConfigStr('camuname'), 
                                              self._bf.cfg.getConfigStr('campasswd'))
        h.add_credentials(self._bf.cfg.getConfigStr('camuname'), 
                          self._bf.cfg.getConfigStr('campasswd'))
        resp, content = h.request("%s/%s%d" % (self._bf.cfg.getConfigStr('camaddr'),
                                               self._bf.cfg.getConfigStr('cammoveurl'),
                                               pos),"GET")
        retStr = "%s \nMoved to Preset %d,\nContent=%s" % (retStr,pos,content)
        bottle.redirect("/")
        return retStr

    def getCamImg(self):
        retStr="getCamImg:"
        h = httplib2.Http(".cache")
        retStr = "%s, uname=%s, passwd=%s" % (retStr,
                                              self._bf.cfg.getConfigStr('camuname'), 
                                              self._bf.cfg.getConfigStr('campasswd'))
        h.add_credentials(self._bf.cfg.getConfigStr('camuname'), 
                          self._bf.cfg.getConfigStr('campasswd'))
        resp, content = h.request("%s/%s" % (self._bf.cfg.getConfigStr('camaddr'),
                                               self._bf.cfg.getConfigStr('camimgurl')
                                               ),"GET")
        bottle.response.content_type = 'image/jpeg'
        return content




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
    bottle.route("/jsonData")(app.getJSONData)
    bottle.route("/fps")(app.getFPS)
    bottle.route("/ts_time")(app.getTs_time)
    bottle.route("/nPeaks")(app.getnPeaks)
    bottle.route("/rate")(app.getRate)    
    bottle.route("/rawImg")(app.getRawImg)    
    bottle.route("/maskedImg")(app.getMaskedImg)
    bottle.route("/saveBgImg")(app.saveBgImg)
    bottle.route("/chartImg")(app.getChartImg)
    bottle.route("/moveCamera")(app.moveCamera)
    bottle.route("/getCamImg")(app.getCamImg)
