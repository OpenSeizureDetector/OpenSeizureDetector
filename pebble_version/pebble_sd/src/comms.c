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
    case KEY_ALARM_FREQ_MIN:
      APP_LOG(APP_LOG_LEVEL_INFO,"Phone Setting ALARM_FREQ_MIN to %d",
	      alarmFreqMin = (int)t->value);
      
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
  app_message_outbox_begin(&iter);
  dict_write_uint8(iter,KEY_DATA_TYPE,(uint8_t)DATA_TYPE_RESULTS);
  dict_write_uint8(iter,KEY_ALARMSTATE,(uint8_t)alarmState);
  dict_write_uint32(iter,KEY_MAXVAL,(uint32_t)maxVal);
  dict_write_uint32(iter,KEY_MAXFREQ,(uint32_t)maxFreq);
  dict_write_uint32(iter,KEY_SPECPOWER,(uint32_t)specPower);
  dict_write_uint32(iter,KEY_ROIPOWER,(uint32_t)roiPower);
  app_message_outbox_send();
  //sendFftSpec();
}

/***************************************************
 * Send Spectrum data to phone
 */
void sendFftSpec() {
  int i;
  DictionaryIterator *iter;
  int outboxSize = app_message_outbox_size_maximum();
  int specLen = sizeof(fftResults)*sizeof(fftResults[0]);
  int nPackets = specLen/outboxSize + 1;
  int nVals = (specLen/nPackets - 2)/sizeof(fftResults[0]);    // number of values in each packet.
  int posMax = 0;
  int posMin = 0;
  APP_LOG(APP_LOG_LEVEL_INFO,"outboxSize = %d, specLen=%d, nPackets = %d, nVals=%d",
	  outboxSize,specLen,nPackets,nVals);

  for (i = 0;i<nPackets;i++) {
    posMin = i*nVals;
    if (((NSAMP/2) - posMin)>nVals)
      posMax = posMin + nVals;
    else
      posMax = NSAMP/2;
    APP_LOG(APP_LOG_LEVEL_INFO,"posMin = %d, posMax = %d",posMin,posMax);
    app_message_outbox_begin(&iter);
    dict_write_uint8(iter,KEY_DATA_TYPE,(uint8_t)DATA_TYPE_SPEC);
    dict_write_uint32(iter,KEY_POS_MIN,(uint32_t)(posMin));
    dict_write_uint32(iter,KEY_POS_MAX,(uint32_t)alarmState);
    dict_write_data(iter,KEY_SPEC_DATA,(uint8_t*)(&fftResults[posMin]),nVals*sizeof(fftResults[0]));
  app_message_outbox_send();
  }

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
