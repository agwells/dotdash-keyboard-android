package net.iowaline.dotdash;

import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class DotDashIMEService extends InputMethodService implements
		KeyboardView.OnKeyboardActionListener, OnSharedPreferenceChangeListener {
	private String TAG = "DotDashIMEService";
	private DotDashKeyboardView inputView;
	private DotDashKeyboard dotDashKeyboard;
	private Keyboard.Key spaceKey;
	int spaceKeyIndex;
	private Keyboard.Key capsLockKey;
	int capsLockKeyIndex;
	private Hashtable<String, String> morseMap;
	private StringBuilder charInProgress;

	private static final int CAPS_LOCK_OFF = 0;
	private static final int CAPS_LOCK_NEXT = 1;
	private static final int CAPS_LOCK_ALL = 2;
	private Integer capsLockState = CAPS_LOCK_OFF;
	
	private static final int AUTO_CAP_MIDSENTENCE = 0;
	private static final int AUTO_CAP_SENTENCE_ENDED = 1;
	private Integer autoCapState = AUTO_CAP_MIDSENTENCE;

	// Keycodes used in the utility keyboard
	public static final int KEYCODE_UP = -10;
	public static final int KEYCODE_DLEFT = -11;
	public static final int KEYCODE_DRIGHT = -12;
	public static final int KEYCODE_DDOWN = -13;
	public static final int KEYCODE_HOME = -20;
	public static final int KEYCODE_END = -21;
	public static final int KEYCODE_DEL = -30;
	
	private SharedPreferences prefs;
	public String[] newlineGroups;
	private int maxCodeLength;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		super.onInitializeInterface();
		dotDashKeyboard = new DotDashKeyboard(this, R.xml.dotdash);
		spaceKey = dotDashKeyboard.getSpaceKey();
		capsLockKey = dotDashKeyboard.getCapsLockKey();
		List<Keyboard.Key> keys = dotDashKeyboard.getKeys();
		spaceKeyIndex = keys.indexOf(spaceKey);
		capsLockKeyIndex = keys.indexOf(capsLockKey);

		// TODO Replace this with an XML file
		morseMap = new Hashtable<String, String>();
		morseMap.put(".-", "a");
		morseMap.put("-...", "b");
		morseMap.put("-.-.", "c");
		morseMap.put("-..", "d");
		morseMap.put(".", "e");
		morseMap.put("..-.", "f");
		morseMap.put("--.", "g");
		morseMap.put("....", "h");
		morseMap.put("..", "i");
		morseMap.put(".---", "j");
		morseMap.put("-.-", "k");
		morseMap.put(".-..", "l");
		morseMap.put("--", "m");
		morseMap.put("-.", "n");
		morseMap.put("---", "o");
		morseMap.put(".--.", "p");
		morseMap.put("--.-", "q");
		morseMap.put(".-.", "r");
		morseMap.put("...", "s");
		morseMap.put("-", "t");
		morseMap.put("..-", "u");
		morseMap.put("...-", "v");
		morseMap.put(".--", "w");
		morseMap.put("-..-", "x");
		morseMap.put("-.--", "y");
		morseMap.put("--..", "z");
		morseMap.put(".----", "1");
		morseMap.put("..---", "2");
		morseMap.put("...--", "3");
		morseMap.put("....-", "4");
		morseMap.put(".....", "5");
		morseMap.put("-....", "6");
		morseMap.put("--...", "7");
		morseMap.put("---..", "8");
		morseMap.put("----.", "9");
		morseMap.put("-----", "0");
		morseMap.put(".----.", "\'");
		morseMap.put(".--.-.", "@");
		morseMap.put(".-...", "&");
		morseMap.put("---...", ":");
		morseMap.put("--..--", ",");
		morseMap.put("...-..-", "$");
		morseMap.put("-...-", "=");
		morseMap.put("---.", "!");
		morseMap.put("-.-.--", "!");
		morseMap.put("-....-", "-");
		morseMap.put("-.--.", "(");
		morseMap.put("-.--.-", ")");
		morseMap.put(".-.-.-", ".");
		morseMap.put(".-.-.", "+");
		morseMap.put("..--..", "?");
		morseMap.put(".-..-.", "\"");
		morseMap.put("-.-.-.", ";");
		morseMap.put("-..-.", "/");
		morseMap.put("..--.-", "_");

		updateNewlinePref();
		
		// This variable is used in onKey to determine how many
		// dots and dashes we need to keep track of (no need recording
		// more than the total number that make up a valid code group)
		maxCodeLength = 0;
		for (String codegroup : morseMap.keySet()) {
			if (codegroup.length() > maxCodeLength) {
				maxCodeLength = codegroup.length();
			}
		}
		charInProgress = new StringBuilder(maxCodeLength);
	}

	@Override
	public View onCreateInputView() {
		inputView = (DotDashKeyboardView) getLayoutInflater().inflate(
				R.layout.input, null);
		inputView.setOnKeyboardActionListener(this);
		inputView.setKeyboard(dotDashKeyboard);
		inputView.setService(this);
		return inputView;
	}

	public void onKey(int primaryCode, int[] keyCodes) {
		// Log.d(TAG, "primaryCode: " + Integer.toString(primaryCode));
		String curCharMatch = morseMap.get(charInProgress.toString());
		boolean startAutoCap = false;

		switch (primaryCode) {

		// 0 represents a dot, 1 represents a dash
		// TODO The documentation for Keyboard.Key says I should be
		// able to give a key a string as a keycode, but it
		// errors out every time I try it.
		case 0:
		case 1:

			if (charInProgress.length() < maxCodeLength) {
				charInProgress.append(primaryCode == 1 ? "-" : ".");
			}
			// Log.d(TAG, "charInProgress: " + charInProgress);
			break;

		// Space button ends the current dotdash sequence
		// Space twice in a row sends through a standard space character
		case KeyEvent.KEYCODE_SPACE:
			if (charInProgress.length() == 0) {
				getCurrentInputConnection().commitText(" ", 1);
				
				if (autoCapState == AUTO_CAP_SENTENCE_ENDED && prefs.getBoolean(DotDashPrefs.AUTOCAP, false)) {
					capsLockState = CAPS_LOCK_NEXT;
					updateCapsLockKey(true);
				}
			} else {
				// Log.d(TAG, "Pressed space, look for " +
				// charInProgress.toString());

				if (curCharMatch != null) {

					if (curCharMatch.contentEquals("\n")) {
						sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
					} else if (curCharMatch.contentEquals("END")) {
						requestHideSelf(0);
						inputView.closing();
					} else {

						boolean uppercase = false;
						if (capsLockState == CAPS_LOCK_NEXT) {
							uppercase = true;
							capsLockState = CAPS_LOCK_OFF;
							updateCapsLockKey(true);
						} else if (capsLockState == CAPS_LOCK_ALL) {
							uppercase = true;
						}
						if (uppercase) {
							curCharMatch = curCharMatch.toUpperCase();
						}
						
						// TODO: When you move the morse code groups to XML, move this to XML too
						if (curCharMatch == "?" || curCharMatch == "." || curCharMatch == "!") {
							startAutoCap = true;
						}
						
						// Log.d(TAG, "Char identified as " + curCharMatch);
						getCurrentInputConnection().commitText(curCharMatch,
								curCharMatch.length());
					}
				}
			}
			clearCharInProgress();
			break;

		// If there's a character in progress, clear it
		// otherwise, send through a backspace keypress
		case KeyEvent.KEYCODE_DEL:
			if (charInProgress.length() > 0) {
				clearCharInProgress();
			} else {
				sendDownUpKeyEvents(primaryCode);
				clearCharInProgress();
				updateSpaceKey(true);
				
				CharSequence cs = getCurrentInputConnection().getTextBeforeCursor(1, 0);
				if (cs != null && cs.length()==0) {
					capsLockState = CAPS_LOCK_NEXT;
					updateCapsLockKey(true);
				} else if (capsLockState == CAPS_LOCK_NEXT) {
					capsLockState = CAPS_LOCK_OFF;
					updateCapsLockKey(true);
				}
			}
			break;

		case KeyEvent.KEYCODE_SHIFT_LEFT:
			switch (capsLockState) {
			case CAPS_LOCK_OFF:
				capsLockState = CAPS_LOCK_NEXT;
				break;
			case CAPS_LOCK_NEXT:
				capsLockState = CAPS_LOCK_ALL;
				break;
			default:
				capsLockState = CAPS_LOCK_OFF;
			}
			updateCapsLockKey(true);
			break;
		}

		updateSpaceKey(true);
		
		if (startAutoCap) {
			autoCapState = AUTO_CAP_SENTENCE_ENDED;
		} else {
			autoCapState = AUTO_CAP_MIDSENTENCE;
		}
	}

	private void clearCharInProgress() {
		charInProgress.setLength(0);
	}

	public void onPress(int arg0) {
		// TODO Auto-generated method stub

	}

	public void onRelease(int arg0) {
		// TODO Auto-generated method stub

	}

	public void onText(CharSequence arg0) {
		// TODO Auto-generated method stub

	}

	public void swipeDown() {
		// TODO Auto-generated method stub

	}

	public void swipeLeft() {
		// TODO Auto-generated method stub

	}

	public void swipeRight() {
		// TODO Auto-generated method stub

	}

	public void swipeUp() {
		// TODO Auto-generated method stub

	}

	public void clearEverything() {
		clearCharInProgress();
		capsLockState = CAPS_LOCK_OFF;
		updateCapsLockKey(false);
		updateSpaceKey(false);
	}

	public void updateCapsLockKey(boolean refreshScreen) {

		CharSequence oldLabel = capsLockKey.label;

		Context context = this.getApplicationContext();
		switch (capsLockState) {
		case CAPS_LOCK_OFF:
			capsLockKey.on = false;
			capsLockKey.label = context.getText(R.string.caps_lock_off);
			break;
		case CAPS_LOCK_NEXT:
			capsLockKey.on = false;
			capsLockKey.label = context.getText(R.string.caps_lock_next);
			break;
		case CAPS_LOCK_ALL:
			capsLockKey.on = true;
			capsLockKey.label = context.getText(R.string.caps_lock_all);
			break;
		}

		if (refreshScreen && !capsLockKey.label.equals(oldLabel)) {
			inputView.invalidateKey(capsLockKeyIndex);
		}
	}

	public void updateSpaceKey(boolean refreshScreen) {
		if (!spaceKey.label.toString().equals(charInProgress.toString())) {
			// Log.d(TAG, "!spaceKey.label.equals(charInProgress)");
			spaceKey.label = charInProgress.toString();
			if (refreshScreen) {
				inputView.invalidateKey(spaceKeyIndex);
			}
		}
	}

	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		if (prefs.getBoolean(DotDashPrefs.AUTOCAP, false) && attribute.initialCapsMode != 0) {
			capsLockState = CAPS_LOCK_NEXT;
		}
		super.onStartInput(attribute, restarting);
	}
	
	public void onStartInputView(android.view.inputmethod.EditorInfo info,
			boolean restarting) {
//		Log.d(TAG, "onStartInputView");
		super.onStartInputView(info, restarting);
		inputView.invalidateKey(spaceKeyIndex);
		updateCapsLockKey(true);
	};

	@Override
	public void onFinishInputView(boolean finishingInput) {
//		Log.d(TAG, "onFinishInputView");
		this.inputView.closeCheatSheet();
		super.onFinishInputView(finishingInput);
		clearEverything();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
//		Log.d(TAG, "prefchange: "+key);
		if (key.contentEquals(DotDashPrefs.NEWLINECODE)) {
			updateNewlinePref();
		}
	}

	/**
	 * Updates the newline character stored in morseMap, based on
	 * the user's current preferences.
	 * 
	 * Not sure how I'm going to support this when I switch the codes
	 * to a selectable XML system...
	 */
	private void updateNewlinePref() {
		// Remove the old ones
		if (newlineGroups != null) {
			for (String s : newlineGroups) {
				morseMap.remove(s);
			}
		}
		
		// Add the new ones
		String rawpref = this.prefs.getString(DotDashPrefs.NEWLINECODE, "what?");
//		Log.d(TAG, "rawpref: "+rawpref);
		if (rawpref.contentEquals(DotDashPrefs.NEWLINECODE_NONE)) {
			newlineGroups = null;
		} else {
			newlineGroups = rawpref.split("\\|");
//			Log.d(TAG, "nl: " + newlineGroups[0]);
		}
		
		if (newlineGroups != null) {
			for (String s : newlineGroups) {
				morseMap.put(s, "\n");
			}
		}
		if (inputView != null) {
			inputView.updateNewlineCode();
		}
	}
}
