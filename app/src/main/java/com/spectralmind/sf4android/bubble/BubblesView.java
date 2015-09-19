package com.spectralmind.sf4android.bubble;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.spectralmind.profiling.FrameCounter;
import com.spectralmind.sf4android.MainActivity;
import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.SD.SDMethods;
import com.spectralmind.sf4android.SD.SDResultPackage;
import com.spectralmind.sf4android.SD.SDResultPackage.SDResult;
import com.spectralmind.sf4android.bubble.Animator.AnimationHistory;
import com.spectralmind.sf4android.bubble.Animator.OnFinishedListener;
import com.spectralmind.sf4android.bubble.rendering.BubbleRessources;
import com.spectralmind.sf4android.bubble.rendering.RenderContextImpl;
import com.spectralmind.sf4android.discovery.Discovery;
import com.spectralmind.sf4android.discovery.Discovery.ArtistInfo;
import com.spectralmind.sf4android.media.Artist;
import com.spectralmind.sf4android.media.Track;
import com.spectralmind.sf4android.patched.ScaleGestureDetector;
import com.spectralmind.sf4android.view.BackgroundRenderer;
import com.spectralmind.sf4android.view.PieMenue;
import com.spectralmind.sf4android.view.PointFs;

public class BubblesView extends SurfaceView {
	
	public class PiemenueSaver{
		public PiemenueSaver(Bubble bubbleInstance,float dx, float dy)
		{
			this.bubbleInstance = bubbleInstance;
			this.dx = dx;
			this.dy = dy;
		}
		
		private Bubble bubbleInstance;
		private float dx;
		private float dy;
		
		public Bubble getBubbleInstance() {return bubbleInstance;}
		public float getDx() {return dx;}
		public float getDy() {return dy;}
	}
	
	public interface Delegate {
		void onTappedBubble(Bubble bubble);
		RelativeLayout getRootLayout();
		ImageView getBackArrow();
		PiemenueSaver getSavedPie();
		void savePiemenue(PiemenueSaver save);
		void setAnimationHistory(ArrayList<AnimationHistory> list);
		ArrayList<AnimationHistory> getHistoryList();
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BubblesView.class);

	private final ViewStateUpdater viewStateUpdater;
	private final FrameCounter frameCounter;
	private final BubbleRessources resources;
	private final RenderContextImpl renderContext;
	private final BubbleContainer bubbles;
	private final BackgroundRenderer backgroundRenderer;
	private final GestureDetector gestureDetector;
	private final ScaleGestureDetector scaleGestureDetector;
	private Delegate delegate;
	private Context context;
	private Discovery discovery;
//	private List<Bubble> bubbleSave;
	private SDMethods sdMethod;
	private MainActivity mMainActivity;
	
	private boolean preventFlingOnScaleEnd = false;
	private boolean preventScrollOnTwoFingerScroll = false;
	
	private RelativeLayout rootLayout;
	private ImageView zoomBack;
	private PieMenue currentPie;
	private Bubble currentBubbleGlow;
	private Animation animationVanish;
	private PiemenueSaver currentPieSaver = null; 
	private String[] menueItems_highlighted = {"piemenu_middle_highlighted",
			"piemenu_right_highlighted","piemenu_left_highlighted"};
	
	private float device_density = 1f;
	static private Handler mHandler = new Handler();
	
	public BubblesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.context = context;
		viewStateUpdater = new ViewStateUpdater(context);
		frameCounter = new FrameCounter(200);
		resources = new BubbleRessources(context);
		bubbles = new BubbleContainer();
		renderContext = new RenderContextImpl(resources);
		discovery = new Discovery();
		sdMethod = new SDMethods();
		
		backgroundRenderer = new BackgroundRenderer(getHolder(), new BackgroundRenderer.Delegate() {
			@Override
			public void draw(Canvas canvas,Activity activity) {
				drawInBackground(canvas,activity);
			}
		});
		gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if(preventScrollOnTwoFingerScroll)
				{
					preventScrollOnTwoFingerScroll = false;
					return true;
				}
				viewStateUpdater.scroll(distanceX, distanceY,ViewStateUpdater.CUSTOM_USER);
				destroyPie();
				zoomBack.setVisibility(View.INVISIBLE);
				
				if (mMainActivity != null)
					mMainActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
				
				return true;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if(Math.abs(velocityX) > 2000 || Math.abs(velocityY) > 2000)
					cancelDiscovery();
				viewStateUpdater.fling(velocityX, velocityY);
				destroyPie();
				zoomBack.setVisibility(View.INVISIBLE);
				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				handleDoubleTap(e.getX(), e.getY());
				return true;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				handleTap(e.getX(), e.getY());
				return true;
			}
		});
		
		scaleGestureDetector = new ScaleGestureDetector(context,
				new ScaleGestureDetector.SimpleOnScaleGestureListener() {
					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						cancelDiscovery();
						viewStateUpdater.scale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY()
								,ViewStateUpdater.CUSTOM_USER);
						destroyPie();
						zoomBack.setVisibility(View.INVISIBLE);
						return true;
					}
					
					@Override
					public boolean onScalesScroll(float dx,float dy)
					{
						preventScrollOnTwoFingerScroll = true;
						viewStateUpdater.scroll(dx, dy,ViewStateUpdater.CUSTOM_USER);
						destroyPie();
						return true;
					}
					
					public void onScaleEnd(ScaleGestureDetector detector) {
						LOGGER.debug("Scale end");
						if(viewStateUpdater.getScaleFactor() < viewStateUpdater.minScale)
							viewStateUpdater.zoomToMinScale(detector.getFocusX(), 
									detector.getFocusY());
						else if(viewStateUpdater.getScaleFactor() > viewStateUpdater.maxScale)
							viewStateUpdater.zoomToMaxScale(detector.getFocusX(), detector.getFocusY());
						preventFlingOnScaleEnd = true;
					}
					
				});

		
		getHolder().addCallback(new SurfaceHolderCallback());

		setFocusable(true);
		setFocusableInTouchMode(true);
	}
	
	public void setDeviceSizeInfo(Context activityContext) {
		int i = (activityContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
	
		switch(i)
		{
			case Configuration.SCREENLAYOUT_SIZE_XLARGE:
				scaleGestureDetector.adjustminDeltaTofitDevice(0.5f);
				break;
			case Configuration.SCREENLAYOUT_SIZE_LARGE:
				scaleGestureDetector.adjustminDeltaTofitDevice(0.75f);
				break;
			case Configuration.SCREENLAYOUT_SIZE_NORMAL:
				scaleGestureDetector.adjustminDeltaTofitDevice(1f);
				break;
			case Configuration.SCREENLAYOUT_SIZE_SMALL:
				scaleGestureDetector.adjustminDeltaTofitDevice(1f);
				break;
			default:
				break;		
		}
		
		DisplayMetrics metrics = new DisplayMetrics();
		Activity activity = (Activity) activityContext;
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		switch(metrics.densityDpi)
		{
			case DisplayMetrics.DENSITY_LOW:
				device_density = (float)DisplayMetrics.DENSITY_XHIGH / DisplayMetrics.DENSITY_LOW;
				scaleGestureDetector.adjustminDeltaTofitDevice(device_density);
				break;
			case DisplayMetrics.DENSITY_MEDIUM:
				device_density = (float)DisplayMetrics.DENSITY_XHIGH / DisplayMetrics.DENSITY_MEDIUM;
				scaleGestureDetector.adjustminDeltaTofitDevice(device_density);
				break;
			case DisplayMetrics.DENSITY_HIGH:
				device_density = (float)DisplayMetrics.DENSITY_XHIGH / DisplayMetrics.DENSITY_HIGH;
				scaleGestureDetector.adjustminDeltaTofitDevice(device_density);
				break;
			case DisplayMetrics.DENSITY_XHIGH:
				device_density = 1.0f;
				scaleGestureDetector.adjustminDeltaTofitDevice(device_density);
				break;	
			default:
				device_density = 1.0f;
				scaleGestureDetector.adjustminDeltaTofitDevice(device_density);
				break;
		}
	}
	
	/** Cancels running animations, removes pie menu, removes discovery bubbles. Ready to switch bubbles */
	public void cleanView() {
		zoomBack.setVisibility(View.INVISIBLE);
		cancelDiscovery();
		destroyPie();
		cancelZoomAnimation();
	}
	
	private void cancelDiscovery()
	{
		bubbles.removeAllDiscoveryBubbles();
		if(discovery != null && discovery.isWorking())
			discovery.cancel();
	}
	
	
	private void createDiscoveryBubble(ArrayList<ArtistInfo> artists,Bubble bubble)
	{
		if(artists == null || artists.isEmpty())
		{
			Toast.makeText(getContext(), SonarflowApplication.getAppContext().getString(R.string.no_artists_found)   , Toast.LENGTH_SHORT).show();
			return;	
		}
		
		double angle = 0;
		float r = 90f;
		r = r/device_density;
		float h = r*2.25f;
		
		final double rad = Math.PI / 180;
		for(ArtistInfo artist : artists)
		{
		float dx = (float) (Math.cos(angle*rad) * h);
		float dy = (float) (Math.sin(angle*rad) * h);
		
		float bubbleCenterx = bubble.getCenter().x;
		float bubbleCentery = bubble.getCenter().y;
		DiscoveryBubble discoveryBubble = new DiscoveryBubble(
				artist.name, 
				new PointF(bubbleCenterx + dx, bubbleCentery + dy),
				r,Color.WHITE, null);
		
		discoveryBubble.rememberOffset(PointFs.copy(viewStateUpdater.getOffset()));
		bubbles.add(discoveryBubble);
		angle = angle + 72;
		}
	}

	private void startDiscovery(final Bubble bubble)
	{
		if(bubble == null)
			return;
		
		discovery.setDiscoveryDelegate(new Discovery.DiscoveryDelegate() {
			@Override
			public void onStart() {
				bubble.shouldDrawBright = true;
			}
			@Override
			public void onFinishedSimilarArtistLodaing(ArrayList<ArtistInfo> artists,
					Bubble bubble) {
				createDiscoveryBubble(artists,bubble);
				bubble.shouldDrawBright = false;
			}
			@Override
			public void onErrorMessage(String message) {
				LOGGER.error(message);
				Toast.makeText(context, SonarflowApplication.getAppContext().getString(R.string.problem_discovering_music) , Toast.LENGTH_SHORT).show();
				bubble.shouldDrawBright = false;
				}
			@Override
			public void onCanceled() {
			bubbles.refreshAllDiscoveryBubbles();
			bubble.shouldDrawBright = false;}
		});
		
		if(bubble.type == DiscoveryBubble.DISCOVERY_BUBBLE)
		{		
			if(discovery.getCurrentSearchArtist().intern() == bubble.getLabel().intern())
				return;
			bubble.setFocused(true);
			discovery.cancel();
			discovery.startForSimilarArtist(bubble.getLabel(),bubble);	
		}
		else if(bubble.canDiscovered())
		{
			if(bubble.getMediaItem() == null)
				return;
			String artist = bubble.getMediaItem().getTracks().get(0).getArtistName();
			
			if(!(discovery.getCurrentSearchArtist().intern() == artist.intern()))
			{
				if(discovery.isWorking())
					discovery.cancel();
				bubbles.removeAllDiscoveryBubbles();
				discovery.startForSimilarArtist(artist,bubble);	
			}
		}
		else
			Toast.makeText(getContext(), SonarflowApplication.getAppContext().getString(R.string.zoom_more_into_bubble), 
					Toast.LENGTH_SHORT).show();
	}
	
	public void save(Bundle outState) {		
		viewStateUpdater.saveState(outState);
		
		if (bubbles != null) {
			if(currentPie != null)
				delegate.savePiemenue(currentPieSaver);
		}
	}

	public void restore(Bundle savedInstanceState) {
		viewStateUpdater.restore(savedInstanceState);
	}

	private void destroyPie()
	{
		if(currentBubbleGlow != null)
			currentBubbleGlow.drawBubbleGlow = false;
		if(currentPie != null)
		{
			currentPie.lockTouch();
			animationVanish = AnimationUtils.loadAnimation(getContext(), 
					R.anim.vanish);
			animationVanish.setAnimationListener(new PieAnimationListner(currentPie));
			currentPie.startAnimation(animationVanish);
			currentPie = null;
		}
	}
	
	private void createPieMenue(final float x, final float y)
	{
		final Bubble bubble = bubbleForViewLocation(new PointF(x, y));
		
		if(bubble == null)
			return;
		
		bubble.drawBubbleGlow = true;
		currentBubbleGlow = bubble;
		final PieMenue pie = new PieMenue(getContext());
		
		BitmapDrawable bd=(BitmapDrawable) this.getResources().getDrawable(getResources().
				getIdentifier("piemenu_whole","drawable", getContext().getPackageName()));
		int height=bd.getBitmap().getHeight();
		int width=bd.getBitmap().getWidth();
		
		pie.createPieMenue(width,height,90,"piemenu_whole", menueItems_highlighted);
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				width, height);

		int offsetx = (int) getResources().getDimension(R.dimen.piemenu_center_x);
		int offsety = (int) getResources().getDimension(R.dimen.piemenu_center_y);
//		int offsetx = height/2;
//		int offsety = width/2;
		
		params.leftMargin =  (int) (x - offsetx);
		params.topMargin =  (int) (y - offsety);
		
		LOGGER.debug("offsetx {}, offsety {} ", offsetx, offsety);

		if(bubble.type == DiscoveryBubble.DISCOVERY_BUBBLE)
			pie.lockItem(1);
		else
			cancelDiscovery();

		pie.setOnItemClickedListener(new PieMenue.OnClickedListener() {
			@Override
			public void onItemClicked(int itemNumber) {
				animationVanish = AnimationUtils.loadAnimation(getContext(), R.anim.vanish);
				animationVanish.setAnimationListener(new PieAnimationListner(currentPie));
				pie.startAnimation(animationVanish);
				switch (itemNumber)
				{
				case 0: 
				if(bubble.type == DiscoveryBubble.DISCOVERY_BUBBLE)
					loadTrack(bubble);
				else
				{
					delegate.onTappedBubble(bubble);
					bubble.drawBubbleGlow = false;
				}
				pie.startAnimation(animationVanish);
				pie.lockTouch();
				currentPie = null; 
				break;
				
				case 1:
				if(discovery.isWorking())
					cancelDiscovery();
				zoomBack.setVisibility(View.INVISIBLE);
				viewStateUpdater.setAnimatorOnFinishedListener(new OnFinishedListener(){
					@Override
					public void onAnimationFinished() {
						if(viewStateUpdater.shouldDrawBackArrow())
						{
							zoomBack.setVisibility(View.VISIBLE);
						}
						else
						{
							zoomBack.setVisibility(View.INVISIBLE);
						}
						viewStateUpdater.springBack();
					}
				});
				bubble.drawBubbleGlow = false;
				viewStateUpdater.zoomTo(bubble); 
				pie.startAnimation(animationVanish);
				pie.lockTouch();
				currentPie = null;
				break;
				
				case 2:
				pie.startAnimation(animationVanish);
				bubble.drawBubbleGlow = false;
				pie.lockTouch();
				currentPie = null;
				startDiscovery(bubble);
				break;
				}
			}
		});

		//Bubble shifting!!!
		int left = params.leftMargin;
		int top = params.topMargin;
		int right = params.leftMargin + width;
		int bottom = params.topMargin + height;
		int viewWidth = viewStateUpdater.getViewWidth();
		int viewHeight = viewStateUpdater.getViewHeight();
		RectF bound = new RectF(0,0,viewWidth,viewHeight);
		
		if(!bound.contains(left-2,top -2,right + 2,bottom +2))
		{
			int dx = 0;
			int dy = 0;
			if(left < 0 + 2)
				dx = 0 + 2 - left;
			else if(right > viewWidth - 2)
				dx = viewWidth -2 - right;
			if(top < 0 + 2)
				dy = 0 + 2 - top;
			else if(bottom > viewHeight - 2)
				dy = viewHeight - 2 - bottom;

			final int dx_ = dx;
			final int dy_ = dy;
			
			viewStateUpdater.setAnimatorOnFinishedListener(new OnFinishedListener()
			{
				@Override
				public void onAnimationFinished() {
					currentPie = pie;
					params.leftMargin = params.leftMargin +  dx_;
					params.topMargin = params.topMargin + dy_;
					rootLayout.addView(pie,params);
					float dx = x - bubble.getCenter().x + dx_; 
					float dy = y - bubble.getCenter().y + dy_;
					currentPieSaver = new PiemenueSaver(bubble,dx,dy);
					viewStateUpdater.springBack();
				}	
			});
			zoomBack.setVisibility(View.INVISIBLE);
			viewStateUpdater.scrollBubbleToShowWholePie(-dx,-dy);
		}
		else
		{
			currentPie = pie;
			rootLayout.addView(pie,params);
			float dx = x - bubble.getCenter().x; 
			float dy = y - bubble.getCenter().y;
			currentPieSaver = new PiemenueSaver(bubble,dx,dy);
		}
	}
	
	private void createSavedPieMenue()
	{
		PiemenueSaver save = delegate.getSavedPie();
		if(save != null)
		{
			float x = save.getBubbleInstance().getCenter().x +
					save.getDx();
			float y = save.getBubbleInstance().getCenter().y +
					save.getDy();
			createPieMenue(x,y);
		}
	}
	
	private void handleTap(float x, float y) {		
		if(viewStateUpdater.isAnimateing())
			viewStateUpdater.cancelAnimation();
		destroyPie();
		
		createPieMenue(x,y);
		//LOGGER.debug("Tap: ({},{}) -> {}", new Object[] { x, y, bubble });
		
		if (bubbles.getSize() == 0 && mMainActivity != null) 
			mMainActivity.noMusicDialog();
		
	}
	
	private Bubble bubbleForViewLocation(PointF location) {
		List<Bubble> discoveryBubbles = bubbles.getDiscoveryBubbles();
		
		synchronized(bubbles){
		for(Bubble bubble : discoveryBubbles)
		{
			if(bubble.bubbleForLocation(location, null) != null)
				return bubble;
		}}
		
		Converter converter = viewStateUpdater.newConverter();
		PointF contentLocation = converter.fromView(location);
		
		Bubble result = null;
		synchronized(bubbles){
			for(Bubble bubble : bubbles) {
				if(bubble.type == DiscoveryBubble.DISCOVERY_BUBBLE)
					continue;
			
			Bubble candidate = bubble.bubbleForLocation(contentLocation, converter);
			if(candidate == null)
				continue;
			if(result == null)
				result = candidate;
			else if(candidate.getRadius() < result.getRadius() && candidate.getVisible())
				result = candidate;
				
		}}
		return result;
	}

	private void handleDoubleTap(float x, float y) {
		if(viewStateUpdater.isAnimateing())
			viewStateUpdater.cancelAnimation();
		Bubble bubble = bubbleForViewLocation(new PointF(x, y));
		
		LOGGER.debug("Double Tap: ({},{}) -> {}", new Object[] { x, y, bubble });
		
		if(bubble != null && delegate != null) {
			if(bubble.type == DiscoveryBubble.DISCOVERY_BUBBLE)
				loadTrack(bubble);
			else
			{
				delegate.onTappedBubble(bubble);
				bubble.startShortBubbleGlow();
			}
		}	
	}

	private void loadTrack(final Bubble bubble)
	{
		if(bubble.getMediaItem() != null)
		{
			bubble.startShortBubbleGlow();
			delegate.onTappedBubble(bubble);
			return;
		}
		sdMethod.cancel();
		bubble.drawBubbleGlow = true;
		//Toast.makeText(getContext(), "Loading " + bubble.getLabel(), Toast.LENGTH_SHORT).show();
		sdMethod.getTracksPreviewFromArtistText(bubble.label, 
				new SDMethods.SDSearchResultRequest() {
			@Override
			public void searchResultReady(SDResultPackage resultpackage) {
				if(!resultpackage.getERROR())
				{
					Artist artist = new Artist(bubble.label);
					List<Track> tracks = Lists.newArrayList();
					for(SDResult result : resultpackage.getSDResults())
					{
						tracks.add(new Track(result.getTrack(), result.getArtist(),
								result.getRelease(), Integer.parseInt(result.getTrackNumber()),
								Integer.parseInt(result.getTrackDuration())*1000,
								result.getTrackPreviewUrl()));
					}
					artist.addTracks(tracks);
					bubble.addMediaItem(artist);
					delegate.onTappedBubble(bubble);
					bubble.drawBubbleGlow = false;
				}
				else
				{
					bubble.drawBubbleGlow = false;
					if(resultpackage.getNOMATCH())
						Toast.makeText(context,SonarflowApplication.getAppContext().getString(R.string.no_songs_could_be_found), Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void searchRequestStatus(String status, boolean error) {
				Toast.makeText(getContext(),SonarflowApplication.getAppContext().getString(R.string.no_internet_connection) , Toast.LENGTH_SHORT).show();
			}
			@Override
			public void switchAPIKey() {
				LOGGER.warn("Switching SD API key to " + sdMethod.switchAPIKey());
			}
			
		});
	}
	
	private void startRenderer() {
		backgroundRenderer.start();
	}

	private void stopRenderer() {
		backgroundRenderer.stop();
	}

	public void cancelZoomAnimation() {
		if(viewStateUpdater.isAnimateing())
			viewStateUpdater.cancelAnimation();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int currentEventBuff = (event.getAction() & MotionEvent.ACTION_MASK);
		
		if(currentEventBuff == MotionEvent.ACTION_UP &&
				viewStateUpdater.getScaleFactor() >= viewStateUpdater.minScale) 
			viewStateUpdater.springBack();
		
		scaleGestureDetector.onTouchEvent(event);
		if(preventFlingOnScaleEnd && currentEventBuff == MotionEvent.ACTION_UP)
			preventFlingOnScaleEnd = false;
		else
			gestureDetector.onTouchEvent(event);
		
		return true;
	}
	
	public void replaceBubbles(List<Bubble> bubbles) {
		this.bubbles.clearAll();
		addBubbles(bubbles);
	}

	public void addBubbles(List<Bubble> bubbles) {
		this.bubbles.addAll(bubbles);
//		this.bubbleSave = bubbles;
		if (bubbles != null) {
			for(Bubble bubble : bubbles) {
				viewStateUpdater.expandBounds(bubble.getBounds());
			}
		}
		viewStateUpdater.setMinScaleFactor(viewStateUpdater.getScaleFactorToFitBounds());
		
		if (mMainActivity != null)
			mMainActivity.getActionBar().setDisplayHomeAsUpEnabled(false);
	}

	private void drawInBackground(Canvas canvas,Activity activity) {
		viewStateUpdater.updateOffset();
		Converter converter = viewStateUpdater.newConverter();
		canvas.drawColor(resources.backgroundColor);
		renderContext.setCanvas(canvas);
		
		synchronized(bubbles)
		{
		for(Bubble bubble : bubbles) {
			if(bubble.type == DiscoveryBubble.DISCOVERY_BUBBLE)
				continue;
			bubble.draw(converter, renderContext,activity,viewStateUpdater);
		}}
		
		renderContext.drawLabels();
		
		synchronized(bubbles)
		{
		for(Bubble bubble : bubbles.getDiscoveryBubbles()) {
			bubble.draw(converter, renderContext,activity,viewStateUpdater);
		}}
		
		frameCounter.increment();
	}

	public float getFramesPerSecond() {
		return 1.0f / frameCounter.getAverageFrameDuration();
	}

	public Delegate getDelegate() {
		return delegate;
	}

	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}
	
	public void zoomOut() {
		bubbles.removeAllDiscoveryBubbles();
		destroyPie();
		viewStateUpdater.zoomOut();
		zoomBack.setVisibility(View.INVISIBLE);
		
		if (mMainActivity != null)
			mMainActivity.getActionBar().setDisplayHomeAsUpEnabled(false);
	}
	
	private void recreateSavedViews()
	{			
		zoomBack = delegate.getBackArrow();
		if(viewStateUpdater.shouldDrawBackArrow())
			zoomBack.setVisibility(View.VISIBLE);
		else
			zoomBack.setVisibility(View.INVISIBLE);
		
		zoomBack.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				destroyPie();
				cancelDiscovery();
				zoomBack.setVisibility(View.INVISIBLE);
				viewStateUpdater.reverseZoomHistory();
			}			
		});
		if(delegate.getHistoryList() != null)
		{
			viewStateUpdater.setAnimationHistory(delegate.getHistoryList());
			viewStateUpdater.adjustAnimatorHistoryList();
		}
		
		viewStateUpdater.setAnimatorOnFinishedListener(new OnFinishedListener(){
			@Override
			public void onAnimationFinished() {
				if(viewStateUpdater.shouldDrawBackArrow())
					zoomBack.setVisibility(View.VISIBLE);
				else
					zoomBack.setVisibility(View.INVISIBLE);
				viewStateUpdater.springBack();
			}
		});
		
		if(currentPie == null)
		mHandler.post(new Runnable() {
			public void run() {
				createSavedPieMenue();
			}});
	}
	
	private class SurfaceHolderCallback implements SurfaceHolder.Callback {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			startRenderer();
			rootLayout = delegate.getRootLayout();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//			LOGGER.info("Surface changed: {}, {}, {}", new Object[] { format, width, height });
			viewStateUpdater.setViewSize(width, height);
			viewStateUpdater.setMinScaleFactor(viewStateUpdater.getScaleFactorToFitBounds());
			recreateSavedViews();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			LOGGER.info("Surface destroyed");
			stopRenderer();
		}
	}
	
	public void setActivityReference(MainActivity activity)
	{
		if(backgroundRenderer != null)
			backgroundRenderer.setActivity(activity);
		
		this.mMainActivity = activity;
	}

	private class PieAnimationListner implements AnimationListener {

		private View pie;
		public PieAnimationListner(View m) {
		pie = m;}
		
		public void onAnimationEnd(Animation arg0) {
			new Handler().post(new Runnable() {
		        public void run() {
		        	rootLayout.removeView(pie);
		        }
		    });
		}
		public void onAnimationRepeat(Animation arg0) {}
		public void onAnimationStart(Animation arg0) {}
	}
	
	public void onDestroy() {
		discovery.cancel();
		
		if(viewStateUpdater.isAnimateing())
			viewStateUpdater.cancelAnimation();
		
		if(currentBubbleGlow != null)
			currentBubbleGlow.drawBubbleGlow = false;
		if(viewStateUpdater.getHistoryList() == null)
			return;
		delegate.setAnimationHistory(viewStateUpdater.getHistoryList());
	}
}
