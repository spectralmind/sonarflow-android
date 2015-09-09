package com.spectralmind.sf4android.Tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.MainActivity.BubbleLoader;
import com.spectralmind.sf4android.MainActivity;
import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.definitions.ClusterDefinition;
import com.spectralmind.sf4android.media.MoodLoader;

import de.umass.lastfm.Tag;


public class ArtistTagAsync
extends
AsyncTask<Void/* Param */, Void /* Progress */, Void /* Result */> 
{
	/** This class retrieves Tags from remote source
	 * The constructor is initialized with an Artist. 
	 *
	 */
	
		public static final int MAX_TAGS_PER_ITEM = 3;
		
		public static Map<String, Integer> othertags = new HashMap<String, Integer>();

		private String mArtist;
		private Collection<Tag> tags = null;
		private List<String> toptags = Lists.newArrayList();
		private List<ClusterDefinition> moods;
		private int sumArtists;
		private int currentNumber;
		private MainActivity mMainActivity;
		private ProgressDialog progressDlg;
		private static Context ctx;
		private BubbleLoader delegate;
		private MoodLoader l;
		private final String lastFM_key = SonarflowApplication.getAppContext().getString(R.string.lastfm_api_key);
		
		private static final Logger LOGGER = LoggerFactory.getLogger(ArtistTagAsync.class);

		public ArtistTagAsync(MainActivity ma, MoodLoader l, String a, Integer current, Integer max, BubbleLoader delegate) {
			super();
			this.mMainActivity = ma;
			this.progressDlg = mMainActivity.getProgressDlg();
			this.moods = l.getDefs();
			this.delegate = delegate;
			this.l = l;
			this.mArtist = a;
			this.currentNumber = current;
			this.sumArtists = max;
		}

		@Override
		protected Void doInBackground(Void... params) 
		{			
			//Log.v("MyLog","Current: "+currentNumber+" Max: "+sumArtists);
			try{
				Log.v("MyLog","Retrieving Tags for "+mArtist);	

				tags = de.umass.lastfm.Artist.getTopTags(mArtist, lastFM_key);
			}
			catch(Exception e){
				Log.v("MyLog","Error: "+e);
			}
			
			if(tags!=null){
				
				// convert to list of strings
				for (Tag tag : tags) {
					toptags.add(tag.getName());
				}

				// get intersection
				List<String> moodtags = getIntersection_Other(toptags, moods);
				//List<String> moodtags = toptags;

				Log.v("MyLog","Current "+currentNumber+" Max: "+sumArtists + ". Found " + moodtags.size()  + " mood tags");
				int i = 0;
				for (String tag : moodtags) {
					// if we have already X tags, do not use the remaining ones
					if (i >= MAX_TAGS_PER_ITEM) continue;
					//					if(tag.getCount() == 0)
					//						continue;
					i++;
					addMood(tag);
					addArtistToMood(tag,mArtist);
				}
			}	

			return null;
		}

		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
//			// set progress
//			int p = progressDlg.getProgress();
//			if (p == 0)
//				progressDlg.setProgress(1);
//			else
//				progressDlg.setProgress(progressDlg.getProgress()+1);
			
			progressDlg.setProgress(currentNumber);
			
			
			// Should be: Check if all Asynctasks are finished
			// Check if this is the task that was started last.
			if(currentNumber==sumArtists){
				Log.v("MyLog","AsyncTasks finished");
				List<Bubble> b = l.createMoods(ctx);

				l.bLoadingFinished = true; // must be set before delegate is called!
				delegate.onFinished(b);

				
				// print out most frequent discarded tags
//				othertags = MapUtil.sortByValue(othertags, false);
//				Log.v("othertags",""+othertags);
				
			}
		}
		
		/**
		 * Calculates intersection of two lists of strings. Ignoring case. Returns "Other", if intersection is empty
		 * @param tags list of strings
		 * @param moods  list of cluster definitions
		 * @return intersection
		 */
		public List<String> getIntersection_Other(List<String> tags,List<ClusterDefinition> moods){
			boolean found = false;
			List<String> l = new ArrayList<String>();
			
			for(String tag : tags){
				boolean bThisFound = false;
				for(ClusterDefinition mood: moods){
					if (mood.containsName(tag)) {
						found = true;
						bThisFound = true;
						l.add(mood.getName()); 
					}		      
				}
				if (!bThisFound) {
					if (!othertags.containsKey(tag)) {
						othertags.put(tag,  1);
					} else {
						othertags.put(tag,  othertags.get(tag) + 1);
					}
				}
			}
			if(!found){
				l.add("Other");
			}
			return l;
		}		
		
		public void addMood(String newMood) {
			if(!l.moodContainer.containsKey(newMood)){
				l.moodContainer.put(newMood, new ArrayList<String>());
			}
		}
		public void addArtistToMood(String mood, String artist) {
		    if(l.moodContainer.containsKey(mood))
		    	l.moodContainer.get(mood).add(artist);        
		}
		
	}
