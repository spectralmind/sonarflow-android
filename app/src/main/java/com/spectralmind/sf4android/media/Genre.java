package com.spectralmind.sf4android.media;

import java.util.ArrayList;

import com.spectralmind.sf4android.definitions.ClusterDefinition;

public class Genre extends MediaGroup<Artist> {
	// Android system has a lot of genre ids. We map some of them to one of our Genres
	private final ArrayList<Long> ids = new ArrayList<Long>();

	Genre(long id, ClusterDefinition definition, int pos) {
		super(definition.getName(), pos);
		this.ids.add(id);
	}
	
	public void addId(Long id) {
		this.ids.add(id);
	}

	public ArrayList<Long> getIds() {
		return ids;
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
}
