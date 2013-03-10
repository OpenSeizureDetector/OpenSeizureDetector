#!/usr/bin/python

import cv
import datetime

SAMPLE_PERIOD = 100  #ms (time between individual samples)
SAMPLING_TIME = 1.0 # sec (time to collect samples before analysing)
IMG_STACK_LEN = int(SAMPLING_TIME/(SAMPLE_PERIOD/1000.))
ANALYSIS_LAYER = 6
FFT_CHAN_MIN = 2
FFT_CHAN_MAX = 20
window1 = "Current"
window2 = "Oldest"
window3 = "Time Data"
window4 = "Fourier Transform"

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

def preProcessImageList(imgList):
    """ Given imgList is a list of tuples (time,image), pre-process each
    image in the list using preProcessImage function.
    """
    outList=[]
    for i in range(0,len(imgList)-1):
        procImg = preProcessImage(imgList[i][1])
        outList.append((imgList[i][0],procImg))
    return(outList)

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
    print "Image Size = (%d x %d) - %d pixels.  Number of Images = %d" \
        %  (width,height,nPixels,len(imgList))

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
    (a,fftMax,b,c)= cv.MinMaxLoc(fftMat)
    #print "fftMax=%f" % (fftMax)
    fftMat_int = cv.CreateMat(nPixels,len(imgList),cv.CV_8UC1)

    cv.DFT(dataMat,fftMat,cv.CV_DXT_ROWS)
    #cv.Split(fftMat,complexParts)
    #cv.magnitude(complexParts[0],complexParts[1],complexParts[0])
    #fftMat = complexParts[0]
    cv.ConvertScale(fftMat,fftMat_int,1000)
    cv.ShowImage(window4,fftMat_int)

    # Apply frequency filter to FFT data
    for x in range(0,FFT_CHAN_MIN):
        for y in range(0,nPixels):
            fftMat[y,x] = 0.0

    for x in range(FFT_CHAN_MAX,len(imgList)-1):
        for y in range(0,nPixels):
            fftMat[y,x] = 0.0
            
    (a,fftMax,b,c)= cv.MinMaxLoc(fftMat)
    print "fftMax=%f (filtered region)" % (fftMax)



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

def CollectSamples(duration,period,camera):
    """ Collect sample frames from camera camera at a rate of period ms
    between frames, for a total of duration seconds, then return a list of 
    preprocessed images.
    """
    imgList=[]
    origImg = cv.QueryFrame(camera)
    lastTime = datetime.datetime.now()
    nSamples = int(duration/(period/1000.))
    while (origImg):
        # Preprocess, then add the new image to the list, along with the 
        # time it was recorded.
        #processedImg = preProcessImage(origImg)
        processedImg = origImg
        imgList.append(
            (lastTime,
             processedImg
             ))

        if (len(imgList)==nSamples): break

        # limit frame rate period to period ms
        frameTime_ms = int((datetime.datetime.now()-lastTime).total_seconds()*1000.)
        if (frameTime_ms>period):
            cv.WaitKey(1)
        else:
            cv.WaitKey(period-frameTime_ms) # This is very important or ShowImage doesn't work!!

        # Now get a new frame ready to start the loop again
        origImg = cv.QueryFrame(camera)
        lastTime = datetime.datetime.now()

    # Note - there is something odd about this time calculation
    # it does not seem to be consistent with the timestamps on the
    # images.
    # timeDiff = (datetime.datetime.now() - lastTime).total_seconds() 
    # fps = 1./timeDiff
    time1 = imgList[0][0]
    time2 = imgList[len(imgList)-1][0]
    timeDiff = (time2-time1).total_seconds()
    if (timeDiff>0):
        fps = len(imgList)/timeDiff
    else:
        fps = 0.
    print "timeDiff=%f, fps=%f fps" % (timeDiff,fps)
    print "Collected %d sample images." % (len(imgList))

    return imgList

def main():
    """
    Main program - controls grabbing images from video stream and loops around each frame.
    """
    #camera = cv.CaptureFromFile("rtsp://192.168.1.18/live_mpeg4.sdp")
    camera = cv.CaptureFromCAM(0)
    if (camera!=None):
        while(1):
            print "Collecting Samples..."
            imgList = CollectSamples(SAMPLING_TIME,SAMPLE_PERIOD,camera)
            origImg = cv.QueryFrame(camera)
            cv.NamedWindow(window1,cv.CV_WINDOW_AUTOSIZE)
            cv.ShowImage(window1,origImg)
            # cv.ShowImage(window2,imgList[0][1])
            cv.WaitKey(1)
            print "Preprocessing...."
            imgList = preProcessImageList(imgList)
            print "Analysing...."
            spectra = getSpectra(imgList)
            cv.WaitKey(1)
    else:
        print "Error - failed to connect to camera"
# End of main()

if __name__ == "__main__":
    main()
