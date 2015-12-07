package org.pocketworkstation.pckeyboard;

/*
 * Copyright (C) 2010, authors of the Hacker's Keyboard project: http://code.google.com/p/hackerskeyboard/ 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

public class AutoSummaryListPreference extends ListPreference {
	private static final String TAG = "HK/AutoSummaryListPreference";

	public AutoSummaryListPreference(Context context) {
		super(context);
	}

	public AutoSummaryListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private void trySetSummary() {
		CharSequence entry = null;
		try {
			entry = getEntry();
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.i(TAG, "Malfunctioning ListPreference, can't get entry");
		}
		if (entry != null) {
			// String percent = getResources().getString(R.string.percent);
			String percent = "percent";
			setSummary(entry.toString().replace("%", " " + percent));
		}
	}

	@Override
	public void setEntries(CharSequence[] entries) {
		super.setEntries(entries);
		trySetSummary();
	}

	@Override
	public void setEntryValues(CharSequence[] entryValues) {
		super.setEntryValues(entryValues);
		trySetSummary();
	}

	@Override
	public void setValue(String value) {
		super.setValue(value);
		trySetSummary();
	}
}
