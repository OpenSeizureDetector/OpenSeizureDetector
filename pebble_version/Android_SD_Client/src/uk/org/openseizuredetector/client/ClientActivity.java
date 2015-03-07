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
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.preference.Preference;
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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
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

public class ClientActivity extends Activity
{
    static final String TAG = "ClientActivity";
    private int okColour = Color.BLUE;
    private int warnColour = Color.MAGENTA;
    private int alarmColour = Color.RED;
    private Menu mOptionsMenu;
    final Handler serverStatusHandler = new Handler();
    private SdData mSdData;
    private Timer mUiTimer;
    private Timer mDataTimer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

	// Initialise the User Interface
        setContentView(R.layout.main);

	mSdData = new SdData();

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
	case R.id.action_settings:
	    Log.v(TAG,"action_settings");
	    try {
		Intent prefsIntent = new Intent(
						ClientActivity.this,
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

	// Timer to retrieve data from the server.
	if (mDataTimer==null) {
	    Log.v(TAG,"onStart(): starting mDataTimer timer");
	    mDataTimer = new Timer();
	    mDataTimer.schedule(new TimerTask() {
		    @Override
		    public void run() {getSdData();}
		}, 0, 1000);	
	    Log.v(TAG,"onStart(): started mDataTimer");
	} else {
	    Log.v(TAG,"onstart(): mDataTimer timer already running.");
	}

	// start timer to refresh user interface every second.
	if (mUiTimer==null) {
	    Log.v(TAG,"onstart(): starting mUiTimer.");
	    mUiTimer = new Timer();
	    mUiTimer.schedule(new TimerTask() {
		    @Override
		    public void run() {updateServerStatus();}
		}, 0, 1000);	
	    Log.v(TAG,"onStart(): started mUiTimer");
	} else {
	    Log.v(TAG,"onStart(): mUiTimer already running");
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
		Log.v(TAG,"failed to find versionName");
		versionName = null;
	    }
	}
	tv.setText("OpenSeizureDetector Client Version "+versionName);

    }

    @Override
    protected void onStop() {
	super.onStop();
    }



    private void getSdData() {
	Log.v(TAG,"getSdData()");
	ConnectivityManager connMgr = (ConnectivityManager) 
            getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadSdDataTask().execute("http://192.168.1.175:8080/data");
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
	    Log.v(TAG,"onPostExecute() - result="+result);
	    mSdData.fromJSON(result);
	    Log.v(TAG,"onPostExecute(): mSdData = "+mSdData.toString());
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
	    conn.setReadTimeout(10000 /* milliseconds */);
	    conn.setConnectTimeout(15000 /* milliseconds */);
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

    /*
     * updateServerStatus - called by the uiTimer timer periodically.
     * requests the ui to be updated by calling serverStatusRunnable.
     */
    private void updateServerStatus() {
	Log.v(TAG,"updateServerStatus()");
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
		if (true) {   // was isServerRunning
		    tv.setText("Server Running OK");
		    tv.setBackgroundColor(okColour);
		    tv = (TextView)findViewById(R.id.textView2);
		    tv.setText("Access Server at http://"
				   +":8080");
		    tv.setBackgroundColor(okColour);
		} else {
		    tv.setText("*** Server Stopped ***");
		    tv.setBackgroundColor(alarmColour);
		}


		try {
		    tv = (TextView) findViewById(R.id.alarmTv);
		    if (mSdData.alarmState==0) {
			tv.setText(mSdData.alarmPhrase);
			    tv.setBackgroundColor(okColour);
		    }
		    if (mSdData.alarmState==1) {
			tv.setText(mSdData.alarmPhrase);
			tv.setBackgroundColor(warnColour);
		    }
		    if (mSdData.alarmState==2) {
			tv.setText(mSdData.alarmPhrase);
			tv.setBackgroundColor(alarmColour);
		    }
		    tv = (TextView) findViewById(R.id.pebTimeTv);
		    tv.setText(mSdData.dataTime.format("%H:%M:%S"));
			// Pebble Connected Phrase
		    tv = (TextView) findViewById(R.id.pebbleTv);
		    if (mSdData.pebbleConnected) {
			tv.setText("Pebble Watch Connected OK");	    
			tv.setBackgroundColor(okColour);
		    } else {
			tv.setText("** Pebble Watch NOT Connected **");
			tv.setBackgroundColor(alarmColour);
		    }
		    tv = (TextView) findViewById(R.id.appTv);
		    if (mSdData.pebbleAppRunning) {
			tv.setText("Pebble App OK");	    
			tv.setBackgroundColor(okColour);
		    } else {
			tv.setText("** Pebble App NOT Running **");	    
			tv.setBackgroundColor(alarmColour);
		    }
		    tv = (TextView) findViewById(R.id.battTv);
		    tv.setText("Pebble Battery = "+String.valueOf(mSdData.batteryPc)+"%");
		    if (mSdData.batteryPc<=20)
			tv.setBackgroundColor(alarmColour);
		    if (mSdData.batteryPc>20)
			tv.setBackgroundColor(warnColour);
		    if (mSdData.batteryPc>=40)
			tv.setBackgroundColor(okColour);
		    
		    tv = (TextView) findViewById(R.id.debugTv);
		    String specStr = "";
		    for (int i=0;i<10;i++)
			specStr = specStr 
			    + mSdData.simpleSpec[i] 
			    + ", ";
		    tv.setText("Spec = "+specStr);
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
		    if (mSdData!=null)
			yVals.add(new Entry(mSdData.simpleSpec[i], i));
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

}
