/**
 * 
 */
package uk.org.openseizuredetector.locator;


import java.util.Iterator;
import java.util.List;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.location.GpsStatus;;

interface LocationReceiver {
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
public class LocationFinder implements LocationListener, Runnable, GpsStatus.Listener {
    LocationManager locMgr;
    LocationReceiver lr;
    String mProvider;
    long mTimeStart;
    Context mContext;
    boolean mTimedOut = false;
    int timeOutCount = 0;
    int timeOutSec;
    Handler mHandler;
    boolean mUseGPS = true;
    String TAG = "LocationFinder";
    
    public LocationFinder(Context contextArg) {
	mContext = contextArg;
	mHandler = new Handler();
	mUseGPS = false;
	locMgr = (LocationManager)
	    contextArg.getSystemService(
					Context.LOCATION_SERVICE);
	
	if (locMgr==null) {
	    Log.v("LocationFinder","ERROR - locMgr is null - THIS WILL NOT WORK!!!");
	}
	
	// List all providers to log.:
	List<String> providers = locMgr.getAllProviders();
	for (String provider : providers) {
	    Log.v("LocationFinder",provider+","+locMgr.getProvider(provider).toString());
	}
	
    }
    
    
    public void getLocationLL(LocationReceiver lr, int timeOutSec, boolean useGPS) {
	this.timeOutSec = timeOutSec;
	mUseGPS = useGPS;
	if (mUseGPS) {
	    mProvider = LocationManager.GPS_PROVIDER;
	} else {
	    mProvider = LocationManager.NETWORK_PROVIDER;
	}
	Log.v("mProvider",mProvider);
	this.lr = lr;
	// Ask for location updates to be sent to the onLocationChanged() method of this class.
	locMgr.requestLocationUpdates(mProvider, 0,0, this);
	// Monitor GPS Status while we are searching for a fix.  this.onGpsStatusChanged()
	// is called when the GPS Status changes.
	locMgr.addGpsStatusListener(this);
	
	// Set a timer running to allow us to give up on getting a GPS fix.  
        mHandler.removeCallbacks(this);
	// this.run() is called when the timer times out.
        mHandler.postDelayed(this, timeOutSec*1000);
    }
    
    public void startFixSearch() {
	locMgr.requestLocationUpdates(mProvider, 36000,10000, this);		
    }
    
    
    public void onLocationChanged(Location loc) {
	Log.v(TAG,"onLocationChanged");
	if (loc!=null) {
	    Log.v(TAG,"onLocationChanged - mTimedOut ="
		  +mTimedOut+" Provider="+loc.getProvider());
	    if ((loc.getProvider().equals(mProvider)) || mTimedOut) {
		locMgr.removeUpdates(this);
		locMgr.removeGpsStatusListener(this);
		// Avoid being called again by the timeOut run() function
		mHandler.removeCallbacks(this); 
		LonLat ll;
		ll = new LonLat(loc.getLongitude(),
				loc.getLatitude(),
				loc.getAccuracy(),
				loc.getProvider());
		lr.onLocationFound(ll);
	    } else {
		Log.v(TAG,"Skipping location update by "+loc.getProvider()+" mProvider="+mProvider+".");
		}
	} else {
	    Log.v(TAG,"loc==null - waiting...");
	}
    }
    
    public void onProviderDisabled(String provider) {
	// re-register for updates
	Log.v("locationListener","onProviderDisabled");
    }
    
    public void onProviderEnabled(String provider) {
	// is provider better than mProvider?
	// is yes, mProvider = provider
	Log.v("locationListener","onProviderEnabled");
    }
    
    public void onStatusChanged(String provider, int status, Bundle extras) {
	Log.v("locationListener","onStatusChanged");
    }
    
    // Called by the mHandler timer to signify timeout.  
    // In which case we give up on GPS and fall back onto NETWORK_PROVIDER.
    // At the moment, if we time out on NETWORK_PROVIDER too, it just
    // keeps trying over and over - maybe set a count in future to 
    // make it give up altogether eventually?
    public void run() {
	Log.v("WAYN","timeout runnable.run");
	mTimedOut = true;
	timeOutCount ++;
	Log.v(TAG,"TimedOut Number "+timeOutCount+
	      "! - using network location instead.");
	//Location loc = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	//onLocationChanged(loc);
	
	// switch off GPS monitoring to save battery.
	locMgr.removeGpsStatusListener(this);
	locMgr.removeUpdates(this);
	
	// Re-start search using network rather than GPS.
	mProvider = LocationManager.NETWORK_PROVIDER;
	Log.v("mProvider",mProvider);
	// Ask for location updates to be sent to the onLocationChanged() method of this class.
	locMgr.requestLocationUpdates(mProvider, 0,0, this);
	
	// Set a timer running to allow us to give up on getting a fix.  
	// this.run() is called when the timer times out.
	mHandler.removeCallbacks(this);
	mHandler.postDelayed(this, timeOutSec*1000);
    }
    
    public void onGpsStatusChanged(int eventNo) {
	//if (eventNo == GpsStatus.GPS_EVENT_SATELLITE_STATUS) 
	{
	    int nSat = 0;
	    GpsStatus gpsStatus = locMgr.getGpsStatus(null);
	    Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
	    Iterator<GpsSatellite> it = sats.iterator();
	    String msg = "Satellites...";
	    while ( it.hasNext() ) {
		nSat++;
		GpsSatellite oSat = (GpsSatellite) it.next() ; 
		Log.v("TEST","LocationActivity - onGpsStatusChange: Satellites: \n" + 
		      oSat.getSnr() ) ; 
		msg = msg + oSat.getPrn()+ ", "
		    + oSat.getSnr()+", "
		    + oSat.hasAlmanac()+","
		    + oSat.hasEphemeris()+", \n";
	    } 
	    msg = msg+" - "+nSat+" in view.";
	    Log.v(TAG,msg);
	}
    }
    
}
