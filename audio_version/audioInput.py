#!/usr/bin/python
#
# Graham Jones, May 2013.

import alsaaudio, audioop

class audioInput:
    """ A simple class to grab frames of audio samples from the audio system """

    def __init__(self,sampFreq,frameSize):
        print("auidioInput.__init__");
        self.audioSampleFreq = sampFreq
        self.frameSize = frameSize

        print "Initialising Audio System"
        for card in alsaaudio.cards():
                print card
        # Set attributes: Mono, 8000 Hz, 16 bit little endian samples
        # PCM_NORMAL forces the system to wait for a full frame of data
        # when we call getFrame.
        self.inp = alsaaudio.PCM(alsaaudio.PCM_CAPTURE,alsaaudio.PCM_NORMAL)
        self.inp.setchannels(1)
        self.inp.setrate(self.audioSampleFreq)
        self.inp.setformat(alsaaudio.PCM_FORMAT_S16_LE)

        # The period size controls the internal number of frames per period.
        # The significance of this parameter is documented in the ALSA api.
        # For our purposes, it is suficcient to know that reads from the device
        # will return this many frames. Each frame being 2 bytes long.
        # This means that the reads below will return either 320 bytes of data
        # or 0 bytes of data. The latter is possible because we are in nonblocking
        # mode.
        self.inp.setperiodsize(self.frameSize)

        print("__init__ complete")


    def getFrame(self):
        ''' Retrurn a list of self.frameSize audio samples '''
        frame = []
        # Read data from device
        l,data = self.inp.read()
        if l:
            #print "got %d samples" % (l)
            for n in range(l):
                frame.append(audioop.getsample(data,2,n))
        else:
            print "no data returned from getFrame"
        return frame



if __name__ == "__main__":
    sampleFreq = 8000
    frameSize = 200
    ai = audioInput(sampleFreq,frameSize)

    for i in range(1,2):
        print ai.getFrame()
