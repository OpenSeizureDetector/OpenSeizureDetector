/**
 * Based on example at http://ijoshsmith.com/2014/01/25/video-streaming-from-an-ip-camera-to-an-android-phone/
 *
 *
 * Note:  This does not work - gives a mediaplayer error E/MediaPlayer: Error (1,-2147483648).
 *
 *
 */

package uk.org.openseizuredetector.bentv;

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

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
                                                                MediaPlayer.OnInfoListener,
                                                                MediaPlayer.OnErrorListener,
                                                                MediaPlayer.OnVideoSizeChangedListener,
                                                                SurfaceHolder.Callback {
    final static String TAG = "MainActivity";
    final static String USERNAME = "guest";
    final static String PASSWORD = "guest";
    //final static String RTSP_URL = "rtsp://guest:guest@192.168.1.25/11";
    //final static String RTSP_URL = "rtsp://guest:guest@192.168.1.26:88/videoSub";
    // NOTE:  setting username and password in header does not work - has to be in URL.
    final static String RTSP_URL = "rtsp://guest:guest@192.168.1.18/live_mpeg4.sdp";
    //final static String RTSP_URL = "rtsp://192.168.1.18/live_mpeg4.sdp";
    //final static String RTSP_URL = "rtsp://192.168.1.18/live_3gpp.sdp";

    private MediaPlayer _mediaPlayer;
    private SurfaceHolder _surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG,"onCreate() 1");
        super.onCreate(savedInstanceState);

        // Set up a full-screen black window.
       /* requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setBackgroundDrawableResource(android.R.color.black);
        */

        setContentView(R.layout.activity_main);


        // Configure the view that renders live video.
        SurfaceView surfaceView =
                (SurfaceView) findViewById(R.id.surfaceView);
        _surfaceHolder = surfaceView.getHolder();
        _surfaceHolder.addCallback(this);
        Log.v(TAG, "original size = " + _surfaceHolder.getSurfaceFrame().toString());
        //_surfaceHolder.setFixedSize(320, 240);
        _surfaceHolder.setFixedSize(650,360);
        Log.v(TAG, "new size = " + _surfaceHolder.getSurfaceFrame().toString());

        Log.v(TAG, "onCreate() 2");


        }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG,"onPrepared() 1");
        _mediaPlayer.start();
        Log.v(TAG, "onPrepared() 2");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG,"surfaceChanged() - w="+width+" h="+height);
        Log.v(TAG, "holder surfaceFrame size = " + _surfaceHolder.getSurfaceFrame().toString());

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated() 1");
        _mediaPlayer = new MediaPlayer();
        _mediaPlayer.setOnErrorListener(this);
        _mediaPlayer.setOnInfoListener(this);
        Log.v(TAG,"surfaceCreated() 2");
        _mediaPlayer.setDisplay(_surfaceHolder);
        Log.v(TAG, "surfaceCreated() 3");

        Context context = getApplicationContext();
        Map<String, String> headers = getRtspHeaders();
        Log.v(TAG, "surfaceCreated() 4 - Headers = " + headers.toString());
        Uri source = Uri.parse(RTSP_URL);
        Log.v(TAG,"surfaceCreated() 4 - Uri = "+source.toString());

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

        }
        catch (Exception e) {
            Log.e(TAG,"Error - "+e.toString());
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
}
