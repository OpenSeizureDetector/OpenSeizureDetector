package uk.org.openseizuredetector;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import java.util.UUID;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


public class MainActivity extends Activity
{
    private UUID SD_UUID = UUID.fromString("03930f26-377a-4a3d-aa3e-f3b19e421c9d");
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


    private PebbleKit.PebbleDataReceiver msgDataHandler = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	Button button = (Button)findViewById(R.id.button1);
	button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
		 TextView statusText = (TextView) findViewById(R.id.textView1);
		 statusText.setText("button pressed!");
		 PebbleDictionary data = new PebbleDictionary();
		 data.addUint8(KEY_SETTINGS, (byte)1);
		 data.addString(1, "A string"); 
		 PebbleKit.sendDataToPebble(
					    getApplicationContext(), 
					    SD_UUID, 
					    data);     
             }
	    });
	onResume();
    }

    @Override
    protected void onPause() {
	super.onPause();
	if (msgDataHandler != null) {
	    unregisterReceiver(msgDataHandler);
	    msgDataHandler = null;
	}
    }
    @Override
    protected void onResume() {
	super.onResume();
	final Handler handler = new Handler();
	msgDataHandler = new PebbleKit.PebbleDataReceiver(SD_UUID) {
		@Override
		public void receiveData(final Context context,
					final int transactionId,
					final PebbleDictionary data) {
		    // Do something with data
		    PebbleKit.sendAckToPebble(context,transactionId);
		    handler.post(new Runnable() {
			    @Override
			    public void run() {
				updateUi(data);
			    }
			});
		}
	    };
	PebbleKit.registerReceivedDataHandler(this,msgDataHandler);
    }

    public void updateUi(final PebbleDictionary data) {
	String statusPhrase;
	String alarmPhrase;
	String viewText = "Unknown";
	long alarmState = 0;
	long maxVal = 0;
	long maxFreq = 0;
	long specPower = 0;
	if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)==1) {
	    alarmState = data.getUnsignedIntegerAsLong(KEY_ALARMSTATE);
	    maxVal = data.getUnsignedIntegerAsLong(KEY_MAXVAL);
	    maxFreq = data.getUnsignedIntegerAsLong(KEY_MAXFREQ);
	    specPower = data.getUnsignedIntegerAsLong(KEY_SPECPOWER);
	    alarmPhrase = "Unknown";
	    if (alarmState==0) alarmPhrase="OK";
	    if (alarmState==1) alarmPhrase="WARNING";
	    if (alarmState==2) alarmPhrase="ALARM";
	    //alarmPhrase = data.toJsonString()+"alarmState="+alarmState;
	    //alarmPhrase = data.getString(10);
	    viewText = alarmPhrase + "\n " +
		"maxVal = "+maxVal+"\n" +
		"maxFreq = "+maxFreq+"\n" +
		"specPower = "+specPower +
		"\n" + data.toJsonString();
	    TextView statusText = (TextView) findViewById(R.id.textView1);
	    statusText.setText(viewText);
	}
	if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)==2) {
	    viewText = "Settings:\n"+data.toJsonString();
	    TextView settingsText = (TextView) findViewById(R.id.textView2);
	    settingsText.setText(viewText);
	}
	    
    }

    public void startWatchApp(View view) {
	PebbleKit.startAppOnPebble(getApplicationContext(),
				   SD_UUID);

    }

    public void stopWatchApp(View view) {
	PebbleKit.closeAppOnPebble(getApplicationContext(),
				   SD_UUID);
    }

}
