#!/usr/bin/python
#
# Graham Jones, May 2013.

import matplotlib.pyplot as pyplot
import numpy

class graphicsOutput:
    """ Graphical output for breathing detector """

    def __init__(self):
        ''' Initialise the graphical output system '''
        pyplot.ion()
        self.timeChart = None
        self.freqChart = None
        self.amplChart = None
        self.im = None
        self.firstRun = True

    def plotRawData(self,window):
        ''' plots the raw audio data stored in the array 'window'.
        at 8kHz and a window size of 800 samples, this correspons to 0.1 sec of data.
        '''
        sampNo = []
        for sn in range(0,len(window)):
            sampNo.append(sn)
        if (self.timeChart==None): 
            self.fig = pyplot.figure(figsize=(5.5,8))
            self.ax1 = self.fig.add_subplot(311)
            #self.ax1.set_autoscaley_on(False)
            self.timeChart, = self.ax1.plot(sampNo,window)
            pyplot.xlabel("sample No")
            pyplot.ylabel("value");
            pyplot.ylim([0,10000]);
            pyplot.xlim([0,800]);
        else:
            self.timeChart.set_xdata(sampNo)
            self.timeChart.set_ydata(window)

        pyplot.show()
        self.fig.canvas.draw()

    def plotFFT(self,sample_fft,sampleFreq):
        ''' Plots the fourier transform data stored in sample_fft.  The sample freq in Hz
        has to be passed as sampleFreq to allow the bin size of the fft to be calculated.
        at a sample frequency of 8kHz and 800 samples used in the analysis, the bin size will be 10Hz.
        '''
        freqs = []
        freqBinWidth = 1.0*sampleFreq/(2*len(sample_fft)) # 2* is to get number of samples used for fft.
        for x in range(len(sample_fft)):
            freq = 1.0*x*freqBinWidth
            freqs.append(freq)

            if (self.firstRun):
                print "Sample Frequency = %3.2f Hz " % (sampleFreq)
                print "Number of Samples = %d" % (len(sample_fft))
                print "Frequency Resolution = %3.2f Hz" % (freqBinWidth)
                self.firstRun = False

        # Set DC component to zero to tidy up graph.
        sample_fft[0] = 0.0

        if (self.freqChart==None): 
            if (self.fig == None):
                self.fig = pyplot.figure()
            self.ax2 = self.fig.add_subplot(312)
            self.freqChart, = self.ax2.plot(freqs,sample_fft)
            # self.ax2.set_yscale('log')
            pyplot.xlabel("freq (Hz)")
            pyplot.ylabel("value");
            pyplot.ylim([0,50000]);
            pyplot.xlim([0,4000]);
        else:
            self.freqChart.set_xdata(freqs)
            self.freqChart.set_ydata(sample_fft)
        # self.ax1.set_autoscaley_on(False)

        self.fig.canvas.draw()
        pyplot.show()

    def plotAmplData(self,amplHist):
        ''' Plots the latest 800 values in the amplHist array.
        The samples are expected to be one sample per frame, so at 8kHz sample frequency
        and a frame size of 200 samples, this means that we plot the last 20 seconds of data
        '''
        nSamplesOrig = 800 # we try to plot the last 800 samples in the history.
        nSamples = nSamplesOrig
        sampNo = []
        if (len(amplHist)<nSamples): nSamples = len(amplHist)
        for sn in range(0,nSamples):
            sampNo.append(sn)
        #print "len(sampNo)=%d, len(amplhist)=%d" % (len(sampNo),len(amplHist))
        if (self.amplChart==None): 
            if (self.fig == None):
                self.fig = pyplot.figure()
            self.ax3 = self.fig.add_subplot(313)
            self.amplChart, = self.ax3.plot(sampNo[-1*nSamples:],amplHist[-1*nSamples:])
            self.ax3.set_yscale('log')
            pyplot.xlabel("frame No")
            pyplot.ylabel("value");
            pyplot.ylim([100,1000000]);
            pyplot.xlim([0,nSamplesOrig]);
        else:
            self.amplChart.set_xdata(sampNo[-1*nSamples:])
            self.amplChart.set_ydata(amplHist[-1*nSamples:])

        pyplot.show()
        self.fig.canvas.draw()


    def plotAmplAnalysis(self,amplHist,amplHistSmoothed):
        ''' Plots the raw and smoothed amplitude history data
        '''
        sampNo = []
        nSamples = len(amplHist)
        for sn in range(0,nSamples):
            sampNo.append(sn)
        sampNoSm = []
        nSamplesSm = len(amplHistSmoothed)
        for sn in range(0,nSamplesSm):
            sampNoSm.append(sn)
        self.fig3 = pyplot.figure()
        self.ax4 = self.fig3.add_subplot(111)
        #print "len(sampNo)=%d, len(amplHist)=%d" % (len(sampNo),len(amplHist))
        self.amplAnalysisChart, = self.ax4.plot(sampNo,amplHist)
        self.ax4.plot(sampNoSm,amplHistSmoothed)
        self.ax4.set_yscale('log')
        pyplot.xlabel("frame No")
        pyplot.ylabel("value");
        #pyplot.ylim([0,1000000]);
        #pyplot.xlim([0,nSamples]);
        
        pyplot.show()
        self.fig.canvas.draw()



    def plotFreqHistory(self,freqHistory,sampleFreq,frameSize):
        # Plot the 2d map of frequency history
        imgx = len(freqHistory)
        imgy = len(freqHistory[0])
        historyLen = frameSize*imgx/sampleFreq  # length of history in seconds.
        #print "imgx=%d, imgy=%d" % (imgx,imgy)
        imgArr = numpy.zeros(shape=(imgy,imgx))
        for x in range(0,imgx):
            for y in range(0,imgy):
                imgArr[y,x] = freqHistory[x][y]
        #print "plotting image"
        if (self.im==None):
            self.fig2 = pyplot.figure()
            self.ax3 = self.fig2.add_subplot(111)
            self.im = self.ax3.imshow(imgArr,aspect='auto')
            self.im.set_cmap('prism')
            pyplot.xlabel("sampleNo")
            pyplot.ylabel("freq bin no")
            pyplot.xlim([0,len(freqHistory)])
            pyplot.ylim([0,1000])
        else:
            self.im = self.ax3.imshow(imgArr,aspect='auto')
        #			im.set_array(imgArr)
        self.fig2.canvas.draw()



if __name__ == "__main__":

    go = graphicsOutput()

