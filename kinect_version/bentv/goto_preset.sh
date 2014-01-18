#!/bin/sh
# Reposition camera to the preset number given on the command line (1-10)

CAMERA_IP=192.168.1.24
echo "moving to preset "$1
wget --quiet --user operator --password operator -O /tmp/out.txt "http://$CAMERA_IP/preset.cgi?-act=goto&-status=1&-number=$1"
sleep 1

echo "move complete"

