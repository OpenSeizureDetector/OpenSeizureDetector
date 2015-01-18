OpenSeizureDetector - Pebble Version
====================================

This seizure detector uses a (Pebble)[http://getpebble.com] smart watch.
The watch has an accelerometer and vibrator motor and a bluetooth radio
to talk to another computer.  Most importantly, it comes with instructions to
write code for it on the manufacturers (web site)[http://developer.getpebble.com].   

So it sounds like an ideal platform for an accelerometer based seizure detector
similar to the Arduino based one I made in 2013.

Principle of Operation
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


Licence
=======
My code is licenced under the GNU Public Licence - for associated libraries 
please see Credits below.

Credits
=======
The following libraries are used:
* (KissFFT)[http://sourceforge.net/projects/kissfft/] by Mark Borgerding
* (fix_fft)[http://www.jjj.de%2Fcrs4%2Finteger_fft.c] - there are lots of copies of this about on the internet, and I don't know whose is the original, its licence, or who to credit.
* (SYLT-FFT)[https://github.com/stg/SYLT-FFT] by D. Taylor.


Graham Jones, 15 January 2015.  (grahamjones139+sd@gmail.com)