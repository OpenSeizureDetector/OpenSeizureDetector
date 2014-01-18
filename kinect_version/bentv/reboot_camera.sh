#!/bin/sh
# Reboot camera

CAMERA_IP=192.168.1.24
read -p "Enter Camera Admin Password:" password
echo $password
echo "rebooting..."
wget  --user admin --password $password -O /tmp/out.txt "http://$CAMERA_IP/sysreboot.cgi"
sleep 1

echo "done!"

