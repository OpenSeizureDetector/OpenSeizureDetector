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
import cv2
import numpy
import time
import depth
import freenect
import filters

class CaptureManager(object):
    
    def __init__(self, capture, previewWindowManager = None,
                 shouldMirrorPreview = False):
        
        self.previewWindowManager = previewWindowManager
        self.shouldMirrorPreview = shouldMirrorPreview

        if (capture!=depth.CV_CAP_FREENECT):
            self._capture = cv2.VideoCapture(capture)
            self._uselibFreenect = False
            print "Using OpenCV Video Capture"
        else:
            import freenect
            self._capture = None
            self._uselibFreenect = True
            print "Using libFreenect Video Capture"
        self._channel = 0
        self._enteredFrame = False
        self._frame = None
        self._imageFilename = None
        self._videoFilename = None
        self._videoEncoding = None
        self._videoWriter = None
        
        self._startTime = None
        self._framesElapsed = long(0)
        self._fpsEstimate = None    

    @property
    def channel(self):
        return self._channel
    
    @property
    def fps(self):
        return self._fpsEstimate

    @channel.setter
    def channel(self, value):
        if self._channel != value:
            self._channel = value
            self._frame = None
    
    @property
    def frame(self):
        """ Grab a frame and set it as self._frame.
        Modified by Graham Jones to use libFreenect for kinect depth sensor
        """
        if self._enteredFrame and self._frame is None:
            if (self._uselibFreenect):
                if (self.channel == depth.CV_CAP_OPENNI_BGR_IMAGE):
                    imgRGB, timestap = freenect.sync_get_video()
                    imgBGR = imgRGB # Create new image by copying original.
                    filters.bgr2rgb(imgRGB,imgBGR)
                    self._frame = imgBGR
                elif (self.channel == depth.CV_CAP_OPENNI_DEPTH_MAP):
                    depthMap, timestamp = freenect.sync_get_depth()
                    depthMap = depthMap.astype(numpy.uint8)
                    self._frame = depthMap
                else:
                    print "Error - Unrecognised channel %d." % self.channel
                    self._frame = None
            else:
                _, self._frame = self._capture.retrieve(channel = self.channel)
        return self._frame
    
    @property
    def isWritingImage(self):
        return self._imageFilename is not None
    
    @property
    def isWritingVideo(self):
        return self._videoFilename is not None
    
    def enterFrame(self):
        """Capture the next frame, if any.
        Modified by GJ to work with libFreenect kinect interface"""
        
        # But first, check that any previous frame was exited.
        assert not self._enteredFrame, \
            'previous enterFrame() had no matching exitFrame()'
        
        
        if (self._uselibFreenect):
            self._enteredFrame = True
        else:
            if self._capture is not None:
                self._enteredFrame = self._capture.grab()
    
    def exitFrame(self):
        """Draw to the window. Write to files. Release the frame."""
        
        # Check whether any grabbed frame is retrievable.
        # The getter may retrieve and cache the frame.
        if self.frame is None:
            self._enteredFrame = False
            return
        
        # Update the FPS estimate and related variables.
        if self._framesElapsed == 0:
            self._startTime = time.time()
        else:
            timeElapsed = time.time() - self._startTime
            self._fpsEstimate =  self._framesElapsed / timeElapsed
        self._framesElapsed += 1
        
        # Draw to the window, if any.
        if self.previewWindowManager is not None:
            if self.shouldMirrorPreview:
                mirroredFrame = numpy.fliplr(self._frame).copy()
                self.previewWindowManager.show(mirroredFrame)
            else:
                self.previewWindowManager.show(self._frame)
        
        # Write to the image file, if any.
        if self.isWritingImage:
            cv2.imwrite(self._imageFilename, self._frame)
            self._imageFilename = None
        
        # Write to the video file, if any.
        self._writeVideoFrame()
        
        # Release the frame.
        self._frame = None
        self._enteredFrame = False
    
    def writeImage(self, filename):
        """Write the next exited frame to an image file."""
        self._imageFilename = filename
    
    def startWritingVideo(
            self, filename,
            encoding = cv2.cv.CV_FOURCC('I','4','2','0')):
        """Start writing exited frames to a video file."""
        self._videoFilename = filename
        self._videoEncoding = encoding
    
    def stopWritingVideo(self):
        """Stop writing exited frames to a video file."""
        self._videoFilename = None
        self._videoEncoding = None
        self._videoWriter = None
    
    def _writeVideoFrame(self):
        
        if not self.isWritingVideo:
            return
        
        if self._videoWriter is None:
            if (self._capture):
                fps = self._capture.get(cv2.cv.CV_CAP_PROP_FPS)
            else:
                fps = -1.0;
            if fps <= 0.0:
                # The capture's FPS is unknown so use an estimate.
                if self._framesElapsed < 20:
                    # Wait until more frames elapse so that the
                    # estimate is more stable.
                    return
                else:
                    fps = self._fpsEstimate
            # if we are using an openCV image source, use that to get
            # the size of the images, otherwise look at the dimensions
            # of the current frame.
            if (self._capture):
                size = (int(self._capture.get(
                    cv2.cv.CV_CAP_PROP_FRAME_WIDTH)),
                        int(self._capture.get(
                            cv2.cv.CV_CAP_PROP_FRAME_HEIGHT)))
            else:
                print self._frame.shape
                size = (self._frame.shape[1],self._frame.shape[0])
            self._videoWriter = cv2.VideoWriter(
                self._videoFilename, self._videoEncoding,
                fps, size)
        # Convert depth maps into bgr - gray scale videos dont play!!
        if (self.channel == depth.CV_CAP_OPENNI_DEPTH_MAP):
                self._frame = cv2.cvtColor(self._frame,cv2.COLOR_GRAY2BGR)
        self._videoWriter.write(self._frame)


class WindowManager(object):
    
    def __init__(self, windowName, keypressCallback = None):
        self.keypressCallback = keypressCallback
        
        self._windowName = windowName
        self._isWindowCreated = False
    
    @property
    def isWindowCreated(self):
        return self._isWindowCreated
    
    def createWindow(self):
        cv2.namedWindow(self._windowName)
        self._isWindowCreated = True
    
    def show(self, frame):
        cv2.imshow(self._windowName, frame)
    
    def destroyWindow(self):
        cv2.destroyWindow(self._windowName)
        self._isWindowCreated = False
    
    def processEvents(self):
        keycode = cv2.waitKey(1)
        if self.keypressCallback is not None and keycode != -1:
            # Discard any non-ASCII info encoded by GTK.
            keycode &= 0xFF
            self.keypressCallback(keycode)
