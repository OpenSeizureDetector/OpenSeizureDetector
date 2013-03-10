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
samplePeriod = 1   # sec
historyLen = 30 #sec
nSamples = audioSampleFreq * samplePeriod

freqHistory = []
print "nSamples = %d\n" % (nSamples)


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
spectra = [];
counter = 0

pylab.ion()
fig = pylab.figure()
ax1 = fig.add_subplot(211)
ax2 = fig.add_subplot(212)
fig2 = pylab.figure()
ax3 = fig2.add_subplot(111)
timeChart = None
freqChart = None
im = None
while True:
	# Collect data for analysis_period seconds, then analyse it.
	#if ((time.time()-t_start) > analysis_period):
	if(len(freqHistory)>=historyLen):
		del freqHistory[0]
	elif (len(samples)>=nSamples):
		print "analysis time!"
		if (len(samples)>nSamples):
			del samples[nSamples:]
			print "samples truncated to %d records" % (len(samples))
		sample_array = numpy.array(samples)
		sample_fft = abs(numpy.fft.rfft(samples)) # throw away imaginary bit
		freqHistory.append(sample_fft) # save for future analysis.


		freqs = []
		times = []
		sample_no = []
		sn = 0
		freqBinWidth = 1.0*audioSampleFreq/len(samples)
		for x in range(len(samples)):
			times.append(1.0*x/audioSampleFreq)
		for x in range(len(sample_fft)):
			freq = 1.0*x*freqBinWidth
			freqs.append(freq)
			sample_no.append(sn)
			sn += 1

		#print len(freqs),len(sample_fft)
		print "Sample Frequency = %3.2f Hz " % (audioSampleFreq)
		print "Number of Samples = %d" % (len(samples))
		print "Frequency Resolution = %3.2f Hz" % (freqBinWidth)

		#freqs = scipy.fftpack.fftfreq(signal.size, t[1]-t[0])

		if (timeChart==None):
			timeChart, = ax1.plot(times, samples)
			pylab.xlabel("time (s)")
			pylab.ylabel("value");
		else:
			timeChart.set_xdata(times)
			timeChart.set_ydata(samples)

		# Throw away the DC component to help with scaling the graph.
		sample_fft[0]=sample_fft[1]
		if (freqChart==None):
			pylab.xlim(0,1000)
			freqChart, = ax2.plot(freqs,sample_fft)
			pylab.xlabel("freq (Hz)")
			pylab.ylabel("amplitude")
		else:
			freqChart.set_xdata(freqs)
			freqChart.set_ydata(sample_fft)
		fig.canvas.draw()

		# Plot the 2d map of frequency history
		imgx = len(freqHistory)
		imgy = len(freqHistory[0])
		print "imgx=%d, imgy=%d" % (imgx,imgy)
		imgArr = numpy.zeros(shape=(imgy,imgx))
		for x in range(0,imgx):
			for y in range(0,imgy):
				imgArr[y,x] = freqHistory[x][y]
		print "plotting image"
		if (im==None):
			pylab.xlabel("time (sec)")
			pylab.ylabel("freq (Hz)")
			pylab.xlim(0,historyLen)
			pylab.ylim(0,1000)
			im = ax3.imshow(imgArr,aspect='auto')
			im.set_cmap('prism')
		else:
			im = ax3.imshow(imgArr,aspect='auto')
		#			im.set_array(imgArr)
		fig2.canvas.draw()


		t_start = time.time()
		samples = []
	else:
		# Read data from device
		l,data = inp.read()
		if l:
			for n in range(l):
				samples.append(audioop.getsample(data,2,n))
			#sys.stdout.write('.')
			#sys.stdout.flush()
