#!/usr/bin/python
#
#############################################################################
#
# Copyright Graham Jones, 2013-2014
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
# benFinder - my first go at using a depth camera to find benjamin in his 
# bedroom - the first stage of looking for odd behaviour that could be
# indicative of a seizure.
#
# For this version we maintain a background depth image which is his 
# bedroom with no-one in it (just toys strewn around).
# The background image is subtracted from each frame, which should make it
# much easier to spot Benjamin if he is there!
#
##############################################################################
"""[application description here]"""
 
__appname__ = "benFinder"
__author__  = "Graham Jones"
__version__ = "0.1"
__license__ = "GNU GPL 3.0 or later"
import os, time
import cv2
import numpy
import depth
import filters
from managers import WindowManager, CaptureManager
import rects
import utils
from timeSeries import TimeSeries
from config_utils import ConfigUtil
import webServer

class BenFinder(object):
    configFname = "config.ini"
    configSection = "benFinder"

    ALARM_STATUS_OK = 0   # All ok, no alarms.
    ALARM_STATUS_WARN = 1 # Warning status
    ALARM_STATUS_FULL = 2 # Full alarm status. 
    ALARM_STATUS_NOT_FOUND = 3 # Benjamin not found in image 
                               # (area below config area_threshold parameter)

    def __init__(self,save=False, inFile = None):
        print "benFinder.__init__()"
        print os.path.realpath(__file__)
        configPath = "%s/%s" % (os.path.dirname(os.path.realpath(__file__)),
                                self.configFname)
        print configPath
        self.cfg = ConfigUtil(configPath,self.configSection)

        self.debug = self.cfg.getConfigBool("debug")
        if (self.debug): print "Debug Mode"

        self._wkdir = self.cfg.getConfigStr("working_directory")
        if (self.debug): print "working_directory=%s\n" % self._wkdir
        self._tmpdir = self.cfg.getConfigStr("tmpdir")
        if (self.debug): print "tmpdir=%s\n" % self._tmpdir


        # Check if we are running from live kinect or a file.
        if (inFile):
            device = depth.CV_CAP_FILE
        else:
            device = depth.CV_CAP_FREENECT

        # Initialise the captureManager
        self._captureManager = CaptureManager(
            device, None, True, inFile=inFile)
        self._captureManager.channel = depth.CV_CAP_OPENNI_DEPTH_MAP

        # If we are runnign from a file, use the first frame as the
        # background image.
        if (inFile):
            self.saveBgImg()

        # If we have asked to save the background image, do that, and exit,
        # otherwise initialise the seizure detector.
        if (save):
            self.saveBgImg()
        else:
            self.loadBgImg()
            self.autoBackgroundImg = None
            self._status = self.ALARM_STATUS_OK
            self._ts = TimeSeries(tslen=self.cfg.getConfigInt("timeseries_length"))
            self._frameCount = 0
            self._outputFrameCount = 0
            self._nPeaks = 0
            self._ts_time = 0
            self._rate = 0
            self._ws = webServer.benWebServer(self)
            self._ws.setBgImg("%s/%s" % (self._tmpdir,
                    self.cfg.getConfigStr("background_depth")))
            self._ws.setChartImg("%s/%s" % (self._tmpdir,
                    self.cfg.getConfigStr("chart_fname")))
            self._ws.setRawImg("%s/%s" % (self._tmpdir,
                    self.cfg.getConfigStr("raw_image_fname")))
            self._ws.setMaskedImg("%s/%s" % (self._tmpdir,
                    self.cfg.getConfigStr("masked_image_fname")))
            self._ws.setDataFname("%s/%s" % (self._tmpdir,
                    self.cfg.getConfigStr("data_fname")))
            self._ws.setAnalysisResults({})
            webServer.setRoutes(self._ws)
            self.run()
    
    def run(self):
        """Run the main loop."""
        while(True):
            self._captureManager.enterFrame()

            frame = self._captureManager.frame
            
            if frame is not None:
                if (self.autoBackgroundImg == None):
                    self.autoBackgroundImg = numpy.float32(frame)
                rawFrame = frame.copy()
                # First work out the region of interest by 
                #    subtracting the fixed background image 
                #    to create a mask.
                #print frame
                #print self._background_depth_img
                absDiff = cv2.absdiff(frame,self._background_depth_img)
                benMask,maskArea = filters.getBenMask(absDiff,8)

                cv2.accumulateWeighted(frame,
                                       self.autoBackgroundImg,0.05)
                # Convert the background image into the same format
                # as the main frame.
                #bg = self.autoBackgroundImg
                bg = cv2.convertScaleAbs(self.autoBackgroundImg,
                                         alpha=1.0)
                # Subtract the background from the frame image
                cv2.absdiff(frame,bg,frame)
                # Scale the difference image to make it more sensitive
                # to changes.
                cv2.convertScaleAbs(frame,frame,alpha=100)
                # Apply the mask so we only see the test subject.
                frame = cv2.multiply(frame,benMask,dst=frame,dtype=-1)

                if (maskArea <= self.cfg.getConfigInt('area_threshold')):
                    bri=(0,0,0)
                else:
                    # Calculate the brightness of the test subject.
                    bri = filters.getMean(frame,benMask)

                # Add the brightness to the time series ready for analysis.
                self._ts.addSamp(bri[0])
                self._ts.addImg(rawFrame)

                # Write timeseries to a file every 'output_framecount' frames.
                if (self._outputFrameCount >= self.cfg.getConfigInt('output_framecount')):
                    # Write timeseries to file
                    self._ts.writeToFile("%s/%s" % \
                        ( self.cfg.getConfigStr('output_directory'),
                          self.cfg.getConfigStr('ts_fname')
                      ))
                    self._outputFrameCount = 0
                else:
                    self._outputFrameCount = self._outputFrameCount + 1
                    

                # Only do the analysis every 15 frames (0.5 sec), or whatever
                # is specified in configuration file analysis_framecount
                # parameter.
                if (self._frameCount < self.cfg.getConfigInt('analysis_framecount')):
                    self._frameCount = self._frameCount +1
                else:
                    # Look for peaks in the brightness (=movement).
                    self._nPeaks,self._ts_time,self._rate = self._ts.findPeaks()
                    #print "%d peaks in %3.2f sec = %3.1f bpm" % \
                    #    (nPeaks,ts_time,rate)

                    oldStatus = self._status
                    if (maskArea > self.cfg.getConfigInt('area_threshold')):
                        # Check for alarm levels
                        if (self._rate > self.cfg.getConfigInt(
                                "rate_warn")):
                            self._status= self.ALARM_STATUS_OK
                        elif (self._rate > self.cfg.getConfigInt(
                                "rate_alarm")):
                            self._status= self.ALARM_STATUS_WARN
                        else:
                            self._status= self.ALARM_STATUS_FULL
                    else:
                        self._status = self.ALARM_STATUS_NOT_FOUND


                    if (oldStatus == self.ALARM_STATUS_OK and
                        self._status == self.ALARM_STATUS_WARN) or \
                        (oldStatus == self.ALARM_STATUS_WARN and 
                         self._status == self.ALARM_STATUS_FULL):
                                # Write timeseries to file
                                self._ts.writeToFile("%s/%s" % \
                                    ( self.cfg.getConfigStr('output_directory'),
                                      self.cfg.getConfigStr('alarm_ts_fname')
                                  ),bgImg=self._background_depth_img)
                        

                    # Collect the analysis results together and send them
                    # to the web server.
                    resultsDict = {}
                    resultsDict['fps'] = "%3.0f" % self.fps
                    resultsDict['bri'] = "%4.0f" % self._ts.mean
                    resultsDict['area'] = "%6.0f" % maskArea
                    resultsDict['nPeaks'] = "%d" % self._nPeaks
                    resultsDict['ts_time'] = self._ts_time
                    resultsDict['rate'] = "%d" % self._rate
                    resultsDict['time_t'] = time.ctime()
                    resultsDict['status'] = self._status
                    self._ws.setAnalysisResults(resultsDict)

                    # Write the results to file as a json string
                    utils.writeJSON(resultsDict,"%s/%s" % \
                                    (self._tmpdir,
                                     self.cfg.getConfigStr("data_fname")))
                    utils.writeLog(resultsDict,"%s/%s" % \
                                    (self._tmpdir,
                                     "benFinder_alarms.log"))
                    # Plot the graph of brightness, and save the images
                    # to disk.
                    self._ts.plotRawData(
                        file=True,
                        fname="%s/%s" % \
                        (self._tmpdir,self.cfg.getConfigStr("chart_fname")))
                        
                    cv2.imwrite("%s/%s" % (self._tmpdir,
                                           self.cfg.getConfigStr(
                                               "raw_image_fname")),
                                rawFrame)
                    cv2.imwrite("%s/%s" % (self._tmpdir,self.cfg.getConfigStr(
                        "masked_image_fname")),
                        frame)
                    self._frameCount = 0
            else:
                print "Null frame received - assuming end of file and exiting"
                break
            self._captureManager.exitFrame()
                

    @property
    def fps(self):
        return self._captureManager.fps

    @property
    def nPeaks(self):
        return self._nPeaks

    @property
    def ts_time(self):
        return self._ts_time

    @property
    def rate(self):
        return self._rate

    @property
    def rawImgFname(self):
        return self.cfg.getConfigStr("raw_image_fname")

    @property
    def maskedImgFname(self):
        return self.cfg.getConfigStr("masked_image_fname")

    @property
    def chartImgFname(self):
        return self.cfg.getConfigStr("chart_fname")

    def saveBgImg(self):
        """ Write a new background image to the appropriate file location."""
        if (self._captureManager.hasEnteredFrame):
            self._captureManager.exitFrame()
        self._captureManager.enterFrame()
        print "Writing image to %s." % self.cfg.getConfigStr("background_depth")
        self._captureManager.writeImage("%s/%s" % 
                                        (self._wkdir,
                                         self.cfg.getConfigStr("background_depth")
                                     ))
        print self._captureManager.frame
        print self._captureManager.frame.dtype
        self._captureManager.exitFrame()
        self.loadBgImg()

    def loadBgImg(self):
        print "Loading background image %s/%s." % \
            (self._wkdir,self.cfg.getConfigStr("background_depth"))
        self._background_depth_img = cv2.imread("%s/%s" % \
                    (self._wkdir,self.cfg.getConfigStr("background_depth")),
                                                cv2.CV_LOAD_IMAGE_GRAYSCALE)
        #                                        cv2.CV_LOAD_IMAGE_UNCHANGED)
        print self._background_depth_img
        print self._background_depth_img.dtype

    
if __name__=="__main__":
    # Boilerplate code from https://gist.github.com/ssokolow/151572
    from optparse import OptionParser
    parser = OptionParser(version="%%prog v%s" % __version__,
            usage="%prog [options] <argument> ...",
            description=__doc__.replace('\r\n', '\n').split('\n--snip--\n')[0])
    parser.add_option('-s', '--save', action="count", dest="save",
        default=0, help="Save a new background image.")
    parser.add_option('-f', '--file', dest="fname",
        help="Save a new background image.")
 
    opts, args  = parser.parse_args()
 
    print opts
    print args
    
    if (opts.save):
        print "Saving new background Image"
        BenFinder(save=True)
        print "Done!"
    elif (opts.fname):
        print "Running from file (not live kinect)"
        BenFinder(inFile=opts.fname)
    else:
        BenFinder(save=False)
