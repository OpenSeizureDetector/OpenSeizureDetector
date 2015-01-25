/*
  fft_test.c - simple test of fft library being used for pebble
  seizure detector.

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

/* These undefines prevent SYLT-FFT using assembler code */
#undef __ARMCC_VERSION
#undef __arm__
#include "src/SYLT-FFT/fft.h"
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

/* CONFIGURATION */
#define SAMP_FREQ 100    // Sample Frequency in Hz
#define SAMP_FREQ_STR ACCEL_SAMPLING_100HZ  // String to pass to sampling system.
#define NSAMP 256       // number of samples of accelerometer data to collect.
#define FFT_BITS 8        // 'bits' parameter to fft_forward.

/* GLOBAL VARIABLES */
uint32_t num_samples = NSAMP;
short accData[NSAMP];   // Using short into for compatibility with integer_fft library.
fft_complex_t fftdata[NSAMP];

int accDataPos = 0;   // Position in accData of last point in time series.
int accDataFull = 0;  // Flag so we know when we have a complete buffer full
                      // of data.
int fftOK = 0;
int maxVal = 0;
int maxLoc = 0;

/**
 * Populate accelData with test data for analysis.
 */
static void populate_data() {
  int i;
  float dt = 1./(float)SAMP_FREQ;

  float dFreq = 5;  // Hz.
  float omega = 2*M_PI*dFreq;

  printf ("dT=%f\nomega=%f\n",dt,omega);
  for (i=0;i<NSAMP;i++) {
    float t = i*dt;
    accData[accDataPos] = abs((int)(100*sin(omega*t)));
    printf("%04d,",accData[accDataPos]);
    accDataPos++;
  }
  accDataFull = 1;
  printf("\npopulate_data complete\n");

}

/****************************************************************
 * Carry out analysis of acceleration time series.
 * Called from clock_tick_handler().
 */
static void do_analysis() {
  unsigned bits;
  int i;
  printf("do_analysis\n");
  for (i=0;i<NSAMP;i++) {
    // FIXME - this needs to recognise that accData is actually a rolling buffer and re-order it too!
    fftdata[i].r = accData[i];
    fftdata[i].i = 0;
  }
  printf("Calling FFT\n");
  // Do the FFT conversion from time to frequency domain.
  fft_fft(fftdata,FFT_BITS);
  fftOK = 0;
  printf("Fft Complete\n");
  // Look for maximm amplitude, and location of maximum.
  // Ignore DC component (position zero)
  maxVal = fftdata[1].r;
  maxLoc = 1;
  for (i=1;i<NSAMP/2;i++) {
    // Find absolute value of the imaginary fft output.
    fftdata[i].r = fftdata[i].r*fftdata[i].r + fftdata[i].i*fftdata[i].i;
    if (fftdata[i].r>maxVal) {
      maxVal = fftdata[i].r;
      maxLoc = i;
    }
  }

  printf("\nSpectrum\n");
  for (i=0;i<NSAMP/2;i++) {
    printf("%04d,",(int)fftdata[i].r);
  }
  printf("\n");

  printf("maxVal = %d, maxLoc=%d, ",maxVal,maxLoc);
  printf("maxFreq = %d\n",maxLoc*SAMP_FREQ/NSAMP);

  accDataPos = 0;
  accDataFull = 0;

}


/**
 * main():  Main programme entry point.
 */
int main(void) {
  printf("fft_test\n");
  printf("SAMP_FREQ=%d Hz - fMax = %d\n ",SAMP_FREQ,SAMP_FREQ/2);
  printf("NSAMP = %d\n",NSAMP);
  printf("T_SAMP = %d\n",NSAMP/SAMP_FREQ);
  printf("Freq Res = %f Hz\n", (float)SAMP_FREQ/NSAMP);


  populate_data();
  do_analysis();
}
