package com.spectralmind.sf4android.media;

public class Mood extends MediaGroup<Artist> {
	private final int artistCount;
//	private final MoodDefinition definition;

	Mood(int artistCount, String name) {
		super(name);
		this.artistCount = artistCount;
//		this.definition = definition;
	}

	public int getArtistCount() {
		return artistCount;
	}

	@Override
	protected void addTrack(Track track) {
		super.addTrack(track);
		Artist artist = findOrCreateArtist(track.getArtistName());
		artist.addTrack(track);
	}

	private Artist findOrCreateArtist(String name) {
		Artist artist = findChildWithName(name);
		if(artist == null) {
			artist = new Artist(name);
			addChild(artist);
		}

		return artist;
	}

/*	public PointF getCenter() {
		return definition.getCenter();
	}

	public Integer getColor() {
		return definition.getColor();
	}*/
}
