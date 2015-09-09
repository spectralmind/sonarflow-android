package com.spectralmind.sf4android.bubble.rendering;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public interface RenderContext {

	public void drawBubble(RectF bubbleRect, int alpha, Integer color);

	public void drawDiscoveryBubble(RectF bubbleRect, int alpha, Integer color);
	
	void drawLabel(PointF center, String text, int alpha, Integer color);
	
	void drawDiscoveryLabel(PointF center, String text, int alpha, Integer color);
	
	void drawCoverArt(Drawable coverArt,Rect bound);
	
	boolean isVisible(RectF viewRect);

	public void drawBubbleGlow(RectF bubbleRect, int alpha,Integer color);

	public void drawDiscoveryBubbleGlow(RectF bubbleRect, int alpha, Integer color);
}
