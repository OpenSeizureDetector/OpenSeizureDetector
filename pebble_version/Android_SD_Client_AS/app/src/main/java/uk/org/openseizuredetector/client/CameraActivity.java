package uk.org.openseizuredetector.client;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends Activity implements IpCamListener {
    private String TAG = "CameraActivity";
    private boolean mUseIpCamera = false;
    private String mCameraIp = null;
    private String mCameraUname = null;
    private String mCameraPasswd = null;
    private int mCmdSet = 0;
    private int mUiTimerPeriod = 1000;
    private Timer mUiTimer;
    private IpCamController mIpCamController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mUiTimer = new Timer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updatePrefs();
        Log.v(TAG, "onStart - mCmdSet = " + mCmdSet);
        mIpCamController = new IpCamController(mCameraIp, mCameraUname, mCameraPasswd, mCmdSet, this);
        // start timer to refresh user interface every 5 seconds
        mUiTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mIpCamController.getImage();
            }
        }, 0, mUiTimerPeriod);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mUiTimer.purge();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUiTimer.purge();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUiTimer.purge();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Log.v(TAG, "action_settings");
                try {
                    Intent prefsIntent = new Intent(
                            this,
                            PrefActivity.class);
                    this.startActivity(prefsIntent);
                } catch (Exception ex) {
                    Log.v(TAG, "exception starting settings activity " + ex.toString());
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGotImage(byte[] img, String msg) {
        Log.v(TAG, "cameraPanTiltOnClickListener.onGotImage()");
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        if (img != null) {
            Bitmap bm = BitmapFactory.decodeByteArray(img, 0, img.length);
            imageView.setImageBitmap(bm);
        }

        TextView textView = (TextView) findViewById(R.id.cameraTextView);
        textView.setText(msg);

    }

    private void updatePrefs() {
        Log.v(TAG, "updatePrefs()");

        SharedPreferences SP = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        mUiTimerPeriod = SP.getInt("uiTimerPeriod", 5000);
        mUseIpCamera = SP.getBoolean("UseIpCamera", true);
        mCameraIp = SP.getString("CameraIp", "192.168.1.25");
        mCameraUname = SP.getString("CameraUname", "guest");
        mCameraPasswd = SP.getString("CameraPasswd", "guest");
        try {
            String cmdSet = SP.getString("CameraCmdSet", "0");
            Log.v(TAG, "cmdSet=" + cmdSet);
            mCmdSet = Integer.parseInt(cmdSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v(TAG, "mCameraIp = " + mCameraIp);
        Log.v(TAG, "mCameraUname = " + mCameraUname);
        Log.v(TAG, "mCameraPasswd = " + mCameraPasswd);

    }
}
