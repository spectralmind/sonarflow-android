package com.spectralmind.sf4android.player;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.media.Track;

class PlaybackQueue {
	private static final int NO_INDEX = -1;

	private final Random random;
	private boolean shuffle;
	private boolean repeatAll;
	private Track currentTrack;
	private int currentTrackIndex;
	private List<Track> queue;
	private List<Integer> itemOrderIndices;

	public PlaybackQueue() {
		this.random = new Random();
		this.currentTrackIndex = NO_INDEX;
		this.queue = Lists.newArrayList();
		this.itemOrderIndices = Lists.newArrayList();
	}

	public void replaceQueue(List<Track> queue) {
		replaceQueue(queue, getStartIndex(queue));
	}

	public void replaceQueue(List<Track> queue, int startIndex) {
		this.queue = Lists.newArrayList(queue);
		if(queue.isEmpty()) {
			currentTrack = null;
			currentTrackIndex = NO_INDEX;
			return;
		}
		currentTrack = queue.get(startIndex);
		createItemOrderIndices();
	}

	private int getStartIndex(List<Track> queue) {
		if(shuffle) {
			return random.nextInt(queue.size());
		}
		else {
			return 0;
		}
	}

	public boolean hasCurrentTrack() {
		return currentTrack != null;
	}

	public Track getCurrentTrack() {
		return currentTrack;
	}

	public boolean getShuffle() {
		return shuffle;
	}

	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
		createItemOrderIndices();
	}

	public void setRepeatAll(boolean repeatAll) {
		this.repeatAll = repeatAll;
	}
	
	public boolean getRepeatAll() {
		return repeatAll;
	}
	
	private void createItemOrderIndices() {
		List<Integer> newIndices = Lists.newArrayListWithCapacity(queue.size());
		for(int i = 0; i < queue.size(); ++i) {
			newIndices.add(i);
		}

		if(shuffle && queue.size() > 1) {
			Collections.swap(newIndices, 0, queue.indexOf(currentTrack));
			Collections.shuffle(newIndices.subList(1, newIndices.size()), random);
		}

		itemOrderIndices = newIndices;
		currentTrackIndex = getPlaybackIndexForTrack(currentTrack);
	}

	private int getPlaybackIndexForTrack(Track track) {
		for(int i = 0; i < queue.size(); ++i) {
			Track aTrack = getTrackForPlaybackIndex(i);
			if(aTrack == track) {
				return i;
			}
		}
		return NO_INDEX;
	}

	private Track getTrackForPlaybackIndex(int index) {
		if(index == NO_INDEX) {
			return null;
		}
		return queue.get(itemOrderIndices.get(index));
	}

	public void clear() {
		queue = Lists.newArrayList();
		currentTrack = null;
		createItemOrderIndices();
	}

	public boolean hasNextItem() {
		return queue.isEmpty() == false && currentTrackIndex + 1 < queue.size();
	}

	public boolean hasPreviousItem() {
		return queue.isEmpty() == false && currentTrackIndex > 0;
	}

	public void skipToFirstItem()
	{
		currentTrackIndex = 0;
		updateCurrentTrack();
	}
	
	public void skipToLastItem()
	{
		currentTrackIndex = queue.size();
		updateCurrentTrack();
	}
	
	public void skipToNextItem() {
		checkState(hasNextItem());
		++currentTrackIndex;
		updateCurrentTrack();
	}

	public void skipToPreviousItem() {
		checkState(hasPreviousItem());
		--currentTrackIndex;
		updateCurrentTrack();
	}

	private void updateCurrentTrack() {
		currentTrack = getTrackForPlaybackIndex(currentTrackIndex);
	}

}
