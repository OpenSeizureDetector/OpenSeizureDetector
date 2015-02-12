package uk.org.openseizuredetector;


import android.os.Parcelable;
import android.os.Parcel;
import android.text.format.Time;

/* based on http://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents */

public class SdData implements Parcelable {
    /* Analysis settings */
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

    public SdData() {
	simpleSpec = new int[10];
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
