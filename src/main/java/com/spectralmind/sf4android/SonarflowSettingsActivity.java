package com.spectralmind.sf4android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

public class SonarflowSettingsActivity extends Activity implements OnClickListener{
	
	private SonarflowSettingsFragment mSonarflowSettingsFragment;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSonarflowSettingsFragment = new SonarflowSettingsFragment();

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
		.replace(android.R.id.content, mSonarflowSettingsFragment)
		.commit();
		
        
		// ancestral navigation
		if (getActionBar() != null) 
			getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed
	            // in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, MainActivity.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
	    }
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.last_fm_logo)
		{
			startActivity(new Intent(Intent.ACTION_VIEW, 
				       Uri.parse("http://www.last.fm/")));
		}
		else if(v.getId() == R.id.seven_digital_logo)
		{
			startActivity(new Intent(Intent.ACTION_VIEW, 
				       Uri.parse("http://www.7digital.com/")));
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this)
		.registerOnSharedPreferenceChangeListener(mSonarflowSettingsFragment);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this)
		.unregisterOnSharedPreferenceChangeListener(mSonarflowSettingsFragment);
	}


}
