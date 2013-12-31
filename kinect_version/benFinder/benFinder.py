#!/usr/bin/python
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
#
# Graham Jones, December 2013 (based on Joseph Howse's 'OpenCV Computer Vision 
#    with Python' book - 
#    http://www.packtpub.com/opencv-computer-vision-with-python/book)
#
#


import cv2
import depth
import filters
from managers import WindowManager, CaptureManager
import rects
from trackers import FaceTracker

class BenFinder(object):
    
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
        self._curveFilter = filters.BGRPortraCurveFilter()
    
    def run(self):
        """Run the main loop."""
        self._windowManager.createWindow()
        while self._windowManager.isWindowCreated:
            self._captureManager.enterFrame()

            frame = self._captureManager.frame
            
            if frame is not None:
                self._faceTracker.update(frame)
                faces = self._faceTracker.faces

                if self._shouldDrawDebugRects:
                    self._faceTracker.drawDebugRects(frame)
                            
            self._captureManager.exitFrame()
            self._windowManager.processEvents()
    
    def onKeypress(self, keycode):
        """Handle a keypress.
        
        space  -> Take a screenshot.
        tab    -> Start/stop recording a screencast.
        x      -> Start/stop drawing debug rectangles around faces.
        d      -> Toggle between video and depth map view
        escape -> Quit.
        
        """
        if keycode == 32: # space
            self._captureManager.writeImage('screenshot.png')
        elif keycode == 9: # tab
            if not self._captureManager.isWritingVideo:
                self._captureManager.startWritingVideo(
                    'screencast.avi')
            else:
                self._captureManager.stopWritingVideo()
        elif keycode == 120: # x
            self._shouldDrawDebugRects = \
                not self._shouldDrawDebugRects
        elif (chr(keycode)=='d'):
            if (self._captureManager.channel == depth.CV_CAP_OPENNI_BGR_IMAGE):
                print "switching to depth map..."
                self._captureManager.channel = depth.CV_CAP_OPENNI_DEPTH_MAP
            else:
                print "switching to video"
                self._captureManager.channel = depth.CV_CAP_OPENNI_BGR_IMAGE

        elif keycode == 27: # escape
            self._windowManager.destroyWindow()


if __name__=="__main__":
    BenFinder().run() 
