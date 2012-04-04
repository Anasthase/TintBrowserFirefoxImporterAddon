/*
 * Firefox Bookmarks Importer for Tint Browser
 * 
 * Copyright (C) 2012 - to infinity and beyond J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.tint.firefoximporter.ui;

import org.tint.firefoximporter.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ErrorDetailsActivity extends Activity {
	
	public static final String EXTRA_ERROR_DETAILS = "EXTRA_ERROR_DETAILS";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.error_details_activity);
		
		Bundle extras = getIntent().getExtras();
		if ((extras != null) &&
				(extras.containsKey(EXTRA_ERROR_DETAILS))) {
			
			TextView details = (TextView) findViewById(R.id.ErrorDetailsActivityDetails);
			details.setText(extras.getString(EXTRA_ERROR_DETAILS));
		}
		
		Button settings = (Button) findViewById(R.id.ErrorDetailsActivitySettings);
		settings.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ErrorDetailsActivity.this, PreferencesActivity.class);
				ErrorDetailsActivity.this.startActivity(i);
			}
		});
		
		Button close = (Button) findViewById(R.id.ErrorDetailsActivityClose);
		close.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				ErrorDetailsActivity.this.finish();
			}
		});
	}

}
