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
import os
import cv2
import numpy
import depth
import filters
from managers import WindowManager, CaptureManager
import rects
from timeSeries import TimeSeries
from config_utils import ConfigUtil

class BenFinder(object):
    configFname = "config.ini"
    configSection = "benFinder"
 
    def __init__(self,save=False):
        configPath = "%s/%s" % (os.path.dirname(os.path.realpath(__file__)),
                                self.configFname)
        print configPath
        self.cfg = ConfigUtil(configPath,self.configSection)

        self.debug = self.cfg.getConfigBool("debug")
        if (self.debug): print "Debug Mode"

        device = depth.CV_CAP_FREENECT
        self._captureManager = CaptureManager(
            device, None, True)
        self._captureManager.channel = depth.CV_CAP_OPENNI_DEPTH_MAP

        if (save):
            self.save()
        else:
            print "Loading background image %s." % \
                self.cfg.getConfigStr("background_depth")
            self._background_depth_img = cv2.imread(
                self.cfg.getConfigStr("background_depth"),
                cv2.CV_LOAD_IMAGE_GRAYSCALE)
            self.autoBackgroundImg = None
            self._ts = TimeSeries()
            self._frameCount = 0
            self.run()
    
    def run(self):
        """Run the main loop."""
        while(True):
            self._captureManager.enterFrame()

            frame = self._captureManager.frame
            
            if frame is not None:
                if (self.autoBackgroundImg == None):
                    self.autoBackgroundImg = numpy.float32(frame)
                rawFrame = frame
                # First work out the region of interest by 
                #    subtracting the fixed background image 
                #    to create a mask.
                absDiff = cv2.absdiff(frame,self._background_depth_img)
                benMask = filters.getBenMask(absDiff,8)

                cv2.accumulateWeighted(frame,
                                       self.autoBackgroundImg,
                                       0.05)
                # Convert the background image into the same format
                # as the main frame.
                bg = cv2.convertScaleAbs(self.autoBackgroundImg,
                                         alpha=1.0)
                # Subtract the background from the frame image
                cv2.absdiff(frame,bg,frame)
                # Scale the difference image to make it more sensitive
                # to changes.
                cv2.convertScaleAbs(frame,frame,alpha=100)
                #frame = cv2.bitwise_and(frame,frame,dst=frame,mask=benMask)
                frame = cv2.multiply(frame,benMask,dst=frame,dtype=-1)
                bri = filters.getMean(frame,benMask)
                #print "%4.0f, %3.0f" % (bri[0],self._captureManager.fps)
                self._ts.addSamp(bri[0])
                # Only do the analysis every 15 frames (0.5 sec)
                if (self._frameCount < 15):
                    self._frameCount = self._frameCount +1
                else:
                    nPeaks,ts_time,rate = self._ts.findPeaks()
                    print "%d peaks in %3.2f sec = %3.1f bpm" % \
                        (nPeaks,ts_time,rate)

                    self._ts.plotRawData(
                        file=True,
                        fname=self.cfg.getConfigStr("chart_fname"))
                    cv2.imwrite(self.cfg.getConfigStr(
                        "raw_image_fname"),
                        rawFrame)
                    cv2.imwrite(self.cfg.getConfigStr(
                        "masked_image_fname"),
                        frame)
                    self._frameCount = 0
            self._captureManager.exitFrame()


    def save(self):
        """ Write a new background image to the appropriate file location."""
        self._captureManager.enterFrame()
        print "Writing image to %s." % self.cfg.getConfigStr("background_depth")
        self._captureManager.writeImage(self.cfg.getConfigStr("background_depth"))
        self._captureManager.exitFrame()

    
if __name__=="__main__":
    # Boilerplate code from https://gist.github.com/ssokolow/151572
    from optparse import OptionParser
    parser = OptionParser(version="%%prog v%s" % __version__,
            usage="%prog [options] <argument> ...",
            description=__doc__.replace('\r\n', '\n').split('\n--snip--\n')[0])
    parser.add_option('-s', '--save', action="count", dest="save",
        default=0, help="Save a new background image.")
 
    opts, args  = parser.parse_args()
 
    
    if (opts.save):
        print "Saving new background Image"
        BenFinder(save=True)
        print "Done!"
    else:
        BenFinder(save=False)
