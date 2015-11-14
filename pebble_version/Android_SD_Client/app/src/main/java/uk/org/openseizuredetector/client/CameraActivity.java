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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends Activity implements IpCamListener, View.OnClickListener {
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

        findViewById(R.id.leftButton).setOnClickListener(this);
        findViewById(R.id.upButton).setOnClickListener(this);
        findViewById(R.id.downButton).setOnClickListener(this);
        findViewById(R.id.rightButton).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updatePrefs();
        Log.v(TAG, "onStart - mCmdSet = " + mCmdSet);
        mIpCamController = new IpCamController(getApplicationContext(),mCameraIp, mCameraUname, mCameraPasswd, mCmdSet, this);
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
            case R.id.action_find_camera:
                Log.v(TAG, "action_find_camera");
                mIpCamController.findCamera();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.leftButton:
                mIpCamController.moveCamera(0);
                break;
            case R.id.upButton:
                mIpCamController.moveCamera(1);
                break;
            case R.id.downButton:
                mIpCamController.moveCamera(2);
                break;
            case R.id.rightButton:
                mIpCamController.moveCamera(3);
                break;
            case R.id.centreButton:
                mIpCamController.moveCamera(4);
                break;
        }
        mIpCamController.getImage();
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
        String prefStr;
        prefStr = SP.getString("UiUpdatePeriod", "5000");
        mUiTimerPeriod = Integer.parseInt(prefStr);
        Log.v(TAG, "updatePrefs() - setting mUiTimerPeriod to " + mUiTimerPeriod);
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
