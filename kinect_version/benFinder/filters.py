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
import cv2
import numpy
import utils

def getBenMask(src,threshold):
    """Returns a mask image and area that is the largest bright area in the 
    source image src.   Threshold defines what we mean by bright (0-255)
    The idea is that we do background subtraction from an image, and Benjamin
    will be the largest area in the resulting image.
    This is based on http://stackoverflow.com/questions/10262600/
                    how-to-detect-region-of-large-of-white-pixels-using-opencv
    """
    # Convert to 8 bit image so threshold function will work
    if (src.dtype == "uint16"):
        print "Converting to 8 bit"
        src8 = cv2.convertScaleAbs(src)
        print src8.dtype
    else:
        src8 = src
        
    # Apply threshold so we only have the bright parts of the image in src_th.
    ret, src_th = cv2.threshold(src8,threshold,255,0)
    
    # create a black mask the same size.
    mask = numpy.zeros(src_th.shape,numpy.uint8)

    # Find contours in thresholded image.
    contours, hier = cv2.findContours(src_th,
                                      cv2.RETR_LIST, 
                                      cv2.CHAIN_APPROX_SIMPLE)

    # Look for the contour surrounding the largest area.
    maxArea = 0
    maxContour = None
    for cnt in contours:
        if (cv2.contourArea(cnt)>maxArea):
            maxArea = cv2.contourArea(cnt)
            maxContour = cnt

    # Check we found one!
    if (maxContour == None):
        print "getBenMask() - Something went wrong - no contours found!"
        return None
    else:
        # Draw the filled in contour onto the mask
        # Note we are drawing in colour '1' because this works nicely
        # mathematically, but doesn't display on screen well at all - too
        # close to black!!
        cv2.drawContours(mask,[maxContour],-1,1,-1)
        # And draw it back onto the soruce image too as an outline.
        cv2.drawContours(src,[maxContour],-1,128,5)
        #cv2.imshow("src",src)
        #cv2.imshow("mask",mask)

    return mask,maxArea


def getMean(src,mask):
    mean = cv2.mean(src,mask = mask)
    return mean


def bgr2rgb(src, dst):
    """convert bgr image to rgb (and vice versa)
    
    Pseudocode:
    dst.b = src.r
    dst.g = src.g
    dst.r = src.b
    
    """
    b, g, r = cv2.split(src)
    cv2.merge((r,g,b), dst)

def recolorRC(src, dst):
    """Simulate conversion from BGR to RC (red, cyan).
    
    The source and destination images must both be in BGR format.
    
    Blues and greens are replaced with cyans. The effect is similar
    to Technicolor Process 2 (used in early color movies) and CGA
    Palette 3 (used in early color PCs).
    
    Pseudocode:
    dst.b = dst.g = 0.5 * (src.b + src.g)
    dst.r = src.r
    
    """
    b, g, r = cv2.split(src)
    cv2.addWeighted(b, 0.5, g, 0.5, 0, b)
    cv2.merge((b, b, r), dst)


def recolorRGV(src, dst):
    """Simulate conversion from BGR to RGV (red, green, value).
    
    The source and destination images must both be in BGR format.
    
    Blues are desaturated. The effect is similar to Technicolor
    Process 1 (used in early color movies).
    
    Pseudocode:
    dst.b = min(src.b, src.g, src.r)
    dst.g = src.g
    dst.r = src.r
    
    """
    b, g, r = cv2.split(src)
    cv2.min(b, g, b)
    cv2.min(b, r, b)
    cv2.merge((b, g, r), dst)


def recolorCMV(src, dst):
    """Simulate conversion from BGR to CMV (cyan, magenta, value).
    
    The source and destination images must both be in BGR format.
    
    Yellows are desaturated. The effect is similar to CGA Palette 1
    (used in early color PCs).
    
    Pseudocode:
    dst.b = max(src.b, src.g, src.r)
    dst.g = src.g
    dst.r = src.r
    
    """
    b, g, r = cv2.split(src)
    cv2.max(b, g, b)
    cv2.max(b, r, b)
    cv2.merge((b, g, r), dst)


def blend(foregroundSrc, backgroundSrc, dst, alphaMask):
    
    # Calculate the normalized alpha mask.
    maxAlpha = numpy.iinfo(alphaMask.dtype).max
    normalizedAlphaMask = (1.0 / maxAlpha) * alphaMask
    
    # Calculate the normalized inverse alpha mask.
    normalizedInverseAlphaMask = \
        numpy.ones_like(normalizedAlphaMask)
    normalizedInverseAlphaMask[:] = \
        normalizedInverseAlphaMask - normalizedAlphaMask
    
    # Split the channels from the sources.
    foregroundChannels = cv2.split(foregroundSrc)
    backgroundChannels = cv2.split(backgroundSrc)
    
    # Blend each channel.
    numChannels = len(foregroundChannels)
    i = 0
    while i < numChannels:
        backgroundChannels[i][:] = \
            normalizedAlphaMask * foregroundChannels[i] + \
            normalizedInverseAlphaMask * backgroundChannels[i]
        i += 1
    
    # Merge the blended channels into the destination.
    cv2.merge(backgroundChannels, dst)


def strokeEdges(src, dst, blurKsize = 7, edgeKsize = 5):
    if blurKsize >= 3:
        blurredSrc = cv2.medianBlur(src, blurKsize)
        graySrc = cv2.cvtColor(blurredSrc, cv2.COLOR_BGR2GRAY)
    else:
        graySrc = cv2.cvtColor(src, cv2.COLOR_BGR2GRAY)
    cv2.Laplacian(graySrc, cv2.cv.CV_8U, graySrc, ksize = edgeKsize)
    normalizedInverseAlpha = (1.0 / 255) * (255 - graySrc)
    channels = cv2.split(src)
    for channel in channels:
        channel[:] = channel * normalizedInverseAlpha
    cv2.merge(channels, dst)


class VFuncFilter(object):
    """A filter that applies a function to V (or all of BGR)."""
    
    def __init__(self, vFunc = None, dtype = numpy.uint8):
        length = numpy.iinfo(dtype).max + 1
        self._vLookupArray = utils.createLookupArray(vFunc, length)
    
    def apply(self, src, dst):
        """Apply the filter with a BGR or gray source/destination."""
        srcFlatView = utils.flatView(src)
        dstFlatView = utils.flatView(dst)
        utils.applyLookupArray(self._vLookupArray, srcFlatView,
                               dstFlatView)

class VCurveFilter(VFuncFilter):
    """A filter that applies a curve to V (or all of BGR)."""
    
    def __init__(self, vPoints, dtype = numpy.uint8):
        VFuncFilter.__init__(self, utils.createCurveFunc(vPoints),
                             dtype)


class BGRFuncFilter(object):
    """A filter that applies different functions to each of BGR."""
    
    def __init__(self, vFunc = None, bFunc = None, gFunc = None,
                 rFunc = None, dtype = numpy.uint8):
        length = numpy.iinfo(dtype).max + 1
        self._bLookupArray = utils.createLookupArray(
            utils.createCompositeFunc(bFunc, vFunc), length)
        self._gLookupArray = utils.createLookupArray(
            utils.createCompositeFunc(gFunc, vFunc), length)
        self._rLookupArray = utils.createLookupArray(
            utils.createCompositeFunc(rFunc, vFunc), length)
    
    def apply(self, src, dst):
        """Apply the filter with a BGR source/destination."""
        b, g, r = cv2.split(src)
        utils.applyLookupArray(self._bLookupArray, b, b)
        utils.applyLookupArray(self._gLookupArray, g, g)
        utils.applyLookupArray(self._rLookupArray, r, r)
        cv2.merge([b, g, r], dst)

class BGRCurveFilter(BGRFuncFilter):
    """A filter that applies different curves to each of BGR."""
    
    def __init__(self, vPoints = None, bPoints = None,
                 gPoints = None, rPoints = None, dtype = numpy.uint8):
        BGRFuncFilter.__init__(self,
                               utils.createCurveFunc(vPoints),
                               utils.createCurveFunc(bPoints),
                               utils.createCurveFunc(gPoints),
                               utils.createCurveFunc(rPoints), dtype)

class BGRCrossProcessCurveFilter(BGRCurveFilter):
    """A filter that applies cross-process-like curves to BGR."""
    
    def __init__(self, dtype = numpy.uint8):
        BGRCurveFilter.__init__(
            self,
            bPoints = [(0,20),(255,235)],
            gPoints = [(0,0),(56,39),(208,226),(255,255)],
            rPoints = [(0,0),(56,22),(211,255),(255,255)],
            dtype = dtype)

class BGRPortraCurveFilter(BGRCurveFilter):
    """A filter that applies Portra-like curves to BGR."""
    
    def __init__(self, dtype = numpy.uint8):
        BGRCurveFilter.__init__(
            self,
            vPoints = [(0,0),(23,20),(157,173),(255,255)],
            bPoints = [(0,0),(41,46),(231,228),(255,255)],
            gPoints = [(0,0),(52,47),(189,196),(255,255)],
            rPoints = [(0,0),(69,69),(213,218),(255,255)],
            dtype = dtype)

class BGRProviaCurveFilter(BGRCurveFilter):
    """A filter that applies Provia-like curves to BGR."""
    
    def __init__(self, dtype = numpy.uint8):
        BGRCurveFilter.__init__(
            self,
            bPoints = [(0,0),(35,25),(205,227),(255,255)],
            gPoints = [(0,0),(27,21),(196,207),(255,255)],
            rPoints = [(0,0),(59,54),(202,210),(255,255)],
            dtype = dtype)

class BGRVelviaCurveFilter(BGRCurveFilter):
    """A filter that applies Velvia-like curves to BGR."""
    
    def __init__(self, dtype = numpy.uint8):
        BGRCurveFilter.__init__(
            self,
            vPoints = [(0,0),(128,118),(221,215),(255,255)],
            bPoints = [(0,0),(25,21),(122,153),(165,206),(255,255)],
            gPoints = [(0,0),(25,21),(95,102),(181,208),(255,255)],
            rPoints = [(0,0),(41,28),(183,209),(255,255)],
            dtype = dtype)


class VConvolutionFilter(object):
    """A filter that applies a convolution to V (or all of BGR)."""
    
    def __init__(self, kernel):
        self._kernel = kernel
    
    def apply(self, src, dst):
        """Apply the filter with a BGR or gray source/destination."""
        cv2.filter2D(src, -1, self._kernel, dst)

class BlurFilter(VConvolutionFilter):
    """A blur filter with a 2-pixel radius."""
    
    def __init__(self):
        kernel = numpy.array([[0.04, 0.04, 0.04, 0.04, 0.04],
                              [0.04, 0.04, 0.04, 0.04, 0.04],
                              [0.04, 0.04, 0.04, 0.04, 0.04],
                              [0.04, 0.04, 0.04, 0.04, 0.04],
                              [0.04, 0.04, 0.04, 0.04, 0.04]])
        VConvolutionFilter.__init__(self, kernel)

class SharpenFilter(VConvolutionFilter):
    """A sharpen filter with a 1-pixel radius."""
    
    def __init__(self):
        kernel = numpy.array([[-1, -1, -1],
                              [-1,  9, -1],
                              [-1, -1, -1]])
        VConvolutionFilter.__init__(self, kernel)

class FindEdgesFilter(VConvolutionFilter):
    """An edge-finding filter with a 1-pixel radius."""
    
    def __init__(self):
        kernel = numpy.array([[-1, -1, -1],
                              [-1,  8, -1],
                              [-1, -1, -1]])
        VConvolutionFilter.__init__(self, kernel)

class EmbossFilter(VConvolutionFilter):
    """An emboss filter with a 1-pixel radius."""
    
    def __init__(self):
        kernel = numpy.array([[-2, -1, 0],
                              [-1,  1, 1],
                              [ 0,  1, 2]])
        VConvolutionFilter.__init__(self, kernel)
