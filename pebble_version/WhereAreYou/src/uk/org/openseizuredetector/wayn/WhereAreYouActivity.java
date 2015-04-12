package uk.org.openseizuredetector.wayn;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class WhereAreYouActivity extends Activity  
	implements LocationReceiver, OnClickListener {
	boolean mActive;
	String mPassword;
	int mTimeOutSec = 60;
	boolean mUseGPS = true;
    EditText pwdText;
    CheckBox enableCheckBox;
    EditText timeOutText;
    CheckBox useGPSCheckBox;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getPrefs();
        savePrefs();
        pwdText = (EditText) findViewById(R.id.passwordText1);
        enableCheckBox = (CheckBox) findViewById(R.id.enableCheckBox);
        timeOutText = (EditText) findViewById(R.id.timeOutText);
        useGPSCheckBox = (CheckBox) findViewById(R.id.useGPSCheckBox);
        setFormValues();
        
        //Enable the testButton
        Button testButton = (Button) findViewById(R.id.testButton);
        testButton.setOnClickListener(this);
        
        // Save the data when the enter or TAB key is pressed
        pwdText.setOnKeyListener(new View.OnKeyListener() {
        	public boolean onKey(View v, int keyCode, KeyEvent event) {
        		if ((event.getAction() == KeyEvent.ACTION_DOWN)&& 
        				((keyCode == KeyEvent.KEYCODE_ENTER) ||
        						(keyCode == KeyEvent.KEYCODE_TAB))) {
        			msgBox("Saving Data");
        			getFormValues();
        			savePrefs();
        			return true;
        		}
        		return false;
        	}
        });

        pwdText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
      			msgBox("Saving Data");
    			getFormValues();
    			savePrefs();
			}
		});
        
        enableCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	public void onCheckedChanged(CompoundButton v, boolean b) {
        		msgBox("Saving Data");
    			getFormValues();
    			savePrefs();
        	}
        });
        
        useGPSCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	public void onCheckedChanged(CompoundButton v, boolean b) {
        		msgBox("Saving Data");
    			getFormValues();
    			savePrefs();
        	}
        });
    }
    
    /** Populate the form components from member variables values */
    private void setFormValues() {
    	pwdText.setText(mPassword);
    	enableCheckBox.setChecked(mActive);
    	timeOutText.setText(Integer.toString(mTimeOutSec));
    	useGPSCheckBox.setChecked(mUseGPS);
    }
    
    /* Read values from the form, and set member variables with the values */
    private void getFormValues() {
    	mPassword = pwdText.getText().toString();
    	mActive = enableCheckBox.isChecked();
    	try {
        	mTimeOutSec = Integer.parseInt(timeOutText.getText().toString());
    	} catch(NumberFormatException nfe) {
    		msgBox("Could Not Parse "+timeOutText.getText().toString()+" as an Integer - defaulting to 60s.");
    		mTimeOutSec = 60;
    	} 
    	mUseGPS = useGPSCheckBox.isChecked();
    }
    
    /* Read preferences from the SharedPreferences storage area and populate member variables with the values */
    private void getPrefs() {
    	SharedPreferences settings = getSharedPreferences("WhereAreYou",MODE_PRIVATE);
    	mActive = settings.getBoolean("Active", true);
    	mPassword = settings.getString("Password","WAYN");
		mTimeOutSec = settings.getInt("TimeOutSec", 60);
		mUseGPS = settings.getBoolean("UseGPS",true);
    }
    
    /** Write the current valuse of the preferences to the SharedPreferences storage area */
    private void savePrefs() {
    	SharedPreferences settings = getSharedPreferences("WhereAreYou",MODE_PRIVATE);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putBoolean("Active", mActive);
    	editor.putString("Password",mPassword);
    	editor.putInt("TimeOutSec", mTimeOutSec);
    	editor.putBoolean("UseGPS", mUseGPS);
    	editor.commit();
    }



    /** Callback called by LocationFinder once it has found the location - passes a LonLat object
     * 	containing information on the location.
     */
    public void onLocationFound(LonLat ll) {
     	if (ll!=null) {
     		msgBox("Found Location - "+ll.toStr()+". Looking up address....");
     	} else {
         	msgBox("Failed to find location");
     	}
	}
    
	/* Callback for the 'Test' button - Uses LocationFinder to find the current location, and displays it on the screen */
	public void onClick(View arg0) {
		msgBox("Searching for Location...");
		Context contextArg = getApplicationContext();
        LocationFinder lf = new LocationFinder(contextArg);
     	lf.getLocationLL(this,mTimeOutSec,mUseGPS);
	 }

	/* Write a message box to the screen, and the log */
	public void msgBox(String msg) {
		BatteryMonitor bm=new BatteryMonitor();
		float batState = bm.getBatteryState(this);
		msg = msg + "\nBattery="+batState+"%";
     	TextView tv = (TextView)( findViewById(R.id.msgText));
     	tv.setText(msg);
     	//Toast.makeText(this,
    	//		msg,
    	//		Toast.LENGTH_SHORT).show();
    	Log.d("WhereAreYouActivity",msg);

    }


}
