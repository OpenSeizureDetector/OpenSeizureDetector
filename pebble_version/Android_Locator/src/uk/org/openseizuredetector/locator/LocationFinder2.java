/**
 * Based on https://github.com/googlesamples/android-play-location/tree/master/BasicLocationSample
 *
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.openseizuredetector.locator;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

interface LocationReceiver2 {
    /** The function to be called once we have found the location */
    public void onLocationFound(LonLat ll);
}


/**
 * @author Graham Jones
 * Based on various examples from the internet.
 * Call with 
 * 			LocationFinder lf = new LocationFinder(this);
 * 			LonLat ll = lf.getLocationLL();
 *
 */
public class LocationFinder2 implements 
        ConnectionCallbacks, OnConnectionFailedListener {

    LocationReceiver lr;
    Context mContext;
    protected static final String TAG = "LocationFinder";
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    public LocationFinder2(Context contextArg) {
	mContext = contextArg;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
	    .addConnectionCallbacks(this)
	    .addOnConnectionFailedListener(this)
	    .addApi(LocationServices.API)
	    .build();
    }

    public void destructor() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void getLocationLL(LocationReceiver lr) {
        mGoogleApiClient.connect();
	this.lr = lr;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
	Location loc;
        loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (loc != null) {
	    Toast.makeText(mContext,"Location is ("
			   +loc.getLatitude()+","
			   +loc.getLongitude()+")",
			   Toast.LENGTH_LONG);
	    LonLat ll;
	    ll = new LonLat(loc.getLongitude(),
				loc.getLatitude(),
				loc.getAccuracy(),
				loc.getProvider());
	    lr.onLocationFound(ll);
        } else {
            Toast.makeText(mContext, "No Location Detected", Toast.LENGTH_LONG).show();
	    lr.onLocationFound(null);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
}
