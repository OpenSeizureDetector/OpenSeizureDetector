#!/usr/bin/python
#
# Graham Jones, May 2013.

from audioInput import audioInput
from graphicsOutput import graphicsOutput
import numpy, scipy, scipy.fftpack

class breathDetector:
    def __init__(self,sampleFreq,frameSize,windowSize,useGraphics,debug=True):
        """ initialise the breath detector using audio input ai and graphics output go. """
        print "breathDetector.__init__()"
        self.sampleFreq = sampleFreq
        self.frameSize = frameSize
        self.windowSize = windowSize
        self.freqHistorySize = 200    # number of frames to store in frequency history
        self.ai = audioInput(self.sampleFreq,self.frameSize)
        if useGraphics:
            self.go = graphicsOutput()
        else:
            self.go = None
        self.graphCount = 0
        self.graphUpdateCount = 5*int(self.windowSize / self.frameSize)
        self.imgCount = 0
        self.imgUpdateCount = 5 * self.graphUpdateCount
        print "self.go = "
        print self.go
        self.debug = debug

        self.window = []   # This is the list of samples that we analyse.
        self.freqHistory = [] # frequency history (from series of FFT analyses)
        self.amplHistory = [] # history of amplitude in the specified frequency range.

        self.freqWinMin = 52 #  Hz  - frequency window of interest - minimum
        self.freqWinMax = 102 # Hz  - frequency window of interest - maximum

        freqBinWidth = 1.0*self.sampleFreq/(self.windowSize) 
        print "freqBinWidth = %f" % (freqBinWidth)

        self.freqWinMinBin = int(self.freqWinMin / freqBinWidth)   # Position in FFT array.
        self.freqWinMaxBin = int(self.freqWinMax / freqBinWidth)   # Position in FFT array

        print "Frequency Window is %f Hz to %f Hz" % (self.freqWinMin,self.freqWinMax)
        print "Which is bin no %d to %d" % (self.freqWinMinBin,self.freqWinMaxBin) 

        

        print "breathDetector.__init__() complete"


    def updateGraphs(self):
        #print "updateGraphs.  self.imgCount = %d, self.imgUpdateCount=%d" % (self.imgCount,self.imgUpdateCount)
        if (self.go!=None and (self.graphCount>=self.graphUpdateCount)): 
            self.go.plotRawData(self.window)
            self.go.plotFFT(self.freqHistory[len(self.freqHistory)-1],self.sampleFreq)
            self.go.plotAmplData(self.amplHistory)
            self.graphCount = 0
        #if (self.go!=None and (self.imgCount>=self.imgUpdateCount)): 
        #    self.go.plotFreqHistory(self.freqHistory,self.sampleFreq,self.frameSize)
        #    self.imgCount = 0
        self.graphCount = self.graphCount + 1
        self.imgCount = self.imgCount + 1

    def analyseWindow(self):
        sample_fft = abs(numpy.fft.rfft(self.window)) # throw away imaginary bit
        self.freqHistory.append(sample_fft) # save for future analysis.

        # trim frequency history array.
        if (len(self.freqHistory)>self.freqHistorySize):
            del self.freqHistory[0]

    def updateWindow(self,frame):
        """ adds the frame of data 'frame' to the end of the list of samples 'window'.
        If the length of the window is more than the required windowSize, it removes the earliest
        frame of data to maintain the required window size.
        """
        self.window.extend(frame)
        #print len(self.window)
        if (len(self.window)>self.windowSize):
            self.window = self.window[self.frameSize:]
        #print len(self.window)

    def updateAmplHist(self):
        ''' Update the history of amplitude in the given frequency range.
        calculates a simple average of the amplitudes between self.freqWinMin and self.freqWinMax
        and appends it to self.amplhist[]. '''

        amplTot = 0
        fft = self.freqHistory[len(self.freqHistory)-1]
        for binNo in range(self.freqWinMinBin,self.freqWinMaxBin+1):
            amplTot = amplTot + fft[binNo]
        #print "amplTot=%f" % (amplTot)
        self.amplHistory.append(amplTot/(self.freqWinMaxBin-self.freqWinMinBin+1))

    def run(self):
        """ run the breath detector main loop """
        print "breathDetector.run()"
        while(1):
            frame = self.ai.getFrame()
            self.updateWindow(frame)
            self.analyseWindow()
            self.updateAmplHist()
            self.updateGraphs()


if __name__ == "__main__":
    sampleFreq = 8000
    frameSize = 200
    windowSize = 800

    bd = breathDetector(sampleFreq,frameSize,windowSize,True)
    bd.run()


