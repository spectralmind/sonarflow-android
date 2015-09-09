package com.spectralmind.sf4android.Tasks;

import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.SD.SDMethods;
import com.spectralmind.sf4android.SD.SDMethods.SDSearchResultRequest;
import com.spectralmind.sf4android.SD.SDResultPackage;
import com.spectralmind.sf4android.SD.SDResultPackage.SDResult;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.media.Album;
import com.spectralmind.sf4android.media.Artist;
import com.spectralmind.sf4android.media.MediaItem;
import com.spectralmind.sf4android.media.Track;


/** This class retrieves artwork from remote source
 * The constructor is initialized with a Bubble. Supported Bubble.mediaItem's are:
 * - Track
 * - Album
 * - Artist
 * @author ewald
 *
 */
public class GetCoverartTaskSD 
extends GetCoverartTask 
implements SDSearchResultRequest
{

	private static final Logger LOGGER = LoggerFactory.getLogger(GetCoverartTaskSD.class);
	private SDMethods sdMethod;

	public GetCoverartTaskSD(Bubble b) {
		super(b);

		sdMethod = new SDMethods();
	}

	protected Drawable doInBackground(Void... params) 
	{
		MediaItem mi = getmBubble().getMediaItem();

		// ImageHolder is a common supertype of last.fm's Artist and Album
		de.umass.lastfm.ImageHolder a = null;

		try {
			if (mi instanceof Track) {
				// nothing do do, as track's cover should have been set by album bubble
			} else if (mi instanceof Album) {
				if (mi.getNumTracks() > 0) {
					sdMethod.cancel();
					LOGGER.debug("Loading album " + getmBubble().getLabel());
					sdMethod.getReleaseFromTextSearch(mi.getTracks().get(0).getArtistName() + " " + ((Album) mi).getName(), this);
				} else {
					GetCoverartTaskSD.LOGGER.warn("MediaItem (Album) has no tracks: " + mi.getName());
				}
			} else if (mi instanceof Artist) {
				sdMethod.cancel();
				LOGGER.debug("Loading artist " + getmBubble().getLabel());
				sdMethod.getArtistFromTextSearch(getmBubble().getLabel(), this);
			} else {
				GetCoverartTaskSD.LOGGER.warn("MediaItem is of time " + mi.getClass().toString() + ", expected either Track or Album");
			}
		} catch (Exception e) {
			GetCoverartTaskSD.LOGGER.warn("Error occured when getting info for " + mi.getName() + ". " + e.toString());
		}


		return null;
	}


	@Override
	public void searchResultReady(SDResultPackage resultpackage) {

		if(!resultpackage.getERROR()) {
			LOGGER.debug("Found " + resultpackage.getSDResults().size() + " results.");

			if (resultpackage.getSDResults().size() >= 1) {

				SDResult r = resultpackage.getSDResults().get(0);

				MediaItem mi = getmBubble().getMediaItem();

				if (mi instanceof Track) {
					// TODO
				} else if (mi instanceof Album) {
					LOGGER.debug("Using [0]: " + resultpackage.getSDResults().get(0).getReleaseUrl());

					if (r.getReleaseImageByteArray() != null && r.getReleaseImageByteArray().length > 0) {

						//get the Drawable
						Drawable cover = null;
						try {
							cover = drawableFromByteBuffer(r.getReleaseImageByteArray(), SonarflowApplication.getAppContext());
//							cover = drawableFromUrl(r.getReleaseImageUrl(), SonarflowApplication.getAppContext());
						} catch (IOException e) {
							LOGGER.error("Error when loading image for " + getmBubble().getLabel());
						}

						GetCoverartTaskSD.LOGGER.debug("A new cover art: " + r.getArtistImageUrl());

						getmBubble().setCover(cover);
					} else {
						LOGGER.error("Error when loading image for " + getmBubble().getLabel());
					}
				} else if (mi instanceof Artist) {
					LOGGER.debug("Using [0]: " + resultpackage.getSDResults().get(0).getArtistUrl());

					if (r.getArtistImageByteArray() != null && r.getArtistImageByteArray().length > 0) {

						//get the Drawable
						Drawable cover = null;
						try {
							cover = drawableFromByteBuffer(r.getArtistImageByteArray(), SonarflowApplication.getAppContext());
						} catch (IOException e) {
							LOGGER.error("Error when loading image for " + getmBubble().getLabel());
						}

						GetCoverartTaskSD.LOGGER.debug("A new cover art: " + r.getArtistImageUrl());

						getmBubble().setCover(cover);
					} else {
						LOGGER.error("Error when loading image for " + getmBubble().getLabel());
					}
				}

			} else {
				// not at least one result
				LOGGER.error("Not found: " + getmBubble().getLabel());
			}
		}
		else
		{
			if(resultpackage.getNOMATCH())
				LOGGER.error("Not found: " + getmBubble().getLabel());
			else {
				LOGGER.error("Error when loading image for " + getmBubble().getLabel());
			}
		}
	}


	@Override
	public void searchRequestStatus(String status,boolean error) {
     Toast.makeText(SonarflowApplication.getAppContext(),SonarflowApplication.getAppContext().getString(R.string.no_internet_connection),  Toast.LENGTH_SHORT).show();
		
	
		
	}


	protected void onPostExecute(Drawable cover)
	{
		// cover would be null
		// nothing
	}

	@Override
	public void switchAPIKey() {
		LOGGER.warn("Switching SD API key to " + sdMethod.switchAPIKey());
	}
	

}
