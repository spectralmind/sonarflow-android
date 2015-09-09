package com.spectralmind.sf4android.player;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.MainActivity;
import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.media.Track;
import com.spectralmind.sf4android.player.MusicPlayer.PlayerState;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class PlayerService extends Service {
	public enum Action {
		PlayTracks("PLAY_TRACKS"), Play("PLAY"), Pause("PAUSE"), TogglePlayback("TOGGLE_PAUSE"), SkipToPrevious("PREV"), SkipToNext(
				"NEXT"), GetPlayerInfo("GET_PLAYER_INFO"), ToggleShuffle("TOGGLE_SHUFFLE"),
				RepeatAll("REPEAT_ALL"), ShowNoti("SHOW_NOTI"), StopNoti("STOP_NOTI"), HideNoti("HIDE_NOTI");

		private final String identifier;

		private Action(String identifier) {
			this.identifier = PlayerService.class.getCanonicalName() + ".action." + identifier;
		}

		public String getIdentifier() {
			return identifier;
		}

		public static Action fromIdentifier(String identifier) {
			for(Action action : values()) {
				if(action.getIdentifier().equals(identifier)) {
					return action;
				}
			}

			throw new IllegalArgumentException("Unknown identifier");
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerService.class);
	private static final String INTENT_ACTION_PLAYER_INFO = PlayerService.class.getCanonicalName()
			+ ".action.PLAYER_INFO";
	private static final String INTENT_PLAYER_INFO_KEY = PlayerService.class.getCanonicalName() + ".PLAYER_INFO";
	private static final String INTENT_TRACKS_KEY = PlayerService.class.getCanonicalName() + ".TRACKS";
	private static final int PLAYER_NOTIFICATION_ID = 2000;
	
	
	private final String lastFM_key = SonarflowApplication.getAppContext().getString(R.string.lastfm_api_key);
	private final String lastFM_secret = SonarflowApplication.getAppContext().getString(R.string.lastfm_api_secret);
			
	private SharedPreferences preference;
	private Notification notification;
	private RemoteViews bigContentView;
	private RemoteViews smallContentView;
	private boolean activity_paused = false;
	private Track nowTrack;
	private boolean notiDontReact = false; //for better performance
	
	public static Intent createPlayIntent(Context context, List<Track> tracksToPlay) { 
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(Action.PlayTracks.getIdentifier());
		intent.putParcelableArrayListExtra(INTENT_TRACKS_KEY, Lists.newArrayList(tracksToPlay));
		return intent;
	}
	
	public static Intent createIntent(Action action, Context context) {
		Intent intent = new Intent(context, PlayerService.class);
		intent.setAction(action.getIdentifier());
		return intent;
	}

	public static IntentFilter createStateIntentFilter() {
		return new IntentFilter(INTENT_ACTION_PLAYER_INFO);
	}

	private static Intent createPlayerInfoIntent(PlayerInfo playerInfo) {
		Intent intent = new Intent(INTENT_ACTION_PLAYER_INFO);
		intent.putExtra(INTENT_PLAYER_INFO_KEY, playerInfo);
		return intent;
	}

	public static PlayerInfo getPlayerInfoFromIntent(Intent intent) {
		return intent.getParcelableExtra(INTENT_PLAYER_INFO_KEY);
	}

	private static Action getActionFromIntent(Intent intent) {
		return Action.fromIdentifier(intent.getAction());
	}

	private static List<Track> getTracksFromIntent(Intent intent) {
		return intent.getParcelableArrayListExtra(INTENT_TRACKS_KEY);
	}

	private final MusicPlayer musicPlayer;
	private OnAudioFocusChangeListener audioFocusListener;

	public PlayerService() {
		musicPlayer = new MusicPlayer();
		musicPlayer.addListener(new MusicPlayer.Listener() {
			@Override
			public void onCurrentTrackChanged() {
				updateScrobbling();
			}

			@Override
			public void onStateChanged() {
				sentStateBroadcast();
				if(musicPlayer.getState() != PlayerState.Preparing)
					updateNotification();
			}
		});
		audioFocusListener = new OnAudioFocusChangeListener() {
			@Override
			public void onAudioFocusChange(int focusChange) {
				handleAudioFocusChange(focusChange);
			}
		};
	}

	private void handleAudioFocusChange(int focusChange) {
		LOGGER.debug("Audio focus changed: {}", focusChange);
		switch(focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS:
				musicPlayer.pause();
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				musicPlayer.setVolume(MusicPlayer.MAX_VOLUME);
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				musicPlayer.pause();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				musicPlayer.setVolume(MusicPlayer.DUCK_VOLUME);
				break;
			default:
				LOGGER.warn("Unhandled audio focus change: {}", focusChange);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		preference = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		createSmallNotiView();
		createLargeNotiView();
		createNoti();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntentAction(getActionFromIntent(intent), intent);
		return Service.START_NOT_STICKY;
	}

	private void handleIntentAction(Action action, Intent intent) {
		LOGGER.debug("Received intent action: {}", action);
		if(requestAudioFocus() == false) {
			LOGGER.info("Could not get audio focus, stopping.");
			musicPlayer.stop();
			return;
		}
		switch(action) {
			case PlayTracks:
				musicPlayer.play(getTracksFromIntent(intent));
				break;
			case Play:
				musicPlayer.resumeFromPause();
				break;
			case Pause:
				musicPlayer.pause();
				break;
			case TogglePlayback:
				musicPlayer.togglePlayback();
				break;
			case SkipToPrevious:
				musicPlayer.skipToPreviousTrack();
				break;
			case SkipToNext:
				musicPlayer.skipToNextTrack();
				break;
			case GetPlayerInfo:
				sentStateBroadcast();
				break;
			case ToggleShuffle:
				musicPlayer.setShuffle(!musicPlayer.getShuffle());
				sentStateBroadcast();
				break;
			case RepeatAll:
				musicPlayer.setRepeatAll(!musicPlayer.getRepeatAll());
				sentStateBroadcast();
				break;
			case StopNoti:
				notiDontReact = true;
				if(musicPlayer.isActive())
					musicPlayer.pause();
				stopForeground(true);
//				((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
//				.cancelAll();
				break;
			case ShowNoti:
				activity_paused = true;
				if(musicPlayer.getState() != PlayerState.Paused && musicPlayer.getState() != PlayerState.Stopped)
					updateNotification();
				break;
			case HideNoti:
				activity_paused = false;
				stopForeground(true);
//				((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
//				.cancelAll();
				break;
			default:
				throw new IllegalArgumentException("Unknown action");
		}
	}

	@SuppressLint("NewApi")
	private void createNoti()
	{
		notification = new Notification();
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.icon = R.drawable.notification_icon;
		notification.contentIntent = MainActivity.createPendingIntent(this);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			notification.contentView = smallContentView;
		else
		{
			notification.contentView = smallContentView;
			notification.bigContentView = bigContentView;
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void createNoti(String title, String text)
	{
		notification = new Notification.Builder(getBaseContext())
		.setSmallIcon(R.drawable.notification_icon)
		.setContentIntent(MainActivity.createPendingIntent(this))
		.setContentTitle(title)
		.setContentText(text)
		.getNotification();
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			notification.contentView = smallContentView;
		else
		{
			notification.contentView = smallContentView;
			notification.bigContentView = bigContentView;
		}
	}

	private void createLargeNotiView()
	{
		bigContentView = new RemoteViews(getPackageName(),R.layout.notification_view_large);

		bigContentView.setOnClickPendingIntent(R.id.exit, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.StopNoti, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.noti_prev_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.SkipToPrevious, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.noti_next_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.SkipToNext, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.exit, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.StopNoti, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.noti_prev_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.SkipToPrevious, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.noti_next_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.SkipToNext, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.large_noti_play_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.Play, getBaseContext()),0));
		bigContentView.setOnClickPendingIntent(R.id.large_noti_pause_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.Pause, getBaseContext()),0));
	}
	
	private void createSmallNotiView()
	{		
		smallContentView = new RemoteViews(getPackageName(),R.layout.notification_view_small);

		smallContentView.setOnClickPendingIntent(R.id.exit, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.StopNoti, getBaseContext()),0));
//		contentView.setOnClickPendingIntent(R.id.noti_prev_button, 
//				PendingIntent.getService(getBaseContext(), 0, 
//						PlayerService.createIntent(Action.SkipToPrevious, getBaseContext()),0));
		smallContentView.setOnClickPendingIntent(R.id.noti_next_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.SkipToNext, getBaseContext()),0));
		smallContentView.setOnClickPendingIntent(R.id.exit, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.StopNoti, getBaseContext()),0));
//		contentView.setOnClickPendingIntent(R.id.noti_prev_button, 
//				PendingIntent.getService(getBaseContext(), 0, 
//						PlayerService.createIntent(Action.SkipToPrevious, getBaseContext()),0));
		smallContentView.setOnClickPendingIntent(R.id.noti_next_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.SkipToNext, getBaseContext()),0));
		smallContentView.setOnClickPendingIntent(R.id.small_noti_pause_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.Pause, getBaseContext()),0));
		smallContentView.setOnClickPendingIntent(R.id.small_noti_play_button, 
				PendingIntent.getService(getBaseContext(), 0, 
						PlayerService.createIntent(Action.Play, getBaseContext()),0));
	}
	
	private void showNotification()
	{
		this.startForeground(1, notification);
	}
	
	@SuppressLint("NewApi")
	private void updateNotification()
	{		
		if(!activity_paused || notiDontReact){
			notiDontReact = false;
			return;
		}

		Track currentTrack = musicPlayer.getCurrentTrack();
		
//		if (currentTrack != null) {
//			createNoti();
////			createNoti(currentTrack.getName(), currentTrack.getArtistName()); //always create a new Notification for better performance
//		}
//		
		
		if (currentTrack != null && musicPlayer.getState() != PlayerState.Paused) {
			
			if(currentTrack != nowTrack)
			{
				smallContentView.setTextViewText(R.id.track, currentTrack.getName());
				smallContentView.setTextViewText(R.id.album, currentTrack.getAlbumName());
				smallContentView.setTextViewText(R.id.artist, currentTrack.getArtistName());
				bigContentView.setTextViewText(R.id.track, currentTrack.getName());
				bigContentView.setTextViewText(R.id.album, currentTrack.getAlbumName());
				bigContentView.setTextViewText(R.id.artist, currentTrack.getArtistName());
				MediaMetadataRetriever mmr = new MediaMetadataRetriever();

				boolean hasCover = false;
				try{
					mmr.setDataSource(currentTrack.getPath());
					
					byte[] buff = mmr.getEmbeddedPicture();
					if(buff != null)
						hasCover = true;
					if(hasCover)
					{
						Bitmap cover =  BitmapFactory.decodeByteArray(buff, 0, buff.length);
						smallContentView.setImageViewBitmap(R.id.small_cover, cover);
						bigContentView.setImageViewBitmap(R.id.large_cover, cover);
					}
					else
					{
						smallContentView.setImageViewResource(R.id.small_cover, R.drawable.icon);
						bigContentView.setImageViewResource(R.id.large_cover, R.drawable.icon);
					}
					showNotification();
				} catch(Exception e) { 
					LOGGER.warn("Could not retrieve data source: {}", currentTrack.getPath());
				}
				nowTrack = currentTrack;
			}
			
			smallContentView.setViewVisibility(R.id.small_noti_play_button, View.INVISIBLE);
			smallContentView.setViewVisibility(R.id.small_noti_pause_button, View.VISIBLE);
			bigContentView.setViewVisibility(R.id.large_noti_play_button, View.INVISIBLE);
			bigContentView.setViewVisibility(R.id.large_noti_pause_button, View.VISIBLE);
			showNotification();
		} else {
			// no current track

			if(notification == null)
				return;
			
//			smallContentView.setImageViewResource(R.id.noti_play_button, R.drawable.av_play);
//			bigContentView.setImageViewResource(R.id.noti_play_button, R.drawable.av_play);
//			smallContentView.setOnClickPendingIntent(R.id.noti_play_button, 
//					PendingIntent.getService(getBaseContext(), 0, 
//							PlayerService.createIntent(Action.Play, getBaseContext()),0));
//			bigContentView.setOnClickPendingIntent(R.id.noti_play_button, 
//					PendingIntent.getService(getBaseContext(), 0, 
//							PlayerService.createIntent(Action.Play, getBaseContext()),0));
	
			bigContentView.setViewVisibility(R.id.large_noti_play_button, View.VISIBLE);
			bigContentView.setViewVisibility(R.id.large_noti_pause_button, View.INVISIBLE);
			smallContentView.setViewVisibility(R.id.small_noti_play_button, View.VISIBLE);
			smallContentView.setViewVisibility(R.id.small_noti_pause_button, View.INVISIBLE);
			showNotification();
		}

	
	}
	
	private boolean requestAudioFocus() {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}

	private void updateScrobbling() {
		if(musicPlayer.isActive() == false) {
			return;
		}
		if(!preference.getBoolean(getString(R.string.pref_lastfm_scrobbling), false))
			return;

		checkState(musicPlayer.getCurrentTrack() != null);
			
		Thread t = new Thread(new Runnable(){
			Track track = musicPlayer.getCurrentTrack();
			
			public void run() {	
				Session session = null;
				try {
					session = Authenticator.getMobileSession(
							preference.getString(getString(R.string.pref_lastfm_username), ""),
							preference.getString(getString(R.string.pref_lastfm_pwd), ""), 
							lastFM_key, lastFM_secret);
					
				} catch (de.umass.lastfm.CallException ex) {
					LOGGER.warn("de.umass.lastfm.CallException: {}",  ex.getMessage());
				}
				if(session == null)
				{
					//					LOGGER.debug("Could not log in");
					Looper.prepare();
					Toast.makeText(getBaseContext(),SonarflowApplication.getAppContext().getString(R.string.could_not_log_in), Toast.LENGTH_SHORT).show();
					Looper.loop();
					return;
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e1) {
					LOGGER.debug("Could not sleep");
				}

				if(track == musicPlayer.getCurrentTrack() && musicPlayer.isPlaying())
				{
					int now = (int)(System.currentTimeMillis()/1000);
					try{
						ScrobbleResult result = de.umass.lastfm.Track.scrobble(
								track.getArtistName(), track.getName(), now, session);
						if(result.isSuccessful() && !result.isIgnored())
						{
							Looper.prepare();
							Toast.makeText(getBaseContext(),SonarflowApplication.getAppContext().getString(R.string.current_song_scrobbled)  , Toast.LENGTH_SHORT).show();
							Looper.loop();
						}}
					catch(Exception e){
						Looper.prepare();
						LOGGER.error(e.getMessage());
						Toast.makeText(getBaseContext(), SonarflowApplication.getAppContext().getString(R.string.problem_with_scrobbling) , Toast.LENGTH_SHORT).show();
						Looper.loop();
					}
				}
			}
		});
		t.start();
	}

	private void sentStateBroadcast() {
		PlayerInfo playerInfo = new PlayerInfo(musicPlayer.getState(), musicPlayer.getShuffle(),
				musicPlayer.getRepeatAll());
		sendBroadcast(createPlayerInfoIntent(playerInfo));
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		musicPlayer.stop();
		super.onDestroy();
	}
}
