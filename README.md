OpenSeizureDetector
===================

A suite of open source and open hardware projects designed to detect and 
alert people to someone suffering from an epileptic seizure (fit).   
This is a work in progress at the moment, and three different technologies 
are being developed to see which work best: 
   * Acceleration Detection
   * Audio Detection
   * Video Detection

Acceleration Detection
======================
It is based on an accelerometer monitoring movement.  It uses a fourier
transform to extract the frequency spectrum of the movement, and monitors
movements in a given frequency band.   The idea is that it will detect the
rhythmic movements associated with a seizure, but not normal day to day
activities.

If the acceleration within the given frequency band is more than a
threshod value, it starts a timer.  If the acceleration remains above
the threshold for a given period, it issues a warning beep.
If it remains above the threshold for a longer specified period, the unit
alarms (continuous tone rather than beep).

This is a development version so it contains a real time clock and SD card
to record the measured spectra to help optimise the device.

My initial intention is to mount this on a floor board on which our son
sleeps (he will not sleep in a bed...).  It may not be sensitive enough to
pick up the movement through the floor, so it may have to be turned into a 
wearable device.

Audio Detection
===============
No working prototype yet....

Video Detection
===============
Getting there. See http://nerdytoad.blogspot.co.uk/2013/03/first-go-at-video-based-epileptic.html.




Graham Jones, 02 February 2013.  (grahamjones139+sd@gmail.com)