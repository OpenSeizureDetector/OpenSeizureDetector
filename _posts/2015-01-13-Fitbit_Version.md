---
layout: post
title: Fitbit Based Seizure Detector
category: Meta

excerpt: Description of Possibility of using a 'Fitbit' activity tracker as a seizure detector.

---
# Update 03 Feb 2015
I abandoned the FitBit approach because of the difficulty in interpreting the 
data and have gone for a Pebble Smart Watch, and have a very promising working
prototype - see [this post](http://jones139.github.io/OpenSeizureDetector/2015/02/01/Pebble_Watch_Version/)




# Hardware
![Fitbit One](http://static1.fitbit.com/simple.b-cssdisabled-png.h47e3210a910010717f0d5ec74009f261.pack?items=%2Fcontent%2Fassets%2Fonezip%2Fimages%2Ffeatures-carousel%2Fone%2Fblack%2Fproduct.png)
The [FitBit One](http://www.fitbit.com/uk/one) hardware is a small, lightweight unit congaining a battery, accelerometer, processor and bluetooth LE radio.

It can communicate with a mobile phone or other computer (via a USB dongle) to 
synchronise data with the [Fitbit.com](http://fitbit.com) website.

# Software
I have not found anything about the firmware that runs on the Fitbit one device itself.

It can be synchronised to the [Fitbit.com](http://fitbit.com) website using
either an android phone application, or the open source [galileo](https://bitbucket.org/benallard/galileo) python tool.

# Potential as a Seizure Detector
The harware device itself is exactly what I want to make an accelerometer based
seizure detector - it is a minature version of the proof of concept device
I investigated previously (see [this page](http://jones139.github.io/OpenSeizureDetector/meta/2014/02/21/Accelerometer_Version/)).

The challenge will be software.....

# Issues with sofware:
* I have not found any information on how to write firmware for the device.  
This would be the ideal solution because I could do custom processing to look
for seizure type movements rather than detecting footsteps like the device 
does now.
* Even if the built-in step counting function would be useful for seizure
detection (ie a high rate of 'steps' may indicate a seizure), this is difficult
to get at.   The  data is uploaded to the fitbit web sever as a raw 'dump' file and it is interpreted on the server.   It looks like it will be difficult to
interpret the dump file format.   The alternative would be to synchronise to 
the fitbit server frequently (every 20 sec?), and query the server at a similar frequency to look at current step count total to allow the rate of steps to be
calculated.   This could work, but would involve a lot of network traffic so 
does not seem sensible.

# Format of the Dump Files
It seems that the data from the fitbit one is encrypted (people were worried
about data privacy.....).   There is some information on the format on the
galileo project [web site](https://bitbucket.org/benallard/galileo/wiki/Megadumpformat), but it does not look like anyone has succeeded in reading the encrypted files.
I thought I would look for some clues by collecting a series of dump files (I tried the smaller micro-dump files) and treating them as a time series.
I then plotted the resulting 2d array as an image to see if there are any obvious patterns:
![patterns](https://raw.githubusercontent.com/jones139/OpenSeizureDetector/master/fitbit_version/micro-dump_images.png).

The only significant pattern I can see in these is a counter of some sort in 
one of the first few bytes, but the bulk of it looks like noise.
There is no discernable difference between the image with the fitbit stationary
and the one with it being shaken.  This is not good - I had hoped to see a few
bytes change when I shook it.

Unfortunately I do not know anything about de-crypting things, so this is
looking difficult.

I think I might need to look for a more open version of the fitbit sensor....


