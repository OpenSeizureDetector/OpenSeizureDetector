#!/usr/bin/python
# Graham Jones, February 2013.

import numpy, scipy, scipy.fftpack
import math
import pylab

pi = 3.14159265
sampleFreq = 100   # Hz   (frequency that we record samples for analysis).
samplePeriod = 10   # sec
nSamples = sampleFreq * samplePeriod


# Generate a waveform to analyse
samples = [];
counter = 0
for sNo in range(samplePeriod*sampleFreq):
	time = 1.0 * sNo / sampleFreq
	print time
	samples.append(1.0 * math.sin(time*2.0*pi/1) +
		       0.5 * math.sin(time*2*pi/10) +
		       0.1 * math.sin(time*2*pi/0.1)
		       )

# now analyse it
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

print "Sample Frequency = %3.2f Hz " % (sampleFreq)
print "Number of Samples = %d" % (len(samples))
print "Frequency Resolution = %3.2f Hz" % (freqBinWidth)
print sample_fft

# And plot the result

pylab.subplot(211)
pylab.plot(times, samples)
pylab.xlabel("time (s)")
pylab.ylabel("value");
pylab.subplot(212)
pylab.plot(freqs,sample_fft)
pylab.xlabel("freq (Hz)")
pylab.ylabel("amplitude")
#pylab.xlim(0,freqs[len(sample_fft)])
#pylab.ylim(0)
#pylab.xlim(0,100)
pylab.show()

