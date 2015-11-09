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
#include "pebble_sd.h"
void sendSettings();
void sendFftSpec();


/*************************************************************
 * Communications with Phone
 *************************************************************/
void inbox_received_callback(DictionaryIterator *iterator, void *context) {
  APP_LOG(APP_LOG_LEVEL_INFO, "Message received!");
  // Get the first pair
  Tuple *t = dict_read_first(iterator);

  // Process all pairs present
  while(t != NULL) {
    // Process this pair's key
    APP_LOG(APP_LOG_LEVEL_INFO,"Key=%d",(int) t->key);
    switch (t->key) {
    case KEY_SETTINGS:
      APP_LOG(APP_LOG_LEVEL_INFO, "***********Phone Requesting Settings");
      sendSettings();
      break;
    case KEY_SET_SETTINGS:
      APP_LOG(APP_LOG_LEVEL_INFO, "***********Phone Setting Settings");
      // We don't actually do anything here - the following sections
      // process the data and update the settings.
      break;
    case KEY_ALARM_FREQ_MIN:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting ALARM_FREQ_MIN to %d",
	      alarmFreqMin = (int)t->value->int16);
      break;
    case KEY_ALARM_FREQ_MAX:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting ALARM_FREQ_MAX to %d",
	      alarmFreqMax = (int)t->value->int16);
      break;
    case KEY_WARN_TIME:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting WARN_TIME to %d",
	      warnTime = (int)t->value->int16);
      break;
    case KEY_ALARM_TIME:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting ALARM_TIME to %d",
	      alarmTime = (int)t->value->int16);
      break;
    case KEY_ALARM_THRESH:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting ALARM_THRESH to %d",
	      alarmThresh = (int)t->value->int16);
      break;
    case KEY_ALARM_RATIO_THRESH:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting ALARM_RATIO_THRESH to %d",
	      alarmRatioThresh = (int)t->value->int16);
      break;
    case KEY_FALL_THRESH_MIN:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting FALL_THRESH_MIN to %d",
	      fallThreshMin = (int)t->value->int16);
      break;
    case KEY_FALL_THRESH_MAX:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting FALL_THRESH_MAX to %d",
	      fallThreshMax = (int)t->value->int16);
      break;
    case KEY_FALL_WINDOW:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting FALL_WINDOW to %d",
	      fallWindow = (int)t->value->int16);
      break;
    }
    // Get next pair, if any
    t = dict_read_next(iterator);
  }}

void inbox_dropped_callback(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}

void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed!");
}

void outbox_sent_callback(DictionaryIterator *iterator, void *context) {
  APP_LOG(APP_LOG_LEVEL_INFO, "Outbox send success!");
}

/***************************************************
 * Send some Seizure Detecto Data to the phone app.
 */
void sendSdData() {
  DictionaryIterator *iter;
  APP_LOG(APP_LOG_LEVEL_DEBUG,"sendSdData()");
  app_message_outbox_begin(&iter);
  dict_write_uint8(iter,KEY_DATA_TYPE,(uint8_t)DATA_TYPE_RESULTS);
  dict_write_uint8(iter,KEY_ALARMSTATE,(uint8_t)alarmState);
  dict_write_uint32(iter,KEY_MAXVAL,(uint32_t)maxVal);
  dict_write_uint32(iter,KEY_MAXFREQ,(uint32_t)maxFreq);
  dict_write_uint32(iter,KEY_SPECPOWER,(uint32_t)specPower);
  dict_write_uint32(iter,KEY_ROIPOWER,(uint32_t)roiPower);
  // Send simplified spectrum - just 10 integers so it fits in a message.
  dict_write_data(iter,KEY_SPEC_DATA,(uint8_t*)(&simpleSpec[0]),
		  10*sizeof(simpleSpec[0]));
  app_message_outbox_send();
  APP_LOG(APP_LOG_LEVEL_DEBUG,"sent Results");
}



/***************************************************
 * Send Seizure Detector Settings to the Phone
 */
void sendSettings() {
  DictionaryIterator *iter;
  app_message_outbox_begin(&iter);
  // Tell the phone this is settings data
  dict_write_uint8(iter,KEY_DATA_TYPE,(uint8_t)DATA_TYPE_SETTINGS);
  dict_write_uint8(iter,KEY_SETTINGS,(uint8_t)1);
  // then the actual settings
  dict_write_uint32(iter,KEY_ALARM_FREQ_MIN,(uint32_t)alarmFreqMin);
  dict_write_uint32(iter,KEY_ALARM_FREQ_MAX,(uint32_t)alarmFreqMax);
  dict_write_uint32(iter,KEY_NMIN,(uint32_t)nMin);
  dict_write_uint32(iter,KEY_NMAX,(uint32_t)nMax);
  dict_write_uint32(iter,KEY_WARN_TIME,(uint32_t)warnTime);
  dict_write_uint32(iter,KEY_ALARM_TIME,(uint32_t)alarmTime);
  dict_write_uint32(iter,KEY_ALARM_THRESH,(uint32_t)alarmThresh);
  dict_write_uint32(iter,KEY_ALARM_RATIO_THRESH,(uint32_t)alarmRatioThresh);
  BatteryChargeState charge_state = battery_state_service_peek();
  dict_write_uint8(iter,KEY_BATTERY_PC,(uint8_t)charge_state.charge_percent);
  dict_write_uint32(iter,KEY_FALL_THRESH_MIN,(uint32_t)fallThreshMin);
  dict_write_uint32(iter,KEY_FALL_THRESH_MAX,(uint32_t)fallThreshMax);
  dict_write_uint32(iter,KEY_FALL_WINDOW,(uint32_t)fallWindow);

  app_message_outbox_send();

}


void comms_init() {
  // Register comms callbacks
  app_message_register_inbox_received(inbox_received_callback);
  app_message_register_inbox_dropped(inbox_dropped_callback);
  app_message_register_outbox_failed(outbox_failed_callback);
  app_message_register_outbox_sent(outbox_sent_callback);
  // Open AppMessage
  app_message_open(app_message_inbox_size_maximum(), 
		   app_message_outbox_size_maximum());
}
