#!/usr/bin/python

import cv
import datetime
import numpy, scipy, scipy.fftpack
import pylab

IMG_STACK_LEN = 100
ANALYSIS_LAYER = 6
FFT_CHAN_MIN = 3
FFT_CHAN_MAX = 20
FREQ_THRESH = 0.50
inputfps     = 15
window1 = "Current"
window2 = "Oldest"
window3 = "Time Data"
window4 = "Fourier Transform"
imgList = []
cv.NamedWindow(window2,cv.CV_WINDOW_NORMAL)
fig = pylab.figure()
ax1 = fig.add_subplot(211)
ax2 = fig.add_subplot(212)
fig.canvas.draw()
freqChart = None
pylab.ion()


def preProcessImage(inImg):
    """
    Returns an image, which is a processed version of the input image inImg.
    Currently just converts to gray scale.
    """
    outImg = cv.CreateImage(cv.GetSize(inImg),8,1)
    cv.CvtColor(inImg,outImg,cv.CV_BGR2GRAY)
    for i in range(ANALYSIS_LAYER):
        outImg = doPyrDown(outImg)
    return(outImg)
# End of preProcessImage

def doPlot(fftMat):
    global freqChart,ax1,ax2,fig
    pixelNo = 58
    sampleFft = []
    freqs = []
    freqBinWidth = 1.0*inputfps/IMG_STACK_LEN
    for x in range(IMG_STACK_LEN):
        freq = 1.0*x*freqBinWidth
        freqs.append(freq)
        sampleFft.append(fftMat[pixelNo,x])

    # Throw away the DC component to help with scaling the graph.
    #    sample_fft[0]=sample_fft[1]
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

def getSpectra(imgList):
    """ Calculates the fourier transforms (against time) of all pixels in
    imgList.
    imgList is a list of tuples (datetime,image).
    Creates a 2 dimensional array, where one dimension is the pixel values in
    the image, and the other is time, then calculates the fourier transform.
    To give the frequency contributions of the values in each pixel.
    """
    (width,height) = cv.GetSize(imgList[0][1])
    nPixels = width * height
    #print "Image Size = (%d x %d) - %d pixels.  Number of Images = %d" \
    #    %  (width,height,nPixels,len(imgList))

    # Create a matrix with pixel values in the y direction, and time (frame no)
    # in the x direction.   This means we can do an FFT on each row to get
    # frequency components of each pixel.
    dataMat = cv.CreateMat(nPixels,len(imgList),cv.CV_32FC1)
    for frameNo in range(len(imgList)):
        for y in range(height-1):
            for x in range(width-1):
                pixelNo = y*width+x
                pixelVal = float(imgList[frameNo][1][y,x]/255.0)
                dataMat[pixelNo,frameNo] = pixelVal
    
    cv.ShowImage(window3,dataMat)

    fftMat = cv.CreateMat(nPixels,len(imgList),cv.CV_32FC1)
    #(a,fftMax,b,c)= cv.MinMaxLoc(fftMat)
    #print "fftMax=%f" % (fftMax)
    fftMat_int = cv.CreateMat(nPixels,len(imgList),cv.CV_8UC1)

    cv.DFT(dataMat,fftMat,cv.CV_DXT_ROWS)
    cv.ConvertScale(fftMat,fftMat_int,1000)
    cv.ShowImage(window4,fftMat_int)

    # Apply frequency filter to FFT data
    for x in range(0,FFT_CHAN_MIN):
        for y in range(0,nPixels):
            fftMat[y,x] = 0.0

    #for x in range(FFT_CHAN_MAX,len(imgList)-1):
    #    for y in range(0,nPixels):
    #        fftMat[y,x] = 0.0

    doPlot(fftMat)

    return fftMat

def pixelNo2xy(pixelNo,img):
    (width,height) = cv.GetSize(img)
    y = int(pixelNo / (width-1))
    x = pixelNo - y*(width-1)
    return (x,y)

def getEquivLoc(x,y,layer):
    """ Returns the equivalent location to x,y in a different layer.
    """
    xl = int((x+1)*2**(layer))
    yl = int((y+0.5)*2**(layer))
    #print "getEquivLoc(%d,%d,%d) -> (%d,%d)" % (x,y,layer,xl,yl)
    return (xl,yl)


def doPyrDown(inImg):
    """
    Returns an image that has been subjected to Gaussian downsampling via pyrDown.
    Returned image is half the size of the original.
    """
    (width,height)= cv.GetSize(inImg)
    outSize = (width/2, height/2)
    outImg = cv.CreateImage(outSize,8,1)
    cv.PyrDown(inImg,outImg,cv.CV_GAUSSIAN_5x5)
    return(outImg)
# end of doPyrDown

def main():
    """
    Main program - controls grabbing images from video stream and loops around each frame.
    """
    camera = cv.CaptureFromFile("rtsp://192.168.1.18/live_mpeg4.sdp")
    #camera = cv.CaptureFromFile("testcards/testcard.mpg")
    #camera = cv.CaptureFromCAM(0)
    if (camera!=None):
        frameSize = (640,480)
        #fps = 30
        videoFormat = cv.FOURCC('p','i','m','1')
        vw = cv.CreateVideoWriter("seizure_test.mpg",videoFormat, inputfps,frameSize,1)

        cv.NamedWindow(window1,cv.CV_WINDOW_AUTOSIZE)
        origImg = cv.QueryFrame(camera)
        lastTime = datetime.datetime.now()
        while (origImg):
            # Preprocess, then add the new image to the list, along with the 
            # time it was recorded.
            imgList.append(
                (lastTime,
                 preProcessImage(origImg)
                 ))
            # Drop the oldest image off the list if we have enough in the list.
            if (len(imgList)>IMG_STACK_LEN):
                imgList.pop(0)  # Remove first item
                
 
            xorig = 0
            yorig = 0
            if (len(imgList) == IMG_STACK_LEN):
                # imgList[] is now a list of tuples (time,image) containing the
                # reduced size images -
                spectra = getSpectra(imgList)
                binWidth = 1.0*inputfps/IMG_STACK_LEN
                #(a,fftMax,b,(freqNo,pixelNo))= cv.MinMaxLoc(spectra)
                for freqNo in range(0,int(len(imgList)/2)):
                    for pixelNo in range(0,70):
                        if (abs(spectra[pixelNo,freqNo])>FREQ_THRESH):
                            print "PixelNo %d exceeds threshold (val=%f) in freq bin %d (%f Hz" % (pixelNo,abs(spectra[pixelNo,freqNo]),freqNo,freqNo*binWidth)
                            (xmax,ymax) = pixelNo2xy(pixelNo,imgList[0][1])
                            (xorig,yorig) = getEquivLoc(xmax,ymax,ANALYSIS_LAYER)
                            if (freqNo<10):
                                colour = cv.Scalar(255,1,1)
                                thickness = 1
                            elif (freqNo>10 and freqNo<20):
                                colour = cv.Scalar(1,255,1)
                                thickness = 5
                            elif (freqNo>20 and freqNo<30):
                                colour = cv.Scalar(1,1,255)
                                thickness = 10
                            elif (freqNo>30):
                                colour = cv.Scalar(255,255,255)
                                thickness = 20
                            cv.Circle(origImg, (xorig,yorig), 30, colour, thickness=thickness, lineType=-1, shift=0) 
            cv.WriteFrame(vw,origImg)
            cv.ShowImage(window1,origImg)
            cv.ShowImage(window2,imgList[0][1])
            cv.WaitKey(1) # This is very important or ShowImage doesn't work!!
                

            timeDiff = (datetime.datetime.now() - lastTime).total_seconds() 
            if (timeDiff<1./inputfps):
                print "timediff=%f, 1/fps=%f" % (timeDiff,1./inputfps)
                cv.WaitKey(1+int(1000.*(1./inputfps - timeDiff)))

            # Note - there is something odd about this time calculation
            # it does not seem to be consistent with the timestamps on the
            # images.
            timeDiff = (datetime.datetime.now() - lastTime).total_seconds() 
            fps = 1./timeDiff
            print "timeDiff=%f, fps=%f fps" % (timeDiff,fps)

            # Now get a new frame ready to start the loop again
            origImg = cv.QueryFrame(camera)
            lastTime = datetime.datetime.now()
        print "no more images..."
    else:
        print "Error - failed to connect to camera"
# End of main()

if __name__ == "__main__":
    main()
