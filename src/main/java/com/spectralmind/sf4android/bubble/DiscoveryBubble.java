package com.spectralmind.sf4android.bubble;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PointF;

import com.spectralmind.sf4android.bubble.rendering.RenderContext;
import com.spectralmind.sf4android.media.MediaItem;

public class DiscoveryBubble extends Bubble {

	public final static int DISCOVERY_BUBBLE = 0;
	private float finalRadius;
	private int labelAlpha = 0;
	private float labelAlpha_ = 0;
	private PointF bornOffset;
	private PointF birthCenter;
	
	public DiscoveryBubble(String label, PointF center, float radius,
			Integer color, MediaItem mediaItem) {
		super(label, center, radius, color, mediaItem, null);
		type = DISCOVERY_BUBBLE;
		finalRadius = radius;
		super.radius = 0;
		birthCenter = new PointF(center.x,center.y);
	}
	
	@Override
	public synchronized void draw(Converter converter, RenderContext context,
			Activity activity,ViewStateUpdater v) {
		updateDrawable(converter);
		int c = 1;
		if(shouldDrawBright)
			c = 4;
		if(drawBubbleGlow)
			context.drawDiscoveryBubbleGlow(bounds, 150, Color.WHITE);
		for(int i = 0; i < c;i++)
			context.drawDiscoveryBubble(bounds, (int) (2*radius), Color.WHITE);
		context.drawDiscoveryLabel(center, label, labelAlpha, Color.BLACK);
	}
	
	public synchronized PointF getCenter()
	{
		return center;
	}

	private synchronized void updateDrawable(Converter converter)
	{
		if(radius < finalRadius)
			radius = radius + 20;
		if(labelAlpha_ < 255)
		{
			labelAlpha_ = (float) (labelAlpha_ + 25.5);
			labelAlpha = (int) labelAlpha_;
		}
		else
			labelAlpha = 255;
		
		float dx = (converter.getOffset().x - bornOffset.x) * converter.getScaleFactor();
		float dy = (converter.getOffset().y - bornOffset.y) * converter.getScaleFactor();
		center.x = birthCenter.x -  dx;
		center.y = birthCenter.y -  dy;
		
		bounds.left = center.x - radius;
		bounds.right = center.x + radius;
		bounds.top = center.y - radius;
		bounds.bottom = center.y + radius;
	}
	
	public void rememberOffset(PointF f)
	{
		bornOffset = f;
	}
	
	@Override
	public synchronized Bubble bubbleForLocation(PointF location, Converter converter)
	{
		PointF locationFromCenter = new PointF(location.x - center.x, location.y - center.y);
		if(location.x < center.x - radius || location.x > center.x + radius || location.y < center.y - radius
				|| location.y > center.y + radius) {
			return null;
		}
		else if(locationFromCenter.x * locationFromCenter.x + locationFromCenter.y * locationFromCenter.y > radius * radius) {
			return null;
		}
		else
			return this;
		
	}
}
