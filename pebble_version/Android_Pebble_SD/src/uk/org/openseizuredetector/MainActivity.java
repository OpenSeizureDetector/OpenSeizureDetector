package uk.org.openseizuredetector;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


public class MainActivity extends Activity
{
    static final String TAG = "MainActivity";
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


    private short fftResults[];
    private Intent sdServerIntent;

    private PebbleKit.PebbleDataReceiver msgDataHandler = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	// Allocate memory for the FFT spectrum results
	fftResults = new short[NSAMP/2];

	// Initialise the User Interface
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

	button = (Button)findViewById(R.id.button2);
	button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
		 Log.v(TAG,"Starting Web Server");
		 sdServerIntent = new Intent(MainActivity.this,SdServer.class);
		 sdServerIntent.setData(Uri.parse("Start"));
		 getApplicationContext().startService(sdServerIntent);
		 
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
		    PebbleKit.sendAckToPebble(context,transactionId);
		    // Do something with data
		    // If we have a spectrum data packet, add it to the 
		    // spectrum arry.
		    if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)
			== DATA_TYPE_SPEC) {
			int posMin = data.getUnsignedIntegerAsLong(KEY_POS_MIN).intValue();
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
		    }
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
	if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)==DATA_TYPE_RESULTS) {
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
	if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)==DATA_TYPE_SETTINGS) {
	    viewText = "Settings:\n"+data.toJsonString();
	    TextView settingsText = (TextView) findViewById(R.id.textView2);
	    settingsText.setText(viewText);
	}
	if (data.getUnsignedIntegerAsLong(KEY_DATA_TYPE)==DATA_TYPE_SPEC) {
	    
	    String fftText = String.format("0=%06d, 10=%06d, 50=%06d",fftResults[0],fftResults[10],fftResults[50]);
	    TextView settingsText = (TextView) findViewById(R.id.textView3);
	    settingsText.setText(fftText);
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
