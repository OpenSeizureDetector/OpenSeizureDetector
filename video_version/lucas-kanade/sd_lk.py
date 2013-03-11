#! /usr/bin/env python
# Investigating using the lucas-kanade motion tracking method for the 
# seizure detector.
# Based on the lkdemo.py file provided with openCV

print "Lucas-Kanade version of seizure detector"

import sys
import datetime

# import the necessary things for OpenCV
import cv2.cv as cv

win_size = 10
MAX_COUNT = 50
Analysis_Period =5 # Seconds


image = None
pt = None
flags = 0
need_to_init = True
last_analysis_time = None

def initFeatures():
    print "initFeatures()"
    eig = cv.CreateImage (cv.GetSize (grey), 32, 1)
    temp = cv.CreateImage (cv.GetSize (grey), 32, 1)

    # the default parameters
    # quality = 0.01
    quality = 0.2
    min_distance = 10

    # search the good points
    features = cv.GoodFeaturesToTrack (
        grey, eig, temp,
        MAX_COUNT,
        quality, min_distance, None, 3, 0, 0.04)

    # refine the corner locations
    features = cv.FindCornerSubPix (
        grey,
        features,
        (win_size, win_size),  (-1, -1),
        (cv.CV_TERMCRIT_ITER | cv.CV_TERMCRIT_EPS, 20, 0.03))

    return features

def ptptdist(p0, p1):
    """ Return the distance^2 between two points. """
    dx = p0[0] - p1[0]
    dy = p0[1] - p1[1]
    return dx**2 + dy**2


def doAnalysis():
    print "doAnalysis()"

if __name__ == '__main__':
    cv.NamedWindow ('Seizure_Detector', cv.CV_WINDOW_AUTOSIZE)

    camera = cv.CaptureFromFile("rtsp://192.168.1.18/live_mpeg4.sdp")
    #camera = cv.CaptureFromFile("../testcards/sample1.mp4")
    #camera = cv.CaptureFromCAM(0)

    last_analysis_time = datetime.datetime.now()

    while 1:
        frame = cv.QueryFrame(camera)

        if image is None:
            image = cv.CreateImage (cv.GetSize (frame), 8, 3)
            image.origin = frame.origin
            grey = cv.CreateImage (cv.GetSize (frame), 8, 1)
            prev_grey = cv.CreateImage (cv.GetSize (frame), 8, 1)
            pyramid = cv.CreateImage (cv.GetSize (frame), 8, 1)
            prev_pyramid = cv.CreateImage (cv.GetSize (frame), 8, 1)
            features = []

        cv.Copy (frame, image)

        # create a grey version of the image
        cv.CvtColor (image, grey, cv.CV_BGR2GRAY)

        if ((datetime.datetime.now() - last_analysis_time).total_seconds() 
            > Analysis_Period):
            doAnalysis()
            features = initFeatures()
            last_analysis_time = datetime.datetime.now()

        if need_to_init:
            features = initFeatures()

        elif features != []:
            # we have points to track, so display them
            features, status, track_error = cv.CalcOpticalFlowPyrLK (
                prev_grey, grey, prev_pyramid, pyramid,
                features,
                (win_size, win_size), 3,
                (cv.CV_TERMCRIT_ITER|cv.CV_TERMCRIT_EPS, 20, 0.03),
                flags)

            # set back the points we keep
            features = [ p for (st,p) in zip(status, features) if st]

            # draw the points as green circles
            for the_point in features:
                cv.Circle (image, (int(the_point[0]), int(the_point[1])), 3, (0, 255, 0, 0), -1, 8, 0)
            
        # swapping
        prev_grey, grey = grey, prev_grey
        prev_pyramid, pyramid = pyramid, prev_pyramid
        need_to_init = False
        
        # we can now display the image
        cv.ShowImage ('Seizure_Detector', image)

        # handle events
        c = cv.WaitKey(10)

        if c == 27:
            # user has press the ESC key, so exit
            break


