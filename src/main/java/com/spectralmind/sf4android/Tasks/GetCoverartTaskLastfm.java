package com.spectralmind.sf4android.Tasks;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;

import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.media.Album;
import com.spectralmind.sf4android.media.Artist;
import com.spectralmind.sf4android.media.MediaItem;
import com.spectralmind.sf4android.media.Track;

import de.umass.lastfm.ImageSize;


/** This class retrieves artwork from remote source
 * The constructor is initialized with a Bubble. Supported Bubble.mediaItem's are:
 * - Track
 * - Album
 * - Artist
 * @author ewald
 *
 */
public class GetCoverartTaskLastfm
extends
GetCoverartTask
{

	private static final Logger LOGGER = LoggerFactory.getLogger(GetCoverartTaskLastfm.class);


	public GetCoverartTaskLastfm(Bubble b) {
		super(b);
	}

	
	protected Drawable doInBackground(Void... params) 
	{
		
		String key = "fce4ee314339e5192fe28938e4795b9b";
		MediaItem mi = getmBubble().getMediaItem();
		String url;
		// ImageHolder is a common supertype of last.fm's Artist and Album
		de.umass.lastfm.ImageHolder a = null;

		try {
			if (mi instanceof Track) {
				a = de.umass.lastfm.Album.getInfo(((Track) mi).getArtistName(), ((Track) mi).getAlbumName(), key);
			} else if (mi instanceof Album) {
				if (mi.getNumTracks() > 0) {
					a = de.umass.lastfm.Album.getInfo(mi.getTracks().get(0).getArtistName(), ((Album) mi).getName(), key);
				} else {
					GetCoverartTaskLastfm.LOGGER.warn("MediaItem (Album) has no tracks: " + mi.getName());
				}
			} else if (mi instanceof Artist) {
				a = de.umass.lastfm.Artist.getInfo(mi.getName(), key);
			} else {
				GetCoverartTaskLastfm.LOGGER.warn("MediaItem is of time " + mi.getClass().toString() + ", expected either Track or Album");
			}
		} catch (Exception e) {
			GetCoverartTaskLastfm.LOGGER.warn("Error occured when getting info for " + mi.getName() + ". " + e.toString());
		}

		// if we did not get an album, back off
		if (a==null) return null;

		// extract url with desired size
		url = a.getImageURL(ImageSize.EXTRALARGE);
		if (url == null) url = a.getImageURL(ImageSize.LARGE);
		if (url == null) url = a.getImageURL(ImageSize.MEDIUM);

//		 get the Drawable
		Drawable cover = null;
		try {
			cover = GetCoverartTaskLastfm.drawableFromUrl(url, SonarflowApplication.getAppContext());
			GetCoverartTaskLastfm.LOGGER.debug("A new cover art: " + url);
		} catch (IOException e) {
			GetCoverartTaskLastfm.LOGGER.warn("Could not load cover art: " + url);
		}

		return cover;
	}
	
	protected void onPostExecute(Drawable cover)
	{
//		if(cover == null)
//		{
//			mBubble.setCover(cover);
//			return;
//		}
		getmBubble().setCover(cover);
//		try {
//			mBubble.setCover(cover);
//			GetCoverartTask.LOGGER.debug("A new cover art: " + url);
//		} catch (IOException e) {
//			GetCoverartTask.LOGGER.warn("Could not load cover art: " + url);
//		}
	}

}
