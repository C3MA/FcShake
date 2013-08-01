package de.c3ma.fc.android.fcshake;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * created at 01.08.2013 - 10:51:24<br />
 * creator: ollo<br />
 * project: FcShake<br />
 * $Id: $<br />
 * @author ollo<br />
 */
public class Preferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
    }
}
