package net.iowaline.dotdash;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class DotDashSettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}
}
