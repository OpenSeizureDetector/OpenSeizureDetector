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


/* ANALYSIS CONFIGURATION */
#define SAMP_FREQ 100    // Sample Frequency in Hz
#define SAMP_FREQ_STR ACCEL_SAMPLING_100HZ  // String to pass to sampling system.
#define NSAMP 512       // number of samples of accelerometer data to collect.
#define FFT_BITS 9        // 'bits' parameter to fft_forward.


// default values of settings
#define ALARM_FREQ_MIN_DEFAULT 5  // Hz
#define ALARM_FREQ_MAX_DEFAULT 10 // Hz
#define WARN_TIME_DEFAULT      10 // sec
#define ALARM_TIME_DEFAULT     20 // sec
#define ALARM_THRESH_DEFAULT   200 // Power of spectrum between ALARM_FREQ_MIN and
                           // ALARM_FREQ_MAX that will indicate an alarm
                           // state.

/* Display Configuration */
#define CLOCK_SIZE 30  // pixels.
#define ALARM_SIZE 30  // pixels.
#define SPEC_SIZE 30   // pixels


/* Phone Communications */
// Analysis Results
#define KEY_ALARMSTATE 2
#define KEY_MAXVAL 3
#define KEY_MAXFREQ 4
#define KEY_SPECPOWER 5
// Settings
#define KEY_SETTINGS 6    // Phone is requesting watch to send current settings.
#define KEY_ALARM_FREQ_MIN 7
#define KEY_ALARM_FREQ_MAX 8
#define KEY_WARN_TIME 9
#define KEY_ALARM_TIME 10
#define KEY_ALARM_THRESH 11



/* GLOBAL VARIABLES */
// Settings (obtained from default constants or persistent storage)
extern int alarmFreqMin;    // Bin number of lower boundary of region of interest
extern int alarmFreqMax;    // Bin number of higher boundary of region of interest
extern int warnTime;        // number of seconds above threshold to raise warning
extern int alarmTime;       // number of seconds above threshold to raise alarm.
extern int alarmThresh;     // Alarm threshold (average power of spectrum within
                     //       region of interest.

extern int accDataPos;   // Position in accData of last point in time series.
extern int accDataFull;  // Flag so we know when we have a complete buffer full
                      // of data.
extern AccelData latestAccelData;  // Latest accelerometer readings received.
extern int maxVal;       // Peak amplitude in spectrum.
extern int maxLoc;       // Location in output array of peak.
extern int maxFreq;      // Frequency corresponding to peak location.
extern long specPower;   // Average power of whole spectrum.
extern long roiPower;    // Average power of spectrum in region of interest
extern int freqRes;      // Actually 1000 x frequency resolution

extern int alarmState;    // 0 = OK, 1 = WARNING, 2 = ALARM
extern int alarmCount;    // number of seconds that we have been in an alarm state.


/* Functions */
// from comms.c
void inbox_received_callback(DictionaryIterator *iterator, void *context);
void inbox_dropped_callback(AppMessageResult reason, void *context);
void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context);
void outbox_sent_callback(DictionaryIterator *iterator, void *context);
void sendSdData();
void comms_init();


// from analysis.c
void analysis_init();
int alarm_check();
void accel_handler(AccelData *data, uint32_t num_samples);
void do_analysis();
int getAmpl(int nBin);
