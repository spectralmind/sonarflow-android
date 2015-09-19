package com.spectralmind.sf4android.view;

import android.graphics.Paint;

public class Paints {
	public static Paint newPaintFromARGB(int a, int r, int g, int b) {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setARGB(a, r, g, b);
		return paint;
	}
}
