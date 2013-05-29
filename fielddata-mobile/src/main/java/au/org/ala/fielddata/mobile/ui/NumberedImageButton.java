package au.org.ala.fielddata.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Draws a number in a red circle in the top right hand corner of the
 * ImageButton.
 * Use setNumber to change the number this is displayed.
 */
public class NumberedImageButton extends ImageButton {

	private Integer number;
	private Paint red;
	private Paint white;
	
	public NumberedImageButton(Context context) {
		super(context);
		
		init();
	}

	public NumberedImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public NumberedImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	protected void init() {
		red = new Paint(Paint.ANTI_ALIAS_FLAG);
		red.setColor(Color.RED);
		
		white = new Paint(Paint.ANTI_ALIAS_FLAG);
		white.setColor(Color.WHITE);
		
		white.setTextSize(15*getResources().getDisplayMetrics().density);
		white.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		number = null;
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// Draw the image as usual.
		super.onDraw(canvas);
		
		if (number == null) {
			return;
		}
	
		int w = getWidth();
		int h = getHeight();
		
		// We want the number to be about 10% of the width/height.
		int d = (int)(Math.max(w, h) / 7.5d);
		if (d%2 == 1) {
			d+=1;
		}
		
		canvas.drawCircle(w-d-1, d+1, d+2, white);
		canvas.drawCircle(w-d, d, d, red);
		
		canvas.drawText(number.toString(), (int)(w-1.5*d), (int)(1.5*d), white);
		
	}
	
	
	/**
	 * Sets the number to be rendered, passing null will remove the number
	 * @param number the number to display, or null to not display any number.
	 */
	public void setNumber(Integer number) {
		this.number = number;
		invalidate();
	}

}
