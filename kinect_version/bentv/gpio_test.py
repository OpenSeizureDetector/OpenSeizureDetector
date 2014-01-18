#!/usr/bin/python
# Import the required module. 
import RPi.GPIO as GPIO 
import time
import os
import httplib2

pinNo = 14 # GPIO 14 = physical pin no 8.

##########################################
# Callback function when button is pressed
def moveCamera(pinNo):
    print('moveCamera called by pin number %d. PresetNo=%d' % (pinNo,moveCamera.presetNo))
    h = httplib2.Http(".cache")
    h.add_credentials('operator', 'operator')
    resp, content = h.request("http://192.168.1.24/preset.cgi?-act=goto&-status=1&-number=%d" % moveCamera.presetNo,
        "GET")
    print "moved to preset %d - content=%s" % (moveCamera.presetNo,content)
    moveCamera.presetNo += 1
    if (moveCamera.presetNo > 4): moveCamera.presetNo = 1
moveCamera.presetNo = 1

##########################################
# Initialise the GPIO Pins
# Set the mode of numbering the pins. 
GPIO.setmode(GPIO.BCM) 
GPIO.setup(pinNo, GPIO.IN, pull_up_down=GPIO.PUD_UP) 
# very long debounce time to prevent presses while camera is moving.
GPIO.add_event_detect(pinNo,GPIO.RISING, callback=moveCamera, bouncetime=1000)
#moveCamera(0)

#############################################
# Main loop - does nothing useful!!!
while 1: 
    #print "main loop..."
    time.sleep(1)

