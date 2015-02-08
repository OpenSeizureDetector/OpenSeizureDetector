package uk.org.openseizuredetector;

import android.app.Activity;
import android.app.IntentService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
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
	    }
	    );

	button = (Button)findViewById(R.id.button2);
	button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
		 Log.v(TAG,"Starting Web Server");
		 startServer();
             }
	    });
	

	// send message to server
	//Message message = Message.obtain(null,SdServer.ADD_RESPONSE_HANDLER);
	//message.replyTo = messenger;
	//try {
	//    SdServer.send(message);
	//} catch (RemoteException e) {
	//    Log.v(TAG,e.toString());
	//	}

	Timer uiTimer = new Timer();
	uiTimer.schedule(new TimerTask() {
		@Override
		public void run() {updateServerStatus();}
	    }, 0, 1000);	

	startServer();
    }


    @Override
    protected void onStart() {
	super.onStart();
	Intent intent = new Intent(this,SdServer.class);
	bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
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
	sdServerIntent = new Intent(MainActivity.this,SdServer.class);
	sdServerIntent.setData(Uri.parse("Start"));
	getApplicationContext().startService(sdServerIntent);
    }

    /**
     * Stop the SdServer service
     */
    private void stopServer() {
	Log.v(TAG,"stopping Server...");
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
