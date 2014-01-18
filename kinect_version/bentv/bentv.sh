#!/bin/sh
while :
do
	/usr/bin/omxplayer --live --win "34 16 684 400" \
				rtsp://guest:guest@192.168.1.24/12
done
