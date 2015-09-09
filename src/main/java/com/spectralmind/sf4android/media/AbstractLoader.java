package com.spectralmind.sf4android.media;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.provider.MediaStore;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;
import com.spectralmind.sf4android.MainActivity;
import com.spectralmind.sf4android.MainActivity.BubbleLoader;
import com.spectralmind.sf4android.bubble.BubbleLayouter;
import com.spectralmind.sf4android.definitions.ClusterAttributeDefinition;
import com.spectralmind.sf4android.definitions.ClusterDefinition;
import com.spectralmind.sf4android.definitions.ClusterDefinitionLoader;
import com.spectralmind.sf4android.definitions.DefinitionMapper;

public abstract class AbstractLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLoader.class);
	
	protected List<ClusterAttributeDefinition> attrDefs;
	protected List<ClusterDefinition> defs;
	protected DefinitionMapper mapper;
	
	protected BubbleLayouter layouter;

	protected boolean bLoadingInProgress = false;
	public boolean bLoadingFinished = false;
	

	private String defsFile;
	private String attrDefsFile;

	public AbstractLoader(String defsFile, String attrDefsFile, BubbleLayouter layouter) {
		this.defsFile = defsFile;
		this.attrDefsFile = attrDefsFile;
		this.layouter = layouter;
	}

	/** This method initializes these protected variables:
	 * <ul>
	 * <li>attrDefs</li>
	 * <li>defs</li>
	 * <li>mapper</li>
	 * </ul>
	 * @param mMainActivity
	 * @param delegate
	 */
	public void load(final MainActivity mMainActivity, BubbleLoader delegate) {
		attrDefs = loadAttributeDefinitions();
		mapper = createDefinitionMapper();
	}


	private List<ClusterAttributeDefinition> loadAttributeDefinitions() {
		URL attributeDefFile = Resources.getResource(attrDefsFile);
		InputStream is = null;
		List<ClusterAttributeDefinition> l;
		try {
			is = attributeDefFile.openStream();
			ClusterDefinitionLoader loader = new ClusterDefinitionLoader();
			l = loader.loadAttributeDefinitions(is);
		}
		catch(IOException e) {
			throw Throwables.propagate(e);
		}
		finally {
			Closeables.closeQuietly(is);
		}
		return l;
	}

	public DefinitionMapper createDefinitionMapper() {
		URL url = Resources.getResource(defsFile);
		InputStream is = null;
		try {
			is = url.openStream();
			ClusterDefinitionLoader loader = new ClusterDefinitionLoader();
			defs = loader.loadDefinitions(is);
			return new DefinitionMapper(defs);
		}
		catch(IOException e) {
			throw Throwables.propagate(e);
		}
		finally {
			Closeables.closeQuietly(is);
		}
	}


	protected void closeCursor(Cursor cursor) {
		// !!!: Cursor does NOT implement closeable in API 14!
		// Closeables.closeQuietly(cursor);
		if(cursor != null) {
			cursor.close();
		}
	}
	
	public boolean isFinished() {
		return bLoadingFinished;
	}


	/**
	 * @return the defs
	 */
	public List<ClusterDefinition> getDefs() {
		return defs;
	}


	protected Track createTrack(Cursor trackCursor) {
		return new Track(trackCursor.getString(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
				trackCursor.getString(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
				trackCursor.getString(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
				trackCursor.getInt(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)),
				trackCursor.getInt(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
				trackCursor.getString(trackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
	}
}
