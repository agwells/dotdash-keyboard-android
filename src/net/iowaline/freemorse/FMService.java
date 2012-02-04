package net.iowaline.freemorse;

import java.util.Hashtable;
import java.util.List;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class FMService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
	private String TAG = "FMService";
	private KeyboardView inputView;
	private DotDashKeyboard dotDashKeyboard;
	private Keyboard.Key spaceKey;
	int spaceKeyIndex;
	private Hashtable<String, String> morseMap;
	private StringBuilder charInProgress;
	private Boolean capsLockDown = false;
	
	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		super.onInitializeInterface();
		dotDashKeyboard = new DotDashKeyboard(this, R.xml.dotdash);
		spaceKey = dotDashKeyboard.getSpaceKey();
		List<Keyboard.Key> keys = dotDashKeyboard.getKeys();
		spaceKeyIndex = keys.indexOf(spaceKey);
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
		inputView = (KeyboardView) getLayoutInflater().inflate(
				R.layout.input, null
		);
		inputView.setOnKeyboardActionListener(this);
		inputView.setKeyboard(dotDashKeyboard);
		inputView.setPreviewEnabled(false);
		return inputView;
	}
	
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		super.onStartInputView(info, restarting);
		charInProgress = new StringBuilder();
	}
	
	@Override
	public void onFinishInput() {
		super.onFinishInput();
		capsLockDown = false;
	}

	public void onKey(int primaryCode, int[] keyCodes) {
		Log.d(TAG, "primaryCode: " + Integer.toString(primaryCode));
		String curCharMatch = morseMap.get(charInProgress.toString());

		switch(primaryCode) {
		
			// 0 represents a dot, 1 represents a dash
			// TODO The documentation for Keyboard.Key says I should be
			// able to give a key a string as a keycode, but it
			// errors out every time I try it.
			case 0:
			case 1:
				
				charInProgress.append(primaryCode==1 ? "-" : ".");
				Log.d(TAG, "charInProgress: " + charInProgress);
				if (charInProgress.length() > 6){
					charInProgress.setLength(0);
					Log.d(TAG, "..truncated"+charInProgress);
				}
				break;

			// Space button ends the current dotdash sequence
			// Space twice in a row sends through a standard space character
			case KeyEvent.KEYCODE_SPACE:
				if (charInProgress.length()==0) {
					getCurrentInputConnection().commitText(" ", 1);
				} else {
					Log.d(TAG, "Pressed space, look for " + charInProgress.toString());
					
					if (curCharMatch != null) {
						
						if (curCharMatch.contentEquals("\n")) {
							sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
						} else if (curCharMatch.contentEquals("END")) {
							requestHideSelf(0);
							inputView.closing();
						} else {
							
							if (capsLockDown) {
								curCharMatch = curCharMatch.toUpperCase();
							}
							Log.d(TAG, "Char identified as " + curCharMatch);
							getCurrentInputConnection().commitText(curCharMatch, curCharMatch.length());
						}
					}
				}
				clearCharInProgress();
				break;
			
			// Send backspace through as a normal one-character backspace
			// TODO Figure out a way to go back one dotdash, rather than one character
			// If there's a character in progress, clear it
			// otherwise, send through a backspace keypress
			case KeyEvent.KEYCODE_DEL:
				if (charInProgress.length() > 0 ){
					clearCharInProgress();
				} else {
					sendDownUpKeyEvents(primaryCode);
					clearCharInProgress();
				}
				break;
				
			case KeyEvent.KEYCODE_SHIFT_LEFT:
				capsLockDown = !capsLockDown;
				break;
		}
		
		// Set the label on the space key
//		if (charInProgress.length() == 0) {
//			spaceKey.label = "";
//		} else if (curCharMatch == null) {
//			spaceKey.label = charInProgress;
//		} else {
//			spaceKey.label = charInProgress + " " + curCharMatch;
//		}
		spaceKey.label = charInProgress;
		inputView.invalidateKey(spaceKeyIndex);
		
//		sendDownUpKeyEvents(KeyEvent.KEYCODE_STAR);
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

}
