package com.spectralmind.sf4android.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.spectralmind.sf4android.MainActivity;
import com.spectralmind.sf4android.R;
import com.spectralmind.sf4android.SonarflowApplication;
import com.spectralmind.sf4android.MainActivity.BubbleLoader;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.bubble.BubbleLayouter;
import com.spectralmind.sf4android.definitions.ClusterDefinitionWithPos;

public class GenreLoader extends AbstractLoader {


	private static final Logger LOGGER = LoggerFactory.getLogger(GenreLoader.class);

	private final Map<String, Genre> genresByDefinitionName = Maps.newHashMap();

	public GenreLoader(String defsFile, String attrDefsFile, BubbleLayouter layouter) {
		super(defsFile, attrDefsFile, layouter);
	}	


	@Override
	public void load(final MainActivity mMainActivity, BubbleLoader delegate) {

		// this loads definitions from xml file
		super.load(mMainActivity, delegate);

		loadGenres(mMainActivity);

		ArrayList<? extends MediaGroup<?>> l = new ArrayList<MediaGroup<?>>(genresByDefinitionName.values());
		Collections.sort(l);
		List<Bubble> b = layouter.createBubbles(l, attrDefs);	

		delegate.onFinished(b);
		bLoadingFinished = true;
	}



	private void loadGenres(Context context) {
		LOGGER.debug("Loading genres...");
		genresByDefinitionName.clear();
		String genreFilter[] = { MediaStore.Audio.Genres.NAME, MediaStore.Audio.Genres._ID };

		Cursor genreCursor = null;
		try {
			genreCursor = new CursorLoader(context, MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, genreFilter, null,
					null, null).loadInBackground();
			if(genreCursor == null || genreCursor.moveToFirst() == false) {
				LOGGER.warn("Genre cursor returned zero results");
				return;
			}
			do {
				Genre genre = findOrCreateGenre(genreCursor);
				// Get the recently added id 
				if (genre.getIds().size() > 0) {
					long lastId = genre.getIds().get(genre.getIds().size()-1);	
					Uri contentURI = MediaStore.Audio.Genres.Members.getContentUri("external", lastId);
					List<Track> tracks = loadTracks(contentURI, context);
					LOGGER.trace("Adding {} tracks to genre {}", tracks.size(), genre.getName());
					genre.addTracks(tracks);	
				} else {
					LOGGER.warn("Genre has no genre id. No tracks loaded.");
				}
			} while(genreCursor.moveToNext());
		} catch (Exception e) {
			LOGGER.warn("No tracks");			
		}
		finally {
			closeCursor(genreCursor);
		}
	}	

	private Genre findOrCreateGenre(Cursor cursor) {
		String genreName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME));
		long genreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID));
		LOGGER.trace("Reading genre row: {}", genreName);
		ClusterDefinitionWithPos definitionPos = mapper.findDefinitionForName(genreName);
		Genre genre = genresByDefinitionName.get(definitionPos.def.getName());
		// if the genre does not yet exist
		if(genre == null) {
			// we create it
			genre = new Genre(genreId, definitionPos.def, definitionPos.pos);
			LOGGER.debug("Created genre: {}", genre.getName());
			genresByDefinitionName.put(definitionPos.def.getName(), genre);
		} else {
			// else, we just add the new genre id
			genre.addId(genreId);
		}
		return genre;
	}	


	private List<Track> loadTracks(Uri contentURI, Context context) {
		String trackColumns[] = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.DURATION };
		Cursor trackCursor = null;
		try {
			trackCursor = new CursorLoader(context, contentURI, trackColumns, null, null, null).loadInBackground();
			if(trackCursor.moveToFirst() == false) {
				LOGGER.warn("Track cursor returned zero results");
				return Lists.newArrayList();
			}

			List<Track> results = Lists.newArrayList();
			do {
				results.add(createTrack(trackCursor));
			} while(trackCursor.moveToNext());
			return results;
		} catch (Exception ex) {
			LOGGER.warn(ex.getLocalizedMessage());
			Toast.makeText(SonarflowApplication.getAppContext(),  SonarflowApplication.getAppContext().getString(R.string.could_not_load_tracks)  , Toast.LENGTH_SHORT).show();
			return Lists.newArrayList();
		}		finally {
			closeCursor(trackCursor);
		}
	}

}
