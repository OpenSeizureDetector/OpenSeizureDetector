package uk.org.openseizuredetector.bentv;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.MediaController;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.app.ActionBar;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.bentv_table_layout);

	findViewById(R.id.rawImgView).
	    setOnClickListener(mGlobal_OnClickListener);
        findViewById(R.id.maskedImgView).
	    setOnClickListener(mGlobal_OnClickListener);
	findViewById(R.id.chartImgView).
	    setOnClickListener(mGlobal_OnClickListener);
	findViewById(R.id.webCamView).
	    setOnClickListener(mGlobal_OnClickListener);

	SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, 
		   R.array.spinner,
          android.R.layout.simple_spinner_dropdown_item);
        //String url = "rtsp://guest:guest@192.168.1.24/12";
                
        
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
                case R.id.rawImgView:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "rawImgView clicked.", 
				   Toast.LENGTH_SHORT).show();                
		    setModeSingleView();
                break;
                case R.id.maskedImgView:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "maskedImgView clicked.", 
				   Toast.LENGTH_SHORT).show();                
		    setModeSingleView();
                break;
                case R.id.chartImgView:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "chartImgView clicked.", 
				   Toast.LENGTH_SHORT).show();                
		    setModeSingleView();
                break;
                case R.id.webCamView:
                    //Inform the user the button2 has been clicked
                    Toast.makeText(getApplicationContext(), 
				   "webCamView clicked.", 
				   Toast.LENGTH_SHORT).show();                
		    setModeSingleView();
                break;
            }
        }
    };

    public boolean setModeSingleView() {
	setContentView(R.layout.bentv_single_image_layout);
	ActionBar actionBar = getActionBar();
	actionBar.setDisplayHomeAsUpEnabled(true);
	return true;
    }

    @Override
    public boolean onNavigateUp() {
	// Respond to the action bar Home/Back button.
	setContentView(R.layout.bentv_table_layout);
	ActionBar actionBar = getActionBar();
	actionBar.setDisplayHomeAsUpEnabled(false);
	return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle presses on the action bar items
	switch (item.getItemId()) {
        case R.id.action_search:
	    Toast.makeText(getApplicationContext(), 
			   "action_search clicked.", 
			   Toast.LENGTH_SHORT).show();                
            return true;
        case R.id.action_compose:
	    Toast.makeText(getApplicationContext(), 
			   "action_compose clicked.", 
			   Toast.LENGTH_SHORT).show();                
            return true;
        case R.id.movecamera_1:
	    Toast.makeText(getApplicationContext(), 
			   "movecamera_1 clicked.", 
			   Toast.LENGTH_SHORT).show();                
            return true;
        case R.id.movecamera_2:
	    Toast.makeText(getApplicationContext(), 
			   "movecamera_2 clicked.", 
			   Toast.LENGTH_SHORT).show();                
            return true;
        case R.id.movecamera_3:
	    Toast.makeText(getApplicationContext(), 
			   "movecamera_3 clicked.", 
			   Toast.LENGTH_SHORT).show();                
            return true;
        case R.id.movecamera_4:
	    Toast.makeText(getApplicationContext(), 
			   "movecamera_4 clicked.", 
			   Toast.LENGTH_SHORT).show();                
            return true;
        default:
            return super.onOptionsItemSelected(item);
	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu items for use in the action bar
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.action_bar, menu);
	return super.onCreateOptionsMenu(menu);
    }

}
