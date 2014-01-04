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

import cv2
import numpy
import depth
import filters
from managers import WindowManager, CaptureManager
import rects
from trackers import FaceTracker
from timeSeries import TimeSeries

class BenFinder(object):
    BACKGROUND_VIDEO_FNAME = "background_video.png"
    BACKGROUND_DEPTH_FNAME = "background_depth.png"
 
    def __init__(self):
        self._windowManager = WindowManager('benFinder',
                                             self.onKeypress)
        device = depth.CV_CAP_FREENECT
        #device = 1
        print "device=%d" % device
        self._captureManager = CaptureManager(
            device, self._windowManager, True)
        self._captureManager.channel = depth.CV_CAP_OPENNI_BGR_IMAGE
        self._faceTracker = FaceTracker()
        self._shouldDrawDebugRects = False
        self._backgroundSubtract = False
        self._autoBackgroundSubtract = False
        self._curveFilter = filters.BGRPortraCurveFilter()
        self.background_video_img = None
        self.background_depth_img = None
        self.autoBackgroundImg = None
        self._ts = TimeSeries()
        self._frameCount = 0
    
    def loadBackgroundImages(self):
        """ Load the background images to be used for background subtraction
        from disk files.
        """
        self.background_video_img = cv2.imread(BenFinder.BACKGROUND_VIDEO_FNAME)
        self.background_depth_img = cv2.imread(BenFinder.BACKGROUND_DEPTH_FNAME,
                                               cv2.CV_LOAD_IMAGE_GRAYSCALE)

    def showBackgroundImage(self):
        """ Display the background image used for subtraction in a separate window
        """
        # Load the images from disk if necessary.
        if (not self.background_depth_img or not self.background_video_img):
            self.loadBackgroundImages()
        # Display the correct image
        if (self._autoBackgroundSubtract):
            cv2.imshow("Auto Background Image", self.autoBackgroundImg)
        else:
            if (self._captureManager.channel == \
                depth.CV_CAP_OPENNI_DEPTH_MAP):
                cv2.imshow("background_depth_img",self.background_depth_img)
            elif (self._captureManager.channel == \
                  depth.CV_CAP_OPENNI_BGR_IMAGE):
                cv2.imshow("background_video_img",self.background_video_img)
            else:
                print "Error - Invalid Channel %d." % \
                    self._captureManager.channel

    def run(self):
        """Run the main loop."""
        self._windowManager.createWindow()
        while self._windowManager.isWindowCreated:
            self._captureManager.enterFrame()

            frame = self._captureManager.frame
            
            if frame is not None:
                if (self._backgroundSubtract):
                    if (self._autoBackgroundSubtract):
                        if (self._captureManager.channel == \
                            depth.CV_CAP_OPENNI_DEPTH_MAP):
                            if (self.autoBackgroundImg == None):
                                self.autoBackgroundImg = numpy.float32(frame)
                            # First work out the region of interest by 
                            #    subtracting the fixed background image 
                            #    to create a mask.
                            absDiff = cv2.absdiff(frame,self.background_depth_img)
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
                            if (self._frameCount < 15):
                                self._frameCount = self._frameCount +1
                            else:
                                self._ts.plotRawData()
                                self._ts.findPeaks()
                                self._frameCount = 0
                        else:
                            print "Auto background subtract only works for depth images!"
                    else:
                        if (self._captureManager.channel == \
                            depth.CV_CAP_OPENNI_DEPTH_MAP):
                            cv2.absdiff(frame,self.background_depth_img,frame)
                            benMask = filters.getBenMask(frame,8)
                            bri = filters.getMean(frame,benMask)
                            print bri
                        elif (self._captureManager.channel == \
                              depth.CV_CAP_OPENNI_BGR_IMAGE):
                            cv2.absdiff(frame,self.background_video_img,frame)
                        else:
                            print "Error - Invalid Channel %d." % \
                                self._captureManager.channel
                    #ret,frame = cv2.threshold(frame,200,255,cv2.THRESH_TOZERO)
                #self._faceTracker.update(frame)
                #faces = self._faceTracker.faces

                #if self._shouldDrawDebugRects:
                #    self._faceTracker.drawDebugRects(frame)
                            
            self._captureManager.exitFrame()
            self._windowManager.processEvents()
    
    def onKeypress(self, keycode):
        """Handle a keypress.
        
        space  -> Take a screenshot.
        tab    -> Start/stop recording a screencast.
        x      -> Start/stop drawing debug rectangles around faces.
        a      -> toggle automatic accumulated background subtraction on or off.
        b      -> toggle simple background subtraction on or off.
        s      -> Save current frame as background image.
        d      -> Toggle between video and depth map view
        i      -> Display the background image that is being used for subtraction.
        escape -> Quit.
        
        """
        print "keycode=%d" % keycode
        if keycode == 32: # space
            self._captureManager.writeImage('screenshot.png')
        elif keycode == 9: # tab
            if not self._captureManager.isWritingVideo:
                print "Starting Video Recording..."
                self._captureManager.startWritingVideo(
                    'screencast.avi')
            else:
                print "Stopping video recording"
                self._captureManager.stopWritingVideo()
        elif keycode == 120: # x
            self._shouldDrawDebugRects = \
                not self._shouldDrawDebugRects
        elif (chr(keycode)=='a'):  # Autometic background subtraction
            if (self._autoBackgroundSubtract == True):
                print "Switching off auto background Subtraction"
                self.autoBackgroundImage = None
                self._autoBackgroundSubtract = False
            else:
                print "Switching on auto background subtraction"
                self._autoBackgroundSubtract = True
        elif (chr(keycode)=='b'):  # Simple background subtraction
            if (self._backgroundSubtract == True):
                print "Switching off background Subtraction"
                self._backgroundSubtract = False
            else:
                print "Switching on background subtraction"
                self.loadBackgroundImages()
                self._backgroundSubtract = True
        elif (chr(keycode)=='d'):
            if (self._captureManager.channel == depth.CV_CAP_OPENNI_BGR_IMAGE):
                print "switching to depth map..."
                self._captureManager.channel = depth.CV_CAP_OPENNI_DEPTH_MAP
            else:
                print "switching to video"
                self._captureManager.channel = depth.CV_CAP_OPENNI_BGR_IMAGE
        elif (chr(keycode)=='i'):
            self.showBackgroundImage()
        elif (chr(keycode)=='s'):
            print "Saving Background Image"
            if (self._captureManager.channel == depth.CV_CAP_OPENNI_DEPTH_MAP):
                self._captureManager.writeImage(BenFinder.BACKGROUND_DEPTH_FNAME)
            elif (self._captureManager.channel == depth.CV_CAP_OPENNI_BGR_IMAGE):
                self._captureManager.writeImage(BenFinder.BACKGROUND_VIDEO_FNAME)
            else:
                print "Invalid Channel %d - doing nothing!" \
                    % self._captureManager.channel
                

        elif keycode == 27: # escape
            self._windowManager.destroyWindow()


if __name__=="__main__":
    BenFinder().run() 
