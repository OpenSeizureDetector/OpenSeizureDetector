---
layout: post
title: System Description
category: Meta

excerpt: A brief overview of how the current (Feb 2014) system works.

---

From a user's perspective the system appears on a 'Benja Telly' video monitor.   This displays the live video stream from the IP web camera mounted in Benjamin's bedroom and a text output from the seizure detector.   The Benja-Telly has a single pushbutton that allows the background image of the seizure detector to be re-set, and allows the IP web camera position to be moved.

# Benja Telly (benTV)
The Benja-Telly (benTV) is a Raspberry Pi single board computer with a USB wireless adapter to provide wireless networking, connected to an LCD TV monitor.   
The video display is provided using the omxplayer video player that has been 
written for the Raspberry Pi (to use its hardware video decoding capabilities).

The user interface is a python pygame based application that handles the button presses, and connects to the seizure detector and the IP web camera position changing interface using http networking.   The text display background changes colour depending on the alarm state of the seizure detector.

# Seizure Detector
## Overview
The seizure detector is a dual core Pentium based laptop connected to a Microsoft Kinect depth camera.
The application is written in Python, using the libfreenect library to interface with the Kinect sensor.
It uses the OpenCV computer vision library for image processing, and a Bottle/CherryPi based web server to provide a user interface.

The system does not attempt to fit a skeleton model to detect motion for two reasons:
* I do not think the problem will be well constrained, so subject to significant noise, which would make detecting small jerky movements difficult.
* To detect fairly high frequency shaking (a few Hz), we will need a high frame rate - probably over 20 fps - this will be difficult without some serious comptuer power.

##  Image Processing
The image processing sequence is:
* Grab a frame from the depth camera
* Subtract the stored background image
* Detect the largest object in the resulting difference image.
* Provided the largest object is big enough, assume that is the patient, and remove all other objects from the image.
* Collect a moving average intensity of the patient image.
* Subtract the current patient image from the moving average background.
* Calculate the average intensity of the resulting difference image.
* This gives us a time series of an intensity value, which is dependent on how much the patient is moving.

## Breathing Detection
The breathing detection is carried out by analysing the last 30 seconds worth 
of time series every second.
* Calulate a moving average of the time series to smooth out noise.
* Do a simple peak detection of the resulting smoothed series.
* The breathing rate is calculated from the number of peaks found in the time series.
* A warning or alarm is produced for low or very low breathing rates.

## Web Interface
* A simple web interface will provide a display in a web browser showing the raw and processed images, the time series, and the resulting text output of the analysis.
* The web interface can be used by other applications to obtain just the text output, images, or to re-set the background image.

<a href="{{site.baseurl}}/resources/img/Network_Configuration.png">
  <img style="width:300px;float:right" 
       src="{{site.baseurl}}/resources/img/Network_Configuration.png">
</a>
<a href="{{site.baseurl}}/resources/img/Software_Configuration.png">
  <img style="width:300px;float:right" 
       src="{{site.baseurl}}/resources/img/Software_Configuration.png">
</a>
