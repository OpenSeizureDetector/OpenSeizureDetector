# Bentv_Android - README
# ======================

## Compiling

### Command Line
 * android update project -p . -n benTV (creates ant build.xml file - see the Android [Documentation](http://developer.android.com/tools/projects/projects-cmdline.html))
 * ant debug    (creates bin/benTV-debug.apk).
 * android avd (start an emulator - see the Android [Documentation](http://developer.android.com/tools/building/building-cmdline.html))
 * adb install -r bin/benTV-debug.apk (to install it on the emulator (-r to reinstall if necessary))
 * adb -s <serial number> install bin/benTV-debug.apk (to install it on an specific device)
 * adb shell pm uninstall uk.org.openseizuredetector.bentv (to uninstall the package - necessary if you change development computer to avoid errors when trying to install a package built on a different computer).

