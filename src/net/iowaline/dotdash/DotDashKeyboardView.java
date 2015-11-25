package net.iowaline.dotdash;

import java.util.HashSet;
import java.util.Set;

import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class DotDashKeyboardView extends KeyboardView {

	private String TAG = this.getClass().getSimpleName();
	private DotDashIMEService service;
	private Dialog cheatsheetDialog;
	private View cheatsheet1;
	private View cheatsheet2;
	private int mSwipeThreshold;
	private GestureDetector gestureDetector;
	
	private Set<Keyboard.Key> pressedKeys = new HashSet<Keyboard.Key>();

	public static final int KBD_NONE = 0;
	public static final int KBD_DOTDASH = 1;
	public static final int KBD_UTILITY = 2;

	public boolean mEnableUtilityKeyboard = false;

	public void setService(DotDashIMEService service) {
		this.service = service;
	}

	public DotDashKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEverythingUp();
	}

	public DotDashKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setEverythingUp();
	}

	private void setEverythingUp() {
		mSwipeThreshold = (int) (300 * getResources().getDisplayMetrics().density);
		setPreviewEnabled(false);
		gestureDetector = new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {

					/**
					 * This function mostly copied from LatinKeyboardBaseView in
					 * the Hacker's Keyboard project
					 * http://code.google.com/p/hackerskeyboard/
					 */
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {

						// If they swip up off the keyboard, launch the cheat
						// sheet. This was originally a check for e2.getY() < 0,
						// but that didn't work in ICS. Possibly ICS stops
						// sending you events after you go past the edge of the
						// window. So I changed it to 10 instead.
						if (e2.getY() <= 10) {
							// If they swipe up off the keyboard, launch the
							// cheat sheet
							showCheatSheet();
							return true;
						} else if (mEnableUtilityKeyboard) {
							final float absX = Math.abs(velocityX);
							final float absY = Math.abs(velocityY);
							float deltaX = e2.getX() - e1.getX();
							int travelMin = Math.min((getWidth() / 3),
									(getHeight() / 3));

							if (velocityX > mSwipeThreshold && absY < absX
									&& deltaX > travelMin) {
								toggleKeyboard();
								return true;
							} else if (velocityX < -mSwipeThreshold
									&& absY < absX && deltaX < -travelMin) {
								toggleKeyboard();
								return true;
							}
						}
						return false;
					}
				});
//		View.OnTouchListener gestureListener = new View.OnTouchListener() {
//
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				return gestureDetector.onTouchEvent(event);
//			}
//		};
//		setOnTouchListener(gestureListener);
	}

	private void toggleKeyboard() {
		if (getKeyboard() == service.dotDashKeyboard) {
			setKeyboard(service.utilityKeyboard);
			// TODO: Make this work. I think it's a layout issue...
//			setPreviewEnabled(true);
		} else {
			setKeyboard(service.dotDashKeyboard);
//			setPreviewEnabled(false);
		}
	}

	public void createCheatSheet() {
		if (this.cheatsheet1 == null) {
			this.cheatsheet1 = this.service.getLayoutInflater().inflate(
					R.layout.cheatsheet1, null);
		}
		if (this.cheatsheet2 == null) {
			this.cheatsheet2 = this.service.getLayoutInflater().inflate(
					R.layout.cheatsheet2, null);
			updateNewlineCode();
		}
		if (this.cheatsheetDialog == null) {
			this.cheatsheetDialog = new Dialog(this.service);

			cheatsheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			cheatsheetDialog.setCancelable(true);
			cheatsheetDialog.setCanceledOnTouchOutside(true);
			cheatsheetDialog.setContentView(cheatsheet1);
			cheatsheet1.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					cheatsheetDialog.setContentView(cheatsheet2);
					return true;
				}
			});
			cheatsheet2.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					cheatsheetDialog.setContentView(cheatsheet1);
					return true;
				}
			});
			Window window = this.cheatsheetDialog.getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.token = this.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		}
	}

	public void showCheatSheet() {
		createCheatSheet();
		this.cheatsheetDialog.show();
	}

	public void closeCheatSheet() {
		if (cheatsheetDialog != null) {
			cheatsheetDialog.dismiss();
		}
	}

	/**
	 * Updates the newline code printed in the cheat sheet, based on the user's
	 * current preference.
	 */
	public void updateNewlineCode() {
		if (cheatsheet2 == null) {
			return;
		}

		String newCode = "disabled";
		if (service.newlineGroups != null && service.newlineGroups.length > 0) {
			newCode = service.newlineGroups[0].replaceAll("(.)", "$1 ").trim();
		}
		((TextView) cheatsheet2.findViewById(R.id.newline_code))
				.setText(newCode);
	}

	public int whichKeyboard() {
		Keyboard kbd = getKeyboard();
		if (kbd == service.dotDashKeyboard) {
			return KBD_DOTDASH;
		} else if (kbd == service.utilityKeyboard) {
			return KBD_UTILITY;
		} else
			return KBD_NONE;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent me) {
		Log.d(TAG, "onTouchEvent");

		// TODO: Unfortunately, since I send the character when you first press
		// the key, all the keys you press while swiping still count as getting
		// pressed.
		//
		// Not sure what I could do about that... maybe a more sensitive 
		// swipe detector?
		if (gestureDetector.onTouchEvent(me)) {
			for(Keyboard.Key k : pressedKeys) {
				k.onReleased(false);
			}
			invalidateAllKeys();
			pressedKeys.clear();
			return true;
		}
		
		int actionmasked = me.getActionMasked();
		int actionindex = me.getActionIndex();

		Set<Keyboard.Key> curPressedKeys = new HashSet<Keyboard.Key>();
//		curPressedKeys.addAll(pressedKeys);
		
		for (int i=0; i < me.getPointerCount(); i++) {
			
			// Find out which key the pointer is on
			int x = (int) me.getX(i);
			int y = (int) me.getY(i);
			if (y < 0) {
				continue;
			}
			int[] keys = service.dotDashKeyboard.getNearestKeys(x, y);
			Keyboard.Key touchedKey = null;
			for (int k : keys) {
				Keyboard.Key key = service.dotDashKeyboard.getKeys().get(k);
				// TODO: This continues to detect it even after you've moved off the keyboard. :-P
				if (key.isInside(x, y)) {
					touchedKey = key;
				}
			}
			
			if (touchedKey != null) {
				if (i == actionindex) {
					switch (actionmasked) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_MOVE:
							curPressedKeys.add(touchedKey);
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_CANCEL:
							curPressedKeys.remove(touchedKey);
							break;
					}
				} else {
					// Since this pointer isn't the one doing the "action"
					// we can probably assume it's "down"
					curPressedKeys.add(touchedKey);
				}
			}
		}
		
		// Now that we know which keys have fingers on 'em this time,
		// let's check to see how that has changed from last time.
		// TODO: Repeatable keys
		
		// Keys that are in curPressedKeys but not in pressedKeys
		// are newly pressed.
		Set<Keyboard.Key> newlyPressed = new HashSet<Keyboard.Key>(curPressedKeys);
		newlyPressed.removeAll(pressedKeys);
		for (Keyboard.Key k : newlyPressed) {
			k.onPressed();
			service.onPress(k.codes[0]);
			invalidateKey(service.dotDashKeyboard.getKeys().indexOf(k));
		}
		
		// Keys that are in pressedKeys but not curPressedKeys
		// are newly released.
		Set<Keyboard.Key> newlyReleased = new HashSet<Keyboard.Key>(pressedKeys);
		newlyReleased.removeAll(curPressedKeys);
		for (Keyboard.Key k : newlyReleased) {
			k.onReleased(false);
			service.onKey(k.codes[0], k.codes);
			service.onRelease(k.codes[0]);
			service.updateCapsLockKey(false);
			invalidateKey(service.dotDashKeyboard.getKeys().indexOf(k));
		}
		
		pressedKeys = curPressedKeys;

//		for (Keyboard.Key k : service.dotDashKeyboard.getKeys()) {
//			Log.d(TAG, "Key " + String.valueOf(k.codes[0]) + " " + (k.pressed ? "down" : "up"));
//		}

		return true;
	}
}
