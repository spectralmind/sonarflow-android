package com.spectralmind.sf4android;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.spectralmind.sf4android.MainActivity.BubbleLoader;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.bubble.BubbleLayouter;
import com.spectralmind.sf4android.bubble.BubblesView;
import com.spectralmind.sf4android.media.GenreLoader;
import com.spectralmind.sf4android.media.MoodLoader;

public class SonarflowState {
	private static final Logger LOGGER = LoggerFactory.getLogger(SonarflowState.class);

	private static final String GENRES_XML = 	"genres.xml";
	private static final String MOODS_XML = 	"moods.xml";
	private static final String ATTRIBUTES_XML ="cluster_attributes.xml";
	
	public static final int GENRE_BUBBLES = 0;
	public static final int MOOD_BUBBLES = 1;

	private List<Bubble> genreBubbles;
	private List<Bubble> moodBubbles;
	private BubbleLayouter layouter;
	
	private GenreLoader loader1;
	private MoodLoader loader2;
	
	
	private int activeView = SonarflowState.GENRE_BUBBLES; 
	

	public SonarflowState() {
		genreBubbles = Lists.newArrayList();
		moodBubbles = Lists.newArrayList();
		layouter = new BubbleLayouter();
		loader1 = new GenreLoader(SonarflowState.GENRES_XML, SonarflowState.ATTRIBUTES_XML, layouter);
		loader2 = new MoodLoader(SonarflowState.MOODS_XML, SonarflowState.ATTRIBUTES_XML, layouter);
	}

	public List<Bubble> getBubbles() {
		if (activeView == SonarflowState.GENRE_BUBBLES)
			return genreBubbles;
		else
			return moodBubbles;
	}	
	
	public boolean allFinished() {
		return loader1.isFinished() && loader2.isFinished();
	}
	
	
	public void loadLibrary(final MainActivity mMainActivity) {	
		mMainActivity.lockOrientation();
		loader1.load(mMainActivity, new BubbleLoader() {
			@Override
			public void onFinished(List<Bubble> mBubbles) {
				genreBubbles = mBubbles;
				if (true) {
					// set in view
					mMainActivity.addBubbles(mBubbles);
				}
				
				if (allFinished()) {
					mMainActivity.freeOrientation();
				}
			}
		});
		
		
		loader2.load(mMainActivity, new BubbleLoader() {
			@Override
			public void onFinished(List<Bubble> mBubbles) {
				moodBubbles = mBubbles;
				if (allFinished()) {
					mMainActivity.freeOrientation();
					// if progressDlg was visible we assume that the user wants to see mood view
					if (mMainActivity.getProgressDlg().isShowing()) {
						try {
							mMainActivity.getProgressDlg().dismiss();
						} catch (IllegalArgumentException exc) {
							LOGGER.warn("Could not dismiss progress bar");
						}
//						switchView(mMainActivity.getView());
//						mMainActivity.invalidateOptionsMenu();
					}
				}
			}
		});
	}
	
	public void switchView(BubblesView view) {
		if (activeView == SonarflowState.GENRE_BUBBLES) {
			// switch to mood
			if (!loader2.isFinished()) {
				LOGGER.warn("mood view not yet finished. No view switch.");
				return;
			}
			activeView = SonarflowState.MOOD_BUBBLES;
			view.replaceBubbles(getBubbles());
			return;
		}
		if (activeView == SonarflowState.MOOD_BUBBLES) {
			// switch to genre
			if (!loader1.isFinished()) {
				LOGGER.warn("genre view not yet finished. No view switch.");
				return;
			}
			activeView = SonarflowState.GENRE_BUBBLES;
			view.replaceBubbles(getBubbles());
			return;
		}	
	}
	
	/**
	 * @return the layouter
	 */
	public BubbleLayouter getLayouter() {
		return layouter;
	}

	/**
	 * @return the loader1
	 */
	public GenreLoader getLoader1() {
		return loader1;
	}

	/**
	 * @return the loader2
	 */
	public MoodLoader getLoader2() {
		return loader2;
	}

	/**
	 * @return the activeView
	 */
	public int getActiveView() {
		return activeView;
	}

	public boolean isOtherViewFinished() {
		if (activeView == SonarflowState.GENRE_BUBBLES && loader2.isFinished()) {
			// we have now genres, and mood is finished
			return true;
		}
		if (activeView == SonarflowState.MOOD_BUBBLES && loader1.isFinished()) {
			// we have now moods and genres is finished
			return true;
		}
		// all other cases
		return false;
	}


}
