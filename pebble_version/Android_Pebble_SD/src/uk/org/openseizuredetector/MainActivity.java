package uk.org.openseizuredetector;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import java.util.UUID;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


public class MainActivity extends Activity
{
    private UUID SD_UUID = UUID.fromString("03930f26-377a-4a3d-aa3e-f3b19e421c9d");

    private PebbleKit.PebbleDataReceiver msgDataHandler = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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
				updateUi();
			    }
			});
		}
	    };
	PebbleKit.registerReceivedDataHandler(this,msgDataHandler);
    }

    public void updateUi() {

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
