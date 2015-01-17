/*
  Pebble_sd - a simple accelerometer based seizure detector/alarm that runs
  on a Pebble smart watch (http://getpebble.com).
  Developed from the boilerplate code produced by the pebble new-project tool.

  See http://openseizuredetector.org for more information.

  Copyright Graham Jones, 2015

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
    along with Pebble_sd.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <pebble.h>

/* Global Variables */
static Window *window;   /* The application window */
static TextLayer *text_layer; /* Text area for writing to the screen */

uint32_t num_samples = 25;  /* Number of accelerometer samples to collect
			      * before processing */


static void accel_handler(AccelData *data, uint32_t num_samples) {
  /**
   * accel_handler():  Respond to accelerometer data being sent to the app.
   * based on example at 
   * http://developer.getpebble.com/guides/pebble-apps/sensors/accelerometer
   */
  static char s_buffer[128];
  snprintf(s_buffer,sizeof(s_buffer),
	   "%d\n%4d,%4d,%4d",
	   (int)num_samples,
	   data[0].x, data[0].y, data[0].z);
  text_layer_set_text(text_layer,s_buffer);

  

}



/************************************************************************
 * Button Click Event Handlers - we will not need these in the final
 * version because it will not be interactive, but leave the code here for
 * now...
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


/***********************************************************************/

static void window_load(Window *window) {
  /**
   * window_load():  initialise the window when it is displayed - in particular
   * the text-layer for writing to the screen.
   */
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  text_layer = text_layer_create((GRect) { 
      .origin = { 0, 20 }, 
	.size = { bounds.size.w, bounds.size.h } });
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  text_layer_set_font(text_layer,
		      fonts_get_system_font(FONT_KEY_GOTHIC_28)
		      );
  layer_add_child(window_layer, text_layer_get_layer(text_layer));
}

static void window_unload(Window *window) {
  /**
   * window_unload:  destroy the text layer when the window is closed
   */
  text_layer_destroy(text_layer);
}

static void init(void) {
  /** 
   * init(): Initialise the application window, register for click 
   * event notification.
   */
  window = window_create();
  window_set_click_config_provider(window, click_config_provider);
  window_set_window_handlers(window, (WindowHandlers) {
    .load = window_load,
    .unload = window_unload,
  });
  const bool animated = true;
  window_stack_push(window, animated);
  accel_data_service_subscribe(num_samples,accel_handler);
  accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);
}

static void deinit(void) {
  /* 
   * deinit(): Destroy the window on application exit
   */
  window_destroy(window);
}

int main(void) {
  /**
   * main(): Main application entry point.
   * Calls the init() function to initialise the window and handlers,
   * then enters the main event loop.
   * Calls de-init when the event loop exits to destroy the window before
   * the application exits.
   */
  init();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);
  app_event_loop();
  deinit();
}
