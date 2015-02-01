---
layout: post
title: Pebble Smart Watch Based Seizure Detector
category: Meta

excerpt: Working prototype of an accelerometer based seizure detector using
a Pebble Smart Watch and associated Android mobile phone.

---

# Pebble Smart Watch Version
The [Pebble](http://getpebble.com) smart watch is a very promising candidate 
to use for an accelerometer based seizure detector.
It has a built in accelerometer, rechargeable battery, bluetooth radio, 
buzzer and display, and it all comes in a wrist watch sized package.

Most importantly the manufacturer provides a software development kit and 
quite a lot of documentation to allow you to write your own software for the 
device.   This project is therefore to develop my [arduino proof of concept 
accelerometer based fit detector](http://jones139.github.io/OpenSeizureDetector/meta/2014/02/21/Accelerometer_Version/) to the Pebble to get a working prototype 
device.

# System Configuration
The configuration of the prototype seizure detector and alarm system is shown below:
<img class="img_med" style="float:left;width:400px;" src="https://raw.githubusercontent.com/jones139/OpenSeizureDetector/master/pebble_version/Android_Pebble_SD/assets/www/img/diagram.jpg">

The main components of the current prototype are described briefly below.

## Pebble Smart Watch
The pebble watch will need to be worn on the wrist or ankle to give significant
movement in the event of a tonic-clonic seizure.

The programme that runs on the watch does the following:

1. Collect accelerometer data at 100 Hz for 10 seconds.
2. Calculate the fourier transform of the accelerometer data to give a frequency spectrum.
3. Analyse the spectrum to calculate the spectrum power in a given frequency band, which we will take to be representative of seizure movement.
4. Monitor the time that the spectrum power is above a threshold level.
5. If the power is above the threshold for a specified time, enter a warning
   or alarm state, depending on how long it has been abvove the threshold.
6. Periodically (every second) send the latest analysis data to the Android phone for recording and initiation of alarms etc.

The information shown on the watch display is shown below:
![](https://raw.githubusercontent.com/jones139/OpenSeizureDetector/master/pebble_version/screenshot_1.png)

## Android Phone
The android phone will need to be within a few metres of the Pebble watch so they can communicate using bluetooth.   It does not do any significant analysis, just acts as a bridge between the Pebble Watch and other applications.

The background service application that runs on the phone does the following:

1.  Monitor the bluetooth connection to the smart watch.
2.  Check that the seizure detector app is running on the watch, and start it ifit is not.
3.  Receive messages containing analysis data from the Pebble smart watch.
4.  Provide a web based interface to the analysis data.


## Web Interface
The Android Phone web server provides a simple web interface to the data, which does the following:
1.  Periodically request a set of data from the android phone (as a JSON string)
2.  Interpret the string into specific data items.
3.  Update the web page text with the latest data.
4.  If alarm conditions are raised, change the colour of the background of the
    relevant item to draw the user's attention to it.

The current prototype web interface in both normal and alarm modes is shown below:
<img class="img_med" style="float:right;width:300px;" src="{{site.baseurl}}/resources/img/Pebble_SD_Web_Sccreenshot.jpg">
<img class="img_med" style="float:right;width:300px;" src="{{site.baseurl}}/resources/img/Pebble_SD_Web_Sccreenshot_alarm.jpg">

# Next Steps for Development

1.  Provide more diagnostic information to the web interface (e.g. fft spectrum), to help with determining the most suitable frequency region of interest and
power threshold (the current settings give false alarms for normal walking about).
2.  Provide some data logging on the phone so we can analyse historical data
to improve performance.
3.  Make it easy to alter analysis settings via the Android App or web
    interface to help with tuning the system.
4.  Add an audible alarm to the web interface (quite easy because I have that on
the android client for the kinect prototype.
5.  Review stability of the system (we don't want too many crashes or false alarms).
6.  Persuade Benjamin to wear the watch - quite a difficult one!
7.  Modify our digital video monitors to connect to the Android Phone and
    respond to the data.
8.  A possible future development could be 'push' notifications where the android app sends an SMS message or calls a number in an alarm condition.



# Technical Details

## Software Development for Pebble

1.  Install the [Pebble SDK](http://developer.getpebble.com/sdk/) (there is a cloud based sdk too, but I am old fashioned and like to run the compiler on my own computer...).
1.  Install the Pebble App on your Android Phone, and pair it with the Pebble - this is because the installation tool talks to the phone, rather than the pebble directly (I haven't managed to get direct bluetooth comms to the Pebble working).
1.  Create a new project and skelleton app with 'pebble new-project <project name>' - creates a directory structure for the project, and produces a simple demo skeleton app.
1.  Compile it with 'pebble build'
1.  Install to phone with 'pebble install --phone 192.168.1.175'  (the ip address will of course vary - needs to be the ip address of your phone on your local network.

### Direct Bluetooth Connection
It is possible to use a direct bluetooth connection to the pebble rather than going via the phone.  To do this you need to pair your computer with the pebble:
> sudo bluez-simple-agent hci0 00:17:e9:dc:e9:af       [Obviously MAC address will be different!]

>  sudo bluez-test-device trusted 00:17:E9:dc:e9:af yes

>  sudo /etc/init.d/bluetooth restart

After this you can install apps directly to the pebble using

>   pebble install --pebble_id 00:17:E9:DC:E9:AF

Direct bluetooth connection seems a bit unreliable though - it will fail if there is a phone connected to the pebble, and sometimes I need to switch the pebble bluetooth off and on a again to get it to work.

 

## Sampling System Design

### What are we trying to detect
To detect a tonic-clonic seizure, I think I am looking for movement in the 5-10 Hz frequency range for several seconds.   
I do not know the axis of the movement, so will have to just combine the 3 acceleration readings into a single value.
A secondary objective could be to detect breathing related movements, which will probably be in the 0.1-0.2 Hz range.

## Sampling System
The Pebble watch has a 3 axis accelerometer, which we can sample at 10, 25 or 100 Hz.   The accelerometer detects the acceleration due to gravity, so one value is always around 980 mg, even with the device stationary.

### FFT Design
Based on a handy FFT summary by [National Instruments](http://zone.ni.com/reference/en-XX/help/372416B-01/svtconcepts/fft_funda/).

#### Frequency Resolution
The frequency resolution (frequency range covered by each fft output data point) is 1/T, where T is the sample acquisition time.   

This means if we collect samples for 1 sec, our output will be in 1Hz steps.   If we collect for 10 sec we will have 0.1Hz steps.   For this application, to collect seizure type movements, 1Hz is probably enough, but if we want to detect breathing movements too, we probably want higher resolution so that we can detect sub 1Hz frequency movements.

So, if we have enough memory, we will collect 10 seconds worth of data into a rolling buffer, but analyse it every second while we are testing to see what we can detect.

#### Maximum Frequency
The maximum frequency detectable by FFT f_max is 1/fs, where fs is the sampling frequency.  On the pebble we have three frequencies to choose from:
* 10 Hz - this would give f_max = 5 Hz.   I suspect this is not good enough - I think a tonic-clonic seizure will give us movement in the 5-10 Hz range.
* 25 Hz - would give f_max = 12.5 Hz - this will probably be good enough for us, but if some human movement is over 12.5 Hz, we will get artifacts appearing in the lower frequency bins, which may confuse us.
* 100 Hz - would give f_max = 50 Hz - definitely more than the sort of frequencies I am looking for, so little risk of getting artifacts in the lower frequency bins, but 10 sec of data at 100 Hz = 1000 data points - not sure how much memory we have on the Pebble, so this may be beyond its capability.


### Design Summary
Collect data for 10 seconds.   Ideally use 100 Hz sampling frequency, but if this proves to be too memory intensive, fall back to 25 Hz sampling frequency, which will probably be ok.

### Expected Results
At 100 Hz sample frequency, collecting data for 10 seconds (=1000 samples), the frequency resolution will be 0.1 Hz.
This means that the expected 5-10 Hz movement due to a seizure will occur in output bins 50-100.
Breathing related movements at say 0.1-0.2 Hz will be in output bins 1 and 2.

We expect to have a very high DC signal (=output bin 0) because the accelerometer detects the acceleration due to gravity which is a constant significant signal upon which the movement is imposed.   Given a small amount of noise, the DC signal will be smeared over several bins, which means that the breathing related movement will be swamped.   To detect that we will need to sample for a longer period (but we will not need to sample at 100 Hz).
