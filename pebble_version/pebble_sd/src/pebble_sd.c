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


/* ANALYSIS CONFIGURATION */
#define SAMP_FREQ 100    // Sample Frequency in Hz
#define SAMP_FREQ_STR ACCEL_SAMPLING_100HZ  // String to pass to sampling system.
#define NSAMP 512       // number of samples of accelerometer data to collect.
#define FFT_BITS 9        // 'bits' parameter to fft_forward.

#define ALARM_FREQ_MIN 5  // Hz
#define ALARM_FREQ_MAX 10 // Hz
#define WARN_TIME      10 // sec
#define ALARM_TIME     20 // sec
#define ALARM_THRESH   200 // Power of spectrum between ALARM_FREQ_MIN and
                           // ALARM_FREQ_MAX that will indicate an alarm
                           // state.

/* Display Configuration */
#define CLOCK_SIZE 30  // pixels.
#define ALARM_SIZE 30  // pixels.
#define SPEC_SIZE 30   // pixels

/* GLOBAL VARIABLES */
static Window *window;
static TextLayer *text_layer;
static TextLayer *alarm_layer;
static TextLayer *clock_layer;
static Layer *spec_layer;
uint32_t num_samples = NSAMP;
short accData[NSAMP];   // Using short into for compatibility with integer_fft library.
fft_complex_t fftdata[NSAMP];   // spectrum calculated by FFT

int accDataPos = 0;   // Position in accData of last point in time series.
int accDataFull = 0;  // Flag so we know when we have a complete buffer full
                      // of data.
AccelData latestAccelData;  // Latest accelerometer readings received.
int maxVal = 0;       // Peak amplitude in spectrum.
int maxLoc = 0;       // Location in output array of peak.
int maxFreq = 0;      // Frequency corresponding to peak location.
long specPower = 0;   // Average power of whole spectrum.
int freqRes = 0;      // Actually 1000 x frequency resolution

int alarmState = 0;    // 0 = OK, 1 = WARNING, 2 = ALARM
int alarmCount = 0;    // number of seconds that we have been in an alarm state.

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


/***********************************************
 * Analyse spectrum and set alarm condition if
 * appropriate.
 * This routine assumes it is called every second to check the 
 * spectrum for an alarm state.
 */
int alarm_check() {
  int nMin = 1000*ALARM_FREQ_MIN/freqRes;
  int nMax = 1000*ALARM_FREQ_MAX/freqRes;

  APP_LOG(APP_LOG_LEVEL_DEBUG,"Alarm Check nMin=%d, nMax=%d",nMin,nMax);
  int fPow = 0;

  for (int i=nMin;i<nMax;i++) {
    fPow = fPow + fftdata[i].r;
  }
  fPow = fPow/(nMax-nMin);
  APP_LOG(APP_LOG_LEVEL_DEBUG,"fPow=%d",fPow);

  if (fPow>ALARM_THRESH) {
    alarmCount++;
    if (alarmCount>ALARM_TIME) {
      alarmState = 2;
    } else if (alarmCount>WARN_TIME) {
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
static void accel_handler(AccelData *data, uint32_t num_samples) {
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
static void do_analysis() {
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


/************************************************************************
 * clock_tick_handler() - Analyse data and update display.
 * Updates the text layer clock_layer to show current time.
 * This function is the handler for tick events.*/
static void clock_tick_handler(struct tm *tick_time, TimeUnits units_changed) {
  static char s_time_buffer[16];
  static char s_alarm_buffer[64];
  static char s_buffer[256];

  // Do FFT analysis
  if (accDataFull) 
    do_analysis();

  // Check the alarm state, and set the global alarmState variable.
  alarm_check();

  // Do something with the alarm - vibrate the pebble watch 
  //  and display message on screen.
  if (alarmState == 0) {
    text_layer_set_text(alarm_layer, "OK");
  }
  if (alarmState == 1) {
    //vibes_short_pulse();
    text_layer_set_text(alarm_layer, "WARNING");
  }
  if (alarmState == 2) {
    //vibes_long_pulse();
    text_layer_set_text(alarm_layer, "** ALARM **");
  }

  // Update data display.
  snprintf(s_buffer,sizeof(s_buffer),
	   "max=%d, P=%ld\n%d Hz",
	   /*latestAccelData.x, latestAccelData.y, latestAccelData.z,*/
	   maxVal,specPower,maxFreq
	   );
  text_layer_set_text(text_layer, s_buffer);

  // and update clock display.
  if (clock_is_24h_style()) {
    strftime(s_time_buffer, sizeof(s_time_buffer), "%H:%M:%S", tick_time);
  } else {
    strftime(s_time_buffer, sizeof(s_time_buffer), "%I:%M:%S", tick_time);
  }
  BatteryChargeState charge_state = battery_state_service_peek();
  snprintf(s_time_buffer,sizeof(s_time_buffer),
	   "%s %d%%", 
	   s_time_buffer,
	   charge_state.charge_percent);
  text_layer_set_text(clock_layer, s_time_buffer);
}


/**************************************************************************
 * draw_spec() - draw the spectrum to the pebble display
 */
void draw_spec(Layer *sl, GContext *ctx) {
  GRect bounds = layer_get_bounds(sl);
  GPoint p0;
  GPoint p1;
  int i,h;

  /* Now draw the spectrum */
  for (i=0;i<bounds.size.w-1;i++) {
    p0 = GPoint(i,bounds.size.h-1);
    //h = bounds.size.h*accData[i]/2000;
    h = bounds.size.h*fftdata[i].r/1000.;
    p1 = GPoint(i,bounds.size.h - h);
    graphics_draw_line(ctx,p0,p1);
  }

}

/***********************************************************************
  Button event handlers
*/
static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Select");
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Up");
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Down");
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}
/**********************************************************************/

/**
 * window_load(): Initialise main window.
 */
static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  text_layer = text_layer_create(
				 (GRect) { 
				   .origin = { 0, 0 }, 
				   .size = { bounds.size.w, 
					     bounds.size.h
					     -CLOCK_SIZE
					     -ALARM_SIZE
					     -SPEC_SIZE } 
				 });
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  text_layer_set_font(text_layer, 
		      fonts_get_system_font(FONT_KEY_GOTHIC_24));
  layer_add_child(window_layer, text_layer_get_layer(text_layer));

  /* Create text layer for alarm display */
  alarm_layer = text_layer_create(
				 (GRect) { 
				   .origin = { 0, bounds.size.h 
					       - CLOCK_SIZE
					       - ALARM_SIZE
					       - SPEC_SIZE
				   }, 
				   .size = { bounds.size.w, ALARM_SIZE } 
				 });
  text_layer_set_text(alarm_layer, "ALARM");
  text_layer_set_text_alignment(alarm_layer, GTextAlignmentCenter);
  text_layer_set_font(alarm_layer, 
		      fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(alarm_layer));


  /* Create layer for spectrum display */
  spec_layer = layer_create(
				 (GRect) { 
				   .origin = { 0, bounds.size.h - CLOCK_SIZE - SPEC_SIZE }, 
				   .size = { bounds.size.w, SPEC_SIZE } 
				 });
  layer_set_update_proc(spec_layer,draw_spec);
  layer_add_child(window_layer, spec_layer);

  /* Create text layer for clock display */
  clock_layer = text_layer_create(
				 (GRect) { 
				   .origin = { 0, bounds.size.h - CLOCK_SIZE }, 
				   .size = { bounds.size.w, CLOCK_SIZE } 
				 });
  text_layer_set_text(clock_layer, "CLOCK");
  text_layer_set_text_alignment(clock_layer, GTextAlignmentCenter);
  text_layer_set_font(clock_layer, 
		      fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(clock_layer));
}

/**
 * window_unload():  destroy window contents
 */
static void window_unload(Window *window) {
  text_layer_destroy(text_layer);
  text_layer_destroy(alarm_layer);
  text_layer_destroy(clock_layer);
  layer_destroy(spec_layer);
}

/**
 * init():  Initialise application - create window for display and register
 * for accelerometer data readings.
 */
static void init(void) {
  window = window_create();
  window_set_click_config_provider(window, click_config_provider);
  window_set_window_handlers(window, (WindowHandlers) {
    .load = window_load,
    .unload = window_unload,
  });
  const bool animated = true;
  window_stack_push(window, animated);

  /* Subscribe to acceleration data service */
  accel_data_service_subscribe(num_samples,accel_handler);
  // Choose update rate
  accel_service_set_sampling_rate(SAMP_FREQ_STR);

  /* Subscribe to TickTimerService */
  tick_timer_service_subscribe(SECOND_UNIT, clock_tick_handler);
}

/**
 * deinit(): destroy window before app exits.
 */
static void deinit(void) {
  window_destroy(window);
}

/**
 * main():  Main programme entry point.
 */
int main(void) {
  init();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);
  app_event_loop();
  deinit();
}
