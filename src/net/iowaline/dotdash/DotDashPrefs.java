package net.iowaline.dotdash;

import android.app.Activity;
import android.os.Bundle;

public class DotDashPrefs extends Activity {

	public static final String AUTOCAP = "autocap";
	public static final String NEWLINECODE = "newline";
	public static final String ENABLEUTILKBD = "enableutilkbd";

	/**
	 * Put this string in the settings array to represent a setting where no
	 * code group for newline is supported
	 */
	public static final String NEWLINECODE_NONE = "X";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, new DotDashSettingsFragment())
			.commit();
	}
}
