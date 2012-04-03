package net.iowaline.dotdash;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class DotDashKeyboardView extends KeyboardView {

	private DotDashIMEService service;
	private AlertDialog cheatsheet;
	
	public void setService( DotDashIMEService service ) {
		this.service = service;
	}
	
	public DotDashKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public DotDashKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
//	@Override
//	public boolean onTouchEvent(MotionEvent me) {
//		Log.d("DotDashKeyboardView", "onTouchEvent!!!");
//		if (me.getAction() == MotionEvent.ACTION_UP && me.getHistoricalY(0)==0) {
//			Log.d("DotDashKeyboardView", "ACTION_UP");
////			showCheatSheet();
//		}
//		return super.onTouchEvent(me);
//	}
	
	public void showCheatSheet() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.service);
		builder.setCancelable(true);
		builder.setNegativeButton("OK", null);
		builder.setTitle("Cheat Sheet");
		this.cheatsheet = builder.create();
		Window window = this.cheatsheet.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.token = this.getWindowToken();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		this.cheatsheet.show();
	}
}
