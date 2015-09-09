package com.spectralmind.sf4android;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class SonarflowHelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sonarflow_help);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_sonarflow_help, menu);
		return true;
	}

}
