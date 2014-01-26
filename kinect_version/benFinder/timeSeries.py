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
import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy
from scipy import signal
import time
import cv2

class TimeSeries:

    def __init__(self,tslen = 300, freq = 30):
        """ Initialise the time series to contain tslen samples,
        sampled at freq samples per second.
        """
        self._tslen = tslen
        self._freq  = freq
        self._ts = []
        self._imgArr = []  # Array of images.
        self._ts2 = None
        self._rtPlot = False


        self._times = []
        for s in range(0,tslen):
            self._times.append(s * 1.0 / freq)
            #print s,self._times[s]

    def addSamp(self,val):
        """ Adds a sample of value val to the end of the time series,
        and truncates the series to tslen if necessary.
        """
        self._ts.append(val)
        if (self.len>self._tslen):
            del self._ts[0]

    def addImg(self,img):
        """ Adds an image to the end of the image time series, and truncats
        the series to tslen if necessary.
        """
        self._imgArr.append(img)
        if (self.lenImgArr > self._tslen):
            del self._imgArr[0]

    @property
    def len(self):
        return (len(self._ts))

    @property
    def lenImgArr(self):
            return (len(self._imgArr))

    @property
    def rawData(self):
        return (self._ts)

    @property
    def imgArr(self):
        return (self._imgArr)

    @property
    def mean(self):
        sumTs = 0.0
        for val in self._ts:
            sumTs = sumTs + val
        return (sumTs/self.len)

    def plotRawData(self,file=False,fname=None):
        """Plots the time series data.  If file=false output is to the
        screen.  Otherwise output is to a file named fname
        """
        # Only plot the graph if we have a full buffer of data, otherwise
        # do nothing.
        if (self.len < self._tslen):
            #print self.len, len(self._times)
            return False
        # If we have not initialised the plotting sytem, do it now.
        if (not self._rtPlot):
            #plt.ion()
            self._fig = plt.figure()
            self._ax1 = self._fig.add_subplot(111)
            self._timeChart = None
            self._peakChart = None
            self._smoothChart = None
            self._rtPlot = True
        if (self._timeChart==None):
            self._timeChart, = self._ax1.plot(self._times,self._ts)
            plt.xlabel("time(s)")
            plt.ylabel("value")
            plt.ylim([0,255])
        else:
            self._timeChart.set_ydata(self._ts)

        if (not self._ts2 == None):
            # Plot the smoothed data
            if (self._smoothChart==None):
                self._smoothChart, = self._ax1.plot(self._times2,self._ts2,'r')
            else:
                self._smoothChart.set_xdata(self._times2)
                self._smoothChart.set_ydata(self._ts2)

            # Plot peak positions
            pkx = []
            pky = []
            for peak in self._peakind:
                pkx.append(self._times2[peak])
                pky.append(self._ts2[peak])

            if (self._peakChart==None):
                self._peakChart, = self._ax1.plot(pkx,pky,'ro',markersize=20,fillstyle='full')
            else:
                self._peakChart.set_xdata(pkx)
                self._peakChart.set_ydata(pky)

        if (file):
            self._fig.savefig(fname,bbox_inches='tight')
        else:
            self._fig.canvas.draw()
        return True

    def findPeaks(self):
        if (self.len < self._tslen):
            return (0,0,0)
        else:
            # Smooth the data, and create new times array to match.
            self._ts2 = self.smoothFlat(self._ts,30)
            self._times2 = self._times[:len(self._ts2)]  # Truncate times array to same lenght
            # Find all the peaks in the data.
            #peakind = signal.find_peaks_cwt(ts2, numpy.arange(20,70),min_snr=1.0)
            maxtab,mintab = peakdet(self._ts2,2)
            #print maxtab
            if len(maxtab)>0:
                self._peakind = maxtab[:,0].astype(int)
                if (self._peakind[0]==0):
                    #print "Trimming off peak at position zero"
                    self._peakind = self._peakind[1:]
            else:
                self._peakind = []
            #print peakind

            ts_time = float(self._tslen) / self._freq
            rate = 60. * len(self._peakind)/ts_time # peaks per minute

            #print "%d peaks in %3.2f sec = %3.1f bpm" % \
            #    (len(self._peakind),ts_time,rate)

            return (len(self._peakind),ts_time,rate)


    def smoothFlat(self,ts,winLen):
        """Do a simple moving average smoothing, using a window of lenghth
        winLen frames.  Note that the returned array is smaller than the
        original one, because only the area where the smoothing is valid
        is returned to avoid edge effects.
        """
        w=numpy.ones(winLen,'d')
        v=numpy.convolve(w/w.sum(),ts,mode='valid')
        return v

    def writeToFile(self,fnameRoot):
        """Write the whole time series, and images to files of root filename fname"""
        # Write brightness timeseries to file.
        print "writeToFile"
        tstampStr = time.strftime("%Y%m%d%H%M%S", time.localtime())
        print tstampStr
        fname = "%s-%s.dat" % (fnameRoot,tstampStr)
        print fname
        f = open(fname,"w")
        for i in range(0,self.len):
            f.write("%d %d\n" % (i,self._ts[i]))
        f.close()

        # Write image array to video file.
        fname = "%s-%s.mjpg" % (fnameRoot,tstampStr)
        print fname
        print self._imgArr[0].shape
        height,width = self._imgArr[0].shape 
        vw = cv2.VideoWriter()
        #fourcc = cv2.cv.FOURCC(*'FMP4')
        fourcc = cv2.cv.FOURCC(*'MJPG')
        #fourcc = cv2.cv.FOURCC('M','J','P','G')
        #print fourcc
        # Note - the isColor parameter is VERY important - spent two hours
        # trying to work out why I was getting empty videos!!!!
        vw.open(fname,fourcc,30,(width,height),isColor=False)
        #print vw
        for i in range(0,self.len):
            vw.write(self._imgArr[i])
        vw.release()



def peakdet(v, delta, x = None):
    """
    from https://gist.github.com/endolith/250860

    Converted from MATLAB script at http://billauer.co.il/peakdet.html
    
    Returns two arrays
    
    function [maxtab, mintab]=peakdet(v, delta, x)
    %PEAKDET Detect peaks in a vector
    %        [MAXTAB, MINTAB] = PEAKDET(V, DELTA) finds the local
    %        maxima and minima ("peaks") in the vector V.
    %        MAXTAB and MINTAB consists of two columns. Column 1
    %        contains indices in V, and column 2 the found values.
    %      
    %        With [MAXTAB, MINTAB] = PEAKDET(V, DELTA, X) the indices
    %        in MAXTAB and MINTAB are replaced with the corresponding
    %        X-values.
    %
    %        A point is considered a maximum peak if it has the maximal
    %        value, and was preceded (to the left) by a value lower by
    %        DELTA.
    
    % Eli Billauer, 3.4.05 (Explicitly not copyrighted).
    % This function is released to the public domain; Any use is allowed.
    
    """
    maxtab = []
    mintab = []
       
    if x is None:
        x = numpy.arange(len(v))
    
    v = numpy.asarray(v)
    
    if len(v) != len(x):
        sys.exit('Input vectors v and x must have same length')
    
    if not numpy.isscalar(delta):
        sys.exit('Input argument delta must be a scalar')
    
    if delta <= 0:
        sys.exit('Input argument delta must be positive')
    
    mn, mx = numpy.Inf, -numpy.Inf
    mnpos, mxpos = numpy.NaN, numpy.NaN
    
    lookformax = True
    
    for i in numpy.arange(len(v)):
        this = v[i]
        if this > mx:
            mx = this
            mxpos = x[i]
        if this < mn:
            mn = this
            mnpos = x[i]
        
        if lookformax:
            if this < mx-delta:
                maxtab.append((mxpos, mx))
                mn = this
                mnpos = x[i]
                lookformax = False
        else:
            if this > mn+delta:
                mintab.append((mnpos, mn))
                mx = this
                mxpos = x[i]
                lookformax = True
 
    return numpy.array(maxtab), numpy.array(mintab)
