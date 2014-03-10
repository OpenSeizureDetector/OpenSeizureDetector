package uk.org.openseizuredetector.bentv;
import android.preference.PreferenceActivity;
import android.os.Bundle;

public class PrefActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
