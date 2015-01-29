/*
  Pebble_sd - a simple accelerometer based seizure detector that runs on a
  Pebble smart watch (http://getpebble.com).

  See http://openseizuredetector.org for more information.

  Copyright Graham Jones, 2015.

  This file is part of pebble_sd.

  Pebble_sd is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  Pebble_sd is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with pebble_sd.  If not, see <http://www.gnu.org/licenses/>.

*/
#include <pebble.h>
/* These undefines prevent SYLT-FFT using assembler code */
#undef __ARMCC_VERSION
#undef __arm__
#include "SYLT-FFT/fft.h"

#include "pebble_sd.h"


/* GLOBAL VARIABLES */
// Settings (obtained from default constants or persistent storage)

uint32_t num_samples = NSAMP;
short accData[NSAMP];   // Using short into for compatibility with integer_fft library.
fft_complex_t fftdata[NSAMP];   // spectrum calculated by FFT

int accDataPos = 0;   // Position in accData of last point in time series.
int accDataFull = 0;  // Flag so we know when we have a complete buffer full
                      // of data.



/*************************************************************
 * Data Analysis
 *************************************************************/

/*********************************************
 * Returns the magnitude of a complex number
 * (well, actually magnitude^2 to save having to do
 * a square root.
 */
int getMagnitude(fft_complex_t c) {
  int mag;
  mag = c.r*c.r + c.i*c.i;
  return mag;
}

int getAmpl(int nBin) {
  return fftdata[nBin].r;
}


/***********************************************
 * Analyse spectrum and set alarm condition if
 * appropriate.
 * This routine assumes it is called every second to check the 
 * spectrum for an alarm state.
 */
int alarm_check() {
  int nMin = 1000*alarmFreqMin/freqRes;
  int nMax = 1000*alarmFreqMax/freqRes;

  APP_LOG(APP_LOG_LEVEL_DEBUG,"Alarm Check nMin=%d, nMax=%d",nMin,nMax);

  for (int i=nMin;i<nMax;i++) {
    roiPower = roiPower + fftdata[i].r;
  }
  roiPower = roiPower/(nMax-nMin);
  APP_LOG(APP_LOG_LEVEL_DEBUG,"roiPower=%ld",roiPower);

  if (roiPower>alarmThresh) {
    alarmCount++;
    if (alarmCount>alarmTime) {
      alarmState = 2;
    } else if (alarmCount>warnTime) {
      alarmState = 1;
    }
  } else {
    alarmState = 0;
    alarmCount = 0;
  }
  APP_LOG(APP_LOG_LEVEL_DEBUG,"alarmState = %d, alarmCount=%d",alarmState,alarmCount);

  return(alarmState);
}


/**
 * accel_handler():  Called whenever accelerometer data is available.
 * Add data to circular buffer accData[] and increments accDataPos to show
 * the position of the latest data in the buffer.
 */
void accel_handler(AccelData *data, uint32_t num_samples) {
  int i;

  // Add the new data to the accData buffer
  for (i=0;i<(int)num_samples;i++) {
    // Wrap around the buffer if necessary
    if (accDataPos>=NSAMP) { 
      accDataPos = 0;
      accDataFull = 1;
      break;
    }
    // Ignore any data when the vibrator motor was running.
    // FIXME - this doesn't seem to work - alarm latches on if the 
    //         vibrator operates.
    if (!data[i].did_vibrate) {
      // add good data to the accData array
      accData[accDataPos] = abs(data[i].x) + abs(data[i].y) + abs(data[i].z);
      accDataPos++;
    }
  }
  latestAccelData = data[num_samples-1];

}

/****************************************************************
 * Carry out analysis of acceleration time series.
 * Called from clock_tick_handler().
 */
void do_analysis() {
  int i;
  APP_LOG(APP_LOG_LEVEL_DEBUG,"do_analysis");

  // Calculate the frequency resolution of the output spectrum.
  // Stored as an integer which is 1000 x the frequency resolution in Hz.
  freqRes = (int)(1000*SAMP_FREQ/NSAMP);
  APP_LOG(APP_LOG_LEVEL_DEBUG,"freqRes=%d",freqRes);
  for (i=0;i<NSAMP;i++) {
    // FIXME - this needs to recognise that accData is actually a rolling buffer and re-order it too!
    fftdata[i].r = accData[i];
    fftdata[i].i = 0;
  }
  // Do the FFT conversion from time to frequency domain.
  fft_fft(fftdata,FFT_BITS);
  // Look for maximm amplitude, and location of maximum.
  // Ignore position zero though (DC component)
  maxVal = getMagnitude(fftdata[1]);
  maxLoc = 1;
  specPower = 0;
  for (i=1;i<NSAMP/2;i++) {
    // Find absolute value of the imaginary fft output.
    fftdata[i].r = getMagnitude(fftdata[i]);
    specPower = specPower + fftdata[i].r;
    if (fftdata[i].r>maxVal) {
      maxVal = fftdata[i].r;
      maxLoc = i;
    }
  }
  maxFreq = (int)(maxLoc*freqRes/1000);
  specPower = specPower/(NSAMP/2);
  APP_LOG(APP_LOG_LEVEL_DEBUG,"specPower=%ld",specPower);
  /* Start collecting new buffer of data */
  /* FIXME = it would be best to make this a rolling buffer and analyse it
  * more frequently.
  */
  accDataPos = 0;
  accDataFull = 0;
}

void analysis_init() {
  /* Subscribe to acceleration data service */
  accel_data_service_subscribe(NSAMP,accel_handler);
  // Choose update rate
  accel_service_set_sampling_rate(SAMP_FREQ_STR);
}

