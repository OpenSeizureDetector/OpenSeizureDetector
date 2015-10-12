package uk.org.openseizuredetector.bentv;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import uk.org.openseizuredetector.bentv.mediaplayergs.MediaPlayerGs;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        MediaPlayerGs.MediaPlayerGsCallback {
    private MediaPlayerGs mp1;
    private MediaPlayerGs mp2;
    private ProgressDialog pd1,pd2;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        mp1 = new MediaPlayerGs(this);
        mp1.setCallback(this);
        mp1.setDataSource("rtsp://guest:guest@192.168.1.6/play2.sdp");
        pd1 = new ProgressDialog(this);
        pd1.setTitle("Buffering...");
        pd1.setMessage("mp1 Buffering");
        pd1.setCancelable(false);
        pd1.setIndeterminate(true);

        mp2 = new MediaPlayerGs(this);
        mp2.setCallback(this);
        mp2.setDataSource("rtsp://guest:guest@192.168.1.25/12");
        pd2 = new ProgressDialog(this);
        pd2.setTitle("Buffering...");
        pd2.setMessage("mp2 Buffering");
        pd2.setCancelable(false);
        pd2.setIndeterminate(true);


        ((SurfaceView) findViewById(R.id.surfaceView1)).getHolder().addCallback(this);
        ((SurfaceView) findViewById(R.id.surfaceView2)).getHolder().addCallback(this);

    }

    // Implement Surface callbacks (surfaceChanged, surfaceCreated, surfaceDestroyed
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged()");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated()");
        if (holder==((SurfaceView) findViewById(R.id.surfaceView1)).getHolder()) {
            Log.i(TAG, "Starting player 1");
            pd1.show();
            mp1.setDisplay(holder);
            mp1.start();
        }
        if (holder==((SurfaceView) findViewById(R.id.surfaceView2)).getHolder()) {
            Log.i(TAG, "Starting player 2");
            pd2.show();
            mp2.setDisplay(holder);
            mp2.start();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (holder==((SurfaceView) findViewById(R.id.surfaceView1)).getHolder()) {
            mp1.release();
        }
        if (holder==((SurfaceView) findViewById(R.id.surfaceView2)).getHolder()) {
            if (mp2!=null) mp2.release();
        }

    }

    @Override
    public void onMediaPlayerGsError(long handle, String msg) {
        Log.e(TAG,"onMediaPlayerGsError - msg="+msg);
    }

    @Override
    public void onMediaPlayerGsPositionUpdated(long handle, int position, int duration) {
        Log.v(TAG,"onMediaPlayerGsPositionUpdated() - position="+position+" duration="+duration);
    }

    @Override
    public void onMediaPlayerGsMediaSizeChanged(long handle, int width, int height) {
        Log.i(TAG, "onMediaPlayerGsMediaSizeChanged() - (" + width + "," + height + ")");
        if (handle == mp1.getHandle())
            pd1.hide();
        if (handle == mp2.getHandle())
            pd2.hide();
    }

    @Override
    public void onMediaPlayerGsStateChanged(long handle, String state) {
        Log.v(TAG,"onMediaPlayerGsStateChanged() - state="+state);
    }
}
