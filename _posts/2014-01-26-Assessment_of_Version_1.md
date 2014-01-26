---
layout: post
title: Assessment of Version 1 of Seizure Detector
category: Meta

excerpt: A Review of Version 1 of the seizure detector, after it has been on soak test for 2 weeks.

---

The initial version of the seizure detector (or should it really be called an
apnea monitor?) has been installed on test in our house for 2 weeks.
This version raises only visual alarms, because I don't trust it enough yet
to produce audible alarms in the middle of the night.   Its function is to
detect an abnormally low breating rate, which occurs after a major seizure.

The findings of this initial soak test are:
  *  It basically works - the assessed breathing rate falls in deep sleep 
compared to lighter sleep with 'fidgeting', so it is promising.
  *  It needs a good background image to work reliably - without this it may
average the intensity of a larger area of the room (not just Benjamin), which 
reduces its sensitivity.   I added a 'reset background image' to the raspberry
pi viewers so that Sandie can easily reset the background herself when necessary.   Another symptom of the background being incorrect is the system alarming if 
Benjamin is not in the room - it may monitor his pile of toys rather than saying
it can not find Benjamin.
  * It gives some false alarms during deep sleep, so we need a slightly higher
sensitivity for breath detection.

So it looks promising, but some development is necessary before I connect it to
an audible alarm.   The main development is to tune the peak detection 
parameters to reduce the false alarms.   To achieve this, I have added more
functionality to benFinder.py so that it records a short video of the raw
data periodically, along with the assessed brightness timeseries, so I can see
if I can find a better way of analysing the very small movement cases.   It
also saves a video and time series when it alarms.

I will give it another week on soak test collecting this data, then have a go
at tuning the parameters to get the required sensitivity.

A later improvement will be to change the Benjamin detection from just being the largest object in the field of view, to doing some image processing to look for
somethign that looks like him - this should reduce the requirement for having a good background image.

