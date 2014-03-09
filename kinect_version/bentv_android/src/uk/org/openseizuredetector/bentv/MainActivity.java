package uk.org.openseizuredetector.bentv;

import android.app.Activity;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Object;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONException;

public class MainActivity extends Activity {
    // Image update period (in ms)
    private static final int UPDATE_DELAY = 5000;

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
	//findViewById(R.id.webCamView).
	//    setOnClickListener(mGlobal_OnClickListener);

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
                //case R.id.webCamView:
                //    //Inform the user the button2 has been clicked
                //    Toast.makeText(getApplicationContext(), 
		//		   "webCamView clicked.", 
		//		   Toast.LENGTH_SHORT).show();                
		//    setModeSingleView();
                //break;
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


    /**
     *********************************************
     * The timer to update the images regularly  *
     *********************************************/
    final CountDownTimer timer = new CountDownTimer(Long.MAX_VALUE, UPDATE_DELAY) {
	    @Override
	    public void onTick(long millisUntilFinished) {
		new DownLoadImageTask().execute("http://192.168.1.14:8080/rawImg",findViewById(R.id.rawImgView));
		new DownLoadImageTask().execute("http://192.168.1.14:8080/maskedImg",findViewById(R.id.maskedImgView));
		new DownLoadImageTask().execute("http://192.168.1.14:8080/chartImg",findViewById(R.id.chartImgView));
		//new DownLoadImageTask().execute("http://192.168.1.24/tmpfs/auto.jpg",findViewById(R.id.webCamView));
		new DownLoadSeizureDataTask().execute("http://192.168.1.14:8080/jsonData",findViewById(R.id.sdOutput));
	    }
	    
	    @Override
	    public void onFinish() {
		// don't care timer should never "finish" since Long.MAXVALUE is 200 years in the future
	    }
	};
    
    @Override
    protected void onStart() {
        super.onStart();
        timer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.start();
    }



    public class DownLoadSeizureDataTask extends AsyncTask<Object, Void, String> {
	/** The system calls this to perform work in a worker thread and
	 * delivers it the parameters given to AsyncTask.execute() */
	TextView targetTextView;
	protected String doInBackground(Object... params) {
	    targetTextView = (TextView)params[1];
	    return loadSeizureDataFromNetwork((String)params[0]);
	}
    
	/** The system calls this to perform work in the UI thread and delivers
	 * the result from doInBackground() */
	protected void onPostExecute(String result) {
	    if (result!=null) {
		Log.v("DownLoadSeizureData",result);
		Toast.makeText(getApplicationContext(), 
			       "Got Seizure Data", 
			       Toast.LENGTH_SHORT).show();                
		try {
		    JSONObject jo = new JSONObject(result);
		    int sdStatus = jo.getInt("status");
		    int sdRate = jo.getInt("rate");
		    targetTextView.setTextSize((float)24.0);
		    targetTextView.setText("Rate = "+sdRate+" bpm");
		    int bgColour;
		    switch(sdStatus) {
		    case 0:   // ok
			bgColour = 0xff0000ff;
			break;
		    case 1:
			bgColour = 0xffaaaa00;
			break;
		    case 2:
			bgColour = 0xffff0000;
			break;
		    case 3:
			bgColour = 0xffaaaaaa;
			break;
		    default:
			bgColour = 0xff000000;
		    }
		    targetTextView.setBackgroundColor(bgColour);

		}
		catch(JSONException ex) {
		    ex.printStackTrace();
		    targetTextView.setText("Failed to Parse Seizure Data");
		}

	    } else {
		Toast.makeText(getApplicationContext(), 
			       "Failed to download seizure data", 
			       Toast.LENGTH_SHORT).show();                
	    }
	}
    }
 
    private String loadSeizureDataFromNetwork(String dataURL){
	try {
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(dataURL);
	    HttpResponse httpResponse = httpClient.execute(httpGet);
	    BufferedReader in = new BufferedReader(new InputStreamReader(httpResponse
        .getEntity().getContent()));

	    String response = in.readLine();
	    Log.v("loadSeizureDataFromNetwork",response);
	    return response;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }   




    public class DownLoadImageTask extends AsyncTask<Object, Void, Bitmap> {
	/** The system calls this to perform work in a worker thread and
	 * delivers it the parameters given to AsyncTask.execute() */
	ImageView targetImgView;
	protected Bitmap doInBackground(Object... params) {
	    targetImgView = (ImageView)params[1];
	    return loadImageFromNetwork((String)params[0]);
	}
    
	/** The system calls this to perform work in the UI thread and delivers
	 * the result from doInBackground() */
	protected void onPostExecute(Bitmap result) {
	    if (result!=null) {
		targetImgView.setImageBitmap(result);
		//Toast.makeText(getApplicationContext(), 
		//	       "Setting Bitmap", 
		//	       Toast.LENGTH_SHORT).show();                
	    } else {
		Toast.makeText(getApplicationContext(), 
			       "Failed to download image", 
			       Toast.LENGTH_SHORT).show();                
	    }
	}
    }
 
    private Bitmap loadImageFromNetwork(String imageURL){
	try {
	    URL url = new URL(imageURL);
	    final String authString = "guest" + ":" + "guest";
	    Log.v("loadImageFromNetwork","authString="+authString);
	    //final String authStringEnc = Base64.encodeToString(authString.getBytes(),Base64.NO_WRAP);
	    //Log.v("loadImageFromNetwork","authString="+authStringEnc);

	    URLConnection conn = url.openConnection();
	    conn.setRequestProperty("Authorization", "Basic " + Base64.encode("guest:guest".getBytes(),Base64.DEFAULT));
	    conn.connect();
	    InputStream is = conn.getInputStream();
	    BufferedInputStream bis = new BufferedInputStream(is, 8192);
	    Bitmap bitmap = BitmapFactory.decodeStream(bis);
	    //InputStream ip = (InputStream)new URL(imageURL).getContent();
	    //Bitmap bitmap = BitmapFactory.decodeStream(ip);
	    return bitmap;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }   



}
