/*
  Pebble_sd - a simple accelerometer based seizure detector that runs on a
  Pebble smart watch (http://getpebble.com).

  See http://openseizuredetector.org for more information.

  Copyright Graham Jones, 2015.

  This file is part of pebble_sd.

  Pebble_sd is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  Pebble_sd is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with pebble_sd.  If not, see <http://www.gnu.org/licenses/>.

*/
package uk.org.openseizuredetector;

import android.os.Parcelable;
import android.os.Parcel;
import android.text.format.Time;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONObject;
import org.json.JSONArray;

/* based on http://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents */

public class SdData implements Parcelable {
    private final static String TAG = "SdData";
    /* Analysis settings */
    public boolean haveSettings = false;   // flag to say if we have received settings or not.
    public long alarmFreqMin;
    public long alarmFreqMax;
    public long nMin;
    public long nMax;
    public long warnTime;
    public long alarmTime;
    public long alarmThresh;
    public long alarmRatioThresh;
    public long batteryPc;

    /* Analysis results */
    public Time dataTime;
    public long alarmState;
    public long maxVal;
    public long maxFreq;
    public long specPower;
    public long roiPower;
    public String alarmPhrase;
    public int simpleSpec[];
    public boolean pebbleConnected = false;
    public boolean pebbleAppRunning = false;
    public boolean serverOK = false;

    public SdData() {
	simpleSpec = new int[10];
	dataTime = new Time(Time.getCurrentTimezone());
    }

    /*
     * Intialise this SdData object from a JSON String
     */
    public boolean fromJSON(String jsonStr) {
	Log.v(TAG,"fromJSON() - parsing jsonString - "+jsonStr);
	try {
	    JSONObject jo = new JSONObject(jsonStr);
	    Log.v(TAG,"fromJSON(): jo = "+jo.toString());
	    Log.v(TAG,"fromJSON(): dataTimeStr="+jo.optString("dataTimeStr"));
	    //Calendar cal = Calendar.getInstance();
	    //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddTHHmmss", Locale.UK);
	    //cal.setTime(sdf.parse(jo.optString("dataTimeStr")));
	    //dataTime = cal.getTime();
	    // FIXME - this doesn't work!!!
	    dataTime.setToNow();
	    Log.v(TAG,"fromJSON(): dataTime = "+dataTime.toString());
	    maxVal = jo.optInt("maxVal");
	    maxFreq = jo.optInt("maxFreq");
	    specPower = jo.optInt("specPower");
	    roiPower = jo.optInt("roiPower");
	    batteryPc = jo.optInt("batteryPc");
	    pebbleConnected = jo.optBoolean("pebbleConnected");
	    pebbleAppRunning = jo.optBoolean("pebbleAppRunning");
	    alarmState = jo.optInt("alarmState");
	    alarmPhrase = jo.optString("alarmPhrase");
	    JSONArray specArr = jo.optJSONArray("simpleSpec");
	    for (int i=0;i<specArr.length();i++) {
		simpleSpec[i] = specArr.optInt(i);
	    }
	    return true;
	} catch (Exception e) {
	    Log.v(TAG,"fromJSON() - error parsing result");
	    return false;
	}


    }


    public String toString() {
	return toDataString();
    }

    public String toDataString() {
	String retval;
	retval = "SdData.toDataString() Output";
		try {
		    JSONObject jsonObj = new JSONObject();
		    if (dataTime != null) {
			jsonObj.put("dataTime",dataTime.format("%d-%m-%Y %H:%M:%S"));
			jsonObj.put("dataTimeStr",dataTime.format("%Y%m%dT%H%M%S"));
		    } else {
			jsonObj.put("dataTimeStr","00000000T000000");
			jsonObj.put("dataTime","00-00-00 00:00:00");
		    }
		    Log.v(TAG,"sdData.dataTime = "+dataTime);
		    jsonObj.put("maxVal",maxVal);
		    jsonObj.put("maxFreq",maxFreq);
		    jsonObj.put("specPower",specPower);
		    jsonObj.put("roiPower",roiPower);
		    jsonObj.put("batteryPc",batteryPc);
		    jsonObj.put("pebbleConnected",pebbleConnected);
		    jsonObj.put("pebbleAppRunning",pebbleAppRunning);
		    jsonObj.put("alarmState",alarmState);
		    jsonObj.put("alarmPhrase",alarmPhrase);
		    JSONArray arr = new JSONArray();
		    for (int i=0;i<simpleSpec.length;i++) {
			arr.put(simpleSpec[i]);
		    }

		    jsonObj.put("simpleSpec",arr);

		    retval = jsonObj.toString();
		} catch (Exception ex) {
		    Log.v(TAG,"Error Creating Data Object - "+ex.toString());
		    retval = "Error Creating Data Object - "+ex.toString();
		}

	return(retval);
    }

    public int describeContents() {
	return 0;
    }

    public void writeToParcel(Parcel outParcel, int flags) {
	//outParcel.writeInt(fMin);
	//outParcel.writeInt(fMax);
    }

    private SdData(Parcel in) {
	//fMin = in.readInt();
	//fMax = in.readInt();
    }

    public static final Parcelable.Creator<SdData> CREATOR = new Parcelable.Creator<SdData>() {
	public SdData createFromParcel(Parcel in) {
	    return new SdData(in);
	}
	public SdData[] newArray(int size) {
	    return new SdData[size];
	}
    };

}
