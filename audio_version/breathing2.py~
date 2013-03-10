#!/usr/bin/python
# Simple attempt to detect beathing from a microphone.
# Based on a useful example from http://ubuntuforums.org/showthread.php?p=6509055
# to get the audio data from the microphone, and
# http://www.acronymchile.com/sigproc.html to process it.
# with some help from http://stackoverflow.com/questions/9456037/scipy-numpy-fft-frequency-analysis
#
#
# Graham Jones, February 2013.

import alsaaudio, time, audioop
import time
import numpy, scipy, scipy.fftpack
import pylab
import sys

for card in alsaaudio.cards():
	print card

audioSampleFreq = 8000 # Hz  (sound card sample freq)
sampleFreq = 100   # Hz   (frequency that we record samples for analysis).
samplePeriod = 10   # sec
rolAvWidth = 3      # take rolling average over 3 samples.
nSamples = sampleFreq * samplePeriod

nRawSamples = int(audioSampleFreq/sampleFreq)  # number of raw samples per analysed sample.
print "nSamples = %d\n" % (nSamples)
print "nRawSamples = %d\n" % (nRawSamples)


# Set attributes: Mono, 8000 Hz, 16 bit little endian samples
inp = alsaaudio.PCM(alsaaudio.PCM_CAPTURE,alsaaudio.PCM_NONBLOCK)
inp.setchannels(1)
inp.setrate(audioSampleFreq)
inp.setformat(alsaaudio.PCM_FORMAT_S16_LE)

# The period size controls the internal number of frames per period.
# The significance of this parameter is documented in the ALSA api.
# For our purposes, it is suficcient to know that reads from the device
# will return this many frames. Each frame being 2 bytes long.
# This means that the reads below will return either 320 bytes of data
# or 0 bytes of data. The latter is possible because we are in nonblocking
# mode.
inp.setperiodsize(160)

analysis_period = samplePeriod    # seconds
t_start = time.time()
samples = [];
raw_samples = []
raw_samples_buf = []
counter = 0
while True:
	# Collect data for analysis_period seconds, then analyse it.
	#if ((time.time()-t_start) > analysis_period):
	if (len(samples)>=nSamples):
		print "analysis time!"
		sample_array = numpy.array(samples)
		sample_fft = abs(numpy.fft.rfft(samples)) # throw away imaginary bit
		offt = open('outfile_fft.dat','w')
		freqs = []
		times = []
		sample_no = []
		sn = 0
		freqBinWidth = 1.0*sampleFreq/len(samples)
		for x in range(len(samples)):
			times.append(1.0*x/sampleFreq)
		for x in range(len(sample_fft)):
			freq = 1.0*x*freqBinWidth
			freqs.append(freq)
			sample_no.append(sn)
			sn += 1
			offt.write('%f %f  ' % 
				   (freq,
				    abs(sample_fft[x].real)))

		#print len(freqs),len(sample_fft)
		print "Sample Frequency = %3.2f Hz " % (sampleFreq)
		print "Number of Samples = %d" % (len(samples))
		print "Frequency Resolution = %3.2f Hz" % (freqBinWidth)

		#freqs = scipy.fftpack.fftfreq(signal.size, t[1]-t[0])

		pylab.subplot(311)
		pylab.plot(raw_samples)
		pylab.subplot(312)
		pylab.plot(times, samples)
		pylab.xlabel("time (s)")
		pylab.ylabel("value");
		pylab.subplot(313)
		# Throw away the DC component to help with scaling the graph.
		sample_fft[0]=sample_fft[1]
		pylab.plot(freqs,sample_fft)
		pylab.xlabel("freq (Hz)")
		pylab.ylabel("amplitude")
		#pylab.xlim(0,freqs[len(sample_fft)])
		pylab.xlim(0,10)
		#pylab.ylim(0)
		#pylab.xlim(0,100)
		pylab.show()
		t_start = time.time()
	else:
		# Read data from device
		l,data = inp.read()
		counter = 0
		if l:
			for n in range(l):
				raw_samples_buf.append(audioop.getsample(data,2,n))
			# we only save the maximum for analysis.
			#print "len_raw_samples_buf = %d, len_samples = %d, len_raw_samples = %d" % (len(raw_samples_buf),len(samples),len(raw_samples))
			if (len(raw_samples_buf)>=nRawSamples):
				samples.append(max(raw_samples_buf))
				raw_samples.extend(raw_samples_buf)
				raw_samples_buf = []
			sys.stdout.write('.')
			sys.stdout.flush()
