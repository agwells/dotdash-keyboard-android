package net.iowaline.dotdash;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class DotDashPrefs extends PreferenceActivity {
	
	public static final String AUTOCAP = "autocap";
	public static final String NEWLINECODE = "newline";
	
	/**
	 * Put this string in the settings array to
	 * represent a setting where no code group for
	 * newline is supported 
	 */
	public static final String NEWLINECODE_NONE = "X";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}
}
