/**
 *     This file is part of Seizure_Detector.
 *
 *   Seizure_Detector is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Foobar is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Graham Jones, 2013.
 *
 * Seizure Detector
 * Samples analogue input and converts to frequency spectrum using
 * FFT.   It monitors one frequency channel against a threshold.  If the threshold is
 * exceeded for a given time, a warning beep is sounded.  If the threshold is exceeded
 * for a longer specified period, an alarm sound is recorded.
 * The frequency spectra are logged to the SD card as a CSV file for future analysis.
 *
 * The circuit:
 *  SD card attached to SPI bus as follows:
 *  CS - pin 10
 *  MOSI - pin 11 on Arduino Uno/Duemilanove/Diecimila
 *  MISO - pin 12 on Arduino Uno/Duemilanove/Diecimila
 *  CLK - pin 13 on Arduino Uno/Duemilanove/Diecimila
 *
 * DS1307 Real Time Clock module connected as follows:
 *   Analogue A4 - STA
 *   Analogue A5 - SCL
 *   2k2 pull up resistors between A4 and +5V and A5 and +5V
 *
 * Analogue accelerometer connected to A0, A1 and A2
 *
 * Piezo buzzer connected between pin D4 and ground.
 *
 * ADXL345 code adapted from example by Jens C Brynildsen
 *   (https://github.com/jenschr/Arduino-libraries/blob/master/ADXL345/examples/ADXL345_no_library/BareBones_ADXL345.pde)
 * ADXL345 has STA (blue wire) on A4
 *         and SCL (yellow wire) on A5
 *
 */
// Feature Selection
#define ANALOGUE_ACCEL 1   // Use Analogue Accelerometer
#define EXTERNAL_VREF 1    // use external VREF to improve
                            // sensitivity of analogue acccel.

#define USE_X 0       // Which axes to include in accel calculation.
#define USE_Y 0
#define USE_Z 1


#include <stdint.h>
#include <math.h>     // for sqrt function.
#include <ffft.h>
#include <Time.h>
#include <Wire.h>
#ifndef ANALOGUE_ACCEL
#include <ADXL345.h>   // Accelerometer
#endif
#include <DS1307RTC.h> // Real Time Clock
#include <Fat16.h>     // SD Card
#include <Fat16util.h> // AD Card memory utilities.
#include <memUtils.h>


// General Settings
static int pinXNo = 0;   // ADC Channel to capture
static int pinYNo = 1;   // ADC Channel to capture
static int pinZNo = 2;   // ADC Channel to capture
static int SD_CS = 10;  // pin number of the chip select for the SD card.
static int AUDIO_ALARM_PIN = 4;  // digital pin for the audio alarm (connect buzzer from this pin to ground.
static int freq = 64;  // sample frequency (Hz) (128 samples = 1 second collection)
#define LOGFN "SEIZURE.CSV"

static int mon_freq_ch_min = 5;   // min channel to monitor - monitor channel 7-10.
static int mon_freq_ch_max = 8;  // max channel to monitor
static int mon_thresh  = 10; // Alarm threshold
static unsigned long mon_warn_millis = 2000; // alarm level must continue for this time to raise warning.
static unsigned long mon_alarm_millis = 5000; // alarm level must continue for this time to raise full alarm.
static unsigned long mon_reset_millis = 2000;
static unsigned long log_millis = 1000;       // SD card logging period.

// Variables for alarms
boolean mon_thresh_exceeded = false;
boolean mon_warn_state = false;
boolean mon_alarm_state = false;
boolean mon_reset_state = true;
unsigned long mon_lastreset_millis = 0;   // time the value fell below the alarm threshold.
unsigned long mon_thresh_millis = 0;  // time that the alarm threshold was exceeded.
unsigned long last_log_millis = 0;    // time we last wrote data to the SD card

// Flag to tell us it is time to collect some data
boolean need_data = false;

// Buffers for the Fast Fourier Transform code.
volatile byte position = 0;
int16_t capture[FFT_N];       // Capture Buffer
complex_t bfly_buff[FFT_N];
uint16_t spectrum[FFT_N/2];   // Output buffer

// Buffers for the SD card logging.
SdCard sdCard;
Fat16 logfile;

#ifndef ANALOGUE_ACCEL
// Buffer for the accelerometer
ADXL345 accel;
#endif

// store error strings in flash to save RAM - uses Fat16Util
#define error(s) error_P(PSTR(s))

void error_P(const char* str) {
  PgmPrint("Error: ");
  SerialPrintln_P(str);
  if (sdCard.errorCode) {
    PgmPrint("SD error: ");
    Serial.println(sdCard.errorCode, HEX);
  }
  // Don't stop just because we can't get to the SD card.
  //while(1);
}

void setup()
{
  int i,x,y,z;
  double gains[3];
  Serial.begin(9600);
  //establishContact();
  Serial.println(memoryFree());

  setSyncProvider(RTC.get);   // the function to get the time from the RTC
  if(timeStatus()!= timeSet) 
     PgmPrintln("Unable to sync with the RTC");
  else
     PgmPrintln("RTC has set the system time");      

  #ifdef ANALOGUE_ACCEL
  // intialise analogue accelerometer
  if (EXTERNAL_VREF)
    analogReference(EXTERNAL);
  else
    analogReference(DEFAULT);
  #else
  // Initialise digital accelerometer
  PgmPrintln("Powering on I2C Accelerometer");
  accel.powerOn();
  accel.setRangeSetting(2);
  accel.getAxisGains(gains);
  Serial.println("gains_orig[]:");
  for(i = 0; i < 3; i++){
    Serial.print(gains[i], 6);
    Serial.print(" ");
  }
  Serial.println("");
  gains[0] = 1; gains[1] = 1; gains[2] = 1;
  accel.setAxisGains(gains);
  accel.getAxisGains(gains);
  Serial.println("gains[]:");
  for(i = 0; i < 3; i++){
    Serial.print(gains[i], 6);
    Serial.print(" ");
  }
  Serial.println("");
  accel.readAccel(&x, &y, &z);
  Serial.print("XYZ COUNTS: ");
  Serial.print(x, DEC);
  Serial.print(" ");
  Serial.print(y, DEC);
  Serial.print(" ");
  Serial.print(z, DEC);
  Serial.println("");
  // might want to adjust gains here with accel.setAxisGains
  #endif

   // Initialise audio output.
   pinMode(AUDIO_ALARM_PIN, OUTPUT);
   tone(AUDIO_ALARM_PIN,1000,100);
 
 
  // Initialise SD Card
  pinMode(SD_CS,OUTPUT);
  if (!sdCard.init()) error("card.init");  
  // initialize a FAT16 volume
  if (!Fat16::init(&sdCard)) {
     error("Fat16::init");
    // make a warning tone
   tone(AUDIO_ALARM_PIN,200,3000);
  } else {
    PgmPrintln("card initialized.");
    tone(AUDIO_ALARM_PIN,2000,100);
  }
  PgmPrint("FFT_N=");
  Serial.println(FFT_N);
  Serial.println(memoryFree());

 
 // Initialise flags
 mon_reset_state = true;
 mon_lastreset_millis = now();

  // initialize timer1 to set sample frequency. 
  noInterrupts();           // disable all interrupts
  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1  = 0;
  OCR1A = 62500/freq;            // compare match register 16MHz/256/freq
  TCCR1B |= (1 << WGM12);   // CTC mode
  TCCR1B |= (1 << CS12);    // 256 prescaler 
  TIMSK1 |= (1 << OCIE1A);  // enable timer compare interrupt
  interrupts();             // enable all interrupts
}

////////////////////////////////////////////////////////
// Interrupt service routine - called regularly to collect
// data and store in FFT capture buffer.
// All this does is set a flag to tell us to collect some data
// the actual work is done in loop() because the arduino
// crashes if I use I2C inside this routine.
//
ISR(TIMER1_COMPA_vect)          // timer compare interrupt service routine
{
  need_data = true;
}

void digitalClockDisplay(){
  // digital clock display of the time
  printDigits(day());
  PgmPrint("/");
  printDigits(month());
  PgmPrint("/");
  Serial.print(year()); 
  PgmPrint(" ");
  printDigits(hour());
  PgmPrint(":");
  printDigits(minute());
  PgmPrint(":");
  printDigits(second());
  //Serial.println(); 
}

void digitalClockLog(){
  // digital clock display of the time to log file.
  printDigitsLog(day());
  logfile.print("/");
  printDigitsLog(month());
  logfile.print("/");
  logfile.print(year()); 
  logfile.print(" ");
  printDigitsLog(hour());
  logfile.print(":");
  printDigitsLog(minute());
  logfile.print(":");
  printDigitsLog(second());
  //Serial.println(); 
}

void printDigitsLog(int digits) {
 // utility function for digital clock display: prints preceding colon and leading 0
  if(digits < 10)
    logfile.print("0");
  logfile.print(digits);
}

void printDigits(int digits){
  // utility function for digital clock display: prints preceding colon and leading 0
  if(digits < 10)
    PgmPrint("0");
  Serial.print(digits);
}

//////////////////////////////////////////////////////////
// Main Loop
//
void loop()
{
  int i, maxChan;
  int x,y,z;

  if (need_data) {
    // Do nothing if we are at the end of the capture buffer.
    if (position >= FFT_N)
      return;
    #ifdef ANALOGUE_ACCEL
    // Read acceleration from analogue input pins
    x = analogRead(pinXNo);
    y = analogRead(pinYNo);
    z = analogRead(pinZNo);
    #else
    // Read acceleration from I2C bus device.
    accel.readAccel(&x,&y,&z);
    #endif
    capture[position] = (USE_X*x+USE_Y*y+USE_Z*z)/(USE_X+USE_Y+USE_Z);
   //capture[position] = (int)sqrt(x*x+y*y+z*z);
    //capture[position] = x+y+z;
    position++;
    need_data = false;
  }


  // See if the FFT capture buffer is full.
  // If so, process data.
  if (position == FFT_N) {
    // Do FFT Calculation
    fft_input(capture,bfly_buff);
    fft_execute(bfly_buff);
    fft_output(bfly_buff,spectrum);
    // Reset buffer position ready for next data.
    position = 0;
     
    // Write results to serial port.
    digitalClockDisplay();
    PgmPrint(",");
    for (byte i=0; i<FFT_N/2; i++) {
      //Serial.write(spectrum[i]);
      Serial.print(spectrum[i]);
      PgmPrint(",");
    }
    Serial.println();
    Serial.println(memoryFree());
    
    // get maximum value in monitored range of frequencies
    maxChan = spectrum[mon_freq_ch_min];
    for (i=mon_freq_ch_min;i<=mon_freq_ch_max;i++) {
       if (spectrum[i]>maxChan) 
         maxChan = spectrum[i];
    }
    // Decide if we need to raise an alarm (value over threshold for given period)
    if ((maxChan>=mon_thresh)) {
       if (!mon_thresh_exceeded) {
         PgmPrintln("new event");
          mon_thresh_exceeded = true;
          mon_thresh_millis = millis();
       } 
       if ((millis() - mon_thresh_millis) >= mon_alarm_millis) {
         mon_alarm_state = true;
         mon_warn_state = false;
         PgmPrintln("****ALARM****");
       } else if ((millis() - mon_thresh_millis) >= mon_warn_millis) {
         mon_warn_state = true;
         PgmPrintln("**warning**");
       } else {
         PgmPrintln("pre-alarm");
       }
    } else {
      if (mon_thresh_exceeded) {
        PgmPrintln("event reset");
        mon_lastreset_millis = now();
        mon_thresh_exceeded = false;
        //mon_warn_state = false;
        //mon_alarm_state = false; 
        //noTone(AUDIO_ALARM_PIN);
      }
      else if (millis()-mon_lastreset_millis > mon_reset_millis) {
         mon_warn_state = false;
         mon_alarm_state = false;
         noTone(AUDIO_ALARM_PIN);
       }
    }
    
    // Write to SD card if we are in a warning or alarm state.
    if (mon_thresh_exceeded || (millis()>(last_log_millis +log_millis))) {
      // Write resutls to SD Card
      PgmPrintln("Writing data to log file...");
      
      logfile.open(LOGFN,O_CREAT | O_APPEND | O_WRITE);
      if (!logfile.isOpen()) error ("create");
      digitalClockLog();
      logfile.print(", ");
      logfile.print(now());
      if (mon_warn_state)       logfile.print(", warning,");
      else if (mon_alarm_state) logfile.print(", *ALARM*,");
      else if (mon_thresh_exceeded) logfile.print(", poss  ,");
      else                      logfile.print(",        ,");
      
      for (byte i=0;i<FFT_N/2;i++) {
         logfile.print( spectrum[i] );
         logfile.print( ", ");
      }
      logfile.print("RAW:,");
      for (byte i=0;i<FFT_N;i++) {
         logfile.print( capture[i] );
         logfile.print( ", ");
      }
      logfile.println();
      logfile.close();
      last_log_millis = millis();
    }
    // make a short pip for a warning
    if (mon_warn_state) tone(AUDIO_ALARM_PIN,200,100);
    // continuous noise for alarm
    if (mon_alarm_state) tone(AUDIO_ALARM_PIN,100);
  }
}

void establishContact() {
 while (Serial.available() <= 0) {
      Serial.write('A');   // send a capital A
      delay(300);
  }
}

