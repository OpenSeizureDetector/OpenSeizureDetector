#!/usr/bin/python
#
#############################################################################
#
# Copyright Graham Jones, December 2013 
#
#  Original version by Joseph Howse in his book, "OpenCV Computer Vision with 
#      Python" (Packt Publishing, 2013).
#      http://nummist.com/opencv/
#      http://www.packtpub.com/opencv-computer-vision-with-python/book
#
#############################################################################
#
#   This file is part of OpenSeizureDetector.
#
#    OpenSeizureDetector is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Foobar is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with OpenSeizureDetector.  If not, see <http://www.gnu.org/licenses/>.
##############################################################################
#
from managers import CaptureManager
import depth

device = depth.CV_CAP_FREENECT
#device = 1
print "device=%d" % device
captureManager = CaptureManager(
    device, None, True)
captureManager.channel = depth.CV_CAP_OPENNI_BGR_IMAGE

captureManager.enterFrame()

#frame = captureManager.frame

captureManager.channel = depth.CV_CAP_OPENNI_DEPTH_MAP
captureManager.writeImage("kintest1.png")
captureManager.exitFrame()
captureManager.enterFrame()
captureManager.channel = depth.CV_CAP_OPENNI_BGR_IMAGE
captureManager.writeImage("kintest2.png")
captureManager.exitFrame()

