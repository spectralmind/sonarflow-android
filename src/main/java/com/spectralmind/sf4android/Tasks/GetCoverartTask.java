package com.spectralmind.sf4android.Tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.bubble.Bubble;


/** This class retrieves artwork from remote source
 * The constructor is initialized with a Bubble. Supported Bubble.mediaItem's are:
 * - Track
 * - Album
 * - Artist
 * @author ewald
 *
 */
public abstract class GetCoverartTask
extends
AsyncTask<Void/* Param */, Void /* Progress */, Drawable /* Result */> 
{

	private Bubble mBubble;

	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetCoverartTask.class);

	private static BitmapFactory.Options bmOptions;


	public GetCoverartTask(Bubble b) {
		super();
		mBubble = b;
		GetCoverartTask.bmOptions = new BitmapFactory.Options();
		GetCoverartTask.bmOptions.inPurgeable = true;
		GetCoverartTask.bmOptions.inInputShareable = true;

	}

	protected Drawable doInBackground(Void... params) 
	{
		throw new UnsupportedOperationException("please override");
	}
	
	protected void onPostExecute(Drawable cover)
	{
		throw new UnsupportedOperationException("please override");
	}
	
	/**
	 * @return the mBubble
	 */
	public Bubble getmBubble() {
		return mBubble;
	}

	

	/** Convenience method to create a Drawable instance from an byte array
	 * 
	 * @param url the URL
	 * @param c the context (to care for pixel density
	 * @return the Drawable
	 * @throws IOException
	 */
	public static Drawable drawableFromByteBuffer(byte[] buf, Context c) throws IOException {
		Bitmap x = BitmapFactory.decodeByteArray(buf, 0, buf.length, GetCoverartTask.bmOptions);
		//Bitmap bmScaled = Bitmap.createScaledBitmap(x,  300, 300, true);
		return new BitmapDrawable(SonarflowApplication.getAppContext().getResources(), x);
	}


	/** Convenience method to create a Drawable instance from an URL
	 * 
	 * @param url the URL
	 * @param c the context (to care for pixel density
	 * @return the Drawable
	 * @throws IOException
	 */
	public static Drawable drawableFromUrl(String url, Context c) throws IOException {
		Bitmap x;

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.connect();
		InputStream input = connection.getInputStream();

		x = BitmapFactory.decodeStream(input);
		return new BitmapDrawable(c.getResources(), x);
	}

}
