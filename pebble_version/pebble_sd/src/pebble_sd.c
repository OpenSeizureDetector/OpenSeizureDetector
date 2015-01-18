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

#define NSAMP 25       // number of samples of accelerometer data to collect.
#define CLOCK_SIZE 30  // pixels.
static Window *window;
static TextLayer *text_layer;
static TextLayer *clock_layer;
uint32_t num_samples = NSAMP;
int accData[NSAMP];


/* Updates the text layer clock_layer to show current time.
 * This function is the handler for tick events.*/
static void clock_display_handler(struct tm *tick_time, TimeUnits units_changed) {
  static char s_time_buffer[16];
  if (clock_is_24h_style()) {
    strftime(s_time_buffer, sizeof(s_time_buffer), "%H:%M:%S", tick_time);
  } else {
    strftime(s_time_buffer, sizeof(s_time_buffer), "%I:%M:%S", tick_time);
  }
  text_layer_set_text(clock_layer, s_time_buffer);
}

static void accel_handler(AccelData *data, uint32_t num_samples) {
  static char s_buffer[120];
  int i;

  for (i=0;i<NSAMP;i++) {
    accData[i] = data[0].x + data[0].y + data[0].z;
  }

  BatteryChargeState charge_state = battery_state_service_peek();
  snprintf(s_buffer,sizeof(s_buffer),
	   "%d,%d,%d\n%d\n%d\n%d%%\n",
	   data[0].x, data[0].y, data[0].z,
	   (int)num_samples,
	   accData[0],
	   charge_state.charge_percent);
  text_layer_set_text(text_layer, s_buffer);

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
				   .size = { bounds.size.w, bounds.size.h-CLOCK_SIZE } 
				 });
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  text_layer_set_font(text_layer, 
		      fonts_get_system_font(FONT_KEY_GOTHIC_24));
  layer_add_child(window_layer, text_layer_get_layer(text_layer));

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
  accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);

  /* Subscribe to TickTimerService */
  tick_timer_service_subscribe(SECOND_UNIT, clock_display_handler);
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
