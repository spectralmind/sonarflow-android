package com.spectralmind.sf4android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SonarflowSettingsFragment extends PreferenceFragment  implements OnSharedPreferenceChangeListener
	{
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        // set version
        //PackageInfo pinfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
        //findPreference(getString(R.string.pref_about)).setSummary(PackageManager.getPac)
        
        // Set current options
        if (getActivity() != null) {
        	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        	for (String key : pref.getAll().keySet()) {
        		onSharedPreferenceChanged(pref, key);
        	}
        }
        
        findPreference("last_fm_logo").setLayoutResource(R.layout.logos);
        
//        getActivity().getLayoutInflater().inflate(R.layout.logos, null)
//        	.findViewById(R.id.last_fm_logo).setOnClickListener(new OnClickListener(){
//				@Override
//				public void onClick(View arg0) {
//					Intent i = new Intent(Intent.ACTION_VIEW, 
//						       Uri.parse("http://www.last.fm/home"));
//						startActivity(i);
//				}
//        	});
    }
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		 // Let's do something a preference value changes
        if (key.equals(getString(R.string.pref_lastfm_scrobbling))) {
        	String sum;
        	if (sharedPreferences.getBoolean(key, false)) {
        		sum = SonarflowApplication.getAppContext().getString(R.string.tracks_will_be_scrobbled_to_last_fm);
        	} else {
        		sum =SonarflowApplication.getAppContext().getString(R.string.tracks_will_not_be_scrobbled_to_last_fm);
        	}
            findPreference(key).setSummary(sum);
        }

        if (key.equals(getString(R.string.pref_lastfm_username)))
        	findPreference(key).setSummary(sharedPreferences.getString(key, ""));

        if (key.equals(getString(R.string.pref_lastfm_pwd)))
        	findPreference(key).setSummary(getString(R.string.lastfm_pwd_default, ""));

	}
}
