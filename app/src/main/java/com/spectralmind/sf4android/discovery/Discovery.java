package com.spectralmind.sf4android.discovery;


import java.util.ArrayList;
import java.util.Collection;

import android.os.Handler;
import android.os.Message;

import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.bubble.Bubble;

import de.umass.lastfm.CallException;

public class Discovery {

	public final static int ON_ARTIST_SIMILAR_FINSHED = 1;
	public final static int ON_CANCELED = -1;
	public final static int ON_START = 0;
	public final static int ON_ERROR_MESSAGE = -2;
	
	private static DiscoveryDelegate delegate;
	private static ArrayList<ArtistInfo> artistsInfo;
	private MyThread thread = null;
	
	private String currentSearchArtist = "";
	private static Bubble currentBubble;
	
	private static String errorMessage;
	
	public class ArtistInfo{
		ArtistInfo(String name,String wikitext)
		{
			this.name = name;
			this.wikitext = wikitext;
		}
		ArtistInfo(String name)
		{
			this.name = name;
		}
		public String name;
		public String wikitext;
	}
	
	public static interface DiscoveryDelegate{
		void onFinishedSimilarArtistLodaing(ArrayList<ArtistInfo> artists,Bubble bubble);
		void onCanceled();
		void onStart();
		void onErrorMessage(String message);
	}
	
	public Discovery(DiscoveryDelegate delegate)
	{
		Discovery.delegate = delegate;
	}
	
	public Discovery(){}
	
	public void setDiscoveryDelegate(DiscoveryDelegate delegate)
	{
		Discovery.delegate = delegate;
	}
	
	final static private Handler handler = new Handler() { 

		public void handleMessage(Message msg) {
			int status = msg.arg1;  
			if(status == Discovery.ON_ARTIST_SIMILAR_FINSHED)
			{
				if(delegate == null)return;
				delegate.onFinishedSimilarArtistLodaing(artistsInfo,currentBubble);
			}
			else if(status == Discovery.ON_START)  
			{
				if(delegate == null)return;
				delegate.onStart();
			}
			else if(status == Discovery.ON_CANCELED)  
			{
				if(delegate == null)return;
				delegate.onCanceled();
			}
			else if(status == Discovery.ON_ERROR_MESSAGE)  
			{
				if(delegate == null)return;
				delegate.onErrorMessage(errorMessage);
			}
		}
	};
	
	public void startForSimilarArtist(final String artist,Bubble bubble)
	{
		currentBubble = bubble;
		thread = new MyThread();
		currentSearchArtist = artist;
		thread.start();
	}
	
	private void sendMessage(int value)
	{
		Message msg = handler.obtainMessage();
		msg.arg1 = value;
		handler.sendMessage(msg);
	}
	
	public boolean isWorking()
	{
		if(thread == null)
			return false;
		return this.thread.working;
	}
	
	public String getCurrentSearchArtist()
	{
		return currentSearchArtist;
	}
	
	public void cancel()
	{
		if(this.thread != null)
			thread.working = false;
		
		thread = null;
		currentSearchArtist = "";
		if(artistsInfo == null)
			return;
		if(artistsInfo.size() != 0)
			artistsInfo.removeAll(artistsInfo);
		sendMessage(Discovery.ON_CANCELED);
	}
	
	private class MyThread extends Thread
	{
		public boolean working;
		
		public void run()
		{
			sendMessage(Discovery.ON_START);
			working = true;
			artistsInfo = new ArrayList<ArtistInfo>();
			
			// EP: do not disable the cache. Was enabled in the MainActivity's onCreate
			//Caller.getInstance().setCache(null);
			
			String key = SonarflowApplication.getAppContext().getString(R.string.lastfm_api_key);
			Collection<de.umass.lastfm.Artist> artists;
			try{
			artists = de.umass.lastfm.Artist.getSimilar(currentSearchArtist,5,key);
			}
			catch(CallException e){
				errorMessage = e.getMessage();
				sendMessage(Discovery.ON_ERROR_MESSAGE);
//				sendMessage(Discovery.ON_CANCELED);
				return;
				}
			int counter = 0;
			for (de.umass.lastfm.Artist artist : artists) {
				if(counter >= 5 || working == false)
					break;
//				artistsInfo.add(new ArtistInfo(
//				artist.getName(),
//				Artist.getInfo(artist.getMbid(), key) != null ? 
//				Artist.getInfo(artist.getMbid(), key).getWikiSummary() : null));
				artistsInfo.add(new ArtistInfo(artist.getName()));
				counter++;
			}
			if(working)
				sendMessage(Discovery.ON_ARTIST_SIMILAR_FINSHED);
		}
	}

	public void destroy() {
		if(thread == null)
			return;
		thread.working = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
