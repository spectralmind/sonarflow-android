package com.spectralmind.sf4android.media;

public class Artist extends MediaGroup<Album> {
	public Artist(String name) {
		super(name);
	}

	@Override
	protected void addTrack(Track track) {
		super.addTrack(track);
		Album album = findOrCreateAlbum(track.getAlbumName());
		album.addTrack(track);
	}

	private Album findOrCreateAlbum(String name) {
		Album album = findChildWithName(name);
		if(album == null) {
			album = new Album(name);
			addChild(album);
		}

		return album;
	}
}
