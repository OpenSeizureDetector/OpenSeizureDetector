package uk.org.openseizuredetector.bentv;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import uk.org.openseizuredetector.bentv.mediaplayergs.MediaPlayerGs;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private MediaPlayerGs mp1;
    private MediaPlayerGs mp2;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mp1 = new MediaPlayerGs(this);
        mp1.setDataSource("rtsp://guest:guest@192.168.1.6/play2.sdp");

        mp2 = new MediaPlayerGs(this);
        mp2.setDataSource("rtsp://guest:guest@192.168.1.25/12");

        ((SurfaceView) findViewById(R.id.surfaceView1)).getHolder().addCallback(this);
        ((SurfaceView) findViewById(R.id.surfaceView2)).getHolder().addCallback(this);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged()");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated()");
        if (holder==((SurfaceView) findViewById(R.id.surfaceView1)).getHolder()) {
            Log.i(TAG, "Starting player 1");
            mp1.setDisplay(holder);
            mp1.start();
        }
        if (holder==((SurfaceView) findViewById(R.id.surfaceView2)).getHolder()) {
            Log.i(TAG, "Starting player 2");
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
            mp2.release();
        }

    }
}
