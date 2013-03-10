#!/usr/bin/python

import cv
import math

nxSources = 3   # Number of test sources to generate in x direction
nySources = 3   # Number of test sources to generate in y direction
fSourceMin = 0. # Minimum frequency of sources
fSourceMax = 8. # Maximum frequency of sources.
frameSize = (640,480)
fps = 30
videoFormat = cv.FOURCC('p','i','m','1')
videoLen = 120 # seconds

size = 30 # pixels
ampl = 30 # pixels


def main():
    vw = cv.CreateVideoWriter("testcard.mpg",videoFormat, fps,frameSize,1)

    nFrames = videoLen * fps
    nSources = nxSources * nySources
    xorigin = 0.5*frameSize[0]/(nxSources)
    yorigin = 0.5*frameSize[0]/(nySources)
    font = cv.InitFont(cv.CV_FONT_HERSHEY_SIMPLEX, 1, 1, 0, 3, 8) #Creates a font
    for n in range(0,nFrames):
        frame = cv.CreateImage(frameSize,8,3)
        cv.Set(frame,cv.Scalar(0,0,0))
        msg = "fmin = %f, fmax=%f" % (fSourceMin,fSourceMax)
        cv.PutText(frame,msg, (0,30),font, 255) #Draw the text
        tFrame = 1.0 * n/fps  # time in seconds.
        for ySource in range(0,nySources):
            for xSource in range(0,nxSources):
                sourceNo = xSource + ySource*nxSources
                #print "sourceNo = %d" % (sourceNo)
                freq = fSourceMin + sourceNo * (fSourceMax-fSourceMin)/(nSources-1)
                angVel = freq * (2*math.pi)
                xOff = ampl * math.sin(angVel*tFrame)
                #print "(%d,%d), freq=%f, xoff=%f" % (xSource,ySource,freq,xOff)
                x = int(xorigin+xSource*(frameSize[0]/(nxSources)) + xOff)
                y = int(yorigin+ySource*(frameSize[1]/(nySources)))
        #print "Frame Number %d: time=%f   w=%f  Center=(%d,%d)." % (n,tFrame,angVel,x,y)
                cv.Circle(frame, (x,y), size, cv.Scalar(255,1,1), thickness=-1, lineType=-1, shift=0) 
        cv.WriteFrame(vw,frame)


if __name__ == "__main__":
    main()
