package com.spectralmind.sf4android.view;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class BackgroundRenderer {
	public interface Delegate {
		void draw(Canvas canvas,Activity activity);
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundRenderer.class);

	private final SurfaceHolder surfaceHolder;
	private final Delegate delegate;
	private final AtomicBoolean shouldRun = new AtomicBoolean();
	private Activity mainActivity = null;
	private Thread renderThread;

	public BackgroundRenderer(SurfaceHolder surfaceHolder, Delegate delegate) {
		this.surfaceHolder = surfaceHolder;
		this.delegate = checkNotNull(delegate);
	}

	public void start() {
		checkState(shouldRun.get() == false);
		shouldRun.set(true);
		renderThread = new RenderThread();
		renderThread.start();
	}

	
	public void setActivity(Activity activity)
	{
		mainActivity  = activity;
	}
	
	public void stop() {
		checkState(shouldRun.get());
		shouldRun.set(false);
		try {
			renderThread.join();
		}
		catch(InterruptedException e) {
			LOGGER.error("Interrupted while joining", e);
		}
	}

	private class RenderThread extends Thread {
		@Override
		public void run() {
			while(shouldRun.get()) {
				Canvas canvas = null;
				try {
					canvas = surfaceHolder.lockCanvas();
					if(canvas == null) {
						LOGGER.info("Canvas is null, stopping");
						return;
					}
					delegate.draw(canvas,mainActivity);
				}
				finally {
					if(canvas != null) {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}
	}
}
