package com.spectralmind.sf4android.media;

import java.util.List;

import android.graphics.drawable.Drawable;

import com.google.common.base.Objects;

public abstract class MediaItem {
	
	private final String name;
	protected Drawable cover = null;
	private MediaGroup<?> parent;
	
	/**
	 * @return the parent
	 */
	public MediaGroup<?> getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(MediaGroup<?> parent) {
		this.parent = parent;
	}

	public MediaItem(String name) {
		this.name = name;
	}

	public void setCover(Drawable cover)
	{
		this.cover = cover;
	}
	
	public Drawable getCover()
	{
		return cover;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", name).toString();
	}

	public int getNumTracks() {
		return 1;
	}

	public boolean shouldShowAsBubble() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}

		if(o == null || o.getClass() != this.getClass()) {
			return false;
		}

		MediaItem other = (MediaItem) o;
		return Objects.equal(name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	public abstract List<Track> getTracks();
}
