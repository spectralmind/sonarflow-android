package com.spectralmind.sf4android.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.player.PlayerService.Action;

public class PlayerToolbarFragment extends Fragment {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerToolbarFragment.class);

	private final BroadcastReceiver playerStateReceiver;

	private PlayerInfo lastPlayerInfo;

	public PlayerToolbarFragment() {
		playerStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				
				updateToolbarFromPlayerInfo(PlayerService.getPlayerInfoFromIntent(intent));
			}
		};
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		registerPlayeStateReceiver();
		requestPlayerStateUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterPlayerStateReceiver();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.playback_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		PlayerService.Action action = getActionForItem(item);
		if(action != null) {
			sendPlayerAction(action);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private Action getActionForItem(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_play:
				return PlayerService.Action.Play;
			case R.id.menu_pause:
				return PlayerService.Action.Pause;
//			case R.id.menu_previous:
//				return PlayerService.Action.SkipToPrevious;
			case R.id.menu_next:
				return PlayerService.Action.SkipToNext;
			case R.id.menu_shuffle:
				return PlayerService.Action.ToggleShuffle;
			case R.id.menu_repeatAll:
				return PlayerService.Action.RepeatAll;
			default:
				return null;
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem playItem = menu.findItem(R.id.menu_play);
		MenuItem pauseItem = menu.findItem(R.id.menu_pause);
		MenuItem shuffleItem = menu.findItem(R.id.menu_shuffle);
		MenuItem repeatAllItem = menu.findItem(R.id.menu_repeatAll);
		
		if(lastPlayerInfo == null) {
			pauseItem.setVisible(false);
		}
		else {
			pauseItem.setVisible(lastPlayerInfo.getState().canPause());
			shuffleItem.setChecked(lastPlayerInfo.getShuffle());
			repeatAllItem.setChecked(lastPlayerInfo.getRepeatAll());
		}

		playItem.setVisible(!pauseItem.isVisible());
	}

	private void registerPlayeStateReceiver() {
		getActivity().registerReceiver(playerStateReceiver, PlayerService.createStateIntentFilter());
	}

	private void unregisterPlayerStateReceiver() {
		getActivity().unregisterReceiver(playerStateReceiver);
	}

	private void requestPlayerStateUpdate() {
		sendPlayerAction(PlayerService.Action.GetPlayerInfo);
	}

	protected void updateToolbarFromPlayerInfo(PlayerInfo playerInfo) {
		LOGGER.debug("Updating toolbar from player info: {}", playerInfo);
		lastPlayerInfo = playerInfo;
		getActivity().invalidateOptionsMenu();
	}

	private void sendPlayerAction(PlayerService.Action action) {
		LOGGER.debug("Sending player action: {}", action);
		getActivity().startService(PlayerService.createIntent(action, getActivity()));
	}
}
