package com.spectralmind.sf4android.bubble;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class Converter {
	private final PointF offset;
	private float scaleFactor;

	public Converter() {
		this(new PointF(0, 0), 1);
	}

	public Converter(PointF offset, float scaleFactor) {
		this.offset = offset;
		this.scaleFactor = scaleFactor;
	}

	public PointF getOffset()
	{
		return offset;
	}
	
	public float getScaleFactor()
	{
		return scaleFactor;
	}
	
	public static float toView(float value, float scaleFactor) {
		return value * scaleFactor;
	}

	public static float fromView(float value, float scaleFactor) {
		return value / scaleFactor;
	}

	public static PointF fromView(PointF location, PointF offset, float scaleFactor) {
		return new PointF(coordinateFromView(location.x, offset.x, scaleFactor), coordinateFromView(location.y,
				offset.y, scaleFactor));
	}

	public static float coordinateFromView(float value, float offset, float scaleFactor) {
		return fromView(value, scaleFactor) + offset;
	}

	public static float coordinateToView(float value, float offset, float scaleFactor) {
		return toView(value - offset, scaleFactor);
	}

	public static Rect toView(Rect rect, PointF offset, float scaleFactory) {
		return new Rect((int) coordinateToView(rect.left, offset.x, scaleFactory), (int) coordinateToView(rect.top,
				offset.y, scaleFactory), (int) coordinateToView(rect.right, offset.x, scaleFactory),
				(int) coordinateToView(rect.bottom, offset.y, scaleFactory));
	}

	public static void toView(RectF rect, RectF viewRect, PointF offset, float scaleFactory) {
		viewRect.left = coordinateToView(rect.left, offset.x, scaleFactory);
		viewRect.top = coordinateToView(rect.top, offset.y, scaleFactory);
		viewRect.right = coordinateToView(rect.right, offset.x, scaleFactory);
		viewRect.bottom = coordinateToView(rect.bottom, offset.y, scaleFactory);
	}

	public float toView(float value) {
		return toView(value, scaleFactor);
	}

	public float xToView(float value) {
		return coordinateToView(value, offset.x, scaleFactor);
	}

	public float yToView(float value) {
		return coordinateToView(value, offset.y, scaleFactor);
	}

	public Rect toView(Rect rect) {
		return toView(rect, offset, scaleFactor);
	}

	public RectF toView(RectF rect) {
		RectF viewRect = new RectF();
		toView(rect, viewRect, offset, scaleFactor);
		return viewRect;
	}

	public void toView(RectF rect, RectF viewRect) {
		toView(rect, viewRect, offset, scaleFactor);
	}

	public void toView(PointF point, PointF viewPoint) {
		viewPoint.x = xToView(point.x);
		viewPoint.y = yToView(point.y);
	}

	public float fromView(float value) {
		return value / scaleFactor;
	}

	public PointF fromView(PointF location) {
		return fromView(location, offset, scaleFactor);
	}

	public void copyWithOffsetDelta(Converter original, PointF delta) {
		offset.x = original.offset.x - delta.x;
		offset.y = original.offset.y - delta.y;
		scaleFactor = original.scaleFactor;
	}
}
