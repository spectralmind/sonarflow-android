package com.spectralmind.sf4android.media;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.MainActivity;
import com.spectralmind.sf4android.MainActivity.BubbleLoader;
import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.Tasks.ArtistTagAsync;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.bubble.BubbleLayouter;

public class MoodLoader extends AbstractLoader {


	private static final Logger LOGGER = LoggerFactory.getLogger(MoodLoader.class);

	public  Map<String, List<String>> moodContainer = new HashMap<String, List<String>>();
	public  Map<String, Integer> moodCount = new HashMap<String, Integer>();
	public  Map<String, List<Track>> artistContainer = new HashMap<String, List<Track>>();
	public  List<Mood> moodCollection = Lists.newArrayList();

	public MoodLoader(String defsFile, String attrDefsFile, BubbleLayouter layouter) {
		super(defsFile, attrDefsFile, layouter);
	}	


	@Override
	public void load(final MainActivity mMainActivity, BubbleLoader delegate) {
		super.load(mMainActivity, delegate);

		// prepare progressbar
		if (mMainActivity.getProgressDlg() == null) {
			mMainActivity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					ProgressDialog bar = new ProgressDialog(mMainActivity);
					bar.setMessage(mMainActivity.getString(com.spectralmind.sf4android.R.string.creating_moodclusters));
					bar.setIndeterminate(true);
					bar.setProgress(0);
					bar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				//	bar.setCancelable(false);
//					bar.setCanceledOnTouchOutside(false);
//					bar.show();
					mMainActivity.setProgressDlg(bar);
				}
			});
		}

		LOGGER.debug("Getting all artists and tracks...");
		loadTracksAndArtist(mMainActivity);

		final int numArtists = artistContainer.size();

		mMainActivity.getProgressDlg().setIndeterminate(false);
		mMainActivity.getProgressDlg().setMax(numArtists);

		int counter = 0;
		for(Entry<String, List<Track>> entry : artistContainer.entrySet()){
			counter++;
			new ArtistTagAsync(mMainActivity, this, entry.getKey(), counter, numArtists, delegate).execute();
		}		

	}

	private void sortMood() {
		if(moodCollection.size() == 0)
			return;
		LOGGER.debug("Sorting mood objects...");
		int buff_max;
		int merken;
		for(int j = 0;j < moodCollection.size();j++)
		{
			Mood mood = moodCollection.get(j);
			buff_max = moodCollection.get(j).getArtistCount();
			merken = j;
			for(int k = j; k < moodCollection.size(); k++)
			{
				if(moodCollection.get(k).getArtistCount() >= buff_max)
				{
					mood = moodCollection.get(k);
					buff_max = moodCollection.get(k).getArtistCount();
					merken = k;
				}
			}
			moodCollection.set(merken, moodCollection.get(j));
			moodCollection.set(j, mood);
		}
	}

	public List<Bubble> createMoods(Context context) {
		LOGGER.debug("Creating mood objects...");
		for (Map.Entry<String, List<String>> entry : moodContainer.entrySet())
		{
			String moodName = entry.getKey();
			List<String> artistsInMood = entry.getValue();
			Mood mood = new Mood(artistsInMood.size(), moodName);
			for(String artist : artistsInMood){
				mood.addTracks(artistContainer.get(artist));
			}
			moodCollection.add(mood);
		}
		sortMood();
		Log.v("MyLog","collection moods: "+ moodCollection);

		return layouter.createBubbles(moodCollection, attrDefs);			
	}
	
	/** Changes global variable artistContainer */
	private void loadTracksAndArtist(Context context) {
		String trackColumns[] = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.DURATION };
		Cursor trackCursor = null;
		
		try {
			trackCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					trackColumns, null, null, null);
			
			if(trackCursor.moveToFirst() == false) {
				LOGGER.warn("Track cursor returned zero results");
				return;
			}
			
			do {
				String artist = trackCursor.getString(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				
				if(!artistContainer.containsKey(artist)){
					List<Track> tracks = Lists.newArrayList();
					tracks.add(createTrack(trackCursor));
					artistContainer.put(artist, tracks);
				}
				else
				{
					artistContainer.get(artist).add(createTrack(trackCursor));
				}
				
			} while(trackCursor.moveToNext());
			
			return;
		} catch (Exception ex) {
			LOGGER.warn(ex.getLocalizedMessage());
			Toast.makeText(SonarflowApplication.getAppContext(),  SonarflowApplication.getAppContext().getString(R.string.could_not_load_tracks)  , Toast.LENGTH_SHORT).show();
			return;
		}
		finally {
			closeCursor(trackCursor);
		}
	}

}
