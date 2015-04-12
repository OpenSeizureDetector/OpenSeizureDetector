package uk.org.openseizuredetector.locator;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.CompoundButton.OnCheckedChangeListener;
//import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LocatorActivity extends Activity  
{
    private String TAG = "LocatorActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Enable the testButton
        Button prefsButton = (Button) findViewById(R.id.prefsButton);
        prefsButton.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"prefsButton.onClick()");
		    Intent prefsIntent = new Intent(
						    LocatorActivity.this,
						    PrefActivity.class);
		    startActivity(prefsIntent);
		}
	    });        
    }
    

    // Show a 'Toast' message box.
    private void showToast(String msg) {
	Toast.makeText(this,
		       msg,
		       Toast.LENGTH_SHORT).show();

    }

}
