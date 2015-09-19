package com.spectralmind.sf4android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.spectralmind.sf4android.bubble.Animator.AnimationHistory;
import com.spectralmind.sf4android.bubble.Bubble;
import com.spectralmind.sf4android.bubble.BubblesView;
import com.spectralmind.sf4android.bubble.BubblesView.PiemenueSaver;
import com.spectralmind.sf4android.media.MediaItem;
import com.spectralmind.sf4android.player.PlayerService;
import com.spectralmind.sf4android.player.PlayerToolbarFragment;

import de.umass.lastfm.Caller;
import de.umass.lastfm.cache.FileSystemCache;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener, OnClickListener {

	public interface BubbleLoader{
		public void onFinished(List<Bubble> bubbles);
	};
	
	private static final String PLAYER_TOOLBAR_FRAGMENT_TAG = "PlayerToolbarFragment";
	private static final String VIEW_TOOLBAR_FRAGMENT_TAG = "ViewToolbarFragment";

	private static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);
	
	private SonarflowState sonarflowState;
	
	private ProgressDialog progressDlg = null;
	
	private BubblesView view;
	private ImageView backArrow;
	private SaveContainer musicView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		// ancestral navigation
		if (getActionBar() != null) 
			getActionBar().setHomeButtonEnabled(true);
		

	
	
		view = (BubblesView) findViewById(R.id.bubbleView);
		view.setActivityReference(this);
		backArrow = (ImageView) findViewById(R.id.backArrow);
		backArrow.setVisibility(View.INVISIBLE);
			
		// Set default preferences. Since thrid argument is false, we can safely
		// do that without overrding any user settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
				
		boolean restoreViewState = true;

		
		if (savedInstanceState != null) {
			musicView = ((SaveContainer) getFragmentManager().findFragmentById(
					R.id.root));
			sonarflowState = musicView.getState();			
		}
		
		if (savedInstanceState == null || sonarflowState == null) { // just once in app
			musicView = new SaveContainer();
			musicView.setRetainInstance(true);
			FragmentTransaction fragmentTransaction = getFragmentManager()
					.beginTransaction();
			fragmentTransaction.add(R.id.root, musicView).commit();
			sonarflowState = initializeSonarflow();
			restoreViewState = false;

			// Prepare Last.fm cache
			Caller.getInstance().setCache(new FileSystemCache(getCacheDir()));
			
			// Prepare http cache
			try {
				File httpCacheDir = new File(getApplicationContext()
						.getCacheDir(), "http");
				long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
				HttpResponseCache.install(httpCacheDir, httpCacheSize);
			} catch (IOException e) {
				MainActivity.LOGGER
						.info( "HTTP_response_cache_installation_failed" + e);
			}
		}
		
		createToolbars();

		view.addBubbles(sonarflowState.getBubbles());
		view.setDeviceSizeInfo(this);
		view.setDelegate(new BubblesView.Delegate() {
			@Override
			public void onTappedBubble(Bubble bubble) {
				playMediaItem(bubble.getMediaItem());
			}

			@Override
			public RelativeLayout getRootLayout() {
				return (RelativeLayout) findViewById(R.id.root);
			}

			@Override
			public void savePiemenue(PiemenueSaver save) {
				musicView.savePiemenue(save);
			}

			@Override
			public PiemenueSaver getSavedPie() {
				return musicView.getPiemenueSaver();
			}

			@Override
			public void setAnimationHistory(ArrayList<AnimationHistory> list) {
				musicView.setAnimationList(list);
			}

			@Override
			public ArrayList<AnimationHistory> getHistoryList() {
				return musicView.getAnimationList();
			}

			@Override
			public ImageView getBackArrow() {
				return backArrow;
			}
		});
		
		/*
		Log.v("MyLog","Bubbles" + bubbles);
		if (bubbles != null && bubbles.size() > 0)
//		view.addBubbles(bubbles);
		//if not null added by PO for error handling during development
		
		if(bubbles!=null){
			if (bubbles.size() > 0)
				while (bubbles.get(bubbles.size() - 1).type == DiscoveryBubble.DISCOVERY_BUBBLE)
					bubbles.remove(bubbles.size() - 1);
		}
		*/
		if (restoreViewState) {
			view.restore(savedInstanceState);
		}
		
		// Log settings
		LOGGER.info("scrobbling setting: "
				+ PreferenceManager.getDefaultSharedPreferences(this)
						.getBoolean(getString(R.string.pref_lastfm_scrobbling),
								false));
	}
	
	public void addBubbles(List<Bubble> bubbles)
	{
		// check if now bubbles
		if(bubbles!=null){
			if (bubbles.size() == 0) 
				noMusicDialog();
			view.addBubbles(bubbles);
		} else {
			LOGGER.warn("addBubbles: argument bubbles ==  null!");
		}
	}
	
	
	private void createToolbars() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager.findFragmentByTag(PLAYER_TOOLBAR_FRAGMENT_TAG) != null) {
			return;
		}

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(new PlayerToolbarFragment(), PLAYER_TOOLBAR_FRAGMENT_TAG);
		fragmentTransaction.commit();
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_menu, menu);
		// set handler for mood switch button
		Switch toggle = (Switch) menu.findItem(R.id.menu_moodview).getActionView().findViewById(R.id.switcher);
		toggle.setOnCheckedChangeListener(this);
		
		// View Toolbar
		if (sonarflowState != null) {
//			LOGGER.debug("onCreateOptionsMenu with sonarflowState.getActiveView() == {}", sonarflowState.getActiveView());
			toggle.setChecked(sonarflowState.getActiveView() == SonarflowState.MOOD_BUBBLES);
		} else {
			LOGGER.warn("HTTP response cache installation failed:");
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
	
		// set handler for mood switch button
		Switch toggle = (Switch) menu.findItem(R.id.menu_moodview).getActionView().findViewById(R.id.switcher);
		toggle.setChecked(sonarflowState.getActiveView() == SonarflowState.MOOD_BUBBLES);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		LOGGER.debug("onOptionsItemSelected with {}", item.getItemId());
		
        // zoom out or home button
		if ( /*  item.getItemId() == R.id.menu_zoom_out ||    */ item.getItemId() == android.R.id.home) {
			view.zoomOut();
			return true;
		}
		if (item.getItemId() == R.id.settings) {
			Intent intent = new Intent(this, SonarflowSettingsActivity.class);
			startActivity(intent);
			return true;
		}
		if (item.getItemId() == R.id.feedback) {

			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_EMAIL,
					new String[] { getString(R.string.email_feedback) });
			i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fb_subject));
			i.putExtra(Intent.EXTRA_TEXT, getString(R.string.fb_intro)
					+ SonarflowApplication.getUserDeviceDetailsAsString());
			try {
				startActivity(Intent.createChooser(i, SonarflowApplication.getAppContext().getString(R.string.send_feedback_mail)));
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(MainActivity.this,
						getString(R.string.no_email_installed),
						Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		
		if (item.getItemId() == R.id.help) {
			Intent intent = new Intent(this, SonarflowHelpActivity.class);
			startActivity(intent);
			return true;
		}
	
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startService(PlayerService.createIntent(PlayerService.Action.HideNoti, this));
	}

	@Override
	protected void onPause() {
		super.onPause();
		startService(PlayerService.createIntent(PlayerService.Action.ShowNoti, this));
	}

	protected void onStop() {
		HttpResponseCache cache = HttpResponseCache.getInstalled();
		if (cache != null) {
			cache.flush();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		view.onDestroy();
		super.onDestroy();
	}

	private void playMediaItem(MediaItem mediaItem) {
		startService(PlayerService.createPlayIntent(this, mediaItem.getTracks()));
	}

	private SonarflowState initializeSonarflow() {
		SonarflowState state = new SonarflowState();
		state.loadLibrary(this);
    	return state;
	}
	
	/** Recreates the sonarflowState, reloading the music library */
	private void basicReloadLibrary() {
		// This is to start the media scanner
//		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse 
//				("file://" + Environment.getExternalStorageDirectory()))); 

		sonarflowState = initializeSonarflow();
		view.addBubbles(sonarflowState.getBubbles());
		recreate();
	}
	
	public void noMusicDialog() {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		// 2. Chain together various setter methods to set the dialog characteristics
		builder.setMessage(R.string.nomusicdlg_msg)
		       .setTitle(R.string.nomusicdlg_title)
		       .setPositiveButton(R.string.nomusicdlg_button, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               basicReloadLibrary();
		           }
		       });

		// 3. Get the AlertDialog from create()
		AlertDialog dialog = builder.create();
		
		dialog.show();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return sonarflowState;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		((SaveContainer) getFragmentManager().findFragmentById(R.id.root))
				.saveState(sonarflowState);
		if (view != null)
			view.save(outState);
	}
	
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// isChecked == true    -> switch was set to mood
		// isChecked == false   -> switch was set to genre
		
//		LOGGER.debug("onCheckedChanged with isChecked == {}", isChecked);

		// check if there is change
		// if not, return immediately
		if (!isChecked && sonarflowState.getActiveView() == SonarflowState.GENRE_BUBBLES)
			return;
		if (isChecked && sonarflowState.getActiveView() == SonarflowState.MOOD_BUBBLES)
			return;

//		LOGGER.debug("onCheckedChanged continues with...");
		
		// check if other view is finished and ready to be shown
		if (sonarflowState.isOtherViewFinished()) {
			view.cleanView();
			sonarflowState.switchView(view);			
		} else {
			// switch the button back
			//buttonView.toggle();
			buttonView.setChecked(sonarflowState.getActiveView() == SonarflowState.MOOD_BUBBLES);
			if (progressDlg != null) {
				// assertion: progressBar is initializid properly
				progressDlg.show();
			}
			// show message
//			Toast.makeText(this, "Please try again in a few seconds.", Toast.LENGTH_SHORT).show();
		}
	}

	public static PendingIntent createPendingIntent(Context context) {
		return PendingIntent.getActivity(context, 0, new Intent(context,
				MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	public void lockOrientation() {
		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}
	}
	
	public void freeOrientation() {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			}
		});		
	}

	/**
	 * @return the progressBar
	 */
	public ProgressDialog getProgressDlg() {
		return progressDlg;
	}

	/**
	 * @param progressBar the progressBar to set
	 */
	public void setProgressDlg(ProgressDialog progressBar) {
		this.progressDlg = progressBar;
	}

	/**
	 * @return the view
	 */
	public BubblesView getView() {
		return view;
	}

	public void onClick(View arg0) {
	
}
}
