package uk.org.openseizuredetector;

import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
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

/**
 * Based on example at:
 * http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android
 * and 
 * http://developer.android.com/guide/components/services.html#ExtendingService
 */
public class SdServer extends Service
{
    private WebServer webServer;
    private final static String TAG = "SdServer";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private SdData sdData;

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
	Message msg = mServiceHandler.obtainMessage();
	msg.arg1 = startId;
	mServiceHandler.sendMessage(msg);
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
            String answer = "SdServer Response";
            return new NanoHTTPD.Response(answer);
        }
    }

}
