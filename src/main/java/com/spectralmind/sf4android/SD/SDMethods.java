package com.spectralmind.sf4android.SD;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.os.Handler;
import android.os.Message;

import com.spectralmind.sf4android.SD.SDResultPackage.SDResult;

public final class SDMethods {

	public interface SDSearchResultRequest {
		public abstract void searchResultReady(SDResultPackage resultpackage);
		public abstract void searchRequestStatus(String status,boolean error);	
		public abstract void switchAPIKey();
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SDMethods.class);
	

	private static final String[] SEVEN_DIGITAL_API_KEY = {"7dpyraw4cf", "7dy4uvztg9", "7dspm579pd", "7dxbs8jnnn", "7d8buy5r7j"};
	private static int iCurrentAPIKey = 0;
	private static final int LOWER_LIMIT_FOR_SWITCHING = 50;

	

	private SDResultPackage resultpackage;
	private SDSearchResultRequest SDrsr;
	private String message;
	private MyThread thread;
	private Message msg;
	private int methodNumber;
	private String serachArtistByText,serachID,userKey;
	private String country;

	public final static int ARTIST_FROM_TEXT = 0;
	public final static int RELEASE_FROM_ARTISTID = 1;
	public final static int TRACK_FROM_RELEASEID = 2;
	public final static int TRACKPREVIEW_FROM_RELEASEID = 3;
	public final static int TRACKSPREVIEW_FROM_ARTISTTEXT= 4;
	public final static int RELEASE_FROM_TEXT = 5;

	public final static int ON_FINISHED = 0;
	public final static int ON_ERROR = -1;
	public final static int ON_STATUS = 1;
	public final static int ON_API_SWITCH = 2;

	public SDMethods()
	{
		userKey = getCurrentAPIKey();
	}
	

	public String getCurrentAPIKey() {
		return SEVEN_DIGITAL_API_KEY[iCurrentAPIKey];
	}
	
	public String switchAPIKey() {
		iCurrentAPIKey++;
		// go back to first, if to high
		if (iCurrentAPIKey == SEVEN_DIGITAL_API_KEY.length) iCurrentAPIKey = 0;
		
		userKey = getCurrentAPIKey();
		
		return userKey;
	}


	public void setCountry(String value)
	{
		this.country = value;
	}

	private  Handler handler = new Handler() { //just handler can manipulate UIThread

		public void handleMessage(Message msg) {
			int status = msg.arg1;  //if 1 send resultpackage
			if(status == ON_FINISHED)
			{
				SDrsr.searchResultReady(resultpackage); //just a handler can call this method
			}
			else if(status == ON_STATUS)  //send status message
			{
				SDrsr.searchRequestStatus(message,false);
			}
			else if(status == ON_ERROR)  //send error message
			{
				SDrsr.searchRequestStatus(message,true);
			}
			else if(status == ON_API_SWITCH)  //switch api key
			{
				SDrsr.switchAPIKey();
			}
		}
	};

	public void getArtistFromTextSearch(String text,SDSearchResultRequest sdrsr)
	{
		SDrsr = sdrsr;
		serachArtistByText = URLencode(text);
		methodNumber = SDMethods.ARTIST_FROM_TEXT;
		thread = new MyThread();
		thread.start();
	}

	public void getReleasesFromArtistID(String ID,SDSearchResultRequest sdrsr)
	{
		SDrsr = sdrsr;
		serachID = URLencode(ID);
		methodNumber = SDMethods.RELEASE_FROM_ARTISTID;
		thread = new MyThread();
		thread.start();
	}

	public void getTracksFromReleaseID(String ID,SDSearchResultRequest sdrsr)
	{
		SDrsr = sdrsr;
		serachID = URLencode(ID);
		methodNumber = SDMethods.TRACK_FROM_RELEASEID;
		thread = new MyThread();
		thread.start();
	}

	public void getTracksPreviewFromReleaseID(String ID,SDSearchResultRequest sdrsr)
	{
		SDrsr = sdrsr;
		serachID = URLencode(ID);
		methodNumber = SDMethods.TRACKPREVIEW_FROM_RELEASEID;
		thread = new MyThread();
		thread.start();
	}

	public void getTracksPreviewFromArtistText(String text,SDSearchResultRequest sdrsr)
	{
		SDrsr = sdrsr;
		serachArtistByText = URLencode(text);
		methodNumber = SDMethods.TRACKSPREVIEW_FROM_ARTISTTEXT;
		thread = new MyThread();
		thread.start();
	}
	public void getReleaseFromTextSearch(String text,SDSearchResultRequest sdrsr)
	{
		SDrsr = sdrsr;
		serachArtistByText = URLencode(text);
		methodNumber = SDMethods.RELEASE_FROM_TEXT;
		thread = new MyThread();
		thread.start();
	}


	public void cancel()
	{
		if(thread != null)
		{
			thread.setCancel(true);
			thread.sendMessage("Canceled!");
			thread = null;
		}
	}

	private String URLencode(String in)
	{
		in = in.trim();
		in = URLEncoder.encode(in);
		return in;
	}

	private boolean proveIfExist(String title,ArrayList<String> titleContainer)
	{
		for(String Title : titleContainer)
		{
			if(title.intern() == Title.intern())
				return true;
		}
		return false;
	}

	private class MyThread extends Thread {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		BufferedReader input = null;
		URL url = null;
		boolean error = false;
		DocumentBuilder docBuilder = null;
		Document doc = null;
		boolean CANCEL = false;

		public void setCancel(boolean value)
		{
			this.CANCEL = value;
		}

		public void run() {	

			if(methodNumber == SDMethods.ARTIST_FROM_TEXT)
			{	
				resultpackage = new SDResultPackage();
				if(connectANDcreateDoc("http://api.7digital.com/1.2/artist/search?q=" +
						serachArtistByText + 		
						"&imageSize=300&oauth_consumer_key=" +
						userKey + "&pagesize=5&country=" + country)) {
					if(!checkForErros(doc,input))  {

						NodeList searchResults=doc.getElementsByTagName("searchResults");
						if (searchResults != null && searchResults.getLength() > 0) {


							for (int i = 0; i < searchResults.getLength() && !CANCEL; i++) {

								NodeList nl = ((org.w3c.dom.Element) searchResults.item(i)).getElementsByTagName("artist");
								if (nl != null && nl.getLength() > 0) {

									try {

										Node nArtist = nl.item(0);

										SDResult resultBuff = resultpackage.new SDResult();

										resultBuff.setArtistID		(((org.w3c.dom.Element) nArtist).getAttribute("id"));
										resultBuff.setArtist   		(((org.w3c.dom.Element) nArtist).getElementsByTagName("name").item(0).getFirstChild().getNodeValue());
										resultBuff.setArtistUrl		(((org.w3c.dom.Element) nArtist).getElementsByTagName("url").item(0).getFirstChild().getNodeValue());
										resultBuff.setArtistImageUrl	(((org.w3c.dom.Element) nArtist).getElementsByTagName("image").item(0).getFirstChild().getNodeValue());
										resultBuff.writeArtistImageByteFromURL(resultBuff.getArtistImageUrl());
										resultBuff.setPopularity	(((org.w3c.dom.Element) nArtist).getElementsByTagName("popularity").item(0).getFirstChild().getNodeValue());

										resultpackage.addSDResultItem(resultBuff);

									} catch (DOMException e) {
										e.printStackTrace();
										sendErrorMessage(e.getMessage());
										error = true;
									} catch (IOException e) {
										e.printStackTrace();
										error = true;
										sendErrorMessage(e.getMessage());
									}									
								} else {
									// log
									error = true;
									sendErrorMessage("searchResults/searchResult/release not found in XML");
								}
							}
						}
					}
				}
				else
					error = true;
				onFinished();
			}

			else if(methodNumber == SDMethods.RELEASE_FROM_ARTISTID)
			{	
				resultpackage = new SDResultPackage();
				ArrayList<String> titleContainer = new ArrayList<String>();

				if(connectANDcreateDoc("http://api.7digital.com/1.2/artist/releases?artistid=" +
						serachID + 		
						"&imageSize=350&oauth_consumer_key=" +
						userKey + "&pagesize=70&country=" + country))
				{
					if(!checkForErros(doc,input))  				
					{
						NodeList releases=doc.getElementsByTagName("release");
						if (releases != null && releases.getLength() > 0) 
						{
							for (int i = 0; i < releases.getLength() && !CANCEL; i++)
							{

								SDResult resultBuff = resultpackage.new SDResult();
								resultBuff.setReleaseID(((org.w3c.dom.Element) releases.item(i)).getAttribute("id"));
								Node node = releases.item(i);
								NodeList nodeList;
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("title");
								if(!proveIfExist(nodeList.item(0).getFirstChild().getNodeValue(),titleContainer))
								{	
									titleContainer.add(nodeList.item(0).getFirstChild().getNodeValue());
									resultBuff.setRelease(nodeList.item(0).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("type");
									resultBuff.setReleaseType(nodeList.item(0).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("url");
									resultBuff.setReleaseUrl(nodeList.item(1).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("year");
									resultBuff.setReleaseYear(nodeList.item(0).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("releaseDate");
									resultBuff.setReleaseDate(nodeList.item(0).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("image");    
									try {
										resultBuff.writeReleaseImageByteFromURL(nodeList.item(0).getFirstChild().getNodeValue());
									} catch (DOMException e) {
										e.printStackTrace();
										sendErrorMessage(e.getMessage());
										error = true;
									} catch (IOException e) {
										e.printStackTrace();
										sendErrorMessage(e.getMessage());
										error = true;
									}
									resultBuff.setReleaseImageUrl(nodeList.item(0).getFirstChild().getNodeValue());


									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("artist");
									Node node1 = nodeList.item(0);
									nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("name");
									resultBuff.setArtist(nodeList.item(0).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("appearsAs");
									resultBuff.setReleaseAppearsAs(nodeList.item(0).getFirstChild().getNodeValue());
									nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("url");
									resultBuff.setArtistUrl(nodeList.item(0).getFirstChild().getNodeValue());


									nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("label");
									Node node2 = nodeList.item(0);
									nodeList = ((org.w3c.dom.Element) node2).getElementsByTagName("name");
									resultBuff.setRecordProducer(nodeList.item(0).getFirstChild().getNodeValue());

									resultpackage.addSDResultItem(resultBuff);
								}
							}
						}
					}
				}
				else
					error = true;
				onFinished();
			}   

			else if(methodNumber == SDMethods.TRACK_FROM_RELEASEID)
			{
				resultpackage = new SDResultPackage();

				if(connectANDcreateDoc("http://api.7digital.com/1.2/release/tracks?releaseid=" +
						serachID + 		
						"&imageSize=350&oauth_consumer_key=" +
						userKey + "&pagesize=20&country=" + country))
				{
					if(!checkForErros(doc,input))  				
					{
						NodeList releases=doc.getElementsByTagName("track");
						if (releases != null && releases.getLength() > 0) 
						{
							for (int i = 0; i < releases.getLength() && !CANCEL; i++)
							{
								SDResult resultBuff = resultpackage.new SDResult();
								resultBuff.setTrackID(((org.w3c.dom.Element) releases.item(i)).getAttribute("id"));
								resultBuff.setTrackPreviewUrl("http://api.7digital.com/1.2/track/preview?trackid=" +
										((org.w3c.dom.Element) releases.item(i)).getAttribute("id") + "&oauth_consumer_key=7d8buy5r7j");
								Node node = releases.item(i);
								NodeList nodeList;
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("title");
								resultBuff.setTrack(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("trackNumber");
								resultBuff.setTrackNumber(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("duration");
								resultBuff.setTrackDuration(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("url");
								resultBuff.setTrackUrl(nodeList.item(3).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("streamingReleaseDate");
								if(nodeList.item(0).hasChildNodes())
									resultBuff.setTrackStreamingReleaseDate(nodeList.item(0).getFirstChild().getNodeValue());


								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("release");
								resultBuff.setReleaseID(((org.w3c.dom.Element) nodeList.item(0)).getAttribute("id"));
								Node node1 = nodeList.item(0);
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("title");
								resultBuff.setRelease(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("type");
								resultBuff.setReleaseType(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("url");
								resultBuff.setReleaseUrl(nodeList.item(1).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("image");    
								try {
									resultBuff.writeReleaseImageByteFromURL(nodeList.item(0).getFirstChild().getNodeValue());
								} catch (DOMException e) {
									e.printStackTrace();
									sendErrorMessage(e.getMessage());
									error = true;
								} catch (IOException e) {
									e.printStackTrace();
									sendErrorMessage(e.getMessage());
									error = true;
								}
								resultBuff.setReleaseImageUrl(nodeList.item(0).getFirstChild().getNodeValue());

								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("artist");
								resultBuff.setArtistID(((org.w3c.dom.Element) nodeList.item(0)).getAttribute("id"));
								Node node2 = nodeList.item(0);
								nodeList = ((org.w3c.dom.Element) node2).getElementsByTagName("name");
								resultBuff.setArtist(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node2).getElementsByTagName("appearsAs");
								resultBuff.setReleaseAppearsAs(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node2).getElementsByTagName("url");
								resultBuff.setArtistUrl(nodeList.item(0).getFirstChild().getNodeValue());

								resultpackage.addSDResultItem(resultBuff);
							}
						}
					}
				}
				else
					error = true;
				onFinished();
			}

			//			else if (methodNumber == SDMethods.TRACKPREVIEW_FROM_RELEASEID)
			//			{
			//				resultpackage = new SDResultPackage();
			//
			//				if(connectANDcreateDoc("http://api.7digital.com/1.2/release/tracks?releaseid=" +
			//						serachID + 		
			//						"&imageSize=350&oauth_consumer_key=" +
			//						userKey + "&pagesize=20&country=" + country))
			//				{
			//					if(!checkForErros(doc,input))  				
			//					{
			//						NodeList releases=doc.getElementsByTagName("track");
			//						if (releases != null && releases.getLength() > 0) 
			//						{
			//							for (int i = 0; i < releases.getLength() && !CANCEL; i++)
			//							{
			//								SDResult resultBuff = resultpackage.new SDResult();
			//								resultBuff.setTrackPreviewUrl("http://api.7digital.com/1.2/track/preview?trackid=" +
			//										((org.w3c.dom.Element) releases.item(i)).getAttribute("id") + "&oauth_consumer_key=" + userKey);
			//								resultpackage.addSDResultItem(resultBuff);
			//							}
			//						}
			//					}
			//				}
			//				else
			//					error = true;
			//				onFinished();
			//			}


			else if(methodNumber == SDMethods.TRACKSPREVIEW_FROM_ARTISTTEXT)
			{
				resultpackage = new SDResultPackage();

				String artistID = null;

				if(connectANDcreateDoc("http://api.7digital.com/1.2/artist/browse?letter=" +
						serachArtistByText + 		
						"&imageSize=300&oauth_consumer_key=" +
						userKey + "&pagesize=50&country=" + country))
				{
					if(!checkForErros(doc,input))  
					{
						NodeList artists=doc.getElementsByTagName("artist");
						if (artists != null && artists.getLength() > 0) 
						{
							for (int i = 0; i < 1 && !CANCEL; i++)
							{
								artistID = ((org.w3c.dom.Element) artists.item(i)).getAttribute("id");
							}
						}
					}
				}
				else
					error = true;

				if(connectANDcreateDoc("http://api.7digital.com/1.2/artist/toptracks?artistid=" +
						artistID + 		
						"&imageSize=350&oauth_consumer_key=" +
						userKey + "&pagesize=20&country=" + country) && !error)
				{
					if(!checkForErros(doc,input))  
					{
						NodeList tracks=doc.getElementsByTagName("track");
						if (tracks != null && tracks.getLength() > 0) 
						{
							for (int i = 0; i < tracks.getLength() && !CANCEL; i++)
							{
								SDResult resultBuff = resultpackage.new SDResult();
								resultBuff.setTrackID(((org.w3c.dom.Element) tracks.item(i)).getAttribute("id"));
								resultBuff.setTrackPreviewUrl("http://api.7digital.com/1.2/track/preview?trackid=" +
										((org.w3c.dom.Element) tracks.item(i)).getAttribute("id") + "&oauth_consumer_key=7d8buy5r7j");
								Node node = tracks.item(i);
								NodeList nodeList;
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("title");
								resultBuff.setTrack(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("trackNumber");
								resultBuff.setTrackNumber(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("duration");
								resultBuff.setTrackDuration(nodeList.item(0).getFirstChild().getNodeValue());

								nodeList = ((org.w3c.dom.Element) node).getElementsByTagName("release");
								Node node1 = nodeList.item(0);
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("title");
								resultBuff.setRelease(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("type");
								resultBuff.setReleaseType(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("image");    
								try {
									resultBuff.writeReleaseImageByteFromURL(nodeList.item(0).getFirstChild().getNodeValue());
								} catch (DOMException e) {
									e.printStackTrace();
									sendErrorMessage(e.getMessage());
									error = true;
								} catch (IOException e) {
									e.printStackTrace();
									sendErrorMessage(e.getMessage());
									error = true;
								}
								nodeList = ((org.w3c.dom.Element) node1).getElementsByTagName("artist");
								Node node2 = nodeList.item(0);
								nodeList = ((org.w3c.dom.Element) node2).getElementsByTagName("name");
								resultBuff.setArtist(nodeList.item(0).getFirstChild().getNodeValue());
								nodeList = ((org.w3c.dom.Element) node2).getElementsByTagName("appearsAs");
								resultBuff.setReleaseAppearsAs(nodeList.item(0).getFirstChild().getNodeValue());

								resultpackage.addSDResultItem(resultBuff);
							}
						}
					}
				}
				else
					error = true;

				onFinished();
			} 

			else if(methodNumber == SDMethods.RELEASE_FROM_TEXT) {	
				resultpackage = new SDResultPackage();
				if(connectANDcreateDoc("http://api.7digital.com/1.2/release/search?q=" +
						serachArtistByText + 		
						"&imageSize=200&type=album,single&oauth_consumer_key=" +
						userKey + "&pagesize=5&country=" + country))  {
					// max imageSize is 200

					if(!checkForErros(doc,input))  {
						NodeList searchResults=doc.getElementsByTagName("searchResults");
						if (searchResults != null && searchResults.getLength() > 0) {
							for (int i = 0; i < searchResults.getLength() && !CANCEL; i++) {
								NodeList nl = ((org.w3c.dom.Element) searchResults.item(i)).getElementsByTagName("release");
								if (nl != null && nl.getLength() > 0) {

									try {

										Node nRelease = nl.item(0);

										SDResult resultBuff = resultpackage.new SDResult();

										resultBuff.setReleaseID(((org.w3c.dom.Element) nRelease).getAttribute("id"));
										resultBuff.setRelease   		(((org.w3c.dom.Element) nRelease).getElementsByTagName("title").item(0).getFirstChild().getNodeValue());
										// item(1): item(0) would be the artist url
										resultBuff.setReleaseUrl		(((org.w3c.dom.Element) nRelease).getElementsByTagName("url").item(1).getFirstChild().getNodeValue());
										resultBuff.setReleaseImageUrl	(((org.w3c.dom.Element) nRelease).getElementsByTagName("image").item(0).getFirstChild().getNodeValue());

										resultBuff.writeReleaseImageByteFromURL(resultBuff.getReleaseImageUrl());

										resultpackage.addSDResultItem(resultBuff);
									} catch (DOMException e) {
										e.printStackTrace();
										sendErrorMessage(e.getMessage());
										error = true;
									} catch (IOException e) {
										e.printStackTrace();
										error = true;
										sendErrorMessage(e.getMessage());
									}									
								} else {
									// log
									error = true;
									sendErrorMessage("searchResults/searchResult/release not found in XML");
								}
							}
						}
					}
				}
				else
					error = true;
				onFinished();
			}

			//Method 6
			//....................................................................
		}

		private void onFinished()
		{
			if(!error && !CANCEL)
			{
				msg = handler.obtainMessage();	
				msg.arg1 = SDMethods.ON_FINISHED;
				handler.sendMessage(msg);
			}
		}

		private boolean connectANDcreateDoc(String urltext)
		{
			if(CANCEL)
				return false;
		
			try {
				url = new URL(urltext);
				docBuilder = docBuilderFactory.newDocumentBuilder();
				
				URLConnection urlc = url.openConnection();
				
				// SD API key limit handling
				// This should be somerwhere else, not here!
				try {
					int SDcurrent = Integer.parseInt(urlc.getHeaderField("X-RateLimit-Current"));
					int SDlimit = Integer.parseInt(urlc.getHeaderField("X-RateLimit-Limit"));
					//LOGGER.debug("SD API-Calls to go: " + (SDlimit - SDcurrent));
					if ((SDlimit - SDcurrent) < LOWER_LIMIT_FOR_SWITCHING) {
						msg = handler.obtainMessage();	
						msg.arg1 = SDMethods.ON_API_SWITCH;
						handler.sendMessage(msg);
					}
				}catch (NumberFormatException e) {
					LOGGER.warn("Either X-RateLimit-Current or X-RateLimit-Limit header missing from response");
				}
				// 

				doc = docBuilder.parse (new InputSource(url.openStream()));
				doc.getDocumentElement().normalize();
			}catch (SAXException e) {
				sendErrorMessage(e.getMessage());
				return false;
			} catch (IOException e) {
				sendErrorMessage(e.getMessage());
				return false;
			} catch (ParserConfigurationException e) {
				sendErrorMessage(e.getMessage());
				return false;
			}
			return true;
		}

		private void sendMessage(String value)
		{
			if(!CANCEL)
			{
				message = value;
				msg = handler.obtainMessage();	
				msg.arg1 = SDMethods.ON_STATUS;
				handler.sendMessage(msg);
			}
		}
		private void sendErrorMessage(String value)
		{
			if(!CANCEL)
			{
				message = value;
				msg = handler.obtainMessage();	
				msg.arg1 = SDMethods.ON_ERROR;
				handler.sendMessage(msg);
			}
		}

		private boolean checkForErros(Document doc,BufferedReader input){

			if(doc.getDocumentElement().getNodeName() == null)
			{
				resultpackage.setERROR(true);
				resultpackage.setErrorCode("");
				try {
					resultpackage.setErrorMessage(input.readLine());
				} catch (IOException e) {
					e.printStackTrace();
					sendErrorMessage(e.getMessage());
				}
				return true;
			}

			else 
			{
				if(doc.getDocumentElement().getAttribute("status").intern() == "error")
				{
					resultpackage.setERROR(true);
					NodeList error = doc.getElementsByTagName("error");
					resultpackage.setErrorCode(((org.w3c.dom.Element) error.item(0)).getAttribute("code"));
					error = doc.getElementsByTagName("errorMessage");
					resultpackage.setErrorMessage(error.item(0).getFirstChild().getNodeValue());
					return true;
				}
				else
				{
					NodeList items = doc.getElementsByTagName("totalItems");
					if(items.item(0).getFirstChild().getNodeValue().intern() == "0")
					{
						resultpackage.setNOMATCH(true);	
						return true;
					}
				}
			}
			return false;
		}
	}
}
