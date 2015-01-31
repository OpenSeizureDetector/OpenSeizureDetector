package uk.org.openseizuredetector;

import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.app.IntentService;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import java.io.*;
import java.util.*;

/**
 * Based on example at:
 * http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android
 */
public class SdServer extends IntentService
{
    private WebServer server;
    private final static String TAG = "SdServer";

    public SdServer() {
	super("SdServer Constructor");
	Log.v(TAG,"SdServer Created");
    }


    protected void onHandleIntent(Intent workIntent) {
	String dataString = workIntent.getDataString();
	Log.v(TAG,"onHandleIntent - datastring="+dataString);
        server = new WebServer();
        try {
            server.start();
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
