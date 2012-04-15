package net.iowaline.dotdash;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TableLayout;

public class FixedSizeView extends TableLayout {

	public int fixedWidth = 0;
	public int fixedHeight = 0;

	public FixedSizeView(Context context) {
		super(context);
	}

	public FixedSizeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (fixedWidth != 0 && fixedHeight != 0) {
			this.setMeasuredDimension(fixedWidth, fixedHeight);
		}
	}
}