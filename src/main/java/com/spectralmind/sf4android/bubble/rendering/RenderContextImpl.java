package com.spectralmind.sf4android.bubble.rendering;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.google.common.collect.Lists;

public class RenderContextImpl implements RenderContext {
	private final BubbleRessources resources;
	private final List<LabelInfo> labelsToDraw;

	private Canvas canvas;
	private Rect clipBounds;
	private RectF clipBoundsF;
	private int labelIndex;

	public RenderContextImpl(BubbleRessources resources) {
		this.resources = resources;
		this.labelsToDraw = Lists.newArrayList();
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
		this.clipBounds = canvas.getClipBounds();
		this.clipBoundsF = new RectF(canvas.getClipBounds());
	}

	@Override
	public void drawCoverArt(Drawable coverArt, Rect bound) {
		if(coverArt == null)
			return;
		coverArt.setBounds(bound);
		coverArt.draw(canvas);
	}
	
	@Override
	public void drawBubble(RectF bubbleRect, int alpha, Integer color) {
		drawDrawable(bubbleRect, resources.getBubbleImageForSize((int) bubbleRect.width()), alpha, color);
	}

	@Override
	public void drawDiscoveryBubble(RectF bubbleRect, int alpha, Integer color) {
		drawDrawable(bubbleRect, resources.getDiscoveryBubble(), alpha, color);
	}
	
	@Override
	public void drawBubbleGlow(RectF bubbleRect, int alpha,Integer color) {
		drawDrawable(bubbleRect, resources.getBubbleGlowImage(), alpha, color);
	}
	
	@Override
	public void drawDiscoveryBubbleGlow(RectF bubbleRect, int alpha, Integer color) {
		drawDrawable(bubbleRect, resources.getBubbleGlowImage(), alpha, color);
	}
	
	private void drawDrawable(RectF bounds, Drawable drawable, int alpha, Integer color) {
		drawable.setBounds((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);
		drawDrawable(drawable, alpha, color);
	}

	private void drawDrawable(Rect bounds, Drawable drawable, int alpha, Integer color) {
		drawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
		drawDrawable(drawable, alpha, color);
	}

	private void drawDrawable(Drawable drawable, int alpha, Integer color) {
		drawable.setColorFilter(resources.getFilterForColor(color));
		drawable.setAlpha(alpha);
		drawable.draw(canvas);
	}

	@Override
	public void drawLabel(PointF center, String text, int alpha, Integer color) {
		if(labelIndex < labelsToDraw.size()) {
			labelsToDraw.get(labelIndex).update(center, text, alpha, color);
		}
		else {
			labelsToDraw.add(new LabelInfo(center, text, alpha, color));
		}
		++labelIndex;
	}

	
	@Override
	public void drawDiscoveryLabel(PointF center, String text, int alpha,
			Integer color) {
		drawLabelInfo(new LabelInfo(center, text, alpha, color));
	}
	
	public void drawLabels() {
		for(int i = 0; i < labelIndex; ++i) {
			drawLabelInfo(labelsToDraw.get(i));
		}
		labelIndex = 0;
	}

	private void drawLabelInfo(LabelInfo label) {
		if(isVisible(label.getBoxBounds()) == false) {
			return;
		}

		drawDrawable(label.getBoxBounds(), resources.labelBackground, label.getAlpha(), label.getColor());

		resources.textPaint.setAlpha(label.getAlpha());
		Rect textBounds = label.getTextBounds();
		canvas.drawText(label.getText(), textBounds.left, textBounds.bottom, resources.textPaint);
	}

	@Override
	public boolean isVisible(RectF viewRect) {
		return RectF.intersects(viewRect, clipBoundsF);
	}

	private boolean isVisible(Rect viewRect) {
		return Rect.intersects(viewRect, clipBounds);
	}

	private final class LabelInfo {
		private final Rect textBounds;
		private final Rect boxBounds;

		private PointF center;
		private String text;
		private int alpha;
		private Integer color;

		public LabelInfo(PointF center, String text, int alpha, Integer color) {
			this.textBounds = new Rect();
			this.boxBounds = new Rect();
			update(center, text, alpha, color);
		}

		public void update(PointF center, String text, int alpha, Integer color) {
			this.center = center;
			this.text = text;
			this.alpha = alpha;
			this.color = color;
			updateTextBounds();
			updateBoxBounds();
		}

		private void updateTextBounds() {
			if (resources != null && resources.textPaint != null && text != null) {
				resources.textPaint.getTextBounds(text, 0, text.length(), textBounds);
				textBounds.bottom = 0; // ignore underline
				textBounds.offset((int) (center.x - textBounds.width() * 0.5f),
						(int) (center.y + textBounds.height() * 0.5f));
			}
		}

		private void updateBoxBounds() {
			boxBounds.left = textBounds.left - resources.labelPadding.left;
			boxBounds.right = textBounds.right + resources.labelPadding.right;
			boxBounds.top = textBounds.top - resources.labelPadding.top;
			boxBounds.bottom = textBounds.bottom + resources.labelPadding.bottom;
		}

		public Rect getTextBounds() {
			return textBounds;
		}

		public Rect getBoxBounds() {
			return boxBounds;
		}

		public String getText() {
			return text;
		}

		public int getAlpha() {
			return alpha;
		}

		public Integer getColor() {
			return color;
		}
	}
}
