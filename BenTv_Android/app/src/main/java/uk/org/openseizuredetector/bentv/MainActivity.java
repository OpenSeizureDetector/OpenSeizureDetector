/**
 * Based on example at http://ijoshsmith.com/2014/01/25/video-streaming-from-an-ip-camera-to-an-android-phone/
 *
 *
 * Note:  This does not work - gives a mediaplayer error E/MediaPlayer: Error (1,-2147483648).
 *
 *
 */

package uk.org.openseizuredetector.bentv;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gst_sdk_tutorials.rtspviewersf.GStreamerSurfaceView;
import com.gst_sdk_tutorials.rtspviewersf.PlayerConfiguration;
import com.gst_sdk_tutorials.rtspviewersf.RTSPViewerSF;

import java.util.HashMap;
import java.util.Map;
import org.freedesktop.gstreamer.GStreamer;


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
                                                                MediaPlayer.OnInfoListener,
                                                                MediaPlayer.OnErrorListener,
                                                                MediaPlayer.OnVideoSizeChangedListener,
                                                                SurfaceHolder.Callback {
    final static String TAG = "MainActivity";
    final static String USERNAME = "guest";
    final static String PASSWORD = "guest";
    final static String RTSP_URL = "rtsp://guest:guest@192.168.1.25/12";
    //final static String RTSP_URL = "rtsp://guest:guest@192.168.1.26:88/videoSub";
    // NOTE:  setting username and password in header does not work - has to be in URL.
    //final static String RTSP_URL = "rtsp://guest:guest@192.168.1.18/live_mpeg4.sdp";
    //final static String RTSP_URL = "rtsp://192.168.1.18/live_mpeg4.sdp";
    //final static String RTSP_URL = "rtsp://192.168.1.18/live_3gpp.sdp";

    private MediaPlayer _mediaPlayer;
    private SurfaceHolder _surfaceHolder;


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

    private long native_custom_data;      // Native code will store the player here

    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
    private int position;                 // Current position, reported by native code
    private int duration;                 // Current clip duration, reported by native code
    private int desired_position;         // Position where the users wants to seek to
    private PlayerConfiguration playerConfig = new PlayerConfiguration();              // URI of the clip being played
    private String state;
    private boolean is_full_screen;


    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("mediaplayer");
    }

    public MainActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate() 1");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // And configure the native (Gstreamer) video view.

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.i(TAG,"Gstreamer Initialised OK");
        Toast.makeText(this, "Gstreamer Initialised ok!!", Toast.LENGTH_LONG).show();

        SurfaceView sv = (SurfaceView)findViewById(R.id.surfaceView2);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);

        if (!nativeLayerInit())
            throw new RuntimeException("Failed to initialize Native layer(not all necessary interface methods implemeted?)");

        native_custom_data = nativePlayerCreate();
        nativeSetUri (native_custom_data, RTSP_URL, USERNAME, PASSWORD);

        nativePlay(native_custom_data);


    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG,"onPrepared() 1");
        _mediaPlayer.start();
        Log.v(TAG, "onPrepared() 2");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged() - w=" + width + " h=" + height);
        Log.v(TAG, "holder surfaceFrame size = " + holder.getSurfaceFrame().toString());

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder==((SurfaceView)findViewById(R.id.surfaceView1)).getHolder()) {
            Log.v(TAG, "surfaceCreated()  - SurfaceView 1");
            _mediaPlayer = new MediaPlayer();
            _mediaPlayer.setOnErrorListener(this);
            _mediaPlayer.setOnInfoListener(this);
            Log.v(TAG, "surfaceCreated() 2");
            _mediaPlayer.setDisplay(_surfaceHolder);
            Log.v(TAG, "surfaceCreated() 3");

            Context context = getApplicationContext();
            Map<String, String> headers = getRtspHeaders();
            Log.v(TAG, "surfaceCreated() 4 - Headers = " + headers.toString());
            Uri source = Uri.parse(RTSP_URL);
            Log.v(TAG, "surfaceCreated() 4 - Uri = " + source.toString());

            try {
                // Specify the IP camera's URL and auth headers.
                _mediaPlayer.setDataSource(context, source, headers);
                //_mediaPlayer.setDataSource(RTSP_URL);
                //_mediaPlayer.setDataSource(context, source);
                //_mediaPlayer = MediaPlayer.create(this,R.raw.stream_12);
                //_mediaPlayer.start();
                Log.v(TAG, "surfaceCreated() 5");

                // Begin the process of setting up a video stream.
                _mediaPlayer.setOnPreparedListener(this);
                Log.v(TAG, "surfaceCreated() 6");
                _mediaPlayer.prepareAsync();
                Log.v(TAG, "surfaceCreated() 7");
            } catch (Exception e) {
                Log.e(TAG, "Error - " + e.toString());
            }
        }
        else if (holder==((SurfaceView)findViewById(R.id.surfaceView2)).getHolder()) {
            Log.v(TAG, "surfaceCreated() - SurfaceView 2");
            nativeSurfaceInit (native_custom_data, holder.getSurface());
            Log.i (TAG, "GStreamer initialized:");
            nativeSetUri (native_custom_data, RTSP_URL, USERNAME, PASSWORD);
            nativeSetPosition (native_custom_data, 0);
            nativePlay(native_custom_data);
            Log.v(TAG,"native player playing...");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed() 1");
        _mediaPlayer.release();
    }

    private Map<String, String> getRtspHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        String basicAuthValue = getBasicAuthValue(USERNAME, PASSWORD);
        headers.put("Authorization", basicAuthValue);
        return headers;
    }

    private String getBasicAuthValue(String usr, String pwd) {
        String credentials = usr + ":" + pwd;
        int flags = Base64.URL_SAFE | Base64.NO_WRAP;
        byte[] bytes = credentials.getBytes();
        return "Basic " + Base64.encodeToString(bytes, flags);
    }

    /**
     * info and error callbacks from
     * http://stackoverflow.com/questions/23716019/how-to-resume-mediaplayer-in-android-after-press-the-home-button-and-reopen-the
     *
     * @param mp
     * @param whatError
     * @param extra
     * @return
     */
    public boolean onError(MediaPlayer mp, int whatError, int extra) {
        Log.v(TAG, "onError Called");

        if (whatError == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            Log.v(TAG, "Media Error, Server Died " + extra);
        } else if (whatError == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            Log.v(TAG, "Media Error, Error Unknown " + extra);
        }

        return false;
    }

    public boolean onInfo(MediaPlayer mp, int whatInfo, int extra) {
        if (whatInfo == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) {
            Log.v(TAG, "Media Info, Media Info Bad Interleaving " + extra);
        } else if (whatInfo == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
            Log.v(TAG, "Media Info, Media Info Not Seekable " + extra);
        } else if (whatInfo == MediaPlayer.MEDIA_INFO_UNKNOWN) {
            Log.v(TAG, "Media Info, Media Info Unknown " + extra);
        } else if (whatInfo == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
            Log.v(TAG, "MediaInfo, Media Info Video Track Lagging " + extra);
        /*
         * Android Version 2.0 and Higher } else if (whatInfo ==
         * MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
         * Log.v(LOGTAG,"MediaInfo, Media Info Metadata Update " + extra);
         */
        }
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(TAG,"onVideoSizeChanged - w="+width+" h="+height);
    }

    // Called from native code.
    private void nativeStateChanged(long data, final String state) {
        final TextView tv;
        final String message;
        int player_id = 0;

        this.state = state;



        runOnUiThread(new Runnable() {
            public void run() {
                Log.i(TAG,"nativeStateChanged() - state="+state);
                Toast.makeText(getApplicationContext(), "state=" + state, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Called from native code.
    private void nativeErrorOccured(long data, String message) {
        final String ui_message;
        int player_id = 0;

        ui_message = "Player " + player_id + ":" + message;

        runOnUiThread(new Runnable() {
            public void run() {
                Log.e(TAG,ui_message);
                Toast.makeText(getApplicationContext(), ui_message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Called from native code
    private void nativePositionUpdated(long data, final int position, final int duration) {

        Log.v(TAG,"nativePositionUpdate()");
        this.position = position;
        this.duration = duration;
    }

    // Called from native code when the size of the media changes or is first detected.
    // Inform the video surface about the new size and recalculate the layout.
    private void nativeMediaSizeChanged (long data, int width, int height) {
        int player_id = 0;

        Log.i ("GStreamer", "Media size changed to " + width + "x" + height);

        final GStreamerSurfaceView gsv = (GStreamerSurfaceView) findViewById(R.id.surfaceView2);
        gsv.media_width = width;
        gsv.media_height = height;
        runOnUiThread(new Runnable() {
            public void run() {
                gsv.requestLayout();
            }
        });
    }


}
