package uk.org.openseizuredetector.wayn;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class LonLat {
    public final double _lon;  //longitude in degrees
    public final double _lat;  // latitude in degrees
    public final float _acc;  // fix accuracy in metres.
    public double lon() {return _lon;};
    public double lat() {return _lat;};
    public String provider;
    public LonLat(double llon,double llat, float lacc, String provider) {
	_lon = llon;
	_lat = llat;
	_acc = lacc;
	this.provider = provider;
    }

    /**
     * Returns location as a human readable string 
     */
    String toStr() {
	NumberFormat df = new DecimalFormat("#0.000");
	return ("lon="+df.format(_lon)+", lat="+df.format(_lat)+":  accuracy="+df.format(_acc)+" m ("+provider+")");
    }

    /**
     * Returns location as a Geo URI (http://wikipedia.org/wiki/Geo_URI).
     */
    String toGeoUri() {
	NumberFormat df = new DecimalFormat("#0.000");
	return ("geo:"+df.format(_lat)+","+df.format(_lon)+";u="+df.format(_acc));
    }
    
    
    
}
