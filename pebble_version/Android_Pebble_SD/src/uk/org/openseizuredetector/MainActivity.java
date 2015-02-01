package uk.org.openseizuredetector;

import android.app.Activity;
import android.app.IntentService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends Activity
{
    static final String TAG = "MainActivity";

    private Intent sdServerIntent;

    final Handler serverStatusHandler = new Handler();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

	// Initialise the User Interface
        setContentView(R.layout.main);
	Button button = (Button)findViewById(R.id.button1);
	button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
		 Log.v(TAG,"onClick(): "+v.toString());
		 // request settings from pebble
	     }
	    }
	    );

	button = (Button)findViewById(R.id.button2);
	button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
		 Log.v(TAG,"Starting Web Server");
		 sdServerIntent = new Intent(MainActivity.this,SdServer.class);
		 sdServerIntent.setData(Uri.parse("Start"));
		 getApplicationContext().startService(sdServerIntent);
	     
             }
	    });
	

	Timer uiTimer = new Timer();
	uiTimer.schedule(new TimerTask() {
		@Override
		public void run() {updateServerStatus();}
	    }, 0, 1000);	

	onResume();
    }

    /**
     * Based on http://stackoverflow.com/questions/7440473/android-how-to-check-if-the-intent-service-is-still-running-or-has-stopped-running
     */
    public boolean isServerRunning() {
	//Log.v(TAG,"isServerRunning()................");
	ActivityManager manager = 
	    (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	for (RunningServiceInfo service : 
		 manager.getRunningServices(Integer.MAX_VALUE)) {
	    //Log.v(TAG,"Service: "+service.service.getClassName());
	    if ("uk.org.openseizuredetector.SdServer"
		.equals(service.service.getClassName())) {
		//Log.v(TAG,"Yes!");
            return true;
	    }
	}
	//Log.v(TAG,"No!");
	return false;
    }


    /*
     * updateServerStatus - called by the uiTimer timer periodically.
     * requests the ui to be updated by calling serverStatusRunnable.
     */
    private void updateServerStatus() {
	serverStatusHandler.post(serverStatusRunnable);
    }
	
    /*
     * serverStatusRunnable - called by updateServerStatus
     */
    final Runnable serverStatusRunnable = new Runnable() {
	    public void run() {
		String serverText = "*** Server Stopped ***";
		if (isServerRunning()) {
		    serverText = "Server Running OK";
		}
		TextView serverTextView = 
		    (TextView) findViewById(R.id.textView3);
		serverTextView.setText(serverText);	    
	    }
	};
    

    @Override
    protected void onPause() {
	super.onPause();
    }
    @Override
    protected void onResume() {
	super.onResume();
    }

    public void updateUi() {
	String statusPhrase;
	String alarmPhrase;
	String viewText = "Unknown";
    }



}
