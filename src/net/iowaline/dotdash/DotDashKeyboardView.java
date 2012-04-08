package net.iowaline.dotdash;

import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class DotDashKeyboardView extends KeyboardView {

	private DotDashIMEService service;
	private Dialog cheatsheetDialog;
	private View cheatsheet1;
	private View cheatsheet2;

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
		setPreviewEnabled(false);
		final GestureDetector gestureDetector = new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {

						if (e2.getY() < 0) {
			                // If they swipe up off the keyboard, launch the cheat sheet
							showCheatSheet();
						}
	                	return false;
					}
				});
		View.OnTouchListener gestureListener = new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		setOnTouchListener(gestureListener);
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
					FixedSizeView fsv = (FixedSizeView) cheatsheet2;
					fsv.fixedHeight = cheatsheet1.getMeasuredHeight();
					fsv.fixedWidth = cheatsheet1.getMeasuredWidth();
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
	 * Updates the newline code printed in the cheat sheet, based
	 * on the user's current preference.
	 */
	public void updateNewlineCode() {
		if (cheatsheet2 == null) {
			return;
		}
		
		String newCode = "disabled";
		if (service.newlineGroups != null && service.newlineGroups.length > 0) {
			newCode = service.newlineGroups[0].replaceAll("(.)", "$1 ").trim();
		}
		((TextView) cheatsheet2.findViewById(R.id.newline_code)).setText(newCode);
	}
}
