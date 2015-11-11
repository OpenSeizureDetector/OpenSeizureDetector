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

import android.app.ActionBar;
import android.app.Activity;
import android.app.IntentService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.preference.Preference;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.Context;
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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.conn.util.InetAddressUtils;
import java.lang.CharSequence;
import android.util.AttributeSet;

//MPAndroidChart
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.filter.Approximator;
import com.github.mikephil.charting.data.filter.Approximator.ApproximatorType;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;

import uk.org.openseizuredetector.SdServer;

public class MainActivity extends Activity
{
    static final String TAG = "MainActivity";
    private int okColour = Color.BLUE;
    private int warnColour = Color.MAGENTA;
    private int alarmColour = Color.RED;
    SdServer mSdServer;
    boolean mBound = false;
    private Menu mOptionsMenu;

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

        // Force the screen to stay on when the app is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Deal with the 'AcceptAlarm Button'
		Button button = (Button) findViewById(R.id.acceptAlarmButton);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG, "acceptAlarmButton.onClick()");
				if (mBound) {
					mSdServer.acceptAlarm();
				}
			}
		});

	// start timer to refresh user interface every second.
	Timer uiTimer = new Timer();
	uiTimer.schedule(new TimerTask() {
		@Override
		public void run() {updateServerStatus();}
	    }, 0, 1000);	

    }

    /**
     * Create Action Bar
     */
    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
	getMenuInflater().inflate(R.menu.main_activity_actions,menu);
	mOptionsMenu = menu;
	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	Log.v(TAG,"Option "+item.getItemId()+" selected");
	switch (item.getItemId()) {
	case R.id.action_launch_pebble_app:
	    Log.v(TAG,"action_launch_pebble_app");
	    try {
		PackageManager pm = this.getPackageManager();
		Intent pebbleAppIntent = pm.getLaunchIntentForPackage("com.getpebble.android");
		this.startActivity(pebbleAppIntent);
	    } catch (Exception ex) {
		Log.v(TAG,"exception starting pebble App "+ex.toString());
	    }
	    return true;

    case R.id.action_accept_alarm:
        Log.v(TAG,"action_accept_alarm");
        if (mBound)      {
            mSdServer.acceptAlarm();
        }
        return true;
	case R.id.action_start_stop:
	    // Respond to the start/stop server menu item.
	    Log.v(TAG,"action_sart_stop");
	    if (mBound) {
		Log.v(TAG,"Stopping Server");
		unbindFromServer();
		stopServer();
	    } else {
		Log.v(TAG,"Starting Server");
		startServer();
		// and bind to it so we can see its data
		bindToServer();
	    }
	    return true;
	case R.id.action_test_fault_beep:
	    Log.v(TAG,"action_test_fault_beep");
	    if (mBound) {
		mSdServer.faultWarningBeep();
	    }
	    return true;
	case R.id.action_test_alarm_beep:
	    Log.v(TAG,"action_test_alarm_beep");
	    if (mBound) {
		mSdServer.alarmBeep();
	    }
	    return true;
	case R.id.action_test_warning_beep:
	    Log.v(TAG,"action_test_warning_beep");
	    if (mBound) {
		mSdServer.warningBeep();
	    }
	    return true;
	case R.id.action_test_sms_alarm:
	    Log.v(TAG,"action_test_sms_alarm");
	    if (mBound) {
		mSdServer.sendSMSAlarm();
	    }
	    return true;
	case R.id.action_settings:
	    Log.v(TAG,"action_settings");
	    try {
		Intent prefsIntent = new Intent(
						MainActivity.this,
						PrefActivity.class);
		this.startActivity(prefsIntent);
	    } catch (Exception ex) {
		Log.v(TAG,"exception starting settings activity "+ex.toString());
	    }
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    @Override
    protected void onStart() {
	super.onStart();
	SharedPreferences SP = PreferenceManager
	    .getDefaultSharedPreferences(getBaseContext());
	boolean audibleAlarm = SP.getBoolean("AudibleAlarm",true);
	Log.v(TAG,"onStart - auidbleAlarm = "+audibleAlarm);

	TextView tv;
	tv = (TextView) findViewById(R.id.versionTv);
	String versionName = "unknown";
	// From http://stackoverflow.com/questions/4471025/
	//         how-can-you-get-the-manifest-version-number-
	//         from-the-apps-layout-xml-variable
	final PackageManager packageManager = getPackageManager();
	if (packageManager != null) {
	    try {
		PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
		versionName = packageInfo.versionName;
	    } catch (PackageManager.NameNotFoundException e) {
		Log.v(TAG,"failed to find versionName");
		versionName = null;
	    }
	}
	tv.setText("OpenSeizureDetector Server Version " + versionName);

	if (!isServerRunning()) {
	    Log.v(TAG,"Server not Running - Starting Server");
	    startServer();
	} else {
	    Log.v(TAG,"Server Already Running OK");
	}
	// and bind to it so we can see its data
	bindToServer();

    }

    @Override
    protected void onStop() {
	super.onStop();
	unbindFromServer();
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
	    if (mSdServer!=null) {
		Log.v(TAG,"onServiceConnected() - Asking server to update its settings");
		mSdServer.updatePrefs();
	    }
	    else {
		Log.v(TAG,"onServiceConnected() - mSdServer is null - this is wrong!");
	    }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
	    Log.v(TAG,"onServiceDisonnected()");
            mBound = false;
        }
    };


    /**
     * bind to an already running server.
     */
    private void bindToServer() {
	Log.v(TAG,"bindToServer() - binding to SdServer");
	Intent intent = new Intent(this,SdServer.class);
	bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
    }

    /** 
     * unbind from server
     */
    private void unbindFromServer() {
	// unbind this activity from the service if it is bound.
	if (mBound) {
	    Log.v(TAG,"unbindFromServer() - unbinding");
	    try {
		unbindService(mConnection);
		mBound = false;
	    } catch (Exception ex) {
		Log.e(TAG,"unbindFromServer() - error unbinding service - "+ex.toString());
	    }
	} else {
	    Log.v(TAG,"unbindFromServer() - not bound to server - ignoring");
	}
    }

    /**
     * Start the SdServer service
     */
    private void startServer() {
	// Start the server
	sdServerIntent = new Intent(MainActivity.this,SdServer.class);
	sdServerIntent.setData(Uri.parse("Start"));
	getApplicationContext().startService(sdServerIntent);

	// Change the action bar icon to show the option to stop the service.
	if (mOptionsMenu!=null) {
	    Log.v(TAG,"Changing menu icons");
	    MenuItem menuItem = mOptionsMenu.findItem(R.id.action_start_stop);
	    menuItem.setIcon(R.drawable.stop_server);
	    menuItem.setTitle("Stop Server");
	} else {
	    Log.v(TAG,"mOptionsMenu is null - not changing icons!");
	}
    }

    /**
     * Stop the SdServer service
     */
    private void stopServer() {
	Log.v(TAG,"stopping Server...");

	// then send an Intent to stop the service.
	sdServerIntent = new Intent(MainActivity.this,SdServer.class);
	sdServerIntent.setData(Uri.parse("Stop"));
	getApplicationContext().stopService(sdServerIntent);

	// Change the action bar icon to show the option to start the service.
	if (mOptionsMenu!=null) {
	    Log.v(TAG,"Changing action bar icons");
	    mOptionsMenu.findItem(R.id.action_start_stop).setIcon(R.drawable.start_server);
	    mOptionsMenu.findItem(R.id.action_start_stop).setTitle("Start Server");
	} else {
	    Log.v(TAG,"mOptionsMenu is null, not changing icons!");
	}

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
     * serverStatusRunnable - called by updateServerStatus - updates the
     * user interface to reflect the current status received from the server.
     */
    final Runnable serverStatusRunnable = new Runnable() {
	    public void run() {
		TextView tv;
		tv = (TextView) findViewById(R.id.textView1);
		if (isServerRunning()) {
		    tv.setText("Server Running OK");
		    tv.setBackgroundColor(okColour);
		    tv = (TextView)findViewById(R.id.textView2);
		    tv.setText("Access Server at http://"
				   +getLocalIpAddress()
				   +":8080");
		    tv.setBackgroundColor(okColour);
		} else {
		    tv.setText("*** Server Stopped ***");
		    tv.setBackgroundColor(alarmColour);
		}


		try {
		    if (mBound) {
			tv = (TextView) findViewById(R.id.alarmTv);
			if ((mSdServer.sdData.alarmState==0)
                    && !mSdServer.sdData.alarmStanding
                    && !mSdServer.sdData.fallAlarmStanding) {
			    tv.setText("OK");
			    tv.setBackgroundColor(okColour);
			}
			if ((mSdServer.sdData.alarmState==1)
                    && !mSdServer.sdData.alarmStanding
                    && !mSdServer.sdData.fallAlarmStanding) {
			    tv.setText("WARNING");
			    tv.setBackgroundColor(warnColour);
			}
			if (mSdServer.sdData.alarmStanding) {
			    tv.setText("**ALARM**");
			    tv.setBackgroundColor(alarmColour);
			}
                if (mSdServer.sdData.fallAlarmStanding) {
                    tv.setText("**FALL**");
                    tv.setBackgroundColor(alarmColour);
                }
			tv = (TextView) findViewById(R.id.pebTimeTv);
			tv.setText(mSdServer.mPebbleStatusTime.format("%H:%M:%S"));
			// Pebble Connected Phrase
			tv = (TextView) findViewById(R.id.pebbleTv);
			if (mSdServer.sdData.pebbleConnected) {
			    tv.setText("Pebble Watch Connected OK");	    
			    tv.setBackgroundColor(okColour);
			} else {
			    tv.setText("** Pebble Watch NOT Connected **");
			    tv.setBackgroundColor(alarmColour);
			}
			tv = (TextView) findViewById(R.id.appTv);
			if (mSdServer.sdData.pebbleAppRunning) {
			    tv.setText("Pebble App OK");	    
			    tv.setBackgroundColor(okColour);
			} else {
			    tv.setText("** Pebble App NOT Running **");	    
			    tv.setBackgroundColor(alarmColour);
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
			    specStr = specStr 
				+ mSdServer.sdData.simpleSpec[i] 
				+ ", ";
			tv.setText("Spec = "+specStr);
		    }
		    else {
			tv = (TextView) findViewById(R.id.alarmTv);
			tv.setText("Not Connected to Server");
			tv.setBackgroundColor(warnColour);
		    }
		} catch (Exception e) {
		    Log.v(TAG,"ServerStatusRunnable: Exception - "+e.toString());
		}
		////////////////////////////////////////////////////////////
		// Produce graph
		LineChart mChart = (LineChart) findViewById(R.id.chart1);
		mChart.setDescription("");
		mChart.setNoDataTextDescription("You need to provide data for the chart.");
		// X Values
		ArrayList<String> xVals = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
		    xVals.add((i) + "");
		}
		// Y Values
		ArrayList<Entry> yVals = new ArrayList<Entry>();
		for (int i = 0; i < 10; i++) {
		    if (mSdServer!=null)
			yVals.add(new Entry(mSdServer.sdData.simpleSpec[i], i));
		    else
			yVals.add(new Entry(i, i));
		}

		// create a dataset and give it a type
		LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
		set1.setColor(Color.BLACK);
		set1.setLineWidth(1f);

		ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
		dataSets.add(set1); // add the datasets
		LineData data = new LineData(xVals, dataSets);
		//data.setValueTextSize(10f);
		mChart.setData(data);
		mChart.invalidate();
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

    //public void updateUi() {
    //	String statusPhrase;
    //	String alarmPhrase;
    //	String viewText = "Unknown";
    //}

    class ResponseHandler extends Handler {
	@Override public void handleMessage(Message message) {
	    Log.v(TAG,"Message="+message.toString());
	}
    }

}
