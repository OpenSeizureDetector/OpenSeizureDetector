* Compilation

This app uses native gstreamer libraries, so these are needed along with the Android NDK to compile them.

1. Download the Gstreamer for Android Sdk from http://docs.gstreamer.com/display/GstSDK/Installing+for+Android+development.
2. Unpack it (mine is in /usr/local/gstreamer-android)
3. Update build_native.sh to point GSTREAMER_ROOT_ANDROID to the gstreamer NDK directory.
4. Execute ./build_native.sh - this compiles the native code into library files. in ./app/src/main/libs.
5. Create a symbolic link from ./app/src/main/libs to ./app/src/main/jniLibs, becuse this is where the Gradle build sytem looks for libraries. (ln -s ./app/src/main/libs ./app/src/main/jniLibs)