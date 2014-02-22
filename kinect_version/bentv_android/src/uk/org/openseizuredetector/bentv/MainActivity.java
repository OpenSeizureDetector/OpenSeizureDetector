package uk.org.openseizuredetector.bentv;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.MediaController;
import android.widget.VideoView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String url = "rtsp://guest:guest@192.168.1.24/12";
                
        
        //VideoView videoView = (VideoView)findViewById(R.id.your_video_view);
        //MediaController mc = new MediaController(this);
        //videoView.setMediaController(mc);

        //Uri uri = Uri.parse(url);

        //videoView.setVideoURI(uri);

        //videoView.requestFocus();
        //videoView.start();
        
        //Intent i = new Intent("org.videolan.vlc.VLCApplication.gui.video.VideoPlayerActivity");
        //i.setAction(Intent.ACTION_VIEW);		
        //i.setData(Uri.parse(url));
        //startActivity(i);        
        
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.bentv_table_layout);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
