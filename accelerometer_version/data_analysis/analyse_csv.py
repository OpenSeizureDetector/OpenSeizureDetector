#!/usr/bin/python
# Visualisation of accelerometer data logged by seizure_detector.
#
# Graham Jones, February 2013
#
import sys
import csv
import matplotlib.pyplot as pyplot
import numpy as np

pyplot.ion()
fig = pyplot.figure()
ax = fig.add_subplot(111)
pyplot.title("Accelerometer on floorboard - Various Activities\nValues are amplitude at each frequency")
pyplot.xlabel("Time (sec)")
pyplot.ylabel("Frequency (Hz)")


ntimes = 280  # number of records ot use in x direction
nfreq = 32    # number of frequency bins to use in y direction
maxval = 10   # threshold amplitude (to make colour range useful)

# Create an array of the correct size, filled with zeroes.
imgArr = np.zeros(shape=(nfreq,ntimes))
im = None
# Open the data file
with open('input.csv','rb') as csvfile:
    dataReader = csv.reader(csvfile,delimiter=',', quotechar='"')
    colNo = 0
    # read each row in turn
    for row in dataReader:
        if (colNo == 0):
            pyplot.xlabel("Time (sec) from %s" % (row[0]))
        # read the frequency spectrum from the row.
        for i in range(0,nfreq-1):
            # convert value to float ***Otherwise it is a STRING!!!!***
            val = float(row[i+3])
            # apply the maximum value threshold
            if (val<maxval):
                imgArr[i,colNo] = val
            else:
                imgArr[i,colNo] = maxval
        colNo += 1
        # If we have collected enough data for the first graph, plot it.
        if (colNo == ntimes-1): 
            print imgArr
            #cm = pyplot.colors.Colormap("accel",10)
            if (im==None):
                im = pyplot.imshow(imgArr,
                                   aspect='auto')
                pyplot.colorbar(shrink=.92)
            else:
                im.set_array(imgArr)
            pyplot.show() 
            
            # wait for input, then collect data for the next graph and repeat..
            raw_input()
            print "Press Enter for next plot..."
            #c = sys.stdin.read(1)
            colNo = 0

