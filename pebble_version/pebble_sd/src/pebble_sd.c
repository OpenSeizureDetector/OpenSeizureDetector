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


/* CONFIGURATION */
#define SAMP_FREQ 100    // Sample Frequency in Hz
#define SAMP_FREQ_STR ACCEL_SAMPLING_100HZ  // String to pass to sampling system.
#define NSAMP 512       // number of samples of accelerometer data to collect.
#define FFT_BITS 9        // 'bits' parameter to fft_forward.
#define CLOCK_SIZE 30  // pixels.
#define SPEC_SIZE 30   // pixels

/* GLOBAL VARIABLES */
static Window *window;
static TextLayer *text_layer;
static TextLayer *clock_layer;
static TextLayer *spec_layer;
uint32_t num_samples = NSAMP;
short accData[NSAMP];   // Using short into for compatibility with integer_fft library.
int accDataPos = 0;   // Position in accData of last point in time series.
int accDataFull = 0;  // Flag so we know when we have a complete buffer full
                      // of data.
AccelData latestAccelData;
int fftOK = 0;
int maxVal = 0;
int maxLoc = 0;

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
    accData[accDataPos] = data[i].x + data[i].y + data[i].z;
    accDataPos++;
  }
  latestAccelData = data[num_samples-1];

}

/* Carry out analysis of acceleration time series.
 * Called from clock_tick_handler().
 */
static void do_analysis() {
  static fft_complex_t fftdata[NSAMP];
  unsigned bits;
  int i;
  APP_LOG(APP_LOG_LEVEL_DEBUG,"do_analysis");
  for (i=0;i<NSAMP;i++) {
    // FIXME - this needs to recognise that accData is actually a rolling buffer and re-order it too!
    fftdata[i].r = accData[i];
    fftdata[i].i = 0;
  }
  // Do the FFT conversion from time to frequency domain.
  fft_fft(fftdata,FFT_BITS);
  fftOK = 0;
  // Look for maximm amplitude, and location of maximum.
  maxVal = fftdata[0].r;
  maxLoc = 0;
  for (i=0;i<NSAMP/2;i++) {
    //APP_LOG(APP_LOG_LEVEL_DEBUG,"i=%d, accData=%ld fftr=%ld",i,fftdata[i].r,fftdata[i].i);
    if (fftdata[i].r>maxVal) {
      maxVal = fftdata[i].r;
      maxLoc = i;
    }
  }
  /* Start collecting new buffer of data */
  /* FIXME = it would be best to make this a rolling buffer and analyse it
  * more frequently.
  */
  accDataPos = 0;
  accDataFull = 0;
}


/* clock_tick_handler() - Analyse data and update display.
 * Updates the text layer clock_layer to show current time.
 * This function is the handler for tick events.*/
static void clock_tick_handler(struct tm *tick_time, TimeUnits units_changed) {
  static char s_time_buffer[16];
  static char s_buffer[120];

  // Do FFT analysis
  if (accDataFull) 
    do_analysis();

  // Update display.
  BatteryChargeState charge_state = battery_state_service_peek();
  snprintf(s_buffer,sizeof(s_buffer),
	   "%d,%d,%d\n%d\nmax=%d,pos=%d",
	   latestAccelData.x, latestAccelData.y, latestAccelData.z,
	   accData[accDataPos-1],
	   maxVal,maxLoc
	   );
  text_layer_set_text(text_layer, s_buffer);

  // and update clock display.
  if (clock_is_24h_style()) {
    strftime(s_time_buffer, sizeof(s_time_buffer), "%H:%M:%S", tick_time);
  } else {
    strftime(s_time_buffer, sizeof(s_time_buffer), "%I:%M:%S", tick_time);
  }
  snprintf(s_time_buffer,sizeof(s_time_buffer),
	   "%s %d%%", 
	   s_time_buffer,
	   charge_state.charge_percent);
  text_layer_set_text(clock_layer, s_time_buffer);
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
					     bounds.size.h-CLOCK_SIZE-SPEC_SIZE } 
				 });
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  text_layer_set_font(text_layer, 
		      fonts_get_system_font(FONT_KEY_GOTHIC_24));
  layer_add_child(window_layer, text_layer_get_layer(text_layer));

  /* Create text layer for spectrum display */
  spec_layer = text_layer_create(
				 (GRect) { 
				   .origin = { 0, bounds.size.h - CLOCK_SIZE - SPEC_SIZE }, 
				   .size = { bounds.size.w, SPEC_SIZE } 
				 });
  text_layer_set_text(spec_layer, "SPECTRUM");
  layer_add_child(window_layer, text_layer_get_layer(spec_layer));

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
  text_layer_destroy(clock_layer);
  text_layer_destroy(spec_layer);
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
