package com.spectralmind.sf4android.media;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class Track extends MediaItem implements Parcelable {
	private final String artistName;
	private final String albumName;
	private final int trackNumber;
	private final int duration;
	private final String path;
	
	public Track(String name, String artistName, String albumName, int trackNumber, int duration, String path) {
		super(name);
		this.artistName = artistName;
		this.albumName = albumName;
		this.trackNumber = trackNumber;
		this.duration = duration;
		this.path = path;
	}
	
	public String getArtistName() {
		return artistName;
	}

	public String getAlbumName() {
		return albumName;
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public String getPath() {
		return path;
	}

	@Override
	public List<Track> getTracks() {
		return Lists.newArrayList(this);
	}

	@Override
	public boolean equals(Object o) {
		if(super.equals(o) == false) {
			return false;
		}

		Track other = (Track) o;
		return Objects.equal(path, other.path);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), path);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", getName()).add("path", path).toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getName());
		dest.writeString(artistName);
		dest.writeString(albumName);
		dest.writeInt(trackNumber);
		dest.writeInt(duration);
		dest.writeString(path);
	}

	private static Track fromParcel(Parcel parcel) {
		Track track = new Track(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(),
				parcel.readInt(), parcel.readString());

		return track;
	}

	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
		@Override
		public Track createFromParcel(Parcel in) {
			return Track.fromParcel(in);
		}

		@Override
		public Track[] newArray(int size) {
			return new Track[size];
		}
	};

	public String getDescription() {
		return String.format("%s (%s)", getName(), getContextInfo());
	}

	public String getContextInfo() {
		return String.format("%s - %s", getArtistName(), getAlbumName());
	}
	
	public int getDuration() {
		return duration;
	}
}
