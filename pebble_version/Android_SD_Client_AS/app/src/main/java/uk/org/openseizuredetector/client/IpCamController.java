package uk.org.openseizuredetector.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;


/**
 * the onGotImage(img,msg) function is called when an image is retrieved from the ip camera.  img is the byte array containing the
 * image data, and msg is a message for the user about the call.
 */
interface IpCamListener {
    public void onGotImage(byte[] img, String msg);
}

/**
 * IpCamController will grab an image of an IP Camera on demand, carrying out the activity in the background to avoid locking
 * up the user interface.
 * Created by graham on 04/08/15.
 */
public class IpCamController {
    private String mIpAddr;  // ip address of camera
    private String mUname;   // camera user name
    private String mPasswd;   // camera password
    private int mCmdSet;   // Command set for ip camera 0 = mJPEG, 1=h264.
    private String TAG = "IpCamController";
    private IpCamListener mIpCamListener = null;   // the class containing the onGotImage() callback function.
    private byte[] mResult;
    private String mMsg;
    private ImageDownloader mImageDownloader;

    /**
     * Create an instance of IpCamController with specified ip adress, username, password and command set.
     * Command set will be used to select different brands of IP camera, which use different commands to  obtain images,
     * but this is not implemented at present.
     *
     * @param ipAddr
     * @param uname
     * @param passwd
     * @param cmdSet
     * @TODO Implement cmdSet to use different iP Cameras.
     * @TODO Extend class to do more with the IP camera - move a pan/tilt camera etc.
     */
    public IpCamController(String ipAddr, String uname, String passwd, int cmdSet, IpCamListener listener) {
        mIpAddr = ipAddr;
        mUname = uname;
        mPasswd = passwd;
        mCmdSet = cmdSet;
        mIpCamListener = listener;
        mResult = null;
        mMsg = "";
    }

    /**
     * grab a still image from the camera.  Returns a JPEG image as a byte array.
     *
     * @return the image data as a byte array.
     */
    public void getImage() {
        Log.v(TAG, "getImage() - mIpAddr = " + mIpAddr);
        mMsg = "";
        ImageDownloader imageDownloader = new ImageDownloader();
        String url = getImgUrl(mIpAddr, mUname, mPasswd, mCmdSet);
        Log.v(TAG, "getImage = url=" + url);
        imageDownloader.execute(url);
    }

    public String getImgUrl(String ipAddr, String uname, String passwd, int cmdSet) {
        Log.v(TAG, "getImgUrl() - cmdSet = " + cmdSet);
        String url = "";
        switch (cmdSet) {
            case 0:  // mjpeg camera
                Log.v(TAG, "cmdSet 0 - mjpeg");
                url = "http://" + ipAddr + "/snapshot.cgi?user=" + mUname + "&pwd=" + mPasswd;
                break;
            case 1:  // h.264 camera
                Log.v(TAG, "cmdSet 1 = H264");
                url = "http://" + ipAddr + "/tmpfs/auto.jpg?usr=" + mUname + "&pwd=" + mPasswd;
                break;
            default:
                url = "";
                //mIpCamListener.onGotImage(null,"Unrecognised Camera Type "+cmdSet);
        }
        Log.v(TAG, "getImgUrl() - returning " + url);
        return url;
    }

    /**
     * Move the camera in specified direction 0->left 1->up 2->down 3->right.
     *
     * @param dir
     */
    public void moveCamera(int dir) {

    }

    /**
     * Scan the local network for a device that responds to our camera URL, and return the IP address of the responding device.
     */
    public void findCamera() {
        Log.v(TAG, "findCamera()");
        mMsg = "";
        CameraFinder cameraFinder = new CameraFinder();
        cameraFinder.execute(new String[]{"/snapshot.cgi?user=" + mUname + "&pwd=" + mPasswd});


    }

    /**
     * get the ip address of the phone.
     * Based on http://stackoverflow.com/questions/11015912/how-do-i-get-ip-address-in-ipv4-format
     */
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
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

    /**
     * imageDownloader - based on http://javatechig.com/android/download-image-using-asynctask-in-android
     */
    private class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            Log.v(TAG, "doInBackground - calling downloadImage(" + params[0] + ")");
            //try {Thread.sleep(10000);} catch(Exception e) {Log.v(TAG,"Exception during sleep - "+e.toString());}
            Bitmap bitmap = (Bitmap) downloadImage((String) params[0]);
            Log.v(TAG, "doInBackground - returned from downloadImage()");
            return bitmap;
        }

        @Override
        protected void onPreExecute() {
            Log.v(TAG, "onPreExecute()");
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.v(TAG, "onPostExecute()");
            super.onPostExecute(bitmap);
            byte[] data;
            Log.v(TAG, "onPostExecute() - converting bitmap to byte array...");
            try {
                if (bitmap != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                    data = os.toByteArray();
                    mMsg = mMsg + " - OK";
                } else {
                    mMsg = mMsg + " - null image recieved";
                    data = null;
                }
                mIpCamListener.onGotImage(data, mMsg);
            } catch (Exception e) {
                Log.e(TAG, "Error in onPostExecute()");
                e.printStackTrace();
                mMsg = mMsg + " - ERROR retrieving IpCamera Image";
                mIpCamListener.onGotImage(null, mMsg);
            }
        }


        private Bitmap downloadImage(String url) {
            byte[] result = null;
            final DefaultHttpClient client = new DefaultHttpClient();
            final HttpGet getRequest = new HttpGet(url);
            try {
                HttpResponse response = client.execute(getRequest);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    Log.w(TAG, "Error " + statusCode +
                            " while retrieving bitmap from " + url);
                    mMsg = mMsg + "Error Retrieving Bitmap from " + url + " Status Code = " + statusCode;
                    return null;
                }

                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = null;
                    try {
                        Log.v(TAG, "entity=" + entity.toString());
                        // getting contents from the stream
                        inputStream = entity.getContent();
                        Log.v(TAG, "inputStrem=" + inputStream.toString());
                        // decoding stream data back into image Bitmap that android understands
                        final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Log.v(TAG, "downloadImage() - Returning Bitmap");
                        return bitmap;

                        //inputStream.read(result);
                        //return result;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            } catch (Exception e) {
                // You Could provide a more explicit error message for IOException
                getRequest.abort();
                Log.e(TAG, "Something went wrong while" +
                        " retrieving bitmap from " + url + ": Error is: " + e.toString());
            }
            return null;
        }
    }

    /**
     * cameraMover - move the camera.   Call CameraMover.execute with an object {"IP Addres" (String), Direction (int)}
     */
    private class CameraMover extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            Log.v(TAG, "doInBackground - calling moveCamera(" + params[0] + ")");
            //try {Thread.sleep(10000);} catch(Exception e) {Log.v(TAG,"Exception during sleep - "+e.toString());}
            boolean retVal = moveCamera((String) params[0], (int) params[1]);
            Log.v(TAG, "doInBackground - returned from moveCamera()");
            return retVal;
        }

        @Override
        protected void onPreExecute() {
            Log.v(TAG, "onPreExecute()");
        }

        @Override
        protected void onPostExecute(Boolean retVal) {
            Log.v(TAG, "onPostExecute()");
            super.onPostExecute(retVal);
            mIpCamListener.onGotImage(null, mMsg);
        }


        private boolean moveCamera(String ipAddress, int direction) {
            String url = "http://" + ipAddress + "?decoder_control.cgi";
            byte[] result = null;
            final DefaultHttpClient client = new DefaultHttpClient();
            final HttpGet getRequest = new HttpGet(url);
            try {
                HttpResponse response = client.execute(getRequest);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    Log.w(TAG, "Error " + statusCode +
                            " moving camera using " + url);
                    mMsg = mMsg + "Error moving camera using " + url + " Status Code = " + statusCode;
                    return false;
                }

            } catch (Exception e) {
                // You Could provide a more explicit error message for IOException
                getRequest.abort();
                Log.e(TAG, "Something went wrong while" +
                        " moving camera " + url + ": Error is: " + e.toString());
                return false;
            }
            return true;
        }
    }

    /**
     * cameraFinder - scan the local network for an IP camera, and return the IP address of the camera by calling onCameraFound().
     */
    private class CameraFinder extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String cameraIpAddr = null;
            String phoneIpAddr = getLocalIpAddress();
            Log.v(TAG, "doInBackground() - phone IP Address = " + phoneIpAddr);
            String[] ipParts = phoneIpAddr.split("\\.");
            String networkIpBase = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";
            Log.v(TAG, "doInBackground() - network Base = " + networkIpBase);

            for (int i = 1; i <= 255; i++) {
                String testIpAddr = networkIpBase + i;
                Log.v(TAG, "doInBackground() - testing " + testIpAddr);
                if (isCamera(testIpAddr)) {
                    Log.d(TAG, "doInBackground() - found Camera at " + testIpAddr);
                    cameraIpAddr = testIpAddr;
                }
            }

            return cameraIpAddr;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.v(TAG, "onPostExecute() - Camera IP Address = " + s);
            mIpCamListener.onGotImage(null, s);
        }

        private boolean isCamera(String ipAddr) {
            String url = "http://" + ipAddr + "/snapshot.cgi?user=guest&pwd=guest";
            Log.v(TAG, "isCamera() - url = " + url);
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
            HttpConnectionParams.setSoTimeout(httpParams, 3000);
            final DefaultHttpClient client = new DefaultHttpClient(httpParams);
            final HttpGet getRequest = new HttpGet(url);
            try {
                HttpResponse response = client.execute(getRequest);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    Log.d(TAG, "IP Address " + ipAddr + "gave status code " + statusCode);
                    return false;
                } else {
                    Log.d(TAG, "CAMERA FOUND AT IP Address " + ipAddr + "(status code " + statusCode + ")");
                    return true;
                }
            } catch (Exception e) {
                Log.d(TAG, "IP Address " + ipAddr + " connection failed");
                //e.printStackTrace();
                return false;
            }
        }
    }


}

