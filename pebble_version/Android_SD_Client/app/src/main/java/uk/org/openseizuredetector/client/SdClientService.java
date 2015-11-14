/*
  Pebble_sd - a simple accelerometer based seizure detector that runs on a
  Pebble smart watch (http://getpebble.com).

  See http://openseizuredetector.org for more information.

  Copyright Graham Jones, 2015.

  This file is part of pebble_sd.

  Pebble_sd is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  Pebble_sd is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with pebble_sd.  If not, see <http://www.gnu.org/licenses/>.

*/


package uk.org.openseizuredetector.client;

import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.util.StringTokenizer;
import java.net.HttpURLConnection;
import java.net.URL;
import android.net.Uri;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;
import android.text.format.Time;
import org.json.JSONObject;
import org.json.JSONArray;

import uk.org.openseizuredetector.SdData;


/**
 * Based on 
 * openseizure detector server SdServer.java
 */
public class SdClientService extends Service
{
    // Notification ID
    private int NOTIFICATION_ID = 1;

    private NotificationManager mNM;

    private final static String TAG = "SdClientService";
    private Looper mServiceLooper;
    private Timer statusTimer = null;
    private int mCancelAudiblePeriod = 10;  // Cancel Audible Period in minutes
    private int mFaultTimerPeriod = 30;  // Cancel Audible Period in sec
    private long mCancelAudibleTimeRemaining = 0;
    private CountDownTimer mCancelAudibleTimer = null;
    private boolean mCancelAudible = false;
    private FaultTimer mFaultTimer = null;
    private boolean mFaultTimerCompleted = false;
    private HandlerThread thread;
    private WakeLock mWakeLock = null;
    public SdData mSdData;
    public boolean mAudibleFaultWarning = true;
    public boolean mAudibleAlarm = true;
    public boolean mAudibleWarning = true;
    public String mServerIP = "192.168.1.175";
    private int mDataUpdatePeriod = 2000;
    private Time mStatusTime = null;
    //private boolean mStatusOK = false;
    private final IBinder mBinder = new SdBinder();

    /**
     * class to handle binding the MainApp activity to this service
     * so it can access sdData.
     */
    public class SdBinder extends Binder {
	SdClientService getService() {
	    return SdClientService.this;
	}
    }

    /**
     * Constructor for SdClientService class - does not do much!
     */
    public SdClientService() {
	super();
	mSdData = new SdData();
	Log.v(TAG,"SdClientService Created");
    }


    @Override
    public IBinder onBind(Intent intent) {
	Log.v(TAG,"onBind()");
	return mBinder;
    }



    /**
     * onCreate() - called when services is created.  Starts message
     * handler process to listen for messages from other processes.
     */
    @Override
    public void onCreate() {
	Log.v(TAG,"onCreate()");

	// FIXME - do we really need this for the client???
	// Create a wake lock, but don't use it until the service is started.
	PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
	mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        "MyWakelockTag");
    }

    /**
     * onStartCommand - start regular download of status data from the server.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.v(TAG,"SdClientService service starting");
	
	// Update preferences.
	Log.v(TAG,"calling updatePrefs()");
	updatePrefs();
	
	// Display a notification icon in the status bar of the phone to
	// show the service is running.
	Log.v(TAG,"showing Notification");
	showNotification();

	// Start timer to check status of pebble regularly.
	mStatusTime = new Time(Time.getCurrentTimezone());
	mStatusTime.setToNow();
	if (statusTimer==null) {
	    Log.v(TAG,"onCreate(): starting status timer");
	    statusTimer = new Timer();
	    statusTimer.schedule(new TimerTask() {
		    @Override
		    public void run() {getSdData();}
		}, 0, mDataUpdatePeriod);	
	} else {
	    Log.v(TAG,"onCreate(): status timer already running.");
	}
	
	// Apply the wake-lock to prevent CPU sleeping (very battery intensive!)
	if (mWakeLock!=null) {
	    mWakeLock.acquire();
	    Log.v(TAG,"Applied Wake Lock to prevent device sleeping");
	} else {
	    Log.d(TAG,"mmm...mWakeLock is null, so not aquiring lock.  This shouldn't happen!");
	}

	return START_STICKY;
    }

    @Override
    public void onDestroy() {
	Log.v(TAG,"onDestroy(): Service stopping");
	// release the wake lock to allow CPU to sleep and reduce
	// battery drain.
	if (mWakeLock!=null) {
	    mWakeLock.release();
	    Log.v(TAG,"Released Wake Lock to allow device to sleep.");
	} else {
	    Log.d(TAG,"mmm...mWakeLock is null, so not releasing lock.  This shouldn't happen!");
	}

	try {
	    // Stop the status timer
	    if (statusTimer!=null) {
		Log.v(TAG,"onDestroy(): cancelling status timer");
		statusTimer.cancel();
		statusTimer.purge();
		statusTimer = null;
	    }

	    // Cancel the notification.
	    Log.v(TAG,"onDestroy(): cancelling notification");
	    mNM.cancel(NOTIFICATION_ID);

	    // stop this service.
	    Log.v(TAG,"onDestroy(): calling stopSelf()");
	    stopSelf();

	} catch(Exception e) {
	    Log.v(TAG,"Error in onDestroy() - "+e.toString());
	}
    }



    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
	Log.v(TAG,"showNotification()");
        CharSequence text = "Alarm Client for OpenSeizureDetector Running";
        Notification notification = 
	   new Notification(R.drawable.star_of_life_24x24, text,
			     System.currentTimeMillis());
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ClientActivity.class), 0);
        notification.setLatestEventInfo(this, "Alarm Client for OpenSeizureDetector",
                      text, contentIntent);
	notification.flags |= Notification.FLAG_NO_CLEAR;
        mNM = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(NOTIFICATION_ID, notification);
    }


    /*
     * Inhibit fault alarm initiation for a period to avoid spurious warning 
     * beeps caused by short term network interruptions.
     */
    private class FaultTimer extends CountDownTimer {
	public long mFaultTimerRemaining = 0;
	public FaultTimer(long startTime, long interval) {
	    super(startTime, interval);
	}	
	@Override
	public void onFinish() {
	    mFaultTimerCompleted = true;
	    Log.v(TAG,"mFaultTimer - removing mFaultTimerRunning flag");
	}
	@Override
	public void onTick(long msRemaining) {
	    mFaultTimerRemaining = msRemaining/1000;
	    Log.v(TAG,"mFaultTimer - onTick() - Time Remaining = "
		  + mFaultTimerRemaining);
	}
	
    }


    /*
     * Temporary cancel audible alarms, for the period specified by the
     * CancelAudiblePeriod setting.
     */
    private class CancelAudibleTimer extends CountDownTimer {
	public CancelAudibleTimer(long startTime, long interval) {
	    super(startTime, interval);
	}	
	@Override
	public void onFinish() {
	    mCancelAudible = false;
	    Log.v(TAG,"mCancelAudibleTimer - removing cancelAudible flag");
	}
	@Override
	public void onTick(long msRemaining) {
	    mCancelAudibleTimeRemaining = msRemaining/1000;
	    Log.v(TAG,"mCancelAudibleTimer - onTick() - Time Remaining = "
		  + mCancelAudibleTimeRemaining);
	}
	
    }

    /** 
     * Start the fault timer that is used to require a fault to remain
     * standing for a period before raising fault beeps.
     */
    public void startFaultTimer() {
	if (mFaultTimer!=null) {
	    Log.v(TAG,"startFaultTimer(): fault timer already running - not doing anything.");
	} else {
	    Log.v(TAG,"startFaultTimer(): starting fault timer.");
	    mFaultTimerCompleted = false;
	    mFaultTimer = 
		// conver to ms.
		new FaultTimer(mFaultTimerPeriod*1000,1000);
	    mFaultTimer.start();
	}
    }

    public void stopFaultTimer() {
	if (mFaultTimer!=null) {
	    Log.v(TAG,"stopFaultTimer(): fault timer already running - cancelling it.");
	    mFaultTimer.cancel();
	    mFaultTimer = null;
	    mFaultTimerCompleted = false;
	} else {
	    Log.v(TAG,"stopFaultTimer(): fault timer not running - not doing anything.");
	}
    }

    
    public void cancelAudible() {
	// Start timer to remove the cancel audible flag
	// after the required period.
	if (mCancelAudibleTimer!=null) {
	    Log.v(TAG,"onCreate(): cancel audible timer already running - cancelling it.");
	    mCancelAudibleTimer.cancel();
	    mCancelAudibleTimer = null;
	    mCancelAudible = false;
	} else {
	    Log.v(TAG,"cancelAudible(): starting cancel audible timer");
	    mCancelAudible = true;
	    mCancelAudibleTimer = 
		// conver to ms.
		new CancelAudibleTimer(mCancelAudiblePeriod*60*1000,1000);
	    mCancelAudibleTimer.start();
	}
    }

    public boolean isAudibleCancelled() {
	return mCancelAudible;
    }

    public long cancelAudibleTimeRemaining() {
	return mCancelAudibleTimeRemaining;
    }

    /* from http://stackoverflow.com/questions/12154940/how-to-make-a-beep-in-android */
    /**
     * beep for duration miliseconds.
     */
    private void beep(int duration) {
	ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
	toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, duration); 
	Log.v(TAG,"beep()");
    }

    /*
     * beep, provided mAudibleAlarm is set
     */
    public void faultWarningBeep() {
	if (mFaultTimerCompleted) {
	    if (mCancelAudible) {
		Log.v(TAG,"faultWarningBeep() - CancelAudible Active - silent beep...");
	    } else {
		if (mAudibleFaultWarning) {
		    beep(10);
		    Log.v(TAG,"faultWarningBeep()");
		} else {
		    Log.v(TAG,"faultWarningBeep() - silent...");
		}
	    }
	} else {
	    startFaultTimer();
	    Log.v(TAG,"faultWarningBeep() - starting Fault Timer");
	}
    }



    /*
     * beep, provided mAudibleAlarm is set
     */
    public void alarmBeep() {
	if (mCancelAudible) {
	    Log.v(TAG,"alarmBeep() - CancelAudible Active - silent beep...");
	} else {
	    if (mAudibleAlarm) {
		beep(1000);
		Log.v(TAG,"alarmBeep()");
	    } else {
		Log.v(TAG,"alarmBeep() - silent...");
	    }
	}
    }

    /*
     * beep, provided mAudibleWarning is set
     */
    public void warningBeep() {
	if (mCancelAudible) {
	    Log.v(TAG,"warningBeep() - CancelAudible Active - silent beep...");
	} else {
	    if (mAudibleWarning) {
		beep(100);
		Log.v(TAG,"warningBeep()");
	    } else {
		Log.v(TAG,"warningBeep() - silent...");
	    }
	}
    }


    /**
     * updatePrefs() - update basic settings from the SharedPreferences
     * - defined in res/xml/prefs.xml
     */
    public void updatePrefs() {
	Log.v(TAG,"updatePrefs()");
	SharedPreferences SP = PreferenceManager
	    .getDefaultSharedPreferences(getBaseContext());
	mAudibleFaultWarning = SP.getBoolean("AudibleFaultWarning",true);
	Log.v(TAG,"updatePrefs() - mAudibleFaultWarning = "+mAudibleFaultWarning);
	mAudibleAlarm = SP.getBoolean("AudibleAlarm",true);
	Log.v(TAG,"updatePrefs() - mAudibleAlarm = "+mAudibleAlarm);
	mAudibleWarning = SP.getBoolean("AudibleWarning",true);
	Log.v(TAG,"updatePrefs() - mAudibleWarning = "+mAudibleWarning);
	mServerIP = SP.getString("ServerIP","192.168.1.175");
	Log.v(TAG,"updatePrefs() - mServerIP = "+mServerIP);
	try {
	    String dataUpdatePeriodStr = SP.getString("DataUpdatePeriod","2000");
	    mDataUpdatePeriod = Integer.parseInt(dataUpdatePeriodStr);
	    Log.v(TAG,"updatePrefs() - mDataUpdatePeriod = "+mDataUpdatePeriod);
	} catch (Exception ex) {
	    Log.v(TAG,"onStart() - Problem parsing preferences!");
	    Toast toast = Toast.makeText(getApplicationContext(),"Problem Parsing Preferences - Something won't work",Toast.LENGTH_SHORT);
	    toast.show();
	}

	// Parse the CancelAudible period setting.
	try {
	    String cancelAudiblePeriodStr = SP.getString("CancelAudiblePeriod","10");
	    mCancelAudiblePeriod = Integer.parseInt(cancelAudiblePeriodStr);
	    Log.v(TAG,"onStart() - mCancelAudiblePeriod = "+mCancelAudiblePeriod);
	} catch (Exception ex) {
	    Log.v(TAG,"onStart() - Problem cancelAudiblePeriod preference!");
	    Toast toast = Toast.makeText(getApplicationContext(),"Problem Parsing CancelAudiblePeriod Preference",Toast.LENGTH_SHORT);
	    toast.show();
	}

	// Parse the faultTimer period setting.
	try {
	    String faultTimerPeriodStr = SP.getString("FaultTimerPeriod","30");
	    mFaultTimerPeriod = Integer.parseInt(faultTimerPeriodStr);
	    Log.v(TAG,"onStart() - mFaultTimerPeriod = "+mFaultTimerPeriod);
	} catch (Exception ex) {
	    Log.v(TAG,"onStart() - Problem with FaultTimerPeriod preference!");
	    Toast toast = Toast.makeText(getApplicationContext(),"Problem Parsing FaultTimerPeriod Preference",Toast.LENGTH_SHORT);
	    toast.show();
	}

    }

    /**
     * Check the seizure detector data for alarm conditions and beep and
     * display the ClientActivity if an alarm condition is shown.
     */
    private void checkAlarms() {
	Log.v(TAG,"checkAlarms()");
	    // Check the alarm state, and raise alarms if necessary.
	    if (mSdData.alarmState==0) {
		Log.v(TAG,"Status=OK");
	    }
	    if (mSdData.alarmState==1) {
		Log.v(TAG,"Status=WARNING");
		warningBeep();
	    }
	    if (mSdData.alarmState==2) {
		Log.v(TAG,"Status=ALARM");
		alarmBeep();
		Intent clientActivityIntent = 
		    new Intent(this, ClientActivity.class);
		clientActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(clientActivityIntent);
	    }
	    if ((mSdData.pebbleConnected == false) || 
		(mSdData.pebbleAppRunning == false)) {
		faultWarningBeep();
	    } else {
		stopFaultTimer();
	    }
    }



    /**
     * Retrive the current Seizure Detector Data from the server.
     * Uses teh DownloadSdDataTask class to download the data in the
     * background.  The data is processed in DownloadSdDataTask.onPostExecute().
     */
    private void getSdData() {
	Log.v(TAG,"getSdData()");
	ConnectivityManager connMgr = (ConnectivityManager) 
            getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        //if (networkInfo != null && networkInfo.isConnected()) {
	if (true) {
            new DownloadSdDataTask().execute("http://"+mServerIP+":8080/data");
        } else {
            Log.v(TAG,"No network connection available.");
        }    }
    
    private class DownloadSdDataTask extends AsyncTask<String, Void, String> {
        @Override
	    protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
	    protected void onPostExecute(String result) {
	    //Log.v(TAG,"onPostExecute() - result="+result);
	    if (result.startsWith("Unable to retrieve web page")) {
		    Log.v(TAG,"onPostExecute - Unable to retrieve data");
		    mSdData.serverOK = false;
		    mSdData.pebbleConnected = false;
		    mSdData.pebbleAppRunning = false;
		    mSdData.alarmState = 0;
		    mSdData.alarmPhrase = "Warning - No Connection to Server";
		    //faultWarningBeep();
		} else {
		    // Populate mSdData using the received data.
		    mSdData.serverOK = true;
		    mStatusTime.setToNow();
		    mSdData.fromJSON(result);
		    Log.v(TAG,"onPostExecute(): mSdData = "+mSdData.toString());
		}
	    checkAlarms();
	}
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl) throws IOException {
	InputStream is = null;
	// Only display the first 500 characters of the retrieved
	// web page content.
	int len = 500;
        
	try {
	    URL url = new URL(myurl);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setReadTimeout(5000 /* milliseconds */);
	    conn.setConnectTimeout(5000 /* milliseconds */);
	    conn.setRequestMethod("GET");
	    conn.setDoInput(true);
	    // Starts the query
	    conn.connect();
	    int response = conn.getResponseCode();
	    Log.d(TAG, "The response is: " + response);
	    is = conn.getInputStream();
	    
	    // Convert the InputStream into a string
	    String contentAsString = readInputStream(is, len);
	    return contentAsString;
	    
	    // Makes sure that the InputStream is closed after the app is
	    // finished using it.
	} finally {
	    if (is != null) {
		is.close();
	    } 
	}
    }
    
    // Reads an InputStream and converts it to a String.
    public String readInputStream(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
	Reader reader = null;
	reader = new InputStreamReader(stream, "UTF-8");        
	char[] buffer = new char[len];
	reader.read(buffer);
	return new String(buffer);
    }




}
