package com.spectralmind.sf4android.bubble;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.spectralmind.sf4android.bubble.Animator.AnimationHistory;
import com.spectralmind.sf4android.bubble.Animator.OnFinishedListener;
import com.spectralmind.sf4android.patched.OverScroller;
import com.spectralmind.sf4android.view.PointFs;

class ViewStateUpdater {
	private static final Logger LOGGER = LoggerFactory.getLogger(ViewStateUpdater.class);

	private static final String VIEW_STATE_KEY = ViewStateUpdater.class.getCanonicalName() + "-State";
	public static float BOUNDS_PADDING = 450;
	public static int BOUNCE_LIMIT = 450;
	
	private static final PointF PROFILE_OFFSET = new PointF(-46.7755f, -31.4934f);
	private static final float PROFILE_SCALE_FACTOR = 1.4566f;

	public final static int CUSTOM_USER = 0;
	public final static int ANIMATOR = 1;
	
	public static class ViewState implements Parcelable {
		public final PointF offset;

		public RectF bounds;
		public float scaleFactor;
		public RectF contentBounds;
		public int viewWidth;
		public int viewHeight;
		public boolean backArrow;
		
		public ViewState(boolean backArrow,RectF bounds, PointF offset, float scaleFactor, int viewWidth, int viewHeight,
				boolean isPieOn,PointF pieCenter) {
			this(backArrow,bounds, offset, scaleFactor, null, viewWidth, viewHeight);
		}

		public ViewState(boolean backArrow,RectF bounds, PointF offset, float scaleFactor, RectF contentBounds, int viewWidth,
				int viewHeight) {
			this.bounds = bounds;
			this.offset = offset;
			this.scaleFactor = scaleFactor;
			this.contentBounds = contentBounds;
			this.viewWidth = viewWidth;
			this.viewHeight = viewHeight;
			this.backArrow = backArrow;
		}

		private static ViewState fromParcel(Parcel parcel) {
			boolean backArrow = (parcel.readInt() != 0);
			boolean hasContentBounds = (parcel.readInt() != 0);
			RectF contentBounds = null;
			if(hasContentBounds) {
				contentBounds = RectF.CREATOR.createFromParcel(parcel);
			}
			return new ViewState(backArrow,RectF.CREATOR.createFromParcel(parcel), PointF.CREATOR.createFromParcel(parcel),
					parcel.readFloat(), contentBounds, parcel.readInt(), parcel.readInt());
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			parcel.writeInt((contentBounds == null ? 0 : 1));
			if(contentBounds != null) {
				contentBounds.writeToParcel(parcel, flags);
			}
			bounds.writeToParcel(parcel, flags);
			offset.writeToParcel(parcel, flags);
			parcel.writeFloat(scaleFactor);
			parcel.writeInt(viewWidth);
			parcel.writeInt(viewHeight);
			parcel.writeInt(backArrow == true ? 1 : 0);
		}

		public static final Parcelable.Creator<ViewState> CREATOR = new Parcelable.Creator<ViewState>() {
			@Override
			public ViewState createFromParcel(Parcel in) {
				return ViewState.fromParcel(in);
			} 

			@Override
			public ViewState[] newArray(int size) {
				return new ViewState[size];
			}
		};

		@Override
		public int describeContents() {
			return 0;	
		}
	}

	private final OverScroller scroller;
	private Animator animator;
	private PointF center;
	private ViewState viewState;

	private boolean fitDuringNextResize;
	
	public float minScale = 1;
	public float maxScale = 10000;
	public boolean lockMaxScale = false;
	
	public ViewStateUpdater(Context context) {
		scroller = new OverScroller(context);
		viewState = new ViewState(false,new RectF(500, 500, 1, 1), new PointF(500, 500), 1, 100, 100,false,null);
		animator = new Animator(this);
	}

	public synchronized void saveState(Bundle outState) {
		outState.putParcelable(VIEW_STATE_KEY, viewState);
	}

	public synchronized void restore(Bundle savedInstanceState) {
		viewState = savedInstanceState.getParcelable(VIEW_STATE_KEY);
		fitDuringNextResize = false;
	}

	public synchronized boolean updateOffset() {
		if(scroller.isFinished()) {
			return false;
		}
		
		scroller.computeScrollOffset();
		viewState.offset.x = Converter.fromView(scroller.getCurrX(), viewState.scaleFactor);
		viewState.offset.y = Converter.fromView(scroller.getCurrY(), viewState.scaleFactor);
		// LOGGER.trace("Scroller running: ({}, {})", viewState.offset.x, viewState.offset.y);
		return true;
	}

	public boolean shouldDrawBackArrow()
	{
		return viewState.backArrow;
	}
	
	public void setDrawBackArrow(boolean value)
	{
		viewState.backArrow = value;
	}
	
	public void reverseZoomHistory()
	{
		animator.reverseHistory();
	}
	
	public synchronized RectF getBounds() {
		return new RectF(viewState.bounds);
	}
	
	public synchronized PointF getOffset() {
		return PointFs.copy(viewState.offset);
	}

	public synchronized int getViewWidth()
	{
		return this.viewState.viewWidth;
	}
	
	public synchronized int getViewHeight()
	{
		return this.viewState.viewHeight;
	}
	
	public synchronized float getScaleFactor()
	{
		return this.viewState.scaleFactor;
	}
	
	public void adjustAnimatorHistoryList()
	{
		for(AnimationHistory h : animator.getHistoryList())
		{
			h.focusX = center.x;
			h.focusY = center.y;
		}
	}
	
	public void setMinScaleFactor(float minScale)
	{
		this.minScale = minScale;
	}
	
	public void maxScaleFactorReached()
	{
		if(lockMaxScale)
			return;
		this.maxScale = viewState.scaleFactor;
		lockMaxScale = true;
	}
	
	public synchronized void setViewSize(int newWidth, int newHeight) {
		PointF oldCenter = getContentPointInViewCenter();
		viewState.viewWidth = newWidth;
		viewState.viewHeight = newHeight;
		
		center = new PointF(newWidth/2,newHeight/2);
		
		PointF newCenter = getContentPointInViewCenter();
		if(fitDuringNextResize) {
			setScaleFactorToFitBounds();
			adjustOffsetToCenterContent();
			fitDuringNextResize = false;
		}
		else {
			viewState.offset.x += oldCenter.x - newCenter.x;
			viewState.offset.y += oldCenter.y - newCenter.y;
		}
	}

	public PointF getContentPointInViewCenter() {
		return Converter.fromView(new PointF(viewState.viewWidth / 2, viewState.viewHeight / 2), viewState.offset,
				viewState.scaleFactor);
	}

	public synchronized Converter newConverter() {
		return new Converter(getOffset(), viewState.scaleFactor);
	}

	public void forceScroll(float x,float y) // just scroll this if scaleFactor wont change
	{
		viewState.offset.x += x;
		viewState.offset.y += y;
	}
	
	public synchronized void scroll(float distanceX, float distanceY, int user) {
		scroller.forceFinished(true);
		if(user != ViewStateUpdater.ANIMATOR)
			cancelAnimation();
		
		viewState.offset.x += Converter.fromView(distanceX, viewState.scaleFactor);
		viewState.offset.y += Converter.fromView(distanceY, viewState.scaleFactor);
	    //LOGGER.trace("Scrolled to: ({}, {})", viewState.offset.x, viewState.offset.y);
		if(viewState.scaleFactor < minScale)
			clipOffsetToBouncePadding();
	}
	
	public synchronized void zoomTo(Bubble bubble) {
		scroller.forceFinished(true);
		animator.zoomIntobubble(bubble);
	}
	
	public synchronized void zoomToMinScale(float x,float y)
	{
		animator.zoomToMinScale(x, y);
	}
	
	public void zoomToMaxScale(float x, float y) {
		animator.zooToMaxScale(x,y);
	}
	
	public synchronized void scrollBubbleToShowWholePie(int dx, int dy)
	{
		animator.zoomBubbleToShowWholePie(dx,dy);
	}
	
	private void clipOffsetToBouncePadding() {
		float contentBouncePadding = Converter.fromView(BOUNCE_LIMIT, viewState.scaleFactor);
		viewState.offset.x = clip(viewState.offset.x, leftOffsetLimit() - contentBouncePadding, rightOffsetLimit()
				+ contentBouncePadding);
		viewState.offset.y = clip(viewState.offset.y, topOffsetLimit() - contentBouncePadding, bottomOffsetLimit()
				+ contentBouncePadding);
	}

	private float clip(float value, float min, float max) {
		if(min > max) {
			return (min + max) / 2;
		}

		if(value < min) {
			return min;
		}

		if(value > max) {
			return max;
		}

		return value;
	}

	public synchronized void fling(float velocityX, float velocityY) {
		cancelAnimation();
		if(isOutOfBounds()) {
			LOGGER.debug("Fling: Forcing finished");
			scroller.forceFinished(true);
			springBack();
			return;
		}

		Converter converter = newConverter();
		fling((int) converter.toView(viewState.offset.x), (int) converter.toView(viewState.offset.y), (int) -velocityX,
				(int) -velocityY, (int) converter.toView(leftOffsetLimit()),
				(int) converter.toView(rightOffsetLimit()), (int) converter.toView(topOffsetLimit()),
				(int) converter.toView(bottomOffsetLimit()), BOUNCE_LIMIT, BOUNCE_LIMIT);
	}

	private void fling(int x, int y, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY,
			int horizontalPadding, int verticalPadding) {
		LOGGER.debug("Fling: ({},{}) x: {}/{}/{} y: {}/{}/{}", new Object[] { velocityX, velocityY, minX, x, maxX,
				minY, y, maxY });
		scroller.fling(x, y, velocityX, velocityY, minX, maxX, minY, maxY, horizontalPadding, verticalPadding);
	}

	private boolean isOutOfBounds() {
		return viewState.offset.x < leftOffsetLimit() || viewState.offset.x > rightOffsetLimit()
				|| viewState.offset.y < topOffsetLimit() || viewState.offset.y > bottomOffsetLimit();
	}

	private float leftOffsetLimit() {
		return viewState.bounds.left - Converter.fromView(BOUNDS_PADDING, viewState.scaleFactor);
	}

	private float topOffsetLimit() {
		return viewState.bounds.top - Converter.fromView(BOUNDS_PADDING, viewState.scaleFactor);
	}

	private float rightOffsetLimit() {
		return viewState.bounds.right - Converter.fromView(viewState.viewWidth - BOUNDS_PADDING, viewState.scaleFactor);
	}

	private float bottomOffsetLimit() {
		return viewState.bounds.bottom
				- Converter.fromView(viewState.viewHeight - BOUNDS_PADDING, viewState.scaleFactor);
	}

	public synchronized void springBack() {
		if(scroller.isFinished() == false) {
			return;
		}

		Converter converter = newConverter();
		springBack(converter, (int) converter.toView(viewState.offset.x), (int) converter.toView(viewState.offset.y),
				(int) converter.toView(leftOffsetLimit()), (int) converter.toView(rightOffsetLimit()),
				(int) converter.toView(topOffsetLimit()), (int) converter.toView(bottomOffsetLimit()));
	}

	private void springBack(Converter converter, int x, int y, int minX, int maxX, int minY, int maxY) {
		if(minX > maxX) {
			int delta = minX - maxX;
			minX -= delta / 2;
			maxX += delta - delta / 2;
			checkState(minX <= maxX);
		}
		if(minY > maxY) {
			int delta = minY - maxY;
			minY -= delta / 2;
			maxY += delta - delta / 2;
			checkState(minY <= maxY);
		}
		scroller.getCurrX();
		LOGGER.debug("Spring back: x: {}/{}/{} y: {}/{}/{}", new Object[] { minX, x, maxX, minY, y, maxY });
		scroller.springBack(x, y, minX, maxX, minY, maxY);
	}

	public synchronized void scale(float scaleFactor, float focusX, float focusY,int user) {
		if(user != ViewStateUpdater.ANIMATOR)
			cancelAnimation();
		
		PointF contentFocus = new PointF(focusX / viewState.scaleFactor + viewState.offset.x, focusY
				/ viewState.scaleFactor + viewState.offset.y);
		
		if((viewState.scaleFactor > maxScale || viewState.scaleFactor < minScale)
				&& user == ViewStateUpdater.CUSTOM_USER) // slow user scaling if limit is crossed
			scaleFactor = (float) Math.sqrt(scaleFactor);
		
		viewState.scaleFactor *= scaleFactor;
//		if(viewState.scaleFactor < maxScale && lockMaxScale)
//			lockMaxScale = false;
		
		viewState.offset.x = (contentFocus.x * viewState.scaleFactor - focusX) / viewState.scaleFactor;
		viewState.offset.y = (contentFocus.y * viewState.scaleFactor - focusY) / viewState.scaleFactor;
		// LOGGER.trace("Scaled to: {} with offset ({}, {})", new Object[] { viewState.scaleFactor, viewState.offset.x,
		// viewState.offset.y });
		clipOffsetToBouncePadding();
	}

	public synchronized void expandBounds(RectF toInclude) {
		if(viewState.contentBounds == null) {
			viewState.contentBounds = new RectF(toInclude);
		}
		else {
			viewState.contentBounds.union(toInclude);
		}
		viewState.bounds = new RectF(viewState.contentBounds);

		setScaleFactorToFitBounds();
		adjustOffsetToCenterContent();
		
		fitDuringNextResize = true;

		// setProfilingState();
	}
	
	public void adjustOffsetToCenterContent() {
		viewState.offset.x = viewState.bounds.left
				- (Converter.fromView(viewState.viewWidth, viewState.scaleFactor) - viewState.bounds.width()) * 0.5f;
		viewState.offset.y = viewState.bounds.top
				- (Converter.fromView(viewState.viewHeight, viewState.scaleFactor) - viewState.bounds.height()) * 0.5f;
	}
	
	public PointF getOffsetToCenterContent()
	{
		float x = viewState.bounds.left
				- (Converter.fromView(viewState.viewWidth, viewState.scaleFactor) - viewState.bounds.width()) * 0.5f;
		float y = viewState.bounds.top
				- (Converter.fromView(viewState.viewHeight, viewState.scaleFactor) - viewState.bounds.height()) * 0.5f;
		return new PointF(x,y);
	}

	public void setScaleFactorToFitBounds() {
		viewState.scaleFactor = Math.min(viewState.viewWidth / viewState.bounds.width(), viewState.viewHeight
				/ viewState.bounds.height());
	}

	public float getScaleFactorToFitBounds() {
		return Math.min(viewState.viewWidth / viewState.bounds.width(), viewState.viewHeight
				/ viewState.bounds.height());
	}
	
//	private void setProfilingState() {
//		viewState.offset.x = PROFILE_OFFSET.x;
//		viewState.offset.y = PROFILE_OFFSET.y;
//		viewState.scaleFactor = PROFILE_SCALE_FACTOR;
//	}

	public synchronized void zoomOut() {
		cancelAnimation();
		animator.zoomOut();
	}

	public boolean isAnimateing() {
		return animator.isAnimateing();
	}
	
	public boolean isAnimatorLocked()
	{
		return animator.islocked();
	}

	public void cancelAnimation()
	{
		animator.forceFinished();
		this.setDrawBackArrow(false);
	}

	public void setAnimatorOnFinishedListener(OnFinishedListener onFinishedListener) {
		if(animator != null)
			animator.setOnFinishedListener(onFinishedListener);
	}
	
	public void setAnimationHistory(ArrayList<AnimationHistory> list)
	{
		animator.setHistoryList(list);
	}
	
	public ArrayList<AnimationHistory> getHistoryList()
	{
		return animator.getHistoryList();
	}
}
