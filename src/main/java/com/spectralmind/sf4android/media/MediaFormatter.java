package com.spectralmind.sf4android.media;

public final class MediaFormatter {

	private MediaFormatter() {
	}

	public static String formatMsDuration(int duration) {
		int seconds = duration / 1000;
		int minutes = seconds / 60;
		int hours = minutes / 60;
		seconds %= 60;
		minutes %= 60;

		if(hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		else {
			return String.format("%d:%02d", minutes, seconds);
		}
	}

}
