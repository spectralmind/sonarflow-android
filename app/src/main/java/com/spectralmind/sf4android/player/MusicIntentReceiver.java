package com.spectralmind.sf4android.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MusicIntentReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context ctx, Intent intent) {
		if(intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			ctx.startService(PlayerService.createIntent(PlayerService.Action.Pause, ctx));
		}
	}
}
