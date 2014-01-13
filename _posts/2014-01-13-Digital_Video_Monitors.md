---
layout: post
title: Digital Video Monitors
category: Meta

excerpt: A Ditigal replacement for my analogue video monitoring system.

---


<a href="{{site.baseurl}}/resources/img/ip_camera.jpg"><img class="img_med" style="float:right;width:300px;" src="{{site.baseurl}}/resources/img/ip_camera.jpg"></a>

I converted my analogue monitoring system to digital by installing a wifi ip 
camera in Benjamin's bedroom and connecting it to my home network.

I connected a <a href="http://raspberrypi.org">Raspberry Pi</a> single board 
computer to each monitor, and installed a small wifi usb adapter into the
raspberry pi's to connect them to the network.

Setting the raspberry pi omxplayer video player to start on boot-up allows
me to display the image and play sound from the ip camera.

<a href="{{site.baseurl}}/resources/img/bentv_screenshot_ok.jpg"><img class="img_med" style="float:right;width:300px;" src="{{site.baseurl}}/resources/img/bentv_screenshot_ok.jpg"></a>

I discovered that if I scale the omxplayer screen a bit, I can free up a 
bit of screen to use for somethign else.   In this case I wrote a simple
python script that uses the pygame library to write to the spare bit of
screen.   This displays a clock so I can tell it is still working, and talks 
to the seizure detector program that is running on a more powerful computer
via a web interface.


I also connected a small microswitch to the Raspberry Pi's which is also 
monitored by the program.  Each time the switch is pressed, the ip camera is
instructed to move to a different preset position, allowing us to look around
the room.

The source code that runs on the Raspberry Pi's is available in a separate
'bentv' <a href="https://github.com/jones139/bentv">github repository</a>.

<a href="{{site.baseurl}}/resources/img/Network_Configuration.png"><img class="img_med" style="float:right;width:300px;" src="{{site.baseurl}}/resources/img/Network_Configuration.png"></a>
In the above picture you can see the cone of light on the right hand side of 
the 
image, which is the Infrared laser in the Kinect, which is scaning that part of
the room to look for Benjamin.

Our house is quite large, with good solid walls, so our wifi network 
configuration is a bit complicated - need two access points.  The following
picture shows the connection of the Raspberry Pi BenTV's to the network,
along with the IP camera in Benjamin's room.
