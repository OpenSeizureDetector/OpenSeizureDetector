/**
 * 
 */
package uk.org.openseizuredetector.wayn;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * @author graham
 *
 */
public class BatteryMonitor {
	public float getBatteryState(Context context) {
        Intent bat = context.registerReceiver(null, new
        		IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = bat.getIntExtra("level", 0);
        int scale = bat.getIntExtra("scale", 100);
        return level * 100 / scale;
	}

}
