package net.iowaline.dotdash;

import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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
	}

	public DotDashKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void createCheatSheet() {
		if (this.cheatsheet1 == null) {
			this.cheatsheet1 = this.service.getLayoutInflater().inflate(
					R.layout.cheatsheet1, null);
		}
		if (this.cheatsheet2 == null) {
			this.cheatsheet2 = this.service.getLayoutInflater().inflate(
					R.layout.cheatsheet2, null);
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
}
