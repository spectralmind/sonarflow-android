package com.spectralmind.sf4android.bubble.rendering;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class OverridingRenderContext implements RenderContext {

	private final Integer colorOverride;
	private RenderContext inner;
	private int maxAlpha;

	public OverridingRenderContext(Integer colorOverride) {
		this.colorOverride = colorOverride;
	}

	public void setOverrides(RenderContext inner, int maxAlpha) {
		this.inner = inner;
		this.maxAlpha = maxAlpha;
	}

	@Override
	public void drawDiscoveryBubble(RectF bubbleRect, int alpha, Integer color) {
		
	}
	
	@Override
	public void drawBubble(RectF bubbleRect, int alpha, Integer color) {
		inner.drawBubble(bubbleRect, Math.min(alpha, maxAlpha), colorOverride);
	}

	@Override
	public void drawLabel(PointF center, String text, int alpha, Integer color) {
		inner.drawLabel(center, text, Math.min(alpha, maxAlpha), colorOverride);
	}

	@Override
	public boolean isVisible(RectF viewRect) {
		return inner.isVisible(viewRect);
	}

	@Override
	public void drawDiscoveryLabel(PointF center, String text, int alpha,
			Integer color) {}

	@Override
	public void drawCoverArt(Drawable coverArt, Rect bound) {
		inner.drawCoverArt(coverArt, bound);
	}

	@Override
	public void drawBubbleGlow(RectF viewBounds, int backgroundAlpha,
			Integer color) {
		inner.drawBubbleGlow(viewBounds, backgroundAlpha, color);
	}

	@Override
	public void drawDiscoveryBubbleGlow(RectF bubbleRect, int alpha,
			Integer color) {
		inner.drawDiscoveryBubbleGlow(bubbleRect, alpha, color);
	}

}
