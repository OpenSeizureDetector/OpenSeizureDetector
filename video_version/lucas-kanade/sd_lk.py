#!/usr/bin/env python
# Investigating using the lucas-kanade motion tracking method for the 
# seizure detector.
# Based on the lkdemo.py file provided with openCV

print "Lucas-Kanade version of seizure detector"

import sys
import datetime
import math
import numpy, scipy, scipy.fftpack
import pylab

# import the necessary things for OpenCV
import cv2.cv as cv

inputfps = 30  # limit sample rate to 30 fps.
outputfps = inputfps
win_size = 10
MAX_COUNT = 30
Analysis_Period =2 # Seconds
FFT_AMPL_THRESH = 50  # Threshold amplitude to detect movement (pixels/sec)
X11 = True  # Whether to display the images usin Xwindows or not.

image = None
pt = None
flags = 0
need_to_init = True
last_frame_time = None
last_analysis_time = None

fig = pylab.figure()
ax1 = fig.add_subplot(211)
ax2 = fig.add_subplot(212)
fig.canvas.draw()
freqChart = None
timeChart = None
pylab.ion()


def initFeatures():
    print "initFeatures()"
    eig = cv.CreateImage (cv.GetSize (grey), 32, 1)
    temp = cv.CreateImage (cv.GetSize (grey), 32, 1)
    mask = cv.CreateImage (cv.GetSize (grey), 8, 1)
    
    # the default parameters
    #quality = 0.01
    quality = 0.3
    # min_distance = 65
    min_distance = 20

    # Create a mask image to hide the top 10% of the image (which contains text)
    (w,h) = cv.GetSize(grey)
    cv.Rectangle(mask,(0,0),(w,h),cv.Scalar(255,0,0),-1)
    cv.Rectangle(mask,(0,0),(w,int(0.1*h)),cv.Scalar(0,0,0),-1)
    # cv.ShowImage ('mask',mask)


    # search the good points
    features = cv.GoodFeaturesToTrack (
        grey, eig, temp,
        MAX_COUNT,
        quality, min_distance, mask, 3, 0, 0.04)

    # refine the corner locations
    features = cv.FindCornerSubPix (
        grey,
        features,
        (win_size, win_size),  (-1, -1),
        (cv.CV_TERMCRIT_ITER | cv.CV_TERMCRIT_EPS, 20, 0.03))

    return features

def ptptdist(p0,p1):
    return math.sqrt(ptptdist2(p0,p1))

def ptptdist2(p0, p1):
    """ Return the distance^2 between two points. """
    dx = p0[0] - p1[0]
    dy = p0[1] - p1[1]
    return dx**2 + dy**2


def doPlot(dataMat,fftMat,times):
    global timeChart,freqChart,ax1,ax2,fig
    pixelNo = 1
    sampleFft = []
    freqs = []
    vals = []
    nSamples,nFeatures = cv.GetSize(dataMat)
    freqBinWidth = 1.0/(times[len(times)-1]-times[0]);
    for x in range(nSamples):
        freq = 1.0*x*freqBinWidth
        freqs.append(freq)
        sampleFft.append(abs(fftMat[pixelNo,x]))
        vals.append(dataMat[pixelNo,x])

    print "len(times)=%d, len(vals)=%d" % (len(times),len(vals))

    # Throw away the DC component to help with scaling the graph.
    sampleFft[0]=0
    if (timeChart==None):
        #pylab.xlim(0,50)
        timeChart, = ax1.plot(times,vals)
        pylab.xlabel("time (sec)")
        pylab.ylabel("brightness")
    else:
        timeChart.set_xdata(times)
        timeChart.set_ydata(vals)

    if (freqChart==None):
        pylab.xlim(0,50)
        freqChart, = ax2.plot(freqs,sampleFft)
        pylab.xlabel("freq (Hz)")
        pylab.ylabel("amplitude")
    else:
        freqChart.set_xdata(freqs)
        freqChart.set_ydata(sampleFft)
    fig.canvas.draw()
    print "doPlot done"



def doAnalysis(timeSeries):
    nSamples = len(timeSeries)
    nFeatures = len(timeSeries[0][1])
    print "doAnalysis() - nSamples = %d, nFeatures = %d" % (nSamples,nFeatures)

    # Create a matrix with feature speeds in the y direction, 
    # and time (frame no) in the x direction.   
    # This means we can do an FFT on each row to get
    # frequency components of each feature.
    dataMat = cv.CreateMat(nFeatures,nSamples-1,cv.CV_32FC1)
    times = []
    for frameNo in range(nSamples-1):
        dt = (timeSeries[frameNo+1][0] - timeSeries[frameNo][0]).total_seconds()
        if (frameNo==0):
            times.append(dt)
        else:
            times.append(times[frameNo-1]+dt)
        for featNo in range(nFeatures):
            ds = ptptdist(timeSeries[frameNo][1][featNo],
                          timeSeries[frameNo+1][1][featNo])
            v = ds/dt
            # set the direction
            #if(timeSeries[frameNo][1][featNo][1] >
            #   timeSeries[frameNo+1][1][featNo][1]):
            #    v*=-1
            dataMat[featNo,frameNo] = v
    
    #cv.ShowImage("dataMat",dataMat)

    fftMat = cv.CreateMat(nFeatures,nSamples-1,cv.CV_32FC1)
    cv.DFT(dataMat,fftMat,cv.CV_DXT_ROWS)
    #cv.ShowImage("fft",fftMat)

    if (X11): doPlot(dataMat,fftMat,times)

    # Look for the dominant frequency of each feature.
    freqBinSize = 1.0/(times[len(times)-1]-times[0]);
    print "freqBinSize = %f Hz" % (freqBinSize)
    maxAmpl = numpy.zeros((nFeatures))
    maxFreq = numpy.zeros((nFeatures))
    for featNo in range(nFeatures):
        maxAmpl[featNo] = FFT_AMPL_THRESH
        maxFreq[featNo] = 0
        for freqBin in range(1,nSamples/2):
            if (fftMat[featNo,freqBin]>maxAmpl[featNo]):
                maxAmpl[featNo] = fftMat[featNo,freqBin]
                maxFreq[featNo] = freqBinSize*freqBin

    # for featNo in range(nFeatures):
    #    print "FeatNo = %d, Freq = %f, ampl=%f" % (featNo,maxFreq[featNo],maxAmpl[featNo])

    timeSeries = []
    return (timeSeries,maxFreq,maxAmpl)


if __name__ == '__main__':
    timeSeries = []  # array of times that data points were collected.
    maxFreq = None
    if (X11): cv.NamedWindow ('Seizure_Detector', cv.CV_WINDOW_AUTOSIZE)

    camera = cv.CaptureFromFile("rtsp://192.168.1.18/live_mpeg4.sdp")
    #camera = cv.CaptureFromFile("../testcards/testcard.mpg")
    #camera = cv.CaptureFromFile("/home/graham/Videos/sample5.mp4")
    #camera = cv.CaptureFromCAM(0)

    frameSize = (640,480)
    videoFormat = cv.FOURCC('p','i','m','1')
    # videoFormat = cv.FOURCC('l','m','p','4')
    vw = cv.CreateVideoWriter("seizure_test.mpg",videoFormat, outputfps,frameSize,1)
    if (vw == None):
        print "ERROR - Failed to create VideoWriter...."

    last_analysis_time = datetime.datetime.now()
    last_frame_time = datetime.datetime.now()
    frame = cv.QueryFrame(camera)

    while 1:

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



            timeSeries.append( (last_frame_time,features) )
            
        if ((datetime.datetime.now() - last_analysis_time).total_seconds() 
            > Analysis_Period):
            font = cv.InitFont(cv.CV_FONT_HERSHEY_SIMPLEX, 0.5, 0.5, 0, 1, 8) 
            (timeSeries,maxFreq,maxAmpl) = doAnalysis(timeSeries)
            #features = initFeatures()
            last_analysis_time = datetime.datetime.now()

        for featNo in range(len(features)):
            pointPos = features[featNo]
            cv.Circle (image, (int(pointPos[0]), int(pointPos[1])), 3, (0, 255, 0, 0), -1, 8, 0)
            if (not maxFreq == None):
                msg = "%d-%3.1f" % (featNo,maxFreq[featNo])
                cv.PutText(image,msg, (int(pointPos[0]+5),int(pointPos[1]+5)),font, (255,255,255)) 





        # swapping
        prev_grey, grey = grey, prev_grey
        prev_pyramid, pyramid = pyramid, prev_pyramid
        need_to_init = False
        
        # we can now display the image
        if (X11): cv.ShowImage ('Seizure_Detector', image)
        cv.WriteFrame(vw,image)

        # handle events
        c = cv.WaitKey(10)

        if c == 27:
            # user has press the ESC key, so exit
            break

        frameTime = (datetime.datetime.now() - last_frame_time).total_seconds()
        actFps = 1.0/frameTime
        if (frameTime < 1/inputfps):
            cv.WaitKey(1+int(1000.*(1./inputfps - frameTime)))

        last_frame_time = datetime.datetime.now()
        frame = cv.QueryFrame(camera)
       # End of main loop.
