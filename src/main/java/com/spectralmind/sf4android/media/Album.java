package com.spectralmind.sf4android.media;

public class Album extends MediaGroup<Track> {
	public Album(String name) {
		super(name);
	}

	@Override
	protected void addTrack(Track track) {
		if(containsChild(track)) {
			return;
		}

		super.addTrack(track);
		addChild(track);
	}
}
