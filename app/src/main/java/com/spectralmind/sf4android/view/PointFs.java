package com.spectralmind.sf4android.view;

import android.graphics.PointF;

public final class PointFs {
	private PointFs() {
	}
	
	public static PointF copy(PointF original) {
		return new PointF(original.x, original.y);
	}
}
