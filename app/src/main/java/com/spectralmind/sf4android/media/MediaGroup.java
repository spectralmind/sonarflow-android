package com.spectralmind.sf4android.media;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public abstract class MediaGroup<ChildType extends MediaItem> extends MediaItem implements Comparable<MediaGroup<ChildType>>  {
	public static final int NO_POSITION = -1;
	private static final int NOT_CALCULATED = -1;
	
	private final List<ChildType> children;
	private int numTracks;
	/** The position of this item in the original resource file (if any) */
	private int positionInResource;

	public MediaGroup(String name) {
		this(name, MediaGroup.NO_POSITION);
	}
	
	public MediaGroup(String name, int pos) {
		super(name);
		this.children = Lists.newArrayList();
		this.numTracks = NOT_CALCULATED;
		this.positionInResource = pos;
	}


	public List<ChildType> getChildren() {
		return children;
	}

	@Override
	public int getNumTracks() {
		if(numTracks == NOT_CALCULATED) {
			numTracks = getSumChildTracks();
		}
		return numTracks;
	}

	private int getSumChildTracks() {
		int sum = 0;
		for(MediaItem child : children) {
			sum += child.getNumTracks();
		}
		return sum;
	}

	@Override
	public boolean shouldShowAsBubble() {
		return true;
	}

	public void addTracks(List<Track> tracks) {
		for(Track track : tracks) {
			addTrack(track);
		}
	}

	protected void addTrack(Track track) {
		numTracks = NOT_CALCULATED;
	}

	protected ChildType findChildWithName(String name) {
		for(ChildType child : children) {
			if(Objects.equal(child.getName(), name)) {
				return child;
			}
		}

		return null;
	}

	protected void addChild(ChildType child) {
		children.add(child);
		child.setParent(this);
	}

	protected boolean containsChild(ChildType child) {
		return children.contains(child);
	}

	@Override
	public List<Track> getTracks() {
		List<Track> childTracks = Lists.newArrayListWithCapacity(getNumTracks());
		for(ChildType child : children) {
			childTracks.addAll(child.getTracks());
		}
		return childTracks;
	}
	
	@Override
	public int compareTo(MediaGroup<ChildType> other) {
	    final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;

	    //this optimization is usually worthwhile, and can
	    //always be added
	    if ( this == other ) return EQUAL;
	    
	    if (this.positionInResource < other.positionInResource)
	    	return BEFORE;
	    else if (this.positionInResource  > other.positionInResource)
	    	return AFTER;
	    else return EQUAL;
	}

	/**
	 * @return the positionInResource
	 */
	public int getPositionInResource() {
		return positionInResource;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", getName()).add("#children", children.size()).toString();
	}
}
