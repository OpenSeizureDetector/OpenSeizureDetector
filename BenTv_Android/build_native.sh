#!/bin/sh
cd app/src/main
export GSTREAMER_ROOT_ANDROID=/usr/local/gstreamer-android
/usr/local/android-ndk-r10e/ndk-build
