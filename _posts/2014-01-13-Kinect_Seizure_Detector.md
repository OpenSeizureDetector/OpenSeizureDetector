---
layout: post
title: Kinect Based Seizure Detector
category: Meta

excerpt: Description of the Kinect based seizure detector prototype

---




<h2>Hardware</h2>
<a href="{{site.baseurl}}/resources/img/kinect_installed.jpg">
  <img style="width:300px;float:right" 
       src="{{site.baseurl}}/resources/img/kinect_installed.jpg">
</a>
The hardware is a <a href="http://en.wikipedia.org/wiki/Kinect">Microsoft Kinect sensor</a> that I bought from the second hand gadget shop in Hartlepool.
I got a pleasant surprise that it came with a power supply and a standard USB plug, so I could connect it directly to my computer without having to modify it.
It is mounted about 40cm off the floor in Benjamin's bedroom.
It is connected to my home server computer, which I moved to Benjamin's room, whcih is an old laptop (dual core Pentium Based), running the server edition of <a href="http://ubuntu.com">Ubuntu linux</a>.


<h2>System Software</h2>
The software to talk to the kinect is the python bindings to libfreenect from <a href="http://openkinect.org">OpenKinect.org</a>.  It can either be built from source or installed from the ubuntu repositories with 'apt-get install python-libfreenect'.

The image processing uses the python bindings to the <a href="http://opencv.org">opencv</a> library - again installed from ubuntu repositories with 'apt-get install python-opencv'.

It also uses the numpy and matplotlib libraries for python to plot graphs, manipulate arrays etc.

<h2>Application Software</h2>
The seizure detector application is the 'benFinder' program in the <a href="https://github.com/jones139/OpenSeizureDetector">OpenSeizureDetector repository</a>.

The application framework used is based on the excellent introductory book
about using Python and opencv <a href="http://www.packtpub.com/opencv-computer-vision-with-python/book)">OpenCV Computer Vision with Python</a> by
Joseph Howse.

I modified his capture manager class to use the libfreenect library to grab images from the kinect.

The program uses a simple configuration file to specify which directories to use, and specify settings for thresholds for peak detection, alarm levels etc.

It provides a web based interface (on port 8080) that is based on the 'bottle' framework using a cherrypi integrated web server.

The basic computation that it does is:
<ol>
<li>Grab a depth image from the kinect camera</li>
<li>Subtract a pre-set background image - this means we only analyse the test, subject, not noise in the background</li>.
<li>Add the new image to a rolling average image, which is used as a short term background</li>
<li>Subtract the rolling average image from the current image</li>
<li>This gives us the very small differences between the current frame and the average - multiply by a factor to give bigger numbers so we can display the resulting image.</li>
<li>Calculate the average brightness of the test subject - the brighter it is the more movement there has been relative to the average image</a>
<li>Add the brightness to a timeseries of brightness values</li>
<li>Repeat.....</li>
</ol>

Every second we analyse the brighness timeseries to look for breathing events 
(changes in brigtness).  At the moment I smooth the data (sing a rolling average) then use a very simple peak finding algorithm, and assume that each peak is a breath.

I can then calculate the breathing rate in breaths per minute.

You can see the resulting output, as displayed via the web interface on the <a href="{{site.baseurl}}/meta/2014/01/12/Example_Output/">Example_Output</a> page.

<h2>Current Status</h2>
At the moment it is a functioning prototype - it can detect that I am breathing,
 and alarm if I can hold my breath long enough (it is using a 30 second time 
series, and it is hard to stay perfectly still for that long!).
The benTV digital video monitors talk to the web interface to display the 
results and display alarms by chaning colour (I don't trust it enough yet to 
make an audible alarm in the night...).
