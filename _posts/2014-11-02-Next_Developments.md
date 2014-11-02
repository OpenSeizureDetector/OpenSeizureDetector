---
layout: post
title: Next Developments
category: Meta

excerpt: The plan for development of the next version of the kinect seizure detector.

---

# Current Version
The current version (as of ~April 2014) works by using background subtraction to define the region of interest to be analysed (it looks for the biggest object that is not in the background image).
Within the region of interest it works out the difference between each frame and the average background, and works out the total intensity of the region.
It looks for peaks within the history of background intensity, which it attributes to breathing events.
That is, the current version is really an apnoea (apnea) detector rather than a seizure detector.

The structure of the software is such that a single programme does all of the following:

1. Collect frames from the kinect.
1. Process the frames to produce a time series of image intensity.
1. Analyse the time series to find peaks and determine breathing rate.
1. Produce images of graphs of the time series.
1. Determine the alarm state based on the calculated breathing rate.
1. Provide a simple web interface to communicate with clients to provide alarm functionality.

# Structure of Version 2
Separate the program into three separate modules:

2. Data logger - collects frames and produces a time series of frames.
2. Data analyser - analyse the time series to assess movement rate.
2. User Interface - web application that serves the assessed movement rate and alarm state to clients.
