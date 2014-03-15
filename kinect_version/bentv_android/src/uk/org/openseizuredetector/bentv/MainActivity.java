package uk.org.openseizuredetector.bentv;

import android.app.Activity;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
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
import java.io.IOException;
import java.lang.Object;
import java.util.HashMap;
//import java.util.Map;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONException;

public class MainActivity extends Activity {
    CountDownTimer mTimer;

    public boolean mAudibleAlarm = false;
    public boolean mPreventSleep = true;
    public int mUpdatePeriod = 500;
    public String mSDBaseURL = "http://192.168.1.14:8080";
    public String mSDDataFname = "jsonData";
    public String mSDRawImgFname = "rawImg";
    public String mSDMaskedImgFname = "maskedImg";
    public String mSDChartImgFname = "chartImg";
    public String mSDResetURL = "saveBgImg";

    // Image update period (in ms)
    private static final int UPDATE_DELAY = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	PreferenceManager.setDefaultValues(this, 
					   R.xml.prefs, 
					   false);
	initFromSettings();

        setContentView(R.layout.bentv_table_layout);

	playWebCamAudio();
	

	//findViewById(R.id.rawImgView).
	//    setOnClickListener(mGlobal_OnClickListener);
        //findViewById(R.id.maskedImgView).
	//    setOnClickListener(mGlobal_OnClickListener);
	//findViewById(R.id.chartImgView).
	//    setOnClickListener(mGlobal_OnClickListener);

	//SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, 
	//	   R.array.spinner,
        //  android.R.layout.simple_spinner_dropdown_item);
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

    @Override
    protected void onStart() {
        super.onStart();
	Log.v("MainActivity.onStart","Starting Timer");
	initFromSettings();
	startTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
	Log.v("MainActivity.onStop","Stopping Timer");
	//Toast.makeText(getApplicationContext(), 
	//	       "Stopping Timer", 
	//	       Toast.LENGTH_SHORT).show();            
	stopTimer();
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


    public void playWebCamAudio() {
	try {
	    MediaPlayer mediaPlayer = new MediaPlayer();
	    
	    mediaPlayer.setDataSource("rtsp://guest:guest@192.168.1.24/11");
	    mediaPlayer.prepare();
	    mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
		    public void onPrepared(MediaPlayer mp) {
			mp.start();
		    }
		});
	    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	} catch (IllegalArgumentException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SecurityException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IllegalStateException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }


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
        case R.id.action_background:
	    resetBackground();
            return true;
        case R.id.action_settings:
	    Intent intent = new Intent(this, PrefActivity.class);         
	    startActivity(intent);
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


    private void initFromSettings() {
	/** Initialise member variables from Android Preferences */
	Log.v("openseizuredetector","initFromSettings()");
	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);	
    	mAudibleAlarm = settings.getBoolean("AudibleAlarm", false);
    	mPreventSleep = settings.getBoolean("PreventSleep", true);
	this.findViewById(android.R.id.content).setKeepScreenOn(mPreventSleep);
	String periodStr = settings.getString("UpdatePeriod","1000");
    	mUpdatePeriod = Integer.parseInt(periodStr);
	mSDBaseURL = settings.getString("SDBaseURL","http://192.168.1.14:8080");
	mSDDataFname = settings.getString("SDDataFname","JSONData");
	mSDRawImgFname = settings.getString("SDRawImgFname","rawImg");
	mSDMaskedImgFname = settings.getString("SDMaskedImgFname","maskedImg");
	mSDChartImgFname = settings.getString("SDChartImgFname","chartImg");
	mSDResetURL = settings.getString("SDResetURL","saveBgImg");
    }


    private void stopTimer() {
	if (mTimer!=null) mTimer.cancel();
    }

    private void startTimer() {
	/**
*********************************************
* The timer to update the images regularly  *
*********************************************/
	if (mTimer!=null) mTimer.cancel();
	Log.v("startTimer","Setting Timer to "+mUpdatePeriod+" ms");
	final CountDownTimer mTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
		@Override
		public void onTick(long millisUntilFinished) {
		    Log.v("onTick","onTick - period = "+mUpdatePeriod+" ms");
		    new DownLoadImageTask().execute(mSDBaseURL+"/"+mSDRawImgFname,findViewById(R.id.rawImgView),findViewById(R.id.statusTextView));
		    new DownLoadImageTask().execute(mSDBaseURL+"/"+mSDMaskedImgFname,findViewById(R.id.maskedImgView),findViewById(R.id.statusTextView));
		    new DownLoadImageTask().execute(mSDBaseURL+"/"+mSDChartImgFname,findViewById(R.id.chartImgView),findViewById(R.id.statusTextView));
		    //new DownLoadImageTask().execute("http://192.168.1.24/tmpfs/auto.jpg",findViewById(R.id.webCamView),findViewById(R.id.statusTextView));
		    new DownLoadSeizureDataTask().execute(mSDBaseURL+"/"+mSDDataFname,findViewById(R.id.sdOutput),findViewById(R.id.statusTextView));
		}
	    
		@Override
		public void onFinish() {
		    // don't care timer should never "finish" since Long.MAXVALUE is 200 years in the future
		}
	    };
	Log.v("startTime","mTimer = "+mTimer);
	mTimer.start();
    }


    private void setBeeps(int status) {
	ToneGenerator toneG;
	if ( mAudibleAlarm ) {
	    switch (status) {
	    case 0: // ok
		break;
	    case 1: // warning
		toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);      	
		// 200 is duration in ms
		toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 100);
		break;
	    case 2: // alarm
		toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
		toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
		break;
	    case 3: // ben not found
		break;
	    }
	}
	else {
	    Log.v("setBeeps","Quiet Alarm");
	}
    }


    public void resetBackground() {
	/** Reset the seizure detector background image */
	Log.v("resetBackground","resetBackground");
	new ResetBackgroundTask().execute(mSDBaseURL+"/"+mSDResetURL,findViewById(R.id.statusTextView));
    }

    public class ResetBackgroundTask extends AsyncTask<Object, Void, String> {
	/** The system calls this to perform work in a worker thread and
	 * delivers it the parameters given to AsyncTask.execute() */
	TextView statusTextView;
	protected String doInBackground(Object... params) {
	    statusTextView = (TextView)params[1];
	    return loadSeizureDataFromNetwork((String)params[0]);
	}
    
	/** The system calls this to perform work in the UI thread and delivers
	 * the result from doInBackground() */
	protected void onPostExecute(String result) {
	    if (result!=null) {
		statusTextView.setText("background reset");
	    } else {
		//Toast.makeText(getApplicationContext(), 
		//	       "Failed to download image", 
		//	       Toast.LENGTH_SHORT).show();                
		statusTextView.setText("Failed to reset background");
	    }
	}
    }
 



    public class DownLoadSeizureDataTask extends AsyncTask<Object, Void, String> {
	/** The system calls this to perform work in a worker thread and
	 * delivers it the parameters given to AsyncTask.execute() */
	TextView targetTextView, statusTextView;
	protected String doInBackground(Object... params) {
	    targetTextView = (TextView)params[1];
	    statusTextView = (TextView)params[2];
	    return loadSeizureDataFromNetwork((String)params[0]);
	}
    
	/** The system calls this to perform work in the UI thread and delivers
	 * the result from doInBackground() */
	protected void onPostExecute(String result) {
	    if (result!=null) {
		Log.v("DownLoadSeizureData",result);
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
			setBeeps(0);
			break;
		    case 1:  // warning
			bgColour = 0xffaaaa00;
			setBeeps(1);
			break;
		    case 2:  // alarm
			bgColour = 0xffff0000;
			setBeeps(2);
			break;
		    case 3:  // not found
			bgColour = 0xffaaaaaa;
			setBeeps(3);
			break;
		    default:
			bgColour = 0xff000000;
		    }
		    targetTextView.setBackgroundColor(bgColour);
		    Calendar c = Calendar.getInstance();
		    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		    String formattedDate = df.format(c.getTime());
		    statusTextView.setText("Updated at "+formattedDate);
		}
		catch(JSONException ex) {
		    ex.printStackTrace();
		    targetTextView.setText("Failed to Parse Seizure Data");
		    statusTextView.setText("Failed to Parse Seizure Data");
		}
	    } else {
		//Toast.makeText(getApplicationContext(), 
		//	       "Failed to download seizure data", 
		//	       Toast.LENGTH_SHORT).show();                
		targetTextView.setText("Failed to download seizure data");
		statusTextView.setText("Failed to download seizure data");
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
	    Log.v("loadSeizureDataFromNetwork","ERROR");
	    return null;
	}
    }   




    public class DownLoadImageTask extends AsyncTask<Object, Void, Bitmap> {
	/** The system calls this to perform work in a worker thread and
	 * delivers it the parameters given to AsyncTask.execute() */
	ImageView targetImgView;
	TextView statusTextView;
	protected Bitmap doInBackground(Object... params) {
	    targetImgView = (ImageView)params[1];
	    statusTextView = (TextView)params[2];
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
		statusTextView.setText("");
	    } else {
		//Toast.makeText(getApplicationContext(), 
		//	       "Failed to download image", 
		//	       Toast.LENGTH_SHORT).show();                
		statusTextView.setText("Failed to download image");
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
