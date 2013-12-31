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
        self._curveFilter = filters.BGRPortraCurveFilter()
        self.background_video_img = None
        self.background_depth_img = None
    
    def loadBackgroundImages(self):
        self.background_video_img = cv2.imread(BenFinder.BACKGROUND_VIDEO_FNAME)
        self.background_depth_img = cv2.imread(BenFinder.BACKGROUND_DEPTH_FNAME,
                                               cv2.CV_LOAD_IMAGE_GRAYSCALE)
        cv2.imshow("background_video_img",self.background_video_img)
        cv2.imshow("background_depth_img",self.background_depth_img)

    def run(self):
        """Run the main loop."""
        self._windowManager.createWindow()
        while self._windowManager.isWindowCreated:
            self._captureManager.enterFrame()

            frame = self._captureManager.frame
            
            if frame is not None:
                if (self._backgroundSubtract):
                    if (self._captureManager.channel == \
                        depth.CV_CAP_OPENNI_DEPTH_MAP):
                        cv2.absdiff(frame,self.background_depth_img,frame)
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
        b      -> toggle background subtraction on or off.
        s      -> Save current frame as background image.
        d      -> Toggle between video and depth map view
        escape -> Quit.
        
        """
        print "keycode=%d" % keycode
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
        elif (chr(keycode)=='s'):
            print "Saving Background Image"
            if (self._captureManager.channel == depth.CV_CAP_OPENNI_DEPTH_MAP):
                self._captureManager.writeImage(BenFinder.BACKGROUND_DEPTH_FNAME)
            elif (self._captureManager.channel == depth.CV_CAP_OPENNI_BGR_IMAGE):
                self._captureManager.writeImage(BenFinder.BACKGROUND_VIDEO_FNAME)
            else:
                print "Invalid Channel %d - doing nothing!" \
                    % self._captureManager.channel
        elif (chr(keycode)=='b'):  # Background subtraction
            if (self._backgroundSubtract == True):
                print "Switching off background Subtraction"
                self._backgroundSubtract = False
            else:
                print "Switching on background subtraction"
                self.loadBackgroundImages()
                self._backgroundSubtract = True
                

        elif keycode == 27: # escape
            self._windowManager.destroyWindow()


if __name__=="__main__":
    BenFinder().run() 
