package uk.org.openseizuredetector.bentv.mediaplayergs;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import org.freedesktop.gstreamer.GStreamer;


/**
 * MediaPlayerGs - a GStreamer based implementation of a mediaplayer class, which is intended
 * to overcome some of the limitations of the android MediaPlayer class in terms of codec
 * comaptibility with some IP cameras.
 * <p/>
 * Copyright Graham Jones, 2015 (graham@openseizuredetector.org.uk)
 * MediaPlayerGs is based heavily on RtspViewer (https://github.com/otonchev/rtspviewersf), by
 * Ognyan Tonchev.
 * <p/>
 * This file is part of MediaPlayerGs.
 * <p/>
 * MediaPlayerGs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MediaPlayerGs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MediaPlayerGs.  If not, see <http://www.gnu.org/licenses/>.
 */
public class MediaPlayerGs {

    private native long nativePlayerCreate();        // Initialize native code, build pipeline, etc

    private native void nativePlayerFinalize(long data);   // Destroy pipeline and shutdown native code

    private native void nativeSetUri(long data, String uri, String user, String pass); // Set the URI of the media to play

    private native void nativePlay(long data);       // Set pipeline to PLAYING

    private native void nativeSetPosition(long data, int milliseconds); // Seek to the indicated position, in milliseconds

    private native void nativePause(long data);      // Set pipeline to PAUSED

    private native void nativeReady(long data);      // Set pipeline to READY

    private static native boolean nativeLayerInit(); // Initialize native class: cache Method IDs for callbacks

    private native void nativeSurfaceInit(long data, Object surface); // A new surface is available

    private native void nativeSurfaceFinalize(long data); // Surface about to be destroyed

    private long mNativePlayerHandle;      // Native code will store the player here
    private Context mContext;

    private String TAG = "MediaPlayerGs";
    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
    private int mPosition;                 // Current position, reported by native code
    private int mDuration;                 // Current clip duration, reported by native code
    private int desired_position;         // Position where the users wants to seek to
    private String mState;
    private boolean is_full_screen;


    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("mediaplayer");
    }


    /**
     * Class constructor - creates and initialises the media player.
     */
    public MediaPlayerGs(Context context) {
        mContext = context;
        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(context);
        } catch (Exception e) {
            Log.e(TAG, "Gstreamer Initialisation Failed");
            return;
        }
        Log.i(TAG, "Gstreamer Initialised OK");

        if (!nativeLayerInit())
            throw new RuntimeException("Failed to initialize Native layer(not all necessary interface methods implemeted?)");
        else
            Log.i(TAG, "MediaPlayerGs Native Layer Initialised OK");


        mNativePlayerHandle = nativePlayerCreate();

    }

    /**
     * Release the resources associated with this MediaPlayerGs
     */
    public void release() {
        stop();
        nativePlayerFinalize(mNativePlayerHandle);

    }

    /**
     * set the media data source (e.g. rtsp URL).
     *
     * @param path - media path (e.g. rtsp://guest:guest@192.168.1.6/play2.sdp)
     */
    public void setDataSource(String path) {
        nativeSetUri (mNativePlayerHandle, path, null,null);
    }

    /**
     * set the media data source (e.g. rtsp URL).
     *
     * @param path - media path (e.g. rtsp://192.168.1.6/play2.sdp)
     * @param uname - user name
     * @param passwd - password
     */
    public void setDataSource(String path, String uname, String passwd) {
        nativeSetUri (mNativePlayerHandle, path, uname,passwd);
    }


    /**
     * Set the SurfaceHolder to be used to display the video.
     *
     * @param sh
     */
    public void setDisplay(SurfaceHolder sh) {
        nativeSurfaceInit(mNativePlayerHandle, sh.getSurface());
    }


    /**
     * Start playback
     */
    public void start() {
        nativePlay(mNativePlayerHandle);
    }

    /**
     * Pause playback
     */
    public void pause() {
        nativePause(mNativePlayerHandle);   // Note calls Pause because no nativeStop function
    }

    /**
     * Stop Playback
     */
    public void stop() {
        nativePause(mNativePlayerHandle);   // Note calls Pause because no nativeStop function
        seekTo(0);
    }

    /**
     * set playback position to given number of miliseconds from start.
     * @param msec - new position (ms)
     */
    public void seekTo(int msec) {
        nativeSetPosition (mNativePlayerHandle, msec);
    }


    // Called from native code.
    private void nativeStateChanged(long data, final String state) {
        final String message;
        this.mState = state;
        Log.i(TAG,"nativeStateChanged() - state="+state);
    }

    // Called from native code.
    private void nativeErrorOccured(long data, String message) {
        final String ui_message;
        ui_message = "nativeErrorOccurred() " + ":" + message;
        Log.e(TAG,ui_message);
    }

    // Called from native code
    private void nativePositionUpdated(long data, final int position, final int duration) {
        Log.v(TAG,"nativePositionUpdate()");
        this.mPosition = position;
        this.mDuration = duration;
    }

    // Called from native code when the size of the media changes or is first detected.
    // Inform the video surface about the new size and recalculate the layout.
    private void nativeMediaSizeChanged (long data, int width, int height) {
        Log.i (TAG, "nativeMediaSizeChanged() - Media size changed to " + width + "x" + height);

    }


}
