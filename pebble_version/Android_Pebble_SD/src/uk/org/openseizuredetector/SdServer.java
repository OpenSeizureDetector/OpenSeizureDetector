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


package uk.org.openseizuredetector;

import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.URL;
import android.net.Uri;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import android.text.format.Time;
import org.json.JSONObject;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


/**
 * Based on example at:
 * http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android
 * and 
 * http://developer.android.com/guide/components/services.html#ExtendingService
 */
public class SdServer extends Service
{
    private UUID SD_UUID = UUID.fromString("03930f26-377a-4a3d-aa3e-f3b19e421c9d");
    private int NSAMP = 512;   // Number of samples in fft input dataset.

    private int KEY_DATA_TYPE = 1;
    private int KEY_ALARMSTATE = 2;
    private int KEY_MAXVAL = 3;
    private int KEY_MAXFREQ = 4;
    private int KEY_SPECPOWER = 5;
    private int KEY_SETTINGS = 6;
    private int KEY_ALARM_FREQ_MIN =7;
    private int KEY_ALARM_FREQ_MAX =8;
    private int KEY_WARN_TIME = 9;
    private int KEY_ALARM_TIME = 10;
    private int KEY_ALARM_THRESH = 11;
    private int KEY_POS_MIN = 12;       // position of first data point in array
    private int KEY_POS_MAX = 13;       // position of last data point in array.
    private int KEY_SPEC_DATA = 14;     // Spectrum data
    private int KEY_ROIPOWER = 15;     
    private int KEY_NMIN = 16;
    private int KEY_NMAX = 17;
    private int KEY_ALARM_RATIO_THRESH = 18;
    private int KEY_BATTERY_PC = 19;

    // Values of the KEY_DATA_TYPE entry in a message
    private int DATA_TYPE_RESULTS = 1;   // Analysis Results
    private int DATA_TYPE_SETTINGS = 2;  // Settings
    private int DATA_TYPE_SPEC = 3;      // FFT Spectrum (or part of a spectrum)

    private NotificationManager mNM;

    private WebServer webServer;
    private final static String TAG = "SdServer";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private boolean mPebbleConnected = false;
    private boolean mPebbleAppRunning = false;
    private boolean mPebbleAppRunningCheck = false;
    private Time mPebbleStatusTime;
    private SdData sdData;
    private short fftResults[];
    private PebbleKit.PebbleDataReceiver msgDataHandler = null;

    /* Analysis results */
    private long alarmState;
    private long maxVal;
    private long maxFreq;
    private long specPower;
    private long roiPower;
    private String alarmPhrase;

    /* Analysis settings */
    private long alarmFreqMin;
    private long alarmFreqMax;
    private long nMin;
    private long nMax;
    private long warnTime;
    private long alarmTime;
    private long alarmThresh;
    private long alarmRatioThresh;
    private long batteryPc;

    /**
     * Constructor for SdServer class - does not do much!
     */
    public SdServer() {
	super();
	//sdData = new SdData();
	Log.v(TAG,"SdServer Created");
    }

    /**
     * Handles messages sent from other processes.
     * FIXME - this does not actually do anthing!
     */
    private final class ServiceHandler extends Handler {
	public ServiceHandler(Looper looper) {
	    super(looper);
	}
	@Override
	public void handleMessage(Message msg) {
	    Log.v(TAG,"SdServer handleMessage() - "+msg.toString());
	    // FIXME - don't know why we stop it here???
	    //stopSelf(msg.arg1);
	}
    }


    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
	Log.v(TAG,"showNotification()");
        CharSequence text = "OpenSeizureDetector Service Running";
        Notification notification = 
	   new Notification(R.drawable.stat_sample, text,
			     System.currentTimeMillis());
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        notification.setLatestEventInfo(this, "OpenSeizureDetector",
                      text, contentIntent);
        mNM = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mNM.notify(1, notification);
    }



    /**
     * onCreate() - called when services is created.  Starts message
     * handler process to listen for messages from other processes.
     */
    @Override
    public void onCreate() {
	Log.v(TAG,"onCreate()");
	showNotification();

	HandlerThread thread = new HandlerThread("ServiceStartArguments",
						 Process.THREAD_PRIORITY_BACKGROUND);
	thread.start();
	mServiceLooper = thread.getLooper();
	mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    /**
     * onStartCommand - start the web server and the message loop for
     * communications with other processes.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.v(TAG,"SdServer service starting");
	mPebbleStatusTime = new Time(Time.getCurrentTimezone());
	// Allocate memory for the FFT spectrum results
	fftResults = new short[NSAMP/2];
	Message msg = mServiceHandler.obtainMessage();
	msg.arg1 = startId;
	mServiceHandler.sendMessage(msg);

	// Start timer to check status of pebble regularly.
	getPebbleStatus();
	Timer statusTimer = new Timer();
	statusTimer.schedule(new TimerTask() {
		@Override
		public void run() {getPebbleStatus();}
	    }, 0, 1000);	

	// Start timer to retrieve pebble settings regularly.
	getPebbleSdSettings();
	Timer settingsTimer = new Timer();
	settingsTimer.schedule(new TimerTask() {
		@Override
		public void run() {getPebbleSdSettings();}
	    }, 0, 1000*60);	

	startPebbleServer();
	startWebServer();
	return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }

    @Override
    public void onDestroy() {
	Log.v(TAG,"SdServer Service stopping");
	if (msgDataHandler != null) {
	    unregisterReceiver(msgDataHandler);
	    msgDataHandler = null;
	}
    }

    private void startPebbleServer() {
	Log.v(TAG,"StartPebbleServer()");
	final Handler handler = new Handler();
	msgDataHandler = new PebbleKit.PebbleDataReceiver(SD_UUID) {
		@Override
		public void receiveData(final Context context,
					final int transactionId,
					final PebbleDictionary data) {
		    Log.v(TAG,"Received message from Pebble");
		    // If we have a message, the app must be running
		    mPebbleAppRunningCheck = true;  
		    PebbleKit.sendAckToPebble(context,transactionId);
		    Log.v(TAG,"Acknowledged Message");
		    Log.v(TAG,"Message is: "+data.toJsonString());
		    if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)
			==DATA_TYPE_RESULTS) {
			Log.v(TAG,"DATA_TYPE = Results");
			Log.v(TAG,"Results Received from Pebble");
			alarmState = data.getUnsignedIntegerAsLong(
								   KEY_ALARMSTATE);
			maxVal = data.getUnsignedIntegerAsLong(KEY_MAXVAL);
			maxFreq = data.getUnsignedIntegerAsLong(KEY_MAXFREQ);
			specPower = data.getUnsignedIntegerAsLong(KEY_SPECPOWER);
			roiPower = data.getUnsignedIntegerAsLong(KEY_ROIPOWER);
			alarmPhrase = "Unknown";
			if (alarmState==0) alarmPhrase="OK";
			if (alarmState==1) alarmPhrase="WARNING";
			if (alarmState==2) alarmPhrase="ALARM";
		    }
		    if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)
			==DATA_TYPE_SETTINGS) {
			Log.v(TAG,"DATA_TYPE = Settings");
			alarmFreqMin = data.getUnsignedIntegerAsLong(KEY_ALARM_FREQ_MIN);
			alarmFreqMax = data.getUnsignedIntegerAsLong(KEY_ALARM_FREQ_MAX);
			nMin = data.getUnsignedIntegerAsLong(KEY_NMIN);
			nMax = data.getUnsignedIntegerAsLong(KEY_NMAX);
			warnTime = data.getUnsignedIntegerAsLong(KEY_WARN_TIME);
			alarmTime = data.getUnsignedIntegerAsLong(KEY_ALARM_TIME);
			alarmThresh = data.getUnsignedIntegerAsLong(KEY_ALARM_THRESH);
			alarmRatioThresh = data.getUnsignedIntegerAsLong(KEY_ALARM_RATIO_THRESH);
			batteryPc = data.getUnsignedIntegerAsLong(KEY_BATTERY_PC);
		    }

		    if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)
			== DATA_TYPE_SPEC) {
			Log.v(TAG,"DATA_TYPE = Spectrum");
			/*			int posMin = data.getUnsignedIntegerAsLong(KEY_POS_MIN).intValue();
			int posMax = data.getUnsignedIntegerAsLong(KEY_POS_MAX).intValue();
			// Read the data that has been sent, and convert it into
			// an integer array.
			byte[] byteArr = data.getBytes(KEY_SPEC_DATA);
			ShortBuffer shortBuf = ByteBuffer.wrap(byteArr)
			    .order(ByteOrder.BIG_ENDIAN)
			    .asShortBuffer();
			short[] shortArray = new short[shortBuf.remaining()];
			shortBuf.get(shortArray);
			for (int i=0;i<shortArray.length;i++) {
			    fftResults[posMin+i] = shortArray[i];
			}
			*/
		    }
		}
	    };
	PebbleKit.registerReceivedDataHandler(this,msgDataHandler);
    }

    /** 
     * Checks the status of the connection to the pebble watch,
     * and sets class variables for use by other functions.
     * If the watch app is not running, it attempts to re-start it.
     */
    public void getPebbleStatus() {
	Time tnow = new Time(Time.getCurrentTimezone());
	tnow.setToNow();
	// Check we are actually connected to the pebble.
	mPebbleConnected = PebbleKit.isWatchConnected(this);
	// And is the pebble_sd app running?
	// set mPebbleAppRunningCheck has been false for more than 10 seconds
	// the app is not talking to us
	// mPebbleAppRunningCheck is set to true in the receiveData handler. 
	if (!mPebbleAppRunningCheck && 
	    ((tnow.toMillis(false) - mPebbleStatusTime.toMillis(false)) > 10000)) {
	    mPebbleAppRunning = false;
	    Log.v(TAG,"Pebble App Not Running - Attempting to Re-Start");
	    startWatchApp();
	} else {
	    mPebbleAppRunning = true;
	}

	// if we have confirmation that the app is running, reset the
	// status time to now and initiate another check.
	if (mPebbleAppRunningCheck) {
	    mPebbleAppRunningCheck = false;
	    mPebbleStatusTime.setToNow();
	}
    }

    /**
     * Request Pebble App to send us its latest settings.
     * Will be received as a message by the receiveData handler
     */
    public void getPebbleSdSettings() {
	Log.v(TAG,"getPebbleSdSettings() - requesting settings from pebble");
	PebbleDictionary data = new PebbleDictionary();
	data.addUint8(KEY_SETTINGS, (byte)1);
	PebbleKit.sendDataToPebble(
				   getApplicationContext(), 
				   SD_UUID, 
				   data);     
    }

    /**
     * Attempt to start the pebble_sd watch app on the pebble watch.
     */
    public void startWatchApp() {
	PebbleKit.startAppOnPebble(getApplicationContext(),
				   SD_UUID);

    }

    /**
     * stop the pebble_sd watch app on the pebble watch.
     */
    public void stopWatchApp() {
	PebbleKit.closeAppOnPebble(getApplicationContext(),
				   SD_UUID);
    }


    /**
     * Start the web server (on port 8080)
     */
    protected void startWebServer() {
	Log.v(TAG,"startWebServer()");
        webServer = new WebServer();
        try {
            webServer.start();
        } catch(IOException ioe) {
            Log.w(TAG, "The server could not start.");
	    Log.w(TAG, ioe.toString());
        }
        Log.w(TAG, "Web server initialized.");
    }

    /**
     * Class describing the seizure detector web server - appears on port
     * 8080.
     */
    private class WebServer extends NanoHTTPD {
        public WebServer()
        {
	    // Set the port to listen on (8080)
            super(8080);
        }
        @Override
        public Response serve(String uri, Method method, 
                              Map<String, String> header,
                              Map<String, String> parameters,
                              Map<String, String> files) {
	    Log.v(TAG,"WebServer.serve() - uri="+uri+" Method="+method.toString());
	    String answer = "Error - you should not see this message! - Something wrong in WebServer.serve()";

	    Iterator it = parameters.keySet().iterator();
	    while (it.hasNext()) {
		Object key = it.next();
		Object value = parameters.get(key);
		Log.v(TAG,"Request parameters - key="+key+" value="+value);
	    }

	    if (uri.equals("/")) uri = "/index.html";
	    switch(uri) {
	    case "/data":
		Log.v(TAG,"WebServer.serve() - Returning data");
		try {
		    JSONObject jsonObj = new JSONObject();
		    jsonObj.put("Time",mPebbleStatusTime.format("%H:%M:%S"));
		    jsonObj.put("alarmState",alarmState);
		    jsonObj.put("alarmPhrase",alarmPhrase);
		    jsonObj.put("maxVal",maxVal);
		    jsonObj.put("maxFreq",maxFreq);
		    jsonObj.put("specPower",specPower);
		    jsonObj.put("roiPower",roiPower);
		    jsonObj.put("pebCon",mPebbleConnected);
		    jsonObj.put("pebAppRun",mPebbleAppRunning);
		    answer = jsonObj.toString();
		} catch (Exception ex) {
		    Log.v(TAG,"Error Creating Data Object - "+ex.toString());
		    answer = "Error Creating Data Object";
		}
		break;

	    case "/settings":
		Log.v(TAG,"WebServer.serve() - Returning settings");
		try {
		    JSONObject jsonObj = new JSONObject();
		    jsonObj.put("alarmFreqMin",alarmFreqMin);
		    jsonObj.put("alarmFreqMax",alarmFreqMax);
		    jsonObj.put("nMin",nMin);
		    jsonObj.put("nMax",nMax);
		    jsonObj.put("warnTime",warnTime);
		    jsonObj.put("alarmTime",alarmTime);
		    jsonObj.put("alarmThresh",alarmThresh);
		    jsonObj.put("alarmRatioThresh",alarmRatioThresh);
		    jsonObj.put("batteryPc",batteryPc);
		    answer = jsonObj.toString();
		} catch (Exception ex) {
		    Log.v(TAG,"Error Creating Data Object - "+ex.toString());
		    answer = "Error Creating Data Object";
		}
		break;

	    default:
		if (uri.startsWith("/index.html") ||
		    uri.startsWith("/js/") ||
		    uri.startsWith("/css/") ||
		    uri.startsWith("/img/")) {
		    Log.v(TAG,"Serving File");
		    return serveFile(uri);
		} 
		else {
		    Log.v(TAG,"WebServer.serve() - Unknown uri -"+
			  uri);
		    answer = "Unknown URI: ";
		}
	    }

            return new NanoHTTPD.Response(answer);
        }
    }

    
    void listFiles(String uri) {
	try {
	    Log.v(TAG,"listFiles("+uri+")");
	    AssetManager assetManager = this.getAssets();
	    String[] fileList = assetManager.list(uri);
	    Log.v(TAG,"listFiles("+uri+") - "+fileList.length+" files found");
	    for (int i=0;i<fileList.length;i++) {
		Log.v(TAG,"File "+i+" = "+fileList[i]);
	    }
	} catch (Exception ioe) {
	    Log.v(TAG,"Error Listing Files - Error = "+ioe.toString());
	}
    }

    /**
     * Return a file from the apps /assets folder
     */
    NanoHTTPD.Response serveFile(String uri) {
	NanoHTTPD.Response res;
	InputStream ip = null;
	try {
	    if (ip!=null) ip.close();
	    String assetPath = "www";
	    listFiles(assetPath);
	    String fname = assetPath+uri;
	    Log.v(TAG,"serveFile - uri="+uri+", fname="+fname);
	    AssetManager assetManager = getResources().getAssets();
	    ip = assetManager.open(fname);
	    String mimeStr = "text/html";
	    res = new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK,
					 mimeStr,ip);
	    res.addHeader("Content-Length", "" + ip.available());
	} catch (IOException ex) {
	    Log.v(TAG,"Error Opening File - "+ex.toString());
	    res = new NanoHTTPD.Response("Error Opening file "+uri);
	} 
	return(res);
    }
    
}
