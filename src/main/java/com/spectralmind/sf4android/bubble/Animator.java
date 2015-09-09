package com.spectralmind.sf4android.bubble;

import java.util.ArrayList;

import android.graphics.PointF;
import android.os.Handler;
import android.view.animation.DecelerateInterpolator;

public class Animator{

	public interface OnFinishedListener
	{
		void onAnimationFinished();
	}
	
	public class AnimationHistory{
		public int scrollStepCount;
		public float focusX;
		public float focusY;
		public int scaleStepCount;
		public float xPerStep;
		public float yPerStep;
	}
	
	private ViewStateUpdater viewStateUpdater;
	private Handler handler = new Handler();
	private OnFinishedListener onFinishedListener;
	
	private boolean animateing = false;
	private boolean lock = false;
	
	private int counter = 0;
	private int counterBuff;

	private ArrayList<AnimationHistory> historyList = new ArrayList<AnimationHistory>();
	private AnimationHistory history;
	
	private Runnable scaleToMin;
	private Runnable scaleToMax;
	private Runnable scrollBubbleToShowPie;
	
	public void setHistoryList(ArrayList<AnimationHistory> list)
	{
		historyList = list;
	}
	
	public ArrayList<AnimationHistory> getHistoryList()
	{
		return historyList;
	}
	
	public Animator(ViewStateUpdater v)
	{
		this.viewStateUpdater = v;
	}
	
	public void zoomOut() {
		start();
		final float finalScale = viewStateUpdater.getScaleFactorToFitBounds();
		final float ratio = finalScale / viewStateUpdater.getScaleFactor();
		final float scaleperStep = (float) Math.pow(Math.E, Math.log(ratio)/15);
		handler.post(new Runnable(){
			@Override
			public void run() {
				if(counter < 15 && ratio < 1.05)
				{
					viewStateUpdater.scale(scaleperStep, viewStateUpdater.getViewWidth()/2, 
							viewStateUpdater.getViewHeight()/2, ViewStateUpdater.ANIMATOR);
					counter++;
					handler.postDelayed(this, 20);
				}
				else
				{
					counter = 0;
					float deltaX = viewStateUpdater.getOffsetToCenterContent().x 
							- viewStateUpdater.getOffset().x;
					float deltaY = viewStateUpdater.getOffsetToCenterContent().y 
							- viewStateUpdater.getOffset().y;
					final float XperStep = deltaX / 10;
					final float YperStep = deltaY / 10;
					handler.post(new Runnable(){
						@Override
						public void run() {
							if(counter < 10)
							{
								viewStateUpdater.forceScroll(XperStep, YperStep);
								counter++;
								handler.postDelayed(this, 20);
							}
							else
								stop();
						}
					});
				}
			}
		});
	}
	
	public void zoomToMinScale(final float x,final float y)
	{
		start();
		lock = true;
		float ratio = viewStateUpdater.minScale / viewStateUpdater.getScaleFactor();
		final int steps = (int) (Math.log(ratio)/Math.log(1.025));
		scaleToMin = new Runnable(){
			float focusX = x;
			float focusY = y;
			int step = steps;
			
			@Override
			public void run() {
				if(counter < step)
				{
				viewStateUpdater.scale(1.025f, focusX, focusY, ViewStateUpdater.ANIMATOR);
				handler.postDelayed(this, 5);
				counter++;
				}
				else
				{
					lock = false;
					stop();
				}
			}
		};
		handler.post(scaleToMin);
	}
	
	public void zooToMaxScale(final float x, final float y) {
		start();
		lock = true;
		float ratio = viewStateUpdater.getScaleFactor() / viewStateUpdater.maxScale;
		final int steps = Math.abs((int)(Math.log(ratio)/Math.log(0.975)));
		scaleToMax = new Runnable(){
			float focusX = x;
			float focusY = y;
			int step = steps; 
			
			@Override
			public void run() {
				if(counter < step)
				{
				viewStateUpdater.scale(0.975f, focusX, focusY, ViewStateUpdater.ANIMATOR);
				handler.postDelayed(this, 5);
				counter++;
				}
				else
				{
					lock = false;
					stop();
				}
			}
		};
		handler.post(scaleToMax);
	}	
	
	public void zoomIntobubble(final Bubble bubble)
	{
		PointF f = bubble.getCenter();
		float dX = f.x - viewStateUpdater.getViewWidth() / 2;
		float dY = f.y - viewStateUpdater.getViewHeight() / 2;
		
		if(Math.abs(dX) < 1 && Math.abs(dY) < 1 && !bubble.hasChildren()
		&& bubble.getCurrentSize() >= Bubble.MAXIMUM_SIZE_TO_SHOW_BACKGROUND - Bubble.MAX_SIZE_TOLERANCE)
			return;
		
		final float xPerStep = dX / 20;
		final float yPerStep = dY / 20;
			
		bubble.resetVisible();
		
		this.history = new AnimationHistory();
		start();
		handler.post(new Runnable(){
			@Override
			public void run() {
				if(counter < 20)
				{
					viewStateUpdater.scroll(xPerStep, yPerStep,ViewStateUpdater.ANIMATOR);
					counter++;
					handler.postDelayed(this, 20);
				}
				else
				{
					history.xPerStep = xPerStep;
					history.yPerStep = yPerStep;
					history.scrollStepCount = counter;
					counter = 0;
					handler.post(new Runnable(){
						private boolean shouldStopScaling()
						{
							if(!bubble.getVisible())
								return true;
							return false;
						}
						@Override
						public void run() {
							if(!bubble.hasChildren())
							{
								if(bubble.getCurrentSize() < Bubble.MAXIMUM_SIZE_TO_SHOW_BACKGROUND)
								{
								viewStateUpdater.scale((float) 1.015, bubble.getCenter().x, bubble.getCenter().y,
										ViewStateUpdater.ANIMATOR);
								counter++;
								handler.postDelayed(this, 10);
								}
								else
								{
									history.focusX = bubble.getCenter().x;
									history.focusY = bubble.getCenter().y;
									history.scaleStepCount = counter;
									historyList.add(history);
									viewStateUpdater.setDrawBackArrow(true);
									stop();
								}
							}
							else if(!shouldStopScaling())
							{
							viewStateUpdater.scale((float) 1.015, bubble.getCenter().x, bubble.getCenter().y,
									ViewStateUpdater.ANIMATOR);
							counter++;
							counterBuff = counter + 5;
							handler.postDelayed(this, 10);
							}
							else 
							{   //we need a little rest zooming
								if(counterBuff > counter){
								viewStateUpdater.scale(1.015f,bubble.getCenter().x,bubble.getCenter().y,
										ViewStateUpdater.ANIMATOR);
								counter++;
								handler.postDelayed(this, 10);
								}
								else
								{
									history.focusX = bubble.getCenter().x;
									history.focusY = bubble.getCenter().y;
									history.scaleStepCount = counter;
									historyList.add(history);
									viewStateUpdater.setDrawBackArrow(true);
									stop();
								}
							}
						}
					});
				}
			}
		});
	}
	
	public void zoomBubbleToShowWholePie(final int x, final int y) 
	{
		scrollBubbleToShowPie = new Runnable(){
			float dx = x;
			float dy = y;
			float nextX = 0;
			float nextY = 0;
			float scrolledX = 0;
			float scrolledY = 0;
			float param = 0.05f;
			DecelerateInterpolator i = new DecelerateInterpolator();
			@Override
			public void run() {
				if(counter < 20)
				{
					float v = i.getInterpolation(param);
					param =+ 0.05f;
					nextX = dx * v;
					nextY = dy * v;
					float deltaX = nextX - scrolledX;
					float deltaY = nextY - scrolledY;
					scrolledX =+ deltaX;
					scrolledY =+ deltaY;
					viewStateUpdater.scroll(deltaX,deltaY,ViewStateUpdater.ANIMATOR);	
					counter++;
					handler.postDelayed(this,20);
				}
				else
				{
					stop();
					reset();
				}
			}
		};
		start();
		handler.post(scrollBubbleToShowPie);
	}
	
	public void reverseHistory()
	{
		if(historyList.size() < 1)
			return;
			start();
			handler.post(new Runnable(){
				@Override
				public void run() {
					if(counter < historyList.get(historyList.size()-1).scaleStepCount)
					{
						viewStateUpdater.scale((float)(1/1.015), historyList.get(historyList.size()-1).focusX,
								historyList.get(historyList.size()-1).focusY,ViewStateUpdater.ANIMATOR);
						counter++;
						handler.postDelayed(this, 10);
					}
					else
					{
						handler.post(new Runnable(){
							@Override
							public void run() {
								if(counter < historyList.get(historyList.size()-1).scrollStepCount)
								{
									float dx = historyList.get(historyList.size()-1).xPerStep;
									float dy = historyList.get(historyList.size()-1).yPerStep;
									viewStateUpdater.scroll(-dx,-dy,ViewStateUpdater.ANIMATOR);
									counter++;
									handler.postDelayed(this, 20);
								}
								else
								{
									historyList.remove(historyList.size()-1);
									if(historyList.size() == 0)
										viewStateUpdater.setDrawBackArrow(false);
									stop();
								}
							}
						});
						counter = 0;
					}
				}
			});
	}
	
	public boolean isAnimateing()
	{
		return this.animateing;
	}

	public void setOnFinishedListener(OnFinishedListener o)
	{
		this.onFinishedListener = o;
	}
	
	public synchronized void forceFinished()
	{
		this.reset();
	}
	
	private void start()
	{
		this.animateing = true;
		counter = 0;
	}
	
	private void stop()
	{
		this.animateing = false;
		if(onFinishedListener != null)
			onFinishedListener.onAnimationFinished();
	}
	
	private void reset()
	{
		if(lock)
			return;
		
		viewStateUpdater.setDrawBackArrow(false);
		handler.removeCallbacksAndMessages(null);
		this.onFinishedListener = null;
		this.animateing = false;
		this.historyList.removeAll(historyList);
		this.history = null;
		counter = 0;
	}

	public boolean islocked() {
		return lock;
	}
}
