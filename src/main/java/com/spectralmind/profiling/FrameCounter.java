package com.spectralmind.profiling;

import java.util.Date;

// ThreadSafe
public class FrameCounter {
	private final float[] frameDurationHistory;
	private int frameDurationIndex;
	private Date previousDate;

	public FrameCounter() {
		this(1);
	}

	public FrameCounter(int filteringWindow) {
		this.frameDurationHistory = new float[filteringWindow];
		previousDate = new Date();
	}

	public void increment() {
		Date currentDate = new Date();
		float delta = (currentDate.getTime() - previousDate.getTime()) / 1000.0f;
		synchronized(frameDurationHistory) {
			previousDate = currentDate;
			frameDurationHistory[frameDurationIndex] = delta;
			++frameDurationIndex;
			if(frameDurationIndex >= frameDurationHistory.length) {
				frameDurationIndex = 0;
			}
		}
	}

	public float getAverageFrameDuration() {
		float sum = 0;
		synchronized(frameDurationHistory) {
			for(float duration : frameDurationHistory) {
				sum += duration;
			}
		}
		return sum / frameDurationHistory.length;
	}
}
