package uk.org.seizuredetector.bentv;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //String uri = "rtsp://guest:guest@192.168.1.24/12";
        String uri = "rtsp://v6.cache1.c.youtube.com/CjYLENy73wIaLQkDsLHya4-Z9hMYDSANFEIJbXYtZ29vZ2xlSARSBXdhdGNoYKX4k4uBjbOiUQw=/0/0/0/video.3gp";
        //String uri = "android.resource://uk.org.seizuredetector.bentv/" + R.raw.ghostbusters;
        VideoView v = (VideoView) findViewById( R.id.videoView );
        v.setVideoURI( Uri.parse(uri) );
        v.setMediaController( new MediaController( this ) );
        v.requestFocus();
        v.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
