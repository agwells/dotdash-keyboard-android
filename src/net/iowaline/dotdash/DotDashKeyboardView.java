package net.iowaline.dotdash;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class DotDashKeyboardView extends KeyboardView {

	private DotDashIMEService service;
	private AlertDialog cheatsheetDialog;
	private View cheatsheet1;
	private View cheatsheet2;
	private View currentCheatSheetView;
	
	public void setService( DotDashIMEService service ) {
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
			this.cheatsheet1 = this.service.getLayoutInflater().inflate(R.layout.cheatsheet1, null);
		}
		if (this.cheatsheet2 == null) {
			this.cheatsheet2 = this.service.getLayoutInflater().inflate(R.layout.cheatsheet2, null);
		}
		if (this.cheatsheetDialog == null){
			AlertDialog.Builder builder = new AlertDialog.Builder(this.service);
			builder.setCancelable(true);
			builder.setPositiveButton("Next", null);
			builder.setNegativeButton("Close", null);
			builder.setView(this.cheatsheet1);
			this.currentCheatSheetView = this.cheatsheet1;
			this.cheatsheetDialog = builder.create();
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
		Button nextButton = cheatsheetDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		nextButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (currentCheatSheetView == cheatsheet1) {
					currentCheatSheetView = cheatsheet2;
				} else {
					currentCheatSheetView = cheatsheet1;
				}
				cheatsheetDialog.setView(currentCheatSheetView);
			}
		});
	}
}
