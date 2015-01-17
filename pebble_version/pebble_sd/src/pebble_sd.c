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

static Window *window;
static TextLayer *text_layer;
uint32_t num_samples = 25;

static void accel_handler(AccelData *data, uint32_t num_samples) {
  static char s_buffer[120];
  snprintf(s_buffer,sizeof(s_buffer),
	   "%d,%d,%d\n%d\n",
	   data[0].x, data[0].y, data[0].z,
	   (int)num_samples);
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
				   .size = { bounds.size.w, bounds.size.h } 
				 });
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  text_layer_set_font(text_layer, 
		      fonts_get_system_font(FONT_KEY_GOTHIC_24));
  layer_add_child(window_layer, text_layer_get_layer(text_layer));
}

/**
 * window_unload():  destroy window contents
 */
static void window_unload(Window *window) {
  text_layer_destroy(text_layer);
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
  accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);}

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
