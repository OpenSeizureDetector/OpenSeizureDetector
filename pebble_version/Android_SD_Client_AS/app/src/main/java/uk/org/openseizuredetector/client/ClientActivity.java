/*
  Pebble_sd - a simple accelerometer based seizure detector that runs on a
  Pebble smart watch (http://getpebble.com).
  ClientActivity is a simple UI that runs on a client device and polls the 
  server (Android_SD_Server) to download status information periodically,
  displays it to the user and raises audible and visual alarms.

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

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.IntentService;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
//import android.net.wifi.WifiManager;
//import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import android.widget.Toast;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

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

import uk.org.openseizuredetector.SdData;

public class ClientActivity extends Activity {
    static final String TAG = "ClientActivity";
    private int okColour = Color.BLUE;
    private int warnColour = Color.MAGENTA;
    private int alarmColour = Color.RED;
    private Menu mOptionsMenu;
    final Handler serverStatusHandler = new Handler();
    private Timer mUiTimer;
    private Timer mDataTimer;
    SdClientService mSdClientService;
    private boolean mBound = false;
    private int mUiUpdatePeriod = 2000;
    private boolean mUseIpCamera = false;
    private String mCameraIp = null;
    private String mCameraUname = null;
    private String mCameraPasswd = null;
    private Intent sdClientServiceIntent;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialise the User Interface
        setContentView(R.layout.main);
        // Force the screen to stay on when the app is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	/* Force display of overflow menu - from stackoverflow
     * "how to force use of..."
	 */
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField =
                    ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.v(TAG, "menubar fiddle exception: " + e.toString());
        }

        // Deal with the 'Cancel Audible Button'
        Button button = (Button) findViewById(R.id.cancelAudibleButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "cancelAudibleButton.onClick()");
                if (mBound) {
                    mSdClientService.cancelAudible();
                }
            }
        });

        // Start the background service to monitor the server.
        startServer();


    }

    /**
     * Create Action Bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        mOptionsMenu = menu;
        return true;
    }

    /**
     * Respond to menu selections (from action bar or menu button)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "Option " + item.getItemId() + " selected");
        switch (item.getItemId()) {
            case R.id.action_show_camera:
                Log.v(TAG,"action_show_camera");
                Intent intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_start_stop:
                // Respond to the start/stop server menu item.
                Log.v(TAG, "action_sart_stop");
                if (mBound) {
                    Log.v(TAG, "Stopping Server");
                    stopServer();
                } else {
                    Log.v(TAG, "Starting Server");
                    startServer();
                }
                return true;
            case R.id.action_test_fault_warning_beep:
                Log.v(TAG, "action_test_fault_warning_beep");
                if (mBound) {
                    mSdClientService.faultWarningBeep();
                }
                return true;
            case R.id.action_test_alarm_beep:
                Log.v(TAG, "action_test_alarm_beep");
                if (mBound) {
                    mSdClientService.alarmBeep();
                }
                return true;
            case R.id.action_test_warning_beep:
                Log.v(TAG, "action_test_warning_beep");
                if (mBound) {
                    mSdClientService.warningBeep();
                }
                return true;
            case R.id.action_settings:
                Log.v(TAG, "action_settings");
                try {
                    Intent prefsIntent = new Intent(
                            ClientActivity.this,
                            PrefActivity.class);
                    this.startActivity(prefsIntent);
                } catch (Exception ex) {
                    Log.v(TAG, "exception starting settings activity " + ex.toString());
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

        mUseIpCamera = SP.getBoolean("useIpCamera", true);
        mCameraIp = SP.getString("cameraIp","192.168.1.25");
        mCameraUname = SP.getString("cameraUname","guest");
        mCameraPasswd = SP.getString("cameraPasswd","guest");

        try {
            String uiUpdatePeriodStr = SP.getString("UiUpdatePeriod", "2000");
            mUiUpdatePeriod = Integer.parseInt(uiUpdatePeriodStr);
            Log.v(TAG, "onStart() - mUiUpdatePeriod = " + mUiUpdatePeriod);
        } catch (Exception ex) {
            Log.v(TAG, "onStart() - Problem parsing preferences!");
            Toast toast = Toast.makeText(getApplicationContext(), "Problem Parsing Preferences - Something won't work", Toast.LENGTH_SHORT);
            toast.show();
        }

        // start timer to refresh user interface every 5 seconds.
        if (mUiTimer == null) {
            Log.v(TAG, "onstart(): starting mUiTimer.");
            mUiTimer = new Timer();
            mUiTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateUI();
                }
            }, 0, mUiUpdatePeriod);
            Log.v(TAG, "onStart(): started mUiTimer");
        } else {
            Log.v(TAG, "onStart(): mUiTimer already running");
        }


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
                Log.v(TAG, "failed to find versionName");
                versionName = null;
            }
        }
        tv.setText("OpenSeizureDetector Client Version " + versionName);

        startServer();

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect from background service.
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        // Stop the User Interface timer
        // Stop the status timer
        if (mUiTimer != null) {
            Log.v(TAG, "onStop(): cancelling UI timer");
            mUiTimer.cancel();
            mUiTimer.purge();
            mUiTimer = null;
        }

    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SdClientService.SdBinder binder = (SdClientService.SdBinder) service;
            mSdClientService = binder.getService();
            mBound = true;
            if (mSdClientService != null) {
                Log.v(TAG, "onServiceConnected() - Asking server to update its settings");
                mSdClientService.updatePrefs();
            } else {
                Log.v(TAG, "onServiceConnected() - mSdClientService is null - this is wrong!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisonnected()");
            mBound = false;
        }
    };

    /**
     * Start the SdClientService service
     */
    private void startServer() {
        // Start the server
        sdClientServiceIntent = new Intent(ClientActivity.this, SdClientService.class);
        sdClientServiceIntent.setData(Uri.parse("Start"));
        getApplicationContext().startService(sdClientServiceIntent);

        // and bind to it so we can see its data
        Intent intent = new Intent(this, SdClientService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Change the action bar icon to show the option to stop the service.
        if (mOptionsMenu != null) {
            Log.v(TAG, "Changing menu icons");
            MenuItem menuItem = mOptionsMenu.findItem(R.id.action_start_stop);
            menuItem.setIcon(R.drawable.stop_server);
            menuItem.setTitle("Stop Server");
        } else {
            Log.v(TAG, "mOptionsMenu is null - not changing icons!");
        }
    }

    /**
     * Stop the SdClientService service
     */
    private void stopServer() {
        Log.v(TAG, "stopping Server...");
        // unbind this activity from the service if it is bound.
        if (mBound) {
            try {
                unbindService(mConnection);
                mBound = false;
            } catch (Exception ex) {
                Log.e(TAG, "stopServer - error unbinding service - " + ex.toString());
            }
        }
        // then send an Intent to stop the service.
        sdClientServiceIntent = new Intent(ClientActivity.this, SdClientService.class);
        sdClientServiceIntent.setData(Uri.parse("Stop"));
        getApplicationContext().stopService(sdClientServiceIntent);

        // Change the action bar icon to show the option to start the service.
        if (mOptionsMenu != null) {
            Log.v(TAG, "Changing action bar icons");
            mOptionsMenu.findItem(R.id.action_start_stop).setIcon(R.drawable.start_server);
            mOptionsMenu.findItem(R.id.action_start_stop).setTitle("Start Server");
        } else {
            Log.v(TAG, "mOptionsMenu is null, not changing icons!");
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
            if ("uk.org.openseizuredetector.SdClientService"
                    .equals(service.service.getClassName())) {
                //Log.v(TAG,"Yes!");
                return true;
            }
        }
        //Log.v(TAG,"No!");
        return false;
    }


    /*
     * updateUI - called by the uiTimer timer periodically.
     * requests the ui to be updated by calling serverStatusRunnable.
     */
    private void updateUI() {
        Log.v(TAG, "updateUI()");
        serverStatusHandler.post(updateUIRunnable);
    }

    /*
     * updateUIRunnable - called by updateUI - updates the
     * user interface to reflect the current status received from the server.
     */
    final Runnable updateUIRunnable = new Runnable() {
        public void run() {
            if (mSdClientService != null) {
                TextView tv;
                tv = (TextView) findViewById(R.id.textView1);
                if (mSdClientService.mSdData.serverOK) {   // was isServerRunning
                    tv.setText("Server Running OK");
                    tv.setBackgroundColor(okColour);
                    tv = (TextView) findViewById(R.id.textView2);
                    tv.setText("Connecting to Server at http://"
                            + mSdClientService.mServerIP + ":8080");
                    tv.setBackgroundColor(okColour);
                } else {
                    tv.setText("***Can Not Access Server***");
                    tv.setBackgroundColor(alarmColour);
                }

                // Deal with Cancel Audible button
                Button cancelAudibleButton =
                        (Button) findViewById(R.id.cancelAudibleButton);
                if (mSdClientService.isAudibleCancelled()) {
                    cancelAudibleButton.setText("Audible Alarms Cancelled "
                            + "for "
                            + mSdClientService.
                            cancelAudibleTimeRemaining()
                            + " sec."
                            + " Press to re-enable");
                } else {
                    if (mSdClientService.mAudibleAlarm) {
                        cancelAudibleButton.setText("Cancel Audible Alarms (temporarily)");
                    } else {
                        cancelAudibleButton.setText("Audible Alarms OFF");
                    }
                }
                try {
                    tv = (TextView) findViewById(R.id.alarmTv);
                    if (mSdClientService.mSdData.alarmState == 0) {
                        tv.setText(mSdClientService.mSdData.alarmPhrase);
                        tv.setBackgroundColor(okColour);
                    }
                    if (mSdClientService.mSdData.alarmState == 1) {
                        tv.setText(mSdClientService.mSdData.alarmPhrase);
                        tv.setBackgroundColor(warnColour);
                    }
                    if (mSdClientService.mSdData.alarmState == 2) {
                        tv.setText(mSdClientService.mSdData.alarmPhrase);
                        tv.setBackgroundColor(alarmColour);
                    }
                    tv = (TextView) findViewById(R.id.pebTimeTv);
                    tv.setText(mSdClientService.mSdData.dataTime.format("%H:%M:%S"));
                    // Pebble Connected Phrase
                    tv = (TextView) findViewById(R.id.pebbleTv);
                    if (mSdClientService.mSdData.pebbleConnected) {
                        tv.setText("Pebble Watch Connected OK");
                        tv.setBackgroundColor(okColour);
                    } else {
                        tv.setText("** Pebble Watch NOT Connected **");
                        tv.setBackgroundColor(alarmColour);
                    }
                    tv = (TextView) findViewById(R.id.appTv);
                    if (mSdClientService.mSdData.pebbleAppRunning) {
                        tv.setText("Pebble App OK");
                        tv.setBackgroundColor(okColour);
                    } else {
                        tv.setText("** Pebble App NOT Running **");
                        tv.setBackgroundColor(alarmColour);
                    }
                    tv = (TextView) findViewById(R.id.battTv);
                    tv.setText("Pebble Battery = " + String.valueOf(mSdClientService.mSdData.batteryPc) + "%");
                    if (mSdClientService.mSdData.batteryPc <= 20)
                        tv.setBackgroundColor(alarmColour);
                    if (mSdClientService.mSdData.batteryPc > 20)
                        tv.setBackgroundColor(warnColour);
                    if (mSdClientService.mSdData.batteryPc >= 40)
                        tv.setBackgroundColor(okColour);

                    tv = (TextView) findViewById(R.id.debugTv);
                    String specStr = "";
                    for (int i = 0; i < 10; i++)
                        specStr = specStr
                                + mSdClientService.mSdData.simpleSpec[i]
                                + ", ";
                    tv.setText("Spec = " + specStr);
                } catch (Exception e) {
                    Log.v(TAG, "updateUIRunnable: Exception - " + e.toString());
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
                    if (mSdClientService.mSdData != null)
                        yVals.add(new Entry(mSdClientService.mSdData.simpleSpec[i], i));
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
            } else {
                Log.v(TAG, "mSdClientService is null - service not running?");
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

}
