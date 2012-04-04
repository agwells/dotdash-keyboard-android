package net.iowaline.dotdash;

import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import net.iowaline.dotdash.R;

public class DotDashIMEService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
//	private String TAG = "DotDashIMEService";
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
		charInProgress = new StringBuilder(7);
		
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
		morseMap.put(".----.",  "\'");
		morseMap.put(".--.-.",  "@");
		morseMap.put(".-...",   "&");
		morseMap.put("---...",  ":");
		morseMap.put("--..--",  ",");
		morseMap.put("...-..-", "$");
		morseMap.put("-...-",   "=");
		morseMap.put("---.",    "!");
		morseMap.put("-.-.--",  "!");
		morseMap.put("-....-",  "-");
		morseMap.put("-.--.",   "(");
		morseMap.put("-.--.-",  ")");
		morseMap.put(".-.-.-",  ".");
//		morseMap.put(".-.-.",   "+");
		morseMap.put("..--..",  "?");
		morseMap.put(".-..-.",  "\"");
		morseMap.put("-.-.-.",  ";");
		morseMap.put("-..-.",   "/");
		morseMap.put("..--.-",  "_");

		// Specially handled
		// The AA prosign, "space down one line" 
		morseMap.put(".-.-",   "\n");
		morseMap.put(".-.-..", "\n");
		// The AR prosign, "end of message"
		morseMap.put(".-.-.",  "END");
	}
	
	@Override
	public View onCreateInputView() {
		inputView = (DotDashKeyboardView) getLayoutInflater().inflate(
				R.layout.input, null
		);
		inputView.setOnKeyboardActionListener(this);
		inputView.setKeyboard(dotDashKeyboard);
		inputView.setPreviewEnabled(false);
		inputView.setService(this);
		return inputView;
	}
	
	public void onKey(int primaryCode, int[] keyCodes) {
//		Log.d(TAG, "primaryCode: " + Integer.toString(primaryCode));
		String curCharMatch = morseMap.get(charInProgress.toString());

		switch(primaryCode) {
		
			// 0 represents a dot, 1 represents a dash
			// TODO The documentation for Keyboard.Key says I should be
			// able to give a key a string as a keycode, but it
			// errors out every time I try it.
			case 0:
			case 1:
				
				if (charInProgress.length() < 7) {
					charInProgress.append(primaryCode==1 ? "-" : ".");
				}
//				Log.d(TAG, "charInProgress: " + charInProgress);
				break;

			// Space button ends the current dotdash sequence
			// Space twice in a row sends through a standard space character
			case KeyEvent.KEYCODE_SPACE:
				if (charInProgress.length()==0) {
					getCurrentInputConnection().commitText(" ", 1);
				} else {
//					Log.d(TAG, "Pressed space, look for " + charInProgress.toString());
					
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
								updateCapsLockKey();
							} else if (capsLockState == CAPS_LOCK_ALL) {
								uppercase = true;
							}
							if (uppercase) {
								curCharMatch = curCharMatch.toUpperCase();
							}
//							Log.d(TAG, "Char identified as " + curCharMatch);
							getCurrentInputConnection().commitText(curCharMatch, curCharMatch.length());
						}
					}
				}
				clearCharInProgress();
				break;
			
			// If there's a character in progress, clear it
			// otherwise, send through a backspace keypress
			case KeyEvent.KEYCODE_DEL:
				if (charInProgress.length() > 0 ){
					clearCharInProgress();
				} else {
					sendDownUpKeyEvents(primaryCode);
					clearEverything();
				}
				break;
				
			case KeyEvent.KEYCODE_SHIFT_LEFT:
				switch( capsLockState ) {
					case CAPS_LOCK_OFF:
						capsLockState = CAPS_LOCK_NEXT;
						break;
					case CAPS_LOCK_NEXT:
						capsLockState = CAPS_LOCK_ALL;
						break;
					default:
						capsLockState = CAPS_LOCK_OFF;
				}
				updateCapsLockKey();
				break;
		}
		
		updateSpaceKey();
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
		updateCapsLockKey();
		updateSpaceKey();
	}

	public void updateCapsLockKey(){

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
		
		if (!capsLockKey.label.equals(oldLabel)) {
			inputView.invalidateKey(capsLockKeyIndex);
		}
	}
	
	public void updateSpaceKey() {
		if (!spaceKey.label.toString().equals(charInProgress.toString())) {
//			Log.d(TAG, "!spaceKey.label.equals(charInProgress)");
			spaceKey.label = charInProgress.toString();
			inputView.invalidateKey(spaceKeyIndex);
		}
	}
	
	@Override
	public void onFinishInputView(boolean finishingInput) {
		clearEverything();
		super.onFinishInputView(finishingInput);
	}
}
