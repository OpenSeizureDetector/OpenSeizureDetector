package uk.org.openseizuredetector.bentv;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.MediaController;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.bentv_table_layout);

	findViewById(R.id.pos1Button).setOnClickListener(mGlobal_OnClickListener);
	findViewById(R.id.pos2Button).setOnClickListener(mGlobal_OnClickListener);
	findViewById(R.id.pos3Button).setOnClickListener(mGlobal_OnClickListener);
        findViewById(R.id.pos4Button).setOnClickListener(mGlobal_OnClickListener);
        
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
        
    }

    //Global On click listener for all views
    final OnClickListener mGlobal_OnClickListener = new OnClickListener() {
        public void onClick(final View v) {
            switch(v.getId()) {
                case R.id.pos1Button:
                    //Inform the user the button1 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "Button1 clicked.", 
				   Toast.LENGTH_SHORT).show();                
                break;
                case R.id.pos2Button:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "Button2 clicked.", 
				   Toast.LENGTH_SHORT).show();                
                break;
                case R.id.pos3Button:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "Button3 clicked.", 
				   Toast.LENGTH_SHORT).show();                
                break;
                case R.id.pos4Button:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "Button4 clicked.", 
				   Toast.LENGTH_SHORT).show();                
                break;
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void onClick(View view) {

    }

}
