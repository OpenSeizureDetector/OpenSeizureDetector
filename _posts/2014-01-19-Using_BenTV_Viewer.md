---
layout: post
title: Using BenTV Viewer
category: Meta

excerpt: Basic instructions on using the BenTV Raspberry Pi Based Viewer applicaton.

---

When the BenTV raspberry pi viewer is switched on, it displays an image from the
web camera with a small two line status bar at the bottom of the screen.

The contents of the status bar depends on the current mode of bentv.  Two modes
are currently supported:

* Camera - allows the user to control the web camera position.
* Fit_Detector - displays the output of the fit detector and allows the user to reset the background image to the current image being viewed by the fit detector camera.

The interface starts in Fit_Detector mode.

The active mode is selected by a short (0.1-0.5 seconds) press of the button attached to the side of the TV monitor.   The active mode switches between the 
available modes with each button press.

A long (>0.5 seconds) button press performs an action depending on the mode:

* In Camera Mode, it moves the camera to the next preset position.
* In Fit_Detector Mode, it instructs the fit detector to save the current image
  as the background image to be used for background subtraction - do this if the
  fit detector is giving odd results - it usually means it is looking at
  something that is not Benjamin because the contents of the room have changed
  since the last time the background was saved.

