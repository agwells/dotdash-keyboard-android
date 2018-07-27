package net.iowaline.dotdash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

@SuppressWarnings("JavaDoc")
@SuppressLint("ClickableViewAccessibility")
public class DotDashKeyboardView extends KeyboardView {

	private final String TAG = this.getClass().getSimpleName();
	private DotDashIMEService service;
	private Dialog cheatSheetDialog;
	private TableLayout cheatSheet1;
	private TableLayout cheatSheet2;
	private int mSwipeThreshold;
	private GestureDetector gestureDetector;
	
	private Set<Keyboard.Key> pressedKeys = new HashSet<>();

	private static final int KBD_NONE = 0;
	public static final int KBD_DOTDASH = 1;
	public static final int KBD_UTILITY = 2;

	public boolean mEnableUtilityKeyboard = false;
	
    private static final int REPEAT_INTERVAL = 50; // ~20 keys per second
    private static final int REPEAT_START_DELAY = 400;
    @SuppressWarnings("unused")
	private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int DEBOUNCE_TIMEOUT = 50; //70;
    private static final int IAMBIC_DOT_LENGTH = 100;
	private static final long AUTOCOMMIT_DELAY = IAMBIC_DOT_LENGTH * 4;
    // TODO: Make this into a SparseArray somehow?
    private final Map<Keyboard.Key, Long> bounceWaits = new HashMap<>();
    
    private static final int MSG_KEY_REPEAT = 1;
    public static final int MSG_IAMBIC_PLAYING = 2;
	public static final int MSG_AUTOCOMMIT = 3;
    // TODO: according to this documentation: http://www.morsecode.nl/iambic.PDF
    // ... it appears that the logic is supposed to be that it "locks" if the
    // opposite key is still held down at the halfway point of the preceding
    // signal. So I will need to add some more logic there to get the timing
    // just right.
    boolean iambic_both_pressed = false;
    
    final Handler handler = new Handler(
		new Handler.Callback() {
			
			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case MSG_KEY_REPEAT:
						Keyboard.Key repeatKey = (Keyboard.Key) msg.obj;
						if (!repeatKey.pressed) {
							return true;
						}
						
						if (!(repeatKey == service.dotDashKeyboard.leftDotdashKey || repeatKey == service.dotDashKeyboard.rightDotdashKey)) {
							getOnKeyboardActionListener().onKey(repeatKey.codes[0], repeatKey.codes);
							handler.sendMessageDelayed(handler.obtainMessage(MSG_KEY_REPEAT, repeatKey), REPEAT_INTERVAL);
						}
						break;
					case MSG_IAMBIC_PLAYING:
						Keyboard.Key lastKeySent = (Keyboard.Key) msg.obj;
						Keyboard.Key nextKeyToSend = null;
						boolean leftKeyPressed = service.dotDashKeyboard.leftDotdashKey.pressed;
						boolean rightKeyPressed = service.dotDashKeyboard.rightDotdashKey.pressed;
						
						// Iambic signal has just ended. Check to see if dot and/or dash are still held down
						if (leftKeyPressed && rightKeyPressed) {
							// Both are pressed, so send the opposite signal from what we just sent
							if (lastKeySent == service.dotDashKeyboard.leftDotdashKey) {
								nextKeyToSend = service.dotDashKeyboard.rightDotdashKey;
							} else {
								nextKeyToSend = service.dotDashKeyboard.leftDotdashKey;
							}
						} else if (leftKeyPressed || rightKeyPressed) {
							// Only one is pressed. Send its signal.
							if (leftKeyPressed) {
								nextKeyToSend = service.dotDashKeyboard.leftDotdashKey;
							} else {
								nextKeyToSend = service.dotDashKeyboard.rightDotdashKey;
							}
							iambic_both_pressed = false;
						} else if (service.iambicModeB && iambic_both_pressed) {
							// Mode b. Send one more signal, with the opposite of the last key
							if (lastKeySent == service.dotDashKeyboard.leftDotdashKey) {
								nextKeyToSend = service.dotDashKeyboard.rightDotdashKey;
							} else {
								nextKeyToSend = service.dotDashKeyboard.leftDotdashKey;
							}
							iambic_both_pressed = false;
						}
						
						if (nextKeyToSend != null) {
							getOnKeyboardActionListener().onKey(nextKeyToSend.codes[0], nextKeyToSend.codes);
							handler.sendMessageDelayed(handler.obtainMessage(MSG_IAMBIC_PLAYING, nextKeyToSend),
									DotDashKeyboardView.get_iambic_delay(nextKeyToSend));
						} else {
							// Iambic is done, so start the autocommit timer.
							if (service.autocommit) {
								long delay = DotDashKeyboardView.AUTOCOMMIT_DELAY;
								// If audio is playing, we want to wait until the end of the tone before
								// we start counting down for autocommit.
								if (service.isAudio()) {
									delay += DotDashKeyboardView.get_iambic_delay(lastKeySent);
								}
								handler.removeMessages(DotDashKeyboardView.MSG_AUTOCOMMIT);
								handler.sendMessageDelayed(
										handler.obtainMessage(DotDashKeyboardView.MSG_AUTOCOMMIT),
										delay
								);
							}
						}
						break;
					case MSG_AUTOCOMMIT:
						service.commitCodeGroup(true);
						break;
				}

				return true;
			}
		}
	);

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

	@SuppressWarnings("deprecation")
	private void setEverythingUp() {
		mSwipeThreshold = (int) (300 * getResources().getDisplayMetrics().density);
		setPreviewEnabled(false);
		gestureDetector = new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {

					/**
					 * This function mostly copied from LatinKeyboardBaseView in
					 * the Hacker's Keyboard project: http://code.google.com/p/hackerskeyboard/
					 * 
				 	 * Copyright (C) 2010, authors of the Hacker's Keyboard project: http://code.google.com/p/hackerskeyboard/ 
				 	 * Copyright (c) 2011, Aaron Wells
					 * 
					 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
					 * use this file except in compliance with the License. You may obtain a copy of
					 * the License at
					 *
					 * http://www.apache.org/licenses/LICENSE-2.0
					 */
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {

						// If they swipe up off the keyboard, launch the cheat
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
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				if (gestureDetector.onTouchEvent(event)) {
//					// Tell the underlying KeyboardView to cancel its
//					// touch event if we've initiated a gesture.
//					MotionEvent cancel = MotionEvent.obtain(event);
//					cancel.setAction(MotionEvent.ACTION_CANCEL);
//					DotDashKeyboardView.this.onTouchEvent(cancel);
//					cancel.recycle();
//					return true;
//				} else {
//					return false;
//				}
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

	@SuppressLint("InflateParams")
	private void createCheatSheet() {
		boolean updateTouchListeners = false;
		if (this.cheatSheetDialog == null) {
			this.cheatSheetDialog = new Dialog(this.service);

			cheatSheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			cheatSheetDialog.setCancelable(true);
			cheatSheetDialog.setCanceledOnTouchOutside(true);
			updateTouchListeners = true;
		}
		if (this.cheatSheet1 == null) {
			this.cheatSheet1 = (TableLayout) this.service.getLayoutInflater().inflate(
					R.layout.cheatsheet1, null);
			this.prettifyCheatSheet(this.cheatSheet1);
			updateTouchListeners = true;
		}
		if (this.cheatSheet2 == null) {
			this.cheatSheet2 = (TableLayout) this.service.getLayoutInflater().inflate(
					R.layout.cheatsheet2, null);
			updateNewlineCode();
			this.prettifyCheatSheet(this.cheatSheet2);
			updateTouchListeners = true;
		}
		
		if (updateTouchListeners) {
			cheatSheetDialog.setContentView(cheatSheet1);
			cheatSheet1.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					cheatSheetDialog.setContentView(cheatSheet2);
					return true;
				}
			});
			cheatSheet2.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					cheatSheetDialog.setContentView(cheatSheet1);
					return true;
				}
			});
			Window window = this.cheatSheetDialog.getWindow();
			if (window != null) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.token = this.getWindowToken();
				lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
				window.setAttributes(lp);
				window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			}
		}
	}
	
	/**
	 * Update the characters in the cheat sheet dialogue to match the user's preference
	 * @todo Probably better performance if I replaced this with two hard-coded versions
	 * of the sheet...
	 * 
	 * @param cheatSheet
	 */
	private void prettifyCheatSheet(TableLayout cheatSheet) {
		// No action necessary.
		if (service.ditDahCharsPref == DotDashIMEService.DIT_DAH_CHARS_UNICODE) {
			return;
		}
		
		for (int i = 0; i < cheatSheet.getChildCount(); i++) {
			TableRow row = (TableRow) cheatSheet.getChildAt(i);
			
			// On my cheat sheets, only the even-number columns
			// contain code groups
			for (int j = 1; j < row.getChildCount(); j += 2) {
				TextView cell = (TextView) row.getChildAt(j);
				cell.setText(service.convertDitDahUnicodeToAscii(cell.getText().toString()));
			}
		}
	}

	private void showCheatSheet() {
		createCheatSheet();
		cheatSheetDialog.show();
	}
	
	public void closeCheatSheet() {
		if (cheatSheetDialog != null) {
			cheatSheetDialog.dismiss();
		}
	}
	
	public void clearCheatSheet() {
		closeCheatSheet();
		this.cheatSheet1 = null;
		this.cheatSheet2 = null;
	}

	/**
	 * Updates the newline code printed in the cheat sheet, based on the user's
	 * current preference.
	 */
	public void updateNewlineCode() {
		if (cheatSheet2 == null) {
			return;
		}

		String newCode = service.getText(R.string.newline_disabled).toString();
		if (service.newlineGroups != null && service.newlineGroups.length > 0) {
			newCode = service.newlineGroups[0].replace(".", DotDashIMEService.UNICODE_DOT).replace("-",  DotDashIMEService.UNICODE_DASH);
		}
		((TextView) cheatSheet2.findViewById(R.id.newline_code))
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

		// Let KeyboardView handle the utility keyboard. 
		if (whichKeyboard() == DotDashKeyboardView.KBD_UTILITY) {
			return super.onTouchEvent(me);
		}

		int actionMasked = me.getActionMasked();
		int actionIndex = me.getActionIndex();
		Set<Keyboard.Key> curPressedKeys = new HashSet<>();
		
		for (int i=0; i < me.getPointerCount(); i++) {
			
			// Find out which key the pointer is on
			int x = (int) me.getX(i);
			int y = (int) me.getY(i);
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
				if (i == actionIndex) {
					switch (actionMasked) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_MOVE:
						case MotionEvent.ACTION_POINTER_DOWN:
							curPressedKeys.add(touchedKey);
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_POINTER_UP:
						case MotionEvent.ACTION_OUTSIDE:
						// TODO: The docs say about ACTION_CANCEL: "You should treat this as an
						// up event, but not perform any action that you normally would".
						// So to really do that I'll need to put some further logic into this.
						// (How do you cancel a keypress?)
						case MotionEvent.ACTION_CANCEL:
							curPressedKeys.remove(touchedKey);
							break;
					}
				} else {
					curPressedKeys.add(touchedKey);
				}
			}
		}
		
		// Now that we know which keys have fingers on 'em this time,
		// let's check to see how that has changed from last time.
		
		// Keys that are in curPressedKeys but not in pressedKeys
		// are newly pressed.
		Set<Keyboard.Key> newlyPressed = new HashSet<>(curPressedKeys);
		newlyPressed.removeAll(pressedKeys);
		for (Keyboard.Key k : newlyPressed) {
			if (k.pressed) {
				continue;
			}
			Long bounceWait = this.bounceWaits.get(k);
			if (bounceWait != null && bounceWait > SystemClock.elapsedRealtime()) {
				continue;
			}

//			k.onPressed();
			k.pressed = true;

			getOnKeyboardActionListener().onPress(k.codes[0]);

			if (service.iambic && (k == service.dotDashKeyboard.leftDotdashKey || k == service.dotDashKeyboard.rightDotdashKey)) {
				// In iambic mode, we only process the key if there's not already a signal going
				// What matters is in the message handler, where we check the state of the keys
				// when the current message ends.
				//
				// TODO: Actually... it might make more sense if the iambic timing was
				// over in DotDashIMEService, using the onPress() method to trigger it.
				if (!handler.hasMessages(MSG_IAMBIC_PLAYING)) {
					getOnKeyboardActionListener().onKey(k.codes[0], k.codes);
					
					handler.sendMessageDelayed(
							handler.obtainMessage(MSG_IAMBIC_PLAYING, k),
							DotDashKeyboardView.get_iambic_delay(k)
					);
				}
				
				// Iambic mode B needs to know if both keys got pressed simultaneously while an Iambic message
				// was in progress.
				if (service.iambicModeB && service.dotDashKeyboard.leftDotdashKey.pressed && service.dotDashKeyboard.rightDotdashKey.pressed) {
					this.iambic_both_pressed = true;
				}
			} else {
				getOnKeyboardActionListener().onKey(k.codes[0], k.codes);
				
				if (k.repeatable && !handler.hasMessages(MSG_KEY_REPEAT, k)) {
					handler.sendMessageDelayed(
							handler.obtainMessage(MSG_KEY_REPEAT, k),
							REPEAT_START_DELAY
					);
				}
			}
			
			invalidateKey(service.dotDashKeyboard.getKeys().indexOf(k));
		}
		
		// Keys that are in pressedKeys but not curPressedKeys
		// are newly released.
		Set<Keyboard.Key> newlyReleased = new HashSet<>(pressedKeys);
		newlyReleased.removeAll(curPressedKeys);
		for (Keyboard.Key k : newlyReleased) {
			if (!k.pressed) {
				continue;
			}

			k.pressed = false;
			getOnKeyboardActionListener().onRelease(k.codes[0]);
			if (k.repeatable) {
				handler.removeMessages(MSG_KEY_REPEAT, k);
			}
			this.bounceWaits.put(k, SystemClock.elapsedRealtime() + DotDashKeyboardView.DEBOUNCE_TIMEOUT);
			invalidateKey(service.dotDashKeyboard.getKeys().indexOf(k));
			
			// If we're not in iambic mode, then the release of a key is probably a decent time to start
			// the autocommit timer.
			if (
					!service.iambic 
					&& service.isAudio()
					&& (k == service.dotDashKeyboard.leftDotdashKey || k == service.dotDashKeyboard.rightDotdashKey)
					&& !service.dotDashKeyboard.leftDotdashKey.pressed
					&& !service.dotDashKeyboard.rightDotdashKey.pressed
			) {
				long delay = DotDashKeyboardView.AUTOCOMMIT_DELAY;
				// If audio is playing, we want to wait until the end of the tone before
				// we start counting down for autocommit.
				if (service.isAudio()) {
					delay += DotDashKeyboardView.get_iambic_delay(k);
				}
				handler.removeMessages(DotDashKeyboardView.MSG_AUTOCOMMIT);
				handler.sendMessageDelayed(
						handler.obtainMessage(DotDashKeyboardView.MSG_AUTOCOMMIT),
						delay
				);
			}
		}
		
		pressedKeys = curPressedKeys;

		for (Keyboard.Key k : service.dotDashKeyboard.getKeys()) {
			Log.d(TAG, "Key " + String.valueOf(k.codes[0]) + " " + (k.pressed ? "down" : "up"));
		}

		return true;
	}
	
	private static long get_iambic_delay(Keyboard.Key k) {
		if (k.codes[0] == DotDashKeyboard.KEYCODE_DOT) {
			// a dot and the space after it
			return IAMBIC_DOT_LENGTH * 2;
		} else {
			// a dash (three dot lengths) and the space after it
			return IAMBIC_DOT_LENGTH * 4;
		}
	}
}
