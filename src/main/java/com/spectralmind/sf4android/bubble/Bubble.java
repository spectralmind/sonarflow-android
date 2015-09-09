package com.spectralmind.sf4android.bubble;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;

import com.google.common.base.Objects;
import com.spectralmind.profiling.MyTimer;
import com.spectralmind.sf4android.Tasks.GetCoverartTaskSD;
import com.spectralmind.sf4android.bubble.rendering.OverridingRenderContext;
import com.spectralmind.sf4android.bubble.rendering.RenderContext;
import com.spectralmind.sf4android.media.Album;
import com.spectralmind.sf4android.media.Artist;
import com.spectralmind.sf4android.media.Genre;
import com.spectralmind.sf4android.media.MediaItem;
import com.spectralmind.sf4android.media.Mood;
import com.spectralmind.sf4android.media.Track;
import com.spectralmind.sf4android.player.PlayerService;

public class Bubble {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bubble.class);
	
	private static final float FADE_SIZE = 25;
	private static final float MINIMUM_SIZE_TO_SHOW_CHILDREN = 340;
	private static final float MINIMUM_SIZE_TO_SHOW_LABEL = 50;
	private static final float MAXIMUM_SIZE_TO_SHOW_LABEL = MINIMUM_SIZE_TO_SHOW_CHILDREN + FADE_SIZE;
	public static final float MAXIMUM_SIZE_TO_SHOW_BACKGROUND = MINIMUM_SIZE_TO_SHOW_CHILDREN + FADE_SIZE;

	public static final int MAX_SIZE_TOLERANCE = (int) (MAXIMUM_SIZE_TO_SHOW_BACKGROUND * 0.5);

	protected float radius;
	protected final String label;
	private final Integer color;
	private final Converter childrenConverter;
	private final OverridingRenderContext childrenRenderContext;
	private final BubbleContainer children;
	private Bubble parent;
	protected final RectF bounds;
	protected final RectF viewBounds;
	protected final PointF viewCenter;
	protected MediaItem mediaItem;
	protected PointF center;

	private float currentSize = 0;
	private boolean visible = false;
	private boolean canDiscovered;
	public boolean shouldDrawBright = false;
	public boolean drawBubbleGlow = false;

	public int type = 1;
	private boolean focused = false;

	private boolean hasCheckedCover = false;
	private Drawable cover = null; 


	public Bubble(String label, float radius, MediaItem mediaItem, Bubble p) {
		this(label, new PointF(0, 0), radius, null, mediaItem, p);
	}

	public Bubble(String label, PointF center, float radius, Integer color, MediaItem mediaItem, Bubble p) {
		this.center = center;
		this.radius = radius;
		this.label = label;
		this.color = color;
		this.children = new BubbleContainer();
		this.childrenConverter = new Converter();
		this.childrenRenderContext = new OverridingRenderContext(color);
		this.bounds = new RectF();
		this.viewBounds = new RectF();
		this.viewCenter = new PointF(0, 0);
		this.mediaItem = mediaItem;
		this.parent = p;
		this.canDiscovered = false;
		
		if(this.radius < MINIMUM_SIZE_TO_SHOW_LABEL
				&& (this.mediaItem instanceof Genre || this.mediaItem instanceof Mood))
			this.radius = MINIMUM_SIZE_TO_SHOW_LABEL - MINIMUM_SIZE_TO_SHOW_LABEL / 5;
			
		updateBounds();
	}

	public void addMediaItem(MediaItem mediaItem)
	{
		if(this.mediaItem == null)
			this.mediaItem = mediaItem;
	}

	private void updateBounds() {
		bounds.left = center.x - radius;
		bounds.top = center.y - radius;
		bounds.right = center.x + radius;
		bounds.bottom = center.y + radius;
	}

	public synchronized PointF getCenter() {
		return this.viewCenter;
	}

	public void setCenter(PointF center) {
		this.center = center;
		updateBounds();
	}

	public void startShortBubbleGlow()
	{
		this.drawBubbleGlow = true;
		new MyTimer(1500).start(
				new MyTimer.Callback(){
					@Override
					public void onTimerEnd() {
						drawBubbleGlow = false;
					}
				});
	}
	
	public float getRadius() {
		return radius;
	}

	public RectF getBounds() {
		return bounds;
	}

	public String getLabel() {
		return label;
	}

	public MediaItem getMediaItem() {
		return mediaItem;
	}

	public synchronized Bubble bubbleForLocation(PointF location, Converter converter) {
		if(location.x < center.x - radius || location.x > center.x + radius || location.y < center.y - radius
				|| location.y > center.y + radius) {
			return null;
		}

		PointF locationFromCenter = new PointF(location.x - center.x, location.y - center.y);
		if(locationFromCenter.x * locationFromCenter.x + locationFromCenter.y * locationFromCenter.y > radius * radius) {
			return null;
		}

		float renderSize = getRenderSize(converter);
		if(shouldShowChildren(renderSize) == false) {
			return this;
		}

		Bubble hitChild = children.bubbleForLocation(locationFromCenter, converter);
		if(hitChild == null && shouldShowBackground(renderSize)) {
			return this;
		}

		return hitChild;
	}

	public synchronized void draw(Converter converter, RenderContext context,
			Activity activity,ViewStateUpdater v) {
		updateViewState(converter);
		if(shouldDraw(converter, context) == false) {
			return;
		}

		int c = 1;
		if(shouldDrawBright)
		{
			c = 4;
		}
		for(int i = 0; i < c; i++)
		{
			drawBackground(converter, context);
		}
		if(drawBubbleGlow)
		{
			drawBubbleGlow(converter,context);
		}
	
		float renderSize = getRenderSize(converter);

		if((mediaItem instanceof Track || mediaItem instanceof Album || mediaItem instanceof Artist) && 
				renderSize > 170 && !shouldShowChildren(renderSize))
		{
			Rect bound = new Rect((int)(viewBounds.left + viewBounds.width()/3), 
					(int)(viewBounds.top + viewBounds.height()/7), 
					(int)(viewBounds.right - viewBounds.width()/3),
					(int)(viewBounds.top + viewBounds.height()/2));

			
			context.drawCoverArt(getCover(activity), bound);
		}

		
		if((mediaItem instanceof Genre || mediaItem instanceof Mood) && !shouldShowChildren(renderSize))
		{
			context.drawLabel(viewCenter, label, 255, color);
			canDiscovered = true;
		}
		else if(shouldShowLabel(renderSize)) {
			context.drawLabel(viewCenter, label, getLabelAlpha(renderSize), color);
		}

		if(shouldShowChildren(renderSize)) {
			drawChildren(converter, context,activity,v);
		}
		if(children.isEmpty() && currentSize > MAXIMUM_SIZE_TO_SHOW_BACKGROUND - FADE_SIZE)
			v.maxScaleFactorReached();
	}

	private void updateViewState(Converter converter) {
		converter.toView(bounds, viewBounds);
		converter.toView(center, viewCenter);
	}

	private void drawChildren(Converter converter, RenderContext context,Activity activity,
			ViewStateUpdater v) {
		float renderSize = getRenderSize(converter);
		if(shouldShowChildren(renderSize) == false) {
			return;
		}

		childrenConverter.copyWithOffsetDelta(converter, center);
		childrenRenderContext.setOverrides(context, getChildrenMaxAlpha(renderSize));
		for(Bubble child : children) {
			child.draw(childrenConverter, childrenRenderContext,activity,v);
		}
	}

	private boolean shouldDraw(Converter converter, RenderContext context) {
		return context.isVisible(viewBounds);
	}

	private float getRenderSize(Converter converter) {
		return converter.toView(2 * radius);
	}

	private void drawBackground(Converter converter, RenderContext context) {
		float renderSize = getRenderSize(converter);
		if(shouldShowBackground(renderSize) == false) {
			this.visible = false;
			return;
		}
		this.visible = true;

		context.drawBubble(viewBounds, getBackgroundAlpha(renderSize), color);
	}
	
	private void drawBubbleGlow(Converter converter, RenderContext context) {
		float renderSize = getRenderSize(converter);
		if(shouldShowBackground(renderSize) == false) {
			return;
		}
		context.drawBubbleGlow(viewBounds, 150, Color.WHITE);
	}

	private boolean shouldShowBackground(float size) {
		boolean buff = size <= MAXIMUM_SIZE_TO_SHOW_BACKGROUND || shouldShowChildren(size) == false;
		this.currentSize = size;
		return buff;
	}

	private boolean shouldShowChildren(float size) {
		return size >= MINIMUM_SIZE_TO_SHOW_CHILDREN && children.isEmpty() == false;
	}

	private boolean shouldShowLabel(float renderSize) {
		return renderSize >= MINIMUM_SIZE_TO_SHOW_LABEL
				&& (renderSize <= MAXIMUM_SIZE_TO_SHOW_LABEL || shouldShowChildren(renderSize) == false);
	}

	private synchronized int getLabelAlpha(float renderSize) {
		int buff = getFadeAlpha(renderSize, MINIMUM_SIZE_TO_SHOW_LABEL, MAXIMUM_SIZE_TO_SHOW_LABEL);
		if(buff > 100)
			this.canDiscovered = true;
		else
			this.canDiscovered = false;
		return buff;
	}

	public void resetVisible()
	{
		this.visible = false;
	}

	public boolean getVisible()
	{
		return this.visible;
	}

	public boolean canDiscovered()
	{
		return canDiscovered;
	}

	public synchronized float getCurrentSize()
	{
		return this.currentSize;
	}

	public boolean hasChildren()
	{
		return !this.children.isEmpty();
	}

	private int getFadeAlpha(float renderSize, float minimum, float maximum) {
		if(renderSize < minimum + FADE_SIZE) {
			return (int) (255 * (renderSize - minimum) / (float) FADE_SIZE);
		}
		else if(shouldShowChildren(renderSize) && renderSize > maximum - FADE_SIZE) {
			return (int) (255 * (maximum - renderSize) / (float) FADE_SIZE);
		}
		else {
			return 255;
		}
	}

	private int getChildrenMaxAlpha(float renderSize) {
		return 255 - getBackgroundAlpha(renderSize);
	}

	private int getBackgroundAlpha(float renderSize) {
		return getFadeAlpha(renderSize, -FADE_SIZE, MAXIMUM_SIZE_TO_SHOW_BACKGROUND);
	}

	public void addChildren(List<Bubble> bubbles) {
		children.addAll(bubbles);
	}

	public synchronized BubbleContainer getChildren()
	{
		return this.children;
	}

	public boolean collidesWith(Bubble other) {
		double sqrDistance = Math.pow(center.x - other.center.x, 2) + Math.pow(center.y - other.center.y, 2);
		double sqrRadius = Math.pow(radius + other.radius, 2);
		return sqrDistance < sqrRadius;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("label", label).add("center", center).add("radius", radius).add("mediaItem", mediaItem).toString();
	}


	public boolean isFocused() {
		return this.focused;
	}

	public void setFocused(boolean b) {
		this.focused = b;
	}

	/**
	 * @return the cover
	 */
	public Drawable getCover(Activity activity) {
		// if called the first time
		if (cover == null && !hasCheckedCover)
		{
			// Do not check again for the cover
			hasCheckedCover = true;
			
			// --- Artist ------
			if (mediaItem instanceof Artist) {
				// Try remote method, start the async task non-blocking
				// This task will update the cover property. 
				if(activity != null)
					activity.runOnUiThread(new Runnable(){
						@Override
						public void run() {
							//new GetCoverartTaskLastfm(Bubble.this).execute();	
							new GetCoverartTaskSD(Bubble.this).execute();
						}
					});
			}
			
			// --- Album ------
			if (mediaItem instanceof Album) {
				// Try local method only if we have at least one track
				if (mediaItem.getNumTracks() > 0) {
					byte[] buff = null;
					try {
						MediaMetadataRetriever mmr = new MediaMetadataRetriever();
						mmr.setDataSource(mediaItem.getTracks().get(0).getPath());
						buff = mmr.getEmbeddedPicture();
					} catch (Exception ex) {
						LOGGER.warn("Could not get cover from " + mediaItem.getTracks().get(0).getPath());
					}
					if(buff != null)
					{
						ByteArrayInputStream is = new ByteArrayInputStream(buff);
						setCover(Drawable.createFromStream(is, "coverArt"));
					}
				}
				// if cover is still null
				if (cover == null) {
					// Try remote method, start the async task non-blocking
					// This task will update the cover property. 
					if(activity != null)
						activity.runOnUiThread(new Runnable(){
							@Override
							public void run() {
								//new GetCoverartTaskLastfm(Bubble.this).execute();	
								new GetCoverartTaskSD(Bubble.this).execute();
							}
						});
				}
			}


			// --- Track ------
			if (mediaItem instanceof Track) {
				// check parent (album)
				if (parent != null && cover == null) setCover(parent.cover);
			}
		}
		
		return cover;
	}

	/**
	 * @param cover the cover to set
	 */
	public void setCover(Drawable cover) {
		if(cover == null)
		{
			this.hasCheckedCover = false;
			return;
		}
		this.cover = cover;
		mediaItem.setCover(cover);
		// set cover for children, too IF this is a Album bubble
		if (mediaItem instanceof Album) {
			for (Bubble b : children) {
				b.setCover(cover);
			}
		}
	}

}
