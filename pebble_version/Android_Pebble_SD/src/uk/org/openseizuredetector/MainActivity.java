package uk.org.openseizuredetector;

import android.app.ActionBar;
import android.app.Activity;
import android.app.IntentService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.conn.util.InetAddressUtils;

import uk.org.openseizuredetector.SdServer;

public class MainActivity extends Activity
{
    static final String TAG = "MainActivity";
    private int okColour = Color.BLUE;
    private int warnColour = Color.MAGENTA;
    private int alarmColour = Color.RED;
    SdServer mSdServer;
    boolean mBound = false;

    private Intent sdServerIntent;

    final Handler serverStatusHandler = new Handler();
    Messenger messenger = new Messenger(new ResponseHandler());

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
		    Log.v(TAG,"Stopping Web Server");
		    stopServer();
		}
	    });

	button = (Button)findViewById(R.id.button2);
	button.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"Starting Web Server");
		    startServer();
		}
	    });
	
	/* Force display of overflow menu - from stackoverflow
	 * "how to force use of..."
	 */
	try {
	    ViewConfiguration config = ViewConfiguration.get(this);
	    Field menuKeyField =
		ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	    if (menuKeyField!=null) {
		menuKeyField.setAccessible(true);
		menuKeyField.setBoolean(config,false);
	    }
	} catch (Exception e) {
	    Log.v(TAG,"menubar fiddle exception: "+e.toString());
	}


	Timer uiTimer = new Timer();
	uiTimer.schedule(new TimerTask() {
		@Override
		public void run() {updateServerStatus();}
	    }, 0, 1000);	

	startServer();
    }

    /**
     * Create Action Bar
     */
    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
	getMenuInflater().inflate(R.menu.main_activity_actions,menu);
	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	Log.v(TAG,"Option "+item.getItemId()+" selected");
	switch (item.getItemId()) {
	    //case R.id.home:
	    //Log.v(TAG,"Home");
	    //return true;
	case R.id.action_sync:
	    Log.v(TAG,"action_sync");
	    return true;
	case R.id.action_new:
	    Log.v(TAG,"action_new");
	    return true;
	case R.id.action_3:
	    Log.v(TAG,"action_3");
	    return true;
	case R.id.action_settings:
	    Log.v(TAG,"action_settings");
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    @Override
    protected void onStart() {
	super.onStart();
	startServer();
    }

    @Override
    protected void onStop() {
	super.onStop();
	if (mBound) {
	    unbindService(mConnection);
	    mBound = false;
	}
    }

  /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SdServer.SdBinder binder = (SdServer.SdBinder) service;
            mSdServer = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /**
     * Start the SdServer service
     */
    private void startServer() {
	// Start the server
	sdServerIntent = new Intent(MainActivity.this,SdServer.class);
	sdServerIntent.setData(Uri.parse("Start"));
	getApplicationContext().startService(sdServerIntent);

	// and bind to it so we can see its data
	Intent intent = new Intent(this,SdServer.class);
	bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
	mBound = true;

    }

    /**
     * Stop the SdServer service
     */
    private void stopServer() {
	Log.v(TAG,"stopping Server...");
	// unbind this activity from the service if it is bound.
	if (mBound) {
	    unbindService(mConnection);
	    mBound = false;
	}
	// then send an Intent to stop the service.
	sdServerIntent = new Intent(MainActivity.this,SdServer.class);
	sdServerIntent.setData(Uri.parse("Stop"));
	getApplicationContext().stopService(sdServerIntent);
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


    /** get the ip address of the phone.
     * Based on http://stackoverflow.com/questions/11015912/how-do-i-get-ip-address-in-ipv4-format
     */
    public String getLocalIpAddress() {
	try {
	    for (Enumeration<NetworkInterface> en = NetworkInterface
		     .getNetworkInterfaces(); en.hasMoreElements();) {
		NetworkInterface intf = en.nextElement();
		for (Enumeration<InetAddress> enumIpAddr = intf
			 .getInetAddresses(); enumIpAddr.hasMoreElements();) {
		    InetAddress inetAddress = enumIpAddr.nextElement();
		    //Log.v(TAG,"ip1--:" + inetAddress);
		    //Log.v(TAG,"ip2--:" + inetAddress.getHostAddress());
		
		    // for getting IPV4 format
		    if (!inetAddress.isLoopbackAddress() 
			&& InetAddressUtils.isIPv4Address(
				 inetAddress.getHostAddress())) {
		    
			String ip = inetAddress.getHostAddress().toString();
			//Log.v(TAG,"ip---::" + ip);
			return ip;
		    }
		}
	    }
	} catch (Exception ex) {
	    Log.e("IP Address", ex.toString());
	}
	return null;
    }


    /* from http://stackoverflow.com/questions/12154940/how-to-make-a-beep-in-android */
    private void beep() {
	ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
	toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); 
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
		    (TextView) findViewById(R.id.textView1);
		serverTextView.setText(serverText);	    

		TextView ipTextView = (TextView)findViewById(R.id.textView2);
		ipTextView.setText("Access Server at http://"
				   +getLocalIpAddress()
				   +":8080");

		TextView tv;
		try {
		    if (mBound) {
			tv = (TextView) findViewById(R.id.alarmTv);
			if (mSdServer.sdData.alarmState==0) {
			    tv.setText(mSdServer.sdData.alarmPhrase);
			    tv.setBackgroundColor(okColour);
			}
			if (mSdServer.sdData.alarmState==1) {
			    tv.setText(mSdServer.sdData.alarmPhrase);
			    tv.setBackgroundColor(warnColour);
			}
			if (mSdServer.sdData.alarmState==2) {
			    tv.setText(mSdServer.sdData.alarmPhrase);
			    tv.setBackgroundColor(alarmColour);
			    beep();
			}
			tv = (TextView) findViewById(R.id.pebTimeTv);
			tv.setText(mSdServer.mPebbleStatusTime.format("%H:%M:%S"));
			// Pebble Connected Phrase
			tv = (TextView) findViewById(R.id.pebbleTv);
			if (mSdServer.mPebbleConnected) {
			    tv.setText("Pebble Watch Connected OK");	    
			    tv.setBackgroundColor(okColour);
			} else {
			    tv.setText("** Pebble Watch NOT Connected **");	    
			    tv.setBackgroundColor(alarmColour);
			}
			tv = (TextView) findViewById(R.id.appTv);
			if (mSdServer.mPebbleAppRunning) {
			    tv.setText("Pebble App OK");	    
			    tv.setBackgroundColor(okColour);
			} else {
			    tv.setText("** Pebble App NOT Running **");	    
			}
			tv = (TextView) findViewById(R.id.battTv);
			tv.setText("Pebble Battery = "+String.valueOf(mSdServer.sdData.batteryPc)+"%");
			if (mSdServer.sdData.batteryPc<=20)
			    tv.setBackgroundColor(alarmColour);
			if (mSdServer.sdData.batteryPc>20)
			    tv.setBackgroundColor(warnColour);
			if (mSdServer.sdData.batteryPc>=40)
			    tv.setBackgroundColor(okColour);
    
			tv = (TextView) findViewById(R.id.debugTv);
			String specStr = "";
			for (int i=0;i<10;i++)
			    specStr = specStr + mSdServer.sdData.simpleSpec[i] + ", ";
			tv.setText("Spec = "+specStr);
		    }
		    else {
			tv = (TextView) findViewById(R.id.alarmTv);
			tv.setText("Not Bound to Server");
		    }
		} catch (Exception e) {
		    Log.v(TAG,"ServerStatusRunnable: Exception - "+e.toString());
		}

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

    class ResponseHandler extends Handler {
	@Override public void handleMessage(Message message) {
	    Log.v(TAG,"Message="+message.toString());
	}
    }

}
