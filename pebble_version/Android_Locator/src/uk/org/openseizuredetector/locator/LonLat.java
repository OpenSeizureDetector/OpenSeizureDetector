package uk.org.openseizuredetector.locator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LonLat {
    public final double _lon;  //longitude in degrees
    public final double _lat;  // latitude in degrees
    public final float _acc;  // fix accuracy in metres.
    public final Date _date;  // date/time of fix.
    public String _provider;
    public double lon() {return _lon;};
    public double lat() {return _lat;};
    public String date() {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);
	return sdf.format(_date);
    };
    public LonLat(double llon,double llat, float lacc, String provider, Date ldate) {
	_lon = llon;
	_lat = llat;
	_acc = lacc;
	_date = ldate;
	_provider = provider;
    }

    /**
     * Returns location as a human readable string 
     */
    String toStr() {
	NumberFormat df = new DecimalFormat("#0.000");
	return ("lon="+df.format(_lon)+", lat="+df.format(_lat)+":  accuracy="+df.format(_acc)+" m ("+_provider+")");
    }

    /**
     * Returns location as a Geo URI (http://wikipedia.org/wiki/Geo_URI).
     */
    String toGeoUri() {
	NumberFormat df = new DecimalFormat("#0.000");
	return ("geo:"
		+df.format(_lat)+","+df.format(_lon)
		+";u="+df.format(_acc)
		// The ?q parameter does not display a marker in osmand so
		// don't bother sending it.
		//+"?q="+df.format(_lat)+","+df.format(_lon)

		);
    }
    
    
    
}
