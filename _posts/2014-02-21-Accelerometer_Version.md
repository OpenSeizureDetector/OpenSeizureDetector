---
layout: post
title: Accelerometer Based Seizure Detector
category: Meta

excerpt: Description of an Accelerometer Based Seizure Detector Prototype.

---

# Hardware
The system uses an Arduino microcontroller, connected to a Freescale MMA7361 three axis accelerometer.   
The accelerometer is a tiny (5mm x 3mm) surface mount device, so soldering it is a challenge - you can see how I did it [here](http://nerdytoad.blogspot.co.uk/2013/02/soldering-onto-surface-mount-ics.html).

![Accelerometer Chip](https://lh4.googleusercontent.com/n2HoXsOBqJQAbcALaIydwQiu_CNj57ycgmDQPHXTqbs=s216-p-no =200x)
To enable data logging so I can tune it to get the frequency response, threshold etc. the arduino is also connected to a real time clock module and a SD card module.

The completed prototype is shown below:
[![Prototype](https://lh5.googleusercontent.com/Cu5eJ3uqflvnI4a-aiFnALWesHUGRBbf5M2B8ivMoGY=s216-p-no)](https://plus.google.com/u/0/photos/yourphotos?pid=5843074365919100130&oid=106497137253664241170 =200x)

# Software
The code is in the [accelerometer_version folder of the github repository](https://github.com/jones139/OpenSeizureDetector/tree/master/accelerometer_version).
It uses fourier transforms to obtain the frequency spectrum of the accelerometer data.  It issues a warning beep if the amplitude over a given frequency range exceeds a threshold for a given period, or a full alarm buzz if the movement is sustained for a longer period..

# Testing
And here is a simple demonstration of it working - you can hear the warning 'pip' and the alarm 'buzz' in the background when I shake my arm to simulate a seizure. 
[![Demo Video](https://lh3.googleusercontent.com/e9RF1-CexlMlUvNHQBvaK8PIdPpEoiF5q_8MGxw_aaQ=s216-p-no)](https://plus.google.com/u/0/photos/yourphotos?pid=5843066251226050578&oid=106497137253664241170)

The analysis of the data from me shaking my arm to simulate a seizure with the device warn on the wrist or on the biscep is shown below:
[![chart](https://lh4.googleusercontent.com/8J4-USZfbs1DFzxRUbRSvSEd93HSY_LM13jhw3tfxbk=s216-p-no)](https://plus.google.com/photos/yourphotos?pid=5844573194261599170&oid=106497137253664241170)
It shows that the sensitivity is comfortably above the background noise level when worn on the wrist, but if it is worn on the biscep the signal is close to the noise level.   This means that this is a viable seizure detector, provided the patient will wear the accelerometer on the wrist (or maybe ankle).

# Further Development
To make this a viable system we would need to

* Make a lightweight wrist mounting (probably a wrist watch case).
* Link to a processor and battery pack via wires.
* Or get hold of a device that includes accelerometer, processor and battery in one device - maybe an android device mounted in a wrist watch such as [this](http://www.ebay.co.uk/itm/NEW-White-Z1-Android-Wrist-Watch-Mobile-Phone-WiFi-GPS-2-0-TFT-Touch-Screen-8GB-/151055243441).
* Then persuade the patient to wear it - not easy in Benjamin's case!.


