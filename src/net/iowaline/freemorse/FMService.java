package net.iowaline.freemorse;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.View;

public class FMService extends InputMethodService {
	private KeyboardView mInputView;
	private Keyboard mDitDahKeyboard;
	
	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		super.onInitializeInterface();
		mDitDahKeyboard = new Keyboard(this, R.xml.ditdah);
	}
	
	@Override
	public View onCreateInputView() {
		mInputView = (KeyboardView) getLayoutInflater().inflate(
				R.layout.input, null
		);
		mInputView.setKeyboard(mDitDahKeyboard);
		return mInputView;
	}
}
