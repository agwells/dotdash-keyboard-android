package net.iowaline.dotdash;

import java.io.FileDescriptor;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetFileDescriptor;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

public class DotDashIMEService extends InputMethodService implements
	KeyboardView.OnKeyboardActionListener, OnSharedPreferenceChangeListener {
//	private String TAG = "DotDashIMEService";
	private DotDashKeyboardView inputView;
	public DotDashKeyboard dotDashKeyboard;
	public Keyboard utilityKeyboard;
	private Keyboard.Key spaceKey;
	int spaceKeyIndex;
	private Keyboard.Key capsLockKey;
	int capsLockKeyIndex;
	private Hashtable<String, String> morseMap;
	private StringBuilder charInProgress;

	private static final int CAPS_LOCK_OFF = 0;
	private static final int CAPS_LOCK_NEXT = 1;
	private static final int CAPS_LOCK_ALL = 2;
	private int capsLockState = CAPS_LOCK_OFF;

	private static final int AUTO_CAP_MIDSENTENCE = 0;
	private static final int AUTO_CAP_SENTENCE_ENDED = 1;
	private int autoCapState = AUTO_CAP_MIDSENTENCE;
	
//	private static final String BULLET = "∙";
//	private static final String BULLET_OPERATOR="∙";
//	private static final String BLACK_CIRCLE="●";
	private static final String INTERPUNCT = "·";
//	private static final String UNICODE_HYPHEN="‐";
//	private static final String NONBREAKING_HYPHEN="‑";
//	private static final String HYPHEN_BULLET="⁃"; // Weird; round
//	private static final String MINUS_SIGN="−";
//	private static final String HORIZ_LINE_EXT="⎯";
//	private static final String HEAVY_MINUS="➖"; // Weird; looks gray
	private static final String EN_DASH="–";
//	private static final String EM_DASH="—";
	
	public static final String UNICODE_DOT = INTERPUNCT;
	public static final String UNICODE_DASH = EN_DASH;
	
	// Keycodes used in the utility keyboard
	public static final int KEYCODE_UP = -10;
	public static final int KEYCODE_LEFT = -11;
	public static final int KEYCODE_RIGHT = -12;
	public static final int KEYCODE_DOWN = -13;
	public static final int KEYCODE_HOME = -20;
	public static final int KEYCODE_END = -21;
	public static final int KEYCODE_DEL = -30;

	// Sync these with ditdahchars_values in arrays.xml
	public static final int DITDAHCHARS_UNICODE = 1;
	public static final int DITDAHCHARS_ASCII = 2;
	
	private SharedPreferences prefs;
	public String[] newlineGroups;
	public int ditdahcharsPref;
	private int maxCodeLength;

	private SoundPool soundpool;
	boolean loaded = false;
	private int dotSound;
	private int dashSound;
	public boolean iambic = false;
	public boolean iambicmodeb = false;
	public boolean autocommit = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
		
		// TODO: Fetch prefs via a background thread, as described here:
		// http://stackoverflow.com/questions/4371273/should-accessing-sharedpreferences-be-done-off-the-ui-thread
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(this);
		this.ditdahcharsPref = Integer.valueOf(this.prefs.getString(DotDashPrefs.DITDAHCHARS, Integer.toString(DITDAHCHARS_UNICODE)));
		this.iambic = this.prefs.getBoolean("iambic", false);
		this.iambicmodeb = this.prefs.getBoolean("iambicmodeb", false);
		this.autocommit = this.prefs.getBoolean("autocommit", false);

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
		// Aaron Wells' custom additions to Morse code
		morseMap.put("....--", "#");
		morseMap.put("-.-.-", "*");
		morseMap.put("..-..", "[");
		morseMap.put("..-..-", "]");
		morseMap.put(".--.-", "{");
		morseMap.put(".--.--", "}");
		morseMap.put("--.--", "<");
		morseMap.put("--.--.", ">");
		morseMap.put("...--.-", "~");
		morseMap.put(".--..-.", "%");
		morseMap.put(".--.---", "^");
		morseMap.put(".-..-", "\\");
		morseMap.put(".--...", "|");

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

	/**
	 * Create (or nullify) the utility keyboard, depending on user's preferences.
	 * 
	 * @return True if the keyboard changed.
	 */
	private boolean setupUtilityKeyboard() {
		boolean nullBefore = (utilityKeyboard == null);
		if (this.prefs.getBoolean(DotDashPrefs.DASHKEYONLEFT, false)) {
			utilityKeyboard = new Keyboard(this, R.xml.utilitykeyboard); 
		} else {
			utilityKeyboard = null;
		}
		
		if (nullBefore && (utilityKeyboard == null)) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		super.onInitializeInterface();
		this.setupUtilityKeyboard();
		dotDashKeyboard = new DotDashKeyboard(this, R.xml.dotdash);
		dotDashKeyboard.setupDotDashKeys(this.prefs.getBoolean(DotDashPrefs.DASHKEYONLEFT, false));
		
		spaceKey = dotDashKeyboard.spaceKey;
		capsLockKey = dotDashKeyboard.capsLockKey;
		List<Keyboard.Key> keys = dotDashKeyboard.getKeys();
		spaceKeyIndex = keys.indexOf(spaceKey);
		capsLockKeyIndex = keys.indexOf(capsLockKey);
		if (isAudio()) {
			loadSoundPool();
		}
	}

	private void loadSoundPool() {
		soundpool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
		soundpool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				loaded = true;
			}
		});
		AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.tone800hz);
		dotSound = soundpool.load(fd.getFileDescriptor(), fd.getStartOffset(), (long)(fd.getLength() * 0.1), 1);
		dashSound = soundpool.load(fd.getFileDescriptor(), fd.getStartOffset(), (long)(fd.getLength() * 0.3), 1);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateInputView() {
		inputView = (DotDashKeyboardView) getLayoutInflater().inflate(
				R.layout.input, null);
		inputView.setOnKeyboardActionListener(this);
		inputView.setKeyboard(dotDashKeyboard);
		inputView.setService(this);
		inputView.mEnableUtilityKeyboard = prefs.getBoolean(
				DotDashPrefs.ENABLEUTILKBD, false);
		return inputView;
	}

	public void onKey(int primaryCode, int[] keyCodes) {
		int kbd = inputView.whichKeyboard();
		if (kbd == DotDashKeyboardView.KBD_DOTDASH) {
			onKeyMorse(primaryCode, keyCodes);
		} else if (kbd == DotDashKeyboardView.KBD_UTILITY) {
			onKeyUtility(primaryCode, keyCodes);
		}
	}

	/**
	 * Handle key input on the utility keyboard. Keys with a positive keycode
	 * are meant to be passed through String.valueOf(), while keys with negative
	 * keycodes must be specially processed
	 * 
	 * @param primaryCode
	 * @param keyCodes
	 */
	public void onKeyUtility(int primaryCode, int[] keyCodes) {
		if (primaryCode > 0) {
			getCurrentInputConnection().commitText(
					String.valueOf((char) primaryCode), 1);
		} else {
			switch (primaryCode) {
			case KEYCODE_UP:
				sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
				break;
			case KEYCODE_LEFT:
				sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
				break;
			case KEYCODE_RIGHT:
				sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
				break;
			case KEYCODE_DOWN:
				sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
				break;
			case KEYCODE_DEL:
				sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
				break;
			case KEYCODE_HOME:
				getCurrentInputConnection().setSelection(0, 0);
				break;
			case KEYCODE_END:
				ExtractedText et = getCurrentInputConnection()
						.getExtractedText(new ExtractedTextRequest(), 0);
				if (et != null) {
					int length = et.text.length();
					getCurrentInputConnection().setSelection(length, length);
				}
				break;
			}

		}
	}

	/**
	 * Handle key input on the Morse Code keyboard. It has 5 keys and each of
	 * them does something different.
	 * 
	 * @param primaryCode
	 * @param keyCodes
	 */
	public void onKeyMorse(int primaryCode, int[] keyCodes) {
		// Log.d(TAG, "primaryCode: " + Integer.toString(primaryCode));
		//String curCharMatch = morseMap.get(charInProgress.toString());

		switch (primaryCode) {

			// 0 represents a dot, 1 represents a dash
			// TODO The documentation for Keyboard.Key says I should be
			// able to give a key a string as a keycode, but it
			// errors out every time I try it.
			case DotDashKeyboard.KEYCODE_DOT:
			case DotDashKeyboard.KEYCODE_DASH:
	
				if (charInProgress.length() < maxCodeLength) {
					charInProgress.append(primaryCode == DotDashKeyboard.KEYCODE_DASH ? "-" : ".");
					updateSpaceKey(true);
				}
				
				if (loaded && isAudio()) {
					int soundid;
					if (primaryCode == DotDashKeyboard.KEYCODE_DOT) {
						soundid = dotSound;
					} else {
						soundid = dashSound;
					}

					AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
					if (!prefs.getBoolean("audioonlyonheadphones", true) || audioManager.isWiredHeadsetOn()) {
						float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
						float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
						float volume = actualVolume / maxVolume;
						soundpool.play(soundid, volume, volume, 1, 0, 1f);
					}
				}

				// Log.d(TAG, "charInProgress: " + charInProgress);
				break;
	
			// Space button ends the current dotdash sequence
			// Space twice in a row sends through a standard space character
			case KeyEvent.KEYCODE_SPACE:
				inputView.handler.removeMessages(DotDashKeyboardView.MSG_AUTOCOMMIT);
				inputView.handler.removeMessages(DotDashKeyboardView.MSG_IAMBIC_PLAYING);
				inputView.iambic_both_pressed = false;
				
				if (charInProgress.length() == 0) {
					getCurrentInputConnection().commitText(" ", 1);
	
					if (autoCapState == AUTO_CAP_SENTENCE_ENDED
							&& prefs.getBoolean(DotDashPrefs.AUTOCAP, false)) {
						capsLockState = CAPS_LOCK_NEXT;
						updateCapsLockKey(true);
					}
				} else {
					commitCodeGroup(false);
				}
				break;
	
			// If there's a character in progress, clear it
			// otherwise, send through a backspace keypress
			case KeyEvent.KEYCODE_DEL:
				inputView.handler.removeMessages(DotDashKeyboardView.MSG_AUTOCOMMIT);
				inputView.handler.removeMessages(DotDashKeyboardView.MSG_IAMBIC_PLAYING);
				inputView.iambic_both_pressed = false;

				if (charInProgress.length() > 0) {
					clearCharInProgress();
					updateSpaceKey(true);
				} else {
					sendDownUpKeyEvents(primaryCode);
	
					if (capsLockState == CAPS_LOCK_NEXT) {
						// If you've hit delete and you were in caps_next state,
						// then caps_off
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
				updateCapsLockKey(false);
				break;
		}
	}

	public boolean isAudio() {
		return prefs.getBoolean("audio", false);
	}

	public void commitCodeGroup(boolean refreshScreen) {
		if (charInProgress.length() == 0) {
			return;
		}

		String curCharMatch  = morseMap.get(charInProgress.toString());
		if (curCharMatch == null) {
			return;
		}
			
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
				// Since we only support the Latin alphabet, I may as well use Locale.US
				curCharMatch = curCharMatch.toUpperCase(Locale.US);
			}

			// Log.d(TAG, "Char identified as " + curCharMatch);
			InputConnection ic = getCurrentInputConnection();
			if (ic != null) {
				ic.commitText(curCharMatch, curCharMatch.length());
			}
					
		}

		clearCharInProgress();
		updateSpaceKey(refreshScreen);
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
		
		if (refreshScreen) {

			// Wrapping this in a try/catch block to avoid crashes in Android
			// 2.1 and earlier, and inexplicable NullPointerExceptions
			try {
				inputView.invalidateKey(capsLockKeyIndex);
			} catch (Exception e) {
				// It doesn't matter if the operation failed, so just ignore
				// this
			}
		}
	}

	/**
	 * Updates the spacebar to display the current character in progress
	 * 
	 * @param boolean refreshScreen Whether or not to refresh the screen afterwards.
	 */
	public void updateSpaceKey(boolean refreshScreen) {
		String newLabel = charInProgress.toString();
		
		// Workaround to maintain consistent styling. Android puts multi-character
		// labels in bold, and single-characters in non-bold. To make the bold state
		// consistent, we turn our single-character label into a three-character one
		// by padding it with spaces.
		if (newLabel.length() == 1) {
			newLabel = " " + newLabel + " ";
		}
		
		if (!spaceKey.label.toString().equals(newLabel)) {
			// Log.d(TAG, "!spaceKey.label.equals(charInProgress)");
			if (newLabel.length() > 0 && ditdahcharsPref == DITDAHCHARS_UNICODE) {
				newLabel = convertDitDahAsciiToUnicode(newLabel);
			}
			spaceKey.label = newLabel;
			if (refreshScreen) {
				// Wrapping this in a try/catch block to avoid crashes in
				// Android 2.1 and earlier
				try {
					inputView.invalidateKey(spaceKeyIndex);
				} catch (IllegalArgumentException iae) {
					// It doesn't matter if the operation failed, so just ignore
					// this
				}
			}
		}
	}

	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);
	}

	public void onStartInputView(android.view.inputmethod.EditorInfo info,
			boolean restarting) {
		// Log.d(TAG, "onStartInputView");
		super.onStartInputView(info, restarting);

//		// Wrapping this in a try/catch block to avoid crashes in Android 2.1
//		// and earlier
		updateAutoCap();
		updateCapsLockKey(true);
		updateSpaceKey(true);
	};

	@Override
	public void onFinishInputView(boolean finishingInput) {
		// Log.d(TAG, "onFinishInputView");
		this.inputView.closeCheatSheet();
		super.onFinishInputView(finishingInput);
		clearEverything();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.contentEquals(DotDashPrefs.NEWLINECODE)) {
			updateNewlinePref();
		} else if (key.contentEquals(DotDashPrefs.ENABLEUTILKBD)) {
			this.setupUtilityKeyboard();
			if (this.inputView != null) {
				inputView.mEnableUtilityKeyboard = prefs.getBoolean(key, false);
				if (!prefs.getBoolean(key, false)) {
					inputView.setKeyboard(dotDashKeyboard);
				}
			}
		} else if (key.contentEquals(DotDashPrefs.DITDAHCHARS)) {
			this.ditdahcharsPref = Integer.valueOf(this.prefs.getString(DotDashPrefs.DITDAHCHARS, Integer.toString(DITDAHCHARS_UNICODE)));
			if (inputView != null) {
				inputView.clearCheatSheet();
			}
		} else if (key.contentEquals(DotDashPrefs.DASHKEYONLEFT)) {
			boolean changed = this.dotDashKeyboard.setupDotDashKeys(this.prefs.getBoolean(key, false));
			if (changed && this.inputView != null) {
				this.inputView.invalidateAllKeys();
			}
		} else if (key.contentEquals("audio")) {
			if (prefs.getBoolean(key, false)) {
				loaded = false;
				loadSoundPool();
			} else {
				if (soundpool != null) {
					soundpool.release();
					soundpool = null;
					loaded = false;
				}
			}
		} else if (key.contentEquals("iambic")) {
			this.iambic = prefs.getBoolean(key, false);
		} else if (key.contentEquals("iambicmodeb")) {
			this.iambicmodeb = prefs.getBoolean(key, false);
		} else if (key.contentEquals("autocommit")) {
			this.autocommit = prefs.getBoolean(key, false);
		}
	}

	/**
	 * Updates the newline character stored in morseMap, based on the user's
	 * current preferences.
	 * 
	 * Not sure how I'm going to support this when I switch the codes to a
	 * selectable XML system...
	 */
	private void updateNewlinePref() {
		// Remove the old ones
		if (newlineGroups != null) {
			for (String s : newlineGroups) {
				morseMap.remove(s);
			}
		}

		// Add the new ones
		// TODO: When we make the morse codes into XML, this'll have to be
		// updated
		String rawpref = this.prefs.getString(DotDashPrefs.NEWLINECODE, ".-.-");
		// Log.d(TAG, "rawpref: "+rawpref);
		if (rawpref.contentEquals(DotDashPrefs.NEWLINECODE_NONE)) {
			newlineGroups = null;
		} else {
			newlineGroups = rawpref.split("\\|");
			// Log.d(TAG, "nl: " + newlineGroups[0]);
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
	
	/**
	 * The cursor position (selection position) has changed
	 */
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
				candidatesStart, candidatesEnd);
		updateAutoCap();
	}

	/**
	 * Update the shift state if autocap is turned on, based on current cursor
	 * position (using InputConnection.getCursorCapsMode())
	 */
	public void updateAutoCap() {

		// Autocap has no effect if Caps Lock is on
		if (capsLockState == CAPS_LOCK_ALL) {
			return;
		}

		// Don't bother with any of this is autocap is turned off
		if (!prefs.getBoolean(DotDashPrefs.AUTOCAP, false)) {
			return;
		}

		int origCapsLockState = capsLockState;
		int newCapsLockState = CAPS_LOCK_OFF;

		EditorInfo ei = getCurrentInputEditorInfo();
		if (ei != null
				&& ei.inputType != EditorInfo.TYPE_NULL
				&& getCurrentInputConnection().getCursorCapsMode(ei.inputType) > 0) {
			newCapsLockState = CAPS_LOCK_NEXT;
		}
		capsLockState = newCapsLockState;
		if (capsLockState != origCapsLockState) {
			updateCapsLockKey(true);
		}
	}

	/**
	 * Converts a string of ASCII ditdahs to Unicode
	 * 
	 * @param ascii
	 * @return
	 */
	protected String convertDitDahAsciiToUnicode(String ascii) {
		return ascii
				.replace(".", DotDashIMEService.UNICODE_DOT)
				.replace("-", DotDashIMEService.UNICODE_DASH);
	}
	
	/**
	 * Converts a string of Unicode ditdahs to ASCII
	 * 
	 * @param unicode The original string with unicode ditdahs
	 * @param padding Whether to pad with spaces
	 * @return
	 */
	protected String convertDitDahUnicodeToAscii(String unicode, boolean padding) {
		return unicode
				.replace(DotDashIMEService.UNICODE_DOT, (padding ? ". " : "."))
				.replace(DotDashIMEService.UNICODE_DASH, (padding ? "- " : "-"))
				.trim();
	}
}
