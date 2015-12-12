package net.iowaline.dotdash;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;

public class DotDashKeyboard extends Keyboard {

	public static final int KEYCODE_DOT = 0;
	public static final int KEYCODE_DASH = 1;
	
	public DotDashKeyboard(Context context, int xmlLayoutResId) {
		super(context, xmlLayoutResId);
	}

	public DotDashKeyboard(Context context, int layoutTemplateResId,
			CharSequence characters, int columns, int horizontalPadding) {
		super(context, layoutTemplateResId, characters, columns,
				horizontalPadding);
	}

	public Keyboard.Key spaceKey;
	public Keyboard.Key capsLockKey;
	public Keyboard.Key leftDotdashKey;
	public Keyboard.Key rightDotdashKey;

	@Override
	protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
			XmlResourceParser parser) {
		// TODO Auto-generated method stub
		Key k = super.createKeyFromXml(res, parent, x, y, parser);
		switch (k.codes[0]) {
			case 0:
				leftDotdashKey = k;
				break;
			case 1:
				rightDotdashKey = k;
				break;
			case 62:
				spaceKey = k;
				break;
			case 59:
				capsLockKey = k;
				break;
		}
		return k;
	}
	
	/**
	 * Sets up the dot & dash keys to match the user's preference.
	 * By default, the dot key is on the left and the dash key is
	 * on the right.
	 * 
	 * @param dashkeyonleft True if the dash key should be on the left
	 * @return boolean True if the keys changed position
	 */
	public boolean setupDotDashKeys(boolean dashkeyonleft) {
		if (dashkeyonleft != (leftDotdashKey.codes[0] == DotDashKeyboard.KEYCODE_DASH)) {
			// Swap 'em!
			int[] code_tmp = leftDotdashKey.codes;
			leftDotdashKey.codes = rightDotdashKey.codes;
			rightDotdashKey.codes = code_tmp;
			
			Drawable icon_tmp = leftDotdashKey.icon;
			leftDotdashKey.icon = rightDotdashKey.icon;
			rightDotdashKey.icon = icon_tmp;
			return true;
		}
		
		return false;
	}
}
