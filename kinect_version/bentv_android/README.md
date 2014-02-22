# Bentv_Android - README
# ======================

## Compiling

### Command Line
 * android update project -p . -n benTV (creates ant build.xml file - see the Android [Documentation](http://developer.android.com/tools/projects/projects-cmdline.html))
 * ant debug    (creates bin/benTV-debug.apk).
 * android avd (start an emulator - see the Android [Documentation](http://developer.android.com/tools/building/building-cmdline.html))
 * adb install bin/benTV-debug.apk (to install it on the emulator)
 * adb install -d bin/benTV-debug.apk (to install it on an attached real device)

