package com.spectralmind.sf4android.player;

import android.os.Parcel;
import android.os.Parcelable;

import com.spectralmind.sf4android.player.MusicPlayer.PlayerState;

public class PlayerInfo implements Parcelable {
	private PlayerState state;
	private boolean shuffle;
	private boolean repeatAll;

	PlayerInfo(PlayerState state, boolean shuffle, boolean repeatAll) {
		this.state = state;
		this.shuffle = shuffle;
		this.repeatAll = repeatAll;
	}

	public PlayerState getState() {
		return state;
	}

	public boolean getShuffle() {
		return shuffle;
	}

	public boolean getRepeatAll()
	{
		return repeatAll;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(state.name());
		dest.writeInt(shuffle ? 1 : 0);
		dest.writeInt(repeatAll ? 1: 0);
	}

	public static PlayerInfo fromParcel(Parcel parcel) {
		return new PlayerInfo(PlayerState.valueOf(parcel.readString()), parcel.readInt() > 0,
				parcel.readInt() > 0);
	}

	public static final Parcelable.Creator<PlayerInfo> CREATOR = new Parcelable.Creator<PlayerInfo>() {
		@Override
		public PlayerInfo createFromParcel(Parcel in) {
			return PlayerInfo.fromParcel(in);
		}

		@Override
		public PlayerInfo[] newArray(int size) {
			return new PlayerInfo[size];
		}
	};
}
