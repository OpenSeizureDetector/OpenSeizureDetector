package uk.org.openseizuredetector;

import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;

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


    // Values of the KEY_DATA_TYPE entry in a message
    private int DATA_TYPE_RESULTS = 1;   // Analysis Results
    private int DATA_TYPE_SETTINGS = 2;  // Settings
    private int DATA_TYPE_SPEC = 3;      // FFT Spectrum (or part of a spectrum)

    private WebServer webServer;
    private final static String TAG = "SdServer";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private SdData sdData;
    private short fftResults[];
    private PebbleKit.PebbleDataReceiver msgDataHandler = null;

    private long alarmState;
    private long maxVal;
    private long maxFreq;
    private long specPower;
    private String alarmPhrase;
    

    public SdServer() {
	super();
	//sdData = new SdData();
	Log.v(TAG,"SdServer Created");
    }

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

    @Override
    public void onCreate() {
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
	// Allocate memory for the FFT spectrum results
	fftResults = new short[NSAMP/2];
	Message msg = mServiceHandler.obtainMessage();
	msg.arg1 = startId;
	mServiceHandler.sendMessage(msg);
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
			alarmPhrase = "Unknown";
			if (alarmState==0) alarmPhrase="OK";
			if (alarmState==1) alarmPhrase="WARNING";
			if (alarmState==2) alarmPhrase="ALARM";
		    }
		    if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)
			==DATA_TYPE_SETTINGS) {
			Log.v(TAG,"DATA_TYPE = Settings");
			Log.v(TAG,"Settings:\n"+data.toJsonString());
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

    public void getPebbleSdSettings() {
	PebbleDictionary data = new PebbleDictionary();
	data.addUint8(KEY_SETTINGS, (byte)1);
	data.addString(1, "A string"); 
	PebbleKit.sendDataToPebble(
				   getApplicationContext(), 
				   SD_UUID, 
				   data);     
    }

    public void startWatchApp() {
	PebbleKit.startAppOnPebble(getApplicationContext(),
				   SD_UUID);

    }

    public void stopWatchApp() {
	PebbleKit.closeAppOnPebble(getApplicationContext(),
				   SD_UUID);
    }



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

    private class WebServer extends NanoHTTPD {
        public WebServer()
        {
            super(8080);
        }
        @Override
        public Response serve(String uri, Method method, 
                              Map<String, String> header,
                              Map<String, String> parameters,
                              Map<String, String> files) {
            String answer = "SdServer Response\n"
		+alarmPhrase+"\n"
		+"maxFreq = "+maxFreq;
            return new NanoHTTPD.Response(answer);
        }
    }

}
