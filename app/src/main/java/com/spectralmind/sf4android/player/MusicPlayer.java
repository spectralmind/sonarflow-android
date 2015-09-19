package com.spectralmind.sf4android.player;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.media.Track;

public class MusicPlayer {
	public interface Listener {
		void onCurrentTrackChanged();

		void onStateChanged();
	}

	public enum PlayerState {
		Stopped(false), Preparing(true), Playing(true), Paused(false);

		private final boolean canPause;

		private PlayerState(boolean canPause) {
			this.canPause = canPause;
		}

		boolean canPause() {
			return canPause;
		}
	}

	public static final float MAX_VOLUME = 1.0f;
	public static final float DUCK_VOLUME = 0.1f;

	private static final Logger LOGGER = LoggerFactory.getLogger(MusicPlayer.class);

	private static final int RESTART_ON_SKIP_AFTER_MS = 3000;

	private final PlaybackQueue playbackQueue;
	private final OnPreparedListener onPreparedListener;
	private final OnCompletionListener onCompletionListener;
	private final OnErrorListener onErrorListener;
	private final List<Listener> listeners;
	private PlayerState state;
	private MediaPlayer mediaPlayer;

	public MusicPlayer() {
		this.playbackQueue = new PlaybackQueue();
		this.listeners = Lists.newArrayList();
		this.onPreparedListener = new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				checkState(mp == mediaPlayer);
				startPlaybackAfterPreparing();
			}
		};
		onCompletionListener = new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				checkState(mp == mediaPlayer);
				skipToNextTrack();
			}
		};
		onErrorListener = new OnErrorListener()
		{
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				return false;
			}
			
		};
		state = PlayerState.Stopped;
	}

	public void play(List<Track> tracks) {
		LOGGER.debug("Playing tracks: {}", tracks);
		playbackQueue.replaceQueue(tracks);
		setVolume(MAX_VOLUME);
		playCurrentTrack();
	}

	public void togglePlayback() {
		if(getState() == PlayerState.Paused) {
			resumeFromPause();
		}
		else {
			pause();
		}
	}
	
	public void resumeFromPause() {
		if(getState() != PlayerState.Paused) {
			return;
		}

		checkState(mediaPlayer != null);
		mediaPlayer.start(); // !!!: Possible race with preparing
		setState(PlayerState.Playing);
	}

	public void pause() {
		if(getState().canPause() == false) {
			return;
		}

		if(getState() == PlayerState.Playing) {
			mediaPlayer.pause();
		}
		setState(PlayerState.Paused);
	}

	private void playCurrentTrack() {
		try {
			tryPlayCurrentTrack();
			notifyListenersOfCurrentTrack();
		}
		catch(IOException e) {
			LOGGER.error("Could not play track: {}", playbackQueue.getCurrentTrack());
		}
	}

	private void tryPlayCurrentTrack() throws IOException {
		releasePlayer();
		if(playbackQueue.hasCurrentTrack() == false) {
			return;
		}

		try {
			createPlayer();
			setState(PlayerState.Preparing);
			mediaPlayer.setDataSource(playbackQueue.getCurrentTrack().getPath());
			mediaPlayer.prepareAsync();
		} catch (Exception ex) {
			throw new IOException();
		}
	}

	private void createPlayer() {
		if(mediaPlayer != null) {
			return;
		}

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnPreparedListener(onPreparedListener);
		mediaPlayer.setOnCompletionListener(onCompletionListener);
		mediaPlayer.setOnErrorListener(onErrorListener);
	}

	private void startPlaybackAfterPreparing() {
		if(getState() != PlayerState.Preparing) {
			return;
		}

		setState(PlayerState.Playing);
		mediaPlayer.start();
	}

	public void skipToNextTrack() {
		if(playbackQueue.hasNextItem() == false) {
			//stop();
			//return;
			if(playbackQueue.getRepeatAll())
				playbackQueue.skipToFirstItem();
			else
				return;
		}
		else
			playbackQueue.skipToNextItem();
		
		playCurrentTrack();
	}

	public void skipToPreviousTrack() {
		if(shouldRestartTrackOnSkip()) {
			seekToTrackPosition(0);
			return;
		}

		if(playbackQueue.hasPreviousItem() == false) {
			//stop();
			//return;
			if(playbackQueue.getRepeatAll())
				playbackQueue.skipToFirstItem();
			else
				return;
		}
		else
			playbackQueue.skipToPreviousItem();
		
		playCurrentTrack();
	}

	public void stop() {
		releasePlayer();
		playbackQueue.clear();
		notifyListenersOfCurrentTrack();
	}

	private boolean shouldRestartTrackOnSkip() {
		return getCurrentPostion() > RESTART_ON_SKIP_AFTER_MS;
	}

	private void seekToTrackPosition(int msTrackPosition) {
		checkState(mediaPlayer != null);
		mediaPlayer.seekTo(msTrackPosition);
	}

	private void releasePlayer() {
		if(mediaPlayer == null) {
			return;
		}

		setState(PlayerState.Stopped);
		mediaPlayer.release();
		mediaPlayer = null;
	}

	public int getSongDuration() {
		return mediaPlayer.getDuration();
	}

	public int getCurrentPostion() {
		if(isMediaPlayerActive() == false) {
			return 0;
		}

		return mediaPlayer.getCurrentPosition();
	}

	private boolean isMediaPlayerActive() {
		return getState().canPause();
	}
	
	public boolean isPlaying()
	{
		return mediaPlayer.isPlaying();
	}
	
	public boolean isPreparing()
	{
		return getState() == PlayerState.Preparing;
	}
	

	public boolean isActive() {
		return getState() != PlayerState.Stopped;
	}

	public Track getCurrentTrack() {
		return playbackQueue.getCurrentTrack();
	}

	public void setVolume(float volume) {
		if(mediaPlayer == null) {
			return;
		}

		mediaPlayer.setVolume(volume, volume);
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public PlayerState getState() {
		return state;
	}

	public void setState(PlayerState playerState) {
		this.state = playerState;
		notifyListenersOfState();
	}
	
	public boolean getShuffle() {
		return playbackQueue.getShuffle();
	}
	
	public void setShuffle(boolean shuffle) {
		playbackQueue.setShuffle(shuffle);
	}

	public void setRepeatAll(boolean repeatAll) {
		playbackQueue.setRepeatAll(repeatAll);
	}
	
	public boolean getRepeatAll() {
		return playbackQueue.getRepeatAll();
	}
	
	private void notifyListenersOfCurrentTrack() {
		for(Listener listener : listeners) {
			listener.onCurrentTrackChanged();
		}
	}

	private void notifyListenersOfState() {
		for(Listener listener : listeners) {
			listener.onStateChanged();
		}
	}
}
