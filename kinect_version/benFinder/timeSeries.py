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
import matplotlib.pyplot as plt
import numpy
from scipy import signal

class TimeSeries:

    def __init__(self,tslen = 300, freq = 30, rtPlot = True):
        """ Initialise the time series to contain tslen samples,
        sampled at freq samples per second.
        """
        self._tslen = tslen
        self._freq  = freq
        self._ts = []
        self._rtPlot = rtPlot

        if (rtPlot):
            plt.ion()
            self._fig = plt.figure()
            self._ax1 = self._fig.add_subplot(111)
            self._timeChart = None
            self._peakChart = None

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

    @property
    def len(self):
        return (len(self._ts))

    @property
    def rawData(self):
        return (self._ts)

    def plotRawData(self):
        if (self.len < self._tslen):
            #print self.len, len(self._times)
            return False
        #print len(self._times), self._times
        #print len(self._ts), self._ts
        if (self._rtPlot):
            if (self._timeChart==None):
                self._timeChart, = self._ax1.plot(self._times,self._ts)
                plt.xlabel("time(s)")
                plt.ylabel("value")
                plt.ylim([0,255])
            else:
                self._timeChart.set_ydata(self._ts)
            self._fig.canvas.draw()
        return True

    def findPeaks(self):
        if (self.len < self._tslen):
            return False
        else:
            peakind = signal.find_peaks_cwt(self._ts, numpy.arange(1,30))
            pkx = []
            pky = []
            for peak in peakind:
                pkx.append(self._times[peak])
                pky.append(self._ts[peak])
                
            if (self._rtPlot):
                if (self._peakChart==None):
                    self._peakChart, = self._ax1.plot(pkx,pky,'ro')
                else:
                    self._peakChart.set_xdata(pkx)
                    self._peakChart.set_ydata(pky)
                self._fig.canvas.draw()
            return True

