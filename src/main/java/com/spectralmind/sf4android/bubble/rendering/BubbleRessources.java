package com.spectralmind.sf4android.bubble.rendering;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.TypedValue;

import com.spectralmind.sf4android.R;

public class BubbleRessources {
	public final int backgroundColor;
	public final Paint textPaint;
	public final NinePatchDrawable labelBackground;
	public final Rect labelPadding;
	private final Drawable[] bubbleImages;
	private final Drawable bubbleGlow;
	private final Drawable discoveryBubble;
	private final ColorFilters colorFilters;

	public BubbleRessources(Context context) {
		backgroundColor = Color.argb(255, 0, 0, 0);
		textPaint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(Color.WHITE);
		final int LABEL_TEXT_SIZE_DP = 13; // DP
		textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				LABEL_TEXT_SIZE_DP, context.getResources().getDisplayMetrics()));
		textPaint.setShadowLayer(2, 1, 1, Color.BLACK);
		labelBackground = (NinePatchDrawable) context.getResources().getDrawable(R.drawable.bubble_text_background);
		labelPadding = new Rect();
		labelBackground.getPadding(labelPadding);
		bubbleGlow = context.getResources().getDrawable(R.drawable.bubble_glow_256);
		bubbleImages = new Drawable[4];
		bubbleImages[0] = context.getResources().getDrawable(R.drawable.bubble_32);
		bubbleImages[1] = context.getResources().getDrawable(R.drawable.bubble_64);
		bubbleImages[2] = context.getResources().getDrawable(R.drawable.bubble_128);
		bubbleImages[3] = context.getResources().getDrawable(R.drawable.bubble_256);
		discoveryBubble = context.getResources().getDrawable(R.drawable.similar_bubble);
		colorFilters = new ColorFilters(Mode.MULTIPLY);
	}

	public Drawable getDiscoveryBubble()
	{
		return discoveryBubble;
	}
	
	public Drawable getBubbleGlowImage()
	{
		return bubbleGlow;
	}
	
	public Drawable getBubbleImageForSize(int size) {
		if(size <= 32) {
			return bubbleImages[0];
		}
		if(size <= 64) {
			return bubbleImages[1];
		}
		if(size <= 128) {
			return bubbleImages[2];
		}
		return bubbleImages[3];
	}
	
	public ColorFilter getFilterForColor(Integer color) {
		return colorFilters.getFilterForColor(color);
	}
}
