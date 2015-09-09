package com.spectralmind.sf4android.SD;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

public class SDResultPackage {
	
	private 
	boolean ERROR;
	boolean NOMATCH;
	String errorMessage;
	String errorCode;
	ArrayList<SDResult> SDResults;
	
	public SDResultPackage(){
		ERROR = false;
		NOMATCH = false;
		errorMessage = null;
		errorCode = null;
		SDResults = new ArrayList<SDResult>();
	}

	public void addSDResultItem(SDResult item)
	{
		SDResults.add(item);
	}
	
	public void setERROR(boolean value){
		ERROR = value;}
	
	public void setNOMATCH(boolean value){
		NOMATCH = value;}
	
	public void setErrorMessage(String value){
		errorMessage = value;}
	
	public void setErrorCode(String value){
		errorCode = value;}
	
	public boolean getERROR()
	{
		return ERROR;
	}
	
	public boolean getNOMATCH()
	{
		return NOMATCH;
	}
	public String getErrorMessage()
	{
		return errorMessage;
	}
	public String getErrorCode()
	{
		return errorCode;
	}
	
	public ArrayList<SDResult> getSDResults() {
		return SDResults;
	}

	public class SDResult{
		private
		String artist;
		String artistID;
		String artistImageUrl;
		String artistUrl;
		String popularity;
		String release;
		String releaseID;
		String releaseImageUrl;
		String releaseUrl;
		String releaseType;
		String releaseYear;
		String releaseDate;
		String releaseAppearsAs;
		String recordProducer;
		String track;
		String trackNumber;
		String trackID;
		String trackStreamingReleaseDate;
		String trackUrl;
		String trackDuration;
		String trackPreviewUrl;

		byte[] artistImageData;
		byte[] releaseImageData;
		
		public String getTrackDuration() {
			return trackDuration;
		}

		public String getTrackPreviewUrl() {
			return trackPreviewUrl;
		}

		public void setTrackPreviewUrl(String trackPreviewUrl) {
			this.trackPreviewUrl = trackPreviewUrl;
		}

		public void setTrackDuration(String trackDuration) {
			this.trackDuration = trackDuration;
		}
		
		public String getTrack() {
			return track;
		}

		void setTrack(String track) {
			this.track = track;
		}

		public String getTrackNumber() {
			return trackNumber;
		}

		void setTrackNumber(String trackNumber) {
			this.trackNumber = trackNumber;
		}

		public String getTrackID() {
			return trackID;
		}

		void setTrackID(String trackID) {
			this.trackID = trackID;
		}

		public String getTrackStreamingReleaseDate() {
			return trackStreamingReleaseDate;
		}

		void setTrackStreamingReleaseDate(String trackStreamingReleaseDate) {
			this.trackStreamingReleaseDate = trackStreamingReleaseDate;
		}

		public String getTrackUrl() {
			return trackUrl;
		}

		public void setTrackUrl(String trackUrl) {
			this.trackUrl = trackUrl;
		}
		
		public String getReleaseDate() {
			return releaseDate;
		}

		public String getReleaseAppearsAs() {
			return releaseAppearsAs;
		}

		public void setReleaseAppearsAs(String releaseAppearsAs) {
			this.releaseAppearsAs = releaseAppearsAs;
		}

		public String getRecordProducer() {
			return recordProducer;
		}

		public void setRecordProducer(String recordProducer) {
			this.recordProducer = recordProducer;
		}

		public void setReleaseDate(String releaseDate) {
			this.releaseDate = releaseDate;
		}
		
		public String getReleaseImageUrl() {
			return releaseImageUrl;
		}

		public void setReleaseImageUrl(String releaseImageUrl) {
			this.releaseImageUrl = releaseImageUrl;
		}

		public String getReleaseYear() {
			return releaseYear;
		}

		public void setReleaseYear(String releaseYear) {
			this.releaseYear = releaseYear;
		}
		
		public String getRelease() {
			return release;
		}

		public void setRelease(String release) {
			this.release = release;
		}

		public String getReleaseID() {
			return releaseID;
		}

		public void setReleaseID(String releaseID) {
			this.releaseID = releaseID;
		}

		public String getReleaseUrl() {
			return releaseUrl;
		}

		public void setReleaseUrl(String releaseURL) {
			this.releaseUrl = releaseURL;
		}

		public String getReleaseType() {
			return releaseType;
		}

		public void setReleaseType(String releaseType) {
			this.releaseType = releaseType;
		}

		public byte[] getArtistImageByteArray()
		{
			return artistImageData;
		}
		
		public byte[] getReleaseImageByteArray()
		{
			return releaseImageData;
		}
		
		public String getArtist() {
			return artist;
		}
		
		public void setArtist(String art) {
			artist = art;
		}
		
		public String getArtistID() {
			return artistID;
		}
		
		public void setArtistID(String artistID) {
			this.artistID = artistID;
		}
		
		public String getArtistImageUrl() {
			return artistImageUrl;
		}
		
		public void setArtistImageUrl(String imageUrl) {
			this.artistImageUrl = imageUrl;
		}
		
		public String getArtistUrl() {
			return artistUrl;
		}
		
		public void setArtistUrl(String sDUrl ) {
			artistUrl = sDUrl;
		}
		
		public String getPopularity() {
			return popularity;
		}
		
		public void setPopularity(String popularity) {
			this.popularity = popularity;
		}
		
		public void writeArtistImageByteFromURL(String url) throws IOException
		{
			InputStream is = (InputStream) new URL(url).getContent();
			artistImageData = IOUtils.toByteArray(is);
		}
		
		public void writeReleaseImageByteFromURL(String url) throws IOException
		{
			InputStream is = (InputStream) new URL(url).getContent();
			releaseImageData = IOUtils.toByteArray(is);
		}

	}

}
