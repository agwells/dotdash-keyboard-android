package net.iowaline.freemorse;

import java.util.Hashtable;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class FMService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
	private String TAG = "FMService";
	private KeyboardView inputView;
	private Keyboard dotDashKeyboard;
	private Hashtable<String, String> morseMap;
	private StringBuilder charInProgress;
	
	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		super.onInitializeInterface();
		dotDashKeyboard = new Keyboard(this, R.xml.dotdash);
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
		morseMap.put(".-.-.-", ".");
		morseMap.put("--..--", ",");
		morseMap.put("---...", ":");
		morseMap.put("..--..", "?");
		morseMap.put(".----.", "\'");
		morseMap.put("-....-", "-");
		morseMap.put("-..-.", "/");
		morseMap.put("-.--.-", "(");
		morseMap.put("-.--.-", ")");
		morseMap.put(".-..-.", "\"");
		morseMap.put(".--.-.", "@");
		morseMap.put("-...-", "=");
	}
	
	@Override
	public View onCreateInputView() {
		inputView = (KeyboardView) getLayoutInflater().inflate(
				R.layout.input, null
		);
		inputView.setOnKeyboardActionListener(this);
		inputView.setKeyboard(dotDashKeyboard);
		return inputView;
	}
	
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		super.onStartInputView(info, restarting);
		charInProgress = new StringBuilder();
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		Log.d(TAG, "primaryCode: " + Integer.toString(primaryCode));
		if (primaryCode == 0 || primaryCode == 1) {
			charInProgress.append(primaryCode==1 ? "-" : ".");
			Log.d(TAG, "charInProgress: " + charInProgress);
			if (charInProgress.length() > 6){
				charInProgress.setLength(0);
				Log.d(TAG, "..truncated"+charInProgress);
			}
		}
		if (primaryCode == -1) {
			if (charInProgress.length()==0) {
				getCurrentInputConnection().commitText(" ", 1);
			} else {
				Log.d(TAG, "Pressed space, look for " + charInProgress.toString());
				String finalChar = morseMap.get(charInProgress.toString());
				if (finalChar != null) {
					Log.d(TAG, "Char identified as " + finalChar);
					getCurrentInputConnection().commitText(finalChar, finalChar.length());
				}
			}
			charInProgress.setLength(0);
		}
//		sendDownUpKeyEvents(KeyEvent.KEYCODE_STAR);
	}

	@Override
	public void onPress(int primaryCode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRelease(int primaryCode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onText(CharSequence text) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void swipeDown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void swipeLeft() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void swipeRight() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void swipeUp() {
		// TODO Auto-generated method stub
		
	}
}
